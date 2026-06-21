import assert from 'node:assert/strict'
import { EventEmitter } from 'node:events'
import test from 'node:test'
import { SshRunner } from '../src/ssh-runner.mjs'

test('ssh runner sends remote env through stdin instead of command line', async () => {
  const captured = {}
  const fakeSsh = {
    exec(command, callback) {
      captured.command = command
      const stream = new EventEmitter()
      stream.stderr = new EventEmitter()
      stream.end = (payload) => {
        captured.payload = payload
        process.nextTick(() => stream.emit('close', 0))
      }
      callback(null, stream)
    },
    end() {}
  }
  class FakeRunner extends SshRunner {
    connect() {
      return Promise.resolve(fakeSsh)
    }
  }

  const runner = new FakeRunner(
    { ssh: { host: 'example.invalid', port: 22, username: 'root', password: 'ssh-secret' } },
    { log() {}, error() {} }
  )

  await runner.execScript('echo "$RK_SECRET_VALUE"\n', { RK_SECRET_VALUE: 'super-secret' })

  assert.equal(captured.command, 'bash -s')
  assert.equal(captured.command.includes('super-secret'), false)
  assert.match(captured.payload, /export RK_SECRET_VALUE='super-secret'/)
  assert.match(captured.payload, /echo "\$RK_SECRET_VALUE"/)
})

test('ssh runner absorbs late client errors after a connection failure', async () => {
  let capturedUnhandled = null
  let connectCalled = false
  const onUnhandled = (error) => {
    capturedUnhandled = error
  }
  process.once('uncaughtException', onUnhandled)
  try {
    const runner = new SshRunner(
      { ssh: { host: 'example.invalid', port: 22, username: 'root', password: 'ssh-secret' } },
      { log() {}, error() {} },
      class FakeClient extends EventEmitter {
        connect() {
          connectCalled = true
          process.nextTick(() => {
            this.emit('error', new Error('Timed out while waiting for handshake'))
            this.emit('error', new Error('Connection lost before handshake'))
          })
        }
      }
    )

    await assert.rejects(
      Promise.race([
        runner.connect(),
        new Promise((_, reject) => setTimeout(() => reject(new Error('connect did not use injected client')), 100))
      ]),
      /Timed out while waiting for handshake/
    )
    await new Promise((resolve) => setImmediate(resolve))
    assert.equal(connectCalled, true)
    assert.equal(capturedUnhandled, null)
  } finally {
    process.removeListener('uncaughtException', onUnhandled)
  }
})
