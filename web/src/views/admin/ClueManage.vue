<template>
  <div class="page-card">
    <el-space wrap style="margin-bottom: 12px">
      <el-select v-model="query.status" placeholder="线索状态" clearable style="width: 140px" @change="reload">
        <el-option label="待处理" :value="0" />
        <el-option label="有效" :value="1" />
        <el-option label="虚假" :value="2" />
      </el-select>
      <el-button type="primary" @click="reload">查询</el-button>
      <span style="color: #909399; font-size: 13px">管理员可核查线索真实性,对虚假线索进行删除</span>
    </el-space>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="caseName" label="相关寻亲信息" width="120" />
      <el-table-column prop="userName" label="提供人" width="120" />
      <el-table-column prop="content" label="线索内容" min-width="240" show-overflow-tooltip />
      <el-table-column prop="contact" label="联系方式" width="130">
        <template #default="{ row }">{{ row.contact || '-' }}</template>
      </el-table-column>
      <el-table-column label="提交时间" width="150">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['info', 'success', 'danger'][row.status] || 'info'">
            {{ ['待处理', '有效', '虚假'][row.status] || '待处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="110" fixed="right">
        <template #default="{ row }">
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
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
const query = ref({ page: 1, size: 10, status: null })

const fmt = t => (t || '').replace('T', ' ').slice(0, 16)

async function load() {
  loading.value = true
  try {
    const params = { page: query.value.page, size: query.value.size }
    if (query.value.status !== null && query.value.status !== '') params.status = query.value.status
    const data = await request.get('/api/admin/clues', { params })
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

async function remove(row) {
  await ElMessageBox.confirm(
    `确定删除 ${row.userName} 对「${row.caseName}」提供的线索吗?删除后寻亲者也将无法查看该线索。`,
    '删除虚假线索', { type: 'warning' })
  await request.delete(`/api/admin/clues/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
