<template>
  <div>
    <div class="page-title">通知公告</div>
    <el-empty v-if="list.length === 0" description="暂无通知" />
    <div class="page-card">
      <div v-for="item in list" :key="item.id" class="notice-item"
           @click="$router.push('/notifications/' + item.id)">
        <span class="notice-title">{{ item.title }}</span>
        <span class="notice-time">{{ formatDateTime(item.publishTime) }}</span>
      </div>
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
.notice-item {
  display: flex;
  justify-content: space-between;
  padding: 14px 4px;
  border-bottom: 1px dashed #ebeef5;
  cursor: pointer;
}
.notice-item:last-child {
  border-bottom: none;
}
.notice-item:hover .notice-title {
  color: #409eff;
}
.notice-time {
  color: #909399;
  font-size: 13px;
}
</style>
