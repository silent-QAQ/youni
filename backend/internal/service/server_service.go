package service

import "github.com/youni/backend/internal/repository"

type HeartbeatReq struct {
	OnlineCount int      `json:"online_count"`
	PlayerList  []string `json:"player_list"`
}

func ServerHeartbeat(serverID string, req *HeartbeatReq) error {
	if err := repository.UpdateServerHeartbeat(serverID, req.OnlineCount, req.PlayerList); err != nil {
		return err
	}
	if err := repository.SyncPlayerList(serverID, req.PlayerList); err != nil {
		return err
	}
	return nil
}
