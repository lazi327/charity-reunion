<template>
  <div class="page-card">
    <el-space wrap style="margin-bottom: 12px">
      <el-button type="primary" @click="openDialog()">新增轮播图</el-button>
      <span style="color: #909399; font-size: 13px">轮播图展示在用户首页顶部,按排序值升序显示</span>
    </el-space>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="图片" width="200">
        <template #default="{ row }">
          <el-image :src="row.image" fit="cover" style="width: 170px; height: 60px; border-radius: 6px"
                    :preview-src-list="[row.image]" preview-teleported />
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="140" />
      <el-table-column prop="linkUrl" label="跳转地址" min-width="160">
        <template #default="{ row }">{{ row.linkUrl || '-' }}</template>
      </el-table-column>
      <el-table-column prop="sortOrder" label="排序" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '展示中' : '已下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openDialog(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggleStatus(row)">
            {{ row.status === 1 ? '下架' : '上架' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑轮播图' : '新增轮播图'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="如:千里寻亲,回家团圆" />
        </el-form-item>
        <el-form-item label="图片">
          <div>
            <el-upload :show-file-list="false" accept="image/*" :http-request="doUpload">
              <el-button :loading="uploading"><el-icon><Upload /></el-icon> 上传图片</el-button>
            </el-upload>
            <el-image v-if="form.image" :src="form.image" fit="cover"
                      style="width: 220px; height: 80px; border-radius: 6px; margin-top: 8px; display: block" />
          </div>
        </el-form-item>
        <el-form-item label="跳转地址">
          <el-input v-model="form.linkUrl" placeholder="点击轮播图跳转的链接,如 /cases 或 https://..." />
        </el-form-item>
        <el-form-item label="排序值">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
          <span style="color: #909399; font-size: 12px; margin-left: 8px">越小越靠前</span>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0"
                     active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../utils/request'

const rows = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const uploading = ref(false)
const form = ref({})

async function load() {
  loading.value = true
  try {
    rows.value = await request.get('/api/admin/banners')
  } finally {
    loading.value = false
  }
}

function openDialog(row) {
  form.value = row ? { ...row } : { id: null, title: '', image: '', linkUrl: '', sortOrder: rows.value.length + 1, status: 1 }
  dialog.value = true
}

async function doUpload({ file }) {
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    const data = await request.post('/api/upload', fd)
    form.value.image = data.url || data
  } finally {
    uploading.value = false
  }
}

async function save() {
  if (!form.value.title || !form.value.image) {
    ElMessage.warning('请填写标题并上传图片')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await request.put(`/api/admin/banners/${form.value.id}`, form.value)
    } else {
      await request.post('/api/admin/banners', form.value)
    }
    ElMessage.success('保存成功')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row) {
  await request.put(`/api/admin/banners/${row.id}`, { ...row, status: row.status === 1 ? 0 : 1 })
  ElMessage.success(row.status === 1 ? '已下架' : '已上架')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除轮播图「${row.title}」吗?`, '提示', { type: 'warning' })
  await request.delete(`/api/admin/banners/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
</script>
