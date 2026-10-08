<template>
  <div class="page-card">
    <b>我的寻亲</b>
    <el-tabs v-model="tab" style="margin-top: 10px">
      <!-- 我发布的寻亲信息 -->
      <el-tab-pane :label="`我发布的信息(${myCases.length})`" name="cases">
        <el-empty v-if="myCases.length === 0" description="还没有发布寻亲信息" :image-size="60">
          <el-button type="danger" @click="$router.push('/cases/new')">立即发布</el-button>
        </el-empty>
        <el-table v-else :data="myCases" stripe>
          <el-table-column prop="name" label="失踪人" width="110" />
          <el-table-column prop="relation" label="类型" width="110" />
          <el-table-column prop="lostPlace" label="失踪地区" width="130" />
          <el-table-column prop="lostDate" label="失踪日期" width="110" />
          <el-table-column prop="viewCount" label="浏览量" width="80" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" size="small">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="发布时间" min-width="150">
            <template #default="{ row }">{{ fmt(row.createTime) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="170">
            <template #default="{ row }">
              <el-button size="small" @click="$router.push('/cases/' + row.id)">查看</el-button>
              <el-button size="small" type="primary" plain @click="$router.push('/cases/' + row.id + '/edit')">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 我提供的线索 -->
      <el-tab-pane :label="`我提供的线索(${myClues.length})`" name="clues">
        <el-empty v-if="myClues.length === 0" description="还没有提供过线索,去档案大厅看看吧" :image-size="60">
          <el-button type="danger" @click="$router.push('/cases')">去逛档案大厅</el-button>
        </el-empty>
        <el-table v-else :data="myClues" stripe>
          <el-table-column prop="caseName" label="关联档案" width="110" />
          <el-table-column prop="content" label="线索内容" min-width="280" show-overflow-tooltip />
          <el-table-column label="处理状态" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success" size="small">有效</el-tag>
              <el-tag v-else-if="row.status === 2" type="info" size="small">虚假</el-tag>
              <el-tag v-else type="warning" size="small">待发布人处理</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="提供时间" width="160">
            <template #default="{ row }">{{ fmt(row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 我收到的线索(我发布的信息下) -->
      <el-tab-pane :label="`收到的线索(${received.length})`" name="received">
        <el-empty v-if="received.length === 0" description="您的档案下暂时还没有线索" :image-size="60" />
        <el-table v-else :data="received" stripe>
          <el-table-column prop="caseName" label="我的档案" width="110" />
          <el-table-column prop="content" label="线索内容" min-width="240" show-overflow-tooltip />
          <el-table-column prop="userName" label="提供人" width="110" />
          <el-table-column prop="contact" label="联络方式" width="150" />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <el-tag v-if="row.status === 1" type="success" size="small">有效</el-tag>
              <el-tag v-else-if="row.status === 2" size="small">虚假</el-tag>
              <el-space v-else>
                <el-button size="small" type="success" plain @click="mark(row, 1)">有效</el-button>
                <el-button size="small" type="info" plain @click="mark(row, 2)">虚假</el-button>
              </el-space>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const tab = ref('cases')
const myCases = ref([])
const myClues = ref([])
const received = ref([])

const statusText = s => ({ 0: '审核中', 1: '已发布', 2: '已驳回' })[s] || '未知'
const statusType = s => ({ 0: 'warning', 1: 'success', 2: 'danger' })[s] || 'info'
const fmt = t => (t || '').replace('T', ' ').slice(0, 16)

async function load() {
  const [a, b, c] = await Promise.all([
    request.get('/api/cases/my'),
    request.get('/api/clues/my'),
    request.get('/api/clues/received')
  ])
  myCases.value = a
  myClues.value = b
  received.value = c
}

async function mark(cl, status) {
  await request.put(`/api/clues/${cl.id}/status?status=${status}`)
  cl.status = status
  ElMessage.success(status === 1 ? '已标记为有效线索' : '已标记为虚假线索')
}

onMounted(load)
</script>
