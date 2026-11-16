<template>
  <div>
    <div class="page-card">
      <div class="page-title">征文通知(Call for Papers)</div>
      <pre class="cfp-text">{{ conference.cfpText || '征文内容暂未发布' }}</pre>
      <div class="dates">
        <el-tag>投稿截止：{{ conference.submitDeadline || '待公布' }}</el-tag>
        <el-tag type="success">录用通知：{{ conference.notifyDeadline || '待公布' }}</el-tag>
        <el-tag type="warning">终稿截止：{{ conference.finalDeadline || '待公布' }}</el-tag>
        <el-tag type="info">会议：{{ conference.startDate || '待公布' }}—{{ conference.endDate || '待公布' }}</el-tag>
      </div>
      <div class="conference-meta">地点：{{ conference.venue || '待公布' }} · 联系邮箱：{{ conference.contactEmail || '待公布' }}</div>
      <el-button link type="primary" @click="$router.push('/materials')">查看会议资料下载区</el-button>
    </div>
    <div class="page-card" style="margin-top: 16px">
      <div class="page-title">征文专题</div>
      <el-empty v-if="topics.length === 0" description="暂无专题" />
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
.cfp-text {
  white-space: pre-wrap;
  line-height: 1.9;
  font-family: inherit;
  color: #303133;
}
.dates { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 16px; }
.topic-card { margin-bottom: 12px; }
.topic-card p { color: #606266; }
.conference-meta { margin-top: 12px; color: #606266; }
.submit-actions {
  margin-top: 16px;
  text-align: center;
}
</style>
