import { SERVICE_MATRIX } from './config.mjs'

const DOCKER_MIRROR_HOST = 'docker.1ms.run'
const DOCKER_MIRROR_URL = `https://${DOCKER_MIRROR_HOST}`
const BACKEND_BASE_IMAGE = `${DOCKER_MIRROR_HOST}/library/eclipse-temurin:11-jre-jammy`
const FRONTEND_BASE_IMAGE = `${DOCKER_MIRROR_HOST}/library/nginx:1.27-alpine`
const MAVEN_IMAGE = `${DOCKER_MIRROR_HOST}/library/maven:3.9.9-eclipse-temurin-11`
const NODE_IMAGE = `${DOCKER_MIRROR_HOST}/library/node:20.18.0-bookworm-slim`
const KANIKO_IMAGE = `${DOCKER_MIRROR_HOST}/kaniko-project/executor:v1.23.2-debug`
const HOST_BACKEND_BASE_IMAGE = `${DOCKER_MIRROR_HOST}/library/eclipse-temurin:11-jre-jammy`
const HOST_FRONTEND_BASE_IMAGE = `${DOCKER_MIRROR_HOST}/library/nginx:1.27-alpine`
const HOST_MAVEN_IMAGE = `${DOCKER_MIRROR_HOST}/library/maven:3.9.9-eclipse-temurin-11`
const HOST_NODE_IMAGE = `${DOCKER_MIRROR_HOST}/library/node:20.18.0-bookworm-slim`
const HOST_BACKEND_BASE_IMAGE_CANDIDATES = [
  HOST_BACKEND_BASE_IMAGE,
  'docker.m.daocloud.io/library/eclipse-temurin:11-jre-jammy',
  'docker.1panel.live/library/eclipse-temurin:11-jre-jammy',
  'swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/eclipse-temurin:11-jre-jammy',
  'dockerproxy.net/library/eclipse-temurin:11-jre-jammy'
]
const HOST_FRONTEND_BASE_IMAGE_CANDIDATES = [
  HOST_FRONTEND_BASE_IMAGE,
  'docker.m.daocloud.io/library/nginx:1.27-alpine',
  'docker.1panel.live/library/nginx:1.27-alpine',
  'swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/nginx:1.27-alpine',
  'dockerproxy.net/library/nginx:1.27-alpine'
]
const HOST_MAVEN_IMAGE_CANDIDATES = [
  HOST_MAVEN_IMAGE,
  'docker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-11',
  'docker.1panel.live/library/maven:3.9.9-eclipse-temurin-11',
  'swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/maven:3.9.9-eclipse-temurin-11',
  'dockerproxy.net/library/maven:3.9.9-eclipse-temurin-11'
]
const HOST_NODE_IMAGE_CANDIDATES = [
  HOST_NODE_IMAGE,
  'docker.m.daocloud.io/library/node:20-bookworm-slim',
  'docker.1panel.live/library/node:20-bookworm-slim',
  'swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/library/node:20-bookworm-slim',
  'dockerproxy.net/library/node:20-slim'
]
const MYSQL_IMAGE = `${DOCKER_MIRROR_HOST}/library/mysql:8.0`
const NACOS_IMAGE = `${DOCKER_MIRROR_HOST}/nacos/nacos-server:v2.3.2`
const REDIS_IMAGE = `${DOCKER_MIRROR_HOST}/library/redis:7`
const RABBITMQ_IMAGE = `${DOCKER_MIRROR_HOST}/heidiks/rabbitmq-delayed-message-exchange:3.13.0-management`
const MINIO_IMAGE = `${DOCKER_MIRROR_HOST}/minio/minio:RELEASE.2024-05-10T01-41-38Z`
const XXL_JOB_IMAGE = `${DOCKER_MIRROR_HOST}/xuxueli/xxl-job-admin:2.3.0`

export function buildRemoteInstallScript(config) {
  const selectedServices = SERVICE_MATRIX.filter((service) => (config.services || []).includes(service.name))
  const serviceLines = selectedServices
    .map((item) => [item.name, item.module, item.jarDir, item.image, item.port, item.containerPort].join('|'))
    .join('\n')
  const k3sHarborRegistry = config.runtime?.mode === 'k3s' && config.k3s?.imageMode === 'registry' && config.registry?.provider === 'harbor'
  const dockerlessK3s = config.runtime?.mode === 'k3s' && config.runtime?.installDocker === false && !k3sHarborRegistry
  const domesticMirror = config.deployment?.domesticMirror !== false
  const harborVersion = config.registry?.harborVersion || '2.14.4'
  const harborInstallerKind = config.registry?.harborInstallerKind || 'online'
  const registryServer = config.registry?.server || 'registry.example.invalid'
  const registryPushServer = k3sHarborRegistry && config.registry?.installHarbor === true
    ? `127.0.0.1:${config.registry?.harborHttpPort || 30090}`
    : registryServer
  const accessMode = config.deployment?.accessMode || 'local-loopback'
  const firewallExternalNotice = accessMode === 'public-nodeport'
    ? 'public access mode: upstream firewall or cloud security group must allow TCP ports: ${ports[*]}'
    : 'local-loopback mode keeps non-SSH ports private'
  const composeGatewayPort = String(config.compose?.gatewayPort || 10010)
  const composeFrontendPort = String(config.compose?.frontendPort || 30080)
  const composeNacosPort = String(config.compose?.nacosPort || 8848)
  const composeRabbitManagementPort = String(config.compose?.rabbitManagementPort || 15672)
  const composeMinioConsolePort = String(config.compose?.minioConsolePort || 9001)
  const composeExposedPorts = unique([
    ...selectedServices.map((service) => {
      if (service.name === 'rk-gateway') return composeGatewayPort
      if (service.name === 'frontend') return composeFrontendPort
      return String(service.port)
    }),
    composeNacosPort
  ])
  const harborInstallerName = `harbor-${harborInstallerKind}-installer-v${harborVersion}.tgz`
  const releaseIdDefault = config.releaseId || 'rk-web-$(date +%Y%m%d%H%M%S)'
  const harborDownloadUrls = Array.isArray(config.registry?.harborDownloadUrls) && config.registry.harborDownloadUrls.length > 0
    ? config.registry.harborDownloadUrls
    : [
        `https://github.com/goharbor/harbor/releases/download/v${harborVersion}/${harborInstallerName}`,
        `https://sourceforge.net/projects/harbor.mirror/files/v${harborVersion}/${harborInstallerName}/download`
      ]
  return `#!/usr/bin/env bash
set -euo pipefail
export TERM="\${TERM:-xterm}"

RK_INSTALL_ACTION="\${RK_INSTALL_ACTION:-install}"
RK_TARGET_PATH=${sh(config.deployment?.targetPath || '/opt/rk-web')}
RK_RELEASE_ID="\${RK_RELEASE_ID:-${releaseIdDefault}}"
RK_REPO_URL=${sh(config.git?.repositoryUrl || '')}
RK_REPO_BRANCH=${sh(config.git?.branch || 'main')}
RK_GIT_PROVIDER=${sh(config.git?.provider || 'gitee')}
RK_GIT_USERNAME="\${RK_GIT_USERNAME:-}"
RK_GIT_TOKEN="\${RK_GIT_TOKEN:-}"
RK_RUNTIME_MODE=${sh(config.runtime?.mode || 'docker-compose')}
RK_ACCESS_MODE=${sh(accessMode)}
RK_BUILD_ON_REMOTE=${sh(String(config.runtime?.buildOnRemote !== false))}
RK_DEPLOY_SERVICES=${sh(String(config.runtime?.deployServices !== false))}
RK_K3S_IMAGE_MODE=${sh(config.k3s?.imageMode || 'local-tar')}
RK_K3S_DOCKERLESS_BUILD=${sh(String(dockerlessK3s))}
RK_NAMESPACE=${sh(config.k3s?.namespace || 'rk-web')}
RK_K3S_FRONTEND_NODE_PORT=${sh(String(config.k3s?.frontendNodePort || 30080))}
RK_K3S_GATEWAY_NODE_PORT=${sh(String(config.k3s?.gatewayNodePort || 30010))}
RK_REGISTRY_PROVIDER=${sh(config.registry?.provider || 'none')}
RK_REGISTRY_SERVER=${sh(registryServer)}
RK_REGISTRY_PUSH_SERVER=${sh(registryPushServer)}
RK_REGISTRY_NAMESPACE=${sh(config.registry?.namespace || 'rk-web')}
RK_IMAGE_PULL_SECRET=${sh(config.registry?.imagePullSecret || 'rk-registry')}
RK_REGISTRY_INSECURE=${sh(String(config.registry?.insecure === true))}
RK_INSTALL_HARBOR=${sh(String(config.registry?.installHarbor === true))}
RK_HARBOR_HTTP_PORT=${sh(String(config.registry?.harborHttpPort || 30090))}
RK_HARBOR_DATA_PATH=${sh(config.registry?.harborDataPath || '/data/harbor')}
RK_HARBOR_VERSION=${sh(harborVersion)}
RK_HARBOR_INSTALLER_KIND=${sh(harborInstallerKind)}
RK_HARBOR_IMAGE_REPOSITORY=${sh('goharbor')}
RK_COMPOSE_PROJECT=${sh(config.compose?.projectName || 'rk-web')}
RK_COMPOSE_GATEWAY_PORT=${sh(composeGatewayPort)}
RK_COMPOSE_FRONTEND_PORT=${sh(composeFrontendPort)}
RK_COMPOSE_NACOS_PORT=${sh(composeNacosPort)}
RK_COMPOSE_RABBITMQ_MANAGEMENT_PORT=${sh(composeRabbitManagementPort)}
RK_COMPOSE_MINIO_CONSOLE_PORT=${sh(composeMinioConsolePort)}
RK_COMPOSE_EXPOSED_PORTS=${sh(composeExposedPorts.join(' '))}
RK_DOMESTIC_MIRROR=${sh(String(domesticMirror))}
RK_DOCKER_CE_REPO_URL=${sh(domesticMirror ? 'https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo' : 'https://download.docker.com/linux/centos/docker-ce.repo')}
RK_DOCKER_APT_REPO_URL=${sh(domesticMirror ? 'https://mirrors.aliyun.com/docker-ce/linux/ubuntu' : 'https://download.docker.com/linux/ubuntu')}
RK_DOCKER_APT_GPG_URL=${sh(domesticMirror ? 'https://mirrors.aliyun.com/docker-ce/linux/ubuntu/gpg' : 'https://download.docker.com/linux/ubuntu/gpg')}
RK_DOCKER_REGISTRY_MIRROR=${sh(domesticMirror ? DOCKER_MIRROR_URL : '')}
RK_HARBOR_DOWNLOAD_URLS=${sh(harborDownloadUrls.join('\n'))}
RK_SERVICE_LINES=${sh(serviceLines)}
RK_BACKEND_BASE_IMAGE=${sh(BACKEND_BASE_IMAGE)}
RK_FRONTEND_BASE_IMAGE=${sh(FRONTEND_BASE_IMAGE)}
RK_MAVEN_IMAGE=${sh(MAVEN_IMAGE)}
RK_NODE_IMAGE=${sh(NODE_IMAGE)}
RK_KANIKO_IMAGE=${sh(KANIKO_IMAGE)}
RK_HOST_BACKEND_BASE_IMAGE=${sh(HOST_BACKEND_BASE_IMAGE)}
RK_HOST_FRONTEND_BASE_IMAGE=${sh(HOST_FRONTEND_BASE_IMAGE)}
RK_HOST_MAVEN_IMAGE=${sh(HOST_MAVEN_IMAGE)}
RK_HOST_NODE_IMAGE=${sh(HOST_NODE_IMAGE)}
RK_HOST_BACKEND_BASE_IMAGE_CANDIDATES=${sh(HOST_BACKEND_BASE_IMAGE_CANDIDATES.join('\n'))}
RK_HOST_FRONTEND_BASE_IMAGE_CANDIDATES=${sh(HOST_FRONTEND_BASE_IMAGE_CANDIDATES.join('\n'))}
RK_HOST_MAVEN_IMAGE_CANDIDATES=${sh(HOST_MAVEN_IMAGE_CANDIDATES.join('\n'))}
RK_HOST_NODE_IMAGE_CANDIDATES=${sh(HOST_NODE_IMAGE_CANDIDATES.join('\n'))}
RK_MYSQL_IMAGE=${sh(MYSQL_IMAGE)}
RK_NACOS_IMAGE=${sh(NACOS_IMAGE)}
RK_REDIS_IMAGE=${sh(REDIS_IMAGE)}
RK_RABBITMQ_IMAGE=${sh(RABBITMQ_IMAGE)}
RK_MINIO_IMAGE=${sh(MINIO_IMAGE)}
RK_XXL_JOB_IMAGE=${sh(XXL_JOB_IMAGE)}

log() { printf '[rk-installer] %s\\n' "$*"; }
fail() { printf '[rk-installer][error] %s\\n' "$*" >&2; exit 1; }
have() { command -v "$1" >/dev/null 2>&1; }
require() { have "$1" || fail "missing command: $1"; }
env_quote() { printf '%s\\n' "$1" | sed "s/'/'\\\\''/g; 1s/^/'/; \\$s/\\$/'/"; }
nacos_ready_url() { printf 'http://127.0.0.1:%s/nacos/v1/console/health/readiness' "$1"; }

download_with_fallback() {
  local target="$1" label="$2" url
  shift 2
  for url in "$@"; do
    [ -n "$url" ] || continue
    log "downloading $label from $url"
    if curl -fL --connect-timeout 20 --retry 3 --retry-delay 5 --speed-limit 1024 --speed-time 60 "$url" -o "$target"; then
      return 0
    fi
  done
  fail "failed to download $label"
}

docker_pull_with_retry() {
  local image="$1"
  local attempt
  for attempt in 1 2 3 4 5; do
    log "pulling docker image $image (attempt $attempt/5)"
    if docker pull "$image"; then
      return 0
    fi
    sleep $((attempt * 10))
  done
  fail "failed to pull docker image $image"
}

docker_pull_candidate() {
  local image="$1"
  docker pull "$image" >/dev/null && docker image inspect "$image" >/dev/null
}

select_docker_image_candidate() {
  local __result_var="$1" candidates="$2" candidate_image
  while IFS= read -r candidate_image; do
    [ -n "$candidate_image" ] || continue
    log "trying docker image $candidate_image"
    if docker_pull_candidate "$candidate_image"; then
      log "using docker image $candidate_image"
      printf -v "$__result_var" '%s' "$candidate_image"
      return 0
    fi
    log "docker image $candidate_image is unavailable, trying next candidate"
  done <<< "$candidates"
  fail "all docker image candidates failed"
}

docker_run_with_image_candidates() {
  local candidates="$1" image
  shift
  select_docker_image_candidate image "$candidates"
  docker run --rm "$@" "$image" bash -lc "$RK_DOCKER_RUN_SCRIPT"
}

docker_build_with_base_image_candidates() {
  local candidates="$1" build_dir="$2" base_image
  shift 2
  select_docker_image_candidate base_image "$candidates"
  docker build --build-arg BASE_IMAGE="$base_image" "$@" "$build_dir"
}

cleanup_docker_build_cache() {
  have docker || return 0
  log "pruning docker build cache and unused images"
  docker builder prune -af || true
  docker image prune -af || true
  docker system df || true
}

check_k3s_node_health() {
  [ "$RK_RUNTIME_MODE" = "k3s" ] || return 0
  require kubectl
  log "checking k3s node health"
  local attempt taints not_ready
  for attempt in $(seq 1 36); do
    kubectl get nodes -o wide || true
    taints="$(kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{" "}{range .spec.taints[*]}{.key}{":"}{.effect}{" "}{end}{"\\n"}{end}' 2>/dev/null || true)"
    not_ready="$(kubectl get nodes --no-headers 2>/dev/null | awk '$2 != "Ready" { print $1 ":" $2 }' || true)"
    if printf '%s\\n' "$taints" | grep -Fq 'node.kubernetes.io/disk-pressure'; then
      log "k3s node has DiskPressure; waiting for kubelet to refresh after cleanup"
      df -h "$RK_TARGET_PATH" /var/lib/docker /var/lib/rancher 2>/dev/null || true
      docker system df 2>/dev/null || true
    elif [ -n "$not_ready" ]; then
      log "k3s node is not ready yet: $not_ready"
    else
      return 0
    fi
    sleep 5
  done
  df -h "$RK_TARGET_PATH" /var/lib/docker /var/lib/rancher 2>/dev/null || true
  docker system df 2>/dev/null || true
  fail "k3s node has DiskPressure or is not Ready; free disk space and retry the install"
}

reset_k3s_namespace_for_fresh_install() {
  require kubectl
  local attempt
  if kubectl get namespace "$RK_NAMESPACE" >/dev/null 2>&1; then
    log "resetting k3s namespace $RK_NAMESPACE for fresh install"
    kubectl delete namespace "$RK_NAMESPACE" --wait=false
    for attempt in $(seq 1 120); do
      if ! kubectl get namespace "$RK_NAMESPACE" >/dev/null 2>&1; then
        break
      fi
      [ "$attempt" -lt 120 ] || fail "k3s namespace $RK_NAMESPACE is still terminating"
      log "k3s namespace $RK_NAMESPACE still terminating (attempt $attempt/120)"
      sleep 3
    done
  fi
  kubectl create namespace "$RK_NAMESPACE" --dry-run=client -o yaml | kubectl apply -f -
}

open_local_firewall_ports() {
  local ports=() port changed=false
  for port in "$@"; do
    [ -n "$port" ] || continue
    case "$port" in
      *[!0-9]*)
        log "skipping invalid firewall port: $port"
        continue
        ;;
    esac
    ports+=("$port")
  done
  [ "\${#ports[@]}" -gt 0 ] || return 0
  log "checking local firewall for TCP ports: \${ports[*]}"
  if have firewall-cmd && firewall-cmd --state >/dev/null 2>&1; then
    for port in "\${ports[@]}"; do
      if firewall-cmd --query-port="\${port}/tcp" >/dev/null 2>&1; then
        log "firewalld already allows tcp/$port"
        continue
      fi
      if firewall-cmd --permanent --add-port="\${port}/tcp"; then
        changed=true
        log "firewalld allowed tcp/$port"
      else
        log "warning: failed to add firewalld tcp/$port"
      fi
    done
    if [ "$changed" = "true" ]; then
      firewall-cmd --reload || log "warning: failed to reload firewalld"
    fi
  elif have ufw && ufw status 2>/dev/null | grep -qi '^Status: active'; then
    for port in "\${ports[@]}"; do
      ufw allow "\${port}/tcp" || log "warning: failed to add ufw tcp/$port"
    done
  else
    log "no active firewalld or ufw detected; skipping local firewall changes"
  fi
  log "${firewallExternalNotice}"
}

open_runtime_firewall_ports() {
  if [ "$RK_ACCESS_MODE" = "local-loopback" ]; then
    log "access mode local-loopback; keeping service ports on target localhost/cluster network only"
    log "only the SSH port needs to be reachable from outside"
    return
  fi
  if [ "$RK_RUNTIME_MODE" = "docker-compose" ]; then
    open_local_firewall_ports $RK_COMPOSE_EXPOSED_PORTS
    return
  fi
  if [ "$RK_RUNTIME_MODE" = "k3s" ]; then
    if [ "$RK_K3S_IMAGE_MODE" = "registry" ] && [ "$RK_REGISTRY_PROVIDER" = "harbor" ] && [ "$RK_INSTALL_HARBOR" = "true" ]; then
      open_local_firewall_ports "$RK_K3S_FRONTEND_NODE_PORT" "$RK_K3S_GATEWAY_NODE_PORT" "$RK_HARBOR_HTTP_PORT"
    else
      open_local_firewall_ports "$RK_K3S_FRONTEND_NODE_PORT" "$RK_K3S_GATEWAY_NODE_PORT"
    fi
  fi
}

rk_preflight() {
  log "preflight start"
  echo "os=$(cat /etc/os-release 2>/dev/null | tr '\\n' ' ' || true)"
  echo "arch=$(uname -m)"
  for cmd in bash git curl tar sed awk; do
    if have "$cmd"; then echo "present:$cmd"; else echo "missing:$cmd"; fi
  done
  if have docker; then echo "present:docker"; else echo "missing:docker"; fi
  if docker compose version >/dev/null 2>&1; then echo "present:docker-compose"; else echo "missing:docker-compose"; fi
  if have kubectl; then echo "present:kubectl"; else echo "missing:kubectl"; fi
  if have k3s; then echo "present:k3s"; else echo "missing:k3s"; fi
  df -Pm "$RK_TARGET_PATH" 2>/dev/null || df -Pm /
  if have kubectl; then
    kubectl get nodes -o wide 2>/dev/null || true
    kubectl get nodes -o jsonpath='{range .items[*]}{.metadata.name}{" taints="}{.spec.taints}{"\\n"}{end}' 2>/dev/null || true
  fi
  log "preflight complete"
}

install_base_packages() {
  log "installing base packages"
  if have dnf; then
    dnf install -y git curl tar gzip unzip ca-certificates shadow-utils
  elif have yum; then
    yum install -y git curl tar gzip unzip ca-certificates shadow-utils
  elif have apt-get; then
    apt-get update
    DEBIAN_FRONTEND=noninteractive apt-get install -y git curl tar gzip unzip ca-certificates
  else
    fail "unsupported Linux package manager"
  fi
}

install_docker_runtime() {
  local docker_already_ready=false
  if have docker && docker compose version >/dev/null 2>&1; then
    log "docker runtime already installed"
    docker_already_ready=true
  else
    log "installing docker runtime"
    if have dnf; then
      dnf install -y dnf-plugins-core || true
      rm -f /etc/yum.repos.d/docker-ce.repo
      dnf config-manager --add-repo "$RK_DOCKER_CE_REPO_URL" || true
      dnf clean metadata || true
      dnf install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    elif have yum; then
      yum install -y yum-utils || true
      rm -f /etc/yum.repos.d/docker-ce.repo
      yum-config-manager --add-repo "$RK_DOCKER_CE_REPO_URL" || true
      yum clean metadata || true
      yum install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    elif have apt-get; then
      install -m 0755 -d /etc/apt/keyrings
      curl -fsSL "$RK_DOCKER_APT_GPG_URL" -o /etc/apt/keyrings/docker.asc
      chmod a+r /etc/apt/keyrings/docker.asc
      echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] $RK_DOCKER_APT_REPO_URL $(. /etc/os-release && echo "$VERSION_CODENAME") stable" > /etc/apt/sources.list.d/docker.list
      apt-get update
      DEBIAN_FRONTEND=noninteractive apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
    fi
  fi
  configure_docker_daemon
  if [ "$docker_already_ready" = "true" ] && systemctl is-active --quiet docker; then
    ensure_docker_service_ready restart
  else
    ensure_docker_service_ready start
  fi
  docker compose version >/dev/null 2>&1 || fail "docker compose is still unavailable after install"
}

ensure_docker_service_ready() {
  local mode="\${1:-start}"
  if [ "$mode" = "restart" ]; then
    systemctl restart docker || true
  else
    systemctl enable docker >/dev/null 2>&1 || true
    systemctl start docker || true
  fi
  for _ in $(seq 1 90); do
    if systemctl is-active --quiet docker && docker info >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done
  systemctl status docker --no-pager -l || true
  journalctl -u docker.service -n 80 --no-pager || true
  fail "docker service did not become ready after $mode"
}

configure_docker_daemon() {
  mkdir -p /etc/docker
  if [ -n "$RK_DOCKER_REGISTRY_MIRROR" ] && [ "$RK_K3S_IMAGE_MODE" = "registry" ] && [ "$RK_REGISTRY_INSECURE" = "true" ]; then
    cat > /etc/docker/daemon.json <<EOF_DOCKER_DAEMON
{
  "registry-mirrors": ["$RK_DOCKER_REGISTRY_MIRROR"],
  "insecure-registries": ["$RK_REGISTRY_SERVER", "$RK_REGISTRY_PUSH_SERVER"]
}
EOF_DOCKER_DAEMON
  elif [ -n "$RK_DOCKER_REGISTRY_MIRROR" ]; then
    cat > /etc/docker/daemon.json <<EOF_DOCKER_DAEMON
{
  "registry-mirrors": ["$RK_DOCKER_REGISTRY_MIRROR"]
}
EOF_DOCKER_DAEMON
  elif [ "$RK_K3S_IMAGE_MODE" = "registry" ] && [ "$RK_REGISTRY_INSECURE" = "true" ]; then
    cat > /etc/docker/daemon.json <<EOF_DOCKER_DAEMON
{
  "insecure-registries": ["$RK_REGISTRY_SERVER", "$RK_REGISTRY_PUSH_SERVER"]
}
EOF_DOCKER_DAEMON
  fi
}

docker_login_registry() {
  [ "$RK_K3S_IMAGE_MODE" = "registry" ] || return 0
  [ -n "$RK_REGISTRY_USER" ] || return 0
  [ -n "$RK_REGISTRY_PASSWORD" ] || return 0
  printf '%s' "$RK_REGISTRY_PASSWORD" | docker login "$RK_REGISTRY_PUSH_SERVER" --username "$RK_REGISTRY_USER" --password-stdin
}

install_harbor_registry() {
  [ "$RK_K3S_IMAGE_MODE" = "registry" ] || return 0
  [ "$RK_REGISTRY_PROVIDER" = "harbor" ] || return 0
  [ "$RK_INSTALL_HARBOR" = "true" ] || return 0
  require docker
  log "installing harbor registry at $RK_REGISTRY_SERVER"
  if harbor_api_ready; then
    if harbor_auth_ready; then
      log "harbor api already available; skipping harbor installer"
      ensure_harbor_project
      return
    fi
    log "existing harbor rejects configured credentials; rebuilding installer-managed harbor"
    reset_installer_managed_harbor
  fi
  if start_existing_harbor && harbor_api_ready; then
    if harbor_auth_ready; then
      log "existing harbor started; skipping harbor installer"
      ensure_harbor_project
      return
    fi
    log "existing harbor rejects configured credentials; rebuilding installer-managed harbor"
    reset_installer_managed_harbor
  fi
  local harbor_dir archive
  harbor_dir="$RK_TARGET_PATH/harbor"
  archive="$harbor_dir/harbor-$RK_HARBOR_INSTALLER_KIND-installer-v$RK_HARBOR_VERSION.tgz"
  mkdir -p "$harbor_dir" "$RK_HARBOR_DATA_PATH"
  if [ ! -f "$archive" ]; then
    mapfile -t harbor_urls <<< "$RK_HARBOR_DOWNLOAD_URLS"
    download_with_fallback "$archive" "harbor $RK_HARBOR_INSTALLER_KIND installer" "\${harbor_urls[@]}"
  fi
  tar -xzf "$archive" -C "$harbor_dir"
  local install_dir
  install_dir="$harbor_dir/harbor"
  if [ -f "$install_dir/prepare" ] && [ -n "$RK_HARBOR_IMAGE_REPOSITORY" ]; then
    sed -i "s#^[[:space:]]*goharbor/prepare:#                    $RK_HARBOR_IMAGE_REPOSITORY/prepare:#" "$install_dir/prepare"
  fi
  docker_pull_with_retry "$RK_HARBOR_IMAGE_REPOSITORY/prepare:v$RK_HARBOR_VERSION"
  cat > "$install_dir/harbor.yml" <<EOF_HARBOR
hostname: ${config.registry?.server || '127.0.0.1:30090'}
http:
  port: $RK_HARBOR_HTTP_PORT
harbor_admin_password: $RK_REGISTRY_PASSWORD
database:
  password: rk_harbor_db_password
data_volume: $RK_HARBOR_DATA_PATH
trivy:
  ignore_unfixed: false
  skip_update: true
  offline_scan: true
jobservice:
  max_job_workers: 10
  job_loggers:
  - STD_OUTPUT
  - FILE
  logger_sweeper_duration: 1
notification:
  webhook_job_max_retry: 3
  webhook_job_http_client_timeout: 3
log:
  level: info
  local:
    location: $RK_HARBOR_DATA_PATH/log
EOF_HARBOR
  (cd "$install_dir" && HARBOR_IMAGE_REPOSITORY="$RK_HARBOR_IMAGE_REPOSITORY" ./install.sh)
  wait_for_harbor
  ensure_harbor_project
}

harbor_api_ready() {
  [ "$RK_REGISTRY_PROVIDER" = "harbor" ] || return 1
  curl -fsS "http://127.0.0.1:$RK_HARBOR_HTTP_PORT/api/v2.0/systeminfo" >/dev/null 2>&1
}

harbor_auth_ready() {
  [ "$RK_REGISTRY_PROVIDER" = "harbor" ] || return 1
  curl -fsS -u "$RK_REGISTRY_USER:$RK_REGISTRY_PASSWORD" "http://127.0.0.1:$RK_HARBOR_HTTP_PORT/api/v2.0/users/current" >/dev/null 2>&1
}

reset_installer_managed_harbor() {
  local install_dir="$RK_TARGET_PATH/harbor/harbor"
  case "$RK_HARBOR_DATA_PATH" in
    ""|"/")
      fail "refusing to reset unsafe harbor data path: $RK_HARBOR_DATA_PATH"
      ;;
  esac
  if [ -f "$install_dir/docker-compose.yml" ]; then
    log "stopping installer-managed harbor compose stack before reset"
    (cd "$install_dir" && docker compose down -v --remove-orphans) || true
  fi
  log "removing installer-managed harbor data and extracted installer"
  rm -rf "$RK_HARBOR_DATA_PATH"
  rm -rf "$install_dir"
}

start_existing_harbor() {
  local install_dir="$RK_TARGET_PATH/harbor/harbor"
  [ -f "$install_dir/docker-compose.yml" ] || return 1
  log "starting existing harbor compose stack"
  (cd "$install_dir" && docker compose up -d) || return 1
  for _ in $(seq 1 24); do
    if harbor_api_ready; then
      return 0
    fi
    sleep 5
  done
  return 1
}

wait_for_harbor() {
  [ "$RK_REGISTRY_PROVIDER" = "harbor" ] || return 0
  log "waiting for harbor api"
  for _ in $(seq 1 120); do
    if harbor_api_ready; then
      return
    fi
    sleep 5
  done
  fail "harbor api did not become ready"
}

ensure_harbor_project() {
  [ "$RK_REGISTRY_PROVIDER" = "harbor" ] || return 0
  local api="http://127.0.0.1:$RK_HARBOR_HTTP_PORT/api/v2.0"
  log "ensuring harbor project $RK_REGISTRY_NAMESPACE"
  if harbor_project_exists "$api"; then
    return
  fi
  local status payload
  payload="/tmp/rk-harbor-project-create-payload.json"
  printf '{"project_name":"%s","metadata":{"public":"false"}}' "$RK_REGISTRY_NAMESPACE" > "$payload"
  status="$(curl -sS -o /tmp/rk-harbor-project-create.json -w "%{http_code}" \
    -u "$RK_REGISTRY_USER:$RK_REGISTRY_PASSWORD" \
    -H 'Content-Type: application/json' \
    -X POST "$api/projects" \
    --data-binary @"$payload")"
  if [ "$status" = "201" ] || [ "$status" = "200" ]; then
    return
  fi
  if [ "$status" = "409" ] || [ "$status" = "422" ]; then
    harbor_project_exists "$api" && return
  fi
  cat /tmp/rk-harbor-project-create.json >&2 || true
  fail "failed to create harbor project $RK_REGISTRY_NAMESPACE, http status $status"
}

harbor_project_exists() {
  local api="$1"
  curl -fsS -u "$RK_REGISTRY_USER:$RK_REGISTRY_PASSWORD" "$api/projects/$RK_REGISTRY_NAMESPACE" >/dev/null 2>&1
}

write_k3s_bootstrap_config() {
  mkdir -p /etc/rancher/k3s
  log "writing k3s bootstrap config"
  cat > /etc/rancher/k3s/config.yaml <<EOF_K3S_CONFIG
disable:
  - traefik
  - local-storage
EOF_K3S_CONFIG
  if [ "$RK_DOMESTIC_MIRROR" = "true" ]; then
    cat >> /etc/rancher/k3s/config.yaml <<EOF_K3S_CONFIG
system-default-registry: ${DOCKER_MIRROR_HOST}
EOF_K3S_CONFIG
  fi
}

write_k3s_registry_config() {
  [ "$RK_K3S_IMAGE_MODE" = "registry" ] || return 0
  [ "$RK_REGISTRY_INSECURE" = "true" ] || return 0
  mkdir -p /etc/rancher/k3s
  cat > /etc/rancher/k3s/registries.yaml <<EOF_REGISTRIES
mirrors:
  "$RK_REGISTRY_SERVER":
    endpoint:
      - "http://$RK_REGISTRY_PUSH_SERVER"
configs:
  "$RK_REGISTRY_PUSH_SERVER":
    auth:
      username: "$RK_REGISTRY_USER"
      password: "$RK_REGISTRY_PASSWORD"
    tls:
      insecure_skip_verify: true
EOF_REGISTRIES
}

copy_k3s_kubeconfig() {
  mkdir -p "$HOME/.kube"
  cp /etc/rancher/k3s/k3s.yaml "$HOME/.kube/config"
}

restart_k3s_runtime() {
  if systemctl is-active --quiet k3s; then
    systemctl restart k3s
    k3s kubectl get nodes >/dev/null
  fi
}

install_k3s_runtime() {
  export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
  if have k3s && k3s kubectl get nodes >/dev/null 2>&1; then
    log "k3s runtime already installed"
    write_k3s_bootstrap_config
    write_k3s_registry_config
    restart_k3s_runtime
    ln -sf /usr/local/bin/k3s /usr/local/bin/kubectl || true
    copy_k3s_kubeconfig
    return
  fi
  write_k3s_bootstrap_config
  write_k3s_registry_config
  log "installing k3s runtime"
  if [ "$RK_DOMESTIC_MIRROR" = "true" ]; then
    curl -sfL https://rancher-mirror.rancher.cn/k3s/k3s-install.sh | INSTALL_K3S_MIRROR=cn INSTALL_K3S_EXEC='--write-kubeconfig-mode=644' sh -s -
  else
    curl -sfL https://get.k3s.io | INSTALL_K3S_EXEC='--write-kubeconfig-mode=644' sh -
  fi
  k3s kubectl get nodes
  ln -sf /usr/local/bin/k3s /usr/local/bin/kubectl || true
  copy_k3s_kubeconfig
}

prepare_directories() {
  mkdir -p "$RK_TARGET_PATH/releases" "$RK_TARGET_PATH/cache/m2" "$RK_TARGET_PATH/cache/npm" "$RK_TARGET_PATH/images" "$RK_TARGET_PATH/runtime"
}

prepare_git_credentials() {
  RK_GIT_ASKPASS=""
  if [ -z "$RK_GIT_USERNAME" ] && [ -z "$RK_GIT_TOKEN" ]; then
    return
  fi
  RK_GIT_ASKPASS="$RK_TARGET_PATH/runtime/git-askpass-$RK_RELEASE_ID.sh"
  cat > "$RK_GIT_ASKPASS" <<'EOF_GIT_ASKPASS'
#!/usr/bin/env sh
case "$1" in
  *Username*) printf '%s\n' "$RK_GIT_USERNAME" ;;
  *Password*) printf '%s\n' "$RK_GIT_TOKEN" ;;
  *) printf '\n' ;;
esac
EOF_GIT_ASKPASS
  chmod 700 "$RK_GIT_ASKPASS"
}

git_clone_source() {
  local destination="$1"
  log "attempting anonymous git clone"
  if GIT_TERMINAL_PROMPT=0 git clone --depth 1 --branch "$RK_REPO_BRANCH" "$RK_REPO_URL" "$destination"; then
    return 0
  fi
  rm -rf "$destination"
  if [ -n "\${RK_GIT_ASKPASS:-}" ]; then
    log "retrying git clone with configured credentials"
    GIT_TERMINAL_PROMPT=0 GIT_ASKPASS="$RK_GIT_ASKPASS" git clone --depth 1 --branch "$RK_REPO_BRANCH" "$RK_REPO_URL" "$destination"
    return
  fi
  fail "anonymous git clone failed and no git credentials were configured"
}

checkout_source() {
  local __result_var="$1"
  local resolved_release_dir="$RK_TARGET_PATH/releases/$RK_RELEASE_ID"
  rm -rf "$resolved_release_dir"
  mkdir -p "$resolved_release_dir"
  log "cloning $RK_GIT_PROVIDER source branch $RK_REPO_BRANCH"
  git_clone_source "$resolved_release_dir/src"
  printf -v "$__result_var" '%s' "$resolved_release_dir"
}

write_runtime_env() {
  local release_dir="$1"
  {
    printf 'RK_PUBLIC_BASE_URL=%s\\n' "$(env_quote "$RK_PUBLIC_BASE_URL")"
    printf 'RK_MYSQL_HOST=%s\\n' "$(env_quote "$RK_MYSQL_HOST")"
    printf 'RK_MYSQL_PORT=%s\\n' "$(env_quote "$RK_MYSQL_PORT")"
    printf 'RK_MYSQL_ROOT_PASSWORD=%s\\n' "$(env_quote "$RK_MYSQL_ROOT_PASSWORD")"
    printf 'RK_MYSQL_APP_USER=%s\\n' "$(env_quote "$RK_MYSQL_APP_USER")"
    printf 'RK_MYSQL_APP_PASSWORD=%s\\n' "$(env_quote "$RK_MYSQL_APP_PASSWORD")"
    printf 'RK_JDBC_DATABASE=%s\\n' "$(env_quote "$RK_JDBC_DATABASE")"
    printf 'RK_NACOS_SERVER_ADDR=%s\\n' "$(env_quote "$RK_NACOS_SERVER_ADDR")"
    printf 'RK_NACOS_NAMESPACE=%s\\n' "$(env_quote "$RK_NACOS_NAMESPACE")"
    printf 'RK_NACOS_GROUP=%s\\n' "$(env_quote "$RK_NACOS_GROUP")"
    printf 'RK_NACOS_USERNAME=%s\\n' "$(env_quote "$RK_NACOS_USERNAME")"
    printf 'RK_NACOS_PASSWORD=%s\\n' "$(env_quote "$RK_NACOS_PASSWORD")"
    printf 'RK_REDIS_HOST=%s\\n' "$(env_quote "$RK_REDIS_HOST")"
    printf 'RK_REDIS_PORT=%s\\n' "$(env_quote "$RK_REDIS_PORT")"
    printf 'RK_REDIS_PASSWORD=%s\\n' "$(env_quote "$RK_REDIS_PASSWORD")"
    printf 'RK_REDIS_DATABASE=%s\\n' "$(env_quote "$RK_REDIS_DATABASE")"
    printf 'RK_REDIS_POOL_MAX_ACTIVE=%s\\n' "$(env_quote "$RK_REDIS_POOL_MAX_ACTIVE")"
    printf 'RK_REDIS_POOL_MAX_IDLE=%s\\n' "$(env_quote "$RK_REDIS_POOL_MAX_IDLE")"
    printf 'RK_REDIS_POOL_MIN_IDLE=%s\\n' "$(env_quote "$RK_REDIS_POOL_MIN_IDLE")"
    printf 'RK_RABBITMQ_HOST=%s\\n' "$(env_quote "$RK_RABBITMQ_HOST")"
    printf 'RK_RABBITMQ_PORT=%s\\n' "$(env_quote "$RK_RABBITMQ_PORT")"
    printf 'RK_RABBITMQ_USERNAME=%s\\n' "$(env_quote "$RK_RABBITMQ_USERNAME")"
    printf 'RK_RABBITMQ_PASSWORD=%s\\n' "$(env_quote "$RK_RABBITMQ_PASSWORD")"
    printf 'RK_RABBITMQ_VHOST=%s\\n' "$(env_quote "$RK_RABBITMQ_VHOST")"
    printf 'RK_MINIO_ENDPOINT=%s\\n' "$(env_quote "$RK_MINIO_ENDPOINT")"
    printf 'RK_MINIO_PUBLIC_BASE_URL=%s\\n' "$(env_quote "$RK_MINIO_PUBLIC_BASE_URL")"
    printf 'RK_MINIO_ROOT_USER=%s\\n' "$(env_quote "$RK_MINIO_ROOT_USER")"
    printf 'RK_MINIO_ROOT_PASSWORD=%s\\n' "$(env_quote "$RK_MINIO_ROOT_PASSWORD")"
    printf 'RK_MINIO_ACCESS_KEY=%s\\n' "$(env_quote "$RK_MINIO_ACCESS_KEY")"
    printf 'RK_MINIO_SECRET_KEY=%s\\n' "$(env_quote "$RK_MINIO_SECRET_KEY")"
    printf 'RK_MINIO_BUCKET=%s\\n' "$(env_quote "$RK_MINIO_BUCKET")"
    printf 'RK_SEATA_TX_GROUP=%s\\n' "$(env_quote "$RK_SEATA_TX_GROUP")"
    printf 'RK_XXL_JOB_ADMIN_ADDRESSES=%s\\n' "$(env_quote "$RK_XXL_JOB_ADMIN_ADDRESSES")"
    printf 'RK_ADMIN_USERNAME=%s\\n' "$(env_quote "$RK_ADMIN_USERNAME")"
    printf 'RK_ADMIN_PASSWORD_BCRYPT=%s\\n' "$(env_quote "$RK_ADMIN_PASSWORD_BCRYPT")"
    printf 'RK_ADMIN_EMAIL=%s\\n' "$(env_quote "$RK_ADMIN_EMAIL")"
    printf 'RK_XXL_JOB_ACCESS_TOKEN=%s\\n' "$(env_quote "$RK_XXL_JOB_ACCESS_TOKEN")"
    printf 'RK_XXL_JOB_EXECUTOR_LOG_PATH=%s\\n' "$(env_quote "$RK_XXL_JOB_EXECUTOR_LOG_PATH")"
    printf 'RK_XXL_JOB_LOG_RETENTION_DAYS=%s\\n' "$(env_quote "$RK_XXL_JOB_LOG_RETENTION_DAYS")"
    printf 'RK_COMPOSE_GATEWAY_PORT=%s\\n' "$(env_quote "$RK_COMPOSE_GATEWAY_PORT")"
    printf 'RK_COMPOSE_FRONTEND_PORT=%s\\n' "$(env_quote "$RK_COMPOSE_FRONTEND_PORT")"
    printf 'RK_COMPOSE_NACOS_PORT=%s\\n' "$(env_quote "$RK_COMPOSE_NACOS_PORT")"
    printf 'RK_COMPOSE_RABBITMQ_MANAGEMENT_PORT=%s\\n' "$(env_quote "$RK_COMPOSE_RABBITMQ_MANAGEMENT_PORT")"
    printf 'RK_COMPOSE_MINIO_CONSOLE_PORT=%s\\n' "$(env_quote "$RK_COMPOSE_MINIO_CONSOLE_PORT")"
  } > "$release_dir/.env"
  chmod 600 "$release_dir/.env"
}

render_runtime_file() {
  local source="$1" target="$2" env_file="$3"
  mkdir -p "$(dirname "$target")"
  RK_ENV_FILE="$env_file" awk '
    BEGIN {
      while ((getline line < ENVIRON["RK_ENV_FILE"]) > 0) {
        if (line !~ /^#/ && line ~ /=/) {
          key=line
          sub(/=.*/, "", key)
          value=line
          sub(/^[^=]*=/, "", value)
          if (value ~ /^'\''.*'\''$/) value=substr(value, 2, length(value) - 2)
          if (value ~ /^".*"$/) value=substr(value, 2, length(value) - 2)
          values[key]=value
        }
      }
    }
    {
      line=$0
      while (match(line, /\\$\\{RK_[A-Z0-9_]+\\}/)) {
        key=substr(line, RSTART + 2, RLENGTH - 3)
        replacement=(key in values) ? values[key] : "<CHANGE_ME:" key ">"
        line=substr(line, 1, RSTART - 1) replacement substr(line, RSTART + RLENGTH)
      }
      print line
    }
  ' "$source" > "$target"
}

append_normalized_sql_file() {
  local source="$1" target="$2" normalized
  normalized="$(mktemp)"
  if iconv -f UTF-8 -t UTF-8 "$source" > "$normalized" 2>/dev/null; then
    cat "$normalized" >> "$target"
  elif iconv -f GB18030 -t UTF-8 "$source" > "$normalized" 2>/dev/null; then
    cat "$normalized" >> "$target"
  else
    rm -f "$normalized"
    fail "failed to normalize SQL file encoding: $source"
  fi
  rm -f "$normalized"
}

write_fresh_install_seed_sql() {
  local release_dir="$1" seed_dir
  seed_dir="$release_dir/runtime/sql/seed-system-only"
  mkdir -p "$seed_dir"
  cat > "$seed_dir/01-system-tenant-admin.sql.tpl" <<'EOF_SEED'
-- RK-Web fresh install seed: system tenant only.
USE rk_user;
INSERT IGNORE INTO rk_tenant (id, tenant_code, tenant_name, logo_url, description, contact_person, contact_phone, contact_email, status, expire_time, create_time, update_time, creator, updater, is_deleted)
VALUES (1, 'system', 'RK System', NULL, 'System management tenant', 'system', NULL, '\${RK_ADMIN_EMAIL}', 1, '2099-12-31 23:59:59', NOW(), NOW(), 1, 1, 0);

USE rk_auth;
INSERT IGNORE INTO user (id, username, password, tenant_id, status, create_time, update_time, is_deleted)
VALUES (1, '\${RK_ADMIN_USERNAME}', '\${RK_ADMIN_PASSWORD_BCRYPT}', 1, 1, NOW(), NOW(), 0);

INSERT IGNORE INTO role (id, tenant_id, role_name, code, role_code, description, sort, type, status, dep_id, create_time, update_time, creator, updater, is_deleted)
VALUES (1, 1, 'Super Administrator', 'ADMIN', 'ADMIN', 'System super administrator', 1, 0, 1, 1, NOW(), NOW(), 1, 1, 0);

INSERT IGNORE INTO account_role (account_id, role_id, create_time)
VALUES (1, 1, NOW());

USE rk_user;
INSERT IGNORE INTO rk_user (id, tenant_id, username, password, real_name, nickname, email, user_type, status, join_time, create_time, update_time, creator, updater, is_deleted)
VALUES (1, 1, '\${RK_ADMIN_USERNAME}', '\${RK_ADMIN_PASSWORD_BCRYPT}', 'RK System', 'RK System', '\${RK_ADMIN_EMAIL}', 1, 1, NOW(), NOW(), NOW(), 1, 1, 0);
EOF_SEED
}

write_create_databases_sql() {
  local release_dir="$1" schema_dir
  schema_dir="$release_dir/runtime/sql/schema"
  mkdir -p "$schema_dir"
  cat > "$schema_dir/00-create-databases.sql.tpl" <<'EOF_DB'
CREATE DATABASE IF NOT EXISTS rk_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_content DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_activity DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_message DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_file DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_search DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_data DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_pay DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_trade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS rk_exam DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS nacos DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS seata DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS xxl_job DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS '\${RK_MYSQL_APP_USER}'@'%' IDENTIFIED BY '\${RK_MYSQL_APP_PASSWORD}';
GRANT ALL PRIVILEGES ON rk_auth.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_user.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_content.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_activity.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_message.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_file.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_search.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_data.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_pay.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_trade.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON rk_exam.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON nacos.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON seata.* TO '\${RK_MYSQL_APP_USER}'@'%';
GRANT ALL PRIVILEGES ON xxl_job.* TO '\${RK_MYSQL_APP_USER}'@'%';
FLUSH PRIVILEGES;
EOF_DB
}

ensure_schema_database_entry() {
  local schema_dir="$1" db="$2" db_file
  db_file="$schema_dir/00-create-databases.sql.tpl"
  [ -n "$db" ] || return 0
  if grep -Fq "CREATE DATABASE IF NOT EXISTS $db " "$db_file"; then
    return 0
  fi
  {
    printf 'CREATE DATABASE IF NOT EXISTS \`%s\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\\n' "$db"
    printf 'GRANT ALL PRIVILEGES ON \`%s\`.* TO '\\''\${RK_MYSQL_APP_USER}'\\''@'\\''%%'\\'';\\n' "$db"
  } >> "$db_file"
}

copy_fresh_install_schema_file() {
  local schema_dir="$1" db="$2" source_file="$3" target
  target="$schema_dir/10-\${db}-schema.sql"
  if [ -f "$target" ]; then
    log "schema $db already copied; newer infrastructure migration schema takes precedence"
    return 0
  fi
  ensure_schema_database_entry "$schema_dir" "$db"
  printf 'USE \`%s\`;\\n' "$db" > "$target"
  append_normalized_sql_file "$source_file" "$target"
}

copy_fresh_install_schema_exports() {
  local release_dir="$1" src schema_dir copied_any
  src="$release_dir/src"
  schema_dir="$release_dir/runtime/sql/schema"
  mkdir -p "$schema_dir"
  copied_any=false
  if [ -d "$src/infrastructure-migration/mysql/export" ]; then
    for source_file in "$src"/infrastructure-migration/mysql/export/*-schema.sql; do
      [ -f "$source_file" ] || continue
      local db
      db="$(basename "$source_file" -schema.sql)"
      copy_fresh_install_schema_file "$schema_dir" "$db" "$source_file"
      copied_any=true
    done
  fi
  if [ -d "$src/database/mysql_export_20260401" ]; then
    for source_file in "$src"/database/mysql_export_20260401/*/schema.sql; do
      [ -f "$source_file" ] || continue
      local db
      db="$(basename "$(dirname "$source_file")")"
      copy_fresh_install_schema_file "$schema_dir" "$db" "$source_file"
      copied_any=true
    done
  fi
  [ "$copied_any" = "true" ] || log "no exported schema source found; using installer schema only"
}

copy_fresh_install_nacos_exports() {
  local release_dir="$1" src target_dir source_root
  src="$release_dir/src"
  target_dir="$release_dir/runtime/nacos/DEFAULT_GROUP"
  mkdir -p "$target_dir"
  source_root=""
  if [ -d "$src/installer/templates/nacos/DEFAULT_GROUP" ]; then
    source_root="$src/installer/templates/nacos/DEFAULT_GROUP"
  elif [ -d "$src/infrastructure-migration/nacos/export/configs/DEFAULT_GROUP" ]; then
    source_root="$src/infrastructure-migration/nacos/export/configs/DEFAULT_GROUP"
  elif [ -d "$src/nacosConfig/DEFAULT_GROUP" ]; then
    source_root="$src/nacosConfig/DEFAULT_GROUP"
  fi
  if [ -z "$source_root" ]; then
    log "no Nacos export source found; using installer overrides only"
    return 0
  fi
  log "copying fresh install Nacos configs from $source_root"
  for source_file in "$source_root"/*; do
    [ -f "$source_file" ] || continue
    case "$(basename "$source_file")" in
      *.yaml|*.yml|*.properties|*.tpl|service.vgroupMapping.*)
        cp "$source_file" "$target_dir/"
        ;;
    esac
  done
}

write_fresh_install_nacos_overrides() {
  local release_dir="$1" target_dir
  target_dir="$release_dir/runtime/nacos/DEFAULT_GROUP"
  mkdir -p "$target_dir"
  cat > "$target_dir/rk-shared-mybatis.yaml.tpl" <<'EOF_NACOS_MYBATIS'
spring:
  datasource:
    url: jdbc:mysql://\${RK_MYSQL_HOST}:\${RK_MYSQL_PORT}/\${rk.jdbc.database:\${RK_JDBC_DATABASE}}?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: \${RK_MYSQL_APP_USER}
    password: \${RK_MYSQL_APP_PASSWORD}
mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: false
    call-setters-on-nulls: true
    jdbc-type-for-null: 'NULL'
  global-config:
    db-config:
      id-type: auto
      logic-delete-field: is_deleted
      logic-delete-value: 1
      logic-not-delete-value: 0
EOF_NACOS_MYBATIS
  cat > "$target_dir/rk-shared-redis.yaml.tpl" <<'EOF_NACOS_REDIS'
spring:
  redis:
    host: \${RK_REDIS_HOST}
    port: \${RK_REDIS_PORT}
    password: \${RK_REDIS_PASSWORD}
    database: \${RK_REDIS_DATABASE}
    connect-timeout: 10000
    timeout: 3000
    lettuce:
      pool:
        max-active: \${RK_REDIS_POOL_MAX_ACTIVE}
        max-idle: \${RK_REDIS_POOL_MAX_IDLE}
        min-idle: \${RK_REDIS_POOL_MIN_IDLE}
        max-wait: -1ms
  cache:
    type: redis
    redis:
      time-to-live: 30m
      cache-null-values: false
EOF_NACOS_REDIS
  cat > "$target_dir/rk-shared-mq.yaml.tpl" <<'EOF_NACOS_MQ'
spring:
  rabbitmq:
    host: \${RK_RABBITMQ_HOST}
    port: \${RK_RABBITMQ_PORT}
    username: \${RK_RABBITMQ_USERNAME}
    password: \${RK_RABBITMQ_PASSWORD}
    virtual-host: \${RK_RABBITMQ_VHOST}
    listener:
      simple:
        prefetch: 1
        acknowledge-mode: manual
EOF_NACOS_MQ
  cat > "$target_dir/rk-shared-minio.yaml.tpl" <<'EOF_NACOS_MINIO'
minio:
  endpoint: \${RK_MINIO_ENDPOINT}
  accessKey: \${RK_MINIO_ACCESS_KEY}
  secretKey: \${RK_MINIO_SECRET_KEY}
  bucketName: \${RK_MINIO_BUCKET}
  publicBaseUrl: \${RK_MINIO_PUBLIC_BASE_URL}
rk:
  minio:
    endpoint: \${RK_MINIO_ENDPOINT}
    access-key: \${RK_MINIO_ACCESS_KEY}
    secret-key: \${RK_MINIO_SECRET_KEY}
    bucket: \${RK_MINIO_BUCKET}
    public-base-url: \${RK_MINIO_PUBLIC_BASE_URL}
EOF_NACOS_MINIO
  cat > "$target_dir/rk-shared-xxljob.yaml.tpl" <<'EOF_NACOS_XXLJOB'
xxl:
  job:
    admin:
      addresses: \${RK_XXL_JOB_ADMIN_ADDRESSES}
    accessToken: \${RK_XXL_JOB_ACCESS_TOKEN}
    executor:
      appname: \${spring.application.name}
      port: 9999
      logpath: \${RK_XXL_JOB_EXECUTOR_LOG_PATH}
      logretentiondays: \${RK_XXL_JOB_LOG_RETENTION_DAYS}
tj:
  xxl-job:
    admin:
      address: \${RK_XXL_JOB_ADMIN_ADDRESSES}
    access-token: \${RK_XXL_JOB_ACCESS_TOKEN}
    executor:
      app-name: \${spring.application.name}
      port: 9999
      log-path: \${RK_XXL_JOB_EXECUTOR_LOG_PATH}
      log-retention-days: \${RK_XXL_JOB_LOG_RETENTION_DAYS}
EOF_NACOS_XXLJOB
  cat > "$target_dir/rk-shared-seata.yaml.tpl" <<'EOF_NACOS_SEATA'
seata:
  enabled: false
  application-id: \${spring.application.name}
  tx-service-group: \${RK_SEATA_TX_GROUP}
  service:
    vgroup-mapping:
      default_tx_group: default
      rk-auth-tx-group: default
      rk-content-tx-group: default
      rk-data-tx-group: default
      rk-exam-tx-group: default
      rk-gateway-tx-group: default
      rk-activity-tx-group: default
      rk-file-tx-group: default
      rk-message-tx-group: default
      rk-pay-tx-group: default
      rk-search-tx-group: default
      rk-trade-tx-group: default
      rk-user-tx-group: default
    grouplist:
      default: rk-seata:8091
  config:
    type: file
  registry:
    type: file
spring:
  cloud:
    seata:
      enabled: false
      tx-service-group: \${RK_SEATA_TX_GROUP}
EOF_NACOS_SEATA
  cat > "$target_dir/rk-user.yaml.tpl" <<'EOF_NACOS_USER'
rk:
  public-base-url: \${RK_PUBLIC_BASE_URL}
  email:
    invitation:
      base-url: \${RK_PUBLIC_BASE_URL}
    alumni-profile:
      base-url: \${RK_PUBLIC_BASE_URL}
  jdbc:
    database: rk_user
tj:
  auth:
    resource:
      enable: true
      excludeLoginPaths:
        - /tenants/**
        - /v2/**
        - /v3/**
        - /swagger-resources/**
        - /webjars/**
        - /doc.html
        - /users/**
        - /students/register
EOF_NACOS_USER
  cat > "$target_dir/seataServer.properties.tpl" <<'EOF_NACOS_SEATA_PROPERTIES'
store.mode=db
store.lock.mode=db
store.session.mode=db
store.db.dbType=mysql
store.db.driverClassName=com.mysql.cj.jdbc.Driver
store.db.url=jdbc:mysql://\${RK_MYSQL_HOST}:\${RK_MYSQL_PORT}/seata?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai&rewriteBatchedStatements=true
store.db.user=\${RK_MYSQL_APP_USER}
store.db.password=\${RK_MYSQL_APP_PASSWORD}
transport.serialization=seata
transport.compressor=none
service.default.grouplist=rk-seata:8091
EOF_NACOS_SEATA_PROPERTIES
  for tx_group in default_tx_group rk-activity-tx-group rk-auth-tx-group rk-content-tx-group rk-data-tx-group rk-exam-tx-group rk-file-tx-group rk-gateway-tx-group rk-message-tx-group rk-pay-tx-group rk-search-tx-group rk-trade-tx-group rk-user-tx-group; do
    printf 'default\n' > "$target_dir/service.vgroupMapping.$tx_group"
  done
}

write_fresh_install_rabbitmq_assets() {
  local release_dir="$1" rabbit_dir
  rabbit_dir="$release_dir/runtime/rabbitmq"
  mkdir -p "$rabbit_dir"
  cat > "$rabbit_dir/enabled_plugins" <<'EOF_RABBITMQ_PLUGINS'
[rabbitmq_delayed_message_exchange,rabbitmq_management,rabbitmq_prometheus].
EOF_RABBITMQ_PLUGINS
}

write_fresh_install_xxl_job_assets() {
  local release_dir="$1" xxl_dir
  xxl_dir="$release_dir/runtime/xxl-job"
  mkdir -p "$xxl_dir/logs"
  cat > "$xxl_dir/application.properties.tpl" <<'EOF_XXL_JOB_PROPERTIES'
server.port=8880
server.servlet.context-path=/xxl-job-admin
management.server.servlet.context-path=/actuator
management.health.mail.enabled=false
spring.mvc.servlet.load-on-startup=0
spring.mvc.static-path-pattern=/static/**
spring.resources.static-locations=classpath:/static/
spring.freemarker.templateLoaderPath=classpath:/templates/
spring.freemarker.suffix=.ftl
spring.freemarker.charset=UTF-8
spring.freemarker.request-context-attribute=request
spring.freemarker.settings.number_format=0.##########
mybatis.mapper-locations=classpath:/mybatis-mapper/*Mapper.xml
spring.datasource.url=jdbc:mysql://rk-mysql:3306/xxl_job?useUnicode=true&characterEncoding=UTF-8&autoReconnect=true&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
spring.datasource.username=\${RK_MYSQL_APP_USER}
spring.datasource.password=\${RK_MYSQL_APP_PASSWORD}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver
spring.datasource.type=com.zaxxer.hikari.HikariDataSource
spring.datasource.hikari.minimum-idle=2
spring.datasource.hikari.maximum-pool-size=10
spring.datasource.hikari.auto-commit=true
spring.datasource.hikari.idle-timeout=30000
spring.datasource.hikari.pool-name=HikariCP
spring.datasource.hikari.max-lifetime=900000
spring.datasource.hikari.connection-timeout=10000
spring.datasource.hikari.connection-test-query=SELECT 1
spring.mail.host=
spring.mail.port=25
spring.mail.username=
spring.mail.from=
spring.mail.password=
spring.mail.properties.mail.smtp.auth=false
spring.mail.properties.mail.smtp.starttls.enable=false
spring.mail.properties.mail.smtp.starttls.required=false
xxl.job.accessToken=\${RK_XXL_JOB_ACCESS_TOKEN}
xxl.job.i18n=zh_CN
xxl.job.triggerpool.fast.max=200
xxl.job.triggerpool.slow.max=100
xxl.job.logretentiondays=\${RK_XXL_JOB_LOG_RETENTION_DAYS}
EOF_XXL_JOB_PROPERTIES
}

prepare_fresh_install_assets() {
  local release_dir="$1" src
  src="$release_dir/src"
  write_create_databases_sql "$release_dir"
  write_fresh_install_seed_sql "$release_dir"
  mkdir -p "$release_dir/runtime/sql/schema" "$release_dir/runtime/nacos/DEFAULT_GROUP"
  copy_fresh_install_schema_exports "$release_dir"
  copy_fresh_install_nacos_exports "$release_dir"
  write_fresh_install_nacos_overrides "$release_dir"
  write_fresh_install_rabbitmq_assets "$release_dir"
  write_fresh_install_xxl_job_assets "$release_dir"
  if [ -d "$src/installer/sql/schema-source" ]; then
    cp "$src"/installer/sql/schema-source/*.sql "$release_dir/runtime/sql/schema/"
  fi
}

render_xxl_job_runtime_file() {
  local release_dir="$1" env_file runtime_dir
  env_file="$release_dir/.env"
  runtime_dir="$release_dir/runtime/rendered/xxl-job"
  rm -rf "$runtime_dir"
  mkdir -p "$runtime_dir"
  render_runtime_file "$release_dir/runtime/xxl-job/application.properties.tpl" "$runtime_dir/application.properties" "$env_file"
}

mysql_exec_compose() {
  local release_dir="$1"
  (cd "$release_dir" && docker compose --env-file .env -p "$RK_COMPOSE_PROJECT" -f docker-compose.yaml exec -T -e MYSQL_PWD="$RK_MYSQL_ROOT_PASSWORD" rk-mysql mysql -uroot)
}

wait_for_compose_mysql() {
  local release_dir="$1" attempt=0
  log "waiting for compose MySQL"
  until (cd "$release_dir" && docker compose --env-file .env -p "$RK_COMPOSE_PROJECT" -f docker-compose.yaml exec -T -e MYSQL_PWD="$RK_MYSQL_ROOT_PASSWORD" rk-mysql mysql -uroot -e "SELECT 1" >/dev/null 2>&1); do
    attempt=$((attempt + 1))
    [ "$attempt" -lt 90 ] || fail "MySQL did not become ready"
    sleep 2
  done
}

initialize_fresh_database() {
  local release_dir="$1" env_file runtime_dir
  env_file="$release_dir/.env"
  runtime_dir="$release_dir/runtime/rendered"
  log "initializing fresh database schema and system tenant"
  rm -rf "$runtime_dir/sql"
  mkdir -p "$runtime_dir/sql/schema" "$runtime_dir/sql/seed-system-only"
  for file in "$release_dir"/runtime/sql/schema/*.sql "$release_dir"/runtime/sql/schema/*.sql.tpl; do
    [ -f "$file" ] || continue
    local base
    base="$(basename "$file" .tpl)"
    render_runtime_file "$file" "$runtime_dir/sql/schema/$base" "$env_file"
  done
  for file in "$release_dir"/runtime/sql/seed-system-only/*.sql "$release_dir"/runtime/sql/seed-system-only/*.sql.tpl; do
    [ -f "$file" ] || continue
    local base
    base="$(basename "$file" .tpl)"
    render_runtime_file "$file" "$runtime_dir/sql/seed-system-only/$base" "$env_file"
  done
  for file in "$runtime_dir"/sql/schema/*.sql; do
    [ -f "$file" ] || continue
    log "importing schema $(basename "$file")"
    mysql_exec_compose "$release_dir" < "$file"
  done
  for file in "$runtime_dir"/sql/seed-system-only/*.sql; do
    [ -f "$file" ] || continue
    log "importing seed $(basename "$file")"
    mysql_exec_compose "$release_dir" < "$file"
  done
}

wait_for_compose_nacos() {
  local attempt=0
  log "waiting for Nacos"
  until curl -fsS "$(nacos_ready_url 8848)" >/dev/null 2>&1; do
    attempt=$((attempt + 1))
    [ "$attempt" -lt 90 ] || fail "Nacos did not become ready"
    sleep 2
  done
}

render_nacos_runtime_files() {
  local release_dir="$1" runtime_dir="$2" env_file
  env_file="$release_dir/.env"
  rm -rf "$runtime_dir"
  mkdir -p "$runtime_dir"
  for file in "$release_dir"/runtime/nacos/DEFAULT_GROUP/*.yaml "$release_dir"/runtime/nacos/DEFAULT_GROUP/*.yml "$release_dir"/runtime/nacos/DEFAULT_GROUP/*.properties "$release_dir"/runtime/nacos/DEFAULT_GROUP/*.tpl "$release_dir"/runtime/nacos/DEFAULT_GROUP/service.vgroupMapping.*; do
    [ -f "$file" ] || continue
    local base
    base="$(basename "$file" .tpl)"
    render_runtime_file "$file" "$runtime_dir/$base" "$env_file"
  done
}

publish_nacos_runtime_file() {
  local file="$1" port="$2" nacos_type
  case "$file" in
    *.yaml|*.yml) nacos_type="yaml" ;;
    *.properties) nacos_type="properties" ;;
    *) nacos_type="text" ;;
  esac
  curl -fsS -X POST "http://127.0.0.1:$port/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$(basename "$file")" \
    --data-urlencode "group=DEFAULT_GROUP" \
    --data-urlencode "type=$nacos_type" \
    --data-urlencode "content=$(cat "$file")" >/dev/null
}

import_nacos_configs() {
  local release_dir="$1" runtime_dir
  runtime_dir="$release_dir/runtime/rendered/nacos/DEFAULT_GROUP"
  log "importing Nacos configs"
  render_nacos_runtime_files "$release_dir" "$runtime_dir"
  for file in "$runtime_dir"/*.yaml "$runtime_dir"/*.yml "$runtime_dir"/*.properties "$runtime_dir"/service.vgroupMapping.*; do
    [ -f "$file" ] || continue
    log "publishing Nacos config $(basename "$file")"
    publish_nacos_runtime_file "$file" 8848
  done
}

mysql_exec_k3s() {
  kubectl -n "$RK_NAMESPACE" exec -i deploy/rk-mysql -- env MYSQL_PWD="$RK_MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -P 3306 -uroot
}

mysql_probe_k3s() {
  timeout 20s kubectl -n "$RK_NAMESPACE" exec deploy/rk-mysql -- env MYSQL_PWD="$RK_MYSQL_ROOT_PASSWORD" mysql -h 127.0.0.1 -P 3306 -uroot -e "SELECT 1" >/dev/null 2>&1
}

wait_for_k3s_mysql() {
  local attempt=0
  log "waiting for k3s MySQL"
  kubectl -n "$RK_NAMESPACE" wait --for=condition=available deployment/rk-mysql --timeout=300s
  log "k3s MySQL deployment is available; probing mysql client"
  until mysql_probe_k3s; do
    attempt=$((attempt + 1))
    [ "$attempt" -lt 90 ] || fail "k3s MySQL did not become ready"
    log "k3s MySQL probe still waiting (attempt $attempt/90)"
    sleep 2
  done
}

initialize_fresh_database_k3s() {
  local release_dir="$1" env_file runtime_dir
  env_file="$release_dir/.env"
  runtime_dir="$release_dir/runtime/rendered"
  log "initializing k3s fresh database schema and system tenant"
  rm -rf "$runtime_dir/sql"
  mkdir -p "$runtime_dir/sql/schema" "$runtime_dir/sql/seed-system-only"
  for file in "$release_dir"/runtime/sql/schema/*.sql "$release_dir"/runtime/sql/schema/*.sql.tpl; do
    [ -f "$file" ] || continue
    local base
    base="$(basename "$file" .tpl)"
    render_runtime_file "$file" "$runtime_dir/sql/schema/$base" "$env_file"
  done
  for file in "$release_dir"/runtime/sql/seed-system-only/*.sql "$release_dir"/runtime/sql/seed-system-only/*.sql.tpl; do
    [ -f "$file" ] || continue
    local base
    base="$(basename "$file" .tpl)"
    render_runtime_file "$file" "$runtime_dir/sql/seed-system-only/$base" "$env_file"
  done
  for file in "$runtime_dir"/sql/schema/*.sql; do
    [ -f "$file" ] || continue
    log "importing k3s schema $(basename "$file")"
    mysql_exec_k3s < "$file"
  done
  for file in "$runtime_dir"/sql/seed-system-only/*.sql; do
    [ -f "$file" ] || continue
    log "importing k3s seed $(basename "$file")"
    mysql_exec_k3s < "$file"
  done
}

wait_for_k3s_nacos() {
  local attempt=0
  log "waiting for k3s Nacos"
  kubectl -n "$RK_NAMESPACE" rollout restart deployment/rk-nacos >/dev/null 2>&1 || true
  kubectl -n "$RK_NAMESPACE" wait --for=condition=available deployment/rk-nacos --timeout=300s
  local port_forward_log="$RK_TARGET_PATH/nacos-port-forward.log"
  local pf_pid=""
  trap 'stop_k3s_nacos_port_forward "$pf_pid"' RETURN
  until curl -fsS "$(nacos_ready_url 18848)" >/dev/null 2>&1; do
    ensure_k3s_nacos_port_forward pf_pid "$port_forward_log" "$pf_pid"
    attempt=$((attempt + 1))
    [ "$attempt" -lt 90 ] || fail "k3s Nacos did not become ready"
    sleep 2
  done
  stop_k3s_nacos_port_forward "$pf_pid"
  trap - RETURN
}

ensure_k3s_nacos_port_forward() {
  local __result_var="$1" port_forward_log="$2" current_pid="\${3:-}"
  if [ -n "$current_pid" ] && kill -0 "$current_pid" >/dev/null 2>&1; then
    printf -v "$__result_var" '%s' "$current_pid"
    return
  fi
  kubectl -n "$RK_NAMESPACE" port-forward svc/rk-nacos 18848:8848 >"$port_forward_log" 2>&1 &
  printf -v "$__result_var" '%s' "$!"
  sleep 1
}

stop_k3s_nacos_port_forward() {
  local pid="\${1:-}"
  [ -n "$pid" ] || return 0
  kill "$pid" >/dev/null 2>&1 || true
}

import_nacos_configs_k3s() {
  local release_dir="$1" runtime_dir
  runtime_dir="$release_dir/runtime/rendered/nacos/DEFAULT_GROUP"
  log "importing k3s Nacos configs"
  render_nacos_runtime_files "$release_dir" "$runtime_dir"
  local port_forward_log="$RK_TARGET_PATH/nacos-import-port-forward.log"
  local pf_pid=""
  trap 'stop_k3s_nacos_port_forward "$pf_pid"' RETURN
  for _ in $(seq 1 30); do
    ensure_k3s_nacos_port_forward pf_pid "$port_forward_log" "$pf_pid"
    curl -fsS "$(nacos_ready_url 18848)" >/dev/null 2>&1 && break
    sleep 2
  done
  for file in "$runtime_dir"/*.yaml "$runtime_dir"/*.yml "$runtime_dir"/*.properties "$runtime_dir"/service.vgroupMapping.*; do
    [ -f "$file" ] || continue
    log "publishing k3s Nacos config $(basename "$file")"
    publish_nacos_runtime_file "$file" 18848
  done
  stop_k3s_nacos_port_forward "$pf_pid"
  trap - RETURN
}

write_backend_dockerfile() {
  local dir="$1"
  cat > "$dir/Dockerfile" <<'EOF'
ARG BASE_IMAGE
FROM \${BASE_IMAGE}
WORKDIR /app
COPY app.jar /app/app.jar
ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "java \${JAVA_OPTS} -jar /app/app.jar"]
EOF
}

write_frontend_dockerfile() {
  local dir="$1"
  cat > "$dir/Dockerfile" <<'EOF'
ARG BASE_IMAGE
FROM \${BASE_IMAGE}
COPY default.conf /etc/nginx/conf.d/default.conf
COPY dist/ /usr/share/nginx/html/
EOF
  cat > "$dir/default.conf" <<'EOF'
server {
  listen 80;
  server_name _;
  root /usr/share/nginx/html;
  index index.html;
  client_max_body_size 64m;
  location /api { proxy_pass http://rk-gateway:10010; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
  location /auth { proxy_pass http://rk-gateway:10010; proxy_set_header Host $host; proxy_set_header X-Real-IP $remote_addr; }
  location /minio-files/ { proxy_pass http://rk-minio:9000/; }
  location / { try_files $uri $uri/ /index.html; }
}
EOF
}

build_compose_service_image() {
  local release_dir="$1" service="$2" module="$3" jar_dir="$4" image="$5"
  local src build_dir
  src="$release_dir/src"
  build_dir="$release_dir/build/$service"
  rm -rf "$build_dir"
  mkdir -p "$build_dir"
  if [ "$service" = "frontend" ]; then
    log "building frontend dist"
    RK_DOCKER_RUN_SCRIPT="npm ci --cache /root/.npm --registry https://registry.npmmirror.com && npm run build" docker_run_with_image_candidates "$RK_HOST_NODE_IMAGE_CANDIDATES" -v "$src:/workspace" -v "$RK_TARGET_PATH/cache/npm:/root/.npm" -w "/workspace/$module"
    mkdir -p "$build_dir/dist"
    cp -R "$src/$module/dist/." "$build_dir/dist/"
    write_frontend_dockerfile "$build_dir"
    docker_build_with_base_image_candidates "$RK_HOST_FRONTEND_BASE_IMAGE_CANDIDATES" "$build_dir" -t "$image:$RK_RELEASE_ID" -t "$image:latest"
  else
    log "building backend module $module"
    RK_DOCKER_RUN_SCRIPT="mvn -Dmaven.repo.local=/root/.m2/repository -pl '$module' -am -DskipTests clean package" docker_run_with_image_candidates "$RK_HOST_MAVEN_IMAGE_CANDIDATES" -v "$src:/workspace" -v "$RK_TARGET_PATH/cache/m2:/root/.m2/repository" -w /workspace
    cp "$(find "$src/$jar_dir/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)" "$build_dir/app.jar"
    write_backend_dockerfile "$build_dir"
    docker_build_with_base_image_candidates "$RK_HOST_BACKEND_BASE_IMAGE_CANDIDATES" "$build_dir" -t "$image:$RK_RELEASE_ID" -t "$image:latest"
  fi
}

build_k3s_service_image() {
  local release_dir="$1" service="$2" module="$3" jar_dir="$4" image="$5"
  if [ "$RK_K3S_DOCKERLESS_BUILD" = "true" ]; then
    build_k3s_service_image_dockerless "$release_dir" "$service" "$module" "$jar_dir" "$image"
    return
  fi
  local src build_dir
  src="$release_dir/src"
  build_dir="$release_dir/build/$service"
  rm -rf "$build_dir"
  mkdir -p "$build_dir"
  if [ "$service" = "frontend" ]; then
    log "building frontend dist"
    RK_DOCKER_RUN_SCRIPT="npm ci --cache /root/.npm --registry https://registry.npmmirror.com && npm run build" docker_run_with_image_candidates "$RK_HOST_NODE_IMAGE_CANDIDATES" -v "$src:/workspace" -v "$RK_TARGET_PATH/cache/npm:/root/.npm" -w "/workspace/$module"
    mkdir -p "$build_dir/dist"
    cp -R "$src/$module/dist/." "$build_dir/dist/"
    write_frontend_dockerfile "$build_dir"
  else
    log "building backend module $module"
    RK_DOCKER_RUN_SCRIPT="mvn -Dmaven.repo.local=/root/.m2/repository -pl '$module' -am -DskipTests clean package" docker_run_with_image_candidates "$RK_HOST_MAVEN_IMAGE_CANDIDATES" -v "$src:/workspace" -v "$RK_TARGET_PATH/cache/m2:/root/.m2/repository" -w /workspace
    cp "$(find "$src/$jar_dir/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)" "$build_dir/app.jar"
    write_backend_dockerfile "$build_dir"
  fi
  local image_ref push_ref
  if [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
    image_ref="$RK_REGISTRY_SERVER/$RK_REGISTRY_NAMESPACE/$image:$RK_RELEASE_ID"
    push_ref="$RK_REGISTRY_PUSH_SERVER/$RK_REGISTRY_NAMESPACE/$image:$RK_RELEASE_ID"
    docker_login_registry
    if [ "$service" = "frontend" ]; then
      docker_build_with_base_image_candidates "$RK_HOST_FRONTEND_BASE_IMAGE_CANDIDATES" "$build_dir" -t "$push_ref"
    else
      docker_build_with_base_image_candidates "$RK_HOST_BACKEND_BASE_IMAGE_CANDIDATES" "$build_dir" -t "$push_ref"
    fi
    docker push "$push_ref"
  else
    image_ref="$image:$RK_RELEASE_ID"
    mkdir -p "$RK_TARGET_PATH/images/$RK_RELEASE_ID"
    if [ "$service" = "frontend" ]; then
      docker_build_with_base_image_candidates "$RK_HOST_FRONTEND_BASE_IMAGE_CANDIDATES" "$build_dir" -t "$image_ref"
    else
      docker_build_with_base_image_candidates "$RK_HOST_BACKEND_BASE_IMAGE_CANDIDATES" "$build_dir" -t "$image_ref"
    fi
    docker save "$image_ref" -o "$RK_TARGET_PATH/images/$RK_RELEASE_ID/$image.tar"
    ctr -a /run/k3s/containerd/containerd.sock -n k8s.io images import "$RK_TARGET_PATH/images/$RK_RELEASE_ID/$image.tar"
  fi
}

ensure_registry_pull_secret() {
  [ "$RK_K3S_IMAGE_MODE" = "registry" ] || return 0
  if [ -n "\${RK_REGISTRY_DOCKER_CONFIG_JSON:-}" ]; then
    printf '%s' "$RK_REGISTRY_DOCKER_CONFIG_JSON" | kubectl -n "$RK_NAMESPACE" create secret generic "$RK_IMAGE_PULL_SECRET" \
      --type=kubernetes.io/dockerconfigjson \
      --from-file=.dockerconfigjson=/dev/stdin \
      --dry-run=client -o yaml | kubectl apply -f -
    return
  fi
  kubectl -n "$RK_NAMESPACE" create secret docker-registry "$RK_IMAGE_PULL_SECRET" \
    --docker-server="$RK_REGISTRY_SERVER" \
    --docker-username="$RK_REGISTRY_USER" \
    --docker-password="$RK_REGISTRY_PASSWORD" \
    --dry-run=client -o yaml | kubectl apply -f -
}

run_k3s_build_job() {
  local job_name="$1" release_dir="$2" image="$3" image_ref="$4" base_image="$5" module="$6" jar_dir="$7"
  local build_dir
  build_dir="$release_dir/build/$job_name"
  mkdir -p "$build_dir"
  cat > "$build_dir/kaniko-job.yaml" <<EOF
apiVersion: batch/v1
kind: Job
metadata:
  name: rk-build-$job_name
  namespace: $RK_NAMESPACE
spec:
  backoffLimit: 0
  template:
    spec:
      restartPolicy: Never
      initContainers:
      - name: prepare-build
        image: $([ "$job_name" = frontend ] && echo "$RK_NODE_IMAGE" || echo "$RK_MAVEN_IMAGE")
        command: ["sh", "-c"]
        args:
        - |-
          set -e &&
          rm -rf /workspace/build/* &&
          if [ "$job_name" = frontend ]; then
            cd /workspace/src/$module &&
            npm ci --cache /workspace/npm-cache --registry https://registry.npmmirror.com &&
            npm run build &&
            mkdir -p /workspace/build/dist &&
            cp -R dist/. /workspace/build/dist/ &&
            cat > /workspace/build/Dockerfile <<'EOF_FRONTEND_DOCKERFILE'
          ARG BASE_IMAGE
          FROM \\\${BASE_IMAGE}
          COPY default.conf /etc/nginx/conf.d/default.conf
          COPY dist/ /usr/share/nginx/html/
          EOF_FRONTEND_DOCKERFILE
            cat > /workspace/build/default.conf <<'EOF_FRONTEND_NGINX'
          server {
            listen 80;
            server_name _;
            root /usr/share/nginx/html;
            index index.html;
            client_max_body_size 64m;
            location /api { proxy_pass http://rk-gateway:10010; proxy_set_header Host \\\$host; proxy_set_header X-Real-IP \\\$remote_addr; }
            location /auth { proxy_pass http://rk-gateway:10010; proxy_set_header Host \\\$host; proxy_set_header X-Real-IP \\\$remote_addr; }
            location /minio-files/ { proxy_pass http://rk-minio:9000/; }
            location / { try_files \\\$uri \\\$uri/ /index.html; }
          }
          EOF_FRONTEND_NGINX
          else
            cd /workspace/src &&
            mvn -Dmaven.repo.local=/workspace/m2 -pl "$module" -am -DskipTests clean package &&
            mkdir -p /workspace/build &&
            cp "\\\$(find "/workspace/src/$jar_dir/target" -maxdepth 1 -type f -name '*.jar' ! -name 'original-*.jar' | head -n 1)" /workspace/build/app.jar &&
            cat > /workspace/build/Dockerfile <<'EOF_BACKEND_DOCKERFILE'
          ARG BASE_IMAGE
          FROM \\\${BASE_IMAGE}
          WORKDIR /app
          COPY app.jar /app/app.jar
          ENV JAVA_OPTS=""
          ENTRYPOINT ["sh", "-c", "java \\\${JAVA_OPTS} -jar /app/app.jar"]
          EOF_BACKEND_DOCKERFILE
          fi
        volumeMounts:
        - name: source-dir
          mountPath: /workspace/src
        - name: build-dir
          mountPath: /workspace/build
EOF
  if [ "$job_name" = frontend ]; then
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
        - name: npm-cache
          mountPath: /workspace/npm-cache
EOF
  else
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
        - name: maven-cache
          mountPath: /workspace/m2
EOF
  fi
  cat >> "$build_dir/kaniko-job.yaml" <<EOF
      containers:
      - name: kaniko
        image: $RK_KANIKO_IMAGE
        args:
        - --context=dir:///workspace/build
        - --dockerfile=/workspace/build/Dockerfile
        - --destination=$image_ref
        - --build-arg=BASE_IMAGE=$base_image
EOF
  if [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
        - --push-retry=3
EOF
    if [ "$RK_REGISTRY_INSECURE" = "true" ]; then
      cat >> "$build_dir/kaniko-job.yaml" <<EOF
        - --insecure
        - --skip-tls-verify
EOF
    fi
  else
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
        - --no-push
        - --tarPath=/images/$image.tar
EOF
  fi
  cat >> "$build_dir/kaniko-job.yaml" <<EOF
        volumeMounts:
        - name: build-dir
          mountPath: /workspace/build
        - name: image-dir
          mountPath: /images
EOF
  if [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
        - name: registry-config
          mountPath: /kaniko/.docker/config.json
          subPath: .dockerconfigjson
          readOnly: true
EOF
  fi
  cat >> "$build_dir/kaniko-job.yaml" <<EOF
      volumes:
      - name: source-dir
        hostPath:
          path: $release_dir/src
          type: Directory
      - name: build-dir
        hostPath:
          path: $build_dir
          type: Directory
      - name: image-dir
        hostPath:
          path: $RK_TARGET_PATH/images/$RK_RELEASE_ID
          type: DirectoryOrCreate
EOF
  if [ "$job_name" = frontend ]; then
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
      - name: npm-cache
        hostPath:
          path: $RK_TARGET_PATH/cache/npm
          type: DirectoryOrCreate
EOF
  else
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
      - name: maven-cache
        hostPath:
          path: $RK_TARGET_PATH/cache/m2
          type: DirectoryOrCreate
EOF
  fi
  if [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
    cat >> "$build_dir/kaniko-job.yaml" <<EOF
      - name: registry-config
        secret:
          secretName: $RK_IMAGE_PULL_SECRET
EOF
  fi
  cat >> "$build_dir/kaniko-job.yaml" <<'EOF'
EOF
  kubectl -n "$RK_NAMESPACE" delete job "rk-build-$job_name" --ignore-not-found=true
  kubectl apply -f "$build_dir/kaniko-job.yaml"
  kubectl wait --for=condition=complete "job/rk-build-$job_name" -n "$RK_NAMESPACE" --timeout=1800s
}

build_k3s_service_image_dockerless() {
  local release_dir="$1" service="$2" module="$3" jar_dir="$4" image="$5"
  log "building $service image in k3s build job"
  mkdir -p "$release_dir/build/$service" "$RK_TARGET_PATH/images/$RK_RELEASE_ID" "$RK_TARGET_PATH/cache/m2" "$RK_TARGET_PATH/cache/npm"
  local image_ref
  image_ref="$image:$RK_RELEASE_ID"
  if [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
    image_ref="$RK_REGISTRY_SERVER/$RK_REGISTRY_NAMESPACE/$image:$RK_RELEASE_ID"
  fi
  ensure_registry_pull_secret
  run_k3s_build_job "$service" "$release_dir" "$image" "$image_ref" "$([ "$service" = frontend ] && echo "$RK_FRONTEND_BASE_IMAGE" || echo "$RK_BACKEND_BASE_IMAGE")" "$module" "$jar_dir"
  if [ "$RK_K3S_IMAGE_MODE" = "local-tar" ]; then
    ctr -a /run/k3s/containerd/containerd.sock -n k8s.io images import "$RK_TARGET_PATH/images/$RK_RELEASE_ID/$image.tar"
  fi
}

for_each_service() {
  printf '%s\\n' "$RK_SERVICE_LINES"
}

build_all_compose_images() {
  local release_dir="$1"
  while IFS='|' read -r name module jar_dir image port container_port; do
    [ -n "$name" ] || continue
    build_compose_service_image "$release_dir" "$name" "$module" "$jar_dir" "$image"
  done < <(for_each_service)
}

build_all_k3s_images() {
  local release_dir="$1"
  while IFS='|' read -r name module jar_dir image port container_port; do
    [ -n "$name" ] || continue
    build_k3s_service_image "$release_dir" "$name" "$module" "$jar_dir" "$image"
  done < <(for_each_service)
}

write_compose_file() {
  local release_dir="$1"
  cat > "$release_dir/docker-compose.yaml" <<'EOF_COMPOSE'
services:
  rk-mysql:
    image: mysql:8.0
    restart: unless-stopped
    environment:
      MYSQL_ROOT_PASSWORD: \${RK_MYSQL_ROOT_PASSWORD}
    volumes: [rk-mysql-data:/var/lib/mysql]
  rk-nacos:
    image: nacos/nacos-server:v2.3.2
    restart: unless-stopped
    depends_on: [rk-mysql]
    ports: ["\${RK_COMPOSE_NACOS_PORT}:8848"]
    environment:
      MODE: standalone
      SPRING_DATASOURCE_PLATFORM: mysql
      MYSQL_SERVICE_HOST: rk-mysql
      MYSQL_SERVICE_DB_NAME: nacos
      MYSQL_SERVICE_USER: root
      MYSQL_SERVICE_PASSWORD: \${RK_MYSQL_ROOT_PASSWORD}
      NACOS_AUTH_ENABLE: "false"
  rk-redis:
    image: redis:7
    restart: unless-stopped
    command: ["redis-server", "--requirepass", "\${RK_REDIS_PASSWORD}"]
  rk-rabbitmq:
    image: $RK_RABBITMQ_IMAGE
    restart: unless-stopped
    environment:
      RABBITMQ_DEFAULT_USER: \${RK_RABBITMQ_USERNAME}
      RABBITMQ_DEFAULT_PASS: \${RK_RABBITMQ_PASSWORD}
      RABBITMQ_DEFAULT_VHOST: \${RK_RABBITMQ_VHOST}
    volumes:
      - ./runtime/rabbitmq/enabled_plugins:/etc/rabbitmq/enabled_plugins:ro
  rk-minio:
    image: minio/minio:RELEASE.2024-05-10T01-41-38Z
    restart: unless-stopped
    command: ["server", "/data", "--console-address", ":9001"]
    environment:
      MINIO_ROOT_USER: \${RK_MINIO_ROOT_USER}
      MINIO_ROOT_PASSWORD: \${RK_MINIO_ROOT_PASSWORD}
    volumes: [rk-minio-data:/data]
  rk-xxl-job:
    image: $RK_XXL_JOB_IMAGE
    restart: unless-stopped
    depends_on: [rk-mysql]
    volumes:
      - ./runtime/rendered/xxl-job/application.properties:/config/application.properties:ro
      - rk-xxl-job-data:/data/applogs
EOF_COMPOSE
  while IFS='|' read -r name module jar_dir image port container_port; do
    [ -n "$name" ] || continue
    local service_name image_name host_port
    service_name="$([ "$name" = frontend ] && echo rk-web-frontend || echo "$name")"
    image_name="$image:$RK_RELEASE_ID"
    host_port="$port"
    [ "$name" = rk-gateway ] && host_port="$RK_COMPOSE_GATEWAY_PORT"
    [ "$name" = frontend ] && host_port="$RK_COMPOSE_FRONTEND_PORT"
    cat >> "$release_dir/docker-compose.yaml" <<EOF_SERVICE
  $service_name:
    image: $image_name
    restart: unless-stopped
    depends_on: [rk-nacos, rk-redis]
    environment:
      SPRING_PROFILES_ACTIVE: prod
      SPRING_CLOUD_NACOS_SERVER_ADDR: rk-nacos:8848
      SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR: rk-nacos:8848
      SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR: rk-nacos:8848
      SPRING_CLOUD_NACOS_CONFIG_NAMESPACE: \\\${RK_NACOS_NAMESPACE}
      SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE: \\\${RK_NACOS_NAMESPACE}
      SPRING_CLOUD_NACOS_CONFIG_GROUP: \\\${RK_NACOS_GROUP}
      SPRING_CLOUD_NACOS_DISCOVERY_GROUP: \\\${RK_NACOS_GROUP}
      DB_HOST: rk-mysql
      DB_USER: root
      DB_PWD: \\\${RK_MYSQL_ROOT_PASSWORD}
      SPRING_REDIS_HOST: rk-redis
      SPRING_REDIS_PASSWORD: \\\${RK_REDIS_PASSWORD}
      SPRING_RABBITMQ_HOST: rk-rabbitmq
      SPRING_RABBITMQ_USERNAME: \\\${RK_RABBITMQ_USERNAME}
      SPRING_RABBITMQ_PASSWORD: \\\${RK_RABBITMQ_PASSWORD}
      SPRING_RABBITMQ_VIRTUAL_HOST: \\\${RK_RABBITMQ_VHOST}
      MINIO_ENDPOINT: http://rk-minio:9000
      MINIO_ACCESS_KEY: \\\${RK_MINIO_ACCESS_KEY}
      MINIO_SECRET_KEY: \\\${RK_MINIO_SECRET_KEY}
      XXL_JOB_ADMIN_ADDRESSES: http://rk-xxl-job:8880/xxl-job-admin
    ports: ["$host_port:$container_port"]
EOF_SERVICE
  done < <(for_each_service)
  cat >> "$release_dir/docker-compose.yaml" <<'EOF_VOLUMES'
volumes:
  rk-mysql-data:
  rk-minio-data:
  rk-xxl-job-data:
EOF_VOLUMES
}

start_compose_middleware() {
  local release_dir="$1"
  log "starting compose middleware"
  (cd "$release_dir" && docker compose --env-file .env -p "$RK_COMPOSE_PROJECT" -f docker-compose.yaml up -d rk-mysql rk-redis rk-rabbitmq rk-minio rk-xxl-job)
}

start_compose_services() {
  local release_dir="$1"
  log "starting compose application services"
  (cd "$release_dir" && docker compose --env-file .env -p "$RK_COMPOSE_PROJECT" -f docker-compose.yaml up -d)
}

deploy_compose() {
  local release_dir="$1"
  prepare_fresh_install_assets "$release_dir"
  write_runtime_env "$release_dir"
  render_xxl_job_runtime_file "$release_dir"
  write_compose_file "$release_dir"
  start_compose_middleware "$release_dir"
  wait_for_compose_mysql "$release_dir"
  initialize_fresh_database "$release_dir"
  (cd "$release_dir" && docker compose --env-file .env -p "$RK_COMPOSE_PROJECT" -f docker-compose.yaml up -d rk-nacos)
  wait_for_compose_nacos
  import_nacos_configs "$release_dir"
  start_compose_services "$release_dir"
}

write_k3s_middleware_manifest() {
  local release_dir="$1" manifest
  manifest="$release_dir/rk-web-k3s-middleware.yaml"
  cat > "$manifest" <<EOF
apiVersion: v1
kind: Namespace
metadata:
  name: $RK_NAMESPACE
---
apiVersion: v1
kind: Secret
metadata:
  name: rk-runtime-secret
  namespace: $RK_NAMESPACE
type: Opaque
stringData:
  RK_MYSQL_ROOT_PASSWORD: "$RK_MYSQL_ROOT_PASSWORD"
  RK_MYSQL_APP_PASSWORD: "$RK_MYSQL_APP_PASSWORD"
  RK_NACOS_PASSWORD: "$RK_NACOS_PASSWORD"
  RK_REDIS_PASSWORD: "$RK_REDIS_PASSWORD"
  RK_RABBITMQ_PASSWORD: "$RK_RABBITMQ_PASSWORD"
  RK_MINIO_ROOT_USER: "$RK_MINIO_ROOT_USER"
  RK_MINIO_ROOT_PASSWORD: "$RK_MINIO_ROOT_PASSWORD"
  RK_MINIO_ACCESS_KEY: "$RK_MINIO_ACCESS_KEY"
  RK_MINIO_SECRET_KEY: "$RK_MINIO_SECRET_KEY"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rk-mysql
  namespace: $RK_NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rk-mysql
  template:
    metadata:
      labels:
        app: rk-mysql
    spec:
      containers:
      - name: rk-mysql
        image: $RK_MYSQL_IMAGE
        ports:
        - containerPort: 3306
        env:
        - name: MYSQL_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_MYSQL_ROOT_PASSWORD
        volumeMounts:
        - name: mysql-data
          mountPath: /var/lib/mysql
      volumes:
      - name: mysql-data
        hostPath:
          path: $RK_TARGET_PATH/k3s-data/$RK_NAMESPACE/mysql
          type: DirectoryOrCreate
---
apiVersion: v1
kind: Service
metadata:
  name: rk-mysql
  namespace: $RK_NAMESPACE
spec:
  selector:
    app: rk-mysql
  ports:
  - port: 3306
    targetPort: 3306
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rk-redis
  namespace: $RK_NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rk-redis
  template:
    metadata:
      labels:
        app: rk-redis
    spec:
      containers:
      - name: rk-redis
        image: $RK_REDIS_IMAGE
        command: ["sh", "-c"]
        args:
        - exec redis-server --requirepass "\$RK_REDIS_PASSWORD"
        ports:
        - containerPort: 6379
        env:
        - name: RK_REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_REDIS_PASSWORD
---
apiVersion: v1
kind: Service
metadata:
  name: rk-redis
  namespace: $RK_NAMESPACE
spec:
  selector:
    app: rk-redis
  ports:
  - port: 6379
    targetPort: 6379
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rk-rabbitmq
  namespace: $RK_NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rk-rabbitmq
  template:
    metadata:
      labels:
        app: rk-rabbitmq
    spec:
      containers:
      - name: rk-rabbitmq
        image: $RK_RABBITMQ_IMAGE
        ports:
        - containerPort: 5672
        - containerPort: 15672
        env:
        - name: RABBITMQ_DEFAULT_USER
          value: "$RK_RABBITMQ_USERNAME"
        - name: RABBITMQ_DEFAULT_PASS
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_RABBITMQ_PASSWORD
        - name: RABBITMQ_DEFAULT_VHOST
          value: "$RK_RABBITMQ_VHOST"
        volumeMounts:
        - name: rabbitmq-enabled-plugins
          mountPath: /etc/rabbitmq/enabled_plugins
          readOnly: true
        - name: rabbitmq-data
          mountPath: /var/lib/rabbitmq
      volumes:
      - name: rabbitmq-enabled-plugins
        hostPath:
          path: $release_dir/runtime/rabbitmq/enabled_plugins
          type: File
      - name: rabbitmq-data
        hostPath:
          path: $RK_TARGET_PATH/k3s-data/$RK_NAMESPACE/rabbitmq
          type: DirectoryOrCreate
---
apiVersion: v1
kind: Service
metadata:
  name: rk-rabbitmq
  namespace: $RK_NAMESPACE
spec:
  selector:
    app: rk-rabbitmq
  ports:
  - name: amqp
    port: 5672
    targetPort: 5672
  - name: management
    port: 15672
    targetPort: 15672
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rk-minio
  namespace: $RK_NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rk-minio
  template:
    metadata:
      labels:
        app: rk-minio
    spec:
      containers:
      - name: rk-minio
        image: $RK_MINIO_IMAGE
        command: ["minio", "server", "/data", "--console-address", ":9001"]
        ports:
        - containerPort: 9000
        - containerPort: 9001
        env:
        - name: MINIO_ROOT_USER
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_MINIO_ROOT_USER
        - name: MINIO_ROOT_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_MINIO_ROOT_PASSWORD
        volumeMounts:
        - name: minio-data
          mountPath: /data
      volumes:
      - name: minio-data
        hostPath:
          path: $RK_TARGET_PATH/k3s-data/$RK_NAMESPACE/minio
          type: DirectoryOrCreate
---
apiVersion: v1
kind: Service
metadata:
  name: rk-minio
  namespace: $RK_NAMESPACE
spec:
  selector:
    app: rk-minio
  ports:
  - name: api
    port: 9000
    targetPort: 9000
  - name: console
    port: 9001
    targetPort: 9001
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rk-nacos
  namespace: $RK_NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rk-nacos
  template:
    metadata:
      labels:
        app: rk-nacos
    spec:
      containers:
      - name: rk-nacos
        image: $RK_NACOS_IMAGE
        ports:
        - containerPort: 8848
        - containerPort: 9848
        - containerPort: 9849
        env:
        - name: MODE
          value: standalone
        - name: PREFER_HOST_MODE
          value: hostname
        - name: SPRING_DATASOURCE_PLATFORM
          value: mysql
        - name: MYSQL_SERVICE_HOST
          value: rk-mysql
        - name: MYSQL_SERVICE_DB_NAME
          value: nacos
        - name: MYSQL_SERVICE_USER
          value: root
        - name: MYSQL_SERVICE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_MYSQL_ROOT_PASSWORD
        - name: NACOS_AUTH_ENABLE
          value: "false"
        volumeMounts:
        - name: nacos-data
          mountPath: /home/nacos/data
      volumes:
      - name: nacos-data
        hostPath:
          path: $RK_TARGET_PATH/k3s-data/$RK_NAMESPACE/nacos
          type: DirectoryOrCreate
---
apiVersion: v1
kind: Service
metadata:
  name: rk-nacos
  namespace: $RK_NAMESPACE
spec:
  selector:
    app: rk-nacos
  ports:
  - name: http
    port: 8848
    targetPort: 8848
  - name: grpc
    port: 9848
    targetPort: 9848
  - name: grpc-raft
    port: 9849
    targetPort: 9849
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: rk-xxl-job
  namespace: $RK_NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: rk-xxl-job
  template:
    metadata:
      labels:
        app: rk-xxl-job
    spec:
      containers:
      - name: rk-xxl-job
        image: $RK_XXL_JOB_IMAGE
        ports:
        - containerPort: 8880
        volumeMounts:
        - name: xxl-job-config
          mountPath: /config/application.properties
          readOnly: true
        - name: xxl-job-logs
          mountPath: /data/applogs
      volumes:
      - name: xxl-job-config
        hostPath:
          path: $release_dir/runtime/rendered/xxl-job/application.properties
          type: File
      - name: xxl-job-logs
        hostPath:
          path: $RK_TARGET_PATH/k3s-data/$RK_NAMESPACE/xxl-job
          type: DirectoryOrCreate
---
apiVersion: v1
kind: Service
metadata:
  name: rk-xxl-job
  namespace: $RK_NAMESPACE
spec:
  selector:
    app: rk-xxl-job
  ports:
  - port: 8880
    targetPort: 8880
---
EOF
  printf '%s\\n' "$manifest"
}

write_k3s_app_manifest() {
  local release_dir="$1" manifest
  manifest="$release_dir/rk-web-k3s-app.yaml"
  : > "$manifest"
  while IFS='|' read -r name module jar_dir image port container_port; do
    [ -n "$name" ] || continue
    local app_name service_type node_port_line image_ref
    app_name="$([ "$name" = frontend ] && echo rk-web-frontend || echo "$name")"
    service_type="ClusterIP"
    node_port_line=""
    if [ "$name" = frontend ]; then
      service_type="NodePort"
      node_port_line="    nodePort: $RK_K3S_FRONTEND_NODE_PORT"
    elif [ "$name" = rk-gateway ]; then
      service_type="NodePort"
      node_port_line="    nodePort: $RK_K3S_GATEWAY_NODE_PORT"
    fi
    image_ref="$image:$RK_RELEASE_ID"
    if [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
      image_ref="$RK_REGISTRY_SERVER/$RK_REGISTRY_NAMESPACE/$image:$RK_RELEASE_ID"
    fi
    cat >> "$manifest" <<EOF
apiVersion: apps/v1
kind: Deployment
metadata:
  name: $app_name
  namespace: $RK_NAMESPACE
spec:
  replicas: 1
  selector:
    matchLabels:
      app: $app_name
  template:
    metadata:
      labels:
        app: $app_name
    spec:
EOF
    if [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
      cat >> "$manifest" <<EOF
      imagePullSecrets:
      - name: $RK_IMAGE_PULL_SECRET
EOF
    fi
    cat >> "$manifest" <<EOF
      containers:
      - name: $app_name
        image: $image_ref
        imagePullPolicy: IfNotPresent
        ports:
        - containerPort: $container_port
        env:
        - name: SPRING_PROFILES_ACTIVE
          value: prod
        - name: SPRING_CLOUD_NACOS_SERVER_ADDR
          value: rk-nacos:8848
        - name: SPRING_CLOUD_NACOS_CONFIG_SERVER_ADDR
          value: rk-nacos:8848
        - name: SPRING_CLOUD_NACOS_DISCOVERY_SERVER_ADDR
          value: rk-nacos:8848
        - name: SPRING_CLOUD_NACOS_CONFIG_NAMESPACE
          value: "$RK_NACOS_NAMESPACE"
        - name: SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE
          value: "$RK_NACOS_NAMESPACE"
        - name: SPRING_CLOUD_NACOS_CONFIG_GROUP
          value: "$RK_NACOS_GROUP"
        - name: SPRING_CLOUD_NACOS_DISCOVERY_GROUP
          value: "$RK_NACOS_GROUP"
        - name: SPRING_CLOUD_NACOS_DISCOVERY_IP
          value: $app_name
        - name: SPRING_CLOUD_NACOS_DISCOVERY_PORT
          value: "$container_port"
        - name: DB_HOST
          value: rk-mysql
        - name: DB_USER
          value: root
        - name: DB_PWD
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_MYSQL_ROOT_PASSWORD
        - name: SPRING_REDIS_HOST
          value: rk-redis
        - name: SPRING_REDIS_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_REDIS_PASSWORD
        - name: SPRING_RABBITMQ_HOST
          value: rk-rabbitmq
        - name: SPRING_RABBITMQ_USERNAME
          value: "$RK_RABBITMQ_USERNAME"
        - name: SPRING_RABBITMQ_PASSWORD
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_RABBITMQ_PASSWORD
        - name: SPRING_RABBITMQ_VIRTUAL_HOST
          value: "$RK_RABBITMQ_VHOST"
        - name: MINIO_ENDPOINT
          value: http://rk-minio:9000
        - name: MINIO_ACCESS_KEY
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_MINIO_ACCESS_KEY
        - name: MINIO_SECRET_KEY
          valueFrom:
            secretKeyRef:
              name: rk-runtime-secret
              key: RK_MINIO_SECRET_KEY
        - name: XXL_JOB_ADMIN_ADDRESSES
          value: http://rk-xxl-job:8880/xxl-job-admin
---
apiVersion: v1
kind: Service
metadata:
  name: $app_name
  namespace: $RK_NAMESPACE
spec:
  type: $service_type
  selector:
    app: $app_name
  ports:
  - port: $container_port
    targetPort: $container_port
$node_port_line
---
EOF
  done < <(for_each_service)
  printf '%s\\n' "$manifest"
}

deploy_k3s() {
  local release_dir="$1"
  export KUBECONFIG=/etc/rancher/k3s/k3s.yaml
  prepare_fresh_install_assets "$release_dir"
  write_runtime_env "$release_dir"
  render_xxl_job_runtime_file "$release_dir"
  ensure_registry_pull_secret
  local middleware_manifest app_manifest
  middleware_manifest="$(write_k3s_middleware_manifest "$release_dir")"
  kubectl apply -f "$middleware_manifest"
  wait_for_k3s_mysql
  initialize_fresh_database_k3s "$release_dir"
  wait_for_k3s_nacos
  import_nacos_configs_k3s "$release_dir"
  app_manifest="$(write_k3s_app_manifest "$release_dir")"
  kubectl apply -f "$app_manifest"
  kubectl -n "$RK_NAMESPACE" get pods
}

rk_install() {
  install_base_packages
  prepare_directories
  prepare_git_credentials
  if [ "$RK_RUNTIME_MODE" = "docker-compose" ]; then
    install_docker_runtime
    open_runtime_firewall_ports
    local release_dir
    checkout_source release_dir
    if [ "$RK_BUILD_ON_REMOTE" = "true" ]; then
      build_all_compose_images "$release_dir"
    else
      log "skipping compose image build; reusing release $RK_RELEASE_ID"
    fi
    if [ "$RK_DEPLOY_SERVICES" = "true" ]; then
      deploy_compose "$release_dir"
    else
      log "skipping compose deployment"
    fi
    return
  fi
  if [ "$RK_RUNTIME_MODE" = "k3s" ]; then
    install_k3s_runtime
    open_runtime_firewall_ports
    check_k3s_node_health
${dockerlessK3s ? '' : '    install_docker_runtime\n'}
    if [ "$RK_K3S_IMAGE_MODE" = "registry" ] && [ "$RK_REGISTRY_PROVIDER" = "harbor" ]; then
      install_docker_runtime
      install_harbor_registry
      write_k3s_registry_config
    elif [ "$RK_K3S_IMAGE_MODE" = "registry" ]; then
      write_k3s_registry_config
    fi
    local release_dir
    checkout_source release_dir
    reset_k3s_namespace_for_fresh_install
    ensure_registry_pull_secret
    if [ "$RK_BUILD_ON_REMOTE" = "true" ]; then
      build_all_k3s_images "$release_dir"
      cleanup_docker_build_cache
    else
      log "skipping k3s image build; reusing release $RK_RELEASE_ID"
    fi
    check_k3s_node_health
    if [ "$RK_DEPLOY_SERVICES" = "true" ]; then
      deploy_k3s "$release_dir"
    else
      log "skipping k3s deployment"
    fi
    return
  fi
  fail "unsupported runtime mode: $RK_RUNTIME_MODE"
}

case "$RK_INSTALL_ACTION" in
  preflight) rk_preflight ;;
  install) rk_install ;;
  *) fail "unsupported action: $RK_INSTALL_ACTION" ;;
esac
`
}

export function buildRemoteEnv(config, action = 'install') {
  return {
    RK_INSTALL_ACTION: action,
    RK_RELEASE_ID: config.releaseId || '',
    RK_PUBLIC_BASE_URL: config.deployment?.publicBaseUrl || 'http://localhost',
    RK_MYSQL_HOST: config.mysql?.host || 'rk-mysql',
    RK_MYSQL_PORT: String(config.mysql?.port || 3306),
    RK_MYSQL_ROOT_PASSWORD: config.mysql?.rootPassword || '',
    RK_MYSQL_APP_USER: config.mysql?.appUser || 'rk_app',
    RK_MYSQL_APP_PASSWORD: config.mysql?.appPassword || '',
    RK_JDBC_DATABASE: config.mysql?.jdbcDatabase || 'rk_user',
    RK_NACOS_SERVER_ADDR: config.nacos?.serverAddr || 'rk-nacos:8848',
    RK_NACOS_NAMESPACE: config.nacos?.namespace || 'public',
    RK_NACOS_GROUP: config.nacos?.group || 'DEFAULT_GROUP',
    RK_NACOS_USERNAME: config.nacos?.username || 'nacos',
    RK_NACOS_PASSWORD: config.nacos?.password || '',
    RK_REDIS_HOST: config.redis?.host || 'rk-redis',
    RK_REDIS_PORT: String(config.redis?.port || 6379),
    RK_REDIS_PASSWORD: config.redis?.password || '',
    RK_REDIS_DATABASE: String(config.redis?.database || 0),
    RK_REDIS_POOL_MAX_ACTIVE: '16',
    RK_REDIS_POOL_MAX_IDLE: '8',
    RK_REDIS_POOL_MIN_IDLE: '1',
    RK_RABBITMQ_HOST: config.rabbitmq?.host || 'rk-rabbitmq',
    RK_RABBITMQ_PORT: String(config.rabbitmq?.port || 5672),
    RK_RABBITMQ_USERNAME: config.rabbitmq?.username || 'rk_app',
    RK_RABBITMQ_PASSWORD: config.rabbitmq?.password || '',
    RK_RABBITMQ_VHOST: config.rabbitmq?.virtualHost || '/rk',
    RK_MINIO_ENDPOINT: config.minio?.endpoint || 'http://rk-minio:9000',
    RK_MINIO_PUBLIC_BASE_URL: config.minio?.publicBaseUrl || 'http://localhost/minio-files/',
    RK_MINIO_ROOT_USER: config.minio?.rootUser || 'rkminio',
    RK_MINIO_ROOT_PASSWORD: config.minio?.rootPassword || '',
    RK_MINIO_ACCESS_KEY: config.minio?.accessKey || '',
    RK_MINIO_SECRET_KEY: config.minio?.secretKey || '',
    RK_MINIO_BUCKET: config.minio?.bucket || 'rk-bucket',
    RK_SEATA_TX_GROUP: config.seata?.txGroup || 'default_tx_group',
    RK_XXL_JOB_ADMIN_ADDRESSES: config.xxlJob?.adminAddress || 'http://rk-xxl-job:8880/xxl-job-admin',
    RK_ADMIN_USERNAME: config.admin?.username || 'admin',
    RK_ADMIN_PASSWORD_BCRYPT: config.adminPasswordBcrypt || '',
    RK_ADMIN_EMAIL: config.admin?.email || 'admin@example.invalid',
    RK_XXL_JOB_ACCESS_TOKEN: config.xxlJob?.accessToken || '',
    RK_XXL_JOB_EXECUTOR_LOG_PATH: config.xxlJob?.executorLogPath || 'logs/xxl-job/jobhandler',
    RK_XXL_JOB_LOG_RETENTION_DAYS: String(config.xxlJob?.logRetentionDays || 30),
    RK_COMPOSE_GATEWAY_PORT: String(config.compose?.gatewayPort || 10010),
    RK_COMPOSE_FRONTEND_PORT: String(config.compose?.frontendPort || 30080),
    RK_COMPOSE_NACOS_PORT: String(config.compose?.nacosPort || 8848),
    RK_COMPOSE_RABBITMQ_MANAGEMENT_PORT: String(config.compose?.rabbitManagementPort || 15672),
    RK_COMPOSE_MINIO_CONSOLE_PORT: String(config.compose?.minioConsolePort || 9001),
    RK_GIT_USERNAME: config.git?.username || '',
    RK_GIT_TOKEN: config.git?.token || '',
    RK_REGISTRY_PROVIDER: config.registry?.provider || 'none',
    RK_REGISTRY_USER: config.registry?.username || '',
    RK_REGISTRY_PASSWORD: config.registry?.password || '',
    RK_REGISTRY_DOCKER_CONFIG_JSON: config.registry?.dockerConfigJson || '',
    RK_REGISTRY_INSECURE: String(config.registry?.insecure === true),
    RK_HARBOR_HTTP_PORT: String(config.registry?.harborHttpPort || 30090)
  }
}

function sh(value) {
  return `'${String(value ?? '').replace(/'/g, `'\\''`)}'`
}

function unique(values) {
  return [...new Set(values.filter((value) => value !== undefined && value !== null && String(value).trim() !== '').map((value) => String(value)))]
}
