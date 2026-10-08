<template>
  <div class="page-card">
    <el-space wrap style="margin-bottom: 12px">
      <el-button type="primary" @click="openEditor()">发布新指南</el-button>
      <span style="color: #909399; font-size: 13px">指南内容支持富文本排版,发布后展示在用户端"寻亲指南"栏目</span>
    </el-space>

    <el-table :data="rows" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" min-width="220" />
      <el-table-column label="封面" width="110">
        <template #default="{ row }">
          <el-image v-if="row.cover" :src="row.cover" fit="cover" style="width: 80px; height: 44px; border-radius: 6px" />
          <span v-else style="color: #c0c4cc">-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '已下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" width="150">
        <template #default="{ row }">{{ fmt(row.updateTime) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="230" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEditor(row)">编辑</el-button>
          <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" plain @click="toggle(row)">
            {{ row.status === 1 ? '下架' : '发布' }}
          </el-button>
          <el-button size="small" type="danger" plain @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialog" :title="form.id ? '编辑寻亲指南' : '发布寻亲指南'" width="860px"
               top="4vh" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="指南标题,如:亲人走失怎么办?黄金72小时自救指南" />
        </el-form-item>
        <el-form-item label="封面图">
          <div>
            <el-upload :show-file-list="false" accept="image/*" :http-request="uploadCover">
              <el-button :loading="coverUploading"><el-icon><Upload /></el-icon> 上传封面(可选)</el-button>
            </el-upload>
            <el-image v-if="form.cover" :src="form.cover" fit="cover"
                      style="width: 160px; height: 60px; border-radius: 6px; margin-top: 8px; display: block" />
          </div>
        </el-form-item>
        <el-form-item label="正文">
          <div style="border: 1px solid #dcdfe6; width: 100%; z-index: 2000">
            <Toolbar :editor="editorRef" :defaultConfig="toolbarConfig" mode="default" style="border-bottom: 1px solid #dcdfe6" />
            <Editor :defaultConfig="editorConfig" mode="default" v-model="form.content"
                    style="height: 380px; overflow-y: hidden"
                    @onCreated="el => editorRef = el" />
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存并发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, shallowRef } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import '@wangeditor/editor/dist/css/style.css'
import request from '../../utils/request'

const rows = ref([])
const loading = ref(false)
const dialog = ref(false)
const saving = ref(false)
const coverUploading = ref(false)
const form = ref({})
const editorRef = shallowRef()

const toolbarConfig = { excludeKeys: ['group-video', 'fullScreen'] }
const editorConfig = {
  placeholder: '请输入指南正文内容,支持标题/加粗/列表/引用/图片等排版…',
  MENU_CONF: {
    uploadImage: {
      async customUpload(file, insertFn) {
        const fd = new FormData()
        fd.append('file', file)
        const url = await request.post('/api/upload', fd)
        insertFn(url, file.name, url)
      }
    }
  }
}

const fmt = t => (t || '').replace('T', ' ').slice(0, 16)

async function load() {
  loading.value = true
  try {
    rows.value = await request.get('/api/admin/guides')
  } finally {
    loading.value = false
  }
}

function openEditor(row) {
  form.value = row ? { ...row } : { id: null, title: '', cover: '', content: '', status: 1 }
  dialog.value = true
}

async function uploadCover({ file }) {
  coverUploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', file)
    form.value.cover = await request.post('/api/upload', fd)
  } finally {
    coverUploading.value = false
  }
}

async function save() {
  const text = (form.value.content || '').replace(/<[^>]+>/g, '').trim()
  if (!form.value.title || !text) {
    ElMessage.warning('请填写标题和正文内容')
    return
  }
  saving.value = true
  try {
    if (form.value.id) {
      await request.put(`/api/admin/guides/${form.value.id}`, form.value)
    } else {
      await request.post('/api/admin/guides', form.value)
    }
    ElMessage.success('已保存')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggle(row) {
  await request.put(`/api/admin/guides/${row.id}`, { ...row, status: row.status === 1 ? 0 : 1 })
  ElMessage.success(row.status === 1 ? '已下架' : '已发布')
  load()
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除指南「${row.title}」吗?`, '提示', { type: 'warning' })
  await request.delete(`/api/admin/guides/${row.id}`)
  ElMessage.success('已删除')
  load()
}

onMounted(load)
onBeforeUnmount(() => editorRef.value?.destroy())
</script>
