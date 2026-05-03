package protocol

import "encoding/json"

const (
	TypeRegister     = "register"
	TypeRegisterAck  = "register_ack"
	TypeMessage      = "message"
	TypeMessageAck   = "message_ack"
	TypePing         = "ping"
	TypePong         = "pong"
	TypeError        = "error"
)

type Frame struct {
	Type    string          `json:"type"`
	Payload json.RawMessage `json:"payload"`
}

type RegisterPayload struct {
	ServerID string `json:"server_id"`
	Token    string `json:"token"`
}

type RegisterAckPayload struct {
	Success bool   `json:"success"`
	Message string `json:"message,omitempty"`
}

type MessagePayload struct {
	MsgID        string `json:"msg_id"`
	SenderServer string `json:"sender_server"`
	SenderUUID   string `json:"sender_uuid"`
	SenderName   string `json:"sender_name"`
	TargetServer string `json:"target_server"`
	ReceiverUUID string `json:"receiver_uuid"`
	Content      string `json:"content"`
	Timestamp    int64  `json:"timestamp"`
}

type MessageAckPayload struct {
	MsgID     string `json:"msg_id"`
	Delivered bool   `json:"delivered"`
}

func Marshal(v interface{}) []byte {
	data, _ := json.Marshal(v)
	return data
}

func Unmarshal(data []byte, v interface{}) error {
	return json.Unmarshal(data, v)
}

func NewFrame(typ string, payload interface{}) *Frame {
	data, _ := json.Marshal(payload)
	return &Frame{
		Type:    typ,
		Payload: data,
	}
}

func (f *Frame) ToJSON() []byte {
	data, _ := json.Marshal(f)
	return data
}
