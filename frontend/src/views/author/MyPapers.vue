<template>
  <div>
    <div class="page-head">
      <div class="page-title">我的投稿</div>
      <el-button @click="openTrash">回收站</el-button>
      <el-button type="primary" @click="$router.push('/author/submit')">
        <el-icon><Plus /></el-icon> 在线投稿
      </el-button>
    </div>
    <div v-if="acceptedPapers.length" class="accept-notices page-card">
      <el-alert type="success" show-icon :closable="false"
                :title="`录用通知：您有 ${acceptedPapers.length} 篇论文已被录用`"
                description="可查看评审意见；尚未提交终稿的论文请按会议要求上传终稿。" />
      <div v-for="row in acceptedPapers" :key="row.id" class="accept-notice">
        <span>《{{ row.title }}》已录用</span>
        <el-button link type="primary" @click="$router.push('/paper/' + row.id)">查看详情</el-button>
      </div>
    </div>
    <div class="page-card">
      <el-empty v-if="list.length === 0" description="还没有投稿,点击右上角「在线投稿」提交第一篇论文" />
      <el-table v-else :data="list" border>
        <el-table-column prop="id" label="编号" width="70" />
        <el-table-column label="论文标题" min-width="220">
          <template #default="{ row }">
            <el-link type="primary" @click="$router.push('/paper/' + row.id)">{{ row.title }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="authors" label="作者" min-width="150" />
        <el-table-column prop="topicName" label="专题" width="160" />
        <el-table-column prop="keywords" label="关键词" min-width="160" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="paperStatusTag[displayStatus(row)]">{{ paperStatusText[displayStatus(row)] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="投稿时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="390" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/paper/' + row.id)">查看</el-button>
            <el-button v-if="row.hasMainFile" link type="primary" @click="downloadPaperFile(row.id, 'original')">下载主稿</el-button>
            <el-button v-if="['DRAFT','SUBMITTED'].includes(row.status)" link type="primary"
                       @click="$router.push('/author/submit?paperId=' + row.id)">编辑</el-button>
            <el-popconfirm v-if="row.status === 'DRAFT'" title="确定将草稿正式提交评审?"
                           confirm-button-text="提交" @confirm="handleSubmitDraft(row)">
              <template #reference>
                <el-button link type="success">提交评审</el-button>
              </template>
            </el-popconfirm>
            <el-button v-if="row.status === 'ACCEPTED'" link type="success" @click="openFinal(row)">
              上传终稿
            </el-button>
            <el-popconfirm v-if="row.status === 'SUBMITTED'" title="撤回后可恢复为草稿，确定撤回?"
                           confirm-button-text="撤回" @confirm="handleWithdraw(row)">
              <template #reference>
                <el-button link type="danger">撤回</el-button>
              </template>
            </el-popconfirm>
            <el-button v-if="row.status === 'WITHDRAWN'" link type="success" @click="handleReopen(row)">恢复为草稿</el-button>
            <el-button v-if="['DRAFT','WITHDRAWN'].includes(row.status)" link type="danger" @click="handleTrash(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="trashVisible" title="论文回收站" width="700px">
      <el-alert type="info" :closable="false" title="删除仅将草稿或已撤回论文移入回收站；文件与历史记录保留，可随时恢复。" />
      <el-table :data="trash" border style="margin-top: 12px">
        <el-table-column prop="id" label="编号" width="80" />
        <el-table-column prop="title" label="论文标题" min-width="240" />
        <el-table-column label="删除时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.deletedAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }"><el-button link type="primary" @click="handleRestore(row)">恢复</el-button></template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!trash.length" description="回收站为空" />
    </el-dialog>

    <el-dialog v-model="finalVisible" title="上传终稿" width="440px">
      <el-upload drag :auto-upload="false" :limit="1" :on-change="(f) => (finalFile = f.raw)"
                 accept=".pdf">
        <el-icon :size="40"><UploadFilled /></el-icon>
        <div>拖拽文件到此处,或点击选择(仅 PDF,≤20MB)</div>
      </el-upload>
      <template #footer>
        <el-button @click="finalVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleFinal">提交终稿</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyPapers, getMyPaperTrash, movePaperToTrash, restorePaperFromTrash,
  reopenWithdrawnPaper, downloadPaperFile, submitFinal, withdrawPaper, submitDraftApi } from '../../api'
import { formatDateTime, paperStatusText, paperStatusTag } from '../../utils/format'

const list = ref([])
const router = useRouter()
const trash = ref([])
const trashVisible = ref(false)
function displayStatus(row) {
  return row.status === 'REVIEWING' && row.reviewPlanPublished
    && Number(row.reviewCount) > 0 && Number(row.reviewCount) === Number(row.submittedCount)
    ? 'REVIEW_RETURNED' : row.status
}
async function openTrash() {
  trash.value = (await getMyPaperTrash()) || []
  trashVisible.value = true
}
async function handleTrash(row) {
  await ElMessageBox.confirm(`将《${row.title}》移入回收站？之后可以恢复。`, '删除论文', { type: 'warning' })
  await movePaperToTrash(row.id)
  ElMessage.success('已移入回收站')
  list.value = await getMyPapers()
}
async function handleRestore(row) {
  await restorePaperFromTrash(row.id)
  ElMessage.success('已恢复论文')
  trash.value = await getMyPaperTrash()
  list.value = await getMyPapers()
}
async function handleReopen(row) {
  await reopenWithdrawnPaper(row.id)
  ElMessage.success('已恢复为草稿，可以继续修改')
  router.push('/author/submit?paperId=' + row.id)
}
const acceptedPapers = computed(() => list.value.filter(row =>
  row.status === 'ACCEPTED' || row.status === 'FINAL_SUBMITTED'))
const finalVisible = ref(false)
const uploading = ref(false)
const finalFile = ref(null)
const currentRow = ref(null)

function openFinal(row) {
  currentRow.value = row
  finalFile.value = null
  finalVisible.value = true
}

async function handleWithdraw(row) {
  await withdrawPaper(row.id)
  ElMessage.success('论文已撤回')
  list.value = await getMyPapers()
}

async function handleSubmitDraft(row) {
  await submitDraftApi(row.id)
  ElMessage.success('草稿已正式提交,等待初审')
  list.value = await getMyPapers()
}

async function handleFinal() {
  if (!finalFile.value) {
    ElMessage.warning('请选择终稿文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', finalFile.value)
    await submitFinal(currentRow.value.id, fd)
    ElMessage.success('终稿提交成功')
    finalVisible.value = false
    list.value = await getMyPapers()
  } finally {
    uploading.value = false
  }
}

onMounted(async () => {
  list.value = (await getMyPapers()) || []
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.accept-notices { margin-bottom: 16px; }
.accept-notice { display: flex; align-items: center; justify-content: space-between; padding: 8px 12px 0; }
</style>
