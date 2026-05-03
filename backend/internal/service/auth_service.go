package service

import (
	"errors"
	"time"

	"github.com/google/uuid"
	"github.com/youni/backend/internal/model"
	"github.com/youni/backend/internal/repository"
	"github.com/youni/backend/pkg/auth"
	"golang.org/x/crypto/bcrypt"
	"gorm.io/gorm"
)

type RegisterServerReq struct {
	ServerName    string `json:"server_name" binding:"required"`
	ServerType    string `json:"server_type" binding:"required,oneof=paper folia fabric forge neoforge"`
	OwnerUUID     string `json:"owner_uuid" binding:"required"`
	GameAddress   string `json:"game_address" binding:"required"`
	TransportMode string `json:"transport_mode" binding:"omitempty,oneof=p2p relay auto"`
	P2PAddress    string `json:"p2p_address"`
	P2PPort       int    `json:"p2p_port"`
	MaxPlayers    int    `json:"max_players"`
}

type RegisterServerResp struct {
	ServerID     string `json:"server_id"`
	ServerSecret string `json:"server_secret"`
}

func RegisterServer(req *RegisterServerReq) (*RegisterServerResp, error) {
	serverID := "srv_" + uuid.New().String()[:8]
	secretPlain := "sec_" + uuid.New().String()
	secretHash, err := bcrypt.GenerateFromPassword([]byte(secretPlain), bcrypt.DefaultCost)
	if err != nil {
		return nil, err
	}

	transportMode := req.TransportMode
	if transportMode == "" {
		transportMode = "auto"
	}

	s := &model.Server{
		ServerID:      serverID,
		ServerName:    req.ServerName,
		ServerSecret:  string(secretHash),
		OwnerUUID:     req.OwnerUUID,
		ServerType:    req.ServerType,
		GameAddress:   req.GameAddress,
		TransportMode: transportMode,
		P2PAddress:    req.P2PAddress,
		P2PPort:       req.P2PPort,
		MaxPlayers:    req.MaxPlayers,
	}
	if s.MaxPlayers <= 0 {
		s.MaxPlayers = 20
	}

	if err := repository.CreateServer(s); err != nil {
		return nil, err
	}

	return &RegisterServerResp{
		ServerID:     serverID,
		ServerSecret: secretPlain,
	}, nil
}

type AuthServerReq struct {
	ServerID     string `json:"server_id" binding:"required"`
	ServerSecret string `json:"server_secret" binding:"required"`
}

type AuthServerResp struct {
	AccessToken string `json:"access_token"`
	ExpiresIn   int    `json:"expires_in"`
}

func AuthServer(req *AuthServerReq) (*AuthServerResp, error) {
	s, err := repository.GetServerByServerID(req.ServerID)
	if err != nil {
		if errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, errors.New("server not found")
		}
		return nil, err
	}

	if err := bcrypt.CompareHashAndPassword([]byte(s.ServerSecret), []byte(req.ServerSecret)); err != nil {
		return nil, errors.New("invalid secret")
	}

	token, err := auth.GenerateServerToken(req.ServerID)
	if err != nil {
		return nil, err
	}

	return &AuthServerResp{
		AccessToken: token,
		ExpiresIn:   3600,
	}, nil
}

type PlayerLoginReq struct {
	UUID     string `json:"uuid" binding:"required"`
	Username string `json:"username" binding:"required"`
}

type PlayerLoginResp struct {
	PlayerID           int64  `json:"player_id"`
	IsNewPlayer        bool   `json:"is_new_player"`
	HasOfflineMessages bool   `json:"has_offline_messages"`
	OfflineMsgCount    int64  `json:"offline_message_count"`
}

func PlayerLogin(serverID string, req *PlayerLoginReq) (*PlayerLoginResp, error) {
	player, err := repository.GetPlayerByUUID(req.UUID)
	isNew := false
	if err != nil {
		if !errors.Is(err, gorm.ErrRecordNotFound) {
			return nil, err
		}
		player = &model.Player{
			UUID:     req.UUID,
			Username: req.Username,
		}
		if err := repository.CreatePlayer(player); err != nil {
			return nil, err
		}
		isNew = true
	}

	_ = repository.UpdatePlayerLogin(req.UUID, serverID)

	_ = repository.DeletePlayerOnline(req.UUID)
	_ = repository.CreatePlayerOnline(&model.PlayerOnline{
		PlayerUUID: req.UUID,
		ServerID:   serverID,
	})

	count, _ := repository.CountUndeliveredMessages(req.UUID)

	return &PlayerLoginResp{
		PlayerID:           player.ID,
		IsNewPlayer:        isNew,
		HasOfflineMessages: count > 0,
		OfflineMsgCount:    count,
	}, nil
}

func PlayerLogout(uuid string) error {
	_ = repository.DeletePlayerOnline(uuid)
	return nil
}

type ServerInfo struct {
	ServerID      string    `json:"server_id"`
	ServerName    string    `json:"server_name"`
	OwnerUUID     string    `json:"owner_uuid"`
	ServerType    string    `json:"server_type"`
	GameAddress   string    `json:"game_address"`
	TransportMode string    `json:"transport_mode"`
	P2PAddress    string    `json:"p2p_address,omitempty"`
	P2PPort       int       `json:"p2p_port,omitempty"`
	RelayURL      string    `json:"relay_url,omitempty"`
	MaxPlayers    int       `json:"max_players"`
	IsOnline      bool      `json:"is_online"`
	LastHeartbeat time.Time `json:"last_heartbeat,omitempty"`
	CreatedAt     time.Time `json:"created_at"`
}

func GetServerList() ([]ServerInfo, error) {
	servers, err := repository.GetOnlineServers()
	if err != nil {
		return nil, err
	}
	var result []ServerInfo
	for _, s := range servers {
		result = append(result, ServerInfo{
			ServerID:      s.ServerID,
			ServerName:    s.ServerName,
			OwnerUUID:     s.OwnerUUID,
			ServerType:    s.ServerType,
			GameAddress:   s.GameAddress,
			TransportMode: s.TransportMode,
			P2PAddress:    s.P2PAddress,
			P2PPort:       s.P2PPort,
			RelayURL:      s.RelayURL,
			MaxPlayers:    s.MaxPlayers,
			IsOnline:      s.IsOnline,
			LastHeartbeat: s.LastHeartbeat,
			CreatedAt:     s.CreatedAt,
		})
	}
	return result, nil
}
