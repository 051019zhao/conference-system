<template>
  <div class="page-card">
    <div class="page-head">
      <div class="page-title">用户管理</div>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon> 创建账号(专家/作者)
      </el-button>
      <el-upload :auto-upload="false" :show-file-list="false" accept=".csv" :on-change="f => handleImport(f.raw)">
        <el-button>批量导入专家 CSV</el-button>
      </el-upload>
      <el-button text @click="downloadTemplate">下载 CSV 模板</el-button>
    </div>

    <el-form inline>
      <el-form-item label="角色">
        <el-select v-model="query.role" clearable placeholder="全部" style="width: 140px">
          <el-option label="管理员" value="ADMIN" />
          <el-option label="专家" value="EXPERT" />
          <el-option label="作者" value="AUTHOR" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="姓名/用户名/邮箱" clearable style="width: 220px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="username" label="用户名" width="130" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column prop="email" label="邮箱" min-width="170" />
      <el-table-column label="角色" width="90">
        <template #default="{ row }">
          <el-tag :type="row.role === 'ADMIN' ? 'danger' : row.role === 'EXPERT' ? 'warning' : 'info'">
            {{ roleText[row.role] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="职称" width="90" />
      <el-table-column prop="affiliation" label="单位" min-width="130" />
      <el-table-column prop="researchKeywords" label="研究方向" min-width="150" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'warning' : 'info'">
            {{ userStatusText[row.status] || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="主旨专家" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.role === 'EXPERT' && row.isKeynote" type="warning" size="small">是</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="290" fixed="right">
        <template #default="{ row }">
          <template v-if="row.status === 2">
            <el-popconfirm title="通过后该用户即可登录,确定?" confirm-button-text="通过" @confirm="handleReview(row, true)">
              <template #reference>
                <el-button link type="success">通过</el-button>
              </template>
            </el-popconfirm>
            <el-popconfirm title="拒绝后账号将被禁用,确定?" confirm-button-text="拒绝" @confirm="handleReview(row, false)">
              <template #reference>
                <el-button link type="danger">拒绝</el-button>
              </template>
            </el-popconfirm>
          </template>
          <template v-else>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggle(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="resetPwd(row)">重置密码</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination class="pager" background layout="total, sizes, prev, pager, next" :total="total"
                   v-model:current-page="query.page" v-model:page-size="query.size"
                   :page-sizes="[10, 20, 50]" @current-change="load" @size-change="load" />

    <!-- 创建账号 -->
    <el-dialog v-model="createVisible" title="创建账号" width="660px">
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="100px">
        <el-form-item label="角色" prop="role">
          <el-radio-group v-model="createForm.role">
            <el-radio value="EXPERT">评审专家</el-radio>
            <el-radio value="AUTHOR">作者</el-radio>
            <el-radio value="ADMIN">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="姓名" prop="realName"><el-input v-model="createForm.realName" /></el-form-item>
        <el-form-item label="邮箱" prop="email"><el-input v-model="createForm.email" /></el-form-item>
        <template v-if="createForm.role === 'EXPERT'">
          <el-form-item label="职称"><el-input v-model="createForm.title" placeholder="教授/副教授…" /></el-form-item>
          <el-form-item label="单位"><el-input v-model="createForm.affiliation" /></el-form-item>
          <el-form-item label="研究方向">
            <el-input v-model="createForm.researchKeywords" placeholder="逗号分隔,如:数据库,数据挖掘(用于自动分配评审)" />
          </el-form-item>
          <el-form-item label="性别"><el-input v-model="createForm.gender" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="createForm.phone" /></el-form-item>
          <el-form-item label="邮编"><el-input v-model="createForm.postalCode" /></el-form-item>
          <el-form-item label="通讯地址"><el-input v-model="createForm.address" /></el-form-item>
          <el-form-item label="个人网站"><el-input v-model="createForm.website" placeholder="https://..." /></el-form-item>
          <el-form-item label="专家简介"><el-input v-model="createForm.bio" type="textarea" /></el-form-item>
          <el-form-item label="可审专题"><el-input v-model="createForm.reviewTopics" placeholder="专题名称，用逗号分隔" /></el-form-item>
          <el-form-item label="评审容量"><el-input-number v-model="createForm.reviewCapacity" :min="0" />（0 表示不限）</el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="creating" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="importVisible" title="导入结果（密码仅本次可见）" width="700px">
      <el-table :data="importResults" border>
        <el-table-column prop="realName" label="姓名" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="password" label="初始密码" />
      </el-table>
    </el-dialog>

    <!-- 创建成功展示账号密码 -->
    <el-dialog v-model="resultVisible" title="账号创建成功" width="440px">
      <el-alert type="warning" :closable="false" show-icon
                title="用户名和密码仅本次展示,请复制后转交本人!" style="margin-bottom: 14px" />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="用户名">{{ created.username }}</el-descriptions-item>
        <el-descriptions-item label="初始密码">{{ created.password }}</el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- 编辑 -->
    <el-dialog v-model="editVisible" title="编辑用户" width="660px">
      <el-form :model="editForm" label-width="100px">
        <el-form-item label="姓名"><el-input v-model="editForm.realName" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
        <template v-if="editForm.role === 'EXPERT'">
          <el-form-item label="职称"><el-input v-model="editForm.title" /></el-form-item>
          <el-form-item label="单位"><el-input v-model="editForm.affiliation" /></el-form-item>
          <el-form-item label="研究方向"><el-input v-model="editForm.researchKeywords" /></el-form-item>
          <el-form-item label="性别"><el-input v-model="editForm.gender" /></el-form-item>
          <el-form-item label="联系电话"><el-input v-model="editForm.phone" /></el-form-item>
          <el-form-item label="邮编"><el-input v-model="editForm.postalCode" /></el-form-item>
          <el-form-item label="通讯地址"><el-input v-model="editForm.address" /></el-form-item>
          <el-form-item label="个人网站"><el-input v-model="editForm.website" /></el-form-item>
          <el-form-item label="专家简介"><el-input v-model="editForm.bio" type="textarea" /></el-form-item>
          <el-form-item label="可审专题"><el-input v-model="editForm.reviewTopics" /></el-form-item>
          <el-form-item label="评审容量"><el-input-number v-model="editForm.reviewCapacity" :min="0" /></el-form-item>
          <el-form-item label="参与评审"><el-switch v-model="editForm.reviewEnabled" /></el-form-item>
          <el-form-item label="主旨专家">
            <el-switch v-model="editForm.isKeynote" active-text="标记为主旨报告专家" />
          </el-form-item>
        </template>
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
import { getUsers, createUserApi, updateUserApi, toggleUserStatus, resetPassword, reviewRegisterApi, importExpertsApi } from '../../api'
import { roleText, userStatusText } from '../../utils/format'

const query = reactive({ role: '', keyword: '', page: 1, size: 10 })
const list = ref([])
const total = ref(0)

const createVisible = ref(false)
const creating = ref(false)
const createFormRef = ref()
const emptyExpert = () => ({ gender: '', phone: '', postalCode: '', address: '', website: '', bio: '',
  reviewTopics: '', reviewCapacity: 0 })
const createForm = reactive({ role: 'EXPERT', realName: '', email: '', title: '', affiliation: '',
  researchKeywords: '', ...emptyExpert() })
const importVisible = ref(false)
const importResults = ref([])
const createRules = {
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const resultVisible = ref(false)
const created = ref({})

const editVisible = ref(false)
const editForm = reactive({
  id: null, role: '', realName: '', email: '', title: '', affiliation: '', researchKeywords: '',
  isKeynote: false, reviewEnabled: true, ...emptyExpert()
})

function openCreate() {
  Object.assign(createForm, { role: 'EXPERT', realName: '', email: '', title: '', affiliation: '',
    researchKeywords: '', ...emptyExpert() })
  createVisible.value = true
}

async function handleCreate() {
  await createFormRef.value.validate()
  creating.value = true
  try {
    created.value = await createUserApi({ ...createForm })
    createVisible.value = false
    resultVisible.value = true
    await load()
  } finally {
    creating.value = false
  }
}

function openEdit(row) {
  Object.assign(editForm, {
    id: row.id,
    role: row.role,
    realName: row.realName,
    email: row.email,
    title: row.title || '',
    affiliation: row.affiliation || '',
    researchKeywords: row.researchKeywords || '',
    isKeynote: !!row.isKeynote,
    reviewEnabled: row.reviewEnabled !== false,
    gender: row.gender || '', phone: row.phone || '', postalCode: row.postalCode || '',
    address: row.address || '', website: row.website || '', bio: row.bio || '',
    reviewTopics: row.reviewTopics || '', reviewCapacity: row.reviewCapacity || 0
  })
  editVisible.value = true
}

async function handleEdit() {
  await updateUserApi(editForm.id, {
    realName: editForm.realName,
    email: editForm.email,
    title: editForm.title,
    affiliation: editForm.affiliation,
    researchKeywords: editForm.researchKeywords,
    isKeynote: editForm.role === 'EXPERT' ? editForm.isKeynote : null,
    gender: editForm.gender, phone: editForm.phone, postalCode: editForm.postalCode,
    address: editForm.address, website: editForm.website, bio: editForm.bio,
    reviewTopics: editForm.reviewTopics, reviewCapacity: editForm.reviewCapacity,
    reviewEnabled: editForm.reviewEnabled
  })
  ElMessage.success('保存成功')
  editVisible.value = false
  await load()
}

async function toggle(row) {
  await ElMessageBox.confirm(
    `确定要${row.status === 1 ? '禁用' : '启用'}账号「${row.realName}」吗?禁用后该用户无法登录。`,
    '提示', { type: 'warning' }
  )
  await toggleUserStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success('操作成功')
  await load()
}

async function handleReview(row, approve) {
  await reviewRegisterApi(row.id, approve)
  ElMessage.success(approve ? '已通过注册审核,该用户现在可以登录' : '已拒绝注册,账号被禁用')
  await load()
}

async function resetPwd(row) {
  await ElMessageBox.confirm(`确定重置「${row.realName}」的密码吗?`, '提示', { type: 'warning' })
  const data = await resetPassword(row.id)
  ElMessageBox.alert('新密码:' + data.password, '重置成功(仅本次展示)', { confirmButtonText: '知道了' })
}

async function handleImport(file) {
  if (!file) return
  const fd = new FormData()
  fd.append('file', file)
  importResults.value = (await importExpertsApi(fd)) || []
  importVisible.value = true
  ElMessage.success(`已导入 ${importResults.value.length} 位专家`)
  await load()
}

function downloadTemplate() {
  const csv = 'realName,email,title,affiliation,researchKeywords,gender,phone,postalCode,address,website,bio,reviewTopics,reviewCapacity\n'
    + '示例专家,example@university.edu,教授,示例大学,"数据库,信息检索",男,13800000000,510000,广州市大学城,https://example.edu,研究简介,数据库,3\n'
  const url = URL.createObjectURL(new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8' }))
  const link = document.createElement('a')
  link.href = url
  link.download = '专家导入模板.csv'
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 1000)
}

async function load() {
  const data = await getUsers({
    role: query.role || undefined,
    keyword: query.keyword || undefined,
    page: query.page,
    size: query.size
  })
  list.value = data.list || []
  total.value = Number(data.total || 0)
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
</style>
