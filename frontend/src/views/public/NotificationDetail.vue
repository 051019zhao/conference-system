<template>
  <div class="page-card">
    <el-page-header @back="$router.back()" content="通知详情" style="margin-bottom: 16px" />
    <h2 class="title">{{ detail.title }}</h2>
    <div class="time">发布时间:{{ formatDateTime(detail.publishTime) }}</div>
    <div class="time">发布单位：{{ detail.publisher || '会议组委会' }}
      <template v-if="detail.expiresAt"> · 有效至：{{ formatDateTime(detail.expiresAt) }}</template>
    </div>
    <el-divider />
    <pre class="content">{{ detail.content }}</pre>
    <el-button v-if="detail.hasPdf" type="primary" @click="downloadNotificationPdf(detail.id, detail.title)">
      下载通知 PDF
    </el-button>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getNotificationDetail, downloadNotificationPdf } from '../../api'
import { formatDateTime } from '../../utils/format'

const route = useRoute()
const detail = ref({})

onMounted(async () => {
  detail.value = (await getNotificationDetail(route.params.id)) || {}
})
</script>

<style scoped>
.title {
  text-align: center;
}
.time {
  text-align: center;
  color: #909399;
  font-size: 13px;
  margin-top: 8px;
}
.content {
  white-space: pre-wrap;
  line-height: 1.9;
  font-family: inherit;
}
</style>
