<template>
  <div>
    <div class="page-card cfp-card">
      <div class="section-kicker">CALL FOR PAPERS</div>
      <h1 class="page-title">征文通知</h1>
      <pre class="cfp-text">{{ conference.cfpText || '征文内容暂未发布' }}</pre>
      <div class="dates">
        <div class="date-item"><span>投稿截止</span><strong>{{ conference.submitDeadline || '待公布' }}</strong></div>
        <div class="date-item"><span>录用通知</span><strong>{{ conference.notifyDeadline || '待公布' }}</strong></div>
        <div class="date-item"><span>终稿截止</span><strong>{{ conference.finalDeadline || '待公布' }}</strong></div>
        <div class="date-item"><span>会议日期</span><strong>{{ conference.startDate || '待公布' }} — {{ conference.endDate || '待公布' }}</strong></div>
      </div>
      <div class="conference-meta">地点：{{ conference.venue || '待公布' }} · 联系邮箱：{{ conference.contactEmail || '待公布' }}</div>
      <el-button link type="primary" @click="$router.push('/materials')">查看会议资料下载区</el-button>
    </div>
    <div class="page-card topics-section">
      <div class="section-kicker">RESEARCH TRACKS</div>
      <h2 class="page-title">征文专题</h2>
      <el-empty v-if="topics.length === 0" description="暂无专题" />
      <div class="topic-grid">
      <el-card v-for="t in topics" :key="t.id" class="topic-card" shadow="never">
        <b>{{ t.name }}</b>
        <p>{{ t.description }}</p>
        <div v-if="t.reportName">主题报告：{{ t.reportName }}
          <span v-if="t.reportTimeNote"> · {{ t.reportTimeNote }}</span>
        </div>
        <div v-if="t.keynoteExpertName">报告专家：{{ t.keynoteExpertName }}</div>
        <div v-if="t.reportLimit != null">报告名额：{{ t.reportLimit }}</div>
        <el-button v-if="t.hasPpt" link type="primary" @click="downloadTopicPpt(t.id, t.name, t.pptExt || 'pptx')">
          下载报告文件
        </el-button>
      </el-card>
      </div>
    </div>
    <div class="submit-actions">
      <el-button type="primary" size="large" @click="goToSubmit">在线投稿</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getConference, getTopics, downloadTopicPpt } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const store = useUserStore()
const conference = ref({})
const topics = ref([])

function goToSubmit() {
  if (store.isLogin && store.role !== 'AUTHOR') {
    ElMessage.warning('当前账号不是作者账号，请退出后使用作者账号登录')
    return
  }
  router.push('/author/submit')
}

onMounted(async () => {
  conference.value = (await getConference()) || {}
  topics.value = (await getTopics()) || []
})
</script>

<style scoped>
.section-kicker { color: #a17f59; font-size: 11px; font-weight: 700; letter-spacing: .18em; margin-bottom: 9px; }
.cfp-card { padding: 38px 44px; }
.cfp-text {
  white-space: pre-wrap;
  line-height: 2;
  font-family: inherit;
  color: #45575f;
  font-size: 15px;
  max-width: 880px;
}
.dates { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 14px; margin-top: 30px; padding-top: 24px; border-top: 1px solid #e8ecea; }
.date-item { display: flex; flex-direction: column; gap: 8px; border-left: 2px solid #b3926d; padding-left: 13px; }
.date-item span { font-size: 12px; color: #7d8c91; }
.date-item strong { font-size: 14px; color: #1f3d4a; line-height: 1.5; }
.conference-meta { margin: 24px 0 4px; color: #687b83; font-size: 13px; }
.topics-section { margin-top: 20px; padding: 36px 44px; }
.topic-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 14px; }
.topic-card :deep(.el-card__body) { padding: 22px; }
.topic-card b { color: #1b3c49; font-size: 16px; }
.topic-card p { color: #657780; line-height: 1.8; margin: 10px 0; }
.topic-card { color: #667980; line-height: 1.7; }
.submit-actions {
  margin-top: 22px;
  text-align: center;
}
@media (max-width: 800px) {
  .dates { grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .topic-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .cfp-card, .topics-section { padding: 24px 20px; }
  .dates { gap: 18px 10px; }
  .cfp-text { font-size: 14px; }
}
</style>
