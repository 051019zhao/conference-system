<template>
  <div class="page-card">
    <div class="page-head">
      <div class="page-title">主旨报告管理</div>
      <el-button type="primary" @click="openEdit(null)">
        <el-icon><Plus /></el-icon> 新增报告
      </el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="报告标题" min-width="220" />
      <el-table-column prop="expertName" label="报告专家" width="100" />
      <el-table-column prop="expertTitle" label="职称" width="90" />
      <el-table-column prop="timeNote" label="时间/地点" min-width="180" />
      <el-table-column prop="sortNo" label="排序" width="70" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑报告' : '新增报告'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="报告专家" required>
          <el-select v-model="form.expertId" placeholder="选择专家" style="width: 100%">
            <el-option v-for="e in experts" :key="e.profileId" :value="e.profileId"
                       :label="`${e.realName}(${e.title || '专家'},${e.affiliation || '-'})`" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告标题" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="摘要">
          <el-input v-model="form.abstractText" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="时间地点">
          <el-input v-model="form.timeNote" placeholder="如:12月18日 09:00-10:00 · 主会场" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getKeynotes, createKeynote, updateKeynoteApi, deleteKeynoteApi, getExpertsLite } from '../../api'

const list = ref([])
const experts = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, expertId: null, title: '', abstractText: '', timeNote: '', sortNo: 0 })

function openEdit(row) {
  Object.assign(form, row
    ? { id: row.id, expertId: row.expertId, title: row.title, abstractText: row.abstractText || '', timeNote: row.timeNote || '', sortNo: row.sortNo || 0 }
    : { id: null, expertId: null, title: '', abstractText: '', timeNote: '', sortNo: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.expertId || !form.title) {
    ElMessage.warning('请选择专家并填写标题')
    return
  }
  const payload = { expertId: form.expertId, title: form.title, abstractText: form.abstractText, timeNote: form.timeNote, sortNo: form.sortNo }
  if (form.id) {
    await updateKeynoteApi(form.id, payload)
  } else {
    await createKeynote(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除报告「${row.title}」?`, '提示', { type: 'warning' })
  await deleteKeynoteApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function load() {
  list.value = (await getKeynotes()) || []
}

onMounted(async () => {
  await load()
  experts.value = (await getExpertsLite()) || []
})
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
</style>
