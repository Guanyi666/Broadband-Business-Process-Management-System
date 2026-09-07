import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import App from './App.vue'
import router from './router'
import pinia from './stores'
import i18n from './locales'
import { permissionDirective } from './directives/permission'
import 'element-plus/dist/index.css'
// 主题变量层：必须在 element-plus 样式之后，才能覆盖 --el-* 变量
import '@/assets/styles/theme.scss'
import '@/assets/styles/global.scss'

const app = createApp(App)

app.use(pinia)
app.use(router)
app.use(i18n)
app.use(ElementPlus, { locale: zhCn })
app.directive('permission', permissionDirective)
app.directive('hasPermi', permissionDirective)

app.mount('#app')