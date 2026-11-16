<template>
  <div>
    <el-row :gutter="16">
      <el-col :xs="12" :sm="12" :md="6">
        <div class="page-card stat">
          <div class="num">{{ stats.paperCount ?? '-' }}</div>
          <div class="label">论文总数</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="page-card stat">
          <div class="num">{{ statusCount('ACCEPTED') + statusCount('FINAL_SUBMITTED') }}</div>
          <div class="label">录用论文</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="page-card stat">
          <div class="num">{{ roleCount('EXPERT') }}</div>
          <div class="label">评审专家</div>
        </div>
      </el-col>
      <el-col :xs="12" :sm="12" :md="6">
        <div class="page-card stat">
          <div class="num">{{ stats.userCount ?? '-' }}</div>
          <div class="label">用户总数</div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="10">
        <div class="page-card">
          <div class="page-title">论文状态分布</div>
          <div ref="pieRef" class="chart"></div>
        </div>
      </el-col>
      <el-col :xs="24" :md="14">
        <div class="page-card">
          <div class="page-title">各专题投稿数量</div>
          <div ref="barRef" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card" style="margin-top: 16px">
      <div class="page-title">专题投稿明细(视图 v_paper_stats)</div>
      <el-empty v-if="!stats.topicStats || stats.topicStats.length === 0" description="暂无数据" />
      <el-table v-else :data="stats.topicStats" size="small" border>
        <el-table-column prop="topicName" label="专题">
          <template #default="{ row }">{{ row.topicName || '未分专题' }}</template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">{{ paperStatusText[row.status] || row.status }}</template>
        </el-table-column>
        <el-table-column prop="cnt" label="数量" width="90" />
      </el-table>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="page-title">专题录用率排名(CTE + 窗口函数 RANK)</div>
      <el-empty v-if="!stats.topicAcceptRank || stats.topicAcceptRank.length === 0" description="暂无数据" />
      <el-table v-else :data="stats.topicAcceptRank" size="small" border>
        <el-table-column label="名次" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.rank === 1" type="warning" size="small">第 1 名</el-tag>
            <span v-else>{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="topicName" label="专题" />
        <el-table-column prop="total" label="投稿" width="80" />
        <el-table-column prop="accepted" label="录用" width="80" />
        <el-table-column label="录用率" width="90">
          <template #default="{ row }">{{ row.rate == null ? '-' : row.rate + '%' }}</template>
        </el-table-column>
      </el-table>
    </div>

    <div class="page-card" style="margin-top: 16px">
      <div class="page-title">会议基本信息</div>
      <el-form :model="conf" label-width="90px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="会议名称"><el-input v-model="conf.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="简称"><el-input v-model="conf.shortName" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="地点"><el-input v-model="conf.venue" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="开始日期"><el-date-picker v-model="conf.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="结束日期"><el-date-picker v-model="conf.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="投稿截止"><el-date-picker v-model="conf.submitDeadline" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="录用通知"><el-date-picker v-model="conf.notifyDeadline" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="终稿截止"><el-date-picker v-model="conf.finalDeadline" type="date" value-format="YYYY-MM-DD" style="width:100%" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系邮箱"><el-input v-model="conf.contactEmail" /></el-form-item>
          </el-col>
          <el-col :span="12"><el-form-item label="主办单位"><el-input v-model="conf.organizerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主办网址"><el-input v-model="conf.organizerUrl" placeholder="https://..." /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="主办 Logo"><el-input v-model="conf.organizerLogoUrl" placeholder="Logo 图片网址" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="协办单位"><el-input v-model="conf.coorganizerName" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="协办网址"><el-input v-model="conf.coorganizerUrl" placeholder="https://..." /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="协办 Logo"><el-input v-model="conf.coorganizerLogoUrl" placeholder="Logo 图片网址" /></el-form-item></el-col>
          <el-col :span="24">
            <el-form-item label="会议简介"><el-input v-model="conf.intro" type="textarea" :rows="3" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="征文内容"><el-input v-model="conf.cfpText" type="textarea" :rows="6" /></el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" :loading="saving" @click="handleSave">保存会议信息</el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { getAdminStats, getConference, updateConferenceApi } from '../../api'
import { paperStatusText } from '../../utils/format'

const stats = ref({})
const conf = ref({})
const saving = ref(false)
const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null
let chartResizeObserver = null

function statusCount(status) {
  const row = (stats.value.byStatus || []).find((s) => s.status === status)
  return row ? row.cnt : 0
}

function roleCount(role) {
  const row = (stats.value.byRole || []).find((s) => s.role === role)
  return row ? row.cnt : 0
}

function renderCharts() {
  if (pieRef.value) {
    pieChart = echarts.init(pieRef.value)
    pieChart.setOption({
      tooltip: { trigger: 'item' },
      legend: { bottom: 0 },
      series: [{
        type: 'pie',
        radius: ['38%', '62%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 6 },
        label: { formatter: '{b}: {c}' },
        data: (stats.value.byStatus || []).map((s) => ({
          name: paperStatusText[s.status] || s.status,
          value: s.cnt
        }))
      }]
    })
  }
  if (barRef.value) {
    // 按专题聚合投稿量
    const agg = {}
    for (const row of stats.value.topicStats || []) {
      const key = row.topicName || '未分专题'
      agg[key] = (agg[key] || 0) + row.cnt
    }
    barChart = echarts.init(barRef.value)
    barChart.setOption({
      tooltip: {},
      grid: { left: 40, right: 20, top: 20, bottom: 50 },
      xAxis: { type: 'category', data: Object.keys(agg), axisLabel: { interval: 0, rotate: 18 } },
      yAxis: { type: 'value', minInterval: 1 },
      series: [{ type: 'bar', barMaxWidth: 42, itemStyle: { borderRadius: [4, 4, 0, 0], color: '#409eff' }, data: Object.values(agg) }]
    })
  }
}

async function handleSave() {
  saving.value = true
  try {
    await updateConferenceApi({
      name: conf.value.name,
      shortName: conf.value.shortName,
      venue: conf.value.venue,
      startDate: conf.value.startDate,
      endDate: conf.value.endDate,
      submitDeadline: conf.value.submitDeadline,
      notifyDeadline: conf.value.notifyDeadline,
      finalDeadline: conf.value.finalDeadline,
      intro: conf.value.intro,
      cfpText: conf.value.cfpText,
      contactEmail: conf.value.contactEmail,
      organizerName: conf.value.organizerName,
      organizerUrl: conf.value.organizerUrl,
      organizerLogoUrl: conf.value.organizerLogoUrl,
      coorganizerName: conf.value.coorganizerName,
      coorganizerUrl: conf.value.coorganizerUrl,
      coorganizerLogoUrl: conf.value.coorganizerLogoUrl
    })
    ElMessage.success('会议信息已保存')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  stats.value = (await getAdminStats()) || {}
  conf.value = (await getConference()) || {}
  await nextTick()
  renderCharts()
  chartResizeObserver = new ResizeObserver(() => {
    pieChart?.resize()
    barChart?.resize()
  })
  if (pieRef.value) chartResizeObserver.observe(pieRef.value)
  if (barRef.value) chartResizeObserver.observe(barRef.value)
})

onBeforeUnmount(() => {
  chartResizeObserver?.disconnect()
  if (pieChart) pieChart.dispose()
  if (barChart) barChart.dispose()
})
</script>

<style scoped>
.stat {
  text-align: center;
}
.num {
  font-size: 30px;
  font-weight: 700;
  color: #409eff;
}
.label {
  color: #909399;
  margin-top: 4px;
}
.bar-row {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}
.chart {
  height: 300px;
  width: 100%;
}
@media (max-width: 600px) {
  .page-card :deep(.el-row > .el-col) { max-width: 100%; flex: 0 0 100%; }
  .chart { height: 260px; }
}
</style>
