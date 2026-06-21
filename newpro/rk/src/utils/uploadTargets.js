const UPLOAD_TARGETS = {
  avatar: { service: 'rk-user', bizType: 'avatar' },
  'tenant-logo': { service: 'rk-user', bizType: 'tenant-logo' },
  background: { service: 'rk-user', bizType: 'background' },
  finance: { service: 'rk-user', bizType: 'finance-proof' },
  gallery: { service: 'rk-activity', bizType: 'gallery' },
  'news-image': { service: 'rk-content', bizType: 'news-image' },
  'news-video': { service: 'rk-content', bizType: 'news-video' },
  'news-attachment': { service: 'rk-content', bizType: 'news-attachment' },
  'works-cover': { service: 'rk-content', bizType: 'works-cover' },
  'works-video': { service: 'rk-content', bizType: 'works-video' },
  'notice-cover': { service: 'rk-user', bizType: 'notice-cover' },
  'notice-attachment': { service: 'rk-user', bizType: 'notice-attachment' },
  'activity-cover': { service: 'rk-activity', bizType: 'activity-cover' },
  'activity-attachment': { service: 'rk-activity', bizType: 'activity-attachment' },
  'competition-cover': { service: 'rk-activity', bizType: 'competition-cover' },
  'competition-rules': { service: 'rk-activity', bizType: 'competition-rules' },
  'competition-materials': { service: 'rk-activity', bizType: 'competition-materials' },
  'competition-results': { service: 'rk-activity', bizType: 'competition-results' },
  'email-image': { service: 'rk-user', bizType: 'email-image' },
  'email-attachment': { service: 'rk-user', bizType: 'email-attachment' },
  'mobile-apk': { service: 'rk-user', bizType: 'mobile-apk' }
}

export function resolveUploadTarget(options) {
  if (typeof options === 'string') {
    const preset = UPLOAD_TARGETS[options]
    if (!preset) {
      throw new Error(`unknown upload type: ${options}`)
    }
    return preset
  }

  const raw = options || {}
  const preset = raw.type ? UPLOAD_TARGETS[raw.type] : null
  const service = raw.service || preset?.service
  const bizType = raw.bizType || preset?.bizType || raw.type

  if (!service || !bizType) {
    throw new Error('upload target requires service and bizType')
  }

  return { service, bizType }
}

export { UPLOAD_TARGETS }
