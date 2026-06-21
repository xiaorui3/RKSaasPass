# RK-Web 开源配置说明

本文档用于开源前审阅。当前仓库已按开源发布方向清理工作区内容，但在提交、推送或创建 GitHub Release 前，请先完成本文档中的安全检查。

![RK-Web 一键安装器海报](docs/assets/rk-web-one-click-installer-poster.png)

![RK-Web 开源商用海报](docs/assets/rk-web-open-source-commercial-poster.png)

## 当前状态

- 当前分支：`cloud-master-new`
- 本次只修改 `G:\RK-Web` 当前项目目录内的文件。
- 当前本地仓库已重写为单提交开源基线，尚未推送、尚未创建 GitHub Release，等待人工审阅。
- GUI 与终端安装器已放入本地发布资产目录，供审阅后上传到 GitHub Release。
- 旧 Git 历史曾包含私有运维资料、部署配置、数据库导出、Nacos 导出和真实环境信息；当前本地 refs 已重写为清理后的开源基线。公开前仍需确认远程仓库、其他克隆和备份中不再保留旧历史。

## GitHub Release 资产

本地已准备两个 Windows 安装器文件。它们不应提交到 Git 仓库，审阅通过后作为 GitHub Release 附件上传。

| 类型 | 本地路径 |
| --- | --- |
| 终端安装器 | `release-assets/v0.1.0/rk-web-installer-cli-v0.1.0.exe` |
| GUI 安装器 | `release-assets/v0.1.0/rk-web-installer-gui-v0.1.0.exe` |

建议 Release 版本号：`v0.1.0`

建议 Release 标题：`RK-Web v0.1.0 Open Source Preview`

建议 Release 说明：

```markdown
RK-Web v0.1.0 开源预览版，包含远程一键安装器的 GUI 版本和终端版本。

发布资产：
- rk-web-installer-cli-v0.1.0.exe
- rk-web-installer-gui-v0.1.0.exe

使用前请先阅读仓库根目录的 OPEN_SOURCE_CONFIGURATION.md，并配置自己的数据库、Nacos、Redis、RabbitMQ、对象存储、短信、邮件和支付参数。
```

## 公开前安全要求

1. 轮换所有曾经出现在旧项目文件或 Git 历史中的凭据，包括云厂商密钥、SSH 密码、数据库密码、Redis/RabbitMQ 密码、邮件授权码、短信密钥、支付密钥、镜像仓库密码、Jenkins/Gitee/GitHub Token。
2. 不要把旧 Git 历史直接推到公开仓库。当前本地已经重写为单提交开源基线；推送到公开远程时应使用该新历史覆盖远程，或从当前工作区重新创建一个全新仓库。
3. GitHub 仓库开启 Secret scanning、Push protection、Dependabot alerts。
4. 首次公开提交前重新运行敏感信息扫描，并人工检查所有命中。
5. `release-assets/` 只用于本地发布附件准备，不提交到仓库。

## 基础运行配置

| 环境变量 | 必填 | 说明 | 示例 |
| --- | --- | --- | --- |
| `NACOS_SERVER_ADDR` | 是 | Nacos 地址 | `127.0.0.1:8848` |
| `NACOS_NAMESPACE` | 是 | Nacos 命名空间 | `public` |
| `MYSQL_HOST` | 是 | MySQL 地址 | `127.0.0.1` |
| `MYSQL_PORT` | 是 | MySQL 端口 | `3306` |
| `RK_WEB_PUBLIC_ORIGIN` | 部署时是 | 网关 CORS 允许的前端公网域名 | `https://example.com` |
| `MINIO_ENDPOINT` | 使用 MinIO 时是 | MinIO 内部访问地址 | `http://minio:9000` |
| `XXL_JOB_ADMIN_ADDRESS` | 可选 | XXL-Job 管理端地址 | `http://rk-xxl-job:8880/xxl-job-admin` |

各服务的 `bootstrap*.yml` 已改为使用本地默认值和环境变量占位，不再写死私有 Nacos 地址、命名空间或生产域名。

## 服务端口

| 服务 | 端口 | 说明 |
| --- | ---: | --- |
| `rk-gateway` | 10010 | API 网关 |
| `rk-auth` | 8081 | 认证、账号、角色、权限 |
| `rk-user` | 8082 | 用户、租户、运营后台 |
| `rk-search` | 8083 | 搜索 |
| `rk-file` | 8084 | 文件、图片、媒体 |
| `rk-message` | 8085 | 通知、短信、邮件 |
| `rk-content` | 8086 | 新闻、作品、内容 |
| `rk-pay` | 8087 | 支付集成 |
| `rk-trade` | 8088 | 订单交易 |
| `rk-exam` | 8089 | 题库考试 |
| `rk-activity` | 8090 | 活动、竞赛 |
| `rk-data` | 8093 | 数据看板 |

## 数据库

开源版本不应携带生产数据库导出或真实业务数据。请自行创建本地数据库和账号。

| 变量 | 说明 |
| --- | --- |
| `RK_AUTH_DB_USERNAME` | `rk_auth` 数据库用户名 |
| `RK_AUTH_DB_PASSWORD` | `rk_auth` 数据库密码 |
| `TEST_DB_PASSWORD` | 测试环境数据库密码，占位默认值为 `change-me` |

其他微服务主要通过 Nacos 共享配置读取数据库连接。公开仓库中只保留模板或环境变量占位，不提交真实连接串、账号、密码或数据导出。

## Nacos 配置

所有服务默认读取：

```text
${NACOS_SERVER_ADDR:127.0.0.1:8848}
${NACOS_NAMESPACE:public}
```

部署时需要在自己的 Nacos 命名空间中准备共享配置，例如：

| 配置 | 内容 |
| --- | --- |
| `rk-shared-spring.yaml` | Spring 通用配置 |
| `rk-shared-redis.yaml` | Redis 地址、密码、库编号 |
| `rk-shared-logs.yaml` | 日志级别和输出策略 |
| `rk-shared-mybatis.yaml` | MyBatis Plus 配置 |
| `rk-shared-feign.yaml` | OpenFeign 配置 |
| `rk-shared-seata.yaml` | Seata 配置 |
| `rk-shared-minio.yaml` | MinIO endpoint、bucket、publicBaseUrl |
| `rk-shared-xxljob.yaml` | XXL-Job admin 地址和执行器配置 |

这些配置里凡是涉及地址、密码、Token、密钥、Bucket、域名的内容，都应由部署环境提供。

## Redis、RabbitMQ、对象存储

| 变量 | 说明 |
| --- | --- |
| `RK_REDIS_PASSWORD` | Redis 密码 |
| `RK_RABBITMQ_USERNAME` | RabbitMQ 用户名 |
| `RK_RABBITMQ_PASSWORD` | RabbitMQ 密码 |
| `RK_FILE_PLATFORM` | 文件平台，默认 `MINIO` |
| `RK_MEDIA_PLATFORM` | 媒体平台，默认 `MINIO` |
| `MINIO_ENDPOINT` | MinIO endpoint |
| `MINIO_ACCESS_KEY` | MinIO access key |
| `MINIO_SECRET_KEY` | MinIO secret key |
| `MINIO_BUCKET` | MinIO bucket |
| `MINIO_PUBLIC_BASE_URL` | 对外访问文件的 URL 前缀 |

本地开发优先使用 MinIO。需要接入云对象存储时，只在环境变量或私有部署配置中填写云厂商参数。

## 腾讯云、阿里云、短信、邮件、支付

以下集成全部为可选项。不开启时保持为空即可。

| 变量 | 说明 |
| --- | --- |
| `TENCENT_ENABLE` | 是否启用腾讯云 COS/VOD |
| `TENCENT_CLOUD_APP_ID` | 腾讯云 AppId |
| `TENCENT_CLOUD_SECRET_ID` | 腾讯云 SecretId |
| `TENCENT_CLOUD_SECRET_KEY` | 腾讯云 SecretKey |
| `TENCENT_CLOUD_REGION` | 腾讯云地域 |
| `TENCENT_COS_BUCKET` | COS bucket |
| `TENCENT_VOD_PROCEDURE` | VOD 任务流 |
| `TENCENT_VOD_URL_KEY` | VOD 播放或签名 key |
| `TENCENT_VOD_PFCG` | VOD 预设配置 |
| `ALIYUN_SMS_ACCESS_ID` | 阿里云短信 AccessId |
| `ALIYUN_SMS_ACCESS_SECRET` | 阿里云短信 AccessSecret |
| `TEST_MAIL_PASSWORD` | 测试邮件密码，占位默认值为 `change-me` |
| `PAY_NOTIFY_HOST` | 支付回调公网基础地址 |
| `ALIPAY_APP_ID` | 支付宝 AppId |
| `ALIPAY_MERCHANT_PRIVATE_KEY` | 支付宝商户私钥 |
| `ALIPAY_PUBLIC_KEY` | 支付宝公钥 |
| `WECHAT_PAY_APP_ID` | 微信支付 AppId |
| `WECHAT_PAY_MCH_ID` | 微信支付商户号 |
| `WECHAT_PAY_MCH_SERIAL_NO` | 微信支付证书序列号 |
| `WECHAT_PAY_PRIVATE_KEY` | 微信支付私钥 |
| `WECHAT_PAY_API_V3_KEY` | 微信支付 API v3 key |

不要把任何真实云厂商密钥、短信密钥、邮件授权码或支付私钥提交到仓库。

## 前端

Vue 前端位于：

```bash
cd newpro/rk
npm install
npm run dev
```

前端 API 地址应通过本地环境文件或运行时配置提供。不要提交 `.env.local`、真实公网域名、真实网关地址或内网 NodePort 地址。

## 远程安装器

安装器位于：

```bash
cd installer/remote-installer
npm install
node src/index.mjs wizard
node src/index.mjs preflight --config C:\path\to\rk-remote-install.json
node src/index.mjs install --config C:\path\to\rk-remote-install.json
```

安装器配置中需要填写自己的 SSH、镜像仓库、Nacos、Redis、RabbitMQ、数据库、对象存储等参数。示例地址只能使用保留测试网段或 `example.com`，不得提交真实服务器地址、用户名、密码或 Token。

## 本次开源清理范围

本次清理删除或忽略了以下类型内容：

- 本地 AI/session 记忆、编辑器私有配置、临时 worktree 快照。
- Jenkins 生成任务 XML、旧远程 SSH 辅助脚本、旧 CI/运维目录。
- Nacos 导出、数据库导出、初始化真实数据、运行日志、压缩日志。
- Playwright 报告、截图、构建产物、前端 dist/build 输出。
- 旧内部待办、部署记录、生产排障记录、含公网 IP 或私有凭据的 Markdown 文档。

保留下来的配置已尽量改为环境变量占位或本地开发默认值。

## 本次核验记录

核验日期：2026-06-21

- 当前工作区旧公网 IP、旧私有 Git 地址、旧镜像仓库地址、云厂商 AccessKey 特征、私钥文件头、历史示例密码等重点规则已复扫，未发现真实值命中。
- Markdown 文档已复查，测试计划中的示例账号密码和 JWT 示例已改为占位。
- 真实公网 IP 已复查，当前保留的是本地地址、私网地址或 RFC 文档示例网段。
- 当前本地 Git 历史已重写为单提交开源基线。旧私有运维信息、旧公网地址、旧私有 Git 地址和云厂商密钥特征不再能从当前本地 refs 中检索到。
- 本地旧分支、tag、stash 和旧 worktree 元数据 refs 已清理，仅保留 `cloud-master-new`。
- `release-assets/v0.1.0/` 下已准备 GUI 和终端两个 Windows 安装器，仅作为 GitHub Release 附件候选，不应提交进仓库。

已运行的验证：

```bash
cd newpro/rk
node --test tests/deploy-package-remote-deploy-contract.test.mjs tests/deploy-package-offline-contract.test.mjs tests/remote-migration-separation-contract.test.mjs tests/security-hardening-contract.test.mjs
```

结果：82/82 通过。

```bash
node --test installer/remote-installer/tests/gui-runner.test.mjs
```

结果：11/11 通过。

```bash
git diff --check
```

结果：无 whitespace error，仅有 Windows 换行提示。

Java/Maven 测试未运行：当前环境未安装 `mvn`，仓库根目录也没有 Maven Wrapper。

## 提交前检查

提交前建议至少运行：

```bash
git status --short
rg -n -I --hidden --no-ignore -g '!**/.git/**' -g '!release-assets/**' -e "BEGIN .*PRIVATE KEY|accessSecret|secretKey|password\s*[:=]"
```

还需要运行你自己的云厂商 key、镜像仓库 Token、Webhook Token、支付密钥、SSH 私钥扫描规则。任何真实命中都必须删除或改为环境变量占位。

## 发布顺序建议

1. 人工审阅当前单提交开源基线和本文档。
2. 轮换旧凭据。
3. 确认远程仓库是否用当前单提交历史强制覆盖，或从当前文件创建全新公开仓库。
4. 重新运行敏感信息扫描。
5. 推送当前清理后的单提交开源版本到 GitHub。
6. 检查 GitHub Secret scanning / Push protection 结果。
7. 创建 `v0.1.0` Release。
8. 上传 GUI 与终端两个安装器附件。
