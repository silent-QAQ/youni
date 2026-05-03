package model

import "time"

type Server struct {
	ID            int64     `gorm:"primaryKey;autoIncrement" json:"id"`
	ServerID      string    `gorm:"uniqueIndex;size:64;not null" json:"server_id"`
	ServerName    string    `gorm:"size:128;not null" json:"server_name"`
	ServerSecret  string    `gorm:"size:256;not null" json:"-"`
	OwnerUUID     string    `gorm:"size:36;not null" json:"owner_uuid"`
	ServerType    string    `gorm:"size:16;not null" json:"server_type"`
	GameAddress   string    `gorm:"size:255;not null" json:"game_address"`
	TransportMode string    `gorm:"size:8;default:auto" json:"transport_mode"`
	P2PAddress    string    `gorm:"size:255" json:"p2p_address,omitempty"`
	P2PPort       int       `json:"p2p_port,omitempty"`
	RelayURL      string    `gorm:"size:255" json:"relay_url,omitempty"`
	MaxPlayers    int       `gorm:"default:20" json:"max_players"`
	IsOnline      bool      `gorm:"default:false" json:"is_online"`
	LastHeartbeat time.Time `json:"last_heartbeat,omitempty"`
	CreatedAt     time.Time `json:"created_at"`
	UpdatedAt     time.Time `json:"updated_at"`
}
