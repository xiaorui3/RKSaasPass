# RK-Web 社团管理平台

RK-Web 是一个面向高校社团、学生组织和校园活动运营的多端管理平台。项目包含 Spring Cloud 微服务后端、Vue 3 Web 前端、Android 客户端，以及用于远程部署的 CLI/GUI 安装器。

![RK-Web 首页界面](docs/assets/rk-web-homepage-screenshot.png)

## 项目能力

- 社团门户：首页推荐、社团公告、推荐社团、快捷入口、个人中心概览。
- 社团与成员：社团资料、成员管理、加入申请、租户隔离、用户角色与权限。
- 活动与赛事：活动发布、活动报名、赛事管理、作品提交、审核与结果展示。
- 内容聚合：新闻动态、作品展示、通知公告、富文本内容与媒体资源管理。
- 运营后台：租户、用户、菜单、日志、配置、发布、备份、迁移和运维任务管理。
- 文件与媒体：MinIO 本地对象存储优先，同时保留云对象存储扩展配置。
- 消息通知：站内通知、短信、邮件等可选集成能力。
- 扩展业务：支付、交易、搜索、题库考试、数据看板等模块化服务。
- 远程安装：提供命令行安装器和桌面 GUI 安装器，支持 Docker Compose 与 K3s 部署模式。

## 界面与安装器

![RK-Web 一键安装器海报](docs/assets/rk-web-one-click-installer-poster.png)

![RK-Web 开源商用海报](docs/assets/rk-web-open-source-commercial-poster.png)

## 技术栈

| 层级 | 技术 |
| --- | --- |
| 后端 | Java 11、Spring Boot、Spring Cloud、Spring Cloud Alibaba、MyBatis Plus |
| 前端 | Vue 3、Vite、Pinia、Vue Router、Element Plus、Axios |
| 移动端 | Android Java |
| 中间件 | MySQL 8、Nacos 2.x、Redis、RabbitMQ、MinIO、XXL-Job、Seata |
| 部署 | Docker Compose、K3s、远程 SSH 安装器、Windows CLI/GUI 安装器 |

## 目录结构

```text
RK-Web/
├─ rk-auth/                    # 认证服务
├─ rk-user/                    # 用户、租户、后台运营与运维服务
├─ rk-gateway/                 # API 网关
├─ rk-content/                 # 内容服务
├─ rk-activity/                # 活动与赛事服务
├─ rk-message/                 # 消息、短信、邮件服务
├─ rk-file/                    # 文件与媒体服务
├─ rk-pay/                     # 支付服务
├─ rk-search/                  # 搜索服务
├─ rk-trade/                   # 交易服务
├─ rk-exam/                    # 考试服务
├─ rk-data/                    # 数据看板服务
├─ rk-common/                  # 公共组件
├─ rk-api/                     # Feign API 与共享 DTO
├─ newpro/rk/                  # Vue 3 Web 前端
├─ android-client/rk-club-android/
│  └─                          # Android 客户端
├─ installer/remote-installer/ # CLI/GUI 远程安装器源码
├─ docs/assets/                # 文档图片资源
└─ OPEN_SOURCE_CONFIGURATION.md# 配置与部署指南
```

## 环境要求

- JDK 11
- Maven 3.6+
- Node.js 18+
- MySQL 8
- Nacos 2.x
- Redis
- RabbitMQ
- MinIO
- 可选：Elasticsearch、Seata、XXL-Job、短信服务、邮件服务、支付服务、云对象存储

详细配置项见 [OPEN_SOURCE_CONFIGURATION.md](OPEN_SOURCE_CONFIGURATION.md)。

## 快速启动前端

```bash
cd newpro/rk
npm install
npm run dev
```

开发服务器默认运行在 `http://localhost:5173`。开发代理默认指向 `http://127.0.0.1:10010`，可通过 `VITE_PROXY_TARGET` 或 `VITE_GATEWAY_BASE_URL` 修改。

## 启动后端服务

后端服务通过 `bootstrap*.yml` 读取 Nacos 地址、命名空间和本地环境变量。启动前需要先准备 MySQL、Nacos、Redis、RabbitMQ 和 MinIO。

常用本地变量示例：

```bash
NACOS_SERVER_ADDR=127.0.0.1:8848
NACOS_NAMESPACE=public
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
```

可以在 IDE 中分别启动各微服务，也可以在根目录使用 Maven 构建：

```bash
mvn -DskipTests package
```

当前仓库没有内置 Maven Wrapper，未安装 Maven 的环境需要先安装 Maven。

## 远程安装器

安装器位于 `installer/remote-installer`，用于把 RK-Web 部署到目标 Linux 主机。

```bash
cd installer/remote-installer
npm install
node src/index.mjs wizard
node src/index.mjs preflight --config examples/rk-remote-install.example.json
node src/index.mjs install --config rk-remote-install.json
npm run gui
```

打包命令：

```bash
npm run build:win
```

本地发行附件建议放在 `release-assets/v0.1.0/`，作为 GitHub Release 附件上传，不提交到 Git 仓库：

```text
release-assets/v0.1.0/rk-web-installer-cli-v0.1.0.exe
release-assets/v0.1.0/rk-web-installer-gui-v0.1.0.exe
```

## Android 客户端

```powershell
powershell -ExecutionPolicy Bypass -File android-client\rk-club-android\build-apk.ps1
```

默认输出：

```text
android-client/rk-club-android/dist/rk-club-debug.apk
```

## 配置与安全

- 所有真实域名、服务器地址、数据库密码、Redis/RabbitMQ 密码、对象存储密钥、短信密钥、邮件授权码、支付私钥、镜像仓库 Token、SSH 凭据都应由部署环境提供。
- 不要提交 `.env.local`、真实安装器配置、私钥文件、日志、数据库导出、Nacos 导出、构建产物和本地发行附件。
- GitHub 发布前建议开启 Secret scanning、Push protection 和 Dependabot alerts。
- 公开发布前请补充或确认 `LICENSE` 文件，以明确项目授权方式。

## 文档

- [配置与部署指南](OPEN_SOURCE_CONFIGURATION.md)
- [远程安装器说明](installer/remote-installer/README.md)
- [前端项目说明](newpro/rk/README.md)
- [Android 客户端说明](android-client/rk-club-android/README.md)
