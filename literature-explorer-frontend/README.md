# 文献助手 - 前端应用

一个基于 Vue 3 的现代化文献管理前端应用，支持文献上传、AI 生成阅读指南、文献检索和管理。

## ✨ 功能特性

- 📚 **文献管理**: 支持 PDF、Word、Markdown 格式文献的上传和管理
- 🤖 **AI 阅读指南**: 集成 Kimi AI，自动生成智能阅读指南
- 🔍 **智能检索**: 支持关键词、标签、文件类型、时间范围等多维度筛选
- 📊 **图表渲染**: 支持 Mermaid 图表在阅读指南中的渲染
- 💻 **响应式设计**: 适配桌面和平板设备
- ⚡ **实时流式**: 支持 SSE 实时显示 AI 生成过程

## 🛠 技术栈

- **框架**: Vue 3 + Composition API
- **构建工具**: Vite
- **UI 框架**: Element Plus
- **状态管理**: Pinia
- **路由**: Vue Router
- **HTTP 客户端**: Axios
- **Markdown 渲染**: marked
- **图表渲染**: mermaid
- **实时通信**: EventSource (SSE)

## 📦 安装和运行

### 环境要求

- Node.js >= 16.0.0
- npm >= 8.0.0

### 安装依赖

```bash
npm install
```

### 开发模式

```bash
npm run dev
```

应用将运行在 `http://localhost:3000`

### 生产构建

```bash
npm run build
```

### 预览构建结果

```bash
npm run preview
```

## 🚀 项目结构

```
src/
├── components/           # 公共组件
│   └── ImportLiteratureModal.vue  # 导入文献模态框
├── views/               # 页面视图
│   ├── LiteratureListView.vue     # 文献列表页
│   └── LiteratureDetailView.vue   # 文献详情页
├── stores/              # Pinia 状态管理
│   └── literatureStore.js         # 文献相关状态
├── router/              # 路由配置
│   └── index.js
├── App.vue              # 根组件
├── main.js              # 应用入口
└── style.css            # 全局样式
```

## 🔧 配置说明

### API 代理配置

项目通过 Vite 代理将 `/api` 请求转发到后端服务：

```javascript
// vite.config.js
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // 后端服务地址
      changeOrigin: true,
    },
  },
}
```

### 后端 API 接口

项目依赖以下后端 API 接口：

- `POST /api/literature/page` - 分页查询文献列表
- `GET /api/literature/{id}` - 获取文献详情
- `POST /api/literature/generate-guide` - 生成阅读指南 (SSE)

## 🎨 界面设计

### 主要页面

1. **文献列表页** (`/`)
   - 文献表格展示
   - 多维度筛选功能
   - 分页导航
   - 导入文献按钮

2. **文献详情页** (`/literature/:id`)
   - 文献基本信息
   - 标签展示
   - AI 阅读指南 (支持 Markdown 和 Mermaid 图表)
   - 全屏阅读模式

3. **导入文献模态框**
   - 文件拖拽上传
   - API Key 输入
   - 实时生成进度显示

### 设计特色

- 🎯 **现代简洁**: 采用 Element Plus 设计语言
- 🌈 **色彩搭配**: 蓝色渐变主题，专业学术风格
- 📱 **响应式**: 适配不同屏幕尺寸
- ⚡ **交互流畅**: 丰富的动画和过渡效果

## 🔄 状态管理

使用 Pinia 进行全局状态管理：

```javascript
// 主要状态
{
  literatureList: [],      // 文献列表
  total: 0,               // 文献总数
  loading: false,         // 加载状态
  queryParams: {},        // 查询参数
  currentLiterature: null // 当前文献详情
}
```

## 🚨 注意事项

1. **API Key 安全**: Kimi API Key 仅在客户端临时使用，不会被存储
2. **文件大小限制**: 单个文件不超过 50MB
3. **文件格式**: 仅支持 PDF、Word (.doc/.docx)、Markdown (.md/.markdown) 格式
4. **浏览器兼容性**: 需要支持 EventSource 的现代浏览器

## 📝 开发说明

### 组件开发

- 使用 Vue 3 Composition API 和 `<script setup>` 语法
- 遵循 Element Plus 组件规范
- 保持组件的单一职责原则

### 样式规范

- 使用 scoped 样式避免污染
- 遵循 BEM 命名规范
- 响应式设计优先

### API 调用

- 统一使用 Axios 进行 HTTP 请求
- 在 Pinia Store 中处理 API 逻辑
- 妥善处理加载状态和错误状态

## 🤝 贡献指南

1. Fork 本仓库
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 打开 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情。

## 🙏 致谢

- [Vue.js](https://vuejs.org/) - 渐进式 JavaScript 框架
- [Element Plus](https://element-plus.org/) - 基于 Vue 3 的组件库
- [Vite](https://vitejs.dev/) - 下一代前端构建工具
- [Pinia](https://pinia.vuejs.org/) - Vue 状态管理库
