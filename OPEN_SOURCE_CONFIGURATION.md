# RK-Web 配置与部署指南

本文档说明 RK-Web 当前功能所需的运行环境、配置项、远程安装参数和 GitHub Release 资产准备方式。所有真实凭据都应由部署环境、密钥管理系统或本地未跟踪配置文件提供，不应写入 Git 仓库。

![RK-Web 首页界面](docs/assets/rk-web-homepage-screenshot.png)

## 功能范围

RK-Web 当前包含以下核心能力：

- 多租户社团门户：社团首页、推荐社团、社团公告、快捷入口、个人中心。
- 组织与成员管理：社团资料、成员身份、加入申请、权限角色、租户隔离。
- 活动与赛事管理：活动发布、报名、赛事流程、作品提交、审核和结果展示。
- 内容与媒体管理：新闻、公告、作品、富文本内容、图片和附件。
- 后台运营中心：租户、用户、菜单、配置、日志、任务、发布、备份和迁移。
- 消息能力：站内通知、短信、邮件等可选通道。
- 文件存储：默认使用 MinIO，可按需接入云对象存储。
- 扩展服务：搜索、支付、交易、考试、数据看板等模块化业务。
- 部署工具：CLI/GUI 远程安装器，支持 Docker Compose 与 K3s 模式。

![RK-Web 一键安装器海报](docs/assets/rk-web-one-click-installer-poster.png)

![RK-Web 开源商用海报](docs/assets/rk-web-open-source-commercial-poster.png)

## 配置原则

1. 仓库只保留示例值、占位符和本地开发默认值。
2. 真实密钥、真实服务器地址、真实数据库连接、真实 Token 和私钥只放在运行环境中。
3. 本地开发优先使用 `127.0.0.1`、`localhost`、`example.com` 或 RFC 文档示例网段。
4. 远程安装器配置文件应放在 Git 忽略目录或仓库外部路径。
5. GitHub Release 附件不提交到 Git 仓库，只在 Release 页面上传。

## 基础环境

| 组件 | 建议版本 | 用途 |
| --- | --- | --- |
| JDK | 11 | 后端微服务运行与构建 |
| Maven | 3.6+ | 后端构建 |
| Node.js | 18+ | 前端与安装器构建 |
| MySQL | 8.x | 业务数据库、Nacos、Seata、XXL-Job |
| Nacos | 2.x | 服务注册与配置中心 |
| Redis | 6+ / 7+ | 缓存、会话、验证码、任务状态 |
| RabbitMQ | 3.13+ | 消息队列，需支持 delayed message exchange |
| MinIO | 2024+ | 本地对象存储 |
| Docker | 24+ | 容器化部署 |
| K3s | 稳定版 | 可选 Kubernetes 部署模式 |

## 后端服务端口

| 服务 | 默认端口 | 说明 |
| --- | ---: | --- |
| `rk-gateway` | 10010 | API 网关 |
| `rk-auth` | 8081 | 认证、账号、角色、权限 |
| `rk-user` | 8082 | 用户、租户、后台运营、运维中心 |
| `rk-search` | 8083 | 搜索服务 |
| `rk-file` | 8084 | 文件与媒体服务 |
| `rk-message` | 8085 | 通知、短信、邮件服务 |
| `rk-content` | 8086 | 新闻、作品、内容服务 |
| `rk-pay` | 8087 | 支付集成 |
| `rk-trade` | 8088 | 订单交易 |
| `rk-exam` | 8089 | 题库考试 |
| `rk-activity` | 8090 | 活动与赛事 |
| `rk-data` | 8093 | 数据看板 |

## 通用运行变量

| 变量 | 必填 | 说明 | 示例 |
| --- | --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | 否 | Spring 运行环境 | `dev` |
| `NACOS_SERVER_ADDR` | 是 | Nacos 地址 | `127.0.0.1:8848` |
| `NACOS_NAMESPACE` | 是 | Nacos 命名空间 | `public` |
| `NACOS_GROUP` | 否 | Nacos 配置分组 | `DEFAULT_GROUP` |
| `RK_PUBLIC_BASE_URL` | 部署时是 | 平台公网入口 | `https://example.com` |
| `RK_WEB_PUBLIC_ORIGIN` | 部署时是 | 网关允许的前端来源 | `https://example.com` |
| `TZ` | 否 | 运行时区 | `Asia/Shanghai` |

各微服务的 `bootstrap*.yml` 默认读取：

```text
${NACOS_SERVER_ADDR:127.0.0.1:8848}
${NACOS_NAMESPACE:public}
```

## 数据库配置

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `MYSQL_HOST` | 是 | MySQL 主机 |
| `MYSQL_PORT` | 是 | MySQL 端口，默认 `3306` |
| `RK_MYSQL_APP_USER` | 是 | RK-Web 应用数据库账号 |
| `RK_MYSQL_APP_PASSWORD` | 是 | RK-Web 应用数据库密码 |
| `RK_MYSQL_ROOT_PASSWORD` | 安装器部署时是 | 初始化数据库和授权时使用 |
| `RK_AUTH_DB_USERNAME` | 本地单服务调试时是 | `rk_auth` 数据库账号 |
| `RK_AUTH_DB_PASSWORD` | 本地单服务调试时是 | `rk_auth` 数据库密码 |
| `TEST_DB_PASSWORD` | 测试时是 | 测试数据库密码，示例默认值为 `change-me` |

建议数据库名：

```text
rk_auth
rk_user
rk_content
rk_activity
rk_message
rk_file
rk_search
rk_data
rk_pay
rk_trade
rk_exam
nacos
seata
xxl_job
```

## Nacos 配置

部署时需要在 Nacos 中准备共享配置。常见配置项如下：

| 配置文件 | 内容 |
| --- | --- |
| `rk-shared-spring.yaml` | Spring 通用配置 |
| `rk-shared-redis.yaml` | Redis 地址、密码、连接池 |
| `rk-shared-rabbitmq.yaml` | RabbitMQ 地址、账号、vhost |
| `rk-shared-logs.yaml` | 日志级别与输出策略 |
| `rk-shared-mybatis.yaml` | MyBatis Plus 配置 |
| `rk-shared-feign.yaml` | OpenFeign 配置 |
| `rk-shared-seata.yaml` | Seata 配置 |
| `rk-shared-minio.yaml` | MinIO endpoint、bucket、publicBaseUrl |
| `rk-shared-xxljob.yaml` | XXL-Job admin 地址与执行器配置 |

Nacos 中不得写入公开仓库不应包含的真实密钥模板。生产部署建议通过密钥管理、Kubernetes Secret、容器环境变量或受控配置中心注入。

## Redis 与 RabbitMQ

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `RK_REDIS_HOST` | 是 | Redis 主机 |
| `RK_REDIS_PORT` | 是 | Redis 端口 |
| `RK_REDIS_PASSWORD` | 部署时是 | Redis 密码 |
| `RK_REDIS_DATABASE` | 否 | Redis 数据库编号 |
| `RK_RABBITMQ_HOST` | 是 | RabbitMQ 主机 |
| `RK_RABBITMQ_PORT` | 是 | RabbitMQ 端口 |
| `RK_RABBITMQ_USERNAME` | 是 | RabbitMQ 用户名 |
| `RK_RABBITMQ_PASSWORD` | 是 | RabbitMQ 密码 |
| `RK_RABBITMQ_VHOST` | 否 | RabbitMQ vhost |

交易等延迟消息场景需要 RabbitMQ delayed message exchange 插件。

## 文件与对象存储

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `RK_FILE_PLATFORM` | 否 | 文件平台，默认 `MINIO` |
| `RK_MEDIA_PLATFORM` | 否 | 媒体平台，默认 `MINIO` |
| `MINIO_ENDPOINT` | 是 | MinIO 内部访问地址 |
| `MINIO_ACCESS_KEY` | 是 | MinIO access key |
| `MINIO_SECRET_KEY` | 是 | MinIO secret key |
| `MINIO_BUCKET` | 是 | 默认 bucket |
| `MINIO_PUBLIC_BASE_URL` | 部署时是 | 对外访问文件的 URL 前缀 |

公网访问地址应配置为业务域名或网关代理地址，不建议直接暴露内部对象存储地址。

## 前端配置

前端位于 `newpro/rk`。

| 变量 | 必填 | 说明 | 默认值 |
| --- | --- | --- | --- |
| `VITE_PROXY_TARGET` | 开发时可选 | Vite 开发代理目标网关 | `http://127.0.0.1:10010` |
| `VITE_GATEWAY_BASE_URL` | 部署时可选 | 前端直连网关地址 | 空 |
| `VITE_MINIO_PROXY_TARGET` | 开发时可选 | Vite MinIO 代理目标 | `http://127.0.0.1:9000` |
| `VITE_MINIO_BASE_URL` | 部署时可选 | 前端访问文件的基础 URL | 空 |
| `VITE_DEFAULT_TENANT_ID` | 可选 | 默认租户 ID | 空 |

开发启动：

```bash
cd newpro/rk
npm install
npm run dev
```

生产构建：

```bash
npm run build
```

## Android 客户端配置

Android 客户端位于 `android-client/rk-club-android`。默认服务地址应替换为部署后的网关地址。

```powershell
powershell -ExecutionPolicy Bypass -File android-client\rk-club-android\build-apk.ps1
```

如需正式发布 APK，请在本地或 CI 的安全变量中配置签名文件与签名密码，不要提交 keystore、签名配置或打包后的私有 APK。

## 短信、邮件与云服务

以下能力均为可选集成。未启用时保持为空即可。

| 变量 | 说明 |
| --- | --- |
| `ALIYUN_SMS_ACCESS_ID` | 阿里云短信 AccessId |
| `ALIYUN_SMS_ACCESS_SECRET` | 阿里云短信 AccessSecret |
| `TEST_MAIL_HOST` | 测试邮件服务器 |
| `TEST_MAIL_USERNAME` | 测试邮件账号 |
| `TEST_MAIL_PASSWORD` | 测试邮件授权码或密码 |
| `TENCENT_ENABLE` | 是否启用腾讯云 COS/VOD |
| `TENCENT_CLOUD_APP_ID` | 腾讯云 AppId |
| `TENCENT_CLOUD_SECRET_ID` | 腾讯云 SecretId |
| `TENCENT_CLOUD_SECRET_KEY` | 腾讯云 SecretKey |
| `TENCENT_CLOUD_REGION` | 腾讯云地域 |
| `TENCENT_COS_BUCKET` | COS bucket |
| `TENCENT_VOD_PROCEDURE` | VOD 任务流 |
| `TENCENT_VOD_URL_KEY` | VOD 播放或签名 key |

## 支付配置

支付模块是可选能力。未启用支付时保持相关变量为空。

| 变量 | 说明 |
| --- | --- |
| `PAY_NOTIFY_HOST` | 支付回调公网基础地址 |
| `ALIPAY_APP_ID` | 支付宝 AppId |
| `ALIPAY_MERCHANT_PRIVATE_KEY` | 支付宝商户私钥 |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 |
| `WECHAT_PAY_APP_ID` | 微信支付 AppId |
| `WECHAT_PAY_MCH_ID` | 微信支付商户号 |
| `WECHAT_PAY_MCH_SERIAL_NO` | 微信支付证书序列号 |
| `WECHAT_PAY_PRIVATE_KEY` | 微信支付私钥 |
| `WECHAT_PAY_API_V3_KEY` | 微信支付 API v3 key |

支付私钥、证书和回调验签材料必须通过部署环境注入，不能写入仓库。

## 运维与任务调度

| 变量 | 必填 | 说明 |
| --- | --- | --- |
| `RK_XXL_JOB_ADMIN_ADDRESSES` | 可选 | XXL-Job admin 地址 |
| `RK_XXL_JOB_ACCESS_TOKEN` | 启用时是 | XXL-Job access token |
| `RK_XXL_JOB_EXECUTOR_LOG_PATH` | 否 | 执行器日志目录 |
| `RK_XXL_JOB_LOG_RETENTION_DAYS` | 否 | 日志保留天数 |
| `RK_SEATA_TX_GROUP` | 启用 Seata 时是 | Seata 事务组 |

## 远程安装器配置

安装器位于 `installer/remote-installer`，支持命令行和桌面 GUI。配置文件应复制示例后在本地填写：

```bash
cd installer/remote-installer
npm install
node src/index.mjs wizard
node src/index.mjs preflight --config examples/rk-remote-install.example.json
node src/index.mjs install --config rk-remote-install.json
```

关键配置分组：

| 分组 | 内容 |
| --- | --- |
| `ssh` | 目标主机、端口、用户名、认证方式 |
| `git` | 仓库地址、分支、可选用户名和 Token |
| `deployment` | 目标目录、部署模式、公网入口 |
| `admin` | 初始管理员账号、邮箱、密码 |
| `mysql` | root 密码、应用账号、应用密码 |
| `nacos` | 命名空间、账号、密码 |
| `redis` | 密码、端口、数据库编号 |
| `rabbitmq` | 用户名、密码、vhost |
| `minio` | root 用户、root 密码、bucket、publicBaseUrl |
| `registry` | Harbor、阿里云 ACR 或自定义镜像仓库 |

安装器日志会对密码、Token、访问密钥、Docker config 等敏感字段做脱敏处理。仍建议把日志目录放在本地受控路径，发布问题时先人工检查日志内容。

## 镜像仓库配置

支持以下镜像仓库模式：

| provider | 说明 |
| --- | --- |
| `none` | 不使用外部镜像仓库 |
| `harbor` | 安装器在目标环境部署 Harbor |
| `aliyun` | 使用阿里云 ACR |
| `custom` | 使用自定义镜像仓库 |

使用外部仓库时需要配置：

```text
registry.server
registry.namespace
registry.username
registry.password
```

镜像仓库密码和 Docker config 不应出现在命令行参数、Git 文件或公开日志中。

## GitHub Release 资产

Windows 安装器建议作为 Release 附件上传：

| 类型 | 建议文件名 |
| --- | --- |
| CLI 安装器 | `rk-web-installer-cli-v0.1.0.exe` |
| GUI 安装器 | `rk-web-installer-gui-v0.1.0.exe` |

本地建议路径：

```text
release-assets/v0.1.0/rk-web-installer-cli-v0.1.0.exe
release-assets/v0.1.0/rk-web-installer-gui-v0.1.0.exe
```

建议 Release 信息：

```text
Tag: v0.1.0
Title: RK-Web v0.1.0
```

Release 说明建议包含：

```markdown
RK-Web v0.1.0 提供社团管理平台后端、Web 前端、Android 客户端源码，以及 Windows CLI/GUI 远程安装器。

使用前请阅读仓库根目录的 OPEN_SOURCE_CONFIGURATION.md，并准备数据库、Nacos、Redis、RabbitMQ、MinIO、镜像仓库和公网入口配置。
```

## 发布前检查

提交或发布前建议运行：

```bash
git status --short
git diff --check
rg -n -I --hidden --no-ignore -g '!**/.git/**' -g '!release-assets/**' -g '!signing/**' "BEGIN .*PRIVATE KEY|LTAI[A-Za-z0-9]{12,}|AKIA[0-9A-Z]{16}|sk-[A-Za-z0-9_-]{20,}|AccessKeySecret|secretKey|password\\s*[:=]"
```

还应人工检查：

- 是否存在真实公网 IP、真实内网拓扑或真实服务器路径。
- 是否存在云厂商密钥、短信密钥、邮件授权码、支付私钥、SSH 私钥、镜像仓库 Token。
- 是否存在数据库导出、Nacos 导出、对象存储导出、日志、截图报告或构建产物。
- 是否存在本地 `.env.local`、安装器真实配置、Android keystore 或签名密码。
- GitHub 仓库是否开启 Secret scanning、Push protection 和 Dependabot alerts。

## 验证命令

前端与安装器相关合同测试：

```bash
cd newpro/rk
node --test tests/deploy-package-remote-deploy-contract.test.mjs tests/deploy-package-offline-contract.test.mjs tests/remote-migration-separation-contract.test.mjs tests/security-hardening-contract.test.mjs
```

GUI runner 测试：

```bash
cd ../..
node --test installer/remote-installer/tests/gui-runner.test.mjs
```

后端 Java 测试需要本地安装 Maven：

```bash
mvn test
```
