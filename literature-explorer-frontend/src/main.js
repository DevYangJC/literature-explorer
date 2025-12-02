import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import router from './router/index.js'
import App from './App.vue'
import './style.css'

const app = createApp(App)
const pinia = createPinia()

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
})

// 全局错误处理
app.config.errorHandler = (error, instance, info) => {
  console.error('全局错误捕获:', error)
  console.error('组件实例:', instance)
  console.error('错误信息:', info)
  
  // 防止错误冒泡到控制台显示
  // 可以在这里添加错误上报逻辑
  return false
}

// 处理未捕获的 Promise 错误
window.addEventListener('unhandledrejection', (event) => {
  console.error('未处理的 Promise 错误:', event.reason)
  event.preventDefault() // 防止错误显示在控制台
})

// 处理全局 JavaScript 错误
window.addEventListener('error', (event) => {
  console.error('全局 JavaScript 错误:', event.error)
  event.preventDefault() // 防止错误显示在页面上
})

// 全局 DOM 监控，清理 Mermaid 错误信息和临时元素
const startGlobalErrorCleanup = () => {
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      mutation.addedNodes.forEach((node) => {
        if (node.nodeType === Node.ELEMENT_NODE) {
          // 检查新添加的元素是否包含 Mermaid 错误信息
          const element = node
          const text = element.textContent || ''
          const id = element.id || ''
          
          // 清理Mermaid临时元素
          if (id.includes('mermaid-modal-') || 
              id.includes('dmermaid-modal-') ||
              (id.length > 10 && /^d[0-9-]+/.test(id))) {
            
            // 确保不是用户内容区域的元素
            if (!element.closest('.streaming-content') && 
                !element.closest('.markdown-content') &&
                !element.closest('.guide-content')) {
              setTimeout(() => {
                if (element.parentNode) {
                  console.log('全局清理Mermaid临时元素:', id)
                  element.parentNode.removeChild(element)
                }
              }, 100)
            }
          }
          
          // 清理错误信息
          if (text.includes('Syntax error in text') || 
              text.includes('mermaid version') ||
              text.includes('Parse error on line') ||
              text.includes('Expecting')) {
            
            // 延迟移除，确保不影响正常渲染
            setTimeout(() => {
              if (element.parentNode) {
                element.parentNode.removeChild(element)
              }
            }, 50)
          }
          
          // 检查子元素
          const errorElements = element.querySelectorAll('*')
          errorElements.forEach(child => {
            const childText = child.textContent || ''
            if (childText.includes('Syntax error in text') || 
                childText.includes('mermaid version') ||
                childText.includes('Parse error on line')) {
              setTimeout(() => {
                if (child.parentNode) {
                  child.parentNode.removeChild(child)
                }
              }, 50)
            }
          })
        }
      })
    })
  })
  
  // 开始监控 body 的变化
  observer.observe(document.body, {
    childList: true,
    subtree: true
  })
}

app.mount('#app')

// 应用挂载后启动全局错误清理
setTimeout(() => {
  startGlobalErrorCleanup()
}, 1000)
