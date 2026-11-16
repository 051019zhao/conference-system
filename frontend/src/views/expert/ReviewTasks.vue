<template>
  <div>
    <div class="page-title">我的评审任务</div>
    <div class="page-card">
      <el-empty v-if="list.length === 0" description="暂无评审任务" />
      <el-table v-else :data="list" border>
        <el-table-column prop="paperId" label="论文编号" width="90" />
        <el-table-column prop="title" label="论文标题" min-width="240" />
        <el-table-column prop="keywords" label="关键词" min-width="160" />
        <el-table-column label="论文状态" width="110">
          <template #default="{ row }">
            <el-tag :type="paperStatusTag[row.paperStatus]" size="small">
              {{ paperStatusText[row.paperStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="我的评审" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.myStatus === 'SUBMITTED'" type="success" size="small">已提交</el-tag>
            <el-tag v-else type="warning" size="small">待评审</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="$router.push('/paper/' + row.paperId)">查看论文</el-button>
            <el-button link type="success" :disabled="row.myStatus === 'SUBMITTED' || row.paperStatus !== 'REVIEWING'"
                       @click="openReview(row)">填写评审</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="'评审:' + current.title" width="780px">
      <el-form ref="formRef" :model="reviewForm" :rules="rules" label-width="100px">
        <el-form-item v-for="field in rubricFields" :key="field.key" :label="field.label" required>
          <el-radio-group v-model="reviewForm.rubric[field.key]" class="rubric-options">
            <el-radio v-for="[value, text] in field.options" :key="value" :value="value">{{ text }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="总体评价" prop="overallComment">
          <el-input v-model="reviewForm.overallComment" type="textarea" :rows="4"
                    placeholder="对论文创新性、技术正确性、写作质量的总体评价" />
        </el-form-item>
        <el-form-item label="修改意见">
          <el-input v-model="reviewForm.revisionComment" type="textarea" :rows="4"
                    placeholder="给作者的修改建议(选填)" />
        </el-form-item>
        <el-form-item label="评审结论" prop="result">
          <el-radio-group v-model="reviewForm.result" @change="reviewForm.recommendationGrade = ''; reviewForm.rejectionReason = ''">
            <el-radio value="ACCEPT">建议录用</el-radio>
            <el-radio value="REJECT">建议退稿</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="reviewForm.result" label="推荐等级" required>
          <el-radio-group v-model="reviewForm.recommendationGrade" class="rubric-options">
            <el-radio v-for="[value, text] in recommendationOptions[reviewForm.result]" :key="value" :value="value">
              {{ text }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="reviewForm.result === 'REJECT'" label="退稿原因" required>
          <el-select v-model="reviewForm.rejectionReason" placeholder="请选择退稿原因" style="width: 100%">
            <el-option v-for="[value, text] in rejectionReasons" :key="value" :value="value" :label="text" />
          </el-select>
        </el-form-item>
        <el-form-item label="推荐期刊">
          <el-switch v-model="reviewForm.recommendJournal" active-text="推荐到期刊发表" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交评审</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getReviewTasks, submitReviewApi } from '../../api'
import { paperStatusText, paperStatusTag } from '../../utils/format'
import { rubricFields, recommendationOptions, rejectionReasons } from '../../utils/reviewRubric'

const list = ref([])
const dialogVisible = ref(false)
const submitting = ref(false)
const formRef = ref()
const current = ref({})
const reviewForm = reactive({
  overallComment: '',
  revisionComment: '',
  rubric: {},
  recommendationGrade: '',
  rejectionReason: '',
  result: '',
  recommendJournal: false
})

const rules = {
  overallComment: [{ required: true, message: '请填写总体评价', trigger: 'blur' }],
  result: [{ required: true, message: '请选择评审结论', trigger: 'change' }]
}

function openReview(row) {
  current.value = row
  reviewForm.overallComment = ''
  reviewForm.revisionComment = ''
  reviewForm.rubric = {}
  reviewForm.recommendationGrade = ''
  reviewForm.rejectionReason = ''
  reviewForm.result = ''
  reviewForm.recommendJournal = false
  dialogVisible.value = true
}

async function handleSubmit() {
  await formRef.value.validate()
  for (const field of rubricFields) {
    if (!reviewForm.rubric[field.key]) {
      ElMessage.warning(`请评价「${field.label}」`)
      return
    }
  }
  if (!reviewForm.recommendationGrade || (reviewForm.result === 'REJECT' && !reviewForm.rejectionReason)) {
    ElMessage.warning('请选择推荐等级及退稿原因')
    return
  }
  submitting.value = true
  try {
    await submitReviewApi(current.value.reviewId, { ...reviewForm })
    ElMessage.success('评审已提交')
    dialogVisible.value = false
    list.value = await getReviewTasks()
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  list.value = (await getReviewTasks()) || []
})
</script>

<style scoped>
.rubric-options { display: flex; flex-wrap: wrap; gap: 4px 12px; }
</style>
