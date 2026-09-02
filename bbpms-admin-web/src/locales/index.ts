import { createI18n } from 'vue-i18n'
import zhCN from './zh-CN'
import enUS from './en-US'

export type Locale = 'zh-CN' | 'en-US'

const stored = (localStorage.getItem('bbpms_locale') as Locale) || 'zh-CN'

const i18n = createI18n({
  legacy: false,
  globalInjection: true,
  locale: stored,
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS
  }
})

export function setLocale(l: Locale) {
  i18n.global.locale.value = l
  localStorage.setItem('bbpms_locale', l)
  document.documentElement.setAttribute('lang', l)
}

export default i18n