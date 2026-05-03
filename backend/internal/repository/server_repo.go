package repository

import (
	"time"

	"github.com/youni/backend/internal/model"
	"github.com/youni/backend/pkg/database"
)

func CreateServer(s *model.Server) error {
	return database.DB.Create(s).Error
}

func GetServerByServerID(serverID string) (*model.Server, error) {
	var s model.Server
	err := database.DB.Where("server_id = ?", serverID).First(&s).Error
	if err != nil {
		return nil, err
	}
	return &s, nil
}

func UpdateServerHeartbeat(serverID string, onlineCount int, playerList []string) error {
	return database.DB.Model(&model.Server{}).
		Where("server_id = ?", serverID).
		Updates(map[string]interface{}{
			"is_online":      true,
			"last_heartbeat": time.Now(),
		}).Error
}

func SetServerOffline(serverID string) error {
	return database.DB.Model(&model.Server{}).
		Where("server_id = ?", serverID).
		Update("is_online", false).Error
}

func GetOnlineServers() ([]model.Server, error) {
	var servers []model.Server
	err := database.DB.Where("is_online = ?", true).Find(&servers).Error
	return servers, err
}
