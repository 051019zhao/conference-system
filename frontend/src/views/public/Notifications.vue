<template>
  <div>
    <div class="section-kicker">NEWS & UPDATES</div>
    <h1 class="page-title">通知公告</h1>
    <el-empty v-if="list.length === 0" description="暂无通知" />
    <div class="page-card">
      <router-link v-for="(item, index) in list" :key="item.id" class="notice-item"
           :to="'/notifications/' + item.id">
        <span class="notice-index">{{ String(index + 1).padStart(2, '0') }}</span>
        <span class="notice-title">{{ item.title }}</span>
        <span class="notice-time">{{ formatDateTime(item.publishTime) }}</span>
        <el-icon class="notice-arrow"><ArrowRight /></el-icon>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNotifications } from '../../api'
import { formatDateTime } from '../../utils/format'

const list = ref([])

onMounted(async () => {
  list.value = (await getNotifications()) || []
})
</script>

<style scoped>
.section-kicker { color: #a17f59; font-size: 11px; font-weight: 700; letter-spacing: .18em; margin-bottom: 9px; }
.notice-item {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr) auto 18px;
  align-items: center;
  gap: 14px;
  padding: 19px 4px;
  border-bottom: 1px solid #e8ecea;
  text-decoration: none;
}
.notice-item:last-child {
  border-bottom: none;
}
.notice-index { color: #b3926d; font: 700 15px Georgia, serif; }
.notice-title { color: #263e49; font-weight: 600; }
.notice-arrow { color: #9eb2b8; }
.notice-item:hover .notice-title, .notice-item:hover .notice-arrow { color: #9a7654; }
.notice-time {
  color: #89979c;
  font-size: 12px;
}
@media (max-width: 600px) {
  .notice-item { grid-template-columns: 30px minmax(0, 1fr) 18px; gap: 8px; }
  .notice-time { grid-column: 2; grid-row: 2; }
  .notice-arrow { grid-column: 3; grid-row: 1 / span 2; }
}
</style>
