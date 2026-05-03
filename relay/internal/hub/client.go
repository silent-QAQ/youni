package hub

import (
	"sync"
	"time"

	"github.com/gorilla/websocket"
	"github.com/youni/relay/pkg/protocol"
)

type Client struct {
	Hub      *Hub
	Conn     *websocket.Conn
	ServerID string
	Send     chan []byte
	joinedAt time.Time

	mu sync.Mutex
}

func NewClient(hub *Hub, conn *websocket.Conn) *Client {
	return &Client{
		Hub:      hub,
		Conn:     conn,
		Send:     make(chan []byte, 256),
		joinedAt: time.Now(),
	}
}

func (c *Client) ReadPump() {
	defer func() {
		c.Hub.Unregister <- c
		c.Conn.Close()
	}()

	c.Conn.SetReadLimit(65536)
	c.Conn.SetReadDeadline(time.Now().Add(time.Duration(c.Hub.PongTimeout) * time.Second))
	c.Conn.SetPongHandler(func(string) error {
		c.Conn.SetReadDeadline(time.Now().Add(time.Duration(c.Hub.PongTimeout) * time.Second))
		return nil
	})

	for {
		_, message, err := c.Conn.ReadMessage()
		if err != nil {
			break
		}

		var frame protocol.Frame
		if err := protocol.Unmarshal(message, &frame); err != nil {
			continue
		}

		c.HandleFrame(&frame)
	}
}

func (c *Client) WritePump() {
	ticker := time.NewTicker(time.Duration(c.Hub.PingInterval) * time.Second)
	defer func() {
		ticker.Stop()
		c.Conn.Close()
	}()

	for {
		select {
		case message, ok := <-c.Send:
			c.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			if !ok {
				c.Conn.WriteMessage(websocket.CloseMessage, []byte{})
				return
			}
			if err := c.Conn.WriteMessage(websocket.TextMessage, message); err != nil {
				return
			}

		case <-ticker.C:
			c.Conn.SetWriteDeadline(time.Now().Add(10 * time.Second))
			frame := protocol.NewFrame(protocol.TypePing, struct{}{})
			if err := c.Conn.WriteMessage(websocket.TextMessage, frame.ToJSON()); err != nil {
				return
			}
		}
	}
}

func (c *Client) HandleFrame(frame *protocol.Frame) {
	switch frame.Type {
	case protocol.TypeRegister:
		c.handleRegister(frame)
	case protocol.TypeMessage:
		c.handleMessage(frame)
	case protocol.TypeMessageAck:
		c.handleMessageAck(frame)
	case protocol.TypePong:
		c.Conn.SetReadDeadline(time.Now().Add(time.Duration(c.Hub.PongTimeout) * time.Second))
	}
}

func (c *Client) handleRegister(frame *protocol.Frame) {
	var payload protocol.RegisterPayload
	if err := protocol.Unmarshal(frame.Payload, &payload); err != nil {
		c.sendRegisterAck(false, "invalid payload")
		return
	}

	if payload.ServerID == "" {
		c.sendRegisterAck(false, "server_id is required")
		return
	}

	if existing := c.Hub.GetClient(payload.ServerID); existing != nil {
		existing.SendClose()
	}

	c.ServerID = payload.ServerID
	c.Hub.Register <- c
	c.sendRegisterAck(true, "")
}

func (c *Client) handleMessage(frame *protocol.Frame) {
	if c.ServerID == "" {
		c.sendError("not registered")
		return
	}

	var payload protocol.MessagePayload
	if err := protocol.Unmarshal(frame.Payload, &payload); err != nil {
		c.sendError("invalid message payload")
		return
	}

	c.Hub.Metrics.AddMessageForwarded()

	target := c.Hub.GetClient(payload.TargetServer)
	if target == nil {
		c.Hub.Metrics.AddMessageDropped()
		ackFrame := protocol.NewFrame(protocol.TypeMessageAck, protocol.MessageAckPayload{
			MsgID:     payload.MsgID,
			Delivered: false,
		})
		c.SendMessage(ackFrame.ToJSON())
		return
	}

	forwardFrame := protocol.NewFrame(protocol.TypeMessage, &payload)
	target.SendMessage(forwardFrame.ToJSON())
}

func (c *Client) handleMessageAck(frame *protocol.Frame) {
	var payload protocol.MessageAckPayload
	if err := protocol.Unmarshal(frame.Payload, &payload); err != nil {
		return
	}

	c.Hub.mu.RLock()
	for _, client := range c.Hub.clients {
		if client.ServerID != c.ServerID {
			ackFrame := protocol.NewFrame(protocol.TypeMessageAck, &payload)
			client.SendMessage(ackFrame.ToJSON())
			break
		}
	}
	c.Hub.mu.RUnlock()
}

func (c *Client) sendRegisterAck(success bool, message string) {
	frame := protocol.NewFrame(protocol.TypeRegisterAck, protocol.RegisterAckPayload{
		Success: success,
		Message: message,
	})
	c.SendMessage(frame.ToJSON())
}

func (c *Client) sendError(msg string) {
	frame := protocol.NewFrame(protocol.TypeError, struct {
		Message string `json:"message"`
	}{Message: msg})
	c.SendMessage(frame.ToJSON())
}

func (c *Client) SendMessage(data []byte) {
	c.mu.Lock()
	defer c.mu.Unlock()

	select {
	case c.Send <- data:
	default:
	}
}

func (c *Client) SendClose() {
	close(c.Send)
}
