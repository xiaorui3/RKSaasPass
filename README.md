# RK-Web

RK-Web is a multi-tenant club and organization management platform. It contains a Spring Cloud microservice backend, a Vue 3 web frontend, an Android client, and a standalone remote installer.

## What Is Included

- Backend microservices: `rk-auth`, `rk-user`, `rk-gateway`, `rk-content`, `rk-activity`, `rk-message`, `rk-file`, `rk-pay`, `rk-search`, `rk-trade`, `rk-exam`, `rk-data`
- Shared modules: `rk-common`, `rk-api`
- Web frontend: `newpro/rk`
- Android client: `android-client/rk-club-android`
- Remote installer source: `installer/remote-installer`
- Open-source configuration guide: [OPEN_SOURCE_CONFIGURATION.md](OPEN_SOURCE_CONFIGURATION.md)

## Required Middleware

- JDK 11
- Maven 3.6+
- Node.js 18+
- MySQL 8
- Nacos 2.x
- Redis
- RabbitMQ
- MinIO or another object storage provider
- Optional: Elasticsearch, Seata, XXL-Job, SMS, email, payment providers

## Configuration

This repository is prepared for open-source use. Real server addresses, cloud keys, payment private keys, SSH helpers, exported middleware data, logs, and local deployment records must not be committed.

Start with [OPEN_SOURCE_CONFIGURATION.md](OPEN_SOURCE_CONFIGURATION.md). It lists every important environment variable and the places that must be configured before local development or deployment.

## Backend

Each service reads Nacos settings from `bootstrap*.yml`. Defaults point to local development placeholders:

```bash
NACOS_SERVER_ADDR=127.0.0.1:8848
NACOS_NAMESPACE=public
MYSQL_HOST=127.0.0.1
MYSQL_PORT=3306
```

Run services from your IDE or with Maven after your local middleware is ready. Service ports are documented in `OPEN_SOURCE_CONFIGURATION.md`.

## Frontend

```bash
cd newpro/rk
npm install
npm run dev
```

The dev server runs on `http://localhost:5173` and proxies API traffic to the gateway.

## Installer

```bash
cd installer/remote-installer
npm install
npm start
npm run gui
```

Packaged Windows release assets are kept locally under `release-assets/v0.1.0/` for upload to GitHub Releases after review. They are intentionally ignored by Git.

## Security Notice

This repository previously contained private deployment material in tracked files and Git history. Removing secrets from the working tree is not enough for a public release. Before publishing an existing remote repository, rotate every exposed credential and either rewrite history with `git filter-repo`/BFG or publish from a fresh sanitized repository.
