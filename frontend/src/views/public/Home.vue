<template>
  <div class="home-page">
    <section class="hero" aria-labelledby="conference-title" @pointermove="moveHeroArt" @pointerleave="resetHeroArt">
      <div class="hero-art" aria-hidden="true">
        <img src="/conference-constellation.svg" alt="" />
      </div>
      <div class="hero-copy">
        <div class="hero-masthead"><span>学术会议 · 广州</span><span>NO. 2026 / SEDB</span></div>
        <div class="eyebrow"><span class="eyebrow-line"></span> {{ conference.shortName || 'SEDB 2026' }} · ACADEMIC CONFERENCE</div>
        <h1 id="conference-title" class="hero-title">{{ conference.name || '会议加载中…' }}</h1>
        <p class="hero-intro">{{ conference.intro }}</p>
        <div class="hero-actions">
          <el-button v-ripple type="primary" size="large" @click="goToSubmit">在线投稿 <el-icon><ArrowRight /></el-icon></el-button>
          <el-button v-ripple size="large" @click="$router.push('/cfp')">查看征文通知</el-button>
        </div>
        <div class="hero-footnote"><span class="footnote-rule"></span> 探索软件工程与数据库技术的交汇处</div>
      </div>
    </section>

    <div class="hero-detail" aria-label="会议概要">
      <div class="detail-row"><span>01 / 举办日期</span><strong>{{ formatDate(conference.startDate) }} — {{ formatDate(conference.endDate) }}</strong></div>
      <div class="detail-row"><span>02 / 会议地点</span><strong>{{ conference.venue || '待公布' }}</strong></div>
      <div class="detail-row"><span>03 / 投稿截止</span><strong>{{ formatDate(conference.submitDeadline) }}</strong></div>
      <div class="detail-countdown"><span v-if="countdown !== null">开幕倒计时</span><strong v-if="countdown !== null">{{ countdown }} <small>天</small></strong><span v-else>会议日程</span><strong v-if="countdown === null">敬请关注</strong></div>
    </div>

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

    <div class="home-content-grid">
      <div class="home-main">
    <section v-if="topics.length" class="tracks-section" aria-labelledby="tracks-title">
      <div class="editorial-heading">
        <div class="heading-number">01 <span>/</span> RESEARCH TRACKS</div>
        <div class="heading-main">
          <div><h2 id="tracks-title">研究方向</h2><p>从基础理论到工程实践，探索值得讨论的问题。</p></div>
          <router-link to="/cfp" class="section-link">查看全部专题 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
      </div>
      <div class="track-grid">
        <router-link v-for="(topic, index) in topics.slice(0, 4)" :key="topic.id" v-ripple class="track-card" :to="'/cfp'">
          <div class="track-top"><span>{{ String(index + 1).padStart(2, '0') }}</span><span class="track-motif" :class="`motif-${index % 4}`" aria-hidden="true"><i></i><i></i><i></i></span></div>
          <h3>{{ topic.name }}</h3>
          <p>{{ topic.description }}</p>
          <span class="track-link">了解方向 <el-icon><ArrowUpRight /></el-icon></span>
        </router-link>
      </div>
    </section>

    <section v-if="keynotes.length" class="featured-section" aria-labelledby="featured-title">
      <div class="editorial-heading">
        <div class="heading-number">02 <span>/</span> KEYNOTE TALKS</div>
        <div class="heading-main">
          <div><h2 id="featured-title">主旨报告</h2><p>来自不同研究方向的观点，在这里交汇。</p></div>
          <router-link to="/experts#reports" class="section-link">查看报告安排 <el-icon><ArrowRight /></el-icon></router-link>
        </div>
      </div>
      <div class="featured-grid">
        <router-link v-for="(keynote, index) in keynotes.slice(0, 2)" :key="keynote.id" v-ripple class="featured-card" to="/experts#reports">
          <span class="featured-no">0{{ index + 1 }}</span>
          <div class="featured-content"><span class="featured-label">KEYNOTE / {{ keynote.timeNote || '时间待定' }}</span><h3>{{ keynote.title }}</h3><p>{{ keynote.expertName }} · {{ keynote.affiliation }}</p></div>
          <el-icon class="featured-arrow"><ArrowUpRight /></el-icon>
        </router-link>
      </div>
    </section>

    <section class="notice-section page-card">
      <div class="section-heading">
        <div>
          <div class="section-kicker">03 / NEWS & UPDATES</div>
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

      <aside class="home-aside" aria-label="会议日程与重要日期">
        <section class="aside-panel" aria-labelledby="schedule-heading">
          <div class="aside-kicker">PROGRAM / {{ conference.shortName || 'SEDB 2026' }}</div>
          <h2 id="schedule-heading" class="aside-title">会议日程 <small>{{ formatDate(conference.startDate) }}</small></h2>
          <p v-if="!schedulePreview.length" class="aside-empty">日程尚未发布，请稍后查看。</p>
          <div v-for="(item, index) in schedulePreview" :key="item.id || `${item.day}-${item.startTime}-${index}`">
            <div v-if="index === 0 || item.day !== schedulePreview[index - 1].day" class="schedule-day">{{ formatDate(item.day) }}</div>
            <router-link v-ripple class="timeline-item" to="/schedule">
              <span class="timeline-time">{{ item.startTime }}</span>
              <span class="timeline-body"><strong>{{ item.title }}</strong><small>{{ item.location || '地点待定' }}</small></span>
            </router-link>
          </div>
          <router-link class="schedule-more" to="/schedule">查看完整日程 <el-icon><ArrowRight /></el-icon></router-link>
        </section>
        <section class="deadline-panel" aria-labelledby="deadline-heading">
          <div class="aside-kicker">IMPORTANT DATES</div>
          <h3 id="deadline-heading">重要日期</h3>
          <div class="deadline-row"><span>投稿截止</span><strong>{{ formatDate(conference.submitDeadline) }}</strong></div>
          <div class="deadline-row"><span>录用通知</span><strong>{{ formatDate(conference.notifyDeadline) }}</strong></div>
          <div class="deadline-row"><span>终稿提交</span><strong>{{ formatDate(conference.finalDeadline) }}</strong></div>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConference, getPublicStats, getNotifications, getTopics, getKeynotes, getSchedule } from '../../api'
import { formatDate, formatDateTime } from '../../utils/format'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const store = useUserStore()
const conference = ref({})
const stats = ref({})
const notifications = ref([])
const topics = ref([])
const keynotes = ref([])
const schedule = ref([])
const schedulePreview = computed(() => [...schedule.value]
  .sort((a, b) => `${a.day} ${a.startTime}`.localeCompare(`${b.day} ${b.startTime}`))
  .slice(0, 5))
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

function moveHeroArt(event) {
  if (event.pointerType === 'touch' || window.matchMedia('(prefers-reduced-motion: reduce)').matches) return
  const bounds = event.currentTarget.getBoundingClientRect()
  const x = ((event.clientX - bounds.left) / bounds.width - 0.5) * 20
  const y = ((event.clientY - bounds.top) / bounds.height - 0.5) * 20
  event.currentTarget.style.setProperty('--art-x', `${x}px`)
  event.currentTarget.style.setProperty('--art-y', `${y}px`)
}

function resetHeroArt(event) {
  event.currentTarget.style.setProperty('--art-x', '0px')
  event.currentTarget.style.setProperty('--art-y', '0px')
}

// 开幕倒计时(天);已开幕或未配置则不显示
const countdown = computed(() => {
  if (!conference.value.startDate) return null
  const days = Math.ceil((new Date(conference.value.startDate) - new Date()) / 86400000)
  return days > 0 ? days : null
})

onMounted(async () => {
  const results = await Promise.allSettled([
    getConference(), getPublicStats(), getNotifications(), getTopics(), getKeynotes(), getSchedule()
  ])
  if (results[0].status === 'fulfilled') conference.value = results[0].value || {}
  if (results[1].status === 'fulfilled') stats.value = results[1].value || {}
  if (results[2].status === 'fulfilled') notifications.value = results[2].value || []
  if (results[3].status === 'fulfilled') topics.value = results[3].value || []
  if (results[4].status === 'fulfilled') keynotes.value = results[4].value || []
  if (results[5].status === 'fulfilled') schedule.value = results[5].value || []
})
</script>

<style scoped src="./Home.css"></style>
