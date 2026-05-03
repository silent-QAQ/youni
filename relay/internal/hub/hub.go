package hub

import (
	"sync"
	"sync/atomic"
	"time"
)

type Metrics struct {
	MessagesForwarded int64
	MessagesDropped   int64
	TotalConnections  int64
}

func (m *Metrics) AddMessageForwarded() {
	atomic.AddInt64(&m.MessagesForwarded, 1)
}

func (m *Metrics) AddMessageDropped() {
	atomic.AddInt64(&m.MessagesDropped, 1)
}

func (m *Metrics) AddConnection() {
	atomic.AddInt64(&m.TotalConnections, 1)
}

func (m *Metrics) Snapshot() (forwarded, dropped, connections int64) {
	return atomic.LoadInt64(&m.MessagesForwarded),
		atomic.LoadInt64(&m.MessagesDropped),
		atomic.LoadInt64(&m.TotalConnections)
}

type Hub struct {
	mu             sync.RWMutex
	clients        map[string]*Client
	Register       chan *Client
	Unregister     chan *Client
	Metrics        *Metrics
	PingInterval   int
	PongTimeout    int
	StartedAt      time.Time
}

func NewHub(pingInterval, pongTimeout int) *Hub {
	return &Hub{
		clients:      make(map[string]*Client),
		Register:     make(chan *Client),
		Unregister:   make(chan *Client),
		Metrics:      &Metrics{},
		PingInterval: pingInterval,
		PongTimeout:  pongTimeout,
		StartedAt:    time.Now(),
	}
}

func (h *Hub) Run() {
	for {
		select {
		case client := <-h.Register:
			h.mu.Lock()
			h.clients[client.ServerID] = client
			h.Metrics.AddConnection()
			h.mu.Unlock()

		case client := <-h.Unregister:
			h.mu.Lock()
			if _, ok := h.clients[client.ServerID]; ok {
				delete(h.clients, client.ServerID)
				close(client.Send)
			}
			h.mu.Unlock()
		}
	}
}

func (h *Hub) GetClient(serverID string) *Client {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return h.clients[serverID]
}

func (h *Hub) GetConnectedServers() []string {
	h.mu.RLock()
	defer h.mu.RUnlock()

	servers := make([]string, 0, len(h.clients))
	for id := range h.clients {
		servers = append(servers, id)
	}
	return servers
}

func (h *Hub) GetConnectedCount() int {
	h.mu.RLock()
	defer h.mu.RUnlock()
	return len(h.clients)
}

func (h *Hub) Broadcast(data []byte) {
	h.mu.RLock()
	defer h.mu.RUnlock()

	for _, client := range h.clients {
		client.SendMessage(data)
	}
}

type StatusInfo struct {
	ConnectedServers int      `json:"connected_servers"`
	ServerList       []string `json:"server_list"`
	MessagesForwarded int64   `json:"messages_forwarded"`
	MessagesDropped  int64   `json:"messages_dropped"`
	TotalConnections int64   `json:"total_connections"`
	UptimeSeconds    int64   `json:"uptime_seconds"`
}

func (h *Hub) GetStatus() StatusInfo {
	forwarded, dropped, connections := h.Metrics.Snapshot()
	return StatusInfo{
		ConnectedServers:  h.GetConnectedCount(),
		ServerList:        h.GetConnectedServers(),
		MessagesForwarded: forwarded,
		MessagesDropped:  dropped,
		TotalConnections:  connections,
		UptimeSeconds:     int64(time.Since(h.StartedAt).Seconds()),
	}
}
