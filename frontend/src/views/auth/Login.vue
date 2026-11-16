<template>
  <div class="login-wrap">
    <el-card class="login-card">
      <div class="login-title">账号登录</div>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0" size="large">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input v-model="form.password" type="password" placeholder="密码" show-password
                    :prefix-icon="Lock" @keyup.enter="handleLogin" />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="handleLogin">登 录</el-button>
      </el-form>
      <div class="login-tip">
        还没有账号?<el-link type="primary" @click="goToRegister">注册作者账号</el-link>
        <span class="divider">|</span>
        <el-link type="info" @click="forgotVisible = true">忘记密码</el-link>
      </div>
    </el-card>

    <!-- 忘记密码:邮箱验证码重置 -->
    <el-dialog v-model="forgotVisible" title="找回密码" width="460px">
      <el-form :model="forgot" label-width="80px">
        <el-form-item label="注册邮箱">
          <el-input v-model="forgot.email" placeholder="注册时填写的邮箱" />
        </el-form-item>
        <el-form-item label="验证码">
          <div class="forgot-code">
            <el-input v-model="forgot.code" />
            <el-button :disabled="forgotCountdown > 0" :loading="sending" @click="sendForgotCode">
              {{ forgotCountdown > 0 ? forgotCountdown + 's 后重发' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="forgot.newPassword" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="forgotVisible = false">取消</el-button>
        <el-button type="primary" :loading="resetting" @click="handleForgot">重置密码</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login, sendCode, forgotPassword } from '../../api'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const route = useRoute()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

function goToRegister() {
  router.push({ path: '/register', query: route.query.redirect ? { redirect: route.query.redirect } : {} })
}

// ---- 忘记密码 ----
const forgotVisible = ref(false)
const sending = ref(false)
const resetting = ref(false)
const forgotCountdown = ref(0)
const forgot = reactive({ email: '', code: '', newPassword: '' })

async function sendForgotCode() {
  if (!forgot.email) {
    ElMessage.warning('请先填写注册邮箱')
    return
  }
  sending.value = true
  try {
    const data = await sendCode(forgot.email)
    if (data && data.devCode) {
      ElMessageBox.alert('验证码:' + data.devCode, '本地联调模式(未配置SMTP)', {
        confirmButtonText: '我知道了'
      }).catch(() => {})
    } else {
      ElMessage.success('验证码已发送,请查收邮箱')
    }
    forgotCountdown.value = 60
    const timer = setInterval(() => {
      forgotCountdown.value--
      if (forgotCountdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function handleForgot() {
  if (!forgot.email || !forgot.code || !forgot.newPassword) {
    ElMessage.warning('请完整填写邮箱、验证码和新密码')
    return
  }
  if (forgot.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  resetting.value = true
  try {
    await forgotPassword({ ...forgot })
    ElMessage.success('密码已重置,请使用新密码登录')
    forgotVisible.value = false
    forgot.code = ''
    forgot.newPassword = ''
  } finally {
    resetting.value = false
  }
}

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form)
    store.setLogin(data.token, data.user)
    ElMessage.success('登录成功')
    if (route.query.redirect) {
      router.push(route.query.redirect)
    } else if (data.user.role === 'ADMIN') {
      router.push('/admin')
    } else if (data.user.role === 'EXPERT') {
      router.push('/expert/tasks')
    } else {
      router.push('/author/papers')
    }
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  display: flex;
  justify-content: center;
  padding: 48px 0;
}
.login-card {
  width: 400px;
}
.login-title {
  font-size: 20px;
  font-weight: 600;
  text-align: center;
  margin-bottom: 24px;
}
.login-btn {
  width: 100%;
}
.login-tip {
  text-align: center;
  margin-top: 16px;
  color: #909399;
  font-size: 14px;
}
.divider {
  margin: 0 8px;
  color: #dcdfe6;
}
.forgot-code {
  display: flex;
  gap: 8px;
  width: 100%;
}
</style>
