<template>
  <div class="app-container">
    <!-- 左侧菜单 -->
    <div class="sidebar">
      <div class="sidebar-header">
        <h3 class="sidebar-title">管理菜单</h3>
        <button class="logout-btn" @click="handleLogout" title="退出登录">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"></path>
            <polyline points="16 17 21 12 16 7"></polyline>
            <line x1="21" y1="12" x2="9" y2="12"></line>
          </svg>
        </button>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        @select="handleMenuSelect"
        router
      >
        <!-- 试题管理分组 -->
        <el-submenu index="question">
          <template #title>试题管理</template>
          <el-menu-item index="/admin/question-manage">
            <el-icon><Document /></el-icon>
            <span>题目管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/category-manage">
            <el-icon><Folder /></el-icon>
            <span>类别管理</span>
          </el-menu-item>
        </el-submenu>
        <!-- 考试管理分组 -->
        <el-submenu index="exam">
          <template #title>考试管理</template>
          <el-menu-item index="/admin/paper-manage">
            <el-icon><Files /></el-icon>
            <span>试卷管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/score-manage">
            <el-icon><DataAnalysis /></el-icon>
            <span>成绩管理</span>
          </el-menu-item>
        </el-submenu>
        <!-- 系统管理分组 -->
        <el-submenu index="system">
          <template #title>系统管理</template>
          <el-menu-item index="/admin/banner-manage">
            <el-icon><Picture /></el-icon>
            <span>轮播图管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/notice-manage">
            <el-icon><Bell /></el-icon>
            <span>公告管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/user-manage">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
        </el-submenu>
        <!-- 视频管理分组 -->
        <el-submenu index="video">
          <template #title>视频管理</template>
          <el-menu-item index="/admin/video-manage">
            <el-icon><VideoPlay /></el-icon>
            <span>视频管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/video-category-manage">
            <el-icon><Collection /></el-icon>
            <span>视频分类</span>
          </el-menu-item>
        </el-submenu>
      </el-menu>
    </div>

    <!-- 右侧内容区 -->
    <div class="main-content">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { 
  Document, Folder, Files, DataAnalysis, 
  Picture, Bell, VideoPlay, Collection 
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const activeMenu = computed(() => {
  return route.path
})

const handleMenuSelect = (index) => {
  router.push(index)
}

const handleLogout = () => {
  localStorage.removeItem('userInfo')
  ElMessage.success('退出成功')
  router.push('/home')
}
</script>

<style scoped>
.app-container {
  display: flex;
  height: 100vh;
  background-color: #f5f5f5;
}

.sidebar {
  width: 250px;
  background: white;
  box-shadow: 2px 0 8px rgba(0,0,0,0.1);
}

.sidebar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.sidebar-title {
  margin: 0;
  color: #333;
}

.logout-btn {
  background: none;
  border: none;
  color: #999;
  cursor: pointer;
  padding: 6px;
  border-radius: 6px;
  transition: all 0.2s;
}

.logout-btn:hover {
  background: #f5f5f5;
  color: #f56c6c;
}

.sidebar-menu {
  border: none;
}

.main-content {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}
</style>
