<template>
  <div v-if="loaded">
    <el-page-header @back="$router.back()" content="论文详情" style="margin-bottom: 16px" />
    <div class="page-card">
      <div class="head">
        <h2>{{ paper.title }}</h2>
        <el-tag :type="paperStatusTag[reviewReturned ? 'REVIEW_RETURNED' : paper.status]" size="large">
          {{ paperStatusText[reviewReturned ? 'REVIEW_RETURNED' : paper.status] }}
        </el-tag>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="编号">{{ paper.id }}</el-descriptions-item>
        <el-descriptions-item label="专题">{{ topicName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="关键词">{{ paper.keywords }}</el-descriptions-item>
        <el-descriptions-item label="英文标题" :span="2">{{ paper.titleEn || '-' }}</el-descriptions-item>
        <el-descriptions-item label="英文关键词" :span="2">{{ paper.keywordsEn || '-' }}</el-descriptions-item>
        <el-descriptions-item label="投稿时间">{{ formatDateTime(paper.createdAt) }}</el-descriptions-item>
        <el-descriptions-item label="摘要" :span="2">
          <pre class="abstract">{{ paper.paperAbstract }}</pre>
        </el-descriptions-item>
        <el-descriptions-item label="英文摘要" :span="2"><pre class="abstract">{{ paper.abstractEn || '-' }}</pre></el-descriptions-item>
      </el-descriptions>

      <div class="section-title">作者列表(按署名顺序)</div>
      <el-table :data="authors" border size="small">
        <el-table-column prop="sortNo" label="顺序" width="70" />
        <el-table-column prop="name" label="姓名" width="120" />
        <el-table-column prop="pinyin" label="拼音" width="130" />
        <el-table-column prop="affiliation" label="单位" min-width="180" />
        <el-table-column prop="postalCode" label="邮编" width="100" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column label="通讯作者" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.isCorresponding" type="success" size="small">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="section-title">论文附件(版本记录)</div>
      <el-table :data="files" size="small" border>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.fileType === 'MAIN' ? 'primary' : row.fileType === 'FINAL' ? 'success' : 'warning'">
              {{ fileTypeText[row.fileType] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column label="版本" width="70">
          <template #default="{ row }">v{{ row.versionNo }} {{ row.isCurrent ? '(当前)' : '' }}</template>
        </el-table-column>
        <el-table-column label="大小" width="90">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="上传时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button v-if="row.fileExt === 'pdf'" link type="primary" @click="previewFile(row)">预览</el-button>
            <el-button link type="primary" @click="downloadFileById(row.id)">下载</el-button>
            <el-popconfirm v-if="canManageSupplement && row.fileType === 'SUPPLEMENT'" title="确定删除该附属资料?"
                           @confirm="handleDeleteSupplement(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="canManageSupplement" class="supplement-upload">
        <el-upload :auto-upload="false" :show-file-list="false"
                   accept=".zip,.rar,.7z,.doc,.docx,.pdf,.png,.jpg,.jpeg"
                   :on-change="(f) => handleUploadSupplement(f.raw)">
          <el-button size="small" type="warning" :loading="uploadingSup">
            <el-icon><Upload /></el-icon> 上传附属资料(压缩包/Word/PDF/图片,≤10MB)
          </el-button>
        </el-upload>
      </div>

      <template v-if="reviews.length > 0">
        <div class="section-title">评审意见</div>
        <el-card v-for="r in reviews" :key="r.id" class="review-card" shadow="never">
          <div class="review-head">
            <b>评审专家{{ indexPlaceholder(r) }}</b>
            <el-tag v-if="r.status === 'SUBMITTED'" size="small"
                    :type="r.result === 'ACCEPT' ? 'success' : 'danger'">
              {{ r.result === 'ACCEPT' ? '建议录用' : '建议退稿' }}
            </el-tag>
            <el-tag v-else size="small" type="info">待提交</el-tag>
            <el-tag v-if="r.recommendJournal" size="small" type="warning">推荐到期刊</el-tag>
          </div>
          <template v-if="r.status === 'SUBMITTED'">
            <div class="score-line" v-if="r.scoreFit">
              <span v-for="dim in scoreDimensions" :key="dim.key" class="score-item">
                {{ dim.label }} <el-rate :model-value="r[dim.key]" disabled :max="5" size="small" />
              </span>
            </div>
            <div v-if="r.rubricJson" class="rubric-summary">
              <div v-for="item in rubricEntries(r.rubricJson)" :key="item.label">{{ item.label }}：{{ item.value }}</div>
              <b>推荐等级：{{ recommendationText(r.recommendationGrade) }}</b>
            </div>
            <p><b>总体评价:</b>{{ r.overallComment }}</p>
            <p v-if="r.revisionComment"><b>修改意见:</b>{{ r.revisionComment }}</p>
          </template>
        </el-card>
      </template>
      <!-- AI 预审报告:仅管理员与评审专家可见,对作者保密(内部参考) -->
      <template v-if="canSeeAiPrecheck">
        <div class="section-title">AI 预审报告(内部参考)</div>
        <el-card v-if="aiPrecheck" shadow="never" class="review-card">
          <template #header>
            <div class="ai-head">
              <span>{{ aiPrecheck.model }} · {{ aiStatusText[aiPrecheck.status] || aiPrecheck.status }}</span>
                <el-button size="small" text :loading="aiLoading" @click="loadAiPrecheck">刷新</el-button>
            </div>
          </template>
          <pre v-if="aiPrecheck.status === 'READY'" class="ai-content">{{ aiPrecheck.content }}</pre>
          <el-alert v-else-if="aiPrecheck.status === 'FAILED'" type="error" :closable="false"
                    :title="'生成失败:' + (aiPrecheck.error || '未知错误')" />
          <div v-else class="muted">AI 正在生成预审报告(约 10-30 秒),请稍后点"刷新"查看。</div>
          <div class="ai-disclaimer">本报告由 AI 自动生成,仅供参考,不构成评审结论;最终结论以人工评审为准。</div>
        </el-card>
        <el-alert v-else type="info" :closable="false"
                  title="尚未生成 AI 预审(管理员可在论文管理中触发,供评审参考)" />
      </template>
    </div>
    <el-dialog v-model="previewVisible" title="论文 PDF 预览" width="80%" @closed="closePreview">
      <iframe v-if="previewUrl" :src="previewUrl" class="pdf-preview" title="论文 PDF 预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPaperDetail, addSupplementApi, deleteSupplementApi, downloadFileById, getPaperFileBlob, getAiPrecheck } from '../../api'
import { formatDateTime, formatSize, paperStatusText, paperStatusTag, scoreDimensions } from '../../utils/format'
import { rubricEntries, recommendationText } from '../../utils/reviewRubric'
import { useUserStore } from '../../stores/user'

const route = useRoute()
const store = useUserStore()
const loaded = ref(false)
const paper = ref({})
const reviewReturned = ref(false)
const authors = ref([])
const reviews = ref([])
const files = ref([])
const topicName = ref('')
const uploadingSup = ref(false)
const previewVisible = ref(false)
const previewUrl = ref('')

async function previewFile(row) {
  closePreview()
  const blob = await getPaperFileBlob(row.id)
  previewUrl.value = URL.createObjectURL(new Blob([blob], { type: 'application/pdf' }))
  previewVisible.value = true
}

function closePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

const fileTypeText = { MAIN: '投稿主稿', FINAL: '终稿', SUPPLEMENT: '附属资料' }

// AI 预审可见性:管理员 或 评审专家(且不是论文作者本人)
const canSeeAiPrecheck = computed(() => {
  const isOwner = paper.value.submitterId === store.user?.id
  return (store.role === 'ADMIN' || store.role === 'EXPERT') && !isOwner
})
const aiPrecheck = ref(null)
const aiLoading = ref(false)
const aiStatusText = { PENDING: '排队中', RUNNING: '生成中', READY: '已完成', FAILED: '失败' }

async function loadAiPrecheck() {
  if (!canSeeAiPrecheck.value) return
  aiLoading.value = true
  try {
    aiPrecheck.value = await getAiPrecheck(paper.value.id)
  } catch (e) {
    aiPrecheck.value = null
  } finally {
    aiLoading.value = false
  }
}

// 附属资料管理权限:本人 + 草稿/待初审状态
const canManageSupplement = computed(() =>
  store.role === 'AUTHOR' &&
  paper.value.submitterId === store.user?.id &&
  ['DRAFT', 'SUBMITTED'].includes(paper.value.status)
)

async function reload() {
  const data = await getPaperDetail(route.params.id)
  paper.value = data.paper || {}
  reviewReturned.value = !!data.reviewReturned
  authors.value = data.authors || []
  reviews.value = data.reviews || []
  files.value = data.files || []
  topicName.value = data.topicName || ''
  loaded.value = true
  loadAiPrecheck()
}

async function handleUploadSupplement(file) {
  if (!file) return
  uploadingSup.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    await addSupplementApi(paper.value.id, fd)
    ElMessage.success('附属资料已上传')
    await reload()
  } finally {
    uploadingSup.value = false
  }
}

async function handleDeleteSupplement(row) {
  await deleteSupplementApi(row.id)
  ElMessage.success('已删除')
  await reload()
}

// 对作者匿名展示专家身份
function indexPlaceholder(r) {
  if (store.role === 'AUTHOR') return ` ${reviews.value.indexOf(r) + 1}`
  return `:${r.expertName}`
}

onMounted(reload)
</script>

<style scoped>
.head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.abstract {
  white-space: pre-wrap;
  font-family: inherit;
  line-height: 1.8;
}
.section-title {
  font-weight: 600;
  margin: 20px 0 10px;
}
.review-card {
  margin-bottom: 10px;
  background: #fafbfc;
}
.review-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.score-line {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 16px;
  margin-bottom: 8px;
  padding: 8px 10px;
  background: #f5f7fa;
  border-radius: 6px;
}
.score-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
}
.rubric-summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 6px; margin: 8px 0; }
.muted {
  color: #909399;
}
.supplement-upload {
  margin-top: 10px;
}
.pdf-preview { width: 100%; height: 70vh; border: 0; }
.ai-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
  color: #606266;
}
.ai-content {
  white-space: pre-wrap;
  font-family: inherit;
  line-height: 1.8;
  font-size: 14px;
}
.ai-disclaimer {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed #ebeef5;
  color: #909399;
  font-size: 12px;
}
</style>
