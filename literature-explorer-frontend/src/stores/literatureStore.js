import { defineStore } from 'pinia'
import axios from 'axios'

export const useLiteratureStore = defineStore('literature', {
  state: () => ({
    // 文献列表相关状态
    literatureList: [],
    total: 0,
    loading: false,
    error: null,
    
    // 查询参数
    queryParams: {
      page: 1,
      size: 10,
      keyword: '',
      tags: [],
      fileType: '',
      startDate: '',
      endDate: ''
    },
    
    // 文献详情
    currentLiterature: null,
    detailLoading: false,
    
    // 可用的标签和文件类型选项（用于筛选）
    availableTags: [],
    availableFileTypes: [
      { label: 'PDF (.pdf)', value: 'pdf' },
      { label: 'Word 文档 (.doc)', value: 'doc' },
      { label: 'Word 文档 (.docx)', value: 'docx' },
      { label: 'Markdown (.md)', value: 'md' },
      { label: 'Excel 表格 (.xls)', value: 'xls' },
      { label: 'Excel 表格 (.xlsx)', value: 'xlsx' }
    ]
  }),
  
  getters: {
    // 获取当前页码
    currentPage: (state) => state.queryParams.page,
    
    // 获取每页大小
    pageSize: (state) => state.queryParams.size,
    
    // 是否有筛选条件
    hasFilters: (state) => {
      const { keyword, tags, fileType, startDate, endDate } = state.queryParams
      return keyword || tags.length > 0 || fileType || startDate || endDate
    }
  },
  
  actions: {
    // 获取文献列表
    async fetchLiteratures() {
      this.loading = true
      this.error = null
      
      try {
        const response = await axios.post('/api/literature/page', {
          pageNum: this.queryParams.page,
          pageSize: this.queryParams.size,
          keyword: this.queryParams.keyword || undefined,
          tags: this.queryParams.tags.length > 0 ? this.queryParams.tags : undefined,
          fileType: this.queryParams.fileType || undefined,
          startDate: this.queryParams.startDate || undefined,
          endDate: this.queryParams.endDate || undefined
        })
        
        if (response.data.success) {
          this.literatureList = response.data.data.records || []
          this.total = response.data.data.total || 0
          
          // 检查当前页码是否超出范围
          const maxPage = Math.ceil(this.total / this.queryParams.size) || 1
          if (this.queryParams.page > maxPage && maxPage > 0) {
            this.queryParams.page = maxPage
          }
          
          // 收集所有标签用于筛选选项
          this.updateAvailableTags()
        } else {
          throw new Error(response.data.message || '获取文献列表失败')
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message || '网络请求失败'
        console.error('获取文献列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    
    // 获取文献详情
    async fetchLiteratureDetail(id) {
      this.detailLoading = true
      this.error = null
      this.currentLiterature = null
      
      try {
        const response = await axios.get(`/api/literature/${id}`)
        
        if (response.data.success) {
          this.currentLiterature = response.data.data
        } else {
          throw new Error(response.data.message || '获取文献详情失败')
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message || '网络请求失败'
        console.error('获取文献详情失败:', error)
      } finally {
        this.detailLoading = false
      }
    },
    
    // 更新查询参数
    updateQueryParams(params) {
      this.queryParams = { ...this.queryParams, ...params }
    },
    
    // 重置查询参数
    resetQueryParams() {
      this.queryParams = {
        page: 1,
        size: 10,
        keyword: '',
        tags: [],
        fileType: '',
        startDate: '',
        endDate: ''
      }
    },
    
    // 设置页码
    setPage(page) {
      if (page > 0) {
        this.queryParams.page = page
      }
    },
    
    // 设置每页大小
    setPageSize(size) {
      if (size > 0) {
        this.queryParams.size = size
        this.queryParams.page = 1 // 重置到第一页
      }
    },
    
    // 更新可用标签列表
    updateAvailableTags() {
      const tagSet = new Set()
      this.literatureList.forEach(literature => {
        if (literature.tags && Array.isArray(literature.tags)) {
          literature.tags.forEach(tag => tagSet.add(tag))
        }
      })
      this.availableTags = Array.from(tagSet).map(tag => ({ label: tag, value: tag }))
    },
    
    // 清除错误状态
    clearError() {
      this.error = null
    },
    
    // 清除当前文献详情
    clearCurrentLiterature() {
      this.currentLiterature = null
      this.detailLoading = false
    },

    // 删除文献
    async deleteLiterature(id) {
      this.error = null

      try {
        const response = await axios.delete(`/api/literature/${id}`)

        if (response.data.success) {
          // 重新获取文献列表
          await this.fetchLiteratures()
          return true
        } else {
          throw new Error(response.data.message || '删除文献失败')
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message || '删除文献失败'
        console.error('删除文献失败:', error)
        throw error
      }
    },

    // 批量删除文献
    async batchDeleteLiterature(ids) {
      this.error = null

      if (!ids || ids.length === 0) {
        throw new Error('请选择要删除的文献')
      }

      try {
        const response = await axios.delete('/api/literature/batch', { data: ids })

        if (response.data.success) {
          // 重新获取文献列表
          await this.fetchLiteratures()
          return true
        } else {
          throw new Error(response.data.message || '批量删除文献失败')
        }
      } catch (error) {
        this.error = error.response?.data?.message || error.message || '批量删除文献失败'
        console.error('批量删除文献失败:', error)
        throw error
      }
    }
  }
})
