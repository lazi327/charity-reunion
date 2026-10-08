<template>
  <div class="page-card">
    <el-space wrap style="margin-bottom: 12px">
      <el-select v-model="query.status" placeholder="状态" clearable style="width: 130px" @change="reload">
        <el-option label="待审核" :value="0" />
        <el-option label="已发布" :value="1" />
        <el-option label="已驳回" :value="2" />
      </el-select>
      <el-input v-model="query.keyword" placeholder="失踪人姓名/失踪地区" clearable style="width: 200px"
                @keyup.enter="reload" @clear="reload" />
      <el-button type="primary" @click="reload">查询</el-button>
    </el-space>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="失踪人" min-width="150">
        <template #default="{ row }">
          <el-image :src="row.photo" fit="cover" style="width: 40px; height: 40px; border-radius: 6px; vertical-align: middle"
                    :preview-src-list="row.photo ? [row.photo] : []" preview-teleported>
            <template #error><div class="ph"><el-icon><User /></el-icon></div></template>
          </el-image>
          <span style="margin-left: 8px">{{ row.name }}</span>
          <el-tag v-if="row.topFlag" type="danger" size="small" style="margin-left: 6px">置顶</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="关系/性别" width="110">
        <template #default="{ row }">{{ row.relation }} / {{ row.gender === 1 ? '男' : '女' }}</template>
      </el-table-column>
      <el-table-column prop="lostPlace" label="失踪地区" width="140" />
      <el-table-column prop="publisherName" label="发布人" width="110" />
      <el-table-column prop="viewCount" label="浏览量" width="80" />
      <el-table-column label="发布时间" width="150">
        <template #default="{ row }">{{ fmt(row.createTime) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="['warning', 'success', 'danger'][row.status]">
            {{ ['待审核', '已发布', '已驳回'][row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="270" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="showDetail(row)">详情</el-button>
          <template v-if="row.status === 0">
            <el-button size="small" type="success" @click="audit(row, 1)">通过</el-button>
            <el-button size="small" type="danger" plain @click="audit(row, 2)">驳回</el-button>
          </template>
          <template v-else>
            <el-button size="small" type="warning" plain @click="toggleTop(row)">
              {{ row.topFlag ? '取消置顶' : '置顶' }}
            </el-button>
          </template>
          <el-button size="small" type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px; justify-content: flex-end" layout="total, prev, pager, next"
                   :total="total" :page-size="query.size" v-model:current-page="query.page"
                   @current-change="load" />

    <el-drawer v-model="drawer" title="寻亲信息详情" size="480px">
      <template v-if="current">
        <el-image v-if="current.photo" :src="current.photo" fit="contain"
                  style="width: 100%; max-height: 240px; border-radius: 8px; margin-bottom: 12px"
                  :preview-src-list="[current.photo]" preview-teleported />
        <el-descriptions :column="1" border>
          <el-descriptions-item label="失踪人姓名">{{ current.name }}</el-descriptions-item>
          <el-descriptions-item label="与寻亲者关系">{{ current.relation }}</el-descriptions-item>
          <el-descriptions-item label="性别">{{ current.gender === 1 ? '男' : '女' }}</el-descriptions-item>
          <el-descriptions-item label="出生年份">{{ current.birthYear || '-' }} 年</el-descriptions-item>
          <el-descriptions-item label="失踪地区">{{ current.lostPlace }}</el-descriptions-item>
          <el-descriptions-item label="失踪日期">{{ current.lostDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="体貌特征">{{ current.features || '-' }}</el-descriptions-item>
          <el-descriptions-item label="寻亲故事">{{ current.story || '-' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ current.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ current.publisherName }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ ['待审核', '已发布', '已驳回'][current.status] }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const drawer = ref(false)
const current = ref(null)
const query = ref({ page: 1, size: 10, status: null, keyword: '' })

const fmt = t => (t || '').replace('T', ' ').slice(0, 16)

async function load() {
  loading.value = true
  try {
    const params = { page: query.value.page, size: query.value.size }
    if (query.value.status !== null && query.value.status !== '') params.status = query.value.status
    if (query.value.keyword) params.keyword = query.value.keyword
    const data = await request.get('/api/admin/cases', { params })
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
  await request.put(`/api/admin/cases/${row.id}/audit`, null, { params: { status } })
  ElMessage.success(status === 1 ? '已通过并发布' : '已驳回')
  load()
}

async function toggleTop(row) {
  await request.put(`/api/admin/cases/${row.id}/top`, null, { params: { top: !row.topFlag } })
  ElMessage.success(row.topFlag ? '已取消置顶' : '已置顶,将展示在列表最前')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除「${row.name}」的寻亲信息吗?相关线索也将无法查看。`, '提示', { type: 'warning' })
  await request.delete(`/api/admin/cases/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>

<style scoped>
.ph {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fa;
  font-size: 18px;
}
</style>
