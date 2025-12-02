<template>
  <el-dialog
    v-model="dialogVisibleComputed"
    title="导入文献"
    width="800px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="!isGenerating"
    @close="handleClose"
  >
    <!-- 上传表单 -->
    <div v-if="!isGenerating" class="upload-form">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="选择文件" prop="file" required>
          <el-upload
            ref="uploadRef"
            :file-list="fileList"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
            :accept="acceptedFileTypes"
            :limit="1"
            drag
            class="upload-area"
          >
            <div class="upload-content">
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">
                <p>将文件拖拽到此处，或<em>点击上传</em></p>
                <p class="upload-tip">
                  支持 PDF、Word、Markdown、Excel 格式，单个文件不超过 50MB
                </p>
              </div>
            </div>
          </el-upload>
        </el-form-item>
        
        <el-form-item label="Kimi API Key" prop="apiKey" required>
          <el-input
            v-model="formData.apiKey"
            type="password"
            placeholder="请输入您的 Kimi API Key"
            show-password
            clearable
          />
          <div class="api-key-tip">
            <el-text size="small" type="info">
              API Key 用于生成智能阅读指南，会自动保存到浏览器本地
            </el-text>
            <el-button 
              v-if="formData.apiKey" 
              link 
              type="danger" 
              size="small" 
              @click="clearSavedApiKey"
              style="margin-left: 8px;"
            >
              清除保存的密钥
            </el-button>
          </div>
        </el-form-item>
      </el-form>
    </div>

    <!-- 生成进度区域 -->
    <div v-else class="generation-area">
      <div class="generation-header">
        <el-icon v-if="!generationComplete" class="loading-icon"><Loading /></el-icon>
        <el-icon v-else class="success-icon"><CircleCheck /></el-icon>
        <div class="generation-info">
          <h3>{{ generationComplete ? '阅读指南生成完成！' : '正在生成阅读指南...' }}</h3>
          <p v-if="progressMessage" class="progress-message">
            <el-icon v-if="!generationComplete"><Loading /></el-icon>
            {{ progressMessage }}
          </p>
        </div>
      </div>
      
      <div class="progress-content">
        <el-card shadow="never" class="content-card">
          <div
            ref="contentRef"
            class="streaming-content"
            v-html="renderedContent"
          ></div>
          
          <!-- 打字机光标 -->
          <span v-if="isStreaming" class="cursor">|</span>
        </el-card>
      </div>
      
      <div v-if="generationComplete" class="generation-footer">
        <el-result
          icon="success"
          title="生成完成！"
          sub-title="阅读指南已成功生成，文献已导入到您的文库中"
        >
          <template #extra>
            <el-button type="primary" @click="handleClose">
              关闭
            </el-button>
          </template>
        </el-result>
      </div>
      
      <div v-if="generationError" class="generation-error">
        <el-result
          icon="error"
          title="生成失败"
          :sub-title="generationError"
        >
          <template #extra>
            <el-button @click="resetForm">重新尝试</el-button>
            <el-button type="primary" @click="handleClose">关闭</el-button>
          </template>
        </el-result>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer v-if="!isGenerating">
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :disabled="!canSubmit"
          :loading="isGenerating"
          @click="handleSubmit"
        >
          生成指南
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading, CircleCheck } from '@element-plus/icons-vue'
import { marked } from 'marked'
import mermaid from 'mermaid'
import { fetchEventSource } from '@microsoft/fetch-event-source'

// Props 和 Emits
const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'success'])

// 响应式数据
const dialogVisible = ref(false)
const formRef = ref()
const uploadRef = ref()
const contentRef = ref()

// 表单数据
const formData = ref({
  file: null,
  apiKey: ''
})

const fileList = ref([])

// API Key 存储相关
const API_KEY_STORAGE_KEY = 'literature_assistant_api_key'

// 从 localStorage 读取保存的 API Key
const loadSavedApiKey = () => {
  try {
    const savedKey = localStorage.getItem(API_KEY_STORAGE_KEY)
    if (savedKey) {
      formData.value.apiKey = savedKey
    }
  } catch (error) {
    console.warn('读取保存的 API Key 失败:', error)
  }
}

// 保存 API Key 到 localStorage
const saveApiKey = (apiKey) => {
  try {
    if (apiKey && apiKey.trim()) {
      localStorage.setItem(API_KEY_STORAGE_KEY, apiKey.trim())
    }
  } catch (error) {
    console.warn('保存 API Key 失败:', error)
  }
}

// 清除保存的 API Key
const clearSavedApiKey = () => {
  try {
    localStorage.removeItem(API_KEY_STORAGE_KEY)
    formData.value.apiKey = ''
    ElMessage.success('已清除保存的 API Key')
  } catch (error) {
    console.warn('清除保存的 API Key 失败:', error)
    ElMessage.error('清除失败，请重试')
  }
}

// 生成状态
const isGenerating = ref(false)
const isStreaming = ref(false)
const generationComplete = ref(false)
const generationError = ref('')
const streamingContent = ref('') // 只存储content类型的内容
const renderedContent = ref('')
const progressMessage = ref('') // 存储进度和状态消息

// SSE 连接控制和状态管理
let abortController = null
const sseState = ref({
  isConnected: false,
  isReconnecting: false,
  reconnectAttempts: 0,
  maxReconnectAttempts: 3,
  reconnectDelay: 1000
})

// 计算属性
const dialogVisibleComputed = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const canSubmit = computed(() => {
  return formData.value.file && formData.value.apiKey.trim()
})

const acceptedFileTypes = '.pdf,.doc,.docx,.md,.markdown,.xls,.xlsx'

// 表单验证规则
const formRules = {
  file: [
    { required: true, message: '请选择要上传的文件', trigger: 'change' }
  ],
  apiKey: [
    { required: true, message: '请输入 Kimi API Key', trigger: 'blur' },
    { min: 10, message: 'API Key 长度不能少于 10 个字符', trigger: 'blur' }
  ]
}

// 监听对话框显示状态
watch(() => props.modelValue, (visible) => {
  if (visible) {
    resetForm()
  } else {
    closeSSEConnection()
  }
})

// 防抖处理渲染
let renderTimer = null
const RENDER_DEBOUNCE_TIME = 50 // 50ms 防抖

// 监听流式内容变化，使用防抖机制优化渲染
watch(streamingContent, (content) => {
  // 清除之前的定时器
  if (renderTimer) {
    clearTimeout(renderTimer)
  }
  
  // 设置新的防抖定时器
  renderTimer = setTimeout(() => {
    renderMarkdownContent(content)
  }, RENDER_DEBOUNCE_TIME)
}, { flush: 'post' })

// 渲染 Markdown 内容
const renderMarkdownContent = async (content) => {
  if (!content) {
    renderedContent.value = ''
    return
  }
  
  try {
    // 配置 marked 选项
    marked.setOptions({
      breaks: false,       // 禁用自动换行符转换，避免过多空行
      gfm: true,          // 支持 GitHub Flavored Markdown
      headerIds: false,   // 禁用标题 ID 生成
      mangle: false,      // 禁用标题 ID 混淆
      sanitize: false,    // 允许 HTML
      pedantic: false,    // 不严格遵循原始markdown.pl
      smartypants: false, // 禁用智能标点符号转换
      silent: true        // 静默模式，不输出警告
    })
    
    // 渲染 Markdown
    renderedContent.value = marked(content)
    
    // 等待 DOM 更新后处理后续操作
    await nextTick()
    
    // 并行处理 Mermaid 图表渲染和滚动
    await Promise.all([
      renderMermaidCharts(),
      scrollToBottom()
    ])
    
    // 渲染完成后清理错误信息
    setTimeout(() => {
      cleanupMermaidErrors()
    }, 200)
    
  } catch (error) {
    console.error('Markdown 渲染错误:', error)
    // 安全的降级显示
    renderedContent.value = `<div class="render-error">
      <p>渲染失败，显示原始内容：</p>
      <pre>${escapeHtml(content)}</pre>
    </div>`
  }
}

// HTML 转义
const escapeHtml = (text) => {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
}

// 滚动到底部
const scrollToBottom = async () => {
  if (contentRef.value) {
    // 使用 smooth 滚动以提升用户体验
    contentRef.value.scrollTo({
      top: contentRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

// 验证 Mermaid 语法
const isValidMermaidSyntax = (code) => {
  if (!code || typeof code !== 'string') return false
  
  const trimmedCode = code.trim()
  if (!trimmedCode) return false
  
  // 基本语法检查
  const validDiagramTypes = [
    'graph', 'flowchart', 'sequenceDiagram', 'classDiagram', 
    'stateDiagram', 'erDiagram', 'journey', 'gantt', 'pie',
    'gitgraph', 'mindmap', 'timeline', 'quadrantChart'
  ]
  
  // 检查是否以有效的图表类型开始
  const firstLine = trimmedCode.split('\n')[0].trim().toLowerCase()
  const hasValidStart = validDiagramTypes.some(type => 
    firstLine.startsWith(type.toLowerCase())
  )
  
  if (!hasValidStart) return false
  
  // 检查常见的语法问题
  const lines = trimmedCode.split('\n')
  
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i].trim()
    if (!line) continue
    
    // 检查是否包含无效字符或格式
    // 避免包含数字开头的节点ID（这是常见的错误来源）
    if (line.includes('|') && line.includes('[')) {
      // 检查节点定义中的问题
      const nodeMatch = line.match(/(\w+)\s*\[([^\]]*)\]/)
      if (nodeMatch) {
        const nodeId = nodeMatch[1]
        const nodeText = nodeMatch[2]
        
        // 节点ID不能以数字开头
        if (/^\d/.test(nodeId)) return false
        
        // 检查节点文本中是否包含未闭合的标签
        if (nodeText.includes('<') && !nodeText.includes('>')) return false
      }
    }
    
    // 检查箭头语法
    if (line.includes('-->') || line.includes('->')) {
      // 确保箭头两边都有有效的节点
      const arrowParts = line.split(/-->|->/)
      if (arrowParts.length !== 2) continue
      
      const leftPart = arrowParts[0].trim()
      const rightPart = arrowParts[1].trim()
      
      if (!leftPart || !rightPart) return false
    }
  }
  
  return true
}

// 清理 Mermaid 错误信息和临时元素
const cleanupMermaidErrors = () => {
  if (!contentRef.value) return
  
  try {
    // 清理全局的Mermaid临时元素
    cleanupGlobalMermaidElements()
    
    // 查找并移除所有可能的错误信息元素
    const errorSelectors = [
      '[class*="error"]',
      '[class*="mermaid-error"]', 
      '.mermaid-syntax-error',
      '.error-text',
      'div[style*="color: red"]',
      'div[style*="color:red"]',
      'span[style*="color: red"]',
      'span[style*="color:red"]'
    ]
    
    errorSelectors.forEach(selector => {
      const elements = contentRef.value.querySelectorAll(selector)
      elements.forEach(element => {
        const text = element.textContent || ''
        if (text.includes('Syntax error') || 
            text.includes('Parse error') || 
            text.includes('mermaid version') || 
            text.includes('Expecting') ||
            text.includes('error in text')) {
          element.remove()
        }
      })
    })
    
    // 更全面地移除包含错误文本的元素
    const allElements = contentRef.value.querySelectorAll('*')
    allElements.forEach(element => {
      const text = element.textContent || ''
      const innerHTML = element.innerHTML || ''
      
      // 检查是否是 Mermaid 错误元素
      if ((text.includes('Syntax error in text') && text.includes('mermaid version')) ||
          (text.includes('Parse error') && text.includes('mermaid')) ||
          innerHTML.includes('mermaid version 10.9.4')) {
        
        // 如果是直接的错误元素，移除它
        if (element.children.length === 0 || 
            (element.children.length === 1 && element.children[0].tagName === 'BR')) {
          element.remove()
        } else {
          // 如果包含其他内容，只清空错误文本
          const walker = document.createTreeWalker(
            element,
            NodeFilter.SHOW_TEXT,
            null,
            false
          )
          
          const textNodesToRemove = []
          let node
          
          while (node = walker.nextNode()) {
            const nodeText = node.textContent || ''
            if (nodeText.includes('Syntax error in text') || 
                nodeText.includes('mermaid version') ||
                nodeText.includes('Parse error on line')) {
              textNodesToRemove.push(node)
            }
          }
          
          textNodesToRemove.forEach(textNode => {
            if (textNode.parentNode) {
              textNode.parentNode.removeChild(textNode)
            }
          })
        }
      }
    })
    
  } catch (error) {
    console.warn('清理 Mermaid 错误信息时出错:', error)
  }
}

// 清理全局Mermaid临时元素
const cleanupGlobalMermaidElements = () => {
  try {
    // 清理body下的所有mermaid临时元素
    const globalSelectors = [
      'div[id*="dmermaid-modal-"]',
      'div[id*="mermaid-modal-"]',
      'div[id^="d"]', // Mermaid有时会创建以'd'开头的临时元素
      '.mermaid[style*="max-width: 512px"]' // 清理可能的临时mermaid元素
    ]
    
    globalSelectors.forEach(selector => {
      const elements = document.body.querySelectorAll(selector)
      elements.forEach(element => {
        // 检查是否是Mermaid相关的临时元素
        const id = element.id || ''
        const className = element.className || ''
        
        if (id.includes('mermaid-modal-') || 
            id.includes('dmermaid-modal-') ||
            (id.length > 10 && /^d[0-9-]+/.test(id)) ||
            className.includes('mermaid')) {
          
          // 确保不是用户内容区域的元素
          if (!element.closest('.streaming-content') && 
              !element.closest('.markdown-content') &&
              !element.closest('.guide-content')) {
            console.log('清理Mermaid临时元素:', element.id || element.className)
            element.remove()
          }
        }
      })
    })
  } catch (error) {
    console.warn('清理全局Mermaid元素时出错:', error)
  }
}

// 渲染 Mermaid 图表（优化版本）
const renderMermaidCharts = async () => {
  if (!contentRef.value) return
  
  try {
    // 首先清理任何现有的错误信息
    cleanupMermaidErrors()
    
    // 查找所有未处理的 mermaid 代码块
    const mermaidBlocks = contentRef.value.querySelectorAll(
      'pre code.language-mermaid:not([data-mermaid-processed]), code.language-mermaid:not([data-mermaid-processed])'
    )
    
    if (mermaidBlocks.length === 0) return
    

    
    // 并行处理所有 Mermaid 图表
    const renderPromises = Array.from(mermaidBlocks).map(async (block, index) => {
      try {
        const code = (block.textContent || block.innerText || '').trim()
        if (!code) return
        
        // 标记为已处理，防止重复处理
        block.setAttribute('data-mermaid-processed', 'true')
        
        // 创建唯一容器 ID
        const containerId = `mermaid-modal-${Date.now()}-${index}-${Math.random().toString(36).substr(2, 9)}`
        
        // 预验证 Mermaid 语法
        if (!isValidMermaidSyntax(code)) {
          throw new Error('Invalid Mermaid syntax')
        }
        
        // 验证 Mermaid 语法并渲染图表
        const { svg } = await mermaid.render(containerId, code)
        
        // 创建容器
        const container = document.createElement('div')
        container.className = 'mermaid-container'
        container.innerHTML = svg
        
        // 替换原来的代码块
        const parent = block.parentElement
        if (parent && parent.tagName === 'PRE') {
          parent.parentNode?.replaceChild(container, parent)
        } else if (block.parentNode) {
          block.parentNode.replaceChild(container, block)
        }
        

        
      } catch (error) {
        console.error('Mermaid 渲染错误:', error)
        
        // 渲染失败时显示友好的错误提示，而不是原始错误
        const errorContainer = document.createElement('div')
        errorContainer.className = 'mermaid-error-container'
        errorContainer.innerHTML = `
          <div class="mermaid-error">
            <p>图表渲染失败</p>
            <details>
              <summary>查看详情</summary>
              <pre><code>${escapeHtml(block.textContent || '')}</code></pre>
            </details>
          </div>
        `
        
        // 替换原来的代码块
        const parent = block.parentElement
        if (parent && parent.tagName === 'PRE') {
          parent.parentNode?.replaceChild(errorContainer, parent)
        } else if (block.parentNode) {
          block.parentNode.replaceChild(errorContainer, block)
        }
        
        // 标记为已处理，防止重复处理
        errorContainer.setAttribute('data-mermaid-processed', 'true')
        errorContainer.setAttribute('data-mermaid-error', 'true')
      }
    })
    
    // 等待所有图表渲染完成
    await Promise.allSettled(renderPromises)
    
    // 渲染完成后多次清理可能出现的错误信息
    setTimeout(() => {
      cleanupMermaidErrors()
    }, 100)
    
    // 再次清理，确保彻底移除错误信息
    setTimeout(() => {
      cleanupMermaidErrors()
    }, 500)
    
  } catch (error) {
    console.error('Mermaid 批量渲染错误:', error)
    // 即使出错也要清理
    cleanupMermaidErrors()
  }
}

// 组件挂载时初始化 Mermaid
onMounted(() => {
  // 加载保存的 API Key
  loadSavedApiKey()
  
  try {
    mermaid.initialize({
      startOnLoad: false,
      theme: 'default',
      securityLevel: 'loose',
      fontFamily: '-apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", sans-serif',
      fontSize: 14,
      suppressErrorRendering: true, // 抑制错误渲染
      logLevel: 'fatal', // 只记录致命错误
      flowchart: {
        useMaxWidth: true,
        htmlLabels: true
      },
      sequence: {
        useMaxWidth: true
      },
      gantt: {
        useMaxWidth: true
      },
      // 自定义错误处理
      errorCallback: (error) => {
        console.error('Mermaid 初始化错误:', error)
        return false // 阻止默认错误处理
      }
    })

    // 拦截 Mermaid 的错误输出
    const originalConsoleError = console.error
    const originalConsoleWarn = console.warn
    
    console.error = function(...args) {
      // 过滤 Mermaid 相关的错误输出
      const message = args.join(' ')
      if (message.includes('Mermaid') || message.includes('mermaid') || 
          message.includes('Parse error') || message.includes('Expecting')) {
        // 静默处理 Mermaid 错误，不输出到控制台
        return
      }
      originalConsoleError.apply(console, args)
    }
    
    console.warn = function(...args) {
      // 过滤 Mermaid 相关的警告输出
      const message = args.join(' ')
      if (message.includes('Mermaid') || message.includes('mermaid')) {
        return
      }
      originalConsoleWarn.apply(console, args)
    }

  } catch (error) {
    console.error('Mermaid 初始化失败:', error)
  }
})

// 文件选择处理
const handleFileChange = (file) => {
  formData.value.file = file.raw
}

// 文件移除处理
const handleFileRemove = () => {
  formData.value.file = null
  fileList.value = []
}

// 文件上传前检查
const beforeUpload = (file) => {
  const isValidType = /\.(pdf|doc|docx|md|markdown|xls|xlsx)$/i.test(file.name)
  if (!isValidType) {
    ElMessage.error('只支持 PDF、Word、Markdown、Excel 格式的文件！')
    return false
  }
  
  const isValidSize = file.size / 1024 / 1024 < 50 // 50MB
  if (!isValidSize) {
    ElMessage.error('文件大小不能超过 50MB！')
    return false
  }
  
  return false // 阻止自动上传
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    // 保存 API Key 到 localStorage
    saveApiKey(formData.value.apiKey)
    
    await startSSEGeneration()
  } catch (error) {

    ElMessage.error('请检查表单信息')
  }
}

// 启动 SSE 生成流程
const startSSEGeneration = async () => {
  try {
    // 重置所有状态
    resetGenerationState()
    
    // 创建 FormData
    const formDataToSend = new FormData()
    formDataToSend.append('file', formData.value.file)
    formDataToSend.append('apiKey', formData.value.apiKey)
    
    // 开始连接
    await connectSSE(formDataToSend)
    
  } catch (error) {
    handleSSEError(error)
  }
}

// 连接 SSE
const connectSSE = async (formDataToSend) => {
  return new Promise((resolve, reject) => {
    // 创建新的中止控制器
    abortController = new AbortController()
    sseState.value.isConnected = false
    sseState.value.isReconnecting = false
    
    fetchEventSource('/api/literature/generate-guide', {
      method: 'POST',
      body: formDataToSend,
      signal: abortController.signal,
      openWhenHidden: true,
      headers: {
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache'
      },
      
      async onopen(response) {
        if (response.ok && response.headers.get('content-type')?.includes('text/event-stream')) {
          sseState.value.isConnected = true
          sseState.value.reconnectAttempts = 0
          resolve()
        } else {
          const errorText = await response.text()
          const error = new Error(`HTTP ${response.status}: ${response.statusText}${errorText ? ` - ${errorText}` : ''}`)
          reject(error)
        }
      },
      
      onmessage(event) {
        try {
          handleSSEEvent(event)
        } catch (error) {
  
          handleSSEMessage({
            type: 'error',
            data: '消息处理失败: ' + error.message
          })
        }
      },
      
      onerror(error) {

        sseState.value.isConnected = false
        
        // 如果不是主动中止，尝试重连
        if (!abortController?.signal.aborted && sseState.value.reconnectAttempts < sseState.value.maxReconnectAttempts) {
          handleSSEReconnection(formDataToSend)
        } else if (!abortController?.signal.aborted) {
          reject(error)
        }
      },
      
      onclose() {

        sseState.value.isConnected = false
        
        // 如果是意外关闭且还在生成中，尝试重连
        if (!abortController?.signal.aborted && isStreaming.value && !generationComplete.value) {
          handleSSEReconnection(formDataToSend)
        }
      }
    })
  })
}

// 处理 SSE 重连
const handleSSEReconnection = async (formDataToSend) => {
  if (sseState.value.isReconnecting || sseState.value.reconnectAttempts >= sseState.value.maxReconnectAttempts) {
    return
  }
  
  sseState.value.isReconnecting = true
  sseState.value.reconnectAttempts++
  
  const delay = sseState.value.reconnectDelay * sseState.value.reconnectAttempts
  progressMessage.value = `连接中断，正在重连... (${sseState.value.reconnectAttempts}/${sseState.value.maxReconnectAttempts})`
  

  
  await new Promise(resolve => setTimeout(resolve, delay))
  
  try {
    await connectSSE(formDataToSend)
    sseState.value.isReconnecting = false
    progressMessage.value = '重连成功，继续生成...'
  } catch (error) {
    sseState.value.isReconnecting = false
    
    if (sseState.value.reconnectAttempts >= sseState.value.maxReconnectAttempts) {
      handleSSEMessage({
        type: 'error',
        data: '连接失败，已达到最大重试次数'
      })
    }
  }
}



// 处理 SSE 事件
const handleSSEEvent = (event) => {
  
  
  // 统一的消息解析和验证
  const messageData = parseSSEEventData(event)
  if (!messageData) {

    return
  }
  
  // 处理消息
  handleSSEMessage(messageData)
}

// 解析 SSE 事件数据
const parseSSEEventData = (event) => {
  try {
    // 根据事件类型处理不同的数据格式
    switch (event.event) {
      case 'start':
      case 'progress':
      case 'complete':
      case 'error':
        return {
          type: event.event,
          data: event.data || '',
          timestamp: Date.now()
        }
        
      case 'content':
        // content 事件直接传递原始数据
        return {
          type: 'content',
          data: event.data || '',
          timestamp: Date.now()
        }
        
      default:
        // 处理没有明确事件类型的消息
        if (!event.event || event.event === 'message') {
          try {
            // 尝试解析 JSON 格式的消息
            const parsedData = JSON.parse(event.data)
            if (parsedData.type) {
              return {
                ...parsedData,
                timestamp: Date.now()
              }
            }
          } catch (e) {
            // 不是 JSON 格式，可能是纯文本内容

          }
        }
        return null
    }
  } catch (error) {

    return null
  }
}

// 处理 SSE 消息
const handleSSEMessage = (messageData) => {
  const { type, data, timestamp } = messageData
  
  switch (type) {
    case 'start':
      handleStartMessage(data)
      break
      
    case 'progress':
      handleProgressMessage(data)
      break
      
    case 'content':
      handleContentMessage(data)
      break
      
    case 'complete':
      handleCompleteMessage(data)
      break
      
    case 'error':
      handleErrorMessage(data)
      break
      
    default:
      console.warn('未知的消息类型:', type, messageData)
  }
}

// 处理开始消息
const handleStartMessage = (data) => {
  progressMessage.value = data || '开始生成阅读指南...'

}

// 处理进度消息
const handleProgressMessage = (data) => {
  progressMessage.value = data || '正在处理...'

}

// 处理内容消息
const handleContentMessage = (data) => {
  try {
    // 处理特殊标记
    const processedContent = processContentData(data)
    
    if (processedContent !== null) {
      streamingContent.value += processedContent

    }
  } catch (error) {

  }
}

// 处理内容数据
const processContentData = (data) => {
  if (typeof data !== 'string') {
    return String(data || '')
  }
  
  // 处理服务端发送的特殊标记
  return data
    .replace(/<empty-line>/g, '\n')
    .replace(/<empty-space>/g, ' ')
}

// 处理完成消息
const handleCompleteMessage = (data) => {
  isStreaming.value = false
  generationComplete.value = true
  progressMessage.value = data || '生成完成！'
  sseState.value.isConnected = false
  

  ElMessage.success('阅读指南生成完成！')
  
  // 确保最终内容被渲染
  nextTick(() => {
    if (contentRef.value) {
      contentRef.value.scrollTop = contentRef.value.scrollHeight
    }
  })
}

// 处理错误消息
const handleErrorMessage = (data) => {
  const errorMessage = data || '生成过程中发生错误'
  
  isStreaming.value = false
  generationError.value = errorMessage
  sseState.value.isConnected = false
  

  ElMessage.error(errorMessage)
}

// SSE 错误处理
const handleSSEError = (error) => {

  
  if (error.name !== 'AbortError') {
    const errorMessage = error.message || '连接失败，请重试'
    generationError.value = errorMessage
    isGenerating.value = false
    isStreaming.value = false
    sseState.value.isConnected = false
    
    ElMessage.error(errorMessage)
  }
}

// 重置生成状态
const resetGenerationState = () => {
  isGenerating.value = true
  isStreaming.value = true
  generationComplete.value = false
  generationError.value = ''
  streamingContent.value = ''
  renderedContent.value = ''
  progressMessage.value = '正在初始化...'
  
  // 重置 SSE 状态
  sseState.value.reconnectAttempts = 0
  sseState.value.isReconnecting = false
}

// 关闭 SSE 连接
const closeSSEConnection = () => {
  if (abortController) {

    abortController.abort()
    abortController = null
  }
  
  // 重置连接状态
  sseState.value.isConnected = false
  sseState.value.isReconnecting = false
  sseState.value.reconnectAttempts = 0
}

// 重置表单
const resetForm = () => {
  // 关闭 SSE 连接
  closeSSEConnection()
  
  // 清理Mermaid临时元素
  cleanupGlobalMermaidElements()
  
  // 重置所有状态
  isGenerating.value = false
  isStreaming.value = false
  generationComplete.value = false
  generationError.value = ''
  streamingContent.value = ''
  renderedContent.value = ''
  progressMessage.value = ''
  
  // 重置表单数据（保留API Key）
  const savedApiKey = formData.value.apiKey
  formData.value = {
    file: null,
    apiKey: savedApiKey // 保留API Key，方便重新尝试
  }
  fileList.value = []
  
  // 清理表单组件
  if (formRef.value) {
    formRef.value.resetFields()
  }
  if (uploadRef.value) {
    uploadRef.value.clearFiles()
  }
}

// 处理关闭
const handleClose = () => {
  if (generationComplete.value) {
    emit('success')
  }
  emit('update:modelValue', false)
}
</script>

<style scoped>
.upload-form {
  padding: 20px 0;
}

.upload-area {
  width: 100%;
}

.upload-area :deep(.el-upload) {
  width: 100%;
}

.upload-area :deep(.el-upload-dragger) {
  width: 100%;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.upload-content {
  text-align: center;
}

.upload-icon {
  font-size: 48px;
  color: #409eff;
  margin-bottom: 16px;
}

.upload-text p {
  margin: 8px 0;
  font-size: 14px;
  color: #606266;
}

.upload-text em {
  color: #409eff;
  font-style: normal;
}

.upload-tip {
  font-size: 12px;
  color: #909399;
}

.api-key-tip {
  margin-top: 8px;
}

.generation-area {
  padding: 20px 0;
  min-height: auto; /* 移除固定最小高度，根据内容自适应 */
}

.generation-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.loading-icon {
  font-size: 24px;
  color: #409eff;
  margin-right: 12px;
  animation: rotate 2s linear infinite;
  flex-shrink: 0;
}

.success-icon {
  font-size: 24px;
  color: #67c23a;
  margin-right: 12px;
  flex-shrink: 0;
}

@keyframes rotate {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.generation-info {
  text-align: center;
}

.generation-header h3 {
  margin: 0 0 8px 0;
  color: #409eff;
  font-weight: 500;
}

.progress-message {
  margin: 0;
  color: #606266;
  font-size: 14px;
  font-weight: normal;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.progress-content {
  margin-bottom: 24px;
}

.content-card {
  max-height: 400px;
  overflow-y: auto;
}

.streaming-content {
  min-height: 20px; /* 减少最小高度，避免过多空白 */
  line-height: 1.4;
  font-size: 14px;
  color: #333;
  word-break: break-word;
  padding: 16px;
  /* 保留空格和换行 */
  /* white-space: pre-wrap; */
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', 'Roboto', sans-serif;
}

.streaming-content :deep(h1),
.streaming-content :deep(h2),
.streaming-content :deep(h3),
.streaming-content :deep(h4),
.streaming-content :deep(h5),
.streaming-content :deep(h6) {
  margin: 12px 0 8px 0;
  color: #2c3e50;
}

.streaming-content :deep(h1):first-child,
.streaming-content :deep(h2):first-child,
.streaming-content :deep(h3):first-child,
.streaming-content :deep(h4):first-child,
.streaming-content :deep(h5):first-child,
.streaming-content :deep(h6):first-child {
  margin-top: 0;
}

.streaming-content :deep(p) {
  margin: 8px 0;
  line-height: 1.6;
  white-space: normal; /* 改为正常空白处理，避免过多换行 */
}

.streaming-content :deep(ul),
.streaming-content :deep(ol) {
  margin: 8px 0;
  padding-left: 20px;
}

.streaming-content :deep(li) {
  margin: 4px 0;
  line-height: 1.5;
  white-space: normal; /* 改为正常空白处理 */
}

.streaming-content :deep(li:first-child) {
  margin-top: 0;
}

.streaming-content :deep(li:last-child) {
  margin-bottom: 0;
}

.streaming-content :deep(code) {
  background-color: #f8f9fa;
  padding: 2px 4px;
  border-radius: 3px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
}

.streaming-content :deep(pre) {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 5px;
  overflow-x: auto;
  margin: 8px 0;
  border: 1px solid #e9ecef;
}

.streaming-content :deep(blockquote) {
  margin: 10px 0;
  padding: 10px 16px;
  background-color: #f8f9fa;
  border-left: 4px solid #409eff;
  color: #6c757d;
}

.streaming-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 10px 0;
  border: 1px solid #ddd;
  font-size: 13px;
}

.streaming-content :deep(th),
.streaming-content :deep(td) {
  padding: 8px 12px;
  border: 1px solid #ddd;
  text-align: left;
}

.streaming-content :deep(th) {
  background-color: #f5f7fa;
  font-weight: 600;
}

.cursor {
  display: inline-block;
  background-color: #409eff;
  color: white;
  padding: 0 2px;
  margin-left: 2px;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}

.generation-footer,
.generation-error {
  text-align: center;
}

.dialog-footer {
  text-align: right;
}

/* 渲染错误样式 */
.streaming-content :deep(.render-error) {
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  padding: 16px;
  margin: 12px 0;
}

.streaming-content :deep(.render-error p) {
  color: #ff4d4f;
  margin: 0 0 8px 0;
  font-weight: 500;
}

.streaming-content :deep(.render-error pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 8px 0 0 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #333;
}

/* Mermaid 图表样式 */
.streaming-content :deep(.mermaid-container) {
  text-align: center;
  margin: 20px 0;
  padding: 15px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
  overflow-x: auto;
  /* 重置行高，防止继承父级导致样式冲突 */
  line-height: normal;
}

.streaming-content :deep(.mermaid-container svg) {
  max-width: 100%;
  height: auto;
  display: block;
  margin: 0 auto;
}

/* Mermaid 错误处理样式 */
.streaming-content :deep(.mermaid-error-container) {
  margin: 16px 0;
  border-radius: 8px;
  overflow: hidden;
}

.streaming-content :deep(.mermaid-error) {
  background-color: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
  padding: 16px;
}

.streaming-content :deep(.mermaid-error p) {
  color: #d46b08;
  margin: 0 0 8px 0;
  font-weight: 500;
  font-size: 14px;
}

.streaming-content :deep(.mermaid-error details) {
  margin-top: 8px;
}

.streaming-content :deep(.mermaid-error summary) {
  color: #d46b08;
  cursor: pointer;
  font-size: 12px;
  margin-bottom: 8px;
}

.streaming-content :deep(.mermaid-error pre) {
  background-color: #f5f5f5;
  padding: 12px;
  border-radius: 4px;
  overflow-x: auto;
  margin: 8px 0 0 0;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 12px;
  line-height: 1.4;
  color: #333;
  max-height: 200px;
  overflow-y: auto;
}
</style>
