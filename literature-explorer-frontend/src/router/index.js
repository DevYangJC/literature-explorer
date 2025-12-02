import { createRouter, createWebHistory } from 'vue-router'
import LiteratureListView from '@/views/LiteratureListView.vue'
import LiteratureDetailView from '@/views/LiteratureDetailView.vue'

const routes = [
  {
    path: '/',
    name: 'LiteratureList',
    component: LiteratureListView,
    meta: {
      title: '文献列表'
    }
  },
  {
    path: '/literature/:id',
    name: 'LiteratureDetail',
    component: LiteratureDetailView,
    meta: {
      title: '文献详情'
    }
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫 - 设置页面标题
router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 文献助手`
  }
  next()
})

export default router
