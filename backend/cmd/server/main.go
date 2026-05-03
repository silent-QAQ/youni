package main

import (
	"fmt"
	"log"

	"github.com/youni/backend/internal/config"
	"github.com/youni/backend/internal/router"
	"github.com/youni/backend/pkg/database"
)

func main() {
	if err := config.Load("config.yaml"); err != nil {
		log.Fatalf("Failed to load config: %v", err)
	}

	if err := database.Init(); err != nil {
		log.Fatalf("Failed to init database: %v", err)
	}

	if err := database.AutoMigrate(); err != nil {
		log.Fatalf("Failed to auto migrate: %v", err)
	}

	r := router.Setup(config.Global.Server.Mode)

	addr := fmt.Sprintf(":%d", config.Global.Server.Port)
	log.Printf("Youni Central Backend starting on %s", addr)
	if err := r.Run(addr); err != nil {
		log.Fatalf("Failed to start server: %v", err)
	}
}
