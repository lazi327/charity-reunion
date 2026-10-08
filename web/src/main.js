import { createApp } from 'vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import Lamp from './components/Lamp.vue'
import router from './router'
import './style.css'

const app = createApp(App)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.component('Lamp', Lamp)
// 让可点击的卡片/标签对键盘可达:role=button + tabindex=0 + Enter/Space 触发原生 click
app.directive('tappable', {
  mounted(el) {
    if (!el.hasAttribute('role')) el.setAttribute('role', 'button')
    if (!el.hasAttribute('tabindex')) el.setAttribute('tabindex', '0')
    el.addEventListener('keydown', (e) => {
      if (e.key === 'Enter' || e.key === ' ' || e.key === 'Spacebar') {
        e.preventDefault()
        el.click()
      }
    })
  }
})
app.use(ElementPlus, { locale: zhCn })
app.use(router)
app.mount('#app')
