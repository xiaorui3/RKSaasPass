import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'

async function readSource(relativePath) {
  return readFile(new URL(relativePath, import.meta.url), 'utf8')
}

test('deploy package archive contract should include the complete RK-Web stack and migration artifacts', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  const requiredServices = [
    'rk-gateway',
    'rk-auth',
    'rk-user',
    'rk-search',
    'rk-file',
    'rk-message',
    'rk-content',
    'rk-pay',
    'rk-trade',
    'rk-exam',
    'rk-activity',
    'rk-data',
    'rk-web-frontend'
  ]
  const requiredMiddleware = [
    'mysql',
    'redis',
    'rabbitmq',
    'nacos',
    'seata',
    'xxl-job',
    'elasticsearch',
    'sentinel',
    'minio'
  ]

  for (const service of requiredServices) {
    assert.equal(impl.includes(service), true, `missing deploy package service contract for ${service}`)
    assert.equal(
      impl.includes(`imageForService(request, "${service}"`) || impl.includes(`${service}:latest`),
      true,
      `missing k8s/compose image reference for ${service}`
    )
  }

  for (const middleware of requiredMiddleware) {
    assert.equal(impl.includes(`${middleware}:`), true, `missing docker compose service block for ${middleware}`)
  }

  assert.equal(impl.includes('scripts/export-all-current-data.sh'), true)
  assert.equal(impl.includes('scripts/export-databases.sh'), true)
  assert.equal(impl.includes('scripts/import-databases.sh'), true)
  assert.equal(impl.includes('scripts/export-minio.sh'), true)
  assert.equal(impl.includes('scripts/import-minio.sh'), true)
  assert.equal(impl.includes('images/image-list.txt'), true)
  assert.equal(impl.includes('images/workload-images.txt'), true)
  assert.equal(impl.includes('upload-log.txt'), true)
  assert.equal(impl.includes('aliyun-registry-upload.log'), true)
  assert.equal(impl.includes('docker-compose.yml'), true)
  assert.equal(impl.includes('k8s/rk-web-stack.yaml'), true)
  assert.equal(impl.includes('k3s/k3s-install-notes.md'), true)
  assert.equal(impl.includes('k8s/k8s-install-notes.md'), true)
})

test('generated one-key deploy bundle should use operator-facing service names and namespace scoped k8s services', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    '"  xxl-job:\\n"',
    '"  frontend:\\n"',
    'XXL_JOB_ADMIN_ADDRESSES: http://xxl-job:8880/xxl-job-admin',
    'buildSentinelDashboardDeployment(namespace)',
    'buildSimpleService(namespace, "sentinel", 8858, 8858)',
    'buildSimpleService(namespace, "rk-web-frontend", 80, 80)',
    'imagePullSecrets:',
    'deployPackageRegistryImagePullSecretName',
    'MYSQL_ROOT_PASSWORD',
    '--max_connections=512',
    'SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE',
    'SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE',
    'SPRING_DATASOURCE_PLATFORM',
    'MYSQL_SERVICE_HOST',
    'RK_GATEWAY_URL: http://rk-gateway:10010',
    'RK_MINIO_URL: http://minio:9000',
    'SPRING_REDIS_HOST',
    'SPRING_RABBITMQ_HOST',
    'JAVA_TOOL_OPTIONS',
    '-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m',
    'resources:',
    'requests:',
    'limits:',
    'initContainers:',
    'copy-mysql-driver',
    'java -Djarmode=layertools -jar /app/app.jar extract',
    "find /tmp/rk-layers -type f -name 'mysql-connector*.jar'",
    '/home/nacos/plugins/mysql',
    'fix-data-permissions',
    'chown -R 1000:0 /usr/share/elasticsearch/data',
    'REMOTE_BUSYBOX_IMAGE',
    'JVM_XMS',
    'JVM_XMX',
    'DEFAULT_SENTINEL_DASHBOARD_IMAGE',
    'swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/bladex/sentinel-dashboard:1.8.6',
    'readinessProbe:',
    'livenessProbe:',
    'startupProbe:',
    'path: /',
    'buildRabbitMqTopologyJob(namespace, rabbitmqImage)',
    'rabbitmqadmin',
    'trade.pay.success.queue',
    'trade.refund.result.queue',
    'trade.delay.order.query',
    'pay.topic',
    'trade.delay.topic',
    'targetPort: 15672',
    '"  namespace: " + namespace + "\\n"',
    'SERVICE_IMAGE_MAP',
    'String mysqlImage = REMOTE_MYSQL_IMAGE',
    'String redisImage = REMOTE_REDIS_IMAGE',
    'String minioImage = REMOTE_MINIO_IMAGE',
    'String nacosImage = REMOTE_NACOS_IMAGE',
    'rabbitmqImageForRuntimeComponent(request, serviceImages)',
    'imageForRuntimeComponent(request, serviceImages, "elasticsearch:7.12.1"',
    'imageForService(request, "rk-user"',
    'imageForService(request, "rk-web-frontend"',
    'DEFAULT_MINIO_CLIENT_IMAGE'
  ]) {
    assert.equal(impl.includes(marker), true, `one-key deploy bundle missing ${marker}`)
  }

  assert.equal(impl.includes('buildSimpleService("mysql", 3306, 3306)'), false, 'k8s service generation must pass namespace')
  assert.equal(impl.includes('"  xxljob:\\n"'), false, 'compose should not expose xxljob alias without hyphen')
  assert.equal(impl.includes('"  rk-web-frontend:\\n"'), false, 'compose should expose frontend service name')
  assert.equal(impl.includes('/seata-server/libs/mysql-connector-java-8.0.23.jar'), false, 'nacos should not depend on a helper jar that is absent from current seata images')
  assert.equal(impl.includes('maven.aliyun.com/repository/public'), false, 'remote Nacos init must not download MySQL driver from Maven during deployment')
  assert.equal(impl.includes('REMOTE_MAVEN_IMAGE'), false, 'remote Nacos init must not need a Maven image at runtime')
})

test('runtime export summary should distinguish node-local misses from hard failures', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'nodeMissed=0',
    'hardFailed=0',
    'node_missed=$nodeMissed',
    'hard_failed=$hardFailed',
    'runtime node misses are expected on multi-node clusters',
    'image not present on source node',
    'nodeMissed=$((nodeMissed + 1))',
    'hardFailed=$((hardFailed + 1))'
  ]) {
    assert.equal(impl.includes(marker), true, `runtime summary missing ${marker}`)
  }

  assert.equal(impl.includes('failed=0\n'), false, 'runtime summary should not use a single ambiguous failed counter')
  assert.equal(impl.includes('echo "failed=$failed" >> runtime-summary/status.txt'), false, 'runtime summary should not report node-local misses as failed images')
})

test('one-key deploy k8s generator should fit small single-node k3s targets by default', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  const resourcesStart = impl.indexOf('private String buildK8sDeploymentResources(String name)')
  const resourcesEnd = impl.indexOf('private String buildK8sVolumeMounts', resourcesStart)
  assert.notEqual(resourcesStart, -1, 'K8s deployment resources builder missing')
  assert.notEqual(resourcesEnd, -1, 'K8s deployment resources builder end missing')
  const resources = impl.slice(resourcesStart, resourcesEnd)
  const rkBranchStart = resources.indexOf('name != null && name.startsWith("rk-")')
  const rkBranchEnd = resources.indexOf('if ("nacos".equals(name)', rkBranchStart)
  assert.notEqual(rkBranchStart, -1, 'RK service resource branch missing')
  assert.notEqual(rkBranchEnd, -1, 'RK service resource branch end missing')
  const rkResources = resources.slice(rkBranchStart, rkBranchEnd)

  assert.match(impl, /JAVA_TOOL_OPTIONS", "-Xms64m -Xmx256m -XX:MaxMetaspaceSize=128m/, 'RK service JVM defaults must fit small migration targets')
  assert.match(rkResources, /memory: 128Mi[\s\S]*memory: 512Mi/, 'RK service requests must be low enough for single-node k3s while keeping a safe limit')
  assert.doesNotMatch(rkResources, /memory: 384Mi|memory: 768Mi/, 'RK service requests must not reserve 384Mi each by default')
  assert.match(resources, /"nacos"\.equals\(name\) \|\| "elasticsearch"\.equals\(name\)[\s\S]*memory: 384Mi[\s\S]*memory: 768Mi/, 'Nacos and Elasticsearch should keep moderate memory without reserving 1Gi class resources')
  assert.match(resources, /"mysql"\.equals\(name\)[\s\S]*memory: 128Mi[\s\S]*memory: 512Mi/, 'Middleware requests should not crowd out application pods on test nodes')
})

test('one-key deploy k8s generator should give core backend services enough memory headroom', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  const resourcesStart = impl.indexOf('private String buildK8sDeploymentResources(String name)')
  const resourcesEnd = impl.indexOf('private String buildK8sVolumeMounts', resourcesStart)
  assert.notEqual(resourcesStart, -1, 'K8s deployment resources builder missing')
  assert.notEqual(resourcesEnd, -1, 'K8s deployment resources builder end missing')
  const resources = impl.slice(resourcesStart, resourcesEnd)

  assert.match(
    resources,
    /"rk-user"\.equals\(name\) \|\| "rk-auth"\.equals\(name\) \|\| "rk-gateway"\.equals\(name\)[\s\S]*memory: 128Mi[\s\S]*memory: 768Mi/,
    'rk-user/auth/gateway need 768Mi limits because 512Mi caused OOMKilled on migrated single-node k3s'
  )
})

test('one-key deploy compose generator should carry complete middleware and app inventory', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const service of [
    'rk-gateway',
    'rk-auth',
    'rk-user',
    'rk-search',
    'rk-file',
    'rk-message',
    'rk-content',
    'rk-pay',
    'rk-trade',
    'rk-exam',
    'rk-activity',
    'rk-data',
    'frontend'
  ]) {
    assert.equal(impl.includes(`"  ${service}:\\n"`), true, `compose generator missing ${service}`)
  }

  for (const middleware of [
    'mysql',
    'redis',
    'rabbitmq',
    'nacos',
    'seata',
    'xxl-job',
    'elasticsearch',
    'sentinel',
    'minio'
  ]) {
    assert.equal(impl.includes(`"  ${middleware}:\\n"`), true, `compose generator missing ${middleware}`)
  }

  assert.equal(impl.includes('"  xxljob:\\n"'), false, 'compose generator should not expose xxljob alias without hyphen')
  assert.equal(impl.includes('"  rk-web-frontend:\\n"'), false, 'compose generator should expose frontend service name')
})

test('one-key deploy k8s generator should persist core middleware through statefulsets and pvc templates', async () => {
  const impl = await readSource('../../../rk-user/src/main/java/com/tianji/user/service/impl/AdminOpsServiceImpl.java')

  for (const marker of [
    'deployPackageK8sStorageClassName',
    'buildPersistentStatefulSet(namespace, "mysql"',
    'buildAliasService(namespace, "rk-mysql", "mysql", 3306, 3306)',
    'buildPersistentStatefulSet(namespace, "redis"',
    'buildAliasService(namespace, "rk-redis", "redis", 6379, 6379)',
    'buildPersistentStatefulSet(namespace, "rabbitmq"',
    'buildAliasService(namespace, "rk-rabbitmq", "rabbitmq", 5672, 5672)',
    'buildPersistentStatefulSet(namespace, "minio"',
    'buildAliasService(namespace, "rk-minio", "minio", 9000, 9000)',
    'buildPersistentStatefulSet(namespace, "nacos"',
    'buildPersistentStatefulSet(namespace, "elasticsearch"',
    'private String buildAliasService(String namespace, String aliasName, String targetName, int port, int targetPort)',
    'private String buildService(String namespace, String name, String selectorName, int port, int targetPort)',
    'kind: StatefulSet',
    'serviceName: " + name + "\\n"',
    'volumeClaimTemplates:',
    'storageClassName: " + storageClassName + "\\n"',
    'accessModes:',
    'ReadWriteOnce',
    'resources:',
    'requests:',
    'storage:',
    'return "20Gi";',
    '/var/lib/mysql',
    '/data',
    '/var/lib/rabbitmq',
    '/home/nacos/data',
    '/usr/share/elasticsearch/data',
    '--appendonly',
    'deleteLegacyPersistentMiddlewareDeployments'
  ]) {
    assert.equal(impl.includes(marker), true, `persistent k8s contract missing ${marker}`)
  }

  assert.equal(
    impl.includes('buildSimpleDeployment(namespace, "mysql"'),
    false,
    'mysql must not be generated as an ephemeral Deployment'
  )
  assert.equal(
    impl.includes('buildSimpleDeployment(namespace, "minio"'),
    false,
    'minio must not be generated as an ephemeral Deployment'
  )
})
