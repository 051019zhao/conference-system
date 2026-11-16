<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo">会议管理后台</div>
      <el-menu router :default-active="$route.path" background-color="#17313e" text-color="#b9c7ca"
               active-text-color="#fff">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <div class="topbar-leading">
          <el-button class="mobile-nav-toggle" text aria-label="打开后台菜单" @click="mobileMenuOpen = true">
            <el-icon :size="22"><Menu /></el-icon>
          </el-button>
          <span class="welcome-name">欢迎,{{ store.realName }}</span>
          <span class="mobile-title">会议管理后台</span>
        </div>
        <div>
          <el-button text @click="$router.push('/')">
            <el-icon><HomeFilled /></el-icon> 前台首页
          </el-button>
          <el-button text type="danger" @click="logout">退出登录</el-button>
        </div>
      </el-header>
      <el-main class="content">
        <router-view />
      </el-main>
    </el-container>
    <el-drawer v-model="mobileMenuOpen" title="会议管理后台" direction="ltr" size="min(300px, 85vw)">
      <el-menu router :default-active="$route.path" @select="mobileMenuOpen = false">
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon><span>{{ item.label }}</span>
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </el-container>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { DataAnalysis, User, Document, Collection, Microphone, Calendar, Bell, FolderOpened, Tickets } from '@element-plus/icons-vue'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()
const mobileMenuOpen = ref(false)
const navItems = [
  { path: '/admin', label: '仪表盘', icon: DataAnalysis },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/papers', label: '论文管理', icon: Document },
  { path: '/admin/topics', label: '专题管理', icon: Collection },
  { path: '/admin/keynotes', label: '主旨报告管理', icon: Microphone },
  { path: '/admin/schedule', label: '日程管理', icon: Calendar },
  { path: '/admin/notifications', label: '通知管理', icon: Bell },
  { path: '/admin/materials', label: '资料管理', icon: FolderOpened },
  { path: '/admin/logs', label: '操作日志', icon: Tickets }
]

function logout() {
  store.logout()
  ElMessage.success('已退出登录')
  router.push('/')
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
}
.admin-layout > .el-container { min-width: 0; }
.aside {
  background: #17313e;
}
.logo {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
  font-family: Georgia, 'Songti SC', SimSun, serif;
}
.aside :deep(.el-menu) {
  border-right: none;
}
.aside :deep(.el-menu-item.is-active) { background: #245061; border-left: 3px solid #c5a57b; }
.aside :deep(.el-menu-item:hover) { background: #214451; }
.topbar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #e4e9e7;
}
.content {
  background: #f5f5f2;
  min-width: 0;
}
.topbar-leading { display: flex; align-items: center; gap: 8px; }
.mobile-nav-toggle, .mobile-title { display: none; }
@media (max-width: 900px) {
  .aside { display: none; }
  .mobile-nav-toggle { display: inline-flex; }
  .mobile-title { display: inline; font-weight: 600; }
  .welcome-name { display: none; }
  .topbar { padding: 0 12px; }
  .content { padding: 12px; }
}
</style>
