package main

import (
	"fmt"
	"log"
	"net/http"

	"github.com/youni/relay/internal/admin"
	"github.com/youni/relay/internal/config"
	"github.com/youni/relay/internal/handler"
	"github.com/youni/relay/internal/hub"
)

func main() {
	if err := config.Load("config.yaml"); err != nil {
		log.Fatalf("Failed to load config: %v", err)
	}

	cfg := config.Global
	h := hub.NewHub(cfg.WebSocket.PingIntervalSec, cfg.WebSocket.PongTimeoutSec)
	go h.Run()

	mux := http.NewServeMux()
	mux.HandleFunc("/ws", func(w http.ResponseWriter, r *http.Request) {
		handler.ServeWS(h, w, r)
	})
	mux.HandleFunc("/", func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		w.Write([]byte(`{"service":"youni-relay","status":"running"}`))
	})

	wsAddr := fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Server.Port)
	log.Printf("[Relay] WebSocket server starting on %s", wsAddr)

	if cfg.Admin.Enabled {
		adminServer := admin.NewAdminServer(h)
		adminAddr := fmt.Sprintf("%s:%d", cfg.Server.Host, cfg.Admin.Port)
		go func() {
			log.Printf("[Relay] Admin API starting on %s", adminAddr)
			if err := http.ListenAndServe(adminAddr, adminServer.Handler()); err != nil {
				log.Fatalf("[Relay] Admin server error: %v", err)
			}
		}()
	}

	if err := http.ListenAndServe(wsAddr, mux); err != nil {
		log.Fatalf("[Relay] WebSocket server error: %v", err)
	}
}
