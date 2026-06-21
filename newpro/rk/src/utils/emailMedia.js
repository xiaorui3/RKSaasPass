import { resolveMediaUrl } from './mediaUrl.js'

export function extractUploadedFileUrl(payload) {
  if (!payload) {
    return ''
  }
  if (typeof payload === 'string') {
    return resolveMediaUrl(payload)
  }
  return resolveMediaUrl(payload.url || payload.storedValue || payload.path || payload.fileUrl || '')
}
