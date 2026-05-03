package model

import "time"

type OfflineMessage struct {
	ID          int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	MsgID       string    `gorm:"uniqueIndex;size:36;not null" json:"msg_id"`
	SenderUUID  string    `gorm:"size:36;not null" json:"sender_uuid"`
	SenderName  string    `gorm:"size:64;not null" json:"sender_name"`
	ReceiverUUID string   `gorm:"index;size:36;not null" json:"receiver_uuid"`
	Content     string    `gorm:"type:text;not null" json:"content"`
	IsDelivered bool      `gorm:"default:false" json:"is_delivered"`
	CreatedAt   time.Time `json:"created_at"`
	DeliveredAt time.Time `json:"delivered_at,omitempty"`
}
