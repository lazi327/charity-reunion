<template>
  <div class="page-card">
    <el-space wrap style="margin-bottom: 12px">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="load">
        <el-option label="待审核" :value="0" />
        <el-option label="正常" :value="1" />
        <el-option label="未通过" :value="2" />
        <el-option label="已停用" :value="3" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="昵称/账号搜索" clearable style="width: 200px"
                @keyup.enter="reload" @clear="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
    </el-space>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="寻亲者" min-width="180">
        <template #default="{ row }">
          <el-avatar :size="32" :src="row.avatar" style="background: #e8d3bd; vertical-align: middle">
            {{ row.nickname?.[0] }}
          </el-avatar>
          <span style="margin-left: 8px">{{ row.nickname }}</span>
          <span style="color: #c0c4cc; font-size: 12px"> ({{ row.username }})</span>
        </template>
      </el-table-column>
      <el-table-column label="性别/年龄" width="100">
        <template #default="{ row }">{{ row.gender === 1 ? '男' : '女' }} / {{ row.age || '-' }}岁</template>
      </el-table-column>
      <el-table-column prop="city" label="城市" width="100" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="createTime" label="注册时间" width="160">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)">详情</el-button>
          <template v-if="row.status === 0">
            <el-button size="small" type="success" @click="audit(row, 1)">通过</el-button>
            <el-button size="small" type="danger" plain @click="audit(row, 2)">不通过</el-button>
          </template>
          <template v-else-if="row.status === 1">
            <el-button size="small" type="warning" plain @click="toggle(row, 3)">停用</el-button>
          </template>
          <template v-else-if="row.status === 3">
            <el-button size="small" type="success" plain @click="toggle(row, 1)">启用</el-button>
          </template>
          <template v-else-if="row.status === 2">
            <el-button size="small" type="success" @click="audit(row, 1)">恢复</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px; justify-content: flex-end" layout="total, prev, pager, next"
                   :total="total" :page-size="query.size" v-model:current-page="query.page"
                   @current-change="load" />

    <el-drawer v-model="drawer" title="用户详情" size="400px">
      <el-descriptions v-if="current" :column="1" border>
        <el-descriptions-item label="昵称">{{ current.nickname }}</el-descriptions-item>
        <el-descriptions-item label="账号">{{ current.username }}</el-descriptions-item>
        <el-descriptions-item label="性别">{{ current.gender === 1 ? '男' : '女' }}</el-descriptions-item>
        <el-descriptions-item label="年龄">{{ current.age || '-' }} 岁({{ current.birthYear || '-' }}年)</el-descriptions-item>
        <el-descriptions-item label="城市">{{ current.city }}</el-descriptions-item>
        <el-descriptions-item label="电话">{{ current.phone }}</el-descriptions-item>
        <el-descriptions-item label="注册时间">{{ fmt(current.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="联系说明">{{ current.aboutMe }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const drawer = ref(false)
const current = ref(null)
const query = ref({ page: 1, size: 10, status: null, keyword: '' })

const statusText = s => ['待审核', '正常', '未通过', '已停用'][s] || s
const statusType = s => ['warning', 'success', 'info', 'danger'][s] || 'info'
const fmt = t => (t || '').replace('T', ' ').slice(0, 16)

async function load() {
  loading.value = true
  try {
    const params = { page: query.value.page, size: query.value.size }
    if (query.value.status !== null && query.value.status !== '') params.status = query.value.status
    if (query.value.keyword) params.keyword = query.value.keyword
    const data = await request.get('/api/admin/users', { params })
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

function showDetail(row) {
  current.value = row
  drawer.value = true
}

async function audit(row, status) {
  await request.put(`/api/admin/users/${row.id}/audit`, null, { params: { status } })
  ElMessage.success(status === 1 ? '已通过审核' : '已标记审核不通过')
  load()
}

async function toggle(row, status) {
  await request.put(`/api/admin/users/${row.id}/status`, null, { params: { status } })
  ElMessage.success(status === 3 ? '账号已停用' : '账号已启用')
  load()
}

onMounted(load)
</script>
