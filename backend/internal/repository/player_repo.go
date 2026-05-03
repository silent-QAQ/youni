package repository

import (
	"time"

	"github.com/youni/backend/internal/model"
	"github.com/youni/backend/pkg/database"
	"gorm.io/gorm"
)

func CreatePlayer(p *model.Player) error {
	return database.DB.Create(p).Error
}

func GetPlayerByUUID(uuid string) (*model.Player, error) {
	var p model.Player
	err := database.DB.Where("uuid = ?", uuid).First(&p).Error
	if err != nil {
		return nil, err
	}
	return &p, nil
}

func UpdatePlayerLogin(uuid string, serverID string) error {
	return database.DB.Model(&model.Player{}).
		Where("uuid = ?", uuid).
		Updates(map[string]interface{}{
			"last_login":     time.Now(),
			"last_server_id": serverID,
		}).Error
}

func CreatePlayerOnline(po *model.PlayerOnline) error {
	return database.DB.Create(po).Error
}

func DeletePlayerOnline(playerUUID string) error {
	return database.DB.Where("player_uuid = ?", playerUUID).Delete(&model.PlayerOnline{}).Error
}

func GetPlayerOnline(playerUUID string) (*model.PlayerOnline, error) {
	var po model.PlayerOnline
	err := database.DB.Where("player_uuid = ?", playerUUID).First(&po).Error
	if err != nil {
		return nil, err
	}
	return &po, nil
}

func SyncPlayerList(serverID string, playerUUIDs []string) error {
	return database.DB.Transaction(func(tx *gorm.DB) error {
		if err := tx.Where("server_id = ?", serverID).Delete(&model.PlayerOnline{}).Error; err != nil {
			return err
		}
		for _, uuid := range playerUUIDs {
			po := model.PlayerOnline{
				PlayerUUID: uuid,
				ServerID:   serverID,
			}
			if err := tx.Create(&po).Error; err != nil {
				return err
			}
		}
		return nil
	})
}
