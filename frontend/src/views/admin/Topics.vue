<template>
  <div class="page-card">
    <div class="page-head">
      <div class="page-title">专题管理</div>
      <el-button type="primary" @click="openEdit(null)">
        <el-icon><Plus /></el-icon> 新增专题
      </el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="专题名称" width="220" />
      <el-table-column prop="description" label="说明" min-width="260" />
      <el-table-column prop="reportLimit" label="报告数量" width="100" />
      <el-table-column prop="reportName" label="报告名称" min-width="190" />
      <el-table-column label="PPT" width="75"><template #default="{ row }">{{ row.hasPpt ? '有' : '—' }}</template></el-table-column>
      <el-table-column prop="sortNo" label="排序" width="80" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑专题' : '新增专题'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="专题名称" required>
          <el-input v-model="form.name" maxlength="100" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="form.sortNo" :min="0" />
        </el-form-item>
        <el-form-item label="报告数量"><el-input-number v-model="form.reportLimit" :min="0" /></el-form-item>
        <el-form-item label="报告专家">
          <el-select v-model="form.keynoteExpertId" clearable placeholder="选择专家" style="width:100%">
            <el-option v-for="e in experts" :key="e.userId" :value="e.userId" :label="e.realName" />
          </el-select>
        </el-form-item>
        <el-form-item label="报告名称"><el-input v-model="form.reportName" /></el-form-item>
        <el-form-item label="报告时间"><el-input v-model="form.reportTimeNote" placeholder="日期、时间与地点" /></el-form-item>
        <el-form-item label="报告 PPT">
          <el-upload :auto-upload="false" :limit="1" accept=".ppt,.pptx,.pdf"
                     :on-change="f => pptFile = f.raw" :on-remove="() => pptFile = null">
            <el-button>选择 PPT/PDF</el-button>
            <template #tip><div class="el-upload__tip">{{ form.hasPpt ? '已上传文件，重新选择将替换。' : '可选，不超过 20MB。' }}</div></template>
          </el-upload>
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
import { getTopics, createTopic, updateTopicApi, deleteTopicApi, getExpertsLite, uploadTopicPptApi } from '../../api'

const list = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, name: '', description: '', sortNo: 0, reportLimit: null,
  keynoteExpertId: null, reportName: '', reportTimeNote: '', hasPpt: false })
const experts = ref([])
const pptFile = ref(null)

function openEdit(row) {
  Object.assign(form, row
    ? { id: row.id, name: row.name, description: row.description || '', sortNo: row.sortNo || 0,
        reportLimit: row.reportLimit, keynoteExpertId: row.keynoteExpertId,
        reportName: row.reportName || '', reportTimeNote: row.reportTimeNote || '', hasPpt: !!row.hasPpt }
    : { id: null, name: '', description: '', sortNo: 0, reportLimit: null,
        keynoteExpertId: null, reportName: '', reportTimeNote: '', hasPpt: false })
  pptFile.value = null
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.name || !form.name.trim()) {
    ElMessage.warning('请填写专题名称')
    return
  }
  const payload = { name: form.name, description: form.description, sortNo: form.sortNo,
    reportLimit: form.reportLimit, keynoteExpertId: form.keynoteExpertId,
    reportName: form.reportName, reportTimeNote: form.reportTimeNote }
  let id = form.id
  if (form.id) {
    await updateTopicApi(form.id, payload)
  } else {
    id = await createTopic(payload)
  }
  if (pptFile.value) {
    const fd = new FormData()
    fd.append('file', pptFile.value)
    await uploadTopicPptApi(id, fd)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除专题「${row.name}」?`, '提示', { type: 'warning' })
  await deleteTopicApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function load() {
  list.value = (await getTopics()) || []
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
