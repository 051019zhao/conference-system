<template>
  <div class="public-layout">
    <header class="header">
      <div class="header-inner">
        <router-link class="brand" to="/">
          <span class="brand-mark" aria-hidden="true">S</span>
          <span class="brand-name">{{ conference.shortName || '会议管理系统' }}</span>
        </router-link>
        <el-menu mode="horizontal" router :default-active="$route.path" class="nav desktop-nav" :ellipsis="false">
          <el-menu-item index="/">首页</el-menu-item>
          <el-menu-item index="/cfp">征文</el-menu-item>
          <el-menu-item index="/experts">专家介绍</el-menu-item>
          <el-menu-item index="/keynotes">主旨报告</el-menu-item>
          <el-menu-item index="/schedule">会议日程</el-menu-item>
          <el-menu-item index="/accepted">录用论文</el-menu-item>
          <el-menu-item index="/notifications">通知</el-menu-item>
          <el-menu-item index="/materials">资料下载</el-menu-item>
        </el-menu>
        <el-button class="mobile-nav-toggle" text aria-label="打开导航菜单" @click="mobileMenuOpen = true">
          <el-icon :size="22"><Menu /></el-icon>
        </el-button>
        <div class="user-area">
          <template v-if="store.isLogin">
            <el-dropdown @command="handleCommand">
              <span class="user-entry">
                <el-avatar :size="28" :src="avatarUrl" @error="avatarUrl = ''" class="top-avatar">
                  {{ store.realName?.charAt(0) }}
                </el-avatar>
                <span class="user-name">{{ store.realName }}<span class="role-label">({{ roleText[store.role] }})</span></span>
                <el-icon><ArrowDown /></el-icon>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="account">个人中心</el-dropdown-item>
                  <el-dropdown-item v-if="store.role === 'AUTHOR'" command="my">我的投稿</el-dropdown-item>
                  <el-dropdown-item v-if="store.role === 'EXPERT'" command="review">评审任务</el-dropdown-item>
                  <el-dropdown-item v-if="store.role === 'ADMIN'" command="admin">后台管理</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
          <template v-else>
            <el-button text @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </header>

    <el-drawer v-model="mobileMenuOpen" title="会议导航" direction="ltr" size="min(320px, 86vw)">
      <el-menu router :default-active="$route.path" @select="mobileMenuOpen = false">
        <el-menu-item index="/">首页</el-menu-item>
        <el-menu-item index="/cfp">征文</el-menu-item>
        <el-menu-item index="/experts">专家介绍</el-menu-item>
        <el-menu-item index="/keynotes">主旨报告</el-menu-item>
        <el-menu-item index="/schedule">会议日程</el-menu-item>
        <el-menu-item index="/accepted">录用论文</el-menu-item>
        <el-menu-item index="/notifications">通知</el-menu-item>
        <el-menu-item index="/materials">资料下载</el-menu-item>
      </el-menu>
    </el-drawer>

    <main class="main">
      <div class="main-inner">
        <router-view />
      </div>
    </main>

    <footer class="footer">
      {{ conference.name || '会议管理系统' }}
      <template v-if="conference.contactEmail"> · 联系邮箱:{{ conference.contactEmail }}</template>
    </footer>

    <AiChat />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { getConference } from '../api'
import { roleText } from '../utils/format'
import AiChat from '../components/AiChat.vue'

const router = useRouter()
const store = useUserStore()
const conference = ref({})
const mobileMenuOpen = ref(false)
const avatarUrl = ref(store.isLogin ? `/api/public/avatar/${store.user?.id}` : '')

onMounted(async () => {
  try {
    conference.value = (await getConference()) || {}
  } catch (e) {
    /* 后端未启动时静默 */
  }
})

function handleCommand(command) {
  if (command === 'logout') {
    store.logout()
    ElMessage.success('已退出登录')
    router.push('/')
  } else if (command === 'my') {
    router.push('/author/papers')
  } else if (command === 'review') {
    router.push('/expert/tasks')
  } else if (command === 'admin') {
    router.push('/admin')
  } else if (command === 'account') {
    router.push('/account')
  }
}
</script>

<style scoped>
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
.header {
  background: #fff;
  border-bottom: 1px solid #e4e9e7;
  position: sticky;
  top: 0;
  z-index: 100;
}
.header::before {
  content: '';
  display: block;
  height: 3px;
  background: #b3926d;
}
.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  display: flex;
  align-items: center;
  padding: 0 16px;
  min-height: 72px;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 19px;
  font-weight: 700;
  color: #1a3c4b;
  cursor: pointer;
  white-space: nowrap;
  margin-right: 12px;
  text-decoration: none;
  min-width: 0;
}
.brand-mark {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid #b3926d;
  color: #9d7650;
  font: 700 22px Georgia, serif;
  flex-shrink: 0;
}
.brand span {
  overflow: hidden;
  text-overflow: ellipsis;
}
.brand .brand-mark { overflow: visible; }
.nav {
  flex: 1;
  border-bottom: none;
}
.nav :deep(.el-menu-item) {
  color: #5f7078;
  font-size: 14px;
  padding: 0 13px;
  border-bottom: 2px solid transparent;
}
.nav :deep(.el-menu-item:hover) { color: #193f50; background: transparent; }
.nav :deep(.el-menu-item.is-active) {
  color: #193f50;
  border-bottom-color: #b3926d;
  font-weight: 700;
}
.user-area {
  white-space: nowrap;
  display: flex;
  align-items: center;
  gap: 4px;
}
.mobile-nav-toggle {
  display: none;
}
.user-entry {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #303133;
}
.top-avatar {
  background: #245466;
  flex-shrink: 0;
}
.main {
  flex: 1;
  padding: 36px 0 48px;
}
.main-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}
.footer {
  text-align: center;
  color: #b7c5c8;
  background: #17313e;
  padding: 28px 12px;
  font-size: 13px;
  letter-spacing: 0.02em;
}
@media (max-width: 1000px) {
  .desktop-nav { display: none; }
  .brand { flex: 1; }
  .mobile-nav-toggle { display: inline-flex; flex-shrink: 0; }
  .user-name { display: inline-block; max-width: 100px; overflow: hidden; text-overflow: ellipsis; }
}
@media (max-width: 480px) {
  .header-inner { padding: 0 12px; min-height: 58px; }
  .brand { font-size: 16px; }
  .brand-mark { width: 28px; height: 28px; font-size: 19px; }
  .role-label { display: none; }
  .main { padding: 16px 0; }
  .main-inner { padding: 0 12px; }
  .footer { padding: 18px 12px; line-height: 1.6; }
}
</style>
