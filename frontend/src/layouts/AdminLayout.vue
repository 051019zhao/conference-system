<template>
  <el-container class="admin-layout">
    <el-aside width="220px" class="aside">
      <div class="logo">会议管理后台</div>
      <el-menu router :default-active="$route.path" background-color="#001529" text-color="#a6adb4"
               active-text-color="#fff">
        <el-menu-item index="/admin">
          <el-icon><DataAnalysis /></el-icon><span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/admin/users">
          <el-icon><User /></el-icon><span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/papers">
          <el-icon><Document /></el-icon><span>论文管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/topics">
          <el-icon><Collection /></el-icon><span>专题管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/keynotes">
          <el-icon><Microphone /></el-icon><span>主旨报告管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/schedule">
          <el-icon><Calendar /></el-icon><span>日程管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/notifications">
          <el-icon><Bell /></el-icon><span>通知管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/materials">
          <el-icon><FolderOpened /></el-icon><span>资料管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/logs">
          <el-icon><Tickets /></el-icon><span>操作日志</span>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="topbar">
        <span>欢迎,{{ store.realName }}</span>
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
  </el-container>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()

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
.aside {
  background: #001529;
}
.logo {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  text-align: center;
  padding: 20px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}
.aside :deep(.el-menu) {
  border-right: none;
}
.topbar {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}
.content {
  background: #f5f7fa;
}
</style>
