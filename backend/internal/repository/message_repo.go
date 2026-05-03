package repository

import (
	"time"

	"github.com/youni/backend/internal/model"
	"github.com/youni/backend/pkg/database"
)

func CreateOfflineMessage(msg *model.OfflineMessage) error {
	return database.DB.Create(msg).Error
}

func GetUndeliveredMessages(receiverUUID string) ([]model.OfflineMessage, error) {
	var msgs []model.OfflineMessage
	err := database.DB.Where("receiver_uuid = ? AND is_delivered = ?", receiverUUID, false).
		Order("created_at ASC").
		Find(&msgs).Error
	return msgs, err
}

func MarkMessagesDelivered(receiverUUID string) error {
	return database.DB.Model(&model.OfflineMessage{}).
		Where("receiver_uuid = ? AND is_delivered = ?", receiverUUID, false).
		Updates(map[string]interface{}{
			"is_delivered":  true,
			"delivered_at":  time.Now(),
		}).Error
}

func CountUndeliveredMessages(receiverUUID string) (int64, error) {
	var count int64
	err := database.DB.Model(&model.OfflineMessage{}).
		Where("receiver_uuid = ? AND is_delivered = ?", receiverUUID, false).
		Count(&count).Error
	return count, err
}
