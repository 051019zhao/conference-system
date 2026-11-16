<template>
  <div class="page-card">
    <div class="page-head">
      <div class="page-title">资料管理</div>
      <el-button type="primary" @click="uploadVisible = true">
        <el-icon><Upload /></el-icon> 上传资料
      </el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="资料名称" min-width="200" />
      <el-table-column prop="description" label="说明" min-width="200" />
      <el-table-column label="大小" width="100">
        <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
      </el-table-column>
      <el-table-column prop="downloadCount" label="下载量" width="90" />
      <el-table-column label="上传时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="uploadVisible" title="上传资料" width="520px">
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="资料名称">
          <el-input v-model="uploadForm.name" placeholder="留空则使用文件名" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="uploadForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="文件" required>
          <el-upload drag :auto-upload="false" :limit="1" :on-change="(f) => (uploadForm.file = f.raw)">
            <el-icon :size="36"><UploadFilled /></el-icon>
            <div>拖拽文件到此处,或点击选择(≤20MB)</div>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="editVisible" title="编辑资料信息" width="480px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="editForm.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMaterials, uploadMaterial, updateMaterialApi, deleteMaterialApi } from '../../api'
import { formatDateTime, formatSize } from '../../utils/format'

const list = ref([])
const uploadVisible = ref(false)
const uploading = ref(false)
const uploadForm = reactive({ name: '', description: '', file: null })

const editVisible = ref(false)
const editForm = reactive({ id: null, name: '', description: '' })

async function handleUpload() {
  if (!uploadForm.file) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', uploadForm.file)
    fd.append('meta', new Blob([JSON.stringify({
      name: uploadForm.name || null,
      description: uploadForm.description || null
    })], { type: 'application/json' }))
    await uploadMaterial(fd)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    uploadForm.name = ''
    uploadForm.description = ''
    uploadForm.file = null
    await load()
  } finally {
    uploading.value = false
  }
}

function openEdit(row) {
  Object.assign(editForm, { id: row.id, name: row.name, description: row.description || '' })
  editVisible.value = true
}

async function handleEdit() {
  await updateMaterialApi(editForm.id, { name: editForm.name, description: editForm.description })
  ElMessage.success('保存成功')
  editVisible.value = false
  await load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除资料「${row.name}」?`, '提示', { type: 'warning' })
  await deleteMaterialApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function load() {
  list.value = (await getMaterials()) || []
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
