import assert from 'node:assert/strict'
import test from 'node:test'

test('remote payload installs harbor and points k3s at local harbor registry', async () => {
  const { buildRemoteInstallScript, buildRemoteEnv } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080' },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)
  const env = buildRemoteEnv(config, 'install')

  assert.match(script, /export TERM="\$\{TERM:-xterm\}"/)
  assert.match(script, /install_harbor_registry\(\)/)
  assert.match(script, /write_k3s_registry_config\(\)/)
  assert.match(script, /RK_REGISTRY_PROVIDER/)
  assert.match(script, /RK_REGISTRY_SERVER='43\.167\.243\.59:30090'/)
  assert.match(script, /RK_REGISTRY_PUSH_SERVER='127\.0\.0\.1:30090'/)
  assert.match(script, /- "http:\/\/\$RK_REGISTRY_PUSH_SERVER"/)
  assert.match(script, /RK_HARBOR_HTTP_PORT/)
  assert.match(script, /projects/)
  assert.match(script, /--skip-tls-verify/)
  assert.equal(env.RK_REGISTRY_USER, 'admin')
  assert.equal(env.RK_REGISTRY_PASSWORD, 'harbor-secret')
})

test('remote payload installs k3s through the cn mirror with bootstrap config files', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)
  const jobStart = script.indexOf('run_k3s_build_job()')
  const jobEnd = script.indexOf('wait_for_k3s_build_job()', jobStart)
  const k3sBuildJob = script.slice(jobStart, jobEnd)

  assert.match(script, /cat > \/etc\/rancher\/k3s\/config.yaml/)
  assert.match(script, /disable:\n  - traefik\n  - local-storage/)
  assert.match(script, /system-default-registry: docker\.1ms\.run/)
  assert.match(script, /https:\/\/rancher-mirror\.rancher\.cn\/k3s\/k3s-install\.sh/)
  assert.match(script, /INSTALL_K3S_MIRROR=cn/)
  assert.match(script, /cp \/etc\/rancher\/k3s\/k3s.yaml "\$HOME\/.kube\/config"/)
})

test('remote payload uses domestic docker repo and bounded harbor downloads', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(script, /RK_DOCKER_CE_REPO_URL='https:\/\/mirrors\.aliyun\.com\/docker-ce\/linux\/centos\/docker-ce\.repo'/)
  assert.match(script, /RK_DOCKER_APT_REPO_URL='https:\/\/mirrors\.aliyun\.com\/docker-ce\/linux\/ubuntu'/)
  assert.match(script, /RK_DOCKER_REGISTRY_MIRROR='https:\/\/docker\.1ms\.run'/)
  assert.match(script, /rm -f \/etc\/yum\.repos\.d\/docker-ce\.repo/)
  assert.doesNotMatch(script, /config-manager --add-repo https:\/\/download\.docker\.com\/linux\/centos\/docker-ce\.repo/)
  assert.match(script, /registry-mirrors/)
  assert.match(script, /download_with_fallback "\$archive" "harbor \$RK_HARBOR_INSTALLER_KIND installer"/)
  assert.match(script, /--connect-timeout 20 --retry 3/)
})

test('remote payload refreshes k3s mirror config when k3s already exists', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(
    script,
    /if have k3s && k3s kubectl get nodes[\s\S]+write_k3s_bootstrap_config[\s\S]+write_k3s_registry_config[\s\S]+restart_k3s_runtime/
  )
})

test('remote payload guards k3s install from node disk pressure and prunes docker build cache', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(script, /check_k3s_node_health\(\)/)
  assert.match(script, /node\.kubernetes\.io\/disk-pressure/)
  assert.match(script, /kubectl get nodes -o wide/)
  assert.match(script, /df -h "\$RK_TARGET_PATH" \/var\/lib\/docker \/var\/lib\/rancher 2>\/dev\/null/)
  assert.match(script, /fail "k3s node has DiskPressure/)
  assert.match(script, /cleanup_docker_build_cache\(\)/)
  assert.match(script, /docker builder prune -af/)
  assert.match(script, /build_all_k3s_images "\$release_dir"[\s\S]+cleanup_docker_build_cache[\s\S]+deploy_k3s "\$release_dir"/)
})

test('remote payload falls back to reachable build images for host builds', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(script, /RK_HOST_NODE_IMAGE='docker\.1ms\.run\/library\/node:20\.18\.0-bookworm-slim'/)
  assert.ok(script.includes("RK_HOST_NODE_IMAGE_CANDIDATES='docker.1ms.run/library/node:20.18.0-bookworm-slim\ndocker.m.daocloud.io/library/node:20-bookworm-slim"))
  assert.match(script, /RK_HOST_MAVEN_IMAGE='docker\.1ms\.run\/library\/maven:3\.9\.9-eclipse-temurin-11'/)
  assert.ok(script.includes("RK_HOST_MAVEN_IMAGE_CANDIDATES='docker.1ms.run/library/maven:3.9.9-eclipse-temurin-11\ndocker.m.daocloud.io/library/maven:3.9.9-eclipse-temurin-11"))
  assert.ok(script.includes("RK_HOST_FRONTEND_BASE_IMAGE_CANDIDATES='docker.1ms.run/library/nginx:1.27-alpine\ndocker.m.daocloud.io/library/nginx:1.27-alpine"))
  assert.ok(script.includes("RK_HOST_BACKEND_BASE_IMAGE_CANDIDATES='docker.1ms.run/library/eclipse-temurin:11-jre-jammy\ndocker.m.daocloud.io/library/eclipse-temurin:11-jre-jammy"))
  assert.match(script, /select_docker_image_candidate\(\)/)
  assert.match(script, /docker_run_with_image_candidates\(\)/)
  assert.match(script, /docker_run_with_image_candidates "\$RK_HOST_NODE_IMAGE_CANDIDATES"/)
  assert.match(script, /docker_run_with_image_candidates "\$RK_HOST_MAVEN_IMAGE_CANDIDATES"/)
  assert.match(script, /docker_build_with_base_image_candidates "\$RK_HOST_FRONTEND_BASE_IMAGE_CANDIDATES"/)
  assert.match(script, /docker_build_with_base_image_candidates "\$RK_HOST_BACKEND_BASE_IMAGE_CANDIDATES"/)
  assert.match(script, /local __result_var="\$1" candidates="\$2" candidate_image/)
  assert.doesNotMatch(script, /local __result_var="\$1" candidates="\$2" image/)
  assert.match(script, /docker_pull_candidate "\$candidate_image"/)
  assert.match(script, /log "using docker image \$candidate_image"/)
  assert.match(script, /printf -v "\$__result_var" '%s' "\$candidate_image"/)
})

test('remote payload waits for Nacos with a Nacos 2.x readiness endpoint', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(script, /nacos_ready_url\(\)/)
  assert.match(script, /\/nacos\/v1\/console\/health\/readiness/)
  assert.match(script, /curl -fsS "\$\(nacos_ready_url 18848\)"/)
  assert.match(script, /ensure_k3s_nacos_port_forward\(\)/)
  assert.match(script, /if \[ -n "\$current_pid" \] && kill -0 "\$current_pid" >\/dev\/null 2>&1; then/)
  assert.match(script, /ensure_k3s_nacos_port_forward pf_pid "\$port_forward_log" "\$pf_pid"/)
  assert.match(script, /stop_k3s_nacos_port_forward "\$pf_pid"/)
  assert.doesNotMatch(script, /curl -fsS "http:\/\/127\.0\.0\.1:(?:8848|18848)\/nacos\/v1\/ns\/operator\/servers"/)
})

test('remote payload bounds k3s MySQL readiness exec probes and logs retries', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://127.0.0.1:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '127.0.0.1:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(script, /mysql_probe_k3s\(\)/)
  assert.match(script, /timeout 20s kubectl -n "\$RK_NAMESPACE" exec deploy\/rk-mysql/)
  assert.match(script, /log "k3s MySQL deployment is available; probing mysql client"/)
  assert.match(script, /log "k3s MySQL probe still waiting \(attempt \$attempt\/90\)"/)
  assert.doesNotMatch(script, /until kubectl -n "\$RK_NAMESPACE" exec deploy\/rk-mysql -- env MYSQL_PWD="\$RK_MYSQL_ROOT_PASSWORD" mysql/)
})

test('remote payload resets k3s namespace before a fresh install', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://127.0.0.1:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '127.0.0.1:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(script, /reset_k3s_namespace_for_fresh_install\(\)/)
  assert.match(script, /kubectl delete namespace "\$RK_NAMESPACE" --wait=false/)
  assert.match(script, /kubectl get namespace "\$RK_NAMESPACE" >\/dev\/null 2>&1/)
  assert.match(script, /log "k3s namespace \$RK_NAMESPACE still terminating \(attempt \$attempt\/120\)"/)
  assert.match(script, /reset_k3s_namespace_for_fresh_install[\s\S]+ensure_registry_pull_secret[\s\S]+build_all_k3s_images/)
})

test('remote payload installs harbor with online installer and docker mirror images', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)

  assert.match(script, /RK_HARBOR_INSTALLER_KIND='online'/)
  assert.match(script, /RK_HARBOR_DOWNLOAD_URLS='https:\/\/github\.com\/goharbor\/harbor\/releases\/download\/v2\.14\.4\/harbor-online-installer-v2\.14\.4\.tgz\nhttps:\/\/sourceforge\.net\/projects\/harbor\.mirror\/files\/v2\.14\.4\/harbor-online-installer-v2\.14\.4\.tgz\/download'/)
  assert.match(script, /local harbor_dir archive/)
  assert.match(script, /archive="\$harbor_dir\/harbor-\$RK_HARBOR_INSTALLER_KIND-installer-v\$RK_HARBOR_VERSION\.tgz"/)
  assert.doesNotMatch(script, /local [^\n]*="[^"\n]*\$harbor_dir[^"\n]*"/)
  assert.match(script, /RK_HARBOR_IMAGE_REPOSITORY='goharbor'/)
  assert.doesNotMatch(script, /docker\.1ms\.run\/goharbor/)
  assert.match(script, /docker_pull_with_retry\(\)/)
  assert.match(script, /docker_pull_with_retry "\$RK_HARBOR_IMAGE_REPOSITORY\/prepare:v\$RK_HARBOR_VERSION"/)
  assert.match(script, /harbor_api_ready\(\)/)
  assert.match(script, /harbor_auth_ready\(\)/)
  assert.match(script, /reset_installer_managed_harbor\(\)/)
  assert.match(script, /existing harbor rejects configured credentials; rebuilding installer-managed harbor/)
  assert.match(script, /start_existing_harbor\(\)/)
  assert.match(script, /if harbor_api_ready; then[\s\S]+harbor api already available; skipping harbor installer[\s\S]+ensure_harbor_project[\s\S]+return/)
  assert.match(script, /if start_existing_harbor && harbor_api_ready; then[\s\S]+existing harbor started; skipping harbor installer[\s\S]+ensure_harbor_project[\s\S]+return/)
  assert.match(script, /docker compose down -v --remove-orphans/)
  assert.match(script, /rm -rf "\$RK_HARBOR_DATA_PATH"/)
  assert.match(script, /local install_dir="\$RK_TARGET_PATH\/harbor\/harbor"[\s\S]+cd "\$install_dir" && docker compose up -d/)
  assert.match(script, /for _ in \$\(seq 1 24\); do[\s\S]+harbor_api_ready[\s\S]+return 0[\s\S]+sleep 5[\s\S]+done[\s\S]+return 1/)
  assert.doesNotMatch(script, /start_existing_harbor\(\)[\s\S]{0,220}wait_for_harbor/)
  assert.match(script, /job_loggers:\n  - STD_OUTPUT\n  - FILE/)
  assert.match(script, /logger_sweeper_duration: 1/)
  assert.match(script, /notification:\n  webhook_job_max_retry: 3\n  webhook_job_http_client_timeout: 3/)
  assert.match(script, /harbor_project_exists\(\)/)
  assert.match(script, /local status/)
  assert.match(script, /printf '\{"project_name":"%s","metadata":\{"public":"false"\}\}' "\$RK_REGISTRY_NAMESPACE" > "\$payload"/)
  assert.match(script, /--data-binary @"\$payload"/)
  assert.match(script, /if \[ "\$status" = "409" \] \|\| \[ "\$status" = "422" \]/)
  assert.ok(script.includes('sed -i "s#^[[:space:]]*goharbor/prepare:#                    $RK_HARBOR_IMAGE_REPOSITORY/prepare:#" "$install_dir/prepare"'))
  assert.match(script, /HARBOR_IMAGE_REPOSITORY="\$RK_HARBOR_IMAGE_REPOSITORY" \.\/install\.sh/)
  assert.doesNotMatch(script, /harbor-offline-installer-v\$RK_HARBOR_VERSION\.tgz/)
})

test('k3s manifest includes managed middleware and fresh-install initialization', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const { normalizeConfig } = await import('../src/config.mjs')
  const config = {
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  }

  const script = buildRemoteInstallScript(config)
  const jobStart = script.indexOf('run_k3s_build_job()')
  const jobEnd = script.indexOf('wait_for_k3s_build_job()', jobStart)
  const k3sBuildJob = script.slice(jobStart, jobEnd)

  assert.match(script, /hostPath:[\s\S]+path: \$RK_TARGET_PATH\/k3s-data\/\$RK_NAMESPACE\/mysql/)
  assert.match(script, /path: \$RK_TARGET_PATH\/k3s-data\/\$RK_NAMESPACE\/rabbitmq/)
  assert.match(script, /path: \$RK_TARGET_PATH\/k3s-data\/\$RK_NAMESPACE\/minio/)
  assert.match(script, /path: \$RK_TARGET_PATH\/k3s-data\/\$RK_NAMESPACE\/nacos/)
  assert.doesNotMatch(script, /path: \$RK_TARGET_PATH\/k3s-data\/(?:mysql|rabbitmq|minio|nacos)\b/)
  assert.match(script, /name: rk-mysql[\s\S]+image: \$RK_MYSQL_IMAGE/)
  assert.match(script, /name: rk-nacos[\s\S]+image: \$RK_NACOS_IMAGE/)
  assert.match(script, /name: rk-nacos[\s\S]+SPRING_DATASOURCE_PLATFORM[\s\S]+value: mysql/)
  assert.match(script, /name: rk-nacos[\s\S]+MYSQL_SERVICE_HOST[\s\S]+value: rk-mysql/)
  assert.match(script, /name: rk-nacos[\s\S]+MYSQL_SERVICE_DB_NAME[\s\S]+value: nacos/)
  assert.match(script, /name: rk-nacos[\s\S]+MYSQL_SERVICE_USER[\s\S]+value: root/)
  assert.match(script, /name: rk-nacos[\s\S]+MYSQL_SERVICE_PASSWORD[\s\S]+secretKeyRef/)
  assert.match(script, /name: rk-redis[\s\S]+image: \$RK_REDIS_IMAGE/)
  assert.match(script, /name: rk-rabbitmq[\s\S]+image: \$RK_RABBITMQ_IMAGE/)
  assert.match(script, /name: rk-minio[\s\S]+image: \$RK_MINIO_IMAGE/)
  assert.match(script, /name: rk-xxl-job[\s\S]+image: \$RK_XXL_JOB_IMAGE/)
  assert.match(script, /RK_RABBITMQ_IMAGE='docker\.1ms\.run\/heidiks\/rabbitmq-delayed-message-exchange:3\.13\.0-management'/)
  assert.match(script, /mountPath: \/etc\/rabbitmq\/enabled_plugins/)
  assert.match(script, /path: \$release_dir\/runtime\/rabbitmq\/enabled_plugins/)
  assert.match(script, /mountPath: \/config\/application.properties/)
  assert.match(script, /path: \$release_dir\/runtime\/rendered\/xxl-job\/application.properties/)
  assert.match(script, /SPRING_CLOUD_NACOS_CONFIG_NAMESPACE[\s\S]+value: "\$RK_NACOS_NAMESPACE"/)
  assert.match(script, /SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE[\s\S]+value: "\$RK_NACOS_NAMESPACE"/)
  assert.match(script, /SPRING_CLOUD_NACOS_CONFIG_GROUP[\s\S]+value: "\$RK_NACOS_GROUP"/)
  assert.match(script, /SPRING_CLOUD_NACOS_DISCOVERY_GROUP[\s\S]+value: "\$RK_NACOS_GROUP"/)
  assert.match(script, /SPRING_CLOUD_NACOS_DISCOVERY_IP[\s\S]+value: \$app_name/)
  assert.match(script, /SPRING_CLOUD_NACOS_DISCOVERY_PORT[\s\S]+value: "\$container_port"/)
  assert.doesNotMatch(script, /SPRING_CLOUD_NACOS_USERNAME/)
  assert.doesNotMatch(script, /SPRING_CLOUD_NACOS_PASSWORD/)
  assert.match(script, /wait_for_k3s_mysql/)
  assert.match(script, /initialize_fresh_database_k3s "\$release_dir"/)
  assert.match(script, /copy_fresh_install_schema_exports "\$release_dir"/)
  assert.match(script, /copy_fresh_install_nacos_exports "\$release_dir"/)
  assert.match(script, /infrastructure-migration\/nacos\/export\/configs\/DEFAULT_GROUP/)
  assert.match(script, /nacosConfig\/DEFAULT_GROUP/)
  assert.match(script, /write_fresh_install_nacos_overrides "\$release_dir"/)
  assert.match(script, /write_fresh_install_rabbitmq_assets "\$release_dir"/)
  assert.match(script, /write_fresh_install_xxl_job_assets "\$release_dir"/)
  assert.match(script, /render_xxl_job_runtime_file "\$release_dir"/)
  assert.match(script, /\[rabbitmq_delayed_message_exchange,rabbitmq_management,rabbitmq_prometheus\]\./)
  assert.match(script, /spring\.datasource\.url=jdbc:mysql:\/\/rk-mysql:3306\/xxl_job/)
  assert.match(script, /rk-shared-redis\.yaml\.tpl/)
  assert.match(script, /host: \$\{RK_REDIS_HOST\}/)
  assert.match(script, /password: \$\{RK_REDIS_PASSWORD\}/)
  assert.match(script, /rk-shared-minio\.yaml\.tpl/)
  assert.match(script, /endpoint: \$\{RK_MINIO_ENDPOINT\}/)
  assert.match(script, /secretKey: \$\{RK_MINIO_SECRET_KEY\}/)
  assert.match(script, /rk-shared-mq\.yaml\.tpl/)
  assert.match(script, /virtual-host: \$\{RK_RABBITMQ_VHOST\}/)
  assert.match(script, /publish_nacos_runtime_file\(\)/)
  assert.match(script, /case "\$file" in/)
  assert.match(script, /\*\.properties\) nacos_type="properties" ;;/)
  assert.match(script, /seataServer\.properties/)
  assert.match(script, /service\.vgroupMapping\.\*/)
  assert.match(script, /copy_fresh_install_schema_file\(\)/)
  assert.match(script, /infrastructure-migration\/mysql\/export\/\*-schema\.sql[\s\S]+database\/mysql_export_20260401\/\*\/schema\.sql/)
  assert.match(script, /newer infrastructure migration schema takes precedence/)
  assert.match(script, /database\/mysql_export_20260401\/\*\/schema\.sql/)
  assert.ok(script.includes('printf \'USE `%s`;\\n\' "$db" > "$target"'))
  assert.match(script, /ensure_schema_database_entry "\$schema_dir" "\$db"/)
  assert.ok(script.includes("printf 'CREATE DATABASE IF NOT EXISTS `%s` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;\\n' \"$db\""))
  assert.ok(script.includes("printf 'GRANT ALL PRIVILEGES ON `%s`.* TO '\\''${RK_MYSQL_APP_USER}'\\''@'\\''%%'\\'';\\n' \"$db\""))
  assert.ok(!script.includes("printf 'GRANT ALL PRIVILEGES ON `%s`.* TO '''${RK_MYSQL_APP_USER}'''@'''%%''';\\n' \"$db\""))
  assert.doesNotMatch(script, /grep -Fq "CREATE DATABASE IF NOT EXISTS `\$db`"/)
  assert.match(script, /append_normalized_sql_file "\$source_file" "\$target"/)
  assert.doesNotMatch(script, /cat "\$source_file" >> "\$target"/)
  assert.match(script, /iconv -f UTF-8 -t UTF-8/)
  assert.match(script, /iconv -f GB18030 -t UTF-8/)
  assert.doesNotMatch(script, /mysql_export_20260401\/\*\/data\.sql/)
  assert.match(script, /import_nacos_configs_k3s "\$release_dir"/)
  assert.doesNotMatch(script, /BEGIN \{[\s\S]{0,400}\bnext\b/)
  assert.match(script, /mysql -h 127\.0\.0\.1 -P 3306 -uroot -e "SELECT 1"/)
  assert.match(script, /mysql -h 127\.0\.0\.1 -P 3306 -uroot/)
  assert.doesNotMatch(script, /mysqladmin ping/)
  assert.match(script, /checkout_source release_dir/)
  assert.doesNotMatch(script, /release_dir="\$\(checkout_source\)"/)
  assert.match(script, /checkout_source\(\)[\s\S]+local __result_var="\$1"[\s\S]+local resolved_release_dir/)
  assert.match(script, /printf -v "\$__result_var" '%s' "\$resolved_release_dir"/)
  assert.doesNotMatch(script, /checkout_source\(\)[\s\S]{0,300}local release_dir="\$RK_TARGET_PATH\/releases\/\$RK_RELEASE_ID"/)
  assert.doesNotMatch(script, /local release_dir="\$1"[^\n]*\$release_dir/)
  assert.match(script, /git_clone_source\(\)/)
  assert.match(script, /GIT_TERMINAL_PROMPT=0/)
  assert.match(script, /GIT_ASKPASS/)
  assert.match(script, /attempting anonymous git clone/)
  assert.match(script, /retrying git clone with configured credentials/)
  assert.match(script, /GIT_TERMINAL_PROMPT=0 git clone --depth 1 --branch "\$RK_REPO_BRANCH" "\$RK_REPO_URL" "\$destination"/)
  assert.match(script, /GIT_TERMINAL_PROMPT=0 GIT_ASKPASS="\$RK_GIT_ASKPASS" git clone --depth 1 --branch "\$RK_REPO_BRANCH" "\$RK_REPO_URL" "\$destination"/)
  assert.match(k3sBuildJob, /hostPath:[\s\S]+path: \$release_dir\/src[\s\S]+type: Directory/)
  assert.doesNotMatch(k3sBuildJob, /alpine\/git:2\.45\.2/)
  assert.match(k3sBuildJob, /FROM \\\$\{BASE_IMAGE\}/)
  assert.match(k3sBuildJob, /\n          ARG BASE_IMAGE\n          FROM \\\$\{BASE_IMAGE\}/)
  assert.match(k3sBuildJob, /\n          server \{\n            listen 80;/)
  assert.match(k3sBuildJob, /\n          EOF_BACKEND_DOCKERFILE/)
  assert.match(k3sBuildJob, /cp "\\\$\(find "\/workspace\/src\/\$jar_dir\/target"/)
  assert.match(k3sBuildJob, /proxy_set_header Host \\\$host/)
  assert.match(k3sBuildJob, /try_files \\\$uri \\\$uri\/ \/index\.html/)
  assert.match(k3sBuildJob, /java \\\$\{JAVA_OPTS\} -jar \/app\/app\.jar/)
  assert.doesNotMatch(k3sBuildJob, /FROM (?<!\\)\$\{BASE_IMAGE\}/)

  const normalizedScript = buildRemoteInstallScript(normalizeConfig(config))
  assert.doesNotMatch(normalizedScript, /local [^\n]*="[^"\n]*\$release_dir[^"\n]*"/)
  assert.doesNotMatch(normalizedScript, /local [^\n]*="[^"\n]*\$[A-Za-z_][A-Za-z0-9_]*[^"\n]*"[^\n]*="[^"\n]*\$[A-Za-z_][A-Za-z0-9_]*[^"\n]*"/)
})

test('compose services override Nacos namespace and group from installer config', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const { normalizeConfig } = await import('../src/config.mjs')
  const config = normalizeConfig({
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    services: ['rk-user'],
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://localhost:30080', domesticMirror: true },
    runtime: { mode: 'docker-compose', installDocker: true },
    nacos: { namespace: 'public', group: 'DEFAULT_GROUP' }
  })

  const script = buildRemoteInstallScript(config)
  const composeStart = script.indexOf('write_compose_file()')
  const composeEnd = script.indexOf('deploy_compose()', composeStart)
  const composeScript = script.slice(composeStart, composeEnd)

  assert.match(composeScript, /SPRING_CLOUD_NACOS_CONFIG_NAMESPACE: \\\$\{RK_NACOS_NAMESPACE\}/)
  assert.match(composeScript, /SPRING_CLOUD_NACOS_DISCOVERY_NAMESPACE: \\\$\{RK_NACOS_NAMESPACE\}/)
  assert.match(composeScript, /SPRING_CLOUD_NACOS_CONFIG_GROUP: \\\$\{RK_NACOS_GROUP\}/)
  assert.match(composeScript, /SPRING_CLOUD_NACOS_DISCOVERY_GROUP: \\\$\{RK_NACOS_GROUP\}/)
  assert.doesNotMatch(composeScript, /SPRING_CLOUD_NACOS_USERNAME/)
  assert.doesNotMatch(composeScript, /SPRING_CLOUD_NACOS_PASSWORD/)
  assert.match(composeScript, /rk-xxl-job:/)
  assert.match(composeScript, /\/config\/application\.properties/)
  assert.match(composeScript, /\/etc\/rabbitmq\/enabled_plugins/)
  assert.match(composeScript, /rk-xxl-job-data:/)
})

test('k3s harbor registry builds service images on the docker host and pushes to harbor', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const { normalizeConfig } = await import('../src/config.mjs')
  const config = normalizeConfig({
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  })

  const script = buildRemoteInstallScript(config)

  assert.match(script, /RK_K3S_DOCKERLESS_BUILD='false'/)
  assert.match(script, /"insecure-registries": \["\$RK_REGISTRY_SERVER", "\$RK_REGISTRY_PUSH_SERVER"\]/)
  assert.match(script, /docker_login_registry\(\)/)
  assert.match(script, /ensure_docker_service_ready restart/)
  assert.doesNotMatch(script, /^\s*systemctl restart docker$/m)
  assert.match(script, /RK_HOST_MAVEN_IMAGE='docker\.1ms\.run\/library\/maven:3\.9\.9-eclipse-temurin-11'/)
  assert.match(script, /RK_HOST_NODE_IMAGE='docker\.1ms\.run\/library\/node:20\.18\.0-bookworm-slim'/)
  assert.match(script, /RK_HOST_BACKEND_BASE_IMAGE='docker\.1ms\.run\/library\/eclipse-temurin:11-jre-jammy'/)
  assert.match(script, /RK_HOST_FRONTEND_BASE_IMAGE='docker\.1ms\.run\/library\/nginx:1\.27-alpine'/)
  assert.match(script, /docker_run_with_image_candidates "\$RK_HOST_MAVEN_IMAGE_CANDIDATES" -v "\$src:\/workspace" -v "\$RK_TARGET_PATH\/cache\/m2:\/root\/\.m2\/repository" -w \/workspace/)
  assert.match(script, /printf '%s' "\$RK_REGISTRY_PASSWORD" \| docker login "\$RK_REGISTRY_PUSH_SERVER" --username "\$RK_REGISTRY_USER" --password-stdin/)
  assert.match(script, /image_ref="\$RK_REGISTRY_SERVER\/\$RK_REGISTRY_NAMESPACE\/\$image:\$RK_RELEASE_ID"/)
  assert.match(script, /push_ref="\$RK_REGISTRY_PUSH_SERVER\/\$RK_REGISTRY_NAMESPACE\/\$image:\$RK_RELEASE_ID"/)
  assert.match(script, /docker_build_with_base_image_candidates "\$RK_HOST_BACKEND_BASE_IMAGE_CANDIDATES" "\$build_dir" -t "\$push_ref"/)
  assert.match(script, /docker push "\$push_ref"/)
  assert.match(script, /if \[ "\$RK_K3S_DOCKERLESS_BUILD" = "true" \]; then[\s\S]+build_k3s_service_image_dockerless/)
})

test('k3s deployment can reuse existing registry images when remote build is disabled', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const { normalizeConfig } = await import('../src/config.mjs')
  const config = normalizeConfig({
    releaseId: 'rk-web-20260620090240',
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30080', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false, buildOnRemote: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test' },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  })

  const script = buildRemoteInstallScript(config)

  assert.match(script, /RK_BUILD_ON_REMOTE='false'/)
  assert.match(script, /if \[ "\$RK_BUILD_ON_REMOTE" = "true" \]; then[\s\S]+build_all_k3s_images "\$release_dir"[\s\S]+fi[\s\S]+deploy_k3s "\$release_dir"/)
  assert.match(script, /RK_RELEASE_ID="\$\{RK_RELEASE_ID:-rk-web-20260620090240\}"/)
})

test('remote payload keeps service ports local by default', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const { normalizeConfig } = await import('../src/config.mjs')
  const config = normalizeConfig({
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://127.0.0.1:30280', domesticMirror: true },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test', frontendNodePort: 30280, gatewayNodePort: 30210 },
    registry: {
      provider: 'harbor',
      server: '127.0.0.1:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  })

  const script = buildRemoteInstallScript(config)

  assert.equal(config.deployment.accessMode, 'local-loopback')
  assert.match(script, /RK_ACCESS_MODE='local-loopback'/)
  assert.match(script, /access mode local-loopback; keeping service ports on target localhost\/cluster network only/)
  assert.match(script, /only the SSH port needs to be reachable from outside/)
  assert.doesNotMatch(script, /cloud security group/)
})

test('remote payload opens local firewall for k3s nodeports and harbor registry port in public mode', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const { normalizeConfig } = await import('../src/config.mjs')
  const config = normalizeConfig({
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:30280', domesticMirror: true, accessMode: 'public-nodeport' },
    runtime: { mode: 'k3s', installK3s: true, installDocker: false },
    k3s: { imageMode: 'registry', namespace: 'rk-web-test', frontendNodePort: 30280, gatewayNodePort: 30210 },
    registry: {
      provider: 'harbor',
      server: '203.0.113.10:30090',
      namespace: 'rk-web',
      username: 'admin',
      password: 'harbor-secret',
      installHarbor: true,
      harborHttpPort: 30090,
      insecure: true,
      imagePullSecret: 'rk-registry'
    }
  })

  const script = buildRemoteInstallScript(config)

  assert.match(script, /open_local_firewall_ports\(\)/)
  assert.match(script, /firewall-cmd --permanent --add-port="\$\{port\}\/tcp"/)
  assert.match(script, /firewall-cmd --reload/)
  assert.match(script, /ufw allow "\$\{port\}\/tcp"/)
  assert.match(script, /public access mode: upstream firewall or cloud security group must allow TCP ports/)
  assert.match(script, /open_runtime_firewall_ports\(\)/)
  assert.match(script, /open_local_firewall_ports "\$RK_K3S_FRONTEND_NODE_PORT" "\$RK_K3S_GATEWAY_NODE_PORT" "\$RK_HARBOR_HTTP_PORT"/)
  assert.match(script, /install_k3s_runtime[\s\S]+open_runtime_firewall_ports[\s\S]+check_k3s_node_health/)
})

test('remote payload opens local firewall for compose exposed ports', async () => {
  const { buildRemoteInstallScript } = await import('../src/remote-payload.mjs')
  const { normalizeConfig } = await import('../src/config.mjs')
  const config = normalizeConfig({
    ssh: { host: '203.0.113.10', port: 30139, username: 'root' },
    services: ['rk-gateway', 'rk-user', 'frontend'],
    git: { provider: 'gitee', repositoryUrl: 'https://gitee.com/example/rk-web.git', branch: 'cloud-master-new' },
    deployment: { targetPath: '/opt/rk-web', publicBaseUrl: 'http://203.0.113.10:38080', domesticMirror: true, accessMode: 'public-nodeport' },
    runtime: { mode: 'docker-compose', installDocker: true },
    compose: { gatewayPort: 18010, frontendPort: 38080, nacosPort: 18848 }
  })

  const script = buildRemoteInstallScript(config)

  assert.match(script, /RK_COMPOSE_GATEWAY_PORT='18010'/)
  assert.match(script, /RK_COMPOSE_FRONTEND_PORT='38080'/)
  assert.match(script, /RK_COMPOSE_NACOS_PORT='18848'/)
  assert.match(script, /RK_COMPOSE_EXPOSED_PORTS='18010 8082 38080 18848'/)
  assert.match(script, /open_local_firewall_ports \$RK_COMPOSE_EXPOSED_PORTS/)
  assert.match(script, /host_port="\$RK_COMPOSE_GATEWAY_PORT"/)
  assert.match(script, /host_port="\$RK_COMPOSE_FRONTEND_PORT"/)
  assert.match(script, /ports: \["\$\{RK_COMPOSE_NACOS_PORT\}:8848"\]/)
  assert.match(script, /install_docker_runtime[\s\S]+open_runtime_firewall_ports[\s\S]+checkout_source release_dir/)
})
