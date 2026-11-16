<template>
  <div>
    <div class="hero page-card">
      <div class="hero-title">{{ conference.name || '会议加载中…' }}</div>
      <div class="hero-meta">
        <span><el-icon><Calendar /></el-icon> {{ formatDate(conference.startDate) }} ~ {{ formatDate(conference.endDate) }}</span>
        <span><el-icon><Location /></el-icon> {{ conference.venue || '-' }}</span>
      </div>
      <div class="hero-badges">
        <el-tag v-if="countdown !== null" type="warning" size="large" effect="dark">
          距会议开幕还有 {{ countdown }} 天
        </el-tag>
        <el-tag v-if="submitDaysLeft !== null" :type="submitDaysLeft > 7 ? 'success' : 'danger'" size="large" effect="plain">
          投稿截止:{{ formatDate(conference.submitDeadline) }}
          {{ submitDaysLeft > 0 ? `(剩 ${submitDaysLeft} 天)` : '(已截止)' }}
        </el-tag>
      </div>
      <p class="hero-intro">{{ conference.intro }}</p>
      <div v-if="conference.organizerName || conference.coorganizerName" class="organizers">
        <a v-for="item in organizations" :key="item.label" :href="safeUrl(item.url) || undefined"
           :target="safeUrl(item.url) ? '_blank' : undefined" rel="noopener noreferrer" class="organization">
          <img v-if="safeUrl(item.logo)" :src="safeUrl(item.logo)" :alt="item.name + ' Logo'" />
          <span>{{ item.label }}：{{ item.name }}</span>
        </a>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="$router.push('/cfp')">查看征文详情</el-button>
        <el-button size="large" @click="goToSubmit">在线投稿</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stats">
      <el-col :span="8">
        <div class="stat-card page-card">
          <div class="stat-num">{{ stats.paperCount ?? '-' }}</div>
          <div class="stat-label">投稿论文</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card page-card">
          <div class="stat-num">{{ stats.acceptedCount ?? '-' }}</div>
          <div class="stat-label">录用论文</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card page-card">
          <div class="stat-num">{{ stats.expertCount ?? '-' }}</div>
          <div class="stat-label">评审专家</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card">
      <div class="page-title">最新通知</div>
      <el-empty v-if="notifications.length === 0" description="暂无通知" />
      <div v-for="item in notifications.slice(0, 5)" :key="item.id"
           class="notice-item" @click="$router.push('/notifications/' + item.id)">
        <span class="notice-title">{{ item.title }}</span>
        <span class="notice-time">{{ formatDateTime(item.publishTime) }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConference, getPublicStats, getNotifications } from '../../api'
import { formatDate, formatDateTime } from '../../utils/format'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const store = useUserStore()
const conference = ref({})
const stats = ref({})
const notifications = ref([])
const organizations = computed(() => [
  { label: '主办单位', name: conference.value.organizerName, url: conference.value.organizerUrl,
    logo: conference.value.organizerLogoUrl },
  { label: '协办单位', name: conference.value.coorganizerName, url: conference.value.coorganizerUrl,
    logo: conference.value.coorganizerLogoUrl }
].filter(item => item.name))

function safeUrl(value) {
  if (!value) return ''
  return /^https?:\/\//i.test(value) || /^\/[^/]/.test(value) ? value : ''
}

function goToSubmit() {
  if (store.isLogin && store.role !== 'AUTHOR') {
    ElMessage.warning('当前账号不是作者账号，请退出后使用作者账号登录')
    return
  }
  // 路由守卫会引导未登录用户先登录，并在登录成功后返回投稿页。
  router.push('/author/submit')
}

// 开幕倒计时(天);已开幕或未配置则不显示
const countdown = computed(() => {
  if (!conference.value.startDate) return null
  const days = Math.ceil((new Date(conference.value.startDate) - new Date()) / 86400000)
  return days > 0 ? days : null
})

// 投稿截止剩余天数;未配置则不显示(截止规则由后端强制执行,这里仅展示)
const submitDaysLeft = computed(() => {
  if (!conference.value.submitDeadline) return null
  return Math.ceil((new Date(conference.value.submitDeadline) - new Date()) / 86400000)
})

onMounted(async () => {
  conference.value = (await getConference()) || {}
  stats.value = (await getPublicStats()) || {}
  notifications.value = (await getNotifications()) || []
})
</script>

<style scoped>
.hero {
  text-align: center;
  padding: 48px 24px;
  background: linear-gradient(135deg, #e8f3ff 0%, #fff 60%);
}
.hero-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 12px;
}
.hero-meta {
  display: flex;
  justify-content: center;
  gap: 24px;
  color: #606266;
  margin-bottom: 16px;
}
.hero-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.hero-intro {
  color: #606266;
  max-width: 720px;
  margin: 0 auto 20px;
  line-height: 1.8;
}
.organizers { display: flex; justify-content: center; gap: 24px; flex-wrap: wrap; margin-bottom: 20px; }
.organization { display: flex; align-items: center; gap: 8px; color: #606266; text-decoration: none; }
.organization img { max-width: 72px; max-height: 48px; object-fit: contain; }
.countdown {
  margin-bottom: 20px;
}
.hero-badges {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.stats {
  margin: 16px 0;
}
.stat-card {
  text-align: center;
}
.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #409eff;
}
.stat-label {
  color: #909399;
  margin-top: 4px;
}
.notice-item {
  display: flex;
  justify-content: space-between;
  padding: 12px 4px;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
}
.notice-item:hover .notice-title {
  color: #409eff;
}
.notice-time {
  color: #909399;
  font-size: 13px;
}
@media (max-width: 600px) {
  .hero { padding: 32px 16px; }
  .hero-title { font-size: 24px; line-height: 1.35; }
  .hero-meta { flex-direction: column; align-items: center; gap: 8px; }
  .hero-actions { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; }
  .hero-actions .el-button { margin: 0; }
  .stat-card { padding: 16px 4px; }
  .stat-num { font-size: 26px; }
  .stat-label { font-size: 12px; }
  .notice-item { flex-direction: column; gap: 4px; }
}
</style>
