<template>
  <div class="profile-wrap">
    <div class="page-card">
      <div class="page-title">我的头像</div>
      <div class="avatar-row">
        <el-avatar :size="80" :src="avatarUrl" @error="avatarUrl = ''">
          {{ (store.user?.realName || '?').charAt(0) }}
        </el-avatar>
        <div>
          <el-upload :auto-upload="false" :show-file-list="false" accept="image/png,image/jpeg"
                     :on-change="(f) => uploadAvatar(f.raw)">
            <el-button type="primary" :loading="uploadingAvatar">上传新头像</el-button>
          </el-upload>
          <div class="avatar-tip">支持 PNG/JPEG,不超过 2MB;图片以 BYTEA 大对象存储在 openGauss 中</div>
        </div>
      </div>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="page-title">基本资料</div>
      <el-form label-width="90px" style="max-width: 460px">
        <el-form-item label="用户名">
          <el-input :model-value="store.user?.username" disabled />
        </el-form-item>
        <el-form-item label="角色">
          <el-input :model-value="roleText[store.role]" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profile.realName" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profile.email" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="profile.gender" clearable placeholder="请选择" style="width:100%">
            <el-option label="男" value="男" /><el-option label="女" value="女" />
            <el-option label="不公开" value="不公开" />
          </el-select>
        </el-form-item>
        <el-form-item label="学历"><el-input v-model="profile.education" placeholder="如：硕士" /></el-form-item>
        <el-form-item label="职称"><el-input v-model="profile.jobTitle" placeholder="如：讲师" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="profile.phone" /></el-form-item>
        <el-form-item label="工作单位"><el-input v-model="profile.workUnit" placeholder="可填写至二级单位" /></el-form-item>
        <el-form-item label="邮编"><el-input v-model="profile.postalCode" /></el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingProfile" @click="saveProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="page-title">修改密码</div>
      <el-form ref="pwdFormRef" :model="pwd" :rules="pwdRules" label-width="90px" style="max-width: 460px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwd.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwd.newPassword" type="password" show-password placeholder="6-20 位" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirm">
          <el-input v-model="pwd.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingPwd" @click="savePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div v-if="store.role === 'EXPERT'" class="page-card" style="margin-top: 16px">
      <div class="page-title">专家档案与评审偏好</div>
      <el-form :model="expert" label-width="95px" style="max-width: 560px">
        <el-form-item label="职称"><el-input v-model="expert.title" /></el-form-item>
        <el-form-item label="所属单位"><el-input v-model="expert.affiliation" /></el-form-item>
        <el-form-item label="通讯地址"><el-input v-model="expert.address" /></el-form-item>
        <el-form-item label="个人网站"><el-input v-model="expert.website" /></el-form-item>
        <el-form-item label="个人简介"><el-input v-model="expert.bio" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="研究方向"><el-input v-model="expert.researchKeywords" /></el-form-item>
        <el-form-item label="可审专题"><el-input v-model="expert.reviewTopics" /></el-form-item>
        <el-form-item label="评审容量"><el-input-number v-model="expert.reviewCapacity" :min="0" />（0 表示不限）</el-form-item>
        <el-form-item label="参与评审"><el-switch v-model="expert.reviewEnabled" /></el-form-item>
        <el-form-item><el-button type="primary" :loading="savingExpert" @click="saveExpert">保存专家档案</el-button></el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getMe, updateProfileApi, changePasswordApi, uploadAvatarApi,
  getMyExpertProfile, updateMyExpertProfile } from '../../api'
import { roleText } from '../../utils/format'
import { useUserStore } from '../../stores/user'

const store = useUserStore()
const expert = reactive({ title: '', affiliation: '', address: '', website: '', bio: '',
  researchKeywords: '', reviewTopics: '', reviewCapacity: 0, reviewEnabled: true })
const savingExpert = ref(false)

async function saveExpert() {
  savingExpert.value = true
  try {
    await updateMyExpertProfile({ ...expert })
    ElMessage.success('专家档案已保存')
  } finally {
    savingExpert.value = false
  }
}

onMounted(async () => {
  if (store.role === 'EXPERT') {
    Object.assign(expert, (await getMyExpertProfile()) || {})
  }
})

// 头像:无自定义头像时 URL 返回 404,el-avatar 自动回退为文字头像
const avatarUrl = ref(`/api/public/avatar/${store.user?.id}?t=${Date.now()}`)
const uploadingAvatar = ref(false)

async function uploadAvatar(file) {
  if (!file) return
  uploadingAvatar.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    await uploadAvatarApi(fd)
    avatarUrl.value = `/api/public/avatar/${store.user?.id}?t=${Date.now()}`
    ElMessage.success('头像已更新')
  } finally {
    uploadingAvatar.value = false
  }
}
const profile = reactive({
  realName: store.user?.realName || '',
  email: store.user?.email || '',
  gender: store.user?.gender || '',
  education: store.user?.education || '',
  jobTitle: store.user?.jobTitle || '',
  phone: store.user?.phone || '',
  workUnit: store.user?.workUnit || '',
  postalCode: store.user?.postalCode || ''
})
const savingProfile = ref(false)

const pwdFormRef = ref()
const savingPwd = ref(false)
const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度 6-20 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwd.newPassword) callback(new Error('两次输入的新密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function saveProfile() {
  if (!profile.realName || !profile.email) {
    ElMessage.warning('姓名和邮箱不能为空')
    return
  }
  savingProfile.value = true
  try {
    await updateProfileApi({ ...profile })
    // 同步本地登录态缓存
    const fresh = await getMe()
    store.setLogin(store.token, fresh)
    ElMessage.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  await pwdFormRef.value.validate()
  savingPwd.value = true
  try {
    await changePasswordApi({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改,请牢记新密码')
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
  } finally {
    savingPwd.value = false
  }
}
</script>

<style scoped>
.profile-wrap {
  max-width: 720px;
  margin: 0 auto;
}
.avatar-row {
  display: flex;
  align-items: center;
  gap: 20px;
}
.avatar-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 13px;
}
</style>
