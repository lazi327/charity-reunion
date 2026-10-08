<template>
  <div class="page-card">
    <el-space wrap style="margin-bottom: 12px">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="reload">
        <el-option label="正常" :value="0" />
        <el-option label="已屏蔽" :value="1" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <span style="color: #909399; font-size: 13px">对不合规的问题或回复进行屏蔽,情节严重的可直接删除</span>
    </el-space>

    <el-table :data="rows" v-loading="loading" stripe @expand-change="onExpand">
      <el-table-column type="expand">
        <template #default="{ row }">
          <div style="padding: 8px 40px">
            <div v-for="a in answersOf(row.id)" :key="a.id" class="answer-row">
              <div class="answer-main">
                <b>{{ a.userName }}</b>
                <span style="color: #c0c4cc; font-size: 12px; margin-left: 8px">{{ fmt(a.createTime) }}</span>
                <el-tag v-if="a.status === 1" type="danger" size="small" style="margin-left: 8px">已屏蔽</el-tag>
                <p style="margin: 4px 0 0">{{ a.content }}</p>
              </div>
              <el-space>
                <el-button size="small" plain @click="toggleAnswer(a)">
                  {{ a.status === 1 ? '恢复' : '屏蔽' }}
                </el-button>
                <el-button size="small" type="danger" plain @click="removeAnswer(a, row)">删除</el-button>
              </el-space>
            </div>
            <p v-if="!answersMap[row.id]?.length" style="color: #909399">暂无回复</p>
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="问题标题" min-width="220" show-overflow-tooltip />
      <el-table-column prop="userName" label="提问者" width="120" />
      <el-table-column label="回复数" width="80">
        <template #default="{ row }">{{ row.answerCount ?? 0 }}</template>
      </el-table-column>
      <el-table-column prop="viewCount" label="浏览量" width="80" />
      <el-table-column label="发布时间" width="150">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'danger' : 'success'">{{ row.status === 1 ? '已屏蔽' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :type="row.status === 1 ? 'success' : 'warning'" plain @click="toggleQuestion(row)">
            {{ row.status === 1 ? '恢复' : '屏蔽' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="removeQuestion(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px; justify-content: flex-end" layout="total, prev, pager, next"
                   :total="total" :page-size="query.size" v-model:current-page="query.page"
                   @current-change="load" />
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const answersMap = ref({})
const query = ref({ page: 1, size: 10, status: null })

const fmt = t => (t || '').replace('T', ' ').slice(0, 16)
const answersOf = id => answersMap.value[id] || []

async function load() {
  loading.value = true
  try {
    const params = { page: query.value.page, size: query.value.size }
    if (query.value.status !== null && query.value.status !== '') params.status = query.value.status
    const data = await request.get('/api/admin/questions', { params })
    rows.value = data.records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  load()
}

async function toggleQuestion(row) {
  const status = row.status === 1 ? 0 : 1
  await request.put(`/api/admin/questions/${row.id}/status`, null, { params: { status } })
  ElMessage.success(status === 1 ? '问题已屏蔽' : '问题已恢复')
  load()
}

async function removeQuestion(row) {
  await ElMessageBox.confirm(`确定删除问题「${row.title}」吗?其下所有回复将一并删除。`, '提示', { type: 'warning' })
  await request.delete(`/api/admin/questions/${row.id}`)
  ElMessage.success('已删除')
  load()
}

async function loadAnswers(row) {
  const data = await request.get(`/api/admin/questions/${row.id}/answers`)
  answersMap.value[row.id] = data
}

async function toggleAnswer(a) {
  const status = a.status === 1 ? 0 : 1
  await request.put(`/api/admin/answers/${a.id}/status`, null, { params: { status } })
  ElMessage.success(status === 1 ? '回复已屏蔽' : '回复已恢复')
  loadAnswers({ id: a.questionId })
}

function onExpand(row) {
  if (!answersMap.value[row.id]) loadAnswers(row)
}

onMounted(load)

async function removeAnswer(a, row) {
  await ElMessageBox.confirm('确定删除该条回复吗?', '提示', { type: 'warning' })
  await request.delete(`/api/admin/answers/${a.id}`)
  ElMessage.success('已删除')
  loadAnswers(row)
  load()
}

</script>

<style scoped>
.answer-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 8px 0;
  border-bottom: 1px dashed #ebeef5;
}
.answer-main {
  flex: 1;
}
</style>
