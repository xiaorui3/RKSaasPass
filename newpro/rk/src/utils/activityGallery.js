function toTime(value) {
  if (!value) return null
  const normalized = typeof value === 'string' ? value.replace(' ', 'T') : value
  const time = new Date(normalized).getTime()
  return Number.isNaN(time) ? null : time
}

function startOfDay(value) {
  const time = toTime(value)
  if (time === null) return null
  const date = new Date(time)
  date.setHours(0, 0, 0, 0)
  return date.getTime()
}

function endOfDay(value) {
  const time = toTime(value)
  if (time === null) return null
  const date = new Date(time)
  date.setHours(23, 59, 59, 999)
  return date.getTime()
}

function searchableText(activity = {}) {
  return [
    activity.activityName,
    activity.title,
    activity.organizer,
    activity.location,
    activity.activityCode,
    activity.startTime,
    activity.endTime
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

function matchesKeyword(activity, keyword) {
  const tokens = String(keyword || '')
    .trim()
    .toLowerCase()
    .split(/\s+/)
    .filter(Boolean)
  if (!tokens.length) return true
  const text = searchableText(activity)
  return tokens.every((token) => text.includes(token))
}

function matchesDateRange(activity, dateRange = []) {
  if (!Array.isArray(dateRange) || dateRange.length === 0) return true
  const [start, end] = dateRange
  const startAt = startOfDay(start)
  const endAt = endOfDay(end)
  const activityStart = toTime(activity.startTime)
  const activityEnd = toTime(activity.endTime) || activityStart
  if (activityStart === null && activityEnd === null) return false
  if (startAt !== null && (activityEnd ?? activityStart) < startAt) return false
  if (endAt !== null && (activityStart ?? activityEnd) > endAt) return false
  return true
}

export function filterGalleryActivities(activities = [], filters = {}) {
  const status = filters.status === '' || filters.status === null ? null : filters.status
  return [...activities]
    .filter((activity) => matchesKeyword(activity, filters.keyword))
    .filter((activity) => matchesDateRange(activity, filters.dateRange))
    .filter((activity) => status === null || Number(activity.activityStatus ?? activity.status) === Number(status))
    .sort((left, right) => (toTime(right.startTime) || 0) - (toTime(left.startTime) || 0))
}

export function buildGalleryActivityQuery(filters = {}) {
  const params = { page: 1, size: Number(filters.size) > 0 ? Number(filters.size) : 500 }
  const keyword = String(filters.keyword || '').trim()
  if (keyword) {
    params.keyword = keyword
    params.title = keyword
  }
  if (filters.recentMode && filters.recentMode !== 'all') {
    const days = Number(String(filters.recentMode).replace(/\D/g, ''))
    if (days > 0) {
      params.recentDays = days
    }
  }
  if (filters.status !== '' && filters.status !== null && filters.status !== undefined) {
    params.status = filters.status
  }
  if (Array.isArray(filters.dateRange)) {
    const [start, end] = filters.dateRange
    if (start) {
      params.startTimeBegin = `${String(start).slice(0, 10)} 00:00:00`
    }
    if (end) {
      params.startTimeEnd = `${String(end).slice(0, 10)} 23:59:59`
    }
  }
  return params
}
