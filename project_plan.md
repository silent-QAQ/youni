# Youni - Minecraft 全服务端社交系统项目计划书

## 1. 项目概述

### 1.1 项目名称
Youni - Minecraft 统一社交网络系统

### 1.2 项目目标
构建一个跨服务端的Minecraft社交系统，支持所有主流服务端实现（Paper、Folia、Fabric、Forge、NeoForge），实现服务器间玩家通讯和信息共享。

### 1.3 核心价值
- **统一协议**：所有服务端使用同一套通信协议
- **跨服社交**：玩家可以在不同服务器间发送消息
- **双模消息传输**：支持服务器间直连(P2P)和中转后端两种模式
- **内部网络**：多个MC服务器可组成聊天内部网络，自由互通
- **广泛兼容**：支持插件端和mod端

## 2. 系统架构

### 2.1 架构总览

系统由三个独立服务 + 客户端插件/Mod组成：

| 服务 | 语言 | 职责 |
|------|------|------|
| **中心后端 (Central)** | Go | 认证授权、玩家管理、路由发现、离线消息 |
| **中转后端 (Relay)** | Go | 消息中转，为无法直连的服务器转发聊天消息 |
| **游戏服务端插件/Mod** | Java | 游戏内交互、消息收发、连接管理 |

消息传输有 **两种模式**，服务器可任选其一或混合使用：

| 模式 | 通信路径 | 适用场景 |
|------|----------|----------|
| **P2P直连模式** | ServerA ←WebSocket→ ServerB | 服务器有公网IP或同内网 |
| **中转模式** | ServerA → Relay → ServerB | 服务器在NAT后，无法直连 |

两种模式共用同一套消息协议格式，对上层业务透明。

### 2.2 整体架构图

```
┌──────────────────────────────────────────────────────────────────────────────┐
│                           ① 中心后端 (Go + MySQL)                            │
│                          认证 / 路由发现 / 离线消息                            │
│                     ※ 不传输任何玩家聊天消息内容                               │
└──────────────────────────────────┬───────────────────────────────────────────┘
                                   │ REST API
                    ┌──────────────┼──────────────────┐
                    │              │                  │
                    ▼              ▼                  ▼
          ┌──────────────┐ ┌──────────────┐  ┌──────────────┐
          │ 游戏服务器 A  │ │ 游戏服务器 B  │  │ 游戏服务器 C  │
          │ (Paper)      │ │ (Fabric)     │  │ (Forge)      │
          │              │ │              │  │              │
          │ Youni插件/Mod│ │ Youni插件/Mod│  │ Youni插件/Mod│
          └──────┬───────┘ └──────┬───────┘  └──────┬───────┘
                 │                │                  │
                 │                │                  │
      ═══════════╪════════════════╪══════════════════╪═══════════════
      消息传输层  │                │                  │
      ═══════════╪════════════════╪══════════════════╪═══════════════
                 │                │                  │
                 │  ┌─────────────────────────────┐  │
                 │  │                             │  │
                 │  │   ② 中转后端 (Go)            │  │
                 ├──┤   接受服务器WebSocket连接     ├──┤
                 │  │   在服务器之间转发消息        │  │
                 │  │   ※ 不访问中心后端           │  │
                 │  │   ※ 仅转发, 不存储消息       │  │
                 │  └─────────────────────────────┘  │
                 │                                    │
                 │         P2P 直连模式               │
                 └────────────┬───────────────────────┘
                              │ WebSocket
                              ▼
                 A ←──────────B──────────→ C
                 (能直连的服务器之间直接通信)
```

### 2.3 两种消息传输模式详解

#### 模式一：P2P 直连（内部网络）

适合：服务器有公网IP、同机房、同内网等可直连场景。

```
Server A ◄──── WebSocket ────► Server B
               消息直传
           (不经过任何后端)
```

- 每个服务器启动一个 WebSocket 服务端，同时维护到其他服务器的 WebSocket 客户端连接
- 服务器间形成 **Mesh 网络**，任意两点可直连
- 向中心后端查询目标玩家所在服务器的P2P地址后直连发送

#### 模式二：中转后端（Relay）

适合：服务器在NAT后、家庭网络、无法开放端口的场景。

```
Server A ──WebSocket──► Relay ──WebSocket──► Server B
                        (中转)
                   仅转发, 不存储
```

- 中转后端是一个独立的Go服务，接受多个服务器的WebSocket长连接
- 服务器连接到中转后端后，中转后端维护 `server_id → connection` 映射
- 发送消息时，中转后端根据目标server_id找到对应连接并转发
- 中转后端不做认证（认证由中心后端完成），只做消息转发
- 中转后端可水平扩展部署多个实例

#### 模式选择

服务器在配置文件中选择模式：
```yaml
youni:
  mode: p2p        # p2p | relay | auto
  p2p:
    port: 9876
    address: "0.0.0.0"
  relay:
    url: "wss://relay.example.com"
```

- `p2p`：仅使用P2P直连
- `relay`：仅使用中转后端
- `auto`（推荐）：优先尝试P2P直连，连接失败则自动回退到中转后端

### 2.4 消息流转过程

#### P2P 直连模式
```
玩家A(服务器A) 想发消息给 玩家B(服务器B)

步骤1: 服务器A 调用中心后端API查询玩家B的位置
       → 中心后端返回: "服务器B, P2P地址: 1.2.3.4:9876"

步骤2: 服务器A 通过WebSocket直连服务器B
       → 发送消息 (不经过任何后端)

步骤3: 服务器B 收到消息 → 投递给玩家B
```

#### 中转模式
```
玩家A(服务器A) 想发消息给 玩家B(服务器B)

步骤1: 服务器A 调用中心后端API查询玩家B的位置
       → 中心后端返回: "服务器B (使用中转模式)"

步骤2: 服务器A 通过已建立的WebSocket连接发送消息到中转后端
       → {type: "message", target_server: "srv_bbb", payload: {...}}

步骤3: 中转后端根据target_server找到服务器B的连接 → 转发消息

步骤4: 服务器B 收到消息 → 投递给玩家B
```

### 2.5 技术栈

| 层级 | 技术选型 | 说明 |
|------|----------|------|
| 中心后端 | Go 1.21+ | 高性能、易于部署 |
| 中转后端 | Go 1.21+ | 高并发WebSocket转发 |
| 数据库 | MySQL 8.0+ | 稳定可靠的关系型数据库 |
| Web框架 | Gin | 轻量级HTTP框架 |
| ORM | GORM | Go ORM库 |
| WebSocket | gorilla/websocket | Go WebSocket库 |
| P2P协议 | WebSocket | 服务器间持久连接 |
| JSON序列化 | encoding/json | 标准库 |
| 插件端 | Paper/Folia API | Bukkit系插件开发 |
| Mod端 | Fabric API / Forge API | Fabric和Forge/NeoForge mod开发 |

## 3. 功能模块设计

### 3.1 中心后端 (backend)

中心后端**不处理任何玩家聊天消息内容**，仅提供：

#### 3.1.1 认证模块 (Auth)
- **服务器注册**：服主注册服务器，获取 Server ID 和 Server Secret
- **服务器认证**：使用 ID + Secret 获取 JWT 访问令牌
- **玩家注册**：玩家首次加入 Youni 服务器时自动注册（UUID绑定）
- **玩家登录**：服务器代玩家调用后端完成登录

#### 3.1.2 路由发现模块 (Discovery)
- **玩家定位**：查询目标玩家当前所在服务器及其通信方式(P2P地址/中转)
- **离线消息暂存**：目标玩家不在线时暂存消息
- **离线消息拉取**：玩家上线后拉取暂存的离线消息

#### 3.1.3 服务器管理模块 (Server)
- **心跳上报**：服务器定期上报在线状态和玩家列表
- **玩家同步**：玩家上下线时更新位置信息
- **服务器列表**：查询已注册服务器列表及其通信模式

### 3.2 中转后端 (relay)

中转后端是一个独立的Go服务，**不需要数据库**，纯内存运行：

#### 3.2.1 连接管理
- **接受连接**：接受游戏服务器的WebSocket长连接
- **身份注册**：服务器连接后发送server_id标识自己
- **连接映射**：维护 `server_id → WebSocket connection` 映射表
- **心跳保活**：检测断开的连接并清理

#### 3.2.2 消息转发
- **消息路由**：根据消息中的目标server_id转发给对应服务器
- **广播**：支持向所有连接的服务器广播消息（如系统通知）
- **消息队列**：目标服务器短暂断连时暂存消息（可选）

#### 3.2.3 管理接口
- **状态查询**：查询当前连接的服务器数量和列表
- **统计信息**：消息转发量、连接数等

### 3.3 游戏服务端模块（插件/Mod）

#### 3.3.1 API 客户端模块
- 与中心后端 REST API 通信
- 处理服务器认证、玩家登录、路由查询

#### 3.3.2 消息传输模块（统一抽象）
- **Transport 接口**：统一的消息传输接口，屏蔽P2P和中转的差异
- **P2P Transport**：WebSocket直连实现
  - P2P Server：启动WebSocket服务端监听
  - P2P Client：连接其他服务器的WebSocket
  - P2P Pool：连接池管理
- **Relay Transport**：中转后端实现
  - Relay Client：连接中转后端的WebSocket
  - 自动重连

#### 3.3.3 游戏交互模块
- **命令系统**：`/youni msg`, `/youni reply` 等
- **事件监听**：玩家加入/退出触发登录/登出
- **消息显示**：游戏内格式化显示跨服消息

#### 3.3.4 服务端适配层
- **Common**：通用接口（Transport、API客户端、命令接口）
- **Paper/Folia**：Bukkit系适配
- **Fabric**：Fabric适配
- **Forge**：Forge适配
- **NeoForge**：NeoForge适配

## 4. 通信协议设计

### 4.1 协议总览

P2P直连和中转模式使用 **相同的JSON协议**，区别仅在路由方式：

| 特性 | P2P直连 | 中转模式 |
|------|---------|----------|
| 传输层 | WebSocket (ws/wss) | WebSocket (ws/wss) |
| 消息格式 | JSON | JSON（相同） |
| 认证 | 握手时JWT验证 | 连接时server_id注册 |
| 路由 | 直接发给目标服务器 | 由中转后端根据target_server转发 |

### 4.2 通用消息帧格式

```json
{
    "type": "消息类型",
    "payload": { ... }
}
```

消息类型：

| type | 说明 | 使用场景 |
|------|------|----------|
| `handshake` | P2P握手请求 | P2P模式 |
| `handshake_ack` | P2P握手响应 | P2P模式 |
| `register` | 中转注册 | 中转模式 |
| `register_ack` | 中转注册确认 | 中转模式 |
| `message` | 聊天消息 | 通用 |
| `message_ack` | 消息投递确认 | 通用 |
| `ping` | 心跳请求 | 通用 |
| `pong` | 心跳响应 | 通用 |
| `error` | 错误 | 通用 |

### 4.3 P2P 直连握手流程

```
Server A                          Server B
   │                                  │
   │──── WS Connect (ws://B:9876) ───>│
   │<──── 101 Switching Protocols ────│
   │                                  │
   │──── Handshake ──────────────────>│
   │     {                            │
   │       "type": "handshake",       │
   │       "payload": {               │
   │         "server_id": "srv_aaa",  │
   │         "token": "jwt_token"     │
   │       }                          │
   │     }                            │
   │                                  │  验证JWT
   │<──── Handshake Ack ──────────────│
   │     {                            │
   │       "type": "handshake_ack",   │
   │       "payload": {               │
   │         "success": true,         │
   │         "server_id": "srv_bbb"   │
   │       }                          │
   │     }                            │
   │                                  │
   │     连接建立, 可传输消息           │
```

### 4.4 中转后端连接流程

```
Server A                        Relay                          Server B
   │                              │                               │
   │──── WS Connect ─────────────>│                               │
   │<──── 101 ────────────────────│                               │
   │                              │                               │
   │──── Register ────────────────>│                               │
   │     {                        │                               │
   │       "type": "register",    │                               │
   │       "payload": {           │                               │
   │         "server_id": "srv_aaa",                              │
   │         "token": "jwt_token" │  (可选: 验证JWT)              │
   │       }                      │                               │
   │     }                        │                               │
   │<──── Register Ack ───────────│                               │
   │     {                        │                               │
   │       "type": "register_ack",│                               │
   │       "payload": {           │                               │
   │         "success": true      │                               │
   │       }                      │                               │
   │     }                        │                               │
   │                              │<──── WS Connect ──────────────│
   │                              │──── 101 ─────────────────────►│
   │                              │<──── Register ────────────────│
   │                              │──── Register Ack ────────────►│
   │                              │                               │
   │    两个服务器都已注册,          │                               │
   │    可以通过Relay互相发消息      │                               │
```

### 4.5 聊天消息传输

```json
// 发送方 → 接收方（或发送方 → Relay → 接收方）
{
    "type": "message",
    "payload": {
        "msg_id": "uuid-v4",
        "sender_server": "srv_aaa",
        "sender_uuid": "player-a-uuid",
        "sender_name": "PlayerA",
        "target_server": "srv_bbb",
        "receiver_uuid": "player-b-uuid",
        "content": "Hello!",
        "timestamp": 1714700000
    }
}

// 接收方 → 发送方（投递确认）
{
    "type": "message_ack",
    "payload": {
        "msg_id": "uuid-v4",
        "delivered": true
    }
}
```

### 4.6 连接管理

| 特性 | P2P模式 | 中转模式 |
|------|---------|----------|
| 连接方式 | 按需连接其他服务器 | 启动时连接中转后端 |
| 连接池 | 维护到各服务器的连接 | 仅一条到中转后端的连接 |
| 懒连接 | 是，首次发消息时建立 | 否，启动时即连接 |
| 超时断开 | 空闲5分钟断开 | 心跳保活不断开 |
| 重连 | 指数退避自动重连 | 指数退避自动重连 |

### 4.7 安全机制

- **P2P认证**：JWT握手验证对方服务器身份
- **中转认证**：连接时可选JWT验证（或由中心后端发的relay_token）
- **速率限制**：防止单个服务器发送过多消息
- **WSS加密**：生产环境使用TLS加密连接

## 5. 数据库设计

### 5.1 数据库表结构

> 数据库仅在中心后端使用，中转后端不需要数据库。

#### 5.1.1 服务器表 (servers)
```sql
CREATE TABLE servers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    server_id VARCHAR(64) UNIQUE NOT NULL COMMENT '服务器唯一标识',
    server_name VARCHAR(128) NOT NULL COMMENT '服务器名称',
    server_secret VARCHAR(256) NOT NULL COMMENT '服务器密钥(bcrypt加密)',
    owner_uuid VARCHAR(36) NOT NULL COMMENT '服主MC UUID',
    server_type ENUM('paper', 'folia', 'fabric', 'forge', 'neoforge') NOT NULL,
    game_address VARCHAR(255) NOT NULL COMMENT '游戏地址(供玩家连接)',
    transport_mode ENUM('p2p', 'relay', 'auto') DEFAULT 'auto' COMMENT '消息传输模式',
    p2p_address VARCHAR(255) COMMENT 'P2P地址(直连模式用)',
    p2p_port INT COMMENT 'P2P端口(直连模式用)',
    relay_url VARCHAR(255) COMMENT '中转后端地址(中转模式用)',
    max_players INT DEFAULT 20,
    is_online BOOLEAN DEFAULT FALSE,
    last_heartbeat DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

#### 5.1.2 玩家表 (players)
```sql
CREATE TABLE players (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    uuid VARCHAR(36) UNIQUE NOT NULL COMMENT 'MC UUID',
    username VARCHAR(64) NOT NULL COMMENT 'MC用户名',
    display_name VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    last_login DATETIME,
    last_server_id VARCHAR(64),
    is_banned BOOLEAN DEFAULT FALSE
);
```

#### 5.1.3 在线状态表 (player_online)
```sql
CREATE TABLE player_online (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    player_uuid VARCHAR(36) UNIQUE NOT NULL,
    server_id VARCHAR(64) NOT NULL,
    online_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_player_uuid (player_uuid),
    INDEX idx_server_id (server_id)
);
```

#### 5.1.4 离线消息表 (offline_messages)
```sql
CREATE TABLE offline_messages (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    msg_id VARCHAR(36) UNIQUE NOT NULL,
    sender_uuid VARCHAR(36) NOT NULL,
    sender_name VARCHAR(64) NOT NULL,
    receiver_uuid VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    is_delivered BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    delivered_at DATETIME,
    INDEX idx_receiver (receiver_uuid, is_delivered)
);
```

## 6. API 接口设计

### 6.1 服务器认证接口

#### 6.1.1 服务器注册
```
POST /api/v1/server/register
Request:
{
    "server_name": "My Server",
    "server_type": "paper",
    "owner_uuid": "player-uuid",
    "game_address": "play.example.com:25565",
    "transport_mode": "auto",
    "p2p_address": "1.2.3.4",
    "p2p_port": 9876,
    "max_players": 100
}
Response:
{
    "code": 200,
    "data": {
        "server_id": "srv_xxxxxxxx",
        "server_secret": "secret_xxxxxxxx"
    }
}
```

#### 6.1.2 服务器认证
```
POST /api/v1/server/auth
Request:
{
    "server_id": "srv_xxxxxxxx",
    "server_secret": "secret_xxxxxxxx"
}
Response:
{
    "code": 200,
    "data": {
        "access_token": "eyJhbGciOiJIUzI1NiIs...",
        "expires_in": 3600
    }
}
```

### 6.2 玩家认证接口

#### 6.2.1 玩家登录
```
POST /api/v1/player/login
Headers: Authorization: Bearer {server_access_token}
Request:
{
    "uuid": "player-uuid",
    "username": "player_name"
}
Response:
{
    "code": 200,
    "data": {
        "player_id": 1,
        "is_new_player": false,
        "has_offline_messages": true,
        "offline_message_count": 3
    }
}
```

#### 6.2.2 玩家登出
```
POST /api/v1/player/logout
Headers: Authorization: Bearer {server_access_token}
Request:
{
    "uuid": "player-uuid"
}
Response:
{
    "code": 200
}
```

### 6.3 路由发现接口

#### 6.3.1 查询玩家位置
```
GET /api/v1/discovery/player/{uuid}
Headers: Authorization: Bearer {server_access_token}
Response (在线):
{
    "code": 200,
    "data": {
        "online": true,
        "server_id": "srv_yyyyyyyy",
        "server_name": "Another Server",
        "transport_mode": "p2p",
        "p2p_address": "5.6.7.8",
        "p2p_port": 9876
    }
}
Response (在线-中转模式):
{
    "code": 200,
    "data": {
        "online": true,
        "server_id": "srv_yyyyyyyy",
        "server_name": "Another Server",
        "transport_mode": "relay"
    }
}
Response (离线):
{
    "code": 200,
    "data": {
        "online": false
    }
}
```

#### 6.3.2 存储离线消息
```
POST /api/v1/discovery/offline-message
Headers: Authorization: Bearer {server_access_token}
Request:
{
    "msg_id": "uuid-v4",
    "sender_uuid": "sender-uuid",
    "sender_name": "PlayerA",
    "receiver_uuid": "receiver-uuid",
    "content": "Hello!"
}
Response:
{
    "code": 200,
    "data": {
        "stored": true
    }
}
```

#### 6.3.3 拉取离线消息
```
GET /api/v1/discovery/offline-messages/{player_uuid}
Headers: Authorization: Bearer {server_access_token}
Response:
{
    "code": 200,
    "data": {
        "messages": [
            {
                "msg_id": "uuid-v4",
                "sender_uuid": "sender-uuid",
                "sender_name": "PlayerA",
                "content": "Hello!",
                "created_at": "2026-05-03T12:00:00Z"
            }
        ]
    }
}
```

### 6.4 服务器管理接口

#### 6.4.1 心跳上报
```
POST /api/v1/server/heartbeat
Headers: Authorization: Bearer {server_access_token}
Request:
{
    "online_count": 50,
    "player_list": ["uuid1", "uuid2", ...]
}
Response:
{
    "code": 200,
    "data": {
        "ok": true
    }
}
```

## 7. 项目目录结构

```
youni/
├── backend/                            # ① 中心后端 (Go)
│   ├── cmd/
│   │   └── server/
│   │       └── main.go
│   ├── internal/
│   │   ├── config/
│   │   │   └── config.go
│   │   ├── handler/
│   │   │   ├── auth.go                 # 认证处理
│   │   │   ├── discovery.go            # 路由发现处理
│   │   │   └── server.go               # 服务器管理处理
│   │   ├── middleware/
│   │   │   └── auth.go                 # JWT认证中间件
│   │   ├── model/
│   │   │   ├── player.go
│   │   │   ├── server.go
│   │   │   └── offline_message.go
│   │   ├── repository/
│   │   │   ├── player_repo.go
│   │   │   ├── server_repo.go
│   │   │   └── message_repo.go
│   │   ├── service/
│   │   │   ├── auth_service.go
│   │   │   ├── discovery_service.go
│   │   │   └── server_service.go
│   │   └── router/
│   │       └── router.go
│   ├── pkg/
│   │   ├── auth/
│   │   │   └── jwt.go
│   │   ├── database/
│   │   │   └── mysql.go
│   │   └── response/
│   │       └── response.go
│   ├── config.yaml                     # 配置文件
│   ├── go.mod
│   └── go.sum
│
├── relay/                              # ② 中转后端 (Go)
│   ├── cmd/
│   │   └── relay/
│   │       └── main.go
│   ├── internal/
│   │   ├── config/
│   │   │   └── config.go
│   │   ├── hub/
│   │   │   ├── hub.go                  # 连接管理中心
│   │   │   ├── client.go               # 单个服务器连接
│   │   │   └── message.go              # 消息路由转发
│   │   ├── handler/
│   │   │   └── ws.go                   # WebSocket处理
│   │   └── admin/
│   │       └── admin.go                # 管理接口(状态/统计)
│   ├── pkg/
│   │   └── protocol/
│   │       └── protocol.go             # 协议定义(与客户端共享)
│   ├── config.yaml
│   ├── go.mod
│   └── go.sum
│
├── common/                             # ③ 客户端通用Java库 (多平台共享)
│   ├── src/main/java/com/youni/common/
│   │   ├── api/                        # 中心后端API客户端
│   │   │   ├── YouniApiClient.java
│   │   │   ├── dto/
│   │   │   └── exception/
│   │   ├── transport/                  # 消息传输层(统一抽象)
│   │   │   ├── MessageTransport.java   # 传输接口
│   │   │   ├── TransportManager.java   # 传输管理器(选择P2P或Relay)
│   │   │   ├── p2p/                    # P2P直连实现
│   │   │   │   ├── P2PServer.java      # WebSocket服务端
│   │   │   │   ├── P2PClient.java      # WebSocket客户端
│   │   │   │   ├── P2PPool.java        # 连接池
│   │   │   │   └── P2PTransport.java   # P2P传输实现
│   │   │   ├── relay/                  # 中转模式实现
│   │   │   │   ├── RelayClient.java    # 中转客户端
│   │   │   │   └── RelayTransport.java # 中转传输实现
│   │   │   └── protocol/              # 通用协议
│   │   │       ├── Frame.java          # 消息帧
│   │   │       └── FrameType.java      # 消息类型枚举
│   │   ├── model/
│   │   │   ├── PlayerInfo.java
│   │   │   ├── ServerInfo.java
│   │   │   └── ChatMessage.java
│   │   ├── manager/
│   │   │   ├── AuthManager.java
│   │   │   └── MessageManager.java
│   │   └── config/
│   │       └── YouniConfig.java
│   └── build.gradle
│
├── plugin-paper/                       # Paper/Folia 插件
│   ├── src/main/java/com/youni/paper/
│   │   ├── YouniPaperPlugin.java
│   │   ├── command/
│   │   │   └── PaperCommandHandler.java
│   │   ├── listener/
│   │   │   └── PaperEventListener.java
│   │   └── platform/
│   │       └── PaperPlatform.java
│   ├── src/main/resources/
│   │   └── plugin.yml
│   └── build.gradle
│
├── mod-fabric/                         # Fabric Mod
│   ├── src/main/java/com/youni/fabric/
│   │   ├── YouniFabricMod.java
│   │   ├── command/
│   │   │   └── FabricCommandHandler.java
│   │   ├── event/
│   │   │   └── FabricEventListener.java
│   │   └── platform/
│   │       └── FabricPlatform.java
│   ├── src/main/resources/
│   │   ├── fabric.mod.json
│   │   └── youni.mixins.json
│   └── build.gradle
│
├── mod-forge/                          # Forge Mod
│   ├── src/main/java/com/youni/forge/
│   │   ├── YouniForgeMod.java
│   │   ├── command/
│   │   │   └── ForgeCommandHandler.java
│   │   ├── event/
│   │   │   └── ForgeEventListener.java
│   │   └── platform/
│   │       └── ForgePlatform.java
│   ├── src/main/resources/
│   │   └── META-INF/mods.toml
│   └── build.gradle
│
└── mod-neoforge/                       # NeoForge Mod
    ├── src/main/java/com/youni/neoforge/
    │   ├── YouniNeoForgeMod.java
    │   ├── command/
    │   │   └── NeoForgeCommandHandler.java
    │   ├── event/
    │   │   └── NeoForgeEventListener.java
    │   └── platform/
    │       └── NeoForgePlatform.java
    ├── src/main/resources/
    │   └── META-INF/mods.toml
    └── build.gradle
```

## 8. 开发计划

### 8.1 第一阶段：基础框架搭建（第1-2周）

| 任务 | 描述 | 优先级 |
|------|------|--------|
| 中心后端初始化 | 创建Go项目，搭建Gin框架 | P0 |
| 中转后端初始化 | 创建独立的relay Go项目 | P0 |
| 数据库设计 | MySQL表结构，GORM模型 | P0 |
| 配置管理 | YAML配置文件读取 | P1 |
| JWT框架 | 认证中间件和工具包 | P0 |
| 协议定义 | 定义通用消息帧格式(Java + Go) | P0 |

### 8.2 第二阶段：后端核心功能（第3-4周）

| 任务 | 描述 | 优先级 |
|------|------|--------|
| 服务器认证 | 服务器注册和认证接口 | P0 |
| 玩家系统 | 玩家注册、登录、登出 | P0 |
| 路由发现 | 玩家位置查询(含通信模式) | P0 |
| 离线消息 | 离线消息存储和拉取 | P0 |
| 心跳上报 | 服务器心跳和玩家同步 | P1 |

### 8.3 第三阶段：中转后端（第5周）

| 任务 | 描述 | 优先级 |
|------|------|--------|
| WebSocket服务 | 接受服务器连接和注册 | P0 |
| 连接管理 | server_id映射、心跳检测 | P0 |
| 消息转发 | 根据target_server转发消息 | P0 |
| 广播功能 | 向所有连接服务器广播 | P1 |
| 管理接口 | 状态查询和统计 | P1 |

### 8.4 第四阶段：通用Java库（第6-7周）

| 任务 | 描述 | 优先级 |
|------|------|--------|
| API客户端 | 与中心后端REST API通信 | P0 |
| Transport抽象 | 统一的传输接口定义 | P0 |
| P2P Transport | WebSocket服务端/客户端/连接池 | P0 |
| Relay Transport | 中转客户端 | P0 |
| TransportManager | 自动选择P2P或Relay | P0 |
| 消息管理 | 消息路由、发送、确认 | P0 |
| 认证管理 | 服务器认证和玩家登录流程 | P0 |

### 8.5 第五阶段：客户端插件/Mod（第8-10周）

| 任务 | 描述 | 优先级 |
|------|------|--------|
| Paper插件 | Paper/Folia插件，集成common库 | P0 |
| Fabric Mod | Fabric Mod，集成common库 | P0 |
| Forge Mod | Forge Mod，集成common库 | P0 |
| NeoForge Mod | NeoForge Mod，集成common库 | P1 |
| 命令系统 | /youni msg, /youni reply 等 | P0 |
| 事件监听 | 玩家加入/退出事件 | P0 |
| 消息显示 | 游戏内格式化显示 | P1 |

### 8.6 第六阶段：测试与优化（第11-12周）

| 任务 | 描述 | 优先级 |
|------|------|--------|
| 集成测试 | 多服务端 + 双模式联合测试 | P0 |
| 性能优化 | 连接池、API、中转吞吐量优化 | P1 |
| 异常处理 | 断线重连、超时、降级 | P0 |
| 安全审计 | Token、消息验证、防注入 | P1 |
| 部署文档 | 部署和接入文档 | P1 |
| Docker化 | 后端Docker部署方案 | P2 |
| 发布 | 发布后端和客户端 | P0 |

## 9. 技术要点

### 9.1 三套协议并行

| 协议 | 端点 | 用途 | 传输内容 |
|------|------|------|----------|
| REST API (HTTPS) | 插件/Mod ↔ 中心后端 | 认证、路由、离线消息 | 不含聊天内容 |
| P2P WebSocket | 游戏服务器 ↔ 游戏服务器 | 直连聊天 | 聊天消息 |
| Relay WebSocket | 游戏服务器 ↔ 中转后端 | 中转聊天 | 聊天消息 |

### 9.2 Transport 统一抽象

```java
public interface MessageTransport {
    void send(String targetServerId, ChatMessage message);
    void onMessage(MessageHandler handler);
    void connect();
    void disconnect();
    boolean isConnected(String targetServerId);
    String getType(); // "p2p" or "relay"
}
```

插件/Mod代码只需调用 `MessageTransport` 接口，不需要关心底层是P2P还是中转。

### 9.3 安全设计

1. **中心后端**：Server ID + Secret → JWT Token
2. **P2P认证**：JWT握手验证对方身份
3. **中转认证**：连接时可选JWT验证
4. **传输加密**：生产环境HTTPS + WSS
5. **防滥用**：速率限制、消息频率控制

### 9.4 兼容性设计

1. **平台抽象层**：Platform接口抽象不同服务端差异
2. **通用Java库**：common模块封装业务逻辑，各平台只做适配
3. **统一协议**：所有服务端相同的协议格式
4. **版本兼容**：目标 Minecraft 1.16+

### 9.5 高可用设计

1. **自动降级**：P2P失败自动切换到Relay（auto模式）
2. **自动重连**：断线后指数退避重连
3. **离线消息**：目标不在线时暂存，上线后拉取
4. **ACK确认**：消息投递确认，失败回退离线消息
5. **中转水平扩展**：Relay可部署多个实例

## 10. 风险评估

| 风险 | 影响 | 概率 | 应对措施 |
|------|------|------|----------|
| 服务器NAT无法直连 | 高 | 高 | Relay中转模式兜底 |
| 中转后端单点故障 | 高 | 中 | 多实例部署 + P2P回退 |
| 服务端API版本变更 | 高 | 中 | 适配层隔离变更影响 |
| WebSocket连接稳定性 | 中 | 中 | 心跳 + 自动重连 + 离线消息 |
| 消息丢失 | 高 | 低 | ACK确认 + 离线消息暂存 |
| 安全漏洞 | 高 | 低 | 定期审计，安全最佳实践 |

## 11. 资源需求

### 11.1 开发环境
- Go 1.21+
- JDK 17+
- MySQL 8.0+
- Gradle 8.0+
- Git

### 11.2 测试环境
- Paper 1.20+ / Folia 1.20+
- Fabric 1.20+
- Forge 1.20+ / NeoForge 1.20+

### 11.3 生产环境

| 服务 | 最低配置 | 说明 |
|------|----------|------|
| 中心后端 | 1核2G | 仅认证和路由，轻量级 |
| 中转后端 | 1核1G | 纯内存转发，无数据库 |
| MySQL | 共享 | 与中心后端同机或独立 |

## 12. 总结

Youni 项目采用**中心认证 + 双模消息传输**架构：

**三个独立服务：**
- **中心后端 (Go)**：信令服务器，负责认证授权、路由发现、离线消息，**不传输聊天消息**
- **中转后端 (Go)**：消息中继，为无法直连的服务器转发消息，**不访问数据库**
- **游戏服务端插件/Mod**：游戏内交互，支持P2P和中转两种模式

**两种消息传输模式：**
- **P2P直连**：服务器间WebSocket直连，组成内部Mesh网络，延迟最低
- **中转模式**：通过中转后端转发，解决NAT/防火墙无法直连问题
- **自动降级**：优先P2P，失败自动切换中转

这种架构兼具灵活性和可靠性：
- 可直连的服务器享受最低延迟的P2P通信
- 无法直连的服务器通过中转后端也能正常通信
- 中心后端不接触聊天内容，保护玩家隐私

---

**项目创建日期**：2026-05-03
**文档版本**：v3.0
**作者**：Youni Team