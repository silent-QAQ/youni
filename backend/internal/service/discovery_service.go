package service

import (
	"errors"

	"github.com/youni/backend/internal/model"
	"github.com/youni/backend/internal/repository"
	"gorm.io/gorm"
)

type PlayerLocation struct {
	Online        bool   `json:"online"`
	ServerID      string `json:"server_id,omitempty"`
	ServerName    string `json:"server_name,omitempty"`
	TransportMode string `json:"transport_mode,omitempty"`
	P2PAddress    string `json:"p2p_address,omitempty"`
	P2PPort       int    `json:"p2p_port,omitempty"`
}

func DiscoverPlayer(playerUUID string) (*PlayerLocation, error) {
	po, err := repository.GetPlayerOnline(playerUUID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return &PlayerLocation{Online: false}, nil
		}
		return nil, err
	}

	s, err := repository.GetServerByServerID(po.ServerID)
	if err != nil {
		return &PlayerLocation{Online: false}, nil
	}

	return &PlayerLocation{
		Online:        true,
		ServerID:      s.ServerID,
		ServerName:    s.ServerName,
		TransportMode: s.TransportMode,
		P2PAddress:    s.P2PAddress,
		P2PPort:       s.P2PPort,
	}, nil
}

type StoreOfflineMessageReq struct {
	MsgID        string `json:"msg_id" binding:"required"`
	SenderUUID   string `json:"sender_uuid" binding:"required"`
	SenderName   string `json:"sender_name" binding:"required"`
	ReceiverUUID string `json:"receiver_uuid" binding:"required"`
	Content      string `json:"content" binding:"required"`
}

func StoreOfflineMessage(req *StoreOfflineMessageReq) error {
	msg := &model.OfflineMessage{
		MsgID:        req.MsgID,
		SenderUUID:   req.SenderUUID,
		SenderName:   req.SenderName,
		ReceiverUUID: req.ReceiverUUID,
		Content:      req.Content,
	}
	return repository.CreateOfflineMessage(msg)
}

type OfflineMessageItem struct {
	MsgID      string `json:"msg_id"`
	SenderUUID string `json:"sender_uuid"`
	SenderName string `json:"sender_name"`
	Content    string `json:"content"`
	CreatedAt  string `json:"created_at"`
}

func GetOfflineMessages(playerUUID string) ([]OfflineMessageItem, error) {
	msgs, err := repository.GetUndeliveredMessages(playerUUID)
	if err != nil {
		return nil, err
	}

	var result []OfflineMessageItem
	for _, m := range msgs {
		result = append(result, OfflineMessageItem{
			MsgID:      m.MsgID,
			SenderUUID: m.SenderUUID,
			SenderName: m.SenderName,
			Content:    m.Content,
			CreatedAt:  m.CreatedAt.Format("2006-01-02T15:04:05Z"),
		})
	}

	_ = repository.MarkMessagesDelivered(playerUUID)
	return result, nil
}
