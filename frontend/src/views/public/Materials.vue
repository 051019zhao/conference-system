<template>
  <div>
    <div class="section-kicker">DOWNLOADS & RESOURCES</div>
    <component :is="embedded ? 'h2' : 'h1'" class="page-title">资料下载</component>
    <div class="page-card">
      <el-empty v-if="list.length === 0" description="暂无资料" />
      <el-table v-else :data="list" border>
        <el-table-column prop="name" label="资料名称" min-width="220" />
        <el-table-column prop="description" label="说明" min-width="200" />
        <el-table-column label="大小" width="110">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="downloadCount" label="下载量" width="90" />
        <el-table-column label="操作" width="110">
          <template #default="{ row }">
            <el-button type="primary" link @click="handleDownload(row)">
              <el-icon><Download /></el-icon> 下载
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMaterials, downloadMaterial } from '../../api'
import { formatSize } from '../../utils/format'

defineProps({ embedded: { type: Boolean, default: false } })

const list = ref([])

async function handleDownload(row) {
  await downloadMaterial(row.id)
  row.downloadCount += 1
}

onMounted(async () => {
  list.value = (await getMaterials()) || []
})
</script>

<style scoped>
.section-kicker { color: #a17f59; font-size: 11px; font-weight: 700; letter-spacing: .18em; margin-bottom: 9px; }
</style>
