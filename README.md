# Literature Explorer 📚

一个基于AI的智能文献管理平台，帮助研究人员高效管理、分析和理解学术文献。

## ✨ 主要功能

### 🚀 智能文献处理
- **AI阅读指南生成**：基于Kimi AI自动生成文献阅读指南，包含核心内容、研究方法、主要结论等
- **多格式支持**：支持PDF、Word、Markdown、Excel等多种文献格式
- **智能分类标签**：AI自动分析文献内容，生成学术分类标签（如人工智能、机器学习、深度学习等）
- **实时处理进度**：通过SSE流式响应实时展示AI处理进度

### 📁 文献管理
- **批量导入**：支持批量上传多个文献文件，自动并行处理
- **文献列表管理**：分页展示文献列表，支持关键词搜索和标签筛选
- **文献详情查看**：查看完整的文献信息、AI生成的阅读指南和分类标签
- **文件下载**：支持下载原始文献文件和导出Markdown格式的阅读指南
- **删除功能**：支持单个删除和批量删除文献记录

### 🎨 用户界面
- **现代化设计**：基于Vue 3 + Element Plus的响应式界面
- **实时状态显示**：动态展示文献处理状态（处理中、已完成、失败）
- **交互式操作**：友好的用户交互体验，支持拖拽上传等

## 🏗️ 技术架构

### 后端技术栈
- **Java 21** + **Spring Boot 3.5.5**
- **MyBatis-Plus**：数据库操作和ORM
- **H2数据库**：嵌入式数据库，支持文件持久化
- **Apache POI**：Office文档处理
- **Apache PDFBox**：PDF文档处理
- **CommonMark**：Markdown解析
- **OkHttp**：HTTP客户端和SSE支持
- **Knife4j**：API文档生成
- **Hutool**：Java工具类库

### 前端技术栈
- **Vue 3** + **Composition API**
- **Element Plus**：UI组件库
- **Vue Router 4**：前端路由
- **Pinia**：状态管理
- **Axios**：HTTP客户端
- **Vite**：构建工具
- **Markdown渲染**：支持阅读指南的Markdown展示
- **Mermaid图表**：支持流程图和思维导图展示

## 🚀 快速开始

### 环境要求
- **Java 21+**
- **Node.js 16+**
- **Maven 3.6+**

### 安装和启动

#### 方式一：使用启动脚本（推荐）

**Windows用户：**
```bash
cd literature-explorer-backend
start.bat
```

**macOS/Linux用户：**
```bash
cd literature-explorer-backend
chmod +x start.sh
./start.sh
```

启动脚本会自动：
1. 启动Spring Boot后端服务（端口：8086）
2. 启动Vue前端开发服务器（端口：5173）
3. 在新窗口中显示服务日志

#### 方式二：手动启动

**1. 启动后端服务：**
```bash
cd literature-explorer-backend

# 构建项目
mvn clean package

# 启动服务
java -jar jar/literature-explorer-0.0.1-SNAPSHOT.jar
```

**2. 启动前端服务：**
```bash
cd literature-explorer-frontend

# 安装依赖（首次运行）
npm install

# 启动开发服务器
npm run dev
```

### 访问应用
- **前端应用**：http://localhost:5173
- **后端API**：http://localhost:8086/api
- **API文档**：http://localhost:8086/api/doc.html

## 📖 使用指南

### 1. 获取Kimi AI API Key
- 访问 [Moonshot AI](https://platform.moonshot.cn/) 注册账号
- 创建API Key并复制

### 2. 导入文献
- **单个导入**：点击"单个导入"按钮，上传文献文件并输入API Key
- **批量导入**：点击"批量导入"按钮，选择多个文献文件并输入API Key

### 3. 查看和处理结果
- 在文献列表中查看处理进度和结果
- 点击文献标题查看详细信息
- 导出AI生成的阅读指南（Markdown格式）
- 下载原始文献文件

## 📋 API接口

### 主要接口
- `POST /api/literature/generate-guide` - 生成文献阅读指南（SSE流式响应）
- `POST /api/literature/batch-import` - 批量导入文献（SSE流式响应）
- `POST /api/literature/page` - 分页查询文献列表
- `GET /api/literature/{id}` - 获取文献详情
- `GET /api/literature/{id}/download` - 下载文献文件
- `GET /api/literature/{id}/export-reading-guide` - 导出阅读指南
- `DELETE /api/literature/{id}` - 删除文献
- `DELETE /api/literature/batch` - 批量删除文献

完整的API文档请访问：http://localhost:8086/api/doc.html

## 🔧 配置说明

### 应用配置（application.yml）
```yaml
# 服务端口
server:
  port: 8086
  servlet:
    context-path: /api

# 文件存储
literature:
  file:
    upload-path: ./uploads/documents
    max-file-size: 10MB
    allowed-extensions: pdf,doc,docx,md,markdown,xls,xlsx

# AI配置
literature:
  ai:
    base-url: https://api.moonshot.cn/v1
    model: kimi-k2-turbo-preview
    max-tokens: 20480
    temperature: 0.7
```

## 📁 项目结构

```
literature-explorer/
├── literature-explorer-backend/     # Spring Boot后端
│   ├── src/main/java/
│   │   └── com/yuyuan/literature/
│   │       ├── controller/          # 控制器层
│   │       ├── service/             # 服务层
│   │       ├── entity/              # 实体类
│   │       ├── dto/                 # 数据传输对象
│   │       ├── mapper/              # MyBatis映射器
│   │       └── config/              # 配置类
│   ├── src/main/resources/
│   │   ├── mapper/                  # MyBatis XML映射文件
│   │   ├── prompts/                 # AI提示词模板
│   │   └── application.yml          # 应用配置
│   └── pom.xml                      # Maven配置
├── literature-explorer-frontend/    # Vue前端
│   ├── src/
│   │   ├── views/                   # 页面组件
│   │   ├── components/              # 通用组件
│   │   ├── stores/                  # Pinia状态管理
│   │   ├── router/                  # 路由配置
│   │   └── main.js                  # 应用入口
│   ├── package.json                 # npm配置
│   └── vite.config.js               # Vite配置
├── start.bat                        # Windows启动脚本
├── start.sh                         # macOS/Linux启动脚本
└── README.md                        # 项目说明
```

## 🎯 核心特性

### AI驱动的文献分析
- 使用先进的AI模型深度理解文献内容
- 自动生成结构化的阅读指南
- 智能识别文献类型和研究领域
- 提取关键信息和核心观点

### 高性能处理
- 支持大文件处理（最大10MB）
- 并行处理多个文献
- 流式响应，实时反馈处理进度
- 虚拟线程优化后台任务

### 数据安全
- 本地文件存储，数据不离开用户环境
- 支持逻辑删除，数据可恢复
- 完整的错误处理和日志记录

## 🐛 故障排除

### 常见问题

**1. 端口被占用**
```bash
# Windows
netstat -ano | findstr 8086
taskkill /PID <PID> /F

# macOS/Linux
lsof -i :8086
kill -9 <PID>
```

**2. API Key无效**
- 确保API Key正确且有效
- 检查API Key的额度和权限
- 确保网络连接正常

**3. 文件处理失败**
- 检查文件格式是否支持
- 确保文件大小不超过10MB
- 查看后端日志获取详细错误信息

**4. 构建失败**
```bash
# 清理并重新构建
mvn clean package -DskipTests
```

## 🤝 贡献指南

欢迎提交Issue和Pull Request来改进项目！

## 📄 许可证

本项目基于MIT许可证开源。

## 📞 联系方式

如有问题或建议，请通过以下方式联系：
- 提交GitHub Issue
- 发送邮件至项目维护者

---

**让AI帮助您更高效地管理学术文献！** 🎓✨