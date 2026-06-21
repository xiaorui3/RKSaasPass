const SUPPORTED_LANGS = new Set(['zh', 'en'])

export function resolveInitialLanguage(storage = globalThis?.localStorage) {
  const savedLang = storage?.getItem?.('rk-lang') || 'zh'
  return SUPPORTED_LANGS.has(savedLang) ? savedLang : 'zh'
}
