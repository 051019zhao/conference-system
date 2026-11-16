<template>
  <div>
    <div class="page-title">{{ isSubmittedEdit ? '修改待初审论文' : isEdit ? '编辑草稿' : '在线投稿' }}</div>
    <el-alert v-if="conference.submitDeadline" type="warning" :closable="false" show-icon
              style="margin-bottom: 14px"
              :title="`投稿截止时间:${conference.submitDeadline}(截止前可保存草稿反复修改,正式提交后进入评审流程)`" />
    <div class="page-card">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="论文标题" prop="title">
          <el-input v-model="form.title" maxlength="200" show-word-limit placeholder="论文完整标题" />
        </el-form-item>
        <el-form-item label="英文标题">
          <el-input v-model="form.titleEn" maxlength="200" show-word-limit placeholder="English title（正式投稿必填）" />
        </el-form-item>
        <el-form-item label="所属专题" prop="topicId">
          <el-select v-model="form.topicId" placeholder="选择征文专题" style="width: 100%">
            <el-option v-for="t in topics" :key="t.id" :value="t.id" :label="t.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词" prop="keywords">
          <el-input v-model="form.keywords" placeholder="3–5 个中文关键词，用逗号分隔，用于匹配评审专家" />
        </el-form-item>
        <el-form-item label="英文关键词">
          <el-input v-model="form.keywordsEn" placeholder="3–5 English keywords, separated by commas" />
        </el-form-item>
        <el-form-item label="摘要" prop="abstractText">
          <el-input v-model="form.abstractText" type="textarea" :rows="6" maxlength="2000" show-word-limit
                    placeholder="论文摘要(中文或英文)" />
        </el-form-item>
        <el-form-item label="英文摘要">
          <el-input v-model="form.abstractEn" type="textarea" :rows="5" maxlength="2000" show-word-limit
                    placeholder="English abstract（正式投稿必填）" />
        </el-form-item>

        <el-form-item label="论文作者" required>
          <div class="authors">
            <div v-for="(a, index) in form.authors" :key="index" class="author-row">
              <span class="drag-handle" draggable="true" title="拖动调整作者顺序"
                    @dragstart="dragIndex = index" @dragover.prevent @drop.prevent="dropAuthor(index)">☷</span>
              <el-input v-model="a.name" placeholder="姓名" class="w-name" />
              <el-input v-model="a.pinyin" placeholder="拼音" class="w-name" />
              <el-input v-model="a.affiliation" placeholder="单位（多个用分号分隔）" class="w-aff" />
              <el-input v-model="a.postalCode" placeholder="邮编" class="w-name" />
              <el-input v-model="a.email" placeholder="邮箱（通讯作者必填）" class="w-mail" />
              <el-checkbox v-model="a.isCorresponding">通讯作者</el-checkbox>
              <el-button circle :disabled="index === 0" @click="move(index, -1)" title="上移">
                <el-icon><ArrowUp /></el-icon>
              </el-button>
              <el-button circle :disabled="index === form.authors.length - 1" @click="move(index, 1)" title="下移">
                <el-icon><ArrowDown /></el-icon>
              </el-button>
              <el-button circle type="danger" :disabled="form.authors.length === 1" @click="form.authors.splice(index, 1)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
            <el-button @click="form.authors.push({ name: '', pinyin: '', affiliation: '', postalCode: '', email: '', isCorresponding: false })">
              <el-icon><Plus /></el-icon> 添加作者
            </el-button>
            <div class="author-order">作者顺序即署名顺序，可拖动左侧手柄或使用上下箭头调整；至少一位通讯作者须填写邮箱。</div>
          </div>
        </el-form-item>

        <el-form-item label="论文主稿" required>
          <el-upload drag :auto-upload="false" :limit="1" accept=".pdf"
                     :on-change="(f) => (file = f.raw)" :on-remove="() => (file = null)">
            <el-icon :size="40"><UploadFilled /></el-icon>
            <div>{{ isEdit && hasOldFile ? '如需更换主稿请重新选择，否则保留原文件' : '拖拽 PDF 到此处，或点击选择' }}</div>
            <template #tip>
              <div class="el-upload__tip">
                只能上传 PDF 文件(内容与扩展名双重校验),且不超过 20MB
                <span v-if="isEdit && hasOldFile">(草稿已带附件)</span>
                <span v-else>(正式投稿必须上传;存草稿可先不上传)</span>
              </div>
            </template>
          </el-upload>
          <el-button v-if="file" link type="primary" @click="previewLocalPdf">预览所选 PDF</el-button>
        </el-form-item>

        <el-form-item label="证明材料">
          <div class="supplements">
            <el-upload v-model:file-list="supplementFiles" :auto-upload="false" multiple :limit="3"
                       accept=".zip,.rar,.7z,.doc,.docx,.pdf,.png,.jpg,.jpeg"
                       :on-change="validateSupplement" :on-exceed="() => ElMessage.warning('每次最多选择 3 份证明材料')">
              <el-button>选择证明材料</el-button>
              <template #tip>
                <div class="el-upload__tip">可选 ZIP、RAR、7z、Word、PDF 或图片；每份不超过 10MB，每次最多 3 份。</div>
              </template>
            </el-upload>
            <div v-if="existingSupplements.length" class="existing-files">
              已保存：{{ existingSupplements.map(f => f.fileName).join('、') }}（本次新选文件会追加）
            </div>
          </div>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit(false)">
            {{ isSubmittedEdit ? '保存修改' : isEdit ? '保存并正式投稿' : '正式投稿' }}
          </el-button>
          <el-button v-if="!isSubmittedEdit" size="large" :loading="savingDraft" @click="handleSubmit(true)">
            {{ isEdit ? '仅保存草稿' : '保存草稿' }}
          </el-button>
          <el-button size="large" @click="$router.back()">返回</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-dialog v-model="previewVisible" title="主稿预览" width="80%" @closed="closePreview">
      <iframe v-if="previewUrl" :src="previewUrl" class="pdf-preview" title="主稿预览" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTopics, getConference, submitPaper, updateDraftApi, submitDraftApi, getPaperDetail } from '../../api'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const topics = ref([])
const conference = ref({})
const file = ref(null)
const previewVisible = ref(false)
const previewUrl = ref('')

function previewLocalPdf() {
  closePreview()
  if (!file.value) return
  previewUrl.value = URL.createObjectURL(file.value)
  previewVisible.value = true
}

function closePreview() {
  if (previewUrl.value) URL.revokeObjectURL(previewUrl.value)
  previewUrl.value = ''
}

onBeforeUnmount(closePreview)
const dragIndex = ref(null)
const supplementFiles = ref([])
const existingSupplements = ref([])
const submitting = ref(false)
const savingDraft = ref(false)

// 编辑模式:URL 带 paperId(草稿或待初审论文可进入编辑)
const editId = computed(() => route.query.paperId ? Number(route.query.paperId) : null)
const isEdit = computed(() => !!editId.value)
const editStatus = ref('')
const isSubmittedEdit = computed(() => editStatus.value === 'SUBMITTED')
const hasOldFile = ref(false)

const form = reactive({
  title: '',
  titleEn: '',
  topicId: null,
  keywords: '',
  keywordsEn: '',
  abstractText: '',
  abstractEn: '',
  authors: [{ name: '', pinyin: '', affiliation: '', postalCode: '', email: '', isCorresponding: true }]
})

const rules = {
  title: [{ required: true, message: '请输入论文标题', trigger: 'blur' }],
  topicId: [{ required: true, message: '请选择专题', trigger: 'change' }],
  keywords: [{ required: true, message: '请填写关键词', trigger: 'blur' }],
  abstractText: [{ required: true, message: '请输入摘要', trigger: 'blur' }]
}

function move(index, delta) {
  const arr = form.authors
  const target = index + delta
  ;[arr[index], arr[target]] = [arr[target], arr[index]]
}

function dropAuthor(index) {
  if (dragIndex.value === null || dragIndex.value === index) return
  const [author] = form.authors.splice(dragIndex.value, 1)
  form.authors.splice(index, 0, author)
  dragIndex.value = null
}

function keywordCount(value) {
  return (value || '').split(/[,，]/).map(s => s.trim()).filter(Boolean).length
}

function validateSupplement(uploadFile) {
  const raw = uploadFile.raw
  if (!raw || !/\.(zip|rar|7z|docx?|pdf|png|jpe?g)$/i.test(raw.name) || raw.size > 10 * 1024 * 1024) {
    supplementFiles.value = supplementFiles.value.filter(f => f.uid !== uploadFile.uid)
    ElMessage.warning('证明材料须为 ZIP、RAR、7z、Word、PDF 或图片，且每份不超过 10MB')
  }
}

function buildFormData() {
  const fd = new FormData()
  if (file.value) {
    fd.append('file', file.value)
  }
  supplementFiles.value.forEach(f => {
    if (f.raw) fd.append('supplements', f.raw)
  })
  fd.append('paper', new Blob([JSON.stringify({
    title: form.title,
    titleEn: form.titleEn,
    topicId: form.topicId,
    keywords: form.keywords,
    keywordsEn: form.keywordsEn,
    abstractText: form.abstractText,
    abstractEn: form.abstractEn,
    authors: form.authors
  })], { type: 'application/json' }))
  return fd
}

/** asDraft=true 存草稿(附件可选);false 正式投稿 */
async function handleSubmit(asDraft) {
  await formRef.value.validate()
  if (form.authors.some((a) => !a.name || !a.name.trim())) {
    ElMessage.warning('请完整填写每位作者的姓名')
    return
  }
  if (!asDraft) {
    if (!form.titleEn.trim() || !form.abstractEn.trim()) {
      ElMessage.warning('正式投稿需填写英文标题和英文摘要')
      return
    }
    if (![form.keywords, form.keywordsEn].every(v => keywordCount(v) >= 3 && keywordCount(v) <= 5)) {
      ElMessage.warning('中英文关键词均需填写 3–5 个')
      return
    }
    if (!form.authors.some(a => a.isCorresponding && a.email?.trim())) {
      ElMessage.warning('请至少指定一位填写邮箱的通讯作者')
      return
    }
    const hasFile = !!file.value || (isEdit.value && hasOldFile.value)
    if (!hasFile) {
      ElMessage.warning('正式投稿必须上传论文 PDF 附件(可先存草稿)')
      return
    }
  }
  const fd = buildFormData()
  try {
    if (asDraft) {
      savingDraft.value = true
      if (isEdit.value) {
        await updateDraftApi(editId.value, fd)
      } else {
        await submitPaper(fd, true)
      }
      ElMessage.success('草稿已保存,可随时回来修改')
      router.push('/author/papers')
    } else {
      submitting.value = true
      if (isSubmittedEdit.value) {
        await updateDraftApi(editId.value, fd)
      } else if (isEdit.value) {
        await updateDraftApi(editId.value, fd)
        await submitDraftApi(editId.value)
      } else {
        await submitPaper(fd, false)
      }
      ElMessage.success(isSubmittedEdit.value ? '修改已保存，论文仍处于待初审状态' : '投稿成功,请等待初审')
      router.push('/author/papers')
    }
  } finally {
    submitting.value = false
    savingDraft.value = false
  }
}

onMounted(async () => {
  topics.value = (await getTopics()) || []
  conference.value = (await getConference()) || {}
  if (editId.value) {
    const data = await getPaperDetail(editId.value)
    const p = data.paper || {}
    if (!['DRAFT', 'SUBMITTED'].includes(p.status)) {
      ElMessage.warning('仅草稿或待初审论文可编辑')
      router.replace('/author/papers')
      return
    }
    editStatus.value = p.status
    form.title = p.title
    form.titleEn = p.titleEn || ''
    form.topicId = p.topicId
    form.keywords = p.keywords
    form.keywordsEn = p.keywordsEn || ''
    form.abstractText = p.paperAbstract
    form.abstractEn = p.abstractEn || ''
    hasOldFile.value = !!p.hasMainFile
    existingSupplements.value = (data.files || []).filter(f => f.fileType === 'SUPPLEMENT')
    const authors = (data.authors || []).map((a) => ({
      name: a.name,
      pinyin: a.pinyin || '',
      affiliation: a.affiliation || '',
      postalCode: a.postalCode || '',
      email: a.email || '',
      isCorresponding: !!a.isCorresponding
    }))
    if (authors.length) form.authors = authors
  }
})
</script>

<style scoped>
.authors {
  width: 100%;
}
.author-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  flex-wrap: wrap;
}
.w-name {
  width: 140px;
}
.w-aff {
  width: 200px;
}
.w-mail {
  width: 200px;
}
.author-order {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.drag-handle { cursor: grab; user-select: none; font-size: 22px; color: #909399; }
.supplements { width: 100%; }
.existing-files { color: #606266; font-size: 13px; overflow-wrap: anywhere; }
.pdf-preview { width: 100%; height: 70vh; border: 0; }
</style>
