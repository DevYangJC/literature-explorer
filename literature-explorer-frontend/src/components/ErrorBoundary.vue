<template>
  <div v-if="hasError" class="error-boundary">
    <div class="error-content">
      <div class="error-icon">
        <el-icon :size="48" color="#f56c6c">
          <Warning />
        </el-icon>
      </div>
      <h3>页面加载出现问题</h3>
      <p>{{ errorMessage }}</p>
      <div class="error-actions">
        <el-button type="primary" @click="retry">重试</el-button>
        <el-button @click="goHome">返回首页</el-button>
      </div>
      <details v-if="showDetails" class="error-details">
        <summary>查看详细信息</summary>
        <pre>{{ errorDetails }}</pre>
      </details>
    </div>
  </div>
  <slot v-else />
</template>

<script setup>
import { ref, onErrorCaptured } from 'vue'
import { useRouter } from 'vue-router'
import { Warning } from '@element-plus/icons-vue'

const router = useRouter()

const hasError = ref(false)
const errorMessage = ref('')
const errorDetails = ref('')
const showDetails = ref(false)

// 捕获组件错误
onErrorCaptured((error, instance, info) => {
  console.error('ErrorBoundary 捕获到错误:', error)
  
  hasError.value = true
  errorMessage.value = '组件渲染时发生错误，请尝试刷新页面'
  errorDetails.value = `错误信息: ${error.message}\n组件信息: ${info}\n错误堆栈: ${error.stack}`
  
  // 在开发环境显示详细信息
  if (process.env.NODE_ENV === 'development') {
    showDetails.value = true
  }
  
  // 防止错误继续传播
  return false
})

// 重试
const retry = () => {
  hasError.value = false
  errorMessage.value = ''
  errorDetails.value = ''
  // 刷新当前路由
  router.go(0)
}

// 返回首页
const goHome = () => {
  hasError.value = false
  router.push('/')
}
</script>

<style scoped>
.error-boundary {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
  padding: 24px;
}

.error-content {
  text-align: center;
  max-width: 500px;
}

.error-icon {
  margin-bottom: 16px;
}

.error-content h3 {
  margin: 16px 0 8px 0;
  color: #303133;
  font-size: 18px;
}

.error-content p {
  color: #606266;
  margin-bottom: 24px;
  line-height: 1.6;
}

.error-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.error-details {
  text-align: left;
  margin-top: 16px;
  padding: 16px;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
}

.error-details summary {
  cursor: pointer;
  color: #606266;
  font-size: 14px;
  margin-bottom: 8px;
}

.error-details pre {
  font-size: 12px;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
  margin: 0;
  line-height: 1.4;
}
</style>
