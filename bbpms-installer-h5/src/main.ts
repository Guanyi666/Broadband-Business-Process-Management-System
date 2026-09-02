import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'

import Vant, { Lazyload, showNotify } from 'vant'
import 'vant/lib/index.css'

import './assets/styles/index.scss'

const app = createApp(App)
app.use(createPinia())
app.use(router)
app.use(Vant)
app.use(Lazyload, { lazyComponent: true })
// Vant 4 Dialog/Notify/Toast/ImagePreview are function components — not Vue plugins.

app.config.errorHandler = (err, _instance, info) => {
  console.error('[BBPMS Error]', err, info)
  showNotify({ type: 'danger', message: '应用异常,请稍后再试' })
}

app.mount('#app')
