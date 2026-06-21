import { uploadFile } from '@/api/common'
import { resolveMediaUrl } from '@/utils/mediaUrl'

export function resolveUploadedPath(result) {
  return result?.data?.relativePath || result?.data?.path || result?.relativePath || result?.path || ''
}

export function resolveUploadedUrl(result) {
  const path = resolveUploadedPath(result)
  const rawUrl = result?.data?.fileUrl || result?.data?.url || result?.fileUrl || result?.url || ''
  return resolveMediaUrl(path || rawUrl)
}

export async function uploadManagedFile(options, type) {
  const result = await uploadFile(options.file, type)
  const path = resolveUploadedPath(result)
  const url = resolveUploadedUrl(result)
  const storedValue = path || result?.data?.fileUrl || result?.data?.url || result?.fileUrl || result?.url || ''
  if (!storedValue) {
    throw new Error('upload response missing stored file path')
  }
  return { result, path, url, storedValue }
}
