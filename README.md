# 校园失物招领智能匹配系统

面向高校师生的失物招领平台，支持 Web 端与微信公众号双通道使用。用户可发布失物/寻物信息，系统基于关键词、区域、时间等多维度自动匹配，并提供站内留言沟通与数据统计可视化。

**BISTU失物招领处**

---

## 功能概览

| 模块 | 说明 |
|------|------|
| 用户管理 | 注册、登录、资料修改、改密码；角色分为普通用户 / 管理员 / 超级管理员 |
| 信息发布 | 发布失物（LOST）/ 寻物（FOUND）帖，上传图片、填写分类/时间/地点/关键词，管理帖子状态 |
| 智能匹配 | 发帖后自动异步匹配，按相似度排序，Top 3 推送通知 |
| 站内沟通 | 失物帖与寻物帖建立会话，文字留言；公开页隐藏联系方式，会话内可见 |
| 数据统计 | 发布量、找回率、分类/区域分布、趋势图（ECharts） |
| 微信公众号 | 注册、绑定、全站找帖、我的帖子、管理员快速发布 |
| 管理后台 | 用户封禁、帖子审核关闭、批量重跑匹配、审计日志 |

---

## 技术栈

**后端**

- Java 17 · Spring Boot 3.5 · Spring Security · JWT
- MyBatis-Plus · MySQL 8 · Redis
- Gradle

**前端**

- Vue 3 · Vite · Element Plus · Pinia · Vue Router · ECharts · Axios

---

## 项目结构

```
Lost-Found/
├── src/main/java/com/lostfound/   # 后端源码
│   ├── controller/                # REST 接口
│   ├── service/                   # 业务逻辑
│   ├── entity/                    # 数据库实体
│   ├── config/                    # 安全、Redis、MyBatis 等配置
│   └── ...
├── src/main/resources/
│   └── application.yml            # 应用配置
├── frontend/                      # Vue 前端
│   └── src/
│       ├── views/                 # 页面
│       ├── api/                   # 接口封装
│       └── router/                # 路由
├── LF.sql                         # 数据库建表与初始数据
├── uploads/                       # 图片上传目录
└── 开发文档/                      # 任务书等文档
```

---

## 环境要求

- JDK 17+
- Node.js 18+（前端开发）
- MySQL 8.0+
- Redis 6+
- Windows / Linux / macOS

---

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < LF.sql
```

脚本会创建 `lost_found` 库及全部表，并插入初始管理员账号。

### 2. 修改配置

编辑 `src/main/resources/application.yml`，至少确认以下项：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/lost_found?...
    username: root
    password: 你的密码
  data:
    redis:
      host: localhost
      port: 6379

wx:
  appid: 你的公众号 AppID
  secret: 你的公众号 AppSecret
  token:  服务器配置中的 Token

jwt:
  secret: 生产环境请更换为足够长的随机字符串
```

### 3. 启动后端

```bash
# Windows
.\gradlew.bat bootRun

# Linux / macOS
./gradlew bootRun
```

后端默认运行在 `http://localhost:8080`。

### 4. 启动前端

```bash
cd frontend
npm install
npm run dev
```

前端默认运行在 `http://localhost:5173`，开发模式下通过 Vite 代理转发 `/api` 与 `/uploads` 到后端。

### 5. 默认账号

| 账号 | 密码 | 角色 |
|------|------|------|
| `admin` | `admin` | 超级管理员 |

首次登录后建议立即修改密码。

---

## 核心业务流程

```
发布失物/寻物帖 → 自动智能匹配 → 查看匹配结果 → 建立会话沟通 → 标记已找回/关闭
```

**帖子状态：** `OPEN`（进行中）→ `MATCHED`（已匹配）→ `RESOLVED`（已找回）/ `CLOSED`（已关闭）

**匹配算法（权重）：**

| 维度 | 权重 | 规则 |
|------|------|------|
| 分类一致 | 0.30 | 失物帖与寻物帖分类相同 |
| 关键词 | 最高 0.40 | Jaccard 相似度 |
| 区域 | 0.20 | `area_code` 相同 |
| 时间 | 0.10 / 0.05 | 遗失/拾获时间差 ≤24h / ≤72h |

最低匹配分默认 `0.30`，可在 `application.yml` 的 `match.min-score` 调整。

---

## 微信公众号配置

1. 登录 [微信公众平台](https://mp.weixin.qq.com/)，在「开发 → 基本配置」中设置 **Token**（与 `wx.token` 一致）。
2. 将服务器 URL 配置为：`https://你的域名/api/wx/callback`。
3. 确保服务器可被微信访问，且 Redis 正常运行（用于多步对话会话与 access_token 缓存）。

**支持的指令：**

| 指令 | 说明 |
|------|------|
| 注册 | 创建网站账号并绑定微信 |
| 绑定 | 关联已有网站账号 |
| 我的帖子 | 查看进行中的帖子 |
| 全站找帖 / 查找 / 搜索 | 按关键词、区域、时间搜索相似帖子 |
| 发布 | 管理员快速发布（11 步向导，支持发图） |
| 取消 | 退出当前操作流程 |

---

## 主要 API

所有接口返回统一格式 `{ "code": 200, "message": "...", "data": ... }`。

**公开接口（无需登录）**

- `POST /api/auth/register` · `POST /api/auth/login` · `POST /api/auth/wx-login`
- `GET /api/posts` · `GET /api/posts/{id}`
- `GET /api/stats/overview` · `/category` · `/area` · `/trend`

**登录后**

- 帖子：`POST/PUT/DELETE /api/posts` · `PUT /api/posts/{id}/resolve`
- 匹配：`GET /api/posts/{id}/matches` · `POST /api/posts/{id}/rematch`
- 会话：`POST/GET /api/threads` · `POST /api/threads/{id}/messages`
- 通知：`GET /api/notifications` · `PUT /api/notifications/read-all`
- 上传：`POST /api/upload/image`
- 用户：`GET/PUT /api/user/me` · `PUT /api/user/change-password`

**管理员（`/api/admin/**`）**

- 用户管理、帖子强制关闭、批量重跑匹配、用户统计、审计日志

---

## 前端页面

| 路径 | 页面 |
|------|------|
| `/home` | 首页 |
| `/posts` | 帖子广场 |
| `/posts/create` | 发布信息 |
| `/my/posts` | 我的帖子 |
| `/my/posts/:id/matches` | 匹配结果 |
| `/threads` | 我的会话 |
| `/notifications` | 通知中心 |
| `/stats` | 数据统计 |
| `/profile` | 个人中心 |
| `/admin/*` | 管理后台 |

---

## 生产部署建议

1. **前端构建：** 在 `frontend` 目录执行 `npm run build`，将 `dist/` 部署到 Nginx 等静态服务器。
2. **反向代理：** 将 `/api` 和 `/uploads` 转发到 Spring Boot 服务（8080 端口）。
3. **图片访问：** 若前后端不同域，配置 `app.public-base-url` 为后端公网地址，确保图片 URL 可正常访问。
4. **敏感配置：** 生产环境请通过环境变量或独立配置文件管理数据库密码、JWT Secret、微信 AppSecret，勿提交到版本库。
5. **上传目录：** 确保 `uploads/` 目录有写权限，并做好备份。

---

## 开发说明

```bash
# 编译后端
./gradlew compileJava

# 运行测试
./gradlew test

# 前端生产构建
cd frontend && npm run build
```

---

## 许可证

见 [LICENSE](LICENSE) 文件。
