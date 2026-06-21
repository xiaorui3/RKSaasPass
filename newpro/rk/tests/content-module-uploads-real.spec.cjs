// @ts-check
const { test, expect } = require('@playwright/test')
const fs = require('node:fs')
const path = require('node:path')

const API_BASE = process.env.PLAYWRIGHT_API_BASE || process.env.VITE_PROXY_TARGET || 'http://127.0.0.1:10010'
const MANAGER = { username: 'manager_a', password: '123456', organizationId: '1' }

test.describe.configure({ timeout: 120000 })

function fixtureUpload(sourcePath, name, mimeType) {
  return {
    name,
    mimeType,
    buffer: fs.readFileSync(path.resolve(__dirname, sourcePath))
  }
}

function visiblePng(name = 'probe.png') {
  return fixtureUpload('../test-screenshots/new-features/admin-achievements.png', name, 'image/png')
}

function fixtureMp4(name = 'probe.mp4') {
  return fixtureUpload('../src/static/logo.mp4', name, 'video/mp4')
}

function tinyPng(name = 'probe.png') {
  return {
    name,
    mimeType: 'image/png',
    buffer: Buffer.from(
      'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jX4kAAAAASUVORK5CYII=',
      'base64'
    )
  }
}

function tinyTxt(name = 'probe.txt', body = 'upload-probe') {
  return {
    name,
    mimeType: 'text/plain',
    buffer: Buffer.from(body, 'utf8')
  }
}

function tinyMp4(name = 'probe.mp4') {
  return {
    name,
    mimeType: 'video/mp4',
    buffer: Buffer.from('00000020667479706d703432000000006d70343269736f6d', 'hex')
  }
}

async function loginApi(request, user = MANAGER) {
  const res = await request.post(`${API_BASE}/auth/login`, { data: user })
  const body = await res.json()
  expect(body.code).toBe(200)
  expect(body.data?.token).toBeTruthy()
  return body.data
}

function uploadTarget(type) {
  const mapping = {
    'news-image': { service: 'rk-content', bizType: 'news-image' },
    'news-video': { service: 'rk-content', bizType: 'news-video' },
    'news-attachment': { service: 'rk-content', bizType: 'news-attachment' },
    'notice-cover': { service: 'rk-user', bizType: 'notice-cover' },
    'notice-attachment': { service: 'rk-user', bizType: 'notice-attachment' },
    'activity-cover': { service: 'rk-activity', bizType: 'activity-cover' },
    'activity-attachment': { service: 'rk-activity', bizType: 'activity-attachment' },
    'competition-cover': { service: 'rk-activity', bizType: 'competition-cover' },
    'competition-rules': { service: 'rk-activity', bizType: 'competition-rules' },
    'competition-materials': { service: 'rk-activity', bizType: 'competition-materials' },
    'competition-results': { service: 'rk-activity', bizType: 'competition-results' },
  }
  return mapping[type]
}

async function uploadFile(request, login, file, type) {
  const res = await request.post(`${API_BASE}/api/files/upload`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    },
    multipart: {
      file,
      ...uploadTarget(type)
    }
  })
  const body = await res.json()
  expect(body.code).toBe(200)
  expect(body.data?.id || body.data?.fileUrl || body.data?.url).toBeTruthy()
  return body.data
}

test('real news upload flow should persist cover video and attachment urls', async ({ request }) => {
  const login = await loginApi(request)
  const seed = Date.now()

  const cover = await uploadFile(request, login, visiblePng(`news-cover-${seed}.png`), 'news-image')
  const video = await uploadFile(request, login, fixtureMp4(`news-video-${seed}.mp4`), 'news-video')
  const attachment = await uploadFile(request, login, tinyTxt(`news-attachment-${seed}.txt`, 'news-attachment'), 'news-attachment')

  const title = `RealNewsUpload${seed}`
  const createRes = await request.post(`${API_BASE}/api/news/add`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    },
    data: {
      title,
      category: '绀惧洟鍔ㄦ€?,
      coverImage: cover.relativePath || cover.path || cover.url,
      videoUrl: video.relativePath || video.path || video.url,
      attachmentUrl: attachment.relativePath || attachment.path || attachment.url,
      summary: 'real upload news summary',
      content: 'real upload news content',
      author: 'codex',
      isFeatured: 0,
      isCrossTenant: 0,
      isPublished: 1
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)

  const latestRes = await request.get(`${API_BASE}/api/news/latest?limit=50`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const latestBody = await latestRes.json()
  expect(latestBody.code).toBe(200)
  const created = (latestBody.data || []).find((item) => item.title === title)
  expect(created?.id).toBeTruthy()

  const detailRes = await request.get(`${API_BASE}/api/news/${created.id}`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const detailBody = await detailRes.json()
  expect(detailBody.code).toBe(200)
  expect(detailBody.data.coverImage).toBe(cover.url)
  expect(detailBody.data.videoUrl).toBe(video.url)
  expect(detailBody.data.attachmentUrl).toBe(attachment.url)
})

test('real notice upload flow should persist cover and attachment urls', async ({ request }) => {
  const login = await loginApi(request)
  const seed = Date.now()

  const cover = await uploadFile(request, login, visiblePng(`notice-cover-${seed}.png`), 'notice-cover')
  const attachment = await uploadFile(request, login, tinyTxt(`notice-attachment-${seed}.txt`, 'notice-attachment'), 'notice-attachment')

  const title = `RealNoticeUpload${seed}`
  const createRes = await request.post(`${API_BASE}/notifications/api/notices/create`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    },
    data: {
      title,
      content: 'real notice content',
      coverImage: cover.relativePath || cover.path || cover.url,
      attachmentUrl: attachment.relativePath || attachment.path || attachment.url,
      noticeType: 4,
      isTop: 0,
      isPublished: false
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)

  const listRes = await request.get(`${API_BASE}/notifications/api/notices/list`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const listBody = await listRes.json()
  expect(listBody.code).toBe(200)
  const created = (listBody.data || []).find((item) => item.title === title)
  expect(created).toBeTruthy()
  expect(created.coverImage).toBe(cover.url)
  expect(created.attachmentUrl).toBe(attachment.url)
})

test('real activity upload flow should persist cover and attachment urls', async ({ request }) => {
  const login = await loginApi(request)
  const seed = Date.now()

  const cover = await uploadFile(request, login, visiblePng(`activity-cover-${seed}.png`), 'activity-cover')
  const attachment = await uploadFile(request, login, tinyTxt(`activity-attachment-${seed}.txt`, 'activity-attachment'), 'activity-attachment')

  const title = `RealActivityUpload${seed}`
  const createRes = await request.post(`${API_BASE}/api/activity/add`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    },
    data: {
      activityName: title,
      activityType: 1,
      organizer: 'manager_a',
      startTime: '2026-05-10 10:00:00',
      endTime: '2026-05-11 10:00:00',
      registrationStartTime: '2026-05-01 10:00:00',
      registrationEndTime: '2026-05-09 10:00:00',
      location: 'real-room',
      coverImage: cover.relativePath || cover.path || cover.url,
      attachmentUrl: attachment.relativePath || attachment.path || attachment.url,
      maxParticipants: 20,
      isCrossTenant: 0,
      content: 'real activity content'
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)
  expect(createBody.data).toBeTruthy()

  const detailRes = await request.get(`${API_BASE}/api/activity/${createBody.data}`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const detailBody = await detailRes.json()
  expect(detailBody.code).toBe(200)
  expect(detailBody.data.activityName).toBe(title)
  expect(detailBody.data.coverImage).toBe(cover.url)
  expect(detailBody.data.attachmentUrl).toBe(attachment.url)
})

test('real competition upload flow should persist all file urls', async ({ request }) => {
  const login = await loginApi(request)
  const seed = Date.now()

  const cover = await uploadFile(request, login, visiblePng(`competition-cover-${seed}.png`), 'competition-cover')
  const rules = await uploadFile(request, login, tinyTxt(`competition-rules-${seed}.txt`, 'competition-rules'), 'competition-rules')
  const materials = await uploadFile(request, login, tinyTxt(`competition-materials-${seed}.txt`, 'competition-materials'), 'competition-materials')
  const results = await uploadFile(request, login, tinyTxt(`competition-results-${seed}.txt`, 'competition-results'), 'competition-results')

  const title = `RealCompetitionUpload${seed}`
  const createRes = await request.post(`${API_BASE}/api/competition`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    },
    data: {
      title,
      subtitle: 'real',
      description: 'real competition description',
      content: 'real competition content',
      competitionType: 'academic',
      level: 'school',
      organizer: 'manager_a',
      location: 'real-lab',
      registrationStart: '2026-05-10 10:00:00',
      registrationEnd: '2026-05-11 10:00:00',
      competitionStart: '2026-05-20 10:00:00',
      competitionEnd: '2026-05-21 10:00:00',
      maxParticipants: 20,
      coverImage: cover.relativePath || cover.path || cover.url,
      rulesFile: rules.relativePath || rules.path || rules.url,
      materialsFile: materials.relativePath || materials.path || materials.url,
      resultsFile: results.relativePath || results.path || results.url,
      awards: 'none',
      contactPerson: 'codex',
      contactPhone: '13800138000',
      contactEmail: 'codex@example.com',
      isFeatured: false,
      isCrossTenant: false,
      isPublished: false,
      priority: 0
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)
  expect(createBody.data).toBeTruthy()

  const detailRes = await request.get(`${API_BASE}/api/competition/${createBody.data}`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const detailBody = await detailRes.json()
  expect(detailBody.code).toBe(200)
  expect(detailBody.data.title).toBe(title)
  expect(detailBody.data.coverImage).toBe(cover.url)
  expect(detailBody.data.rulesFile).toBe(rules.url)
  expect(detailBody.data.materialsFile).toBe(materials.url)
  expect(detailBody.data.resultsFile).toBe(results.url)
})

test('real works update flow should invalidate cached detail and list data', async ({ request }) => {
  const login = await loginApi(request)
  const seed = Date.now()

  const createTitle = `RealWorkUpload${seed}`
  const updateTitle = `${createTitle}-updated`
  const createCover = `rk-content/work-cover/${seed}-create.png`
  const createVideo = `rk-content/work-video/${seed}-create.mp4`
  const updateCover = `rk-content/work-cover/${seed}-update.png`
  const updateVideo = `rk-content/work-video/${seed}-update.mp4`

  const createRes = await request.post(`${API_BASE}/api/works`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    },
    data: {
      title: createTitle,
      description: 'real work description',
      content: 'real work content',
      category: '杞欢椤圭洰',
      authors: 'codex',
      technologies: 'node',
      coverImage: createCover,
      demoVideo: createVideo,
      isFeatured: false,
      displayOrder: 0
    }
  })
  const createBody = await createRes.json()
  expect(createBody.code).toBe(200)

  const listBeforeRes = await request.get(`${API_BASE}/api/works/list`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const listBeforeBody = await listBeforeRes.json()
  expect(listBeforeBody.code).toBe(200)
  const created = (listBeforeBody.data || []).find((item) => item.title === createTitle)
  expect(created?.id).toBeTruthy()

  const detailBeforeRes = await request.get(`${API_BASE}/api/works/${created.id}`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const detailBeforeBody = await detailBeforeRes.json()
  expect(detailBeforeBody.code).toBe(200)
  expect(detailBeforeBody.data.coverImage).toContain(createCover)

  const updateRes = await request.put(`${API_BASE}/api/works`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    },
    data: {
      id: created.id,
      title: updateTitle,
      description: 'real work description updated',
      content: 'real work content updated',
      category: '杞欢椤圭洰',
      authors: 'codex-updated',
      technologies: 'node,cache',
      coverImage: updateCover,
      demoVideo: updateVideo,
      isFeatured: true,
      displayOrder: 1
    }
  })
  const updateBody = await updateRes.json()
  expect(updateBody.code).toBe(200)

  const detailAfterRes = await request.get(`${API_BASE}/api/works/${created.id}`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const detailAfterBody = await detailAfterRes.json()
  expect(detailAfterBody.code).toBe(200)
  expect(detailAfterBody.data.title).toBe(updateTitle)
  expect(detailAfterBody.data.coverImage).toContain(updateCover)
  expect(detailAfterBody.data.demoVideo).toContain(updateVideo)

  const listAfterRes = await request.get(`${API_BASE}/api/works/list`, {
    headers: {
      Authorization: `Bearer ${login.token}`,
      'X-Tenant-Id': String(login.organizationId)
    }
  })
  const listAfterBody = await listAfterRes.json()
  expect(listAfterBody.code).toBe(200)
  const updated = (listAfterBody.data || []).find((item) => item.id === created.id)
  expect(updated?.title).toBe(updateTitle)
  expect(updated?.coverImage).toContain(updateCover)
})
