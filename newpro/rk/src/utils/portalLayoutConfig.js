export const PORTAL_HOME_LAYOUT_CONFIG_KEY = 'portal.home.layout.config'

export const DEFAULT_HOME_SECTIONS = [
  { key: 'quickEntry', title: '快捷入口', visible: true, order: 10 },
  { key: 'activities', title: '近期活动', visible: true, order: 20 },
  { key: 'news', title: '最新动态', visible: true, order: 30 },
  { key: 'notices', title: '通知公告', visible: true, order: 40 }
]

export const DEFAULT_HOME_ENTRY_MODULES = [
  { key: 'activities', title: '社团活动', description: '发现精彩活动，报名参与', icon: '活动', route: '/activities', className: 'e-activity', visible: true, order: 10 },
  { key: 'credit', title: '第二课堂', description: '学分认证、成绩单', icon: '学分', route: '/credit', className: 'e-credit', visible: true, order: 20 },
  { key: 'news', title: '新闻动态', description: '社团资讯、最新公告', icon: '新闻', route: '/news', className: 'e-news', visible: true, order: 30 },
  { key: 'works', title: '作品展示', description: '创意项目、技术成果', icon: '作品', route: '/works', className: 'e-works', visible: true, order: 40 },
  { key: 'competition', title: '学科竞赛', description: '报名参赛、查看成绩', icon: '竞赛', route: '/competition', className: 'e-competition', visible: true, order: 50 },
  { key: 'about', title: '关于我们', description: '社团文化、加入方式', icon: '概况', route: '/about', className: 'e-about', visible: true, order: 60 }
]

const clone = (value) => JSON.parse(JSON.stringify(value))

const toBoolean = (value, fallback = true) => {
  if (value === true || value === false) return value
  if (value === 1 || value === '1') return true
  if (value === 0 || value === '0') return false
  return fallback
}

const toOrder = (value, fallback) => {
  const number = Number(value)
  return Number.isFinite(number) ? number : fallback
}

const parseConfig = (raw) => {
  if (!raw) return {}
  if (typeof raw === 'string') {
    try {
      return JSON.parse(raw)
    } catch {
      return {}
    }
  }
  return typeof raw === 'object' ? raw : {}
}

const normalizeItems = (defaults, configuredItems) => {
  const configuredByKey = new Map(
    (Array.isArray(configuredItems) ? configuredItems : [])
      .filter((item) => item?.key)
      .map((item) => [item.key, item])
  )

  return defaults.map((item) => {
    const configured = configuredByKey.get(item.key) || {}
    return {
      ...item,
      title: String(configured.title || item.title).trim(),
      description: String(configured.description || item.description || '').trim(),
      icon: String(configured.icon || item.icon || '').trim(),
      visible: toBoolean(configured.visible, item.visible),
      order: toOrder(configured.order, item.order)
    }
  })
}

export function normalizePortalHomeLayoutConfig(raw) {
  const config = parseConfig(raw)
  return {
    sections: normalizeItems(DEFAULT_HOME_SECTIONS, config.sections),
    entryModules: normalizeItems(DEFAULT_HOME_ENTRY_MODULES, config.entryModules)
  }
}

export function orderedVisibleItems(items) {
  return [...(Array.isArray(items) ? items : [])]
    .filter((item) => item.visible !== false)
    .sort((left, right) => toOrder(left.order, 0) - toOrder(right.order, 0))
}

export function serializePortalHomeLayoutConfig(config) {
  const normalized = normalizePortalHomeLayoutConfig(config)
  return JSON.stringify({
    sections: normalized.sections.map(({ key, title, visible, order }) => ({ key, title, visible, order })),
    entryModules: normalized.entryModules.map(({ key, title, description, icon, visible, order }) => ({
      key,
      title,
      description,
      icon,
      visible,
      order
    }))
  })
}

export function findConfigRecord(configs, key = PORTAL_HOME_LAYOUT_CONFIG_KEY) {
  return (Array.isArray(configs) ? configs : []).find((item) => item.configKey === key) || null
}
