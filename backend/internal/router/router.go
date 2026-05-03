package router

import (
	"github.com/gin-gonic/gin"
	"github.com/youni/backend/internal/handler"
	"github.com/youni/backend/internal/middleware"
)

func Setup(mode string) *gin.Engine {
	gin.SetMode(mode)
	r := gin.New()
	r.Use(gin.Logger(), gin.Recovery())

	api := r.Group("/api/v1")
	{
		api.POST("/server/register", handler.RegisterServer)
		api.POST("/server/auth", handler.AuthServer)

		auth := api.Group("")
		auth.Use(middleware.ServerAuth())
		{
			auth.POST("/player/login", handler.PlayerLogin)
			auth.POST("/player/logout", handler.PlayerLogout)

			auth.GET("/discovery/player/:uuid", handler.DiscoverPlayer)
			auth.POST("/discovery/offline-message", handler.StoreOfflineMessage)
			auth.GET("/discovery/offline-messages/:uuid", handler.GetOfflineMessages)

			auth.POST("/server/heartbeat", handler.ServerHeartbeat)
			auth.GET("/server/list", handler.GetServerList)
		}
	}

	return r
}
