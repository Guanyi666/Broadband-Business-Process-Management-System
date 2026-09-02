import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import i18n from './locales'
import { permissionDirective } from './directives/permission'
import 'element-plus/dist/index.css'
import '@/assets/styles/global.scss'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(ElementPlus)
app.directive('permission', permissionDirective)

app.mount('#app')