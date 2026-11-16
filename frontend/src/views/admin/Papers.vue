<template>
  <div class="page-card">
    <div class="page-head">
      <div class="page-title">论文管理</div>
      <el-button type="primary" :disabled="selectedPaperIds.length === 0" @click="handlePublishPlans(selectedPaperIds)">
        发布所选评审方案（{{ selectedPaperIds.length }}）
      </el-button>
      <el-button type="success" @click="handleExport">
        <el-icon><Download /></el-icon> 导出 CSV
      </el-button>
    </div>

    <el-form inline>
      <el-form-item label="状态">
        <el-select v-model="query.status" clearable placeholder="全部" style="width: 150px">
          <el-option v-for="(text, key) in paperStatusText" :key="key" :label="text" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="标题/投稿人" clearable style="width: 200px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="48" :selectable="canPublishPlan" />
      <el-table-column prop="id" label="编号" width="70" />
      <el-table-column label="论文标题" min-width="220">
        <template #default="{ row }"><el-link type="primary" @click="openDetail(row.id)">{{ row.title }}</el-link></template>
      </el-table-column>
      <el-table-column prop="submitterName" label="投稿人" width="90" />
      <el-table-column prop="topicName" label="专题" width="150" />
      <el-table-column label="状态" width="110">
        <template #default="{ row }">
          <el-tag :type="paperStatusTag[displayStatus(row)]">{{ paperStatusText[displayStatus(row)] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评审方案" width="100">
        <template #default="{ row }">
          <span v-if="row.status !== 'REVIEWING'">—</span>
          <el-tag v-else :type="row.reviewPlanPublished ? 'success' : 'info'" size="small">
            {{ row.reviewPlanPublished ? '已发布' : '待发布' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="评审" width="120">
        <template #default="{ row }">
          {{ row.submittedCount || 0 }}/{{ row.reviewCount || 0 }} 已提交
        </template>
      </el-table-column>
      <el-table-column label="投稿时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="350" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row.id)">详情</el-button>
          <template v-if="row.status === 'SUBMITTED'">
            <el-button link type="success" @click="doAction(row, 'first-pass', `初审通过论文「${row.title}」并进入外审?`)">初审通过</el-button>
            <el-button link type="danger" @click="doAction(row, 'reject', `确定退稿「${row.title}」?`)">退稿</el-button>
          </template>
          <template v-if="row.status === 'REVIEWING'">
            <el-button v-if="!row.reviewPlanPublished" link type="primary" @click="openAssign(row)">调整专家</el-button>
            <el-button v-if="!row.reviewPlanPublished" link type="warning" @click="doAutoAssign(row)">自动分配</el-button>
            <el-button v-if="!row.reviewPlanPublished" link type="success" @click="handlePublishPlans([row.id])">发布方案</el-button>
            <el-button link @click="doAiPrecheck(row)">AI预审</el-button>
            <el-button link type="danger" :disabled="!reviewReturned(row)" @click="doDecide(row, 'REJECTED')">终审退稿</el-button>
            <el-button link type="success" :disabled="!reviewReturned(row)" @click="doDecide(row, 'ACCEPTED')">录用</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, sizes, prev, pager, next" :total="total"
                   v-model:current-page="query.page" v-model:page-size="query.size"
                   :page-sizes="[10, 20, 50]" @current-change="load" @size-change="load" />

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="论文详情" size="620px">
      <template v-if="detail.paper">
        <h3>{{ detail.paper.title }}</h3>
        <el-tag :type="paperStatusTag[detail.reviewReturned ? 'REVIEW_RETURNED' : detail.paper.status]">
          {{ paperStatusText[detail.reviewReturned ? 'REVIEW_RETURNED' : detail.paper.status] }}
        </el-tag>
        <el-descriptions :column="1" border style="margin-top: 12px">
          <el-descriptions-item label="专题">{{ detail.topicName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关键词">{{ detail.paper.keywords }}</el-descriptions-item>
          <el-descriptions-item label="摘要">
            <pre class="abstract">{{ detail.paper.paperAbstract }}</pre>
          </el-descriptions-item>
        </el-descriptions>
        <div class="sec">作者</div>
        <el-table :data="detail.authors" size="small" border>
          <el-table-column prop="sortNo" label="顺序" width="60" />
          <el-table-column prop="name" label="姓名" width="90" />
          <el-table-column prop="affiliation" label="单位" />
          <el-table-column prop="isCorresponding" label="通讯" width="60">
            <template #default="{ row }">{{ row.isCorresponding ? '是' : '否' }}</template>
          </el-table-column>
        </el-table>
        <div class="sec">附件</div>
        <el-space>
          <el-button v-if="detail.paper.hasMainFile" size="small" @click="downloadPaperFile(detail.paper.id, 'original')">
            投稿版本
          </el-button>
          <el-button v-if="detail.paper.hasFinalFile" size="small" type="success" @click="downloadPaperFile(detail.paper.id, 'final')">
            终稿
          </el-button>
        </el-space>
        <div class="sec">评审记录</div>
        <el-empty v-if="!detail.reviews || detail.reviews.length === 0" description="尚未分配评审" :image-size="60" />
        <el-card v-for="r in detail.reviews || []" :key="r.id" shadow="never" class="review-card">
          <b>{{ r.expertName }}</b>
          <el-tag size="small" style="margin-left: 8px" :type="r.status === 'SUBMITTED' ? 'success' : 'info'">
            {{ r.status === 'SUBMITTED' ? '已提交' : '待评审' }}
          </el-tag>
          <el-tag v-if="r.result" size="small" style="margin-left: 4px" :type="r.result === 'ACCEPT' ? 'success' : 'danger'">
            {{ r.result === 'ACCEPT' ? '建议录用' : '建议退稿' }}
          </el-tag>
          <el-tag v-if="r.recommendJournal" size="small" type="warning" style="margin-left: 4px">推荐期刊</el-tag>
          <div v-if="r.rubricJson" class="rubric-summary">
            <span v-for="item in rubricEntries(r.rubricJson)" :key="item.label">{{ item.label }}：{{ item.value }}</span>
            <b>推荐等级：{{ recommendationText(r.recommendationGrade) }}</b>
          </div>
          <p v-if="r.overallComment" class="rc"><b>总评:</b>{{ r.overallComment }}</p>
          <p v-if="r.revisionComment" class="rc"><b>修改意见:</b>{{ r.revisionComment }}</p>
        </el-card>
      </template>
    </el-drawer>

    <!-- 分配专家 -->
    <el-dialog v-model="assignVisible" title="分配评审专家" width="560px">
      <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
                :title="`论文关键词:${assignPaper.keywords || '无'}。请选至少两位专家；方案发布前可反复调整。`" />
      <el-checkbox-group v-model="selectedExperts">
        <div v-for="e in experts" :key="e.userId" class="expert-line">
          <el-checkbox :value="e.userId">
            {{ e.realName }}({{ e.title || '专家' }},{{ e.affiliation || '-' }})
            <span class="kw">方向:{{ e.researchKeywords || '未填写' }}</span>
          </el-checkbox>
        </div>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="assignVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="handleAssign">确认分配</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminPapers, getPaperDetail, firstPassPaper, rejectPaper,
  assignExperts, autoAssignExperts, decidePaper, getExpertsLite, downloadPaperFile, exportPapersCsv,
  startAiPrecheck, publishReviewPlans
} from '../../api'
import { formatDateTime, paperStatusText, paperStatusTag } from '../../utils/format'
import { rubricEntries, recommendationText } from '../../utils/reviewRubric'

const query = reactive({ status: '', keyword: '', page: 1, size: 10 })
const list = ref([])
const total = ref(0)
const selectedPaperIds = ref([])

function handleSelectionChange(rows) {
  selectedPaperIds.value = rows.map(r => r.id)
}

function reviewReturned(row) {
  return row.status === 'REVIEWING' && row.reviewPlanPublished
    && Number(row.reviewCount) > 0 && Number(row.submittedCount) === Number(row.reviewCount)
}

function displayStatus(row) {
  return reviewReturned(row) ? 'REVIEW_RETURNED' : row.status
}

function canPublishPlan(row) {
  return row.status === 'REVIEWING' && !row.reviewPlanPublished
}

const detailVisible = ref(false)
const detail = ref({})

const assignVisible = ref(false)
const assigning = ref(false)
const experts = ref([])
const selectedExperts = ref([])
const assignPaper = reactive({ id: null, keywords: '' })

async function load() {
  const data = await getAdminPapers({
    status: query.status || undefined,
    keyword: query.keyword || undefined,
    page: query.page,
    size: query.size
  })
  list.value = data.list || []
  total.value = Number(data.total || 0)
}

async function handleExport() {
  await exportPapersCsv({ status: query.status, keyword: query.keyword })
  ElMessage.success('已按当前筛选条件导出')
}

async function openDetail(id) {
  detail.value = await getPaperDetail(id)
  detailVisible.value = true
}

async function doAction(row, action, tip) {
  await ElMessageBox.confirm(tip, '提示', { type: 'warning' })
  if (action === 'first-pass') await firstPassPaper(row.id)
  else await rejectPaper(row.id)
  ElMessage.success('操作成功')
  await load()
}

async function doDecide(row, result) {
  await ElMessageBox.confirm(
    `确定对「${row.title}」做出【${result === 'ACCEPTED' ? '录用' : '退稿'}】的终审决定?`,
    '终审', { type: result === 'ACCEPTED' ? 'success' : 'warning' }
  )
  await decidePaper(row.id, result)
  ElMessage.success('终审完成')
  await load()
}

async function openAssign(row) {
  Object.assign(assignPaper, { id: row.id, keywords: row.keywords })
  experts.value = ((await getExpertsLite()) || []).filter(e => e.reviewEnabled !== false)
  const current = await getPaperDetail(row.id)
  selectedExperts.value = (current.reviews || []).filter(r => r.status === 'PENDING').map(r => r.expertId)
  assignVisible.value = true
}

async function handleAssign() {
  if (selectedExperts.value.length === 0) {
    ElMessage.warning('请至少选择一位专家')
    return
  }
  assigning.value = true
  try {
    await assignExperts(assignPaper.id, selectedExperts.value)
    ElMessage.success('评审方案已保存，请检查后发布')
    assignVisible.value = false
    await load()
  } finally {
    assigning.value = false
  }
}

async function doAutoAssign(row) {
  const matched = await autoAssignExperts(row.id)
  const names = (matched || []).map((e) => e.realName).join('、')
  if ((matched || []).length < 2) ElMessage.warning(`仅找到 ${(matched || []).length} 位匹配专家，请手动补足后发布：${names}`)
  else ElMessage.success(`评审方案已生成：${names}。请检查后发布。`)
  await load()
}

async function handlePublishPlans(ids) {
  await ElMessageBox.confirm(`发布所选 ${ids.length} 篇论文的评审方案？发布后专家即可看到任务。`, '发布评审方案', { type: 'warning' })
  const result = await publishReviewPlans(ids)
  const published = result?.publishedIds || []
  const insufficient = result?.insufficientIds || []
  if (published.length) ElMessage.success(`已发布 ${published.length} 篇论文的评审方案`)
  if (insufficient.length) ElMessage.warning(`论文 #${insufficient.join('、#')} 的专家不足两位，尚未发布；请调整专家`)
  await load()
}

async function doAiPrecheck(row) {
  await startAiPrecheck(row.id)
  ElMessage.success('AI 预审已开始生成(约 10-30 秒),稍后在论文详情中查看')
}

onMounted(load)
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
.sec {
  font-weight: 600;
  margin: 18px 0 8px;
}
.abstract {
  white-space: pre-wrap;
  font-family: inherit;
}
.review-card {
  margin-bottom: 8px;
  background: #fafbfc;
}
.rc {
  margin-top: 6px;
  color: #606266;
}
.rubric-summary { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 4px; margin-top: 8px; font-size: 13px; }
.expert-line {
  margin-bottom: 6px;
}
.kw {
  color: #909399;
  font-size: 13px;
}
</style>
