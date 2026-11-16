<template>
  <div>
    <div class="page-title">主旨报告</div>
    <el-empty v-if="keynotes.length === 0" description="暂无主旨报告" />
    <el-card v-for="k in keynotes" :key="k.id" class="keynote-card" shadow="hover">
      <div class="k-title">{{ k.title }}</div>
      <div class="k-meta">
        <el-icon><User /></el-icon> {{ k.expertName }}({{ k.expertTitle }},{{ k.affiliation }})
      </div>
      <div class="k-meta">
        <el-icon><Clock /></el-icon> {{ k.timeNote || '时间待定' }}
      </div>
      <p class="k-abstract">{{ k.abstractText }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getKeynotes } from '../../api'

const keynotes = ref([])

onMounted(async () => {
  keynotes.value = (await getKeynotes()) || []
})
</script>

<style scoped>
.keynote-card {
  margin-bottom: 16px;
}
.k-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}
.k-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #606266;
  font-size: 14px;
  margin-bottom: 6px;
}
.k-abstract {
  color: #606266;
  line-height: 1.8;
  margin-top: 10px;
}
</style>
