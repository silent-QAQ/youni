package handler

import (
	"github.com/gin-gonic/gin"
	"github.com/youni/backend/internal/service"
	"github.com/youni/backend/pkg/response"
)

func RegisterServer(c *gin.Context) {
	var req service.RegisterServerReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, err.Error())
		return
	}

	resp, err := service.RegisterServer(&req)
	if err != nil {
		response.ServerError(c, err.Error())
		return
	}
	response.OK(c, resp)
}

func AuthServer(c *gin.Context) {
	var req service.AuthServerReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, err.Error())
		return
	}

	resp, err := service.AuthServer(&req)
	if err != nil {
		response.Unauthorized(c, err.Error())
		return
	}
	response.OK(c, resp)
}

func PlayerLogin(c *gin.Context) {
	serverID := c.GetString("server_id")

	var req service.PlayerLoginReq
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, err.Error())
		return
	}

	resp, err := service.PlayerLogin(serverID, &req)
	if err != nil {
		response.ServerError(c, err.Error())
		return
	}
	response.OK(c, resp)
}

func PlayerLogout(c *gin.Context) {
	var req struct {
		UUID string `json:"uuid" binding:"required"`
	}
	if err := c.ShouldBindJSON(&req); err != nil {
		response.BadRequest(c, err.Error())
		return
	}

	_ = service.PlayerLogout(req.UUID)
	response.OK(c, nil)
}

func GetServerList(c *gin.Context) {
	servers, err := service.GetServerList()
	if err != nil {
		response.ServerError(c, err.Error())
		return
	}
	response.OK(c, gin.H{"servers": servers})
}
