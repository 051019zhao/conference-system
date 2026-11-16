<template>
  <div class="page-card">
    <div class="page-title">操作审计日志</div>
    <el-alert type="info" :closable="false" show-icon style="margin-bottom: 12px"
              title="系统自动记录关键业务动作(登录/投稿/评审/终审/用户管理),配合论文状态触发器日志构成完整审计链" />

    <el-form inline>
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="模糊匹配" clearable style="width: 160px" @keyup.enter="load" />
      </el-form-item>
      <el-form-item label="动作">
        <el-select v-model="query.action" clearable placeholder="全部" style="width: 170px">
          <el-option v-for="(text, key) in actionText" :key="key" :label="text" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column label="时间" width="165">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column prop="username" label="用户名" width="110" />
      <el-table-column label="角色" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : row.role === 'EXPERT' ? 'warning' : 'info'">
            {{ roleText[row.role] || row.role || '-' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="动作" width="130">
        <template #default="{ row }">
          <el-tag size="small" effect="plain">{{ actionText[row.action] || row.action }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="280" />
      <el-table-column prop="ip" label="IP" width="120" />
    </el-table>

    <el-pagination class="pager" background layout="total, sizes, prev, pager, next" :total="total"
                   v-model:current-page="query.page" v-model:page-size="query.size"
                   :page-sizes="[10, 20, 50]" @current-change="load" @size-change="load" />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getOperationLogs } from '../../api'
import { formatDateTime, roleText } from '../../utils/format'

const query = reactive({ username: '', action: '', page: 1, size: 10 })
const list = ref([])
const total = ref(0)

const actionText = {
  LOGIN: '登录', REGISTER: '注册', PROFILE_UPDATE: '修改资料', PASSWORD_CHANGE: '修改密码',
  SUBMIT: '投稿', WITHDRAW: '撤回投稿', FINAL: '提交终稿',
  FIRST_PASS: '初审通过', REJECT: '退稿', DECIDE: '终审',
  ASSIGN: '分配评审', REVIEW: '提交评审',
  USER_CREATE: '创建账号', USER_STATUS: '启用/禁用', USER_RESET_PWD: '重置密码'
}

async function load() {
  const data = await getOperationLogs({
    username: query.username || undefined,
    action: query.action || undefined,
    page: query.page,
    size: query.size
  })
  list.value = data.list || []
  total.value = Number(data.total || 0)
}

onMounted(load)
</script>

<style scoped>
.pager {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
