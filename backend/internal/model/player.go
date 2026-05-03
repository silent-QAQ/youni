package model

import "time"

type Player struct {
	ID           int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	UUID         string    `gorm:"uniqueIndex;size:36;not null" json:"uuid"`
	Username     string    `gorm:"size:64;not null" json:"username"`
	DisplayName  string    `gorm:"size:64" json:"display_name,omitempty"`
	CreatedAt    time.Time `json:"created_at"`
	UpdatedAt    time.Time `json:"updated_at"`
	LastLogin    time.Time `json:"last_login,omitempty"`
	LastServerID string    `gorm:"size:64" json:"last_server_id,omitempty"`
	IsBanned     bool      `gorm:"default:false" json:"is_banned"`
}

type PlayerOnline struct {
	ID         int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	PlayerUUID string    `gorm:"uniqueIndex;size:36;not null" json:"player_uuid"`
	ServerID   string    `gorm:"index;size:64;not null" json:"server_id"`
	OnlineAt   time.Time `gorm:"autoCreateTime" json:"online_at"`
}
