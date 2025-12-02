<template>
  <el-dialog
    v-model="dialogVisibleComputed"
    title="批量导入文献"
    width="900px"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="!isProcessing"
    @close="handleClose"
  >
    <!-- 上传表单 -->
    <div v-if="!isProcessing" class="upload-form">
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="120px"
      >
        <el-form-item label="选择文件" prop="files" required>
          <el-upload
            ref="uploadRef"
            :file-list="fileList"
            :auto-upload="false"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
            :before-upload="beforeUpload"
            :accept="acceptedFileTypes"
            :limit="16"
            multiple
            drag
            class="upload-area"
          >
            <div class="upload-content">
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="upload-text">
                <p>将文件拖拽到此处，或<em>点击上传</em></p>
                <p class="upload-tip">
                  支持 PDF、Word、Markdown、Excel 格式，最多16个文件，单个文件不超过 50MB
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

    <!-- 批量处理进度区域 -->
    <div v-else class="processing-area">
      <div class="processing-header">
        <el-icon v-if="!processingComplete" class="loading-icon"><Loading /></el-icon>
        <el-icon v-else class="success-icon"><CircleCheck /></el-icon>
        <div class="processing-info">
          <h3>{{ processingComplete ? '批量处理完成！' : '正在批量处理文献...' }}</h3>
          <p class="progress-summary">
            已完成 {{ completedCount }} / {{ totalCount }} 个文件
          </p>
        </div>
      </div>
      
      <!-- 整体进度条 -->
      <div class="overall-progress">
        <el-progress 
          :percentage="overallProgress" 
          :status="processingComplete ? 'success' : undefined"
          :stroke-width="8"
        />
      </div>
      
      <!-- 文件处理列表 -->
      <div class="file-list">
        <div
          v-for="(fileStatus, index) in fileStatusList"
          :key="index"
          class="file-item"
          :class="{ 'processing': fileStatus.status === 'processing', 'completed': fileStatus.status === 'completed', 'error': fileStatus.status === 'error' }"
        >
          <div class="file-info">
            <el-icon v-if="fileStatus.status === 'pending'" class="file-icon pending"><Clock /></el-icon>
            <el-icon v-else-if="fileStatus.status === 'processing'" class="file-icon processing"><Loading /></el-icon>
            <el-icon v-else-if="fileStatus.status === 'completed'" class="file-icon completed"><Check /></el-icon>
            <el-icon v-else-if="fileStatus.status === 'error'" class="file-icon error"><Close /></el-icon>
            
            <div class="file-details">
              <div class="file-name">{{ fileStatus.filename }}</div>
              <div class="file-message">{{ fileStatus.message }}</div>
            </div>
            
            <!-- 查看详情按钮 -->
            <div v-if="fileStatus.status === 'completed' && fileStatus.literatureId" class="file-actions">
              <el-button
                type="primary"
                size="small"
                link
                @click="viewDetail(fileStatus.literatureId)"
              >
                查看详情
              </el-button>
            </div>
          </div>
        </div>
      </div>
      
      <div v-if="processingComplete" class="processing-footer">
        <el-result
          :icon="hasErrors ? 'warning' : 'success'"
          :title="hasErrors ? '处理完成（部分失败）' : '处理完成！'"
          :sub-title="`成功处理 ${successCount} 个文件${hasErrors ? '，' + errorCount + ' 个文件失败' : ''}`"
        >
          <template #extra>
            <el-button type="primary" @click="handleClose">
              关闭
            </el-button>
          </template>
        </el-result>
      </div>
    </div>

    <!-- 底部按钮 -->
    <template #footer v-if="!isProcessing">
      <div class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button
          type="primary"
          :disabled="!canSubmit"
          :loading="isProcessing"
          @click="handleSubmit"
        >
          开始批量导入
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled, Loading, CircleCheck, Clock, Check, Close } from '@element-plus/icons-vue'
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
const formRef = ref()
const uploadRef = ref()

// 表单数据
const formData = ref({
  files: [],
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

// 处理状态
const isProcessing = ref(false)
const processingComplete = ref(false)
const totalCount = ref(0)
const completedCount = ref(0)
const fileStatusList = ref([])

// SSE 连接控制
let abortController = null

// 计算属性
const dialogVisibleComputed = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const canSubmit = computed(() => {
  return formData.value.files.length > 0 && formData.value.apiKey.trim()
})

const overallProgress = computed(() => {
  if (totalCount.value === 0) return 0
  return Math.round((completedCount.value / totalCount.value) * 100)
})

const successCount = computed(() => {
  return fileStatusList.value.filter(f => f.status === 'completed').length
})

const errorCount = computed(() => {
  return fileStatusList.value.filter(f => f.status === 'error').length
})

const hasErrors = computed(() => {
  return errorCount.value > 0
})

const acceptedFileTypes = '.pdf,.doc,.docx,.md,.markdown,.xls,.xlsx'

// 表单验证规则
const formRules = {
  files: [
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

// 文件选择处理
const handleFileChange = (file, fileListParam) => {
  formData.value.files = fileListParam.map(f => f.raw).filter(Boolean)
}

// 文件移除处理
const handleFileRemove = (file, fileListParam) => {
  formData.value.files = fileListParam.map(f => f.raw).filter(Boolean)
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
    
    await startBatchImport()
  } catch (error) {
    ElMessage.error('请检查表单信息')
  }
}

// 启动批量导入
const startBatchImport = async () => {
  try {
    // 重置所有状态
    resetProcessingState()
    
    // 初始化文件状态列表
    totalCount.value = formData.value.files.length
    fileStatusList.value = formData.value.files.map((file, index) => ({
      index,
      filename: file.name,
      status: 'pending',
      message: '等待处理...',
      literatureId: null
    }))
    
    // 创建 FormData
    const formDataToSend = new FormData()
    formData.value.files.forEach(file => {
      formDataToSend.append('files', file)
    })
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
    
    fetchEventSource('/api/literature/batch-import', {
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
          console.error('处理SSE消息失败:', error)
        }
      },
      
      onerror(error) {
        console.error('SSE连接错误:', error)
        if (!abortController?.signal.aborted) {
          reject(error)
        }
      },
      
      onclose() {
        console.log('SSE连接关闭')
      }
    })
  })
}

// 处理 SSE 事件
const handleSSEEvent = (event) => {
  const messageData = parseSSEEventData(event)
  if (!messageData) return
  
  handleSSEMessage(messageData)
}

// 解析 SSE 事件数据
const parseSSEEventData = (event) => {
  try {
    switch (event.event) {
      case 'batch_start':
      case 'file_start':
      case 'file_saved':
      case 'file_complete':
      case 'file_error':
      case 'batch_complete':
        return {
          type: event.event,
          data: JSON.parse(event.data || '{}'),
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
        return null
    }
  } catch (error) {
    console.error('解析SSE事件数据失败:', error)
    return null
  }
}

// 处理 SSE 消息
const handleSSEMessage = (messageData) => {
  const { type, data } = messageData
  
  switch (type) {
    case 'batch_start':
      handleBatchStart(data)
      break
    case 'file_start':
      handleFileStart(data)
      break
    case 'file_saved':
      handleFileSaved(data)
      break
    case 'file_complete':
      handleFileComplete(data)
      break
    case 'file_error':
      handleFileError(data)
      break
    case 'batch_complete':
      handleBatchComplete(data)
      break
    case 'content':
      // 批量导入时忽略content事件，因为我们只关心整体进度
      break
    default:
      console.warn('未知的消息类型:', type, messageData)
  }
}

// 处理批量开始
const handleBatchStart = (data) => {
  isProcessing.value = true
  totalCount.value = data.total
  console.log('批量处理开始:', data.message)
}

// 处理文件开始
const handleFileStart = (data) => {
  const fileStatus = fileStatusList.value[data.index]
  if (fileStatus) {
    fileStatus.status = 'processing'
    fileStatus.message = '正在处理...'
  }
}

// 处理文件保存完成
const handleFileSaved = (data) => {
  const fileStatus = fileStatusList.value[data.index]
  if (fileStatus) {
    fileStatus.message = '正在生成阅读指南...'
  }
}

// 处理文件完成
const handleFileComplete = (data) => {
  const fileStatus = fileStatusList.value[data.index]
  if (fileStatus) {
    fileStatus.status = 'completed'
    fileStatus.message = '处理完成'
    fileStatus.literatureId = data.literatureId
  }
  completedCount.value = data.completed
}

// 处理文件错误
const handleFileError = (data) => {
  const fileStatus = fileStatusList.value[data.index]
  if (fileStatus) {
    fileStatus.status = 'error'
    fileStatus.message = `处理失败: ${data.error}`
  }
  completedCount.value = data.completed
}

// 处理批量完成
const handleBatchComplete = (data) => {
  processingComplete.value = true
  console.log('批量处理完成:', data.message)
  ElMessage.success('批量导入完成！')
}

// SSE 错误处理
const handleSSEError = (error) => {
  console.error('SSE连接错误:', error)
  
  if (error.name !== 'AbortError') {
    const errorMessage = error.message || '连接失败，请重试'
    isProcessing.value = false
    ElMessage.error(errorMessage)
  }
}

// 重置处理状态
const resetProcessingState = () => {
  isProcessing.value = true
  processingComplete.value = false
  totalCount.value = 0
  completedCount.value = 0
  fileStatusList.value = []
}

// 关闭 SSE 连接
const closeSSEConnection = () => {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
}

// 重置表单
const resetForm = () => {
  // 关闭 SSE 连接
  closeSSEConnection()
  
  // 重置所有状态
  isProcessing.value = false
  processingComplete.value = false
  totalCount.value = 0
  completedCount.value = 0
  fileStatusList.value = []
  
  // 重置表单数据（保留API Key）
  const savedApiKey = formData.value.apiKey
  formData.value = {
    files: [],
    apiKey: savedApiKey
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

// 查看详情
const viewDetail = (literatureId) => {
  if (!literatureId) return
  
  // 在新窗口中打开详情页
  const detailUrl = `/literature/${literatureId}`
  window.open(detailUrl, '_blank')
}

// 处理关闭
const handleClose = () => {
  if (processingComplete.value) {
    emit('success')
  }
  emit('update:modelValue', false)
}

// 组件挂载时初始化
onMounted(() => {
  loadSavedApiKey()
})
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

.processing-area {
  padding: 20px 0;
}

.processing-header {
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

.processing-info {
  text-align: center;
}

.processing-header h3 {
  margin: 0 0 8px 0;
  color: #409eff;
  font-weight: 500;
}

.progress-summary {
  margin: 0;
  color: #606266;
  font-size: 14px;
}

.overall-progress {
  margin-bottom: 24px;
}

.file-list {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 6px;
  padding: 16px;
}

.file-item {
  display: flex;
  flex-direction: column;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: all 0.3s ease;
}

.file-item:last-child {
  margin-bottom: 0;
}

.file-item.pending {
  background-color: #f8f9fa;
}

.file-item.processing {
  background-color: #e6f7ff;
  border: 1px solid #91d5ff;
}

.file-item.completed {
  background-color: #f6ffed;
  border: 1px solid #b7eb8f;
}

.file-item.error {
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
}

.file-info {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.file-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.file-icon.pending {
  color: #909399;
}

.file-icon.processing {
  color: #409eff;
  animation: rotate 2s linear infinite;
}

.file-icon.completed {
  color: #67c23a;
}

.file-icon.error {
  color: #f56c6c;
}

.file-details {
  flex: 1;
}

.file-name {
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
}

.file-message {
  font-size: 12px;
  color: #606266;
}

.file-actions {
  flex-shrink: 0;
  margin-left: auto;
}

.processing-footer {
  text-align: center;
  margin-top: 24px;
}

.dialog-footer {
  text-align: right;
}
</style>
