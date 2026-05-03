package handler

import (
	"github.com/gin-gonic/gin"
	"github.com/youni/backend/internal/service"
	"github.com/youni/backend/pkg/response"
)

func ServerHeartbeat(c *gin.Context) {
	serverID := c.GetString("server_id")

	var req service.HeartbeatReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, err.Error())
		return
	}

	if err := service.ServerHeartbeat(serverID, &req); err != nil {
		response.ServerError(c, err.Error())
		return
	}
	response.OK(c, gin.H{"ok": true})
}
