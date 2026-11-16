<template>
  <div class="page-card">
    <div class="page-head">
      <div class="page-title">通知管理</div>
      <el-button type="primary" @click="openEdit(null)">
        <el-icon><Plus /></el-icon> 新建通知
      </el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="240" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.isPublished ? 'success' : 'info'">
            {{ row.isPublished ? '已发布' : '草稿' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
      </el-table-column>
      <el-table-column prop="publisher" label="发布单位" width="150" />
      <el-table-column label="有效至" width="170">
        <template #default="{ row }">{{ formatDateTime(row.expiresAt) }}</template>
      </el-table-column>
      <el-table-column label="PDF" width="65">
        <template #default="{ row }">{{ row.hasPdf ? '有' : '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button v-if="!row.isPublished" link type="success" @click="handlePublish(row)">发布</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑通知' : '新建通知'" width="640px">
      <el-form :model="form" label-width="70px">
        <el-form-item label="标题" required>
          <el-input v-model="form.title" maxlength="120" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" />
        </el-form-item>
        <el-form-item label="发布单位"><el-input v-model="form.publisher" placeholder="如：会议组委会" /></el-form-item>
        <el-form-item label="有效至">
          <el-date-picker v-model="form.expiresAt" type="datetime" clearable
                          value-format="YYYY-MM-DDTHH:mm:ss" placeholder="不填则长期有效" />
        </el-form-item>
        <el-form-item label="PDF 附件">
          <el-upload :auto-upload="false" :limit="1" accept=".pdf" :on-change="f => pdfFile = f.raw"
                     :on-remove="() => pdfFile = null">
            <el-button>选择 PDF</el-button>
            <template #tip><div class="el-upload__tip">{{ form.hasPdf ? '已有 PDF，重新选择将替换附件。' : '可选，最多 20MB。' }}</div></template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存(草稿)</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getAdminNotifications, createNotificationApi, updateNotificationApi,
  publishNotificationApi, deleteNotificationApi, uploadNotificationPdfApi
} from '../../api'
import { formatDateTime } from '../../utils/format'

const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', content: '', publisher: '', expiresAt: null, hasPdf: false })
const pdfFile = ref(null)

function openEdit(row) {
  Object.assign(form, row
    ? { id: row.id, title: row.title, content: row.content || '', publisher: row.publisher || '',
        expiresAt: row.expiresAt || null, hasPdf: !!row.hasPdf }
    : { id: null, title: '', content: '', publisher: '', expiresAt: null, hasPdf: false })
  pdfFile.value = null
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.title || !form.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  const payload = { title: form.title, content: form.content, publisher: form.publisher, expiresAt: form.expiresAt }
  let id = form.id
  if (form.id) {
    await updateNotificationApi(form.id, payload)
  } else {
    id = await createNotificationApi(payload)
  }
  if (pdfFile.value) {
    const fd = new FormData()
    fd.append('file', pdfFile.value)
    await uploadNotificationPdfApi(id, fd)
  }
  ElMessage.success('已保存(草稿),发布后前台可见')
  dialogVisible.value = false
  await load()
}

async function handlePublish(row) {
  await ElMessageBox.confirm(`确定发布「${row.title}」?发布后前台立即可见。`, '提示', { type: 'warning' })
  await publishNotificationApi(row.id)
  ElMessage.success('发布成功')
  await load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除通知「${row.title}」?`, '提示', { type: 'warning' })
  await deleteNotificationApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function load() {
  list.value = (await getAdminNotifications()) || []
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
</style>
