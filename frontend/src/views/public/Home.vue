<template>
  <div class="home-page">
    <section class="hero" aria-labelledby="conference-title">
      <div class="hero-copy">
        <div class="eyebrow"><span class="eyebrow-line"></span> {{ conference.shortName || 'SEDB 2026' }} · ACADEMIC CONFERENCE</div>
        <h1 id="conference-title" class="hero-title">{{ conference.name || '会议加载中…' }}</h1>
        <p class="hero-intro">{{ conference.intro }}</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" @click="goToSubmit">在线投稿 <el-icon><ArrowRight /></el-icon></el-button>
          <el-button size="large" @click="$router.push('/cfp')">查看征文通知</el-button>
        </div>
      </div>
      <div class="hero-detail">
        <div class="detail-label">会议概要 / CONFERENCE</div>
        <div class="detail-row">
          <span>举办日期</span>
          <strong>{{ formatDate(conference.startDate) }} — {{ formatDate(conference.endDate) }}</strong>
        </div>
        <div class="detail-row">
          <span>会议地点</span>
          <strong>{{ conference.venue || '待公布' }}</strong>
        </div>
        <div class="detail-row">
          <span>投稿截止</span>
          <strong>{{ formatDate(conference.submitDeadline) }}</strong>
        </div>
        <div v-if="countdown !== null || submitDaysLeft !== null" class="detail-countdown">
          <span v-if="countdown !== null">距会议开幕 <b>{{ countdown }}</b> 天</span>
          <span v-if="submitDaysLeft !== null">{{ submitDaysLeft > 0 ? `距投稿截止 ${submitDaysLeft} 天` : '投稿已截止' }}</span>
        </div>
      </div>
    </section>

    <div v-if="organizations.length" class="organizers">
      <a v-for="item in organizations" :key="item.label" :href="safeUrl(item.url) || undefined"
         :target="safeUrl(item.url) ? '_blank' : undefined" rel="noopener noreferrer" class="organization">
        <img v-if="safeUrl(item.logo)" :src="safeUrl(item.logo)" :alt="item.name + ' Logo'" />
        <span class="organization-label">{{ item.label }}</span><span>{{ item.name }}</span>
      </a>
    </div>

    <el-row :gutter="0" class="stats">
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-num">{{ stats.paperCount ?? '-' }}</div>
          <div class="stat-label">投稿论文</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-num">{{ stats.acceptedCount ?? '-' }}</div>
          <div class="stat-label">录用论文</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-card">
          <div class="stat-num">{{ stats.expertCount ?? '-' }}</div>
          <div class="stat-label">评审专家</div>
        </div>
      </el-col>
    </el-row>

    <section class="notice-section page-card">
      <div class="section-heading">
        <div>
          <div class="section-kicker">NEWS & UPDATES</div>
          <h2 class="page-title">最新通知</h2>
        </div>
        <router-link class="all-notices" to="/notifications">查看全部 <el-icon><ArrowRight /></el-icon></router-link>
      </div>
      <el-empty v-if="notifications.length === 0" description="暂无通知" />
      <router-link v-for="(item, index) in notifications.slice(0, 5)" :key="item.id"
           class="notice-item" :to="'/notifications/' + item.id">
        <span class="notice-index">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="notice-title">{{ item.title }}</span>
        <span class="notice-time">{{ formatDateTime(item.publishTime) }}</span>
        <el-icon class="notice-arrow"><ArrowRight /></el-icon>
      </router-link>
    </section>
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
  display: grid;
  grid-template-columns: minmax(0, 1.55fr) minmax(290px, 0.8fr);
  min-height: 430px;
  background: #173642;
  border: 1px solid #173642;
  overflow: hidden;
}
.hero-copy {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
  padding: 62px 64px;
}
.eyebrow, .section-kicker, .detail-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
}
.eyebrow { display: flex; align-items: center; gap: 10px; color: #c5a57b; margin-bottom: 24px; }
.eyebrow-line { width: 28px; height: 1px; background: #c5a57b; }
.hero-title {
  max-width: 690px;
  color: #fff;
  font-family: Georgia, 'Songti SC', SimSun, serif;
  font-size: clamp(31px, 3.1vw, 43px);
  font-weight: 700;
  line-height: 1.35;
  letter-spacing: 0.025em;
}
.hero-intro {
  max-width: 620px;
  color: #c3d0d2;
  font-size: 15px;
  line-height: 1.9;
  margin-top: 24px;
}
.hero-actions { display: flex; flex-wrap: wrap; gap: 12px; margin-top: 32px; }
.hero-actions .el-button { margin: 0; min-width: 150px; height: 44px; }
.hero-actions :deep(.el-button--primary) { background: #c5a57b; border-color: #c5a57b; color: #15323d; }
.hero-actions :deep(.el-button--primary:hover) { background: #d6b78f; border-color: #d6b78f; }
.hero-actions :deep(.el-button:not(.el-button--primary)) { color: #fff; background: transparent; border-color: #9fb4b8; }
.hero-actions :deep(.el-button:not(.el-button--primary):hover) { border-color: #fff; background: rgba(255,255,255,.08); }
.hero-detail { background: #efede7; padding: 44px 36px; border-left: 4px solid #c5a57b; }
.detail-label { color: #9a7654; padding-bottom: 24px; border-bottom: 1px solid #d9d8d1; }
.detail-row { display: flex; flex-direction: column; gap: 7px; padding: 22px 0; border-bottom: 1px solid #d9d8d1; }
.detail-row span { color: #718087; font-size: 12px; letter-spacing: .04em; }
.detail-row strong { color: #193642; font-size: 16px; font-weight: 650; line-height: 1.55; }
.detail-countdown { display: flex; flex-direction: column; gap: 7px; color: #47616b; font-size: 13px; padding-top: 22px; }
.detail-countdown b { color: #9a7654; font: 700 20px Georgia, serif; }
.organizers { display: flex; flex-wrap: wrap; gap: 14px 28px; padding: 20px 4px 0; }
.organization { display: inline-flex; align-items: center; gap: 8px; color: #344a54; text-decoration: none; font-size: 13px; }
.organization:hover { color: #245466; }
.organization-label { color: #819096; font-size: 12px; }
.organization img { max-width: 60px; max-height: 36px; object-fit: contain; }
.stats { margin: 24px 0; background: #fff; border: 1px solid #e5e9e8; }
.stat-card { text-align: center; padding: 23px 8px; }
.stats .el-col + .el-col .stat-card { border-left: 1px solid #e5e9e8; }
.stat-num { color: #1d4555; font: 700 30px Georgia, serif; }
.stat-label { color: #718087; font-size: 13px; margin-top: 6px; }
.notice-section { padding: 30px 36px 20px; }
.section-heading { display: flex; align-items: flex-end; justify-content: space-between; gap: 20px; margin-bottom: 8px; }
.section-kicker { color: #a17f59; margin-bottom: 9px; }
.section-heading .page-title { margin-bottom: 0; }
.all-notices { display: inline-flex; align-items: center; gap: 5px; color: #245466; text-decoration: none; font-size: 13px; font-weight: 700; padding-bottom: 4px; }
.all-notices:hover { color: #9a7654; }
.notice-item { display: grid; grid-template-columns: 38px minmax(0, 1fr) auto 18px; align-items: center; gap: 14px; padding: 19px 4px; border-bottom: 1px solid #e9edeb; text-decoration: none; }
.notice-item:last-child { border-bottom: 0; }
.notice-index { color: #b69873; font: 700 15px Georgia, serif; }
.notice-title { color: #263b45; font-weight: 600; }
.notice-time { color: #88979c; font-size: 12px; }
.notice-arrow { color: #9eb2b8; }
.notice-item:hover .notice-title, .notice-item:hover .notice-arrow { color: #9a7654; }
@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; }
  .hero-copy { padding: 48px 42px; }
  .hero-detail { display: grid; grid-template-columns: repeat(3, 1fr); gap: 0 20px; border-left: 0; border-top: 3px solid #c5a57b; padding: 26px 36px; }
  .detail-label, .detail-countdown { grid-column: 1 / -1; }
  .detail-label { padding-bottom: 14px; }
  .detail-row { border-bottom: 0; padding: 20px 0 10px; }
  .detail-countdown { border-top: 1px solid #d9d8d1; margin-top: 12px; padding-top: 16px; flex-direction: row; gap: 20px; }
}
@media (max-width: 600px) {
  .hero-copy { padding: 38px 25px 42px; }
  .hero-title { font-size: 29px; }
  .hero-intro { font-size: 14px; margin-top: 18px; }
  .hero-actions { margin-top: 24px; width: 100%; }
  .hero-actions .el-button { flex: 1; min-width: 135px; }
  .hero-detail { display: block; padding: 26px 24px; }
  .detail-row { border-bottom: 1px solid #d9d8d1; padding: 15px 0; }
  .detail-row strong { font-size: 15px; }
  .detail-countdown { flex-wrap: wrap; margin-top: 0; border-top: 0; }
  .stats { margin: 16px 0; }
  .stat-card { padding: 19px 3px; }
  .stat-num { font-size: 26px; }
  .stat-label { font-size: 12px; }
  .notice-section { padding: 24px 18px 12px; }
  .notice-item { grid-template-columns: 28px minmax(0, 1fr) 18px; gap: 8px; }
  .notice-time { grid-column: 2; grid-row: 2; }
  .notice-arrow { grid-column: 3; grid-row: 1 / span 2; }
}
</style>
