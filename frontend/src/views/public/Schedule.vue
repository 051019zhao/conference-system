<template>
  <div>
    <div class="page-title">会议日程</div>
    <el-empty v-if="groups.length === 0" description="日程暂未发布" />
    <div v-for="group in groups" :key="group.day" class="page-card day-card">
      <div class="day-title">{{ group.day }}({{ group.week }})</div>
      <el-table :data="group.items" border>
        <el-table-column prop="startTime" label="开始" width="90" />
        <el-table-column prop="endTime" label="结束" width="90" />
        <el-table-column prop="title" label="内容" min-width="240" />
        <el-table-column prop="speaker" label="主讲/负责" width="140" />
        <el-table-column prop="location" label="地点" width="150" />
        <el-table-column prop="type" label="类型" width="110">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getSchedule } from '../../api'

const items = ref([])

const groups = computed(() => {
  const map = new Map()
  for (const it of items.value) {
    const day = String(it.day).slice(0, 10)
    if (!map.has(day)) map.set(day, [])
    map.get(day).push(it)
  }
  const weekNames = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']
  return [...map.entries()].map(([day, list]) => ({
    day,
    week: weekNames[new Date(day.replace(/-/g, '/')).getDay()],
    items: list
  }))
})

function typeTag(type) {
  if (type === '主旨报告') return 'warning'
  if (type === '开幕式' || type === '闭幕式') return 'success'
  return 'info'
}

onMounted(async () => {
  items.value = (await getSchedule()) || []
})
</script>

<style scoped>
.day-card {
  margin-bottom: 16px;
}
.day-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 12px;
}
</style>
