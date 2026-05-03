package handler

import (
	"log"
	"net/http"

	"github.com/gorilla/websocket"
	"github.com/youni/relay/internal/hub"
)

var upgrader = websocket.Upgrader{
	ReadBufferSize:  4096,
	WriteBufferSize: 4096,
	CheckOrigin: func(r *http.Request) bool {
		return true
	},
}

func ServeWS(h *hub.Hub, w http.ResponseWriter, r *http.Request) {
	conn, err := upgrader.Upgrade(w, r, nil)
	if err != nil {
		log.Printf("[WS] Upgrade error: %v", err)
		return
	}

	client := hub.NewClient(h, conn)
	log.Printf("[WS] New connection from %s", r.RemoteAddr)

	go client.WritePump()
	go client.ReadPump()
}
