<template>
  <div class="literature-detail-view">
    <!-- 加载状态 -->
    <div v-if="literatureStore.detailLoading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>

    <!-- 错误状态 -->
    <el-alert
      v-else-if="literatureStore.error"
      :title="literatureStore.error"
      type="error"
      :closable="true"
      @close="literatureStore.clearError()"
      class="mb-24"
    />

    <!-- 详情内容 -->
    <div v-else-if="currentLiterature" class="detail-content">
      <!-- 导航栏 -->
      <div class="detail-header">
        <el-button @click="goBack" class="back-button">
          <el-icon><ArrowLeft /></el-icon>
          返回列表
        </el-button>
        <h2 class="detail-title">文献详情</h2>
      </div>

      <!-- 基本信息卡片 -->
      <el-card class="info-card mb-24" shadow="never">
        <template #header>
          <div class="card-header">
            <h3>基本信息</h3>
            <div class="button-group">
              <el-dropdown
                v-if="currentLiterature.status === 1"
                trigger="click"
                @command="handleExportCommand"
                :disabled="exportingMarkdown || exportingWord"
              >
                <el-button
                  type="success"
                  size="small"
                  :loading="exportingMarkdown || exportingWord"
                >
                  <el-icon><Document /></el-icon>
                  导出指南
                  <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item command="markdown">
                      <el-icon><Document /></el-icon>
                      Markdown 格式
                    </el-dropdown-item>
                    <el-dropdown-item command="word">
                      <el-icon><Document /></el-icon>
                      Word 文档
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button
                type="primary"
                size="small"
                @click="downloadFile"
                :loading="downloading"
              >
                <el-icon><Download /></el-icon>
                下载文件
              </el-button>
            </div>
          </div>
        </template>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="info-item">
              <label class="info-label">文件名：</label>
              <span class="info-value" :title="currentLiterature.originalName">
                {{ currentLiterature.originalName }}
              </span>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label class="info-label">文件大小：</label>
              <span class="info-value">{{ formatFileSize(currentLiterature.fileSize) }}</span>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="12">
            <div class="info-item">
              <label class="info-label">文件类型：</label>
              <el-tag size="small" type="info">{{ currentLiterature.fileType?.toUpperCase() }}</el-tag>
            </div>
          </el-col>
          <el-col :span="12">
            <div class="info-item">
              <label class="info-label">状态：</label>
              <el-tag
                size="small"
                :type="getStatusType(currentLiterature.status)"
              >
                {{ getStatusText(currentLiterature.status) }}
              </el-tag>
            </div>
          </el-col>
        </el-row>
        
        <el-row :gutter="24">
          <el-col :span="24">
            <div class="info-item">
              <label class="info-label">创建时间：</label>
              <span class="info-value">{{ formatDateTime(currentLiterature.createTime) }}</span>
            </div>
          </el-col>
        </el-row>
      </el-card>

      <!-- 标签卡片 -->
      <el-card class="tags-card mb-24" shadow="never">
        <template #header>
          <div class="card-header">
            <h3>标签</h3>
          </div>
        </template>
        
        <div class="tags-content">
          <el-tag
            v-for="tag in currentLiterature.tags"
            :key="tag"
            class="tag-item"
            size="large"
          >
            {{ tag }}
          </el-tag>
          <span v-if="!currentLiterature.tags || currentLiterature.tags.length === 0" class="no-tags">
            暂无标签
          </span>
        </div>
      </el-card>

      <!-- 描述卡片 -->
      <el-card class="description-card mb-24" shadow="never">
        <template #header>
          <div class="card-header">
            <h3>描述</h3>
          </div>
        </template>
        
        <div class="description-content">
          <p v-if="currentLiterature.description" class="description-text">
            {{ currentLiterature.description }}
          </p>
          <p v-else class="no-description">暂无描述</p>
        </div>
      </el-card>

      <!-- 阅读指南卡片 -->
      <el-card class="guide-card mb-24" shadow="never">
        <template #header>
          <div class="card-header">
            <h3>AI 阅读指南</h3>
            <el-button
              v-if="currentLiterature.readingGuideSummary"
              size="small"
              @click="toggleFullscreen"
            >
              <el-icon><FullScreen /></el-icon>
              {{ isFullscreen ? '退出全屏' : '全屏阅读' }}
            </el-button>
          </div>
        </template>
        
        <div
          ref="guideContentRef"
          class="guide-content"
          :class="{ 'fullscreen': isFullscreen }"
        >
          <!-- 全屏模式下的退出按钮 -->
          <div v-if="isFullscreen" class="fullscreen-header">
            <h3 class="fullscreen-title">AI 阅读指南</h3>
            <el-button
              type="default"
              size="small"
              @click="toggleFullscreen"
              class="exit-fullscreen-btn"
            >
              <el-icon><Close /></el-icon>
              退出全屏
            </el-button>
          </div>
                      <div
              v-if="currentLiterature.readingGuideSummary"
              class="markdown-content"
              v-html="renderedGuide"
            ></div>
          <div v-else class="no-guide">
            <el-empty description="暂无阅读指南" />
          </div>
        </div>
      </el-card>

      <!-- 智能问答卡片 -->
      <el-card class="qa-card" shadow="never">
        <template #header>
          <div class="card-header">
            <h3>💡 智能问答</h3>
            <div class="header-actions">
              <el-button
                size="small"
                @click="showHistoryDialog"
              >
                <el-icon><Clock /></el-icon>
                查看历史
              </el-button>
              <el-tag v-if="currentLiterature.status !== 1" type="warning" size="small">
                需要文献完成AI分析后使用
              </el-tag>
            </div>
          </div>
        </template>
        
        <div class="qa-content">
          <!-- 问答输入区 -->
          <div v-if="!isAnswering" class="qa-input-area">
            <el-form :model="qaForm" label-width="100px">
              <el-form-item label="提问">
                <el-input
                  v-model="qaForm.question"
                  type="textarea"
                  :rows="3"
                  placeholder="例如：这篇文献的核心创新点是什么？"
                  maxlength="500"
                  show-word-limit
                  :disabled="currentLiterature.status !== 1"
                />
              </el-form-item>
              
              <el-form-item label="检索模式">
                <el-radio-group v-model="qaForm.crossDoc" :disabled="currentLiterature.status !== 1">
                  <el-radio :label="false">单文献（仅当前文献）</el-radio>
                  <el-radio :label="true">跨文献（检索相关文献）</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item v-if="qaForm.crossDoc" label="检索数量">
                <el-slider
                  v-model="qaForm.topK"
                  :min="1"
                  :max="10"
                  :marks="{ 1: '1', 3: '3', 5: '5', 10: '10' }"
                  show-stops
                />
              </el-form-item>
              
              <el-form-item label="API Key">
                <el-input
                  v-model="qaForm.apiKey"
                  type="password"
                  placeholder="请输入 Kimi API Key"
                  show-password
                  clearable
                  :disabled="currentLiterature.status !== 1"
                >
                  <template #append>
                    <el-button
                      v-if="qaForm.apiKey"
                      @click="clearSavedApiKey"
                      text
                      type="danger"
                      size="small"
                    >
                      清除
                    </el-button>
                  </template>
                </el-input>
                <div class="api-key-tip">
                  <span class="tip-text">API Key 会自动保存到浏览器本地</span>
                </div>
              </el-form-item>
              
              <el-form-item>
                <el-button
                  type="primary"
                  @click="handleAsk"
                  :disabled="!canAsk || currentLiterature.status !== 1"
                  size="large"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  开始提问
                </el-button>
              </el-form-item>
            </el-form>
          </div>
          
          <!-- 回答展示区 -->
          <div v-else class="qa-answer-area">
            <div class="answer-header">
              <div class="answer-title">
                <el-icon v-if="!answerComplete" class="loading-icon"><Loading /></el-icon>
                <el-icon v-else class="success-icon"><CircleCheck /></el-icon>
                <span>{{ answerComplete ? '回答完成' : '正在生成回答...' }}</span>
              </div>
              <div class="header-actions">
                <el-button
                  v-if="answerComplete"
                  size="small"
                  @click="continueAsk"
                >
                  <el-icon><ChatDotRound /></el-icon>
                  继续提问
                </el-button>
                <el-button
                  v-if="answerComplete && qaHistory.length > 0"
                  size="small"
                  @click="resetQA"
                >
                  <el-icon><RefreshLeft /></el-icon>
                  清空对话
                </el-button>
              </div>
            </div>
            
            <div v-if="qaProgressMessage" class="progress-message">
              <el-icon><InfoFilled /></el-icon>
              {{ qaProgressMessage }}
            </div>
            
            <!-- 对话历史记录 -->
            <div v-if="qaHistory.length > 0" class="qa-history">
              <div v-for="(item, index) in qaHistory" :key="index" class="qa-item">
                <div class="question-box">
                  <strong>问题 {{ index + 1 }}：</strong>{{ item.question }}
                </div>
                <div class="answer-box">
                  <div class="markdown-content" v-html="item.renderedAnswer"></div>
                </div>
              </div>
            </div>
            
            <!-- 当前问答（仅在流式输出时或回答未完成时显示） -->
            <div v-if="currentQuestion && (isStreaming || !answerComplete)" class="answer-content">
              <div class="question-box">
                <strong>{{ qaHistory.length > 0 ? `问题 ${qaHistory.length + 1}` : '问题' }}：</strong>{{ currentQuestion }}
              </div>
              
              <div class="answer-box">
                <div
                  ref="answerContentRef"
                  class="markdown-content"
                  v-html="renderedAnswer"
                ></div>
                <span v-if="isStreaming" class="typing-cursor">|</span>
              </div>
            </div>
            
            <div v-if="qaError" class="error-message">
              <el-alert :title="qaError" type="error" :closable="false" />
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 未找到文献 -->
    <el-empty v-else description="未找到文献" />

    <!-- 历史对话弹窗 -->
    <el-dialog
      v-model="historyDialogVisible"
      title="历史对话记录"
      width="80%"
      :close-on-click-modal="false"
    >
      <div class="history-dialog-content">
        <el-tabs v-model="activeHistoryTab" type="border-card">
          <!-- 对话列表 -->
          <el-tab-pane label="对话列表" name="list">
            <div v-if="loadingHistory" class="loading-container">
              <el-skeleton :rows="5" animated />
            </div>
            <div v-else-if="historySessions.length === 0" class="empty-history">
              <el-empty description="暂无对话记录" />
            </div>
            <div v-else class="sessions-list">
              <div
                v-for="session in historySessions"
                :key="session.sessionId"
                class="session-item"
                @click="loadSession(session.sessionId)"
              >
                <div class="session-header">
                  <div class="session-info">
                    <h4 class="session-title">{{ session.sessionTitle }}</h4>
                    <el-tag v-if="session.crossDoc" size="small" type="info">跨文献</el-tag>
                    <el-tag v-else size="small" type="success">单文献</el-tag>
                  </div>
                  <el-button
                    type="danger"
                    size="small"
                    @click.stop="deleteSession(session.sessionId)"
                  >
                    <el-icon><Delete /></el-icon>
                    删除
                  </el-button>
                </div>
                <div class="session-meta">
                  <span v-if="session.literatureName" class="meta-item">
                    <el-icon><Document /></el-icon>
                    {{ session.literatureName }}
                  </span>
                  <span class="meta-item">
                    <el-icon><ChatDotRound /></el-icon>
                    {{ session.qaCount }} 轮对话
                  </span>
                  <span class="meta-item">
                    <el-icon><Clock /></el-icon>
                    {{ formatDateTime(session.updateTime) }}
                  </span>
                </div>
              </div>
            </div>
          </el-tab-pane>

          <!-- 对话详情 -->
          <el-tab-pane label="对话详情" name="detail" :disabled="!selectedSession">
            <div v-if="selectedSession" class="session-detail">
              <div class="detail-header">
                <h3>{{ selectedSession.sessionTitle }}</h3>
                <el-button size="small" @click="loadSessionToQA">
                  <el-icon><ChatDotRound /></el-icon>
                  继续对话
                </el-button>
              </div>
              <div class="detail-records">
                <div
                  v-for="record in selectedSession.records"
                  :key="record.id"
                  class="record-item"
                >
                  <div class="record-question">
                    <strong>问题 {{ record.sequence }}：</strong>{{ record.question }}
                  </div>
                  <div class="record-answer">
                    <div class="markdown-content" v-html="renderMarkdown(record.answer)"></div>
                  </div>
                </div>
              </div>
            </div>
            <el-empty v-else description="请从列表中选择一个对话" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watchEffect, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useLiteratureStore } from '@/stores/literatureStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import { marked } from 'marked'
import mermaid from 'mermaid'
import { ArrowLeft, FullScreen, Close, Download, Document, ArrowDown, ChatDotRound, Loading, CircleCheck, RefreshLeft, InfoFilled, Clock, Delete } from '@element-plus/icons-vue'
import { fetchEventSource } from '@microsoft/fetch-event-source'

const route = useRoute()
const router = useRouter()
const literatureStore = useLiteratureStore()

// 响应式数据
const guideContentRef = ref()
const answerContentRef = ref()
const isFullscreen = ref(false)
const downloading = ref(false)
const exportingMarkdown = ref(false)
const exportingWord = ref(false)

// 问答相关状态
const qaForm = ref({
  question: '',
  apiKey: '',
  crossDoc: false,
  topK: 5
})
const isAnswering = ref(false)
const isStreaming = ref(false)
const answerComplete = ref(false)
const currentQuestion = ref('')
const streamingAnswer = ref('')
const renderedAnswer = ref('')
const qaProgressMessage = ref('')
const qaError = ref('')
const qaHistory = ref([]) // 对话历史
const currentSessionId = ref('') // 当前会话ID
let abortController = null

// 历史对话相关状态
const historyDialogVisible = ref(false)
const activeHistoryTab = ref('list')
const loadingHistory = ref(false)
const historySessions = ref([])
const selectedSession = ref(null)

// API Key 存储相关
const API_KEY_STORAGE_KEY = 'literature_assistant_api_key'

// 计算属性
const currentLiterature = computed(() => literatureStore.currentLiterature)

const canAsk = computed(() => {
  return qaForm.value.question.trim() && qaForm.value.apiKey.trim()
})

const renderedGuide = computed(() => {
  if (!currentLiterature.value?.readingGuideSummary) return ''
  
  try {
    // 配置 marked
    marked.setOptions({
      breaks: true,
      gfm: true
    })
    
    return marked(currentLiterature.value.readingGuideSummary)
  } catch (error) {
    console.error('Markdown 渲染错误:', error)
    return currentLiterature.value.readingGuideSummary
  }
})

// 方法
const goBack = () => {
  router.push('/')
}

const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const getStatusType = (status) => {
  const statusMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return statusMap[status] || 'info'
}

const getStatusText = (status) => {
  const statusMap = {
    0: '处理中',
    1: '已完成',
    2: '失败'
  }
  return statusMap[status] || '未知'
}

const toggleFullscreen = () => {
  isFullscreen.value = !isFullscreen.value
  
  if (isFullscreen.value) {
    document.body.style.overflow = 'hidden'
    // 添加ESC键监听
    document.addEventListener('keydown', handleEscapeKey)
  } else {
    document.body.style.overflow = ''
    // 移除ESC键监听
    document.removeEventListener('keydown', handleEscapeKey)
  }
}

// ESC键退出全屏
const handleEscapeKey = (event) => {
  if (event.key === 'Escape' && isFullscreen.value) {
    toggleFullscreen()
  }
}

// 下载文件
const downloadFile = async () => {
  if (!currentLiterature.value || downloading.value) {
    return
  }

  try {
    downloading.value = true
    
    // 创建下载链接
    const downloadUrl = `/api/literature/${currentLiterature.value.id}/download`
    
    // 使用fetch检查文件是否存在
    const response = await fetch(downloadUrl, { method: 'HEAD' })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    
    // 创建临时链接进行下载
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = currentLiterature.value.originalName || '文献文件'
    link.style.display = 'none'
    
    // 添加到页面并触发下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    
    // 提示下载开始
    ElMessage.success('文件下载已开始')
    
  } catch (error) {
    console.error('下载文件失败:', error)
    
    // 根据错误类型显示不同的提示
    let errorMessage = '下载文件失败，请重试'
    if (error.message.includes('404')) {
      errorMessage = '文件不存在或已被删除'
    } else if (error.message.includes('403')) {
      errorMessage = '没有权限下载该文件'
    } else if (error.message.includes('500')) {
      errorMessage = '服务器错误，请稍后重试'
    }
    
    ElMessage.error(errorMessage)
  } finally {
    downloading.value = false
  }
}

// 导出阅读指南为Markdown
// 处理导出命令
const handleExportCommand = async (command) => {
  if (command === 'markdown') {
    await exportReadingGuideMarkdown()
  } else if (command === 'word') {
    await exportReadingGuideWord()
  }
}

// 导出阅读指南为Markdown
const exportReadingGuideMarkdown = async () => {
  if (!currentLiterature.value || exportingMarkdown.value) {
    return
  }

  try {
    exportingMarkdown.value = true

    // 创建下载链接
    const exportUrl = `/api/literature/${currentLiterature.value.id}/export-reading-guide`

    // 使用fetch检查文件是否存在
    const response = await fetch(exportUrl, {
      method: 'HEAD',
      headers: {
        'Accept': 'text/markdown'
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    // 创建临时链接进行下载
    const link = document.createElement('a')
    link.href = exportUrl
    link.download = currentLiterature.value.originalName.replace(/\.[^/.]+$/, '') + '_阅读指南.md'
    link.style.display = 'none'

    // 添加到页面并触发下载
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)

    // 提示导出开始
    ElMessage.success('Markdown 阅读指南导出已开始')

  } catch (error) {
    console.error('导出Markdown失败:', error)

    // 根据错误类型显示不同的提示
    let errorMessage = '导出阅读指南失败，请重试'
    if (error.message.includes('404')) {
      errorMessage = '阅读指南不存在或文献尚未处理完成'
    } else if (error.message.includes('403')) {
      errorMessage = '没有权限导出该阅读指南'
    } else if (error.message.includes('400')) {
      errorMessage = '文献暂无阅读指南，请等待处理完成'
    } else if (error.message.includes('500')) {
      errorMessage = '服务器错误，请稍后重试'
    }

    ElMessage.error(errorMessage)
  } finally {
    exportingMarkdown.value = false
  }
}

// 导出阅读指南为Word
const exportReadingGuideWord = async () => {
  if (!currentLiterature.value || exportingWord.value) {
    return
  }

  try {
    exportingWord.value = true

    // 创建下载链接
    const exportUrl = `/api/literature/${currentLiterature.value.id}/export-reading-guide-word`

    // 使用fetch检查文件是否存在
    const response = await fetch(exportUrl, {
      method: 'HEAD',
      headers: {
        'Accept': 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      }
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }

    // 创建临时链接进行下载
    const link = document.createElement('a')
    link.href = exportUrl
    link.download = currentLiterature.value.originalName.replace(/\.[^/.]+$/, '') + '_阅读指南.docx'
    link.style.display = 'none'

    // 添加到页面并触发下载
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)

    // 提示导出开始
    ElMessage.success('Word 阅读指南导出已开始')

  } catch (error) {
    console.error('导出Word失败:', error)

    // 根据错误类型显示不同的提示
    let errorMessage = '导出Word阅读指南失败，请重试'
    if (error.message.includes('404')) {
      errorMessage = '阅读指南不存在或文献尚未处理完成'
    } else if (error.message.includes('403')) {
      errorMessage = '没有权限导出该阅读指南'
    } else if (error.message.includes('400')) {
      errorMessage = '文献暂无阅读指南，请等待处理完成'
    } else if (error.message.includes('500')) {
      errorMessage = '服务器错误，请稍后重试'
    }

    ElMessage.error(errorMessage)
  } finally {
    exportingWord.value = false
  }
}

// ==================== 问答功能 ====================

// 加载保存的 API Key
const loadSavedApiKey = () => {
  try {
    const savedKey = localStorage.getItem(API_KEY_STORAGE_KEY)
    if (savedKey) {
      qaForm.value.apiKey = savedKey
    }
  } catch (error) {
    console.warn('读取保存的 API Key 失败:', error)
  }
}

// 保存 API Key
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
    qaForm.value.apiKey = ''
    ElMessage.success('已清除保存的 API Key')
  } catch (error) {
    console.warn('清除保存的 API Key 失败:', error)
    ElMessage.error('清除失败，请重试')
  }
}

// 处理提问
const handleAsk = async () => {
  if (!canAsk.value) {
    ElMessage.warning('请输入问题和 API Key')
    return
  }

  if (currentLiterature.value.status !== 1) {
    ElMessage.warning('请等待文献完成AI分析')
    return
  }

  // 保存 API Key
  saveApiKey(qaForm.value.apiKey)

  // 重置状态
  isAnswering.value = true
  isStreaming.value = true
  answerComplete.value = false
  currentQuestion.value = qaForm.value.question
  streamingAnswer.value = ''
  renderedAnswer.value = ''
  qaProgressMessage.value = ''
  qaError.value = ''

  // 连接 SSE
  await connectQASSE()
}

// 连接问答 SSE
const connectQASSE = async () => {
  try {
    abortController = new AbortController()

    const requestBody = {
      question: qaForm.value.question,
      apiKey: qaForm.value.apiKey,
      literatureId: qaForm.value.crossDoc ? null : currentLiterature.value.id,
      crossDoc: qaForm.value.crossDoc,
      topK: qaForm.value.crossDoc ? qaForm.value.topK : 5
    }

    await fetchEventSource('/api/literature/ask', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'text/event-stream',
        'Cache-Control': 'no-cache'
      },
      body: JSON.stringify(requestBody),
      signal: abortController.signal,
      openWhenHidden: true,

      async onopen(response) {
        if (response.ok && response.headers.get('content-type')?.includes('text/event-stream')) {
          console.log('QA SSE 连接已建立')
        } else {
          const errorText = await response.text()
          throw new Error(`HTTP ${response.status}: ${response.statusText}${errorText ? ` - ${errorText}` : ''}`)
        }
      },

      onmessage(event) {
        try {
          handleQASSEEvent(event)
        } catch (error) {
          console.error('处理 QA SSE 事件失败:', error)
        }
      },

      onerror(error) {
        console.error('QA SSE 错误:', error)
        qaError.value = '连接失败，请重试'
        isStreaming.value = false
        throw error
      },

      onclose() {
        console.log('QA SSE 连接关闭')
      }
    })

  } catch (error) {
    console.error('问答失败:', error)
    if (!abortController?.signal.aborted) {
      qaError.value = error.message || '问答失败，请重试'
      isStreaming.value = false
    }
  }
}

// 处理 QA SSE 事件
const handleQASSEEvent = (event) => {
  const { event: eventType, data } = event

  switch (eventType) {
    case 'start':
      qaProgressMessage.value = data || '开始处理问题...'
      break

    case 'progress':
      qaProgressMessage.value = data || '正在处理...'
      break

    case 'content':
      const processedContent = data
        .replace(/<empty-line>/g, '\n')
        .replace(/<empty-space>/g, ' ')
      streamingAnswer.value += processedContent
      renderAnswerMarkdown()
      scrollAnswerToBottom()
      break

    case 'complete':
      isStreaming.value = false
      answerComplete.value = true
      qaProgressMessage.value = ''
      
      // 保存当前问答到历史
      qaHistory.value.push({
        question: currentQuestion.value,
        answer: streamingAnswer.value,
        renderedAnswer: renderedAnswer.value
      })
      
      // 保存到后端数据库
      saveQAToBackend()
      
      ElMessage.success('回答完成')
      break

    case 'error':
      qaError.value = data || '处理失败'
      isStreaming.value = false
      break

    default:
      console.warn('未知的 SSE 事件类型:', eventType)
  }
}

// 渲染答案 Markdown
const renderAnswerMarkdown = () => {
  if (!streamingAnswer.value) {
    renderedAnswer.value = ''
    return
  }

  try {
    marked.setOptions({
      breaks: true,
      gfm: true
    })
    renderedAnswer.value = marked(streamingAnswer.value)
  } catch (error) {
    console.error('Markdown 渲染错误:', error)
    renderedAnswer.value = streamingAnswer.value
  }
}

// 滚动答案到底部
const scrollAnswerToBottom = async () => {
  await nextTick()
  if (answerContentRef.value) {
    answerContentRef.value.scrollTo({
      top: answerContentRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
}

// 重置问答状态
const resetQA = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }

  isAnswering.value = false
  isStreaming.value = false
  answerComplete.value = false
  currentQuestion.value = ''
  streamingAnswer.value = ''
  renderedAnswer.value = ''
  qaProgressMessage.value = ''
  qaError.value = ''
  qaHistory.value = [] // 清空历史
  qaForm.value.question = '' // 清空问题输入框
}

// 继续提问
const continueAsk = () => {
  // 仅重置当前问答状态，保留历史
  isAnswering.value = false
  isStreaming.value = false
  answerComplete.value = false
  currentQuestion.value = ''
  streamingAnswer.value = ''
  renderedAnswer.value = ''
  qaProgressMessage.value = ''
  qaError.value = ''
  qaForm.value.question = '' // 清空问题输入框
}

// ==================== 历史对话功能 ====================

// 保存问答到后端
const saveQAToBackend = async () => {
  try {
    const response = await fetch('/api/qa-history/save', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        sessionId: currentSessionId.value || undefined,
        question: currentQuestion.value,
        answer: streamingAnswer.value,
        literatureId: qaForm.value.crossDoc ? null : currentLiterature.value.id,
        crossDoc: qaForm.value.crossDoc,
        keyword: qaForm.value.crossDoc ? qaForm.value.question : null
      })
    })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    const result = await response.json()
    if (result.code === 200 && result.data) {
      // 保存会话ID，用于继续对话
      currentSessionId.value = result.data
      console.log('问答记录已保存，会话ID:', currentSessionId.value)
    }
  } catch (error) {
    console.error('保存问答记录失败:', error)
    // 不弹出错误，静默处理
  }
}

// 显示历史对话弹窗
const showHistoryDialog = async () => {
  historyDialogVisible.value = true
  activeHistoryTab.value = 'list'
  selectedSession.value = null
  await loadHistorySessions()
}

// 加载历史会话列表
const loadHistorySessions = async () => {
  try {
    loadingHistory.value = true
    const response = await fetch(`/api/qa-history/sessions?literatureId=${currentLiterature.value?.id || ''}`)
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    const result = await response.json()
    if (result.code === 200) {
      historySessions.value = result.data || []
    }
  } catch (error) {
    console.error('加载历史会话失败:', error)
    ElMessage.error('加载历史记录失败')
    historySessions.value = []
  } finally {
    loadingHistory.value = false
  }
}

// 加载会话详情
const loadSession = async (sessionId) => {
  try {
    const response = await fetch(`/api/qa-history/session/${sessionId}`)
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    const result = await response.json()
    if (result.code === 200 && result.data) {
      selectedSession.value = result.data
      activeHistoryTab.value = 'detail'
    }
  } catch (error) {
    console.error('加载会话详情失败:', error)
    ElMessage.error('加载对话详情失败')
  }
}

// 删除会话
const deleteSession = async (sessionId) => {
  try {
    await ElMessageBox.confirm('确定要删除这个对话吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    const response = await fetch(`/api/qa-history/session/${sessionId}`, {
      method: 'DELETE'
    })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    
    ElMessage.success('删除成功')
    await loadHistorySessions()
    
    if (selectedSession.value?.sessionId === sessionId) {
      selectedSession.value = null
      activeHistoryTab.value = 'list'
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除会话失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 加载会话到当前问答区
 const loadSessionToQA = () => {
  if (!selectedSession.value) return
  
  // 关闭弹窗
  historyDialogVisible.value = false
  
  // 设置会话ID
  currentSessionId.value = selectedSession.value.sessionId
  
  // 载入历史记录
  qaHistory.value = selectedSession.value.records.map(record => ({
    question: record.question,
    answer: record.answer,
    renderedAnswer: renderMarkdown(record.answer)
  }))
  
  // 设置为继续问答模式
  isAnswering.value = false
  answerComplete.value = false
  
  ElMessage.success('已加载历史对话，可以继续提问')
}

// 渲染Markdown
const renderMarkdown = (text) => {
  if (!text) return ''
  try {
    marked.setOptions({
      breaks: true,
      gfm: true
    })
    return marked(text)
  } catch (error) {
    console.error('Markdown 渲染错误:', error)
    return text
  }
}

// 监听历史记录变化，渲染历史中的 Mermaid 图表
watch(() => qaHistory.value.length, async () => {
  if (qaHistory.value.length > 0) {
    await nextTick()
    // 渲染所有历史记录中的 Mermaid
    const historyItems = document.querySelectorAll('.qa-history .qa-item .answer-box')
    for (const item of historyItems) {
      await renderMermaidInAnswer(item)
    }
  }
})

// 监听答案变化，渲染 Mermaid 图表
watch(renderedAnswer, async () => {
  if (renderedAnswer.value && answerContentRef.value) {
    await nextTick()
    await renderMermaidInAnswer(answerContentRef.value)
  }
})

// 在答案区域渲染 Mermaid 图表
const renderMermaidInAnswer = async (container) => {
  if (!container) return
  
  // 查找并渲染 mermaid 图表
  const mermaidBlocks = container.querySelectorAll('pre code.language-mermaid:not([data-processed]), code.language-mermaid:not([data-processed])')
  for (let i = 0; i < mermaidBlocks.length; i++) {
    const block = mermaidBlocks[i]
    const code = block.textContent || block.innerText

    try {
      if (!isValidMermaidSyntax(code)) continue
      
      // 标记为已处理
      block.setAttribute('data-processed', 'true')

      const mermaidContainer = document.createElement('div')
      mermaidContainer.className = 'mermaid-container'
      mermaidContainer.id = `qa-mermaid-${Date.now()}-${i}`

      const { svg } = await mermaid.render(mermaidContainer.id, code)
      mermaidContainer.innerHTML = svg

      const parent = block.parentElement
      if (parent && parent.tagName === 'PRE' && parent.parentNode) {
        parent.parentNode.replaceChild(mermaidContainer, parent)
      } else if (block.parentNode) {
        block.parentNode.replaceChild(mermaidContainer, block)
      }
    } catch (error) {
      console.error('Mermaid 渲染错误:', error)
    }
  }
}

// HTML 转义函数
const escapeHtml = (text) => {
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;')
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

// 渲染 Mermaid 图表
const renderMermaidCharts = async () => {
  await nextTick()
  
  if (!guideContentRef.value) return
  
  // 首先清理任何现有的 Mermaid 错误信息
  cleanupMermaidErrors()
  
  // 查找所有 mermaid 代码块
  const mermaidBlocks = guideContentRef.value.querySelectorAll('pre code.language-mermaid, code.language-mermaid')
  
  for (let i = 0; i < mermaidBlocks.length; i++) {
    const block = mermaidBlocks[i]
    const code = block.textContent || block.innerText
    
    try {
      // 预验证 Mermaid 语法
      if (!isValidMermaidSyntax(code)) {
        throw new Error('Invalid Mermaid syntax')
      }
      
      // 创建容器
      const container = document.createElement('div')
      container.className = 'mermaid-container'
      container.id = `mermaid-${Date.now()}-${i}`
      
      // 渲染图表
      const { svg } = await mermaid.render(container.id, code)
      container.innerHTML = svg
      
      // 替换原来的代码块
      const parent = block.parentElement
      if (parent && parent.tagName === 'PRE' && parent.parentNode) {
        parent.parentNode.replaceChild(container, parent)
      } else if (block.parentNode) {
        block.parentNode.replaceChild(container, block)
      }
    } catch (error) {
      console.error('Mermaid 渲染错误:', error)
      
      // 渲染失败时显示友好的错误提示
      const errorContainer = document.createElement('div')
      errorContainer.className = 'mermaid-error-container'
      errorContainer.innerHTML = `
        <div class="mermaid-error">
          <p>图表渲染失败</p>
          <details>
            <summary>查看详情</summary>
            <pre><code>${escapeHtml(code)}</code></pre>
          </details>
        </div>
      `
      
      // 替换原来的代码块
      const parent = block.parentElement
      if (parent && parent.tagName === 'PRE' && parent.parentNode) {
        parent.parentNode.replaceChild(errorContainer, parent)
      } else if (block.parentNode) {
        block.parentNode.replaceChild(errorContainer, block)
      }
    }
  }
  
  // 渲染完成后清理可能出现的错误信息
  setTimeout(() => {
    cleanupMermaidErrors()
  }, 100)
  
  setTimeout(() => {
    cleanupMermaidErrors()
  }, 500)
}

// 清理 Mermaid 错误信息
const cleanupMermaidErrors = () => {
  if (!guideContentRef.value) return
  
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
      const elements = guideContentRef.value.querySelectorAll(selector)
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
    const allElements = guideContentRef.value.querySelectorAll('*')
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
          if (!element.closest('.markdown-content') && 
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

// 监听渲染指南变化，重新渲染 Mermaid 图表
const handleGuideRendered = async () => {
  if (renderedGuide.value) {
    await nextTick()
    await renderMermaidCharts()
    // 渲染后再次清理错误
    setTimeout(() => {
      cleanupMermaidErrors()
    }, 200)
  }
}

// 生命周期
onMounted(async () => {
  // 初始化 Mermaid
  mermaid.initialize({
    startOnLoad: false,
    theme: 'default',
    securityLevel: 'loose',
    fontFamily: 'Arial, sans-serif',
    suppressErrorRendering: true, // 抑制错误渲染
    logLevel: 'fatal', // 只记录致命错误
    // 自定义错误处理
    errorCallback: (error) => {
      console.error('Mermaid 详情页错误:', error)
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
  
  // 加载保存的 API Key
  loadSavedApiKey()
  
  // 获取文献详情
  const literatureId = route.params.id
  if (literatureId) {
    await literatureStore.fetchLiteratureDetail(literatureId)
    await handleGuideRendered()
  }
})

onUnmounted(() => {
  // 清理全屏状态
  if (isFullscreen.value) {
    document.body.style.overflow = ''
    // 清理ESC键监听
    document.removeEventListener('keydown', handleEscapeKey)
  }
  
  // 最后一次清理 Mermaid 错误
  cleanupMermaidErrors()
  cleanupGlobalMermaidElements()
  
  // 清理当前文献数据
  literatureStore.clearCurrentLiterature()
})

// 监听渲染指南变化

const stopWatching = watchEffect(() => {
  if (renderedGuide.value) {
    handleGuideRendered()
  }
})

onUnmounted(() => {
  stopWatching()
})
</script>

<style scoped>
.literature-detail-view {
  max-width: 1200px;
  margin: 0 auto;
}

.loading-container {
  padding: 24px;
}

.detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.back-button {
  margin-right: 16px;
}

.detail-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
}

.info-card,
.tags-card,
.description-card,
.guide-card {
  border: 1px solid #ebeef5;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.button-group {
  display: flex;
  gap: 8px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #2c3e50;
}

.info-item {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  min-width: 80px;
  font-weight: 500;
  color: #606266;
  margin-right: 8px;
}

.info-value {
  color: #303133;
  word-break: break-all;
}

.tags-content {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-item {
  margin: 0;
}

.no-tags {
  color: #909399;
  font-size: 14px;
}

.description-content {
  line-height: 1.6;
}

.description-text {
  margin: 0;
  color: #303133;
  white-space: pre-wrap;
}

.no-description {
  margin: 0;
  color: #909399;
  font-style: italic;
}

.guide-content {
  position: relative;
  transition: all 0.3s ease;
}

.guide-content.fullscreen {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 9999;
  background: white;
  padding: 0;
  overflow-y: auto;
}

.fullscreen-header {
  position: sticky;
  top: 0;
  background: white;
  border-bottom: 1px solid #ebeef5;
  padding: 16px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 10;
}

.fullscreen-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #2c3e50;
}

.exit-fullscreen-btn {
  position: relative;
}

.guide-content.fullscreen .markdown-content {
  padding: 24px;
}

.markdown-content {
  line-height: 1.8;
  color: #333;
  font-size: 14px;
}

.markdown-content :deep(h1) {
  font-size: 28px;
  margin: 24px 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 2px solid #eee;
  color: #2c3e50;
}

.markdown-content :deep(h2) {
  font-size: 24px;
  margin: 20px 0 12px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #eee;
  color: #2c3e50;
}

.markdown-content :deep(h3) {
  font-size: 20px;
  margin: 16px 0 8px 0;
  color: #2c3e50;
}

.markdown-content :deep(h4) {
  font-size: 18px;
  margin: 14px 0 6px 0;
  color: #2c3e50;
}

.markdown-content :deep(h5),
.markdown-content :deep(h6) {
  font-size: 16px;
  margin: 12px 0 4px 0;
  color: #2c3e50;
}

.markdown-content :deep(p) {
  margin: 12px 0;
  line-height: 1.8;
}

.markdown-content :deep(ul),
.markdown-content :deep(ol) {
  margin: 12px 0;
  padding-left: 24px;
}

.markdown-content :deep(li) {
  margin: 6px 0;
  line-height: 1.6;
}

.markdown-content :deep(blockquote) {
  margin: 16px 0;
  padding: 12px 20px;
  background-color: #f8f9fa;
  border-left: 4px solid #409eff;
  color: #6c757d;
}

.markdown-content :deep(code) {
  background-color: #f8f9fa;
  padding: 3px 6px;
  border-radius: 4px;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  font-size: 0.9em;
  color: #e83e8c;
}

.markdown-content :deep(pre) {
  background-color: #f8f9fa;
  padding: 16px;
  border-radius: 6px;
  overflow-x: auto;
  margin: 16px 0;
  border: 1px solid #e9ecef;
}

.markdown-content :deep(pre code) {
  background: none;
  padding: 0;
  color: #333;
}

.markdown-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 16px 0;
  border: 1px solid #ddd;
}

.markdown-content :deep(th),
.markdown-content :deep(td) {
  padding: 8px 12px;
  border: 1px solid #ddd;
  text-align: left;
}

.markdown-content :deep(th) {
  background-color: #f5f7fa;
  font-weight: 600;
}

.no-guide {
  text-align: center;
  padding: 40px 0;
}

.mb-24 {
  margin-bottom: 24px;
}

/* Mermaid 图表样式 */
.markdown-content :deep(.mermaid-container) {
  text-align: center;
  margin: 24px 0;
  padding: 20px;
  background: #fafafa;
  border: 1px solid #eee;
  border-radius: 8px;
   /* 新增：重置行高，防止继承父级导致样式冲突 */
  line-height: normal;
}

.markdown-content :deep(.mermaid-container svg) {
  max-width: 100%;
  height: auto;
}

/* Mermaid 错误处理样式 */
.markdown-content :deep(.mermaid-error-container) {
  margin: 16px 0;
  border-radius: 8px;
  overflow: hidden;
}

.markdown-content :deep(.mermaid-error) {
  background-color: #fff7e6;
  border: 1px solid #ffd591;
  border-radius: 6px;
  padding: 16px;
}

.markdown-content :deep(.mermaid-error p) {
  color: #d46b08;
  margin: 0 0 8px 0;
  font-weight: 500;
  font-size: 14px;
}

.markdown-content :deep(.mermaid-error details) {
  margin-top: 8px;
}

.markdown-content :deep(.mermaid-error summary) {
  color: #d46b08;
  cursor: pointer;
  font-size: 12px;
  margin-bottom: 8px;
}

.markdown-content :deep(.mermaid-error pre) {
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

/* ==================== 问答面板样式 ==================== */

.qa-card {
  border: 1px solid #ebeef5;
}

.qa-content {
  min-height: 200px;
}

.qa-input-area {
  padding: 8px 0;
}

.api-key-tip {
  margin-top: 4px;
}

.tip-text {
  font-size: 12px;
  color: #909399;
}

.qa-answer-area {
  padding: 8px 0;
}

.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.answer-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #2c3e50;
}

.loading-icon {
  color: #409eff;
  animation: rotating 2s linear infinite;
}

.success-icon {
  color: #67c23a;
}

@keyframes rotating {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.progress-message {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  background: #ecf5ff;
  border: 1px solid #d9ecff;
  border-radius: 4px;
  margin-bottom: 16px;
  color: #409eff;
  font-size: 14px;
}

.answer-content {
  margin-top: 16px;
}

.question-box {
  padding: 12px 16px;
  background: #f5f7fa;
  border-left: 4px solid #409eff;
  border-radius: 4px;
  margin-bottom: 16px;
  color: #303133;
  font-size: 14px;
}

.question-box strong {
  color: #409eff;
  margin-right: 8px;
}

.answer-box {
  position: relative;
  padding: 16px;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  min-height: 100px;
  max-height: 600px;
  overflow-y: auto;
}

.typing-cursor {
  display: inline-block;
  margin-left: 2px;
  animation: blink 1s infinite;
  font-weight: bold;
  color: #409eff;
}

@keyframes blink {
  0%, 50% {
    opacity: 1;
  }
  51%, 100% {
    opacity: 0;
  }
}

.error-message {
  margin-top: 16px;
}

/* 对话历史样式 */
.qa-history {
  margin-bottom: 24px;
}

.qa-item {
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px dashed #e4e7ed;
}

.qa-item:last-child {
  border-bottom: none;
}

.qa-item .question-box {
  background: #f0f2f5;
  border-left-color: #909399;
}

.qa-item .answer-box {
  background: #fafafa;
  border: 1px solid #f0f0f0;
  max-height: 400px;
  overflow-y: auto;
}

/* 历史对话弹窗样式 */
.history-dialog-content {
  min-height: 500px;
}

.sessions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.session-item {
  padding: 16px;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
}

.session-item:hover {
  border-color: #409eff;
  background: #f5f7fa;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.session-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.session-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.session-title {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
}

.session-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #909399;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.session-detail .detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding-bottom: 12px;
  border-bottom: 2px solid #e4e7ed;
}

.detail-records {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.record-item {
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.record-question {
  margin-bottom: 12px;
  padding: 12px;
  background: #e7f4ff;
  border-left: 3px solid #409eff;
  border-radius: 4px;
  font-size: 14px;
}

.record-answer {
  padding: 12px;
  background: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
}
</style>
