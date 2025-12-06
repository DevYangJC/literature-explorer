<template>
  <div class="literature-list-view">
    <!-- 页面标题和导入按钮 -->
    <div class="page-header">
        <div class="header-left">
          <h2 class="page-title">文献管理<span class="page-slogan">智能管理您的学术文献，支持PDF上传、标签分类、全文搜索，让文献整理更高效</span></h2>
        </div>
        <div class="import-buttons">
          <el-button type="success" size="large" @click="showBatchImportModal = true">
            <el-icon><Upload /></el-icon>
            批量导入
          </el-button>
          <el-button type="primary" size="large" @click="showImportModal = true">
            <el-icon><Plus /></el-icon>
            单个导入
          </el-button>
        </div>
    </div>

    <!-- 错误提示 -->
    <el-alert
      v-if="literatureStore.error"
      :title="literatureStore.error"
      type="error"
      :closable="true"
      @close="literatureStore.clearError()"
      class="mb-16"
    />

    <!-- 筛选区域 -->
    <el-card class="filter-card mb-24" shadow="never">
      <el-form :model="queryParams" inline class="filter-form">
        <el-form-item label="关键词">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索文献名称或描述"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        
        <el-form-item label="标签">
          <el-select
            v-model="queryParams.tags"
            placeholder="选择标签"
            multiple
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="tag in literatureStore.availableTags"
              :key="tag.value"
              :label="tag.label"
              :value="tag.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="文件类型">
          <el-select
            v-model="queryParams.fileType"
            placeholder="选择文件类型"
            clearable
            style="width: 150px"
          >
            <el-option
              v-for="type in literatureStore.availableFileTypes"
              :key="type.value"
              :label="type.label"
              :value="type.value"
            />
          </el-select>
        </el-form-item>
        
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 240px"
            @change="handleDateRangeChange"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 文献表格 -->
    <el-card shadow="never">
      <!-- 表格工具栏 -->
      <div class="table-toolbar" v-if="selectedRows.length > 0">
        <div class="selected-info">
          <span>已选择 {{ selectedRows.length }} 项</span>
        </div>
        <div class="toolbar-actions">
          <el-button type="danger" @click="handleBatchDelete" :loading="deleting">
            <el-icon><Delete /></el-icon>
            批量删除
          </el-button>
          <el-button @click="handleClearSelection">
            清除选择
          </el-button>
        </div>
      </div>

      <el-table
        :data="literatureStore.literatureList"
        :loading="literatureStore.loading"
        stripe
        @row-click="handleRowClick"
        @selection-change="handleSelectionChange"
        style="width: 100%"
        class="literature-table"
        :header-cell-style="{ textAlign: 'center' }"
        :cell-style="{ textAlign: 'center' }"
        ref="tableRef"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="originalName" label="文件名" min-width="200">
          <template #default="{ row }">
            <div class="file-name" :title="row.originalName">
              {{ row.originalName }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="tags" label="标签" width="200">
          <template #default="{ row }">
            <div class="tags-container">
              <el-tag
                v-for="tag in row.tags"
                :key="tag"
                size="small"
                class="tag-item"
              >
                {{ tag }}
              </el-tag>
              <span v-if="!row.tags || row.tags.length === 0" class="no-tags">
                暂无标签
              </span>
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="description" label="描述" min-width="300">
          <template #default="{ row }">
            <div class="description text-truncate" :title="row.description">
              {{ row.description || '暂无描述' }}
            </div>
          </template>
        </el-table-column>
        
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="getStatusType(row.status)"
              size="small"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column prop="fileSize" label="文件大小" width="120">
          <template #default="{ row }">
            {{ formatFileSize(row.fileSize) }}
          </template>
        </el-table-column>
        
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="downloadFile(row)"
              :loading="downloadingIds.has(row.id)"
            >
              <el-icon><Download /></el-icon>
              下载
            </el-button>
            <el-button
              type="primary"
              link
              size="small"
              @click.stop="viewDetail(row.id)"
            >
              查看详情
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click.stop="handleDelete(row)"
              :loading="deleting && currentDeletingId === row.id"
            >
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          :page-sizes="[10, 20, 50, 100]"
          :total="literatureStore.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 导入文献模态框 -->
    <ImportLiteratureModal
      v-model="showImportModal"
      @success="handleImportSuccess"
    />
    
    <!-- 批量导入文献模态框 -->
    <BatchImportModal
      v-model="showBatchImportModal"
      @success="handleBatchImportSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useLiteratureStore } from '@/stores/literatureStore'
import { ElMessage, ElMessageBox } from 'element-plus'
import ImportLiteratureModal from '@/components/ImportLiteratureModal.vue'
import BatchImportModal from '@/components/BatchImportModal.vue'
import { Plus, Search, Refresh, Download, Upload, Delete, Document, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const literatureStore = useLiteratureStore()

// 响应式数据
const showImportModal = ref(false)
const showBatchImportModal = ref(false)
const dateRange = ref([])
const downloadingIds = ref(new Set())
const exportingIds = ref(new Set())
const selectedRows = ref([])
const deleting = ref(false)
const currentDeletingId = ref(null)
const tableRef = ref(null)

// 查询参数的本地副本
const queryParams = reactive({
  page: 1,
  size: 10,
  keyword: '',
  tags: [],
  fileType: '',
  startDate: '',
  endDate: ''
})

// 监听 store 中的查询参数变化，同步到本地（仅在初始化时）
watch(() => literatureStore.queryParams, (newParams) => {
  Object.assign(queryParams, newParams)
}, { deep: true, immediate: true })

// 处理搜索
const handleSearch = () => {
  queryParams.page = 1 // 重置到第一页
  literatureStore.updateQueryParams(queryParams)
  literatureStore.fetchLiteratures()
}

// 处理重置
const handleReset = () => {
  const resetParams = {
    page: 1,
    size: 10,
    keyword: '',
    tags: [],
    fileType: '',
    startDate: '',
    endDate: ''
  }
  Object.assign(queryParams, resetParams)
  dateRange.value = []
  literatureStore.updateQueryParams(resetParams)
  literatureStore.fetchLiteratures()
}

// 处理日期范围变化
const handleDateRangeChange = (dates) => {
  if (dates && dates.length === 2) {
    queryParams.startDate = dates[0]
    queryParams.endDate = dates[1]
  } else {
    queryParams.startDate = ''
    queryParams.endDate = ''
  }
}

// 处理页码变化
const handleCurrentChange = (page) => {
  queryParams.page = page
  literatureStore.updateQueryParams({ page })
  literatureStore.fetchLiteratures()
}

// 处理每页大小变化
const handleSizeChange = (size) => {
  queryParams.size = size
  queryParams.page = 1
  literatureStore.updateQueryParams({ size, page: 1 })
  literatureStore.fetchLiteratures()
}

// 处理表格行点击
const handleRowClick = (row) => {
  viewDetail(row.id)
}

// 查看详情
const viewDetail = (id) => {
  router.push(`/literature/${id}`)
}

// 处理导入成功
const handleImportSuccess = () => {
  showImportModal.value = false
  // 刷新列表
  literatureStore.fetchLiteratures()
}

// 处理批量导入成功
const handleBatchImportSuccess = () => {
  showBatchImportModal.value = false
  // 刷新列表
  literatureStore.fetchLiteratures()
}

// 下载文件
const downloadFile = async (literature) => {
  if (!literature || downloadingIds.value.has(literature.id)) {
    return
  }

  try {
    // 添加到下载中的ID集合
    downloadingIds.value.add(literature.id)
    
    // 创建下载链接
    const downloadUrl = `/api/literature/${literature.id}/download`
    
    // 使用fetch检查文件是否存在
    const response = await fetch(downloadUrl, { method: 'HEAD' })
    
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}: ${response.statusText}`)
    }
    
    // 创建临时链接进行下载
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = literature.originalName || '文献文件'
    link.style.display = 'none'
    
    // 添加到页面并触发下载
    document.body.appendChild(link)
    link.click()
    
    // 清理
    document.body.removeChild(link)
    
    // 提示下载开始
    ElMessage.success(`${literature.originalName} 下载已开始`)
    
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
    // 从下载中的ID集合移除
    downloadingIds.value.delete(literature.id)
  }
}

// 处理导出命令
const handleExportCommand = async (command, literature) => {
  if (command === 'markdown') {
    await exportReadingGuideMarkdown(literature)
  } else if (command === 'word') {
    await exportReadingGuideWord(literature)
  }
}

// 导出阅读指南为Markdown
const exportReadingGuideMarkdown = async (literature) => {
  if (exportingIds.value.has(literature.id)) {
    return
  }

  try {
    // 添加到导出中的ID集合
    exportingIds.value.add(literature.id)

    // 创建下载链接
    const exportUrl = `/api/literature/${literature.id}/export-reading-guide`

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
    link.download = literature.originalName.replace(/\.[^/.]+$/, '') + '_阅读指南.md'
    link.style.display = 'none'

    // 添加到页面并触发下载
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)

    // 提示导出开始
    ElMessage.success(`${literature.originalName} Markdown 阅读指南导出已开始`)

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
    // 从导出中的ID集合移除
    exportingIds.value.delete(literature.id)
  }
}

// 导出阅读指南为Word
const exportReadingGuideWord = async (literature) => {
  if (exportingIds.value.has(literature.id)) {
    return
  }

  try {
    // 添加到导出中的ID集合
    exportingIds.value.add(literature.id)

    // 创建下载链接
    const exportUrl = `/api/literature/${literature.id}/export-reading-guide-word`

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
    link.download = literature.originalName.replace(/\.[^/.]+$/, '') + '_阅读指南.docx'
    link.style.display = 'none'

    // 添加到页面并触发下载
    document.body.appendChild(link)
    link.click()

    // 清理
    document.body.removeChild(link)

    // 提示导出开始
    ElMessage.success(`${literature.originalName} Word 阅读指南导出已开始`)

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
    // 从导出中的ID集合移除
    exportingIds.value.delete(literature.id)
  }
}

// 格式化文件大小
const formatFileSize = (bytes) => {
  if (!bytes || bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

// 格式化日期时间
const formatDateTime = (dateTime) => {
  if (!dateTime) return '-'
  return new Date(dateTime).toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

// 获取状态类型
const getStatusType = (status) => {
  const statusMap = {
    0: 'warning',
    1: 'success',
    2: 'danger'
  }
  return statusMap[status] || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  const statusMap = {
    0: '处理中',
    1: '已完成',
    2: '失败'
  }
  return statusMap[status] || '未知'
}

// 处理选择变化
const handleSelectionChange = (selection) => {
  selectedRows.value = selection
}

// 清除选择
const handleClearSelection = () => {
  if (tableRef.value) {
    tableRef.value.clearSelection()
  }
  selectedRows.value = []
}

// 处理单个删除
const handleDelete = async (literature) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除文献"${literature.originalName}"吗？删除后可以恢复。`,
      '确认删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    currentDeletingId.value = literature.id
    deleting.value = true

    await literatureStore.deleteLiterature(literature.id)

    ElMessage.success('文献删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  } finally {
    deleting.value = false
    currentDeletingId.value = null
  }
}

// 处理批量删除
const handleBatchDelete = async () => {
  if (selectedRows.value.length === 0) {
    ElMessage.warning('请选择要删除的文献')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确定要删除选中的 ${selectedRows.value.length} 个文献吗？删除后可以恢复。`,
      '确认批量删除',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    const ids = selectedRows.value.map(row => row.id)
    deleting.value = true

    await literatureStore.batchDeleteLiterature(ids)

    // 清除选择
    handleClearSelection()
    ElMessage.success('批量删除成功')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '批量删除失败')
    }
  } finally {
    deleting.value = false
  }
}

// 组件挂载时获取数据
onMounted(() => {
  literatureStore.fetchLiteratures()
})
</script>

<style scoped>
.literature-list-view {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  gap: 24px;
}

.header-left {
  flex: 1;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #2c3e50;
  display: flex;
  align-items: baseline;
  gap: 16px;
}

.page-slogan {
  font-size: 14px;
  color: #606266;
  font-weight: 400;
  white-space: nowrap;
}

.import-buttons {
  display: flex;
  gap: 12px;
}

.filter-card {
  border: 1px solid #ebeef5;
}

.filter-form {
  margin: 0;
}

.filter-form .el-form-item {
  margin-bottom: 0;
}

.table-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  margin-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.selected-info {
  color: #606266;
  font-weight: 500;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.literature-table {
  margin-bottom: 16px;
}

.literature-table :deep(.el-table__row) {
  cursor: pointer;
}

.literature-table :deep(.el-table__row:hover) {
  background-color: #f5f7fa;
}

.file-name {
  font-weight: 500;
  color: #409eff;
}

.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.tag-item {
  margin: 0;
}

.no-tags {
  color: #909399;
  font-size: 12px;
}

.description {
  max-width: 300px;
  line-height: 1.4;
}

.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}

.mb-16 {
  margin-bottom: 16px;
}

.mb-24 {
  margin-bottom: 24px;
}

.text-truncate {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
