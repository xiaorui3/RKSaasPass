# RK-Web Remote Installer

This package contains the standalone installer engine for open-source RK-Web deployments. It reads a user-supplied config, connects to a target Linux host over SSH, and runs a generated bootstrap script on the target.

The installer is independent from any private production server, local data center, Jenkins job, or internal artifact store. All hostnames, credentials, repository URLs, registry settings, and middleware passwords must be supplied by the operator.

## Commands

```bash
npm install
npm start
npm run gui
node src/index.mjs wizard
npm run validate
node src/index.mjs preflight --config examples/rk-remote-install.example.json
node src/index.mjs install --config rk-remote-install.json
npm run build:win
```

`build:cli` packages the CLI entrypoint into `dist/rk-web-installer.exe`. `build:gui` packages the Electron GUI into `dist-gui/rk-web-installer-gui.exe`. `build:win` builds both.

Running the CLI executable without arguments opens the interactive wizard. Running the GUI executable opens a desktop form for the same values and streams CLI output into the log panel.

## Local Logs

Every command writes a local redacted log file while still printing progress to the console. By default logs are stored under `logs/` in the current working directory:

```bash
dist/rk-web-installer.exe preflight --config C:\temp\rk-remote-install.json
dist/rk-web-installer.exe install --config C:\temp\rk-remote-install.json --log-dir C:\temp\rk-installer-logs
dist/rk-web-installer.exe install --config C:\temp\rk-remote-install.json --log-file C:\temp\rk-install.log
```

Passwords, tokens, access keys, docker config values, and configured secrets are redacted before logs are written.

## GUI

The GUI is a desktop wrapper around the same CLI engine. It does not implement a separate deployment path.

```bash
npm run gui
npm run build:gui
dist-gui/rk-web-installer-gui.exe
```

Use the GUI buttons in this order for a new target:

1. Validate config
2. Run machine preflight
3. Start install

The GUI writes a temporary config under the OS temp directory. Sensitive fields are passed through child-process environment variables and should not be committed.

## Runtime Modes

- `docker-compose`: installs Docker and the Compose plugin, checks out source from the configured Git repository, builds service images on the target machine, starts middleware, imports fresh schema, creates the initial admin account, imports Nacos templates, then starts RK-Web services.
- `k3s`: installs K3s and builds service images for Kubernetes. `local-tar` imports images into containerd without requiring a registry. `registry` pushes images to a configured registry and creates the image pull secret before workloads are applied.

## Sensitive Data

Do not commit real configs. Copy `examples/rk-remote-install.example.json` and fill secrets only in a local file outside Git.

The installer redacts configured passwords, tokens, keys, and docker config values from logs. It passes secrets to the remote script through SSH command environment variables and writes the target `.env` with restrictive permissions.
