import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const currentDir = path.dirname(fileURLToPath(import.meta.url))
const repoRoot = path.resolve(currentDir, '../../..')
const script = readFileSync(path.join(repoRoot, 'ci/remote/build_deploy_service_k8s.sh'), 'utf8')
const gitAttributes = readFileSync(path.join(repoRoot, '.gitattributes'), 'utf8')

test('K8s remote shell scripts are forced to LF line endings for Linux bash', () => {
  assert.doesNotMatch(script, /\r\n/)
  assert.match(gitAttributes, /\*\.sh\s+text\s+eol=lf/)
  assert.match(gitAttributes, /ci\/remote\/\*\*\s+text\s+eol=lf/)
})

test('K8s deploy script targets existing Rainbond StatefulSets before legacy Deployments', () => {
  assert.match(script, /statefulset\/rk-server-\$\{container_name\}/)
  assert.match(script, /deployment\/\$\{SERVICE_NAME\}/)
  assert.match(script, /kubectl_rk set image "\$\{target\}"/)
  assert.match(script, /kubectl_rk rollout status "\$\{target\}"/)
})

test('K8s deploy script imports images to every current cluster node', () => {
  assert.match(script, /resolve_k8s_nodes\(\)/)
  assert.match(script, /type=="InternalIP"/)
  assert.doesNotMatch(script, /RK_K8S_NODES="\$\{RK_K8S_NODES:-master01=192\.168\.1\.190/)
  assert.match(script, /resolve_k8s_builder_node\(\)/)
  assert.match(script, /RK_K8S_BUILDER_NODE="\$\{RK_K8S_BUILDER_NODE:-\$\(resolve_k8s_builder_node\)\}"/)
  assert.match(script, /wait_for_remote_containerd_socket\(\)/)
  assert.match(script, /RK_REMOTE_CONTAINERD_WAIT_SECONDS/)
  assert.match(script, /diagnose_remote_image_import/)
})

test('K8s deploy script distributes local images only to Ready nodes', () => {
  assert.match(script, /resolve_k8s_nodes\(\)/)
  assert.match(script, /type=="Ready"/)
  assert.match(script, /read -r node ready ip/)
  assert.match(script, /\[ "\$\{ready\}" = "True" \]/)
  assert.match(script, /skipping NotReady K8s node for image import/)
})

test('K8s deploy script does not hang indefinitely on unreachable image import nodes', () => {
  assert.match(script, /scp -P "\$\{port\}"[\s\S]*-o ConnectTimeout=15[\s\S]*-o ServerAliveInterval=10[\s\S]*-o ServerAliveCountMax=3/)
  assert.match(script, /ssh -p "\$\{port\}"[\s\S]*-o ConnectTimeout=15[\s\S]*-o ServerAliveInterval=10[\s\S]*-o ServerAliveCountMax=3/)
})

test('K8s deploy script carries runtime memory settings into backend pods', () => {
  assert.match(script, /kubectl_rk set env "\$\{target\}" "JAVA_OPTS=\$\{java_opts\}"/)
})

test('K8s backend build job resolves the built jar inside the Maven pod', () => {
  assert.match(script, /jar_path="\\\$\(find '\/rk-ci\/releases\/\$\{RELEASE_ID\}\/src\/\$\{jar_dir\}\/target'/)
  assert.match(script, /cp "\\\$\{jar_path\}" '\/rk-ci\/releases\/\$\{RELEASE_ID\}\/build\/\$\{SERVICE_NAME\}\/app\.jar'/)
  assert.doesNotMatch(script, /cp "\$\(find/)
})
