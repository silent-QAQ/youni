package admin

import (
	"encoding/json"
	"net/http"

	"github.com/youni/relay/internal/hub"
)

type AdminServer struct {
	hub *hub.Hub
	mux *http.ServeMux
}

func NewAdminServer(h *hub.Hub) *AdminServer {
	a := &AdminServer{
		hub: h,
		mux: http.NewServeMux(),
	}
	a.registerRoutes()
	return a
}

func (a *AdminServer) registerRoutes() {
	a.mux.HandleFunc("/status", a.handleStatus)
	a.mux.HandleFunc("/servers", a.handleServers)
}

func (a *AdminServer) handleStatus(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}
	status := a.hub.GetStatus()
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"code": 200,
		"data": status,
	})
}

func (a *AdminServer) handleServers(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodGet {
		http.Error(w, "method not allowed", http.StatusMethodNotAllowed)
		return
	}
	servers := a.hub.GetConnectedServers()
	writeJSON(w, http.StatusOK, map[string]interface{}{
		"code": 200,
		"data": map[string]interface{}{
			"count":   len(servers),
			"servers": servers,
		},
	})
}

func (a *AdminServer) Handler() http.Handler {
	return a.mux
}

func writeJSON(w http.ResponseWriter, code int, v interface{}) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(code)
	json.NewEncoder(w).Encode(v)
}
