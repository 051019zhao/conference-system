<template>
  <div>
    <div class="page-title">录用论文</div>
    <div class="page-card">
      <el-alert type="success" :closable="false" show-icon style="margin-bottom: 14px"
                title="以下论文已通过评审并被本会议录用,按投稿编号倒序排列" />
      <el-empty v-if="list.length === 0" description="暂无录用论文,评审结束后将在此公示" />
      <el-table v-else :data="list" border>
        <el-table-column prop="id" label="编号" width="70" />
        <el-table-column prop="title" label="论文标题" min-width="280" />
        <el-table-column prop="authors" label="作者" min-width="180" />
        <el-table-column prop="topicName" label="专题" width="170" />
        <el-table-column prop="keywords" label="关键词" min-width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="paperStatusTag[row.status]" size="small">
              {{ paperStatusText[row.status] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="论文文件" width="130">
          <template #default="{ row }">
            <el-button link type="primary" :loading="downloadingId === row.id" @click="handleDownload(row)">
              下载 PDF
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAcceptedPapers, downloadAcceptedPaper } from '../../api'
import { paperStatusText, paperStatusTag } from '../../utils/format'

const list = ref([])
const downloadingId = ref(null)

async function handleDownload(row) {
  downloadingId.value = row.id
  try {
    await downloadAcceptedPaper(row.id, row.title)
  } finally {
    downloadingId.value = null
  }
}

onMounted(async () => {
  list.value = (await getAcceptedPapers()) || []
})
</script>
