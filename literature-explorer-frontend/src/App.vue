<template>
  <div id="app">
    <el-container class="app-container">
      <!-- 顶部导航栏 -->
      <el-header class="app-header">
        <div class="header-content">
          <div class="title-section">
            <h1 class="app-title">
              <img src="/logo.png" alt="Logo" class="app-logo" />
              文献助手
            </h1>
          </div>
        </div>
      </el-header>
      
      <!-- 主内容区域 -->
      <el-main class="app-main">
        <ErrorBoundary>
          <router-view />
        </ErrorBoundary>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import ErrorBoundary from './components/ErrorBoundary.vue'
</script>

<style scoped>
.app-container {
  min-height: 100vh;
}

.app-header {
  background: #ffffff;
  color: #333333;
  display: flex;
  align-items: center;
  padding: 0 24px;
  box-shadow: 0 2px 8px 0 rgba(0, 0, 0, 0.08);
  border-bottom: 1px solid #e5e7eb;
}

.header-content {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.title-section {
  display: flex;
  align-items: center;
  gap: 12px;
}

.app-logo {
  height: 32px;
  width: auto;
}

.app-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  letter-spacing: 0.5px;
  color: #333333;
  position: relative;
  display: flex;
  align-items: center;
  gap: 8px;
}


.app-main {
  background-color: #f5f7fa;
  padding: 24px;
  min-height: calc(100vh - 60px);
}
</style>
