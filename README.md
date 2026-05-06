youni是mc中幽匿感测体的拼音，它可以检测外界不同物体的声音并发出红石信号。
我们的项目也一样，我们是一个跨端跨版本跨服的授权登录系统，让玩家在我们的平台注册后可以在安装我们插件/mod的服务器上直接登录，并且安装我们插件/mod的服务器可以互相以p2p或者连接中转服务器的形式将多方信息组成小的社交网络，为玩家之间带来更好的社交体验
目前计划开发fabric+（neo）forge+paper（原版ui+龙核+ia+萌芽+ce+云拾+ax+南天+paiui（html））+web，并支持1.12.2forge，1.18forge/fabric，1.20.1fabric/（neo）forge，1.20.4fabric/（neo）forge，1.21fabric/forge，1.21.4fabric/（neo）froge，1.21.8fabric，1.21.11fabric和paper/folia
<img width="808" height="294" alt="c478180f-efb6-4502-87fd-fcbc581b71a6" src="https://github.com/user-attachments/assets/80319dad-856b-4e4b-b957-2d6cf8f17433" />

项目允许其他网站直接使用，但请在首页保留youni字样（可以不起眼位置），当然这不是最重要的，
最重要的是给我保留一下silentqaq这个id吧


下面是详细内容
# Youni - 跨服务器社交系统

**适用于 Minecraft 的现代社区解决方案**

***

## 目录

1. [项目简介](#项目简介)
2. [技术栈](#技术栈)
3. [功能特性](#功能特性)
4. [快速开始](#快速开始)
5. [API 文档](#api-文档)
6. [开发指南](#开发指南)
7. [插件端文档](#插件端文档)
8. [常见问题](#常见问题)

***

## 项目简介

**Youni** 是一个面向 Minecraft 多服网络的现代社交与皮肤管理平台，提供：

- 统一的账号系统，支持 QQ 绑定
- 完整的皮肤管理与预览（含 3D 视角）
- 皮肤库分享系统（原创/转载标记）
- 多角色系统（每个账号可创建多名角色）
- 跨服好友、聊天大厅
- Yggdrasil 第三方认证服务（兼容 PCL2、HMCL 等）
- 考核中心与技能认证
- 服务器白名单与封禁管理

***

## 技术栈

### 后端

- **语言**：Go 1.20+
- **Web 框架**：Gin
- **ORM**：GORM
- **数据库**：MySQL 8.0+
- **认证**：JWT + OAuth 2.0
- **3D 渲染**：Three.js

### 前端

- **框架**：原生 HTML/CSS/JS + Vue 3
- **3D 渲染**：Three.js
- **风格**：深色主题

### 插件端

- **服务端**：Paper (1.19+ / 1.20+)
- **语言**：Java 17+

***

## 功能特性

### 1. 账号系统

- 用户注册（邮箱/验证码或 QQ 登录）
- 密码与邮箱绑定
- QQ 等级同步与展示
- OAuth 2.0 支持

### 2. 皮肤系统

- 皮肤上传（支持 64x32 旧格式自动升级为 64x64）
- 经典/纤细 (Steve/Alex) 模型切换
- 个人衣柜（最多 8 个皮肤）
- 激活皮肤设置
- 3D 实时预览（可旋转）
- 3D 皮肤层渲染（帽子/外套/袖子/裤子）

### 3. 皮肤库系统

- 皮肤分享社区
- 搜索/分页浏览
- 双视角预览（正面/背面）
- 原创/转载标记
- 下载计数
- 一键添加到个人衣柜
- 上传者管理权限

### 4. 角色系统

- 每个账号最多创建多个角色（含默认角色）
- 角色名与角色 UUID 自定义
- 每个角色可绑定独立皮肤
- Yggdrasil 支持角色切换与皮肤加载

### 5. Yggdrasil 认证

- 完整的 Yggdrasil API 实现
- 兼容 PCL2、HMCL 等主流第三方启动器
- 支持角色皮肤、披风加载
- 认证服务器配置：`http://your-domain/yggdrasil/`

### 6. 社交系统

- 好友列表与申请
- 亲密度与师徒/伴侣关系
- 跨服聊天大厅
- 离线消息

### 7. 考核中心

- 技能领域（如：建筑、命令方块、PVP 等）
- 题目与题库管理
- 在线答题与等级评定
- 展示设置（选择公开的领域）
- 管理员审核题目

### 8. 服务器管理

- 服务器绑定与白名单
- 封禁系统
- 服务器设置管理
- 玩家搜索

***

## 快速开始

### 环境要求

| 组件           | 版本要求         |
| ------------ | ------------ |
| Go           | 1.20+        |
| MySQL        | 8.0+         |
| Node.js (可选) | 18+ (用于前端开发) |

### 1. 安装与配置

1. **克隆项目**
   ```bash
   git clone <repo-url>
   cd youni/backend
   ```
2. **配置文件**
   复制示例配置：
   ```bash
   cp config.example.yaml config.yaml
   ```
   编辑 `config.yaml`，设置数据库连接与其他参数。
3. **初始化数据库**
   首次运行会自动执行数据库迁移。
4. **编译与运行**
   ```bash
   go build -o youni.exe ./cmd/server
   ./youni.exe
   ```
5. **Web 访问**
   - 首页：`http://localhost:8080/`
   - 登录：`http://localhost:8080/login`
   - 个人主页：`http://localhost:8080/dashboard`
   - 皮肤库：`http://localhost:8080/gallery`
   - 考核中心：`http://localhost:8080/exam`

### 2. 使用 PCL2 登录

1. 在 PCL2 中选择"外置登录"
2. 输入认证服务器地址：`http://localhost:8080/yggdrasil/`
3. 使用你在网站上注册的账号密码登录
4. 即可看到所有角色并选择要使用的角色

***

## API 文档

### 公开端点

#### 皮肤库

| 端点                     | 方法  | 说明                                     |
| ---------------------- | --- | -------------------------------------- |
| `/api/web/gallery`     | GET | 获取皮肤列表（支持 `page`, `page_size`, `q` 参数） |
| `/api/web/gallery/:id` | GET | 获取单个皮肤详情                               |

#### 认证

| 端点                   | 方法   | 说明    |
| -------------------- | ---- | ----- |
| `/api/web/register`  | POST | 用户注册  |
| `/api/web/login`     | POST | 用户登录  |
| `/api/web/send-code` | POST | 发送验证码 |

#### Yggdrasil

| 端点                                                         | 说明       |
| ---------------------------------------------------------- | -------- |
| `/yggdrasil/`                                              | 元数据      |
| `/yggdrasil/authserver/authenticate`                       | 登录认证     |
| `/yggdrasil/authserver/refresh`                            | Token 刷新 |
| `/yggdrasil/sessionserver/session/minecraft/join`          | 游戏服加入    |
| `/yggdrasil/sessionserver/session/minecraft/hasJoined`     | 游戏服验证    |
| `/yggdrasil/sessionserver/session/minecraft/profile/:uuid` | 获取皮肤     |
| `/yggdrasil/textures/:hash`                                | 纹理文件下载   |

### 需认证端点

#### 用户资料

| 端点                        | 方法   | 说明       |
| ------------------------- | ---- | -------- |
| `/api/web/profile`        | GET  | 获取当前用户资料 |
| `/api/web/profile/update` | POST | 更新资料     |

#### 皮肤管理

| 端点                            | 方法         | 说明     |
| ----------------------------- | ---------- | ------ |
| `/api/web/skins`              | GET        | 获取皮肤列表 |
| `/api/web/skins/upload`       | POST       | 上传皮肤   |
| `/api/web/skins/:id/activate` | PUT        | 激活皮肤   |
| `/api/web/skins/:id`          | PUT/DELETE | 更新/删除  |

#### 角色管理

| 端点                        | 方法         | 说明      |
| ------------------------- | ---------- | ------- |
| `/api/web/characters`     | GET/POST   | 列出/创建角色 |
| `/api/web/characters/:id` | PUT/DELETE | 更新/删除   |

#### 皮肤库管理

| 端点                              | 方法     | 说明        |
| ------------------------------- | ------ | --------- |
| `/api/web/gallery/upload`       | POST   | 从衣柜上传到皮肤库 |
| `/api/web/gallery/:id/wardrobe` | POST   | 添加到衣柜     |
| `/api/web/gallery/mine`         | GET    | 我上传的皮肤    |
| `/api/web/gallery/:id`          | DELETE | 删除（仅上传者）  |

***

## 开发指南

### 项目结构

```
youni/
├── backend/
│   ├── cmd/
│   │   └── server/              # 主入口
│   ├── internal/
│   │   ├── model/               # 数据模型
│   │   ├── repository/          # 数据访问层
│   │   ├── handler/             # API 处理器
│   │   ├── service/             # 业务逻辑
│   │   └── middleware/          # 中间件
│   ├── pkg/
│   │   ├── config/              # 配置
│   │   ├── response/            # 响应封装
│   │   └── database/            # 数据库初始化
│   ├── web/                     # 前端静态文件
│   └── config.yaml              # 配置
└── plugin-paper/                # 服务端插件
```

### 添加新功能

1. 在 `internal/model/` 添加数据结构
2. 在 `internal/repository/` 添加数据访问
3. 在 `internal/handler/` 添加 API 处理
4. 在 `internal/router/router.go` 注册路由
5. 在 `web/` 添加前端页面（如有需要）

### 数据库迁移

GORM 自动迁移已配置，修改模型后重启服务器即可。

***

## 插件端文档

### 功能

- 聊天菜单（GUI）
- 好友列表
- 在线状态同步
- 跨服消息

### 构建

```bash
cd plugin-paper
mvn clean package
```

生成的 jar 文件位于 `target/`。

### 配置

在 `plugins/Youni/config.yml` 中填写后端 API 地址与密钥。

***

## 常见问题

### Q: 登录时提示身份验证失败？

A: 请确保：

1. PCL2 中的认证服务器地址是 `http://your-domain/yggdrasil/`（注意末尾斜杠）
2. 已在网站上注册账号并验证邮箱

### Q: 皮肤不显示？

A: 检查：

1. 皮肤文件是否为 64x32 或 64x64 的 PNG
2. 在个人主页中已激活该皮肤
3. 如果使用角色，检查角色是否绑定了皮肤

### Q: 如何修改初始管理员账号？

A: 在 `pkg/database/mysql.go` 中修改默认初始化逻辑，或直接在数据库 `players` 表中设置 `is_admin = 1`。

***

## 许可证

本项目仅供学习与交流使用。
