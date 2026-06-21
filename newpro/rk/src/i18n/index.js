import { createI18n } from 'vue-i18n'
import zh from './zh.js'
import en from './en.js'
import { resolveInitialLanguage } from './language.js'

const savedLang = resolveInitialLanguage()

const i18n = createI18n({
  legacy: false,
  locale: savedLang,
  fallbackLocale: 'zh',
  messages: { zh, en }
})

if (typeof document !== 'undefined') {
  document.documentElement.setAttribute('lang', savedLang)
}

export default i18n

export function setLanguage(lang) {
  i18n.global.locale.value = lang
  localStorage.setItem('rk-lang', lang)
  document.documentElement.setAttribute('lang', lang)
}

export function toggleLanguage() {
  const current = i18n.global.locale.value
  const next = current === 'zh' ? 'en' : 'zh'
  setLanguage(next)
  return next
}
