<template>
  <div>
    <div class="page-title">专家介绍</div>
    <el-row :gutter="16">
      <el-col v-for="e in experts" :key="e.userId" :xs="24" :sm="12" :lg="8">
        <el-card class="expert-card" shadow="hover">
          <div class="expert-head">
            <el-avatar :size="56" :src="`/api/public/avatar/${e.userId}`" @error="e.avatarFailed = true">
              {{ e.avatarFailed ? e.realName?.charAt(0) : '' }}
            </el-avatar>
            <div>
              <div class="expert-name">
                {{ e.realName }}
                <el-tag v-if="e.isKeynote" type="warning" size="small">主旨报告专家</el-tag>
              </div>
              <div class="expert-title">{{ e.title || '-' }} · {{ e.affiliation || '-' }}</div>
              <div v-if="e.gender" class="expert-title">{{ e.gender }}</div>
            </div>
          </div>
          <div class="expert-keywords">
            <el-tag v-for="k in keywordList(e.researchKeywords)" :key="k" size="small" effect="plain">
              {{ k }}
            </el-tag>
          </div>
          <p class="expert-bio">{{ e.bio }}</p>
          <a v-if="safeWebsite(e.website)" :href="safeWebsite(e.website)" target="_blank" rel="noopener noreferrer">
            个人主页
          </a>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="experts.length === 0" description="暂无专家信息" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPublicExperts } from '../../api'

const experts = ref([])

function keywordList(raw) {
  return (raw || '').split(/[,，]/).map((s) => s.trim()).filter(Boolean)
}

function safeWebsite(value) {
  return /^https?:\/\//i.test(value || '') ? value : ''
}

onMounted(async () => {
  experts.value = (await getPublicExperts()) || []
})
</script>

<style scoped>
.expert-card {
  margin-bottom: 16px;
}
.expert-head {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 12px;
}
.expert-name {
  font-size: 17px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
}
.expert-title {
  color: #909399;
  font-size: 13px;
  margin-top: 4px;
}
.expert-keywords {
  margin-bottom: 10px;
}
.expert-keywords .el-tag {
  margin-right: 6px;
}
.expert-bio {
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
}
</style>
