package handler

import (
	"github.com/gin-gonic/gin"
	"github.com/youni/backend/internal/service"
	"github.com/youni/backend/pkg/response"
)

func DiscoverPlayer(c *gin.Context) {
	playerUUID := c.Param("uuid")
	if playerUUID == "" {
		response.BadRequest(c, "uuid is required")
		return
	}

	loc, err := service.DiscoverPlayer(playerUUID)
	if err != nil {
		response.ServerError(c, err.Error())
		return
	}
	response.OK(c, loc)
}

func StoreOfflineMessage(c *gin.Context) {
	var req service.StoreOfflineMessageReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, err.Error())
		return
	}

	if err := service.StoreOfflineMessage(&req); err != nil {
		response.ServerError(c, err.Error())
		return
	}
	response.OK(c, gin.H{"stored": true})
}

func GetOfflineMessages(c *gin.Context) {
	playerUUID := c.Param("uuid")
	if playerUUID == "" {
		response.BadRequest(c, "uuid is required")
		return
	}

	msgs, err := service.GetOfflineMessages(playerUUID)
	if err != nil {
		response.ServerError(c, err.Error())
		return
	}
	if msgs == nil {
		msgs = []service.OfflineMessageItem{}
	}
	response.OK(c, gin.H{"messages": msgs})
}
