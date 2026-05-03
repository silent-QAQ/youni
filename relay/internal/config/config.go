package config

import (
	"os"

	"gopkg.in/yaml.v3"
)

type Config struct {
	Server    ServerConfig    `yaml:"server"`
	Admin     AdminConfig     `yaml:"admin"`
	WebSocket WebSocketConfig `yaml:"websocket"`
}

type ServerConfig struct {
	Host string `yaml:"host"`
	Port int    `yaml:"port"`
	Mode string `yaml:"mode"`
}

type AdminConfig struct {
	Enabled bool `yaml:"enabled"`
	Port    int  `yaml:"port"`
}

type WebSocketConfig struct {
	ReadBufferSize    int `yaml:"read_buffer_size"`
	WriteBufferSize   int `yaml:"write_buffer_size"`
	PingIntervalSec   int `yaml:"ping_interval_seconds"`
	PongTimeoutSec    int `yaml:"pong_timeout_seconds"`
}

var Global Config

func Load(path string) error {
	data, err := os.ReadFile(path)
	if err != nil {
		return err
	}
	if err := yaml.Unmarshal(data, &Global); err != nil {
		return err
	}
	if Global.Server.Port == 0 {
		Global.Server.Port = 9877
	}
	if Global.Admin.Port == 0 {
		Global.Admin.Port = 9878
	}
	if Global.WebSocket.ReadBufferSize == 0 {
		Global.WebSocket.ReadBufferSize = 4096
	}
	if Global.WebSocket.WriteBufferSize == 0 {
		Global.WebSocket.WriteBufferSize = 4096
	}
	if Global.WebSocket.PingIntervalSec == 0 {
		Global.WebSocket.PingIntervalSec = 25
	}
	if Global.WebSocket.PongTimeoutSec == 0 {
		Global.WebSocket.PongTimeoutSec = 60
	}
	return nil
}
