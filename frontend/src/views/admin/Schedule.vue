<template>
  <div class="page-card">
    <div class="page-head">
      <div class="page-title">日程管理</div>
      <el-button type="primary" @click="openEdit(null)">
        <el-icon><Plus /></el-icon> 新增日程
      </el-button>
    </div>
    <el-table :data="list" border>
      <el-table-column prop="day" label="日期" width="110">
        <template #default="{ row }">{{ formatDate(row.day) }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始" width="80" />
      <el-table-column prop="endTime" label="结束" width="80" />
      <el-table-column prop="title" label="内容" min-width="220" />
      <el-table-column prop="speaker" label="主讲/负责" width="110" />
      <el-table-column prop="location" label="地点" width="130" />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="sortNo" label="排序" width="70" />
      <el-table-column label="操作" width="150">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑日程' : '新增日程'" width="560px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="日期" required>
          <el-date-picker v-model="form.day" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-row>
          <el-col :span="12"><el-form-item label="开始时间"><el-time-picker v-model="form.startTime" format="HH:mm" value-format="HH:mm" style="width: 100%" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="结束时间"><el-time-picker v-model="form.endTime" format="HH:mm" value-format="HH:mm" style="width: 100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="内容" required>
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="主讲/负责">
          <el-input v-model="form.speaker" />
        </el-form-item>
        <el-form-item label="地点">
          <el-input v-model="form.location" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" allow-create filterable default-first-option style="width: 100%">
            <el-option label="开幕式" value="开幕式" />
            <el-option label="主旨报告" value="主旨报告" />
            <el-option label="分论坛" value="分论坛" />
            <el-option label="茶歇" value="茶歇" />
            <el-option label="闭幕式" value="闭幕式" />
            <el-option label="常规" value="常规" />
          </el-select>
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
import { getSchedule, createSchedule, updateScheduleApi, deleteScheduleApi } from '../../api'
import { formatDate } from '../../utils/format'

const list = ref([])
const dialogVisible = ref(false)
const form = reactive({
  id: null, day: '', startTime: '', endTime: '', title: '', speaker: '', location: '', type: '常规', sortNo: 0
})

function openEdit(row) {
  Object.assign(form, row
    ? {
        id: row.id, day: formatDate(row.day), startTime: row.startTime || '', endTime: row.endTime || '',
        title: row.title, speaker: row.speaker || '', location: row.location || '', type: row.type || '常规', sortNo: row.sortNo || 0
      }
    : { id: null, day: '', startTime: '', endTime: '', title: '', speaker: '', location: '', type: '常规', sortNo: 0 })
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.day || !form.title) {
    ElMessage.warning('请填写日期和内容')
    return
  }
  const payload = { ...form, day: form.day }
  if (form.id) {
    await updateScheduleApi(form.id, payload)
  } else {
    await createSchedule(payload)
  }
  ElMessage.success('保存成功')
  dialogVisible.value = false
  await load()
}

async function handleDelete(row) {
  await ElMessageBox.confirm(`确定删除日程「${row.title}」?`, '提示', { type: 'warning' })
  await deleteScheduleApi(row.id)
  ElMessage.success('删除成功')
  await load()
}

async function load() {
  list.value = (await getSchedule()) || []
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
