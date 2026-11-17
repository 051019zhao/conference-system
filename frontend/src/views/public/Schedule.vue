<template>
  <div class="program-page">
    <header class="program-intro">
      <div class="section-kicker">CONFERENCE PROGRAM / SEDB 2026</div>
      <h1>会议日程</h1>
      <p>按日期浏览报告与论坛安排。具体时间和地点以现场通知为准。</p>
    </header>

    <el-empty v-if="groups.length === 0" description="日程暂未发布" />
    <div v-else class="program-layout">
      <nav class="program-days" aria-label="按日期跳转">
        <router-link v-for="group in groups" :key="group.day" v-ripple
                     :to="{ path: '/schedule', hash: '#day-' + group.day }"
                     class="day-link" :class="{ active: activeDay === group.day }">
          <strong>{{ group.day.slice(5) }}</strong><span>{{ group.week }}</span>
        </router-link>
      </nav>

      <div class="program-groups">
        <section v-for="group in groups" :id="'day-' + group.day" :key="group.day" class="day-card">
          <div class="day-header">
            <span>DAY {{ String(group.index).padStart(2, '0') }}</span>
            <div><h2>{{ group.day }}</h2><small>{{ group.week }}</small></div>
          </div>
          <div v-for="item in group.items" :key="item.id" class="agenda-row">
            <div class="agenda-time"><strong>{{ item.startTime }}</strong><small>{{ item.endTime }}</small></div>
            <div class="agenda-content">
              <span class="agenda-type" :class="typeClass(item.type)">{{ item.type || '会议活动' }}</span>
              <h3>{{ item.title }}</h3>
              <p><span v-if="item.speaker">{{ item.speaker }}</span><span v-if="item.location">{{ item.location }}</span></p>
            </div>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { getSchedule } from '../../api'

const route = useRoute()
const items = ref([])
const weekNames = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六']

const groups = computed(() => {
  const byDay = new Map()
  for (const item of items.value) {
    const day = String(item.day).slice(0, 10)
    if (!byDay.has(day)) byDay.set(day, [])
    byDay.get(day).push(item)
  }
  return [...byDay.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([day, list], index) => ({
    day,
    index: index + 1,
    week: weekNames[new Date(day.replace(/-/g, '/')).getDay()],
    items: list.sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
  }))
})
const activeDay = computed(() => route.hash.replace('#day-', '') || groups.value[0]?.day)

function typeClass(type) {
  if (type === '主旨报告') return 'keynote'
  if (type === '开幕式' || type === '闭幕式') return 'ceremony'
  return 'forum'
}

onMounted(async () => {
  items.value = (await getSchedule()) || []
})
</script>

<style scoped>
.program-intro { position: relative; overflow: hidden; background: #173642; color: #fff; padding: 45px 50px; margin-bottom: 28px; }
.program-intro::after { content: ''; position: absolute; right: -90px; top: -130px; width: 390px; height: 390px; border: 1px solid rgba(196,166,122,.42); border-radius: 50%; box-shadow: 0 0 0 54px rgba(140,183,181,.06), 0 0 0 108px rgba(140,183,181,.04); }
.section-kicker { color: #c6a67b; font: 700 11px Georgia, serif; letter-spacing: .17em; }
.program-intro h1 { position: relative; z-index: 1; font: 700 40px Georgia, 'Songti SC', SimSun, serif; margin: 17px 0 10px; }
.program-intro p { position: relative; z-index: 1; color: #b6cacc; font-size: 14px; line-height: 1.8; }
.program-layout { display: grid; grid-template-columns: 200px minmax(0, 1fr); gap: 22px; align-items: start; }
.program-days { position: sticky; top: 104px; display: grid; background: #fff; border: 1px solid #e1e8e5; }
.day-link { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 21px 20px; color: #5f737c; text-decoration: none; border-bottom: 1px solid #e8ecea; transition: color .2s ease, background-color .2s ease; }
.day-link:last-child { border-bottom: 0; }
.day-link strong { font: 700 17px Georgia, serif; }
.day-link span { font-size: 12px; }
.day-link:hover, .day-link.active { color: #1d4555; background: #f2f5f2; }
.day-link.active { box-shadow: inset 3px 0 #b3926d; }
.program-groups { display: grid; gap: 22px; min-width: 0; }
.day-card { background: #fff; border: 1px solid #e1e8e5; scroll-margin-top: 100px; }
.day-header { display: flex; align-items: center; gap: 22px; padding: 25px 30px; background: #edeae2; border-bottom: 1px solid #dce2db; }
.day-header > span { color: #a37f58; font: 700 12px Georgia, serif; letter-spacing: .13em; }
.day-header h2 { color: #1b3b48; font: 700 25px Georgia, serif; }
.day-header small { color: #829198; font-size: 12px; }
.agenda-row { display: grid; grid-template-columns: 100px minmax(0, 1fr); gap: 24px; padding: 24px 30px; border-bottom: 1px solid #e8ecea; transition: background-color .2s ease; }
.agenda-row:last-child { border-bottom: 0; }
.agenda-row:hover { background: #f8f9f6; }
.agenda-time { display: flex; flex-direction: column; gap: 4px; border-right: 1px solid #dce4df; }
.agenda-time strong { color: #a27e58; font: 700 20px Georgia, serif; }
.agenda-time small { color: #9caaad; font-size: 12px; }
.agenda-content { min-width: 0; }
.agenda-type { display: inline-block; color: #6e827f; font-size: 11px; font-weight: 700; letter-spacing: .05em; }
.agenda-type.keynote { color: #a17e54; }
.agenda-type.ceremony { color: #37647a; }
.agenda-content h3 { color: #1d3d4a; font: 700 18px/1.5 Georgia, 'Songti SC', SimSun, serif; margin: 6px 0; }
.agenda-content p { display: flex; flex-wrap: wrap; gap: 4px 18px; color: #84949a; font-size: 12px; }
@media (max-width: 760px) {
  .program-intro { padding: 34px 26px; }
  .program-intro h1 { font-size: 32px; }
  .program-layout { grid-template-columns: 1fr; }
  .program-days { top: 58px; display: flex; overflow-x: auto; z-index: 10; }
  .day-link { flex: 1 0 auto; min-width: 112px; padding: 13px 15px; }
  .day-link.active { box-shadow: inset 0 -3px #b3926d; }
  .day-header { padding: 20px; }
  .agenda-row { grid-template-columns: 68px minmax(0, 1fr); gap: 14px; padding: 20px; }
  .agenda-time strong { font-size: 16px; }
  .agenda-content h3 { font-size: 16px; }
}
</style>
