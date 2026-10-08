<template>
  <div v-loading="loading">
    <div class="head-bar">
      <span class="head-title"><el-icon><DataLine /></el-icon> 平台运营总览</span>
      <el-button type="warning" plain @click="openLogs"><el-icon><Lock /></el-icon> 登录安全日志</el-button>
    </div>
    <el-row :gutter="16">
      <el-col :span="6" v-for="c in cards" :key="c.label">
        <div class="stat-card" :style="{ borderTop: '3px solid ' + c.color }">
          <div class="num" :style="{ color: c.color }">{{ c.value }}</div>
          <div class="label">{{ c.label }}</div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card" style="margin-top: 16px">
      <h3 style="margin-top: 0"><el-icon><Tickets /></el-icon> 待办提醒</h3>
      <p v-if="stats.pendingUsers > 0" style="color: #e6a23c">
        有 <b>{{ stats.pendingUsers }}</b> 位寻亲者等待注册审核 →
        <el-link type="primary" @click="$router.push('/admin/users')">去审核</el-link>
      </p>
      <p v-if="stats.pendingCases > 0" style="color: #e6a23c">
        有 <b>{{ stats.pendingCases }}</b> 条寻亲信息等待审核 →
        <el-link type="primary" @click="$router.push('/admin/cases')">去审核</el-link>
      </p>
      <p v-if="stats.pendingClues > 0" style="color: #e6a23c">
        收到 <b>{{ stats.pendingClues }}</b> 条寻亲线索待处理 →
        <el-link type="primary" @click="$router.push('/admin/clues')">去查看</el-link>
      </p>
      <p v-if="!pendingTotal" style="color: #67c23a">暂无待办事项,一切顺利</p>
    </div>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="page-card chart-card">
          <h4><el-icon><MapLocation /></el-icon> 失踪人口地区分布</h4>
          <div ref="regionEl" class="chart"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card chart-card">
          <h4><el-icon><TrendCharts /></el-icon> 寻亲线索提供趋势(近12个月)</h4>
          <div ref="trendEl" class="chart"></div>
        </div>
      </el-col>
    </el-row>
    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <div class="page-card chart-card">
          <h4><el-icon><PieChart /></el-icon> 寻亲信息类型分布</h4>
          <div ref="typeEl" class="chart"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="page-card chart-card">
          <h4><el-icon><ChatDotRound /></el-icon> 问答互动情况</h4>
          <div ref="qaEl" class="chart"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 登录安全日志抽屉 -->
    <el-drawer v-model="logsVisible" title="登录安全日志" size="620px" @open="loadLogs">
      <div class="log-search">
        <el-input v-model="logKeyword" placeholder="按用户名筛选" clearable style="width: 220px"
                  @keyup.enter="reloadLogs" @clear="reloadLogs">
          <template #append>
            <el-button :icon="Search" @click="reloadLogs" />
          </template>
        </el-input>
        <el-radio-group v-model="logFilter" style="margin-left: 12px" @change="onFilterChange">
          <el-radio-button :value="''">全部</el-radio-button>
          <el-radio-button :value="1">成功</el-radio-button>
          <el-radio-button :value="0">失败</el-radio-button>
        </el-radio-group>
      </div>
      <el-table :data="logs" v-loading="logsLoading" size="small" style="margin-top: 12px">
        <el-table-column prop="username" label="账号" width="110" />
        <el-table-column prop="ip" label="IP" width="120" show-overflow-tooltip />
        <el-table-column label="结果" width="70">
          <template #default="{ row }">
            <el-tag :type="row.success === 1 ? 'success' : 'danger'" size="small" effect="light">
              {{ row.success === 1 ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="msg" label="描述" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="150" />
      </el-table>
      <el-pagination class="log-page" layout="total, prev, pager, next" :total="logTotal"
                     :page-size="logSize" :current-page="logPage" @current-change="p => { logPage = p; loadLogs() }" />
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onBeforeUnmount, ref } from 'vue'
import * as echarts from 'echarts'
import { Search } from '@element-plus/icons-vue'
import request from '../../utils/request'

const stats = ref({})
const loading = ref(true)
const regionEl = ref(null)
const trendEl = ref(null)
const typeEl = ref(null)
const qaEl = ref(null)
let charts = []

const cards = computed(() => [
  { label: '寻亲者总数', value: stats.value.totalUsers ?? '-', color: '#409eff' },
  { label: '待审核用户', value: stats.value.pendingUsers ?? '-', color: '#e6a23c' },
  { label: '寻亲信息总数', value: stats.value.totalCases ?? '-', color: '#67c23a' },
  { label: '已发布信息', value: stats.value.publishedCases ?? '-', color: '#14b8a6' },
  { label: '待审核信息', value: stats.value.pendingCases ?? '-', color: '#f56c6c' },
  { label: '寻亲线索总数', value: stats.value.totalClues ?? '-', color: '#8b5cf6' },
  { label: '寻亲指南', value: stats.value.totalGuides ?? '-', color: '#ec4899' },
  { label: '私信消息数', value: stats.value.totalMessages ?? '-', color: '#909399' }
])

const pendingTotal = computed(() =>
  (stats.value.pendingUsers ?? 0) + (stats.value.pendingCases ?? 0) + (stats.value.pendingClues ?? 0))

/* ---------- 登录安全日志抽屉 ---------- */
const logsVisible = ref(false)
const logs = ref([])
const logsLoading = ref(false)
const logKeyword = ref('')
const logFilter = ref('')
const logPage = ref(1)
const logSize = 15
const logTotal = ref(0)

function openLogs() {
  logPage.value = 1
  logsVisible.value = true
}
function reloadLogs() {
  logPage.value = 1
  loadLogs()
}
function onFilterChange(v) {
  logSuccessFilter.value = v === '' ? undefined : Number(v)
  reloadLogs()
}
const logSuccessFilter = ref(undefined)

async function loadLogs() {
  logsLoading.value = true
  try {
    const params = { page: logPage.value, size: logSize }
    if (logKeyword.value) params.keyword = logKeyword.value
    if (logSuccessFilter.value !== undefined) params.success = logSuccessFilter.value
    const data = await request.get('/api/admin/login-logs', { params })
    logs.value = data.records || []
    logTotal.value = data.total || 0
  } finally {
    logsLoading.value = false
  }
}

function renderBar(el, title, map, color) {
  const entries = Object.entries(map || {}).sort((a, b) => b[1] - a[1])
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 60 },
    xAxis: { type: 'category', data: entries.map(e => e[0]), axisLabel: { rotate: 35 } },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: entries.map(e => e[1]), barMaxWidth: 32, itemStyle: { color, borderRadius: [4, 4, 0, 0] } }]
  })
  charts.push(chart)
}

function renderLine(el, map) {
  const entries = Object.entries(map || {}).sort((a, b) => a[0].localeCompare(b[0]))
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 40 },
    xAxis: { type: 'category', data: entries.map(e => e[0]) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'line', smooth: true, data: entries.map(e => e[1]),
      itemStyle: { color: '#8b5cf6' }, areaStyle: { opacity: 0.15 }
    }]
  })
  charts.push(chart)
}

function renderPie(el, data, colors) {
  const chart = echarts.init(el)
  chart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0 },
    color: colors,
    series: [{ type: 'pie', radius: ['40%', '65%'], center: ['50%', '45%'], data }]
  })
  charts.push(chart)
}

async function loadCharts() {
  const data = await request.get('/api/admin/charts')
  await nextTick()
  renderBar(regionEl.value, '地区', data.regionDist, '#409eff')
  renderLine(trendEl.value, data.clueTrend)
  renderBar(typeEl.value, '类型', data.typeDist, '#67c23a')
  renderPie(qaEl.value, [
    { name: '问答问题', value: stats.value.totalQuestions ?? 0 },
    { name: '回复数', value: stats.value.totalAnswers ?? 0 }
  ], ['#e6a23c', '#409eff'])
}

function resize() {
  charts.forEach(c => c.resize())
}

onMounted(async () => {
  try {
    stats.value = await request.get('/api/admin/stats')
    await loadCharts()
    window.addEventListener('resize', resize)
  } finally {
    loading.value = false
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  charts.forEach(c => c.dispose())
})
</script>

<style scoped>
.head-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}
.head-title {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
}
.log-search {
  display: flex;
  align-items: center;
}
.log-page {
  margin-top: 12px;
  justify-content: flex-end;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px;
  text-align: center;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  margin-bottom: 16px;
}
.num {
  font-size: 30px;
  font-weight: 700;
}
.label {
  color: #909399;
  margin-top: 6px;
  font-size: 14px;
}
.chart-card {
  margin-top: 0;
}
.chart-card h4 {
  margin: 0 0 8px;
  color: #303133;
}
.chart {
  height: 280px;
}
</style>
