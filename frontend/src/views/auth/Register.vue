<template>
  <div class="reg-wrap">
    <el-card class="reg-card">
      <div class="auth-kicker">SEDB 2026 · AUTHOR ACCOUNT</div>
      <h1 class="reg-title">注册作者账号</h1>
      <p class="reg-subtitle">注册后需等待管理员审核通过</p>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="3-20 位,登录使用" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="真实姓名" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="用于接收验证码" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirm">
          <el-input v-model="form.confirm" type="password" show-password placeholder="再次输入密码" />
        </el-form-item>
        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" placeholder="邮箱验证码" />
            <el-button :disabled="countdown > 0" :loading="sending" @click="handleSendCode">
              {{ countdown > 0 ? countdown + 's 后重发' : '发送验证码' }}
            </el-button>
          </div>
        </el-form-item>
        <el-button type="primary" class="reg-btn" :loading="loading" @click="handleRegister">注 册</el-button>
        <div class="reg-tip">
          已有账号?<el-link type="primary" @click="$router.push('/login')">去登录</el-link>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { sendCode, register } from '../../api'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)

const form = reactive({
  username: '', realName: '', email: '', password: '', confirm: '', code: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度 3-20 位', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.password) callback(new Error('两次输入的密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
}

async function handleSendCode() {
  if (!form.email) {
    ElMessage.warning('请先填写邮箱')
    return
  }
  sending.value = true
  try {
    const data = await sendCode(form.email)
    // 本地联调(后端未启用邮件)时后端直接返回验证码
    if (data && data.devCode) {
      ElMessageBox.alert('验证码:' + data.devCode, '本地联调模式(未配置SMTP)', {
        confirmButtonText: '我知道了'
      }).catch(() => {})
    } else {
      ElMessage.success('验证码已发送,请查收邮箱')
    }
    countdown.value = 60
    const timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } finally {
    sending.value = false
  }
}

async function handleRegister() {
  await formRef.value.validate()
  loading.value = true
  try {
    await register({
      username: form.username,
      realName: form.realName,
      email: form.email,
      password: form.password,
      code: form.code
    })
    ElMessage.success('注册成功!账号已进入待审核状态,管理员审核通过后即可登录')
    router.push({ path: '/login', query: route.query.redirect ? { redirect: route.query.redirect } : {} })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reg-wrap {
  display: flex;
  justify-content: center;
  padding: 42px 0 70px;
}
.reg-card {
  width: min(540px, 100%);
  border-top: 3px solid #b3926d;
  box-shadow: 0 16px 38px rgba(25, 53, 68, .06);
}
.reg-card :deep(.el-card__body) { padding: 38px; }
.auth-kicker { text-align: center; color: #a17f59; font-size: 11px; font-weight: 700; letter-spacing: .18em; margin-bottom: 12px; }
.reg-title {
  font-family: Georgia, 'Songti SC', SimSun, serif;
  font-size: 28px;
  font-weight: 700;
  color: #1c3b48;
  text-align: center;
  margin-bottom: 6px;
}
.reg-subtitle { text-align: center; color: #829198; font-size: 13px; margin-bottom: 28px; }
.code-row {
  display: flex;
  gap: 8px;
  width: 100%;
}
.reg-btn {
  width: 100%;
}
.reg-tip {
  text-align: center;
  margin-top: 14px;
  color: #909399;
  font-size: 14px;
}
@media (max-width: 600px) {
  .reg-wrap { padding: 20px 0 38px; }
  .reg-card :deep(.el-card__body) { padding: 28px 18px; }
  .reg-card :deep(.el-form-item__label) { justify-content: flex-start; }
}
</style>
