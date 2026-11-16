<template>
  <div>
    <div class="section-kicker">KEYNOTE TALKS</div>
    <h1 class="page-title">主旨报告</h1>
    <el-empty v-if="keynotes.length === 0" description="暂无主旨报告" />
    <div class="keynote-grid">
    <el-card v-for="(k, index) in keynotes" :key="k.id" class="keynote-card" shadow="never">
      <div class="report-index">REPORT {{ String(index + 1).padStart(2, '0') }}</div>
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
.section-kicker { color: #a17f59; font-size: 11px; font-weight: 700; letter-spacing: .18em; margin-bottom: 9px; }
.keynote-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 18px; }
.keynote-card {
  border-top: 3px solid #b3926d;
  background: #fff;
}
.keynote-card :deep(.el-card__body) { padding: 30px; }
.report-index {
  color: #a17f59;
  font: 700 12px Georgia, serif;
  letter-spacing: .12em;
  margin-bottom: 22px;
}
.k-title {
  font-family: Georgia, 'Songti SC', SimSun, serif;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.45;
  color: #1b3a48;
  margin-bottom: 18px;
}
.k-meta {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #667981;
  font-size: 14px;
  margin-bottom: 8px;
}
.k-abstract {
  color: #4c6069;
  line-height: 1.9;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid #e8ecea;
}
@media (max-width: 760px) { .keynote-grid { grid-template-columns: 1fr; } }
@media (max-width: 600px) { .keynote-card :deep(.el-card__body) { padding: 23px; } .k-title { font-size: 20px; } }
</style>
