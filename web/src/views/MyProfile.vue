<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <!-- 左侧:身份卡 + 完善度环 -->
      <el-col :span="7">
        <div class="page-card id-card fade-up">
          <el-avatar :size="86" :src="form.avatar" :style="{ background: 'var(--grad-hero)', fontSize: '32px' }">
            {{ form.nickname?.[0] || form.username?.[0] }}
          </el-avatar>
          <h3 style="margin: 10px 0 2px">{{ form.nickname || form.username }}</h3>
          <div class="meta">@{{ form.username }}</div>
          <div class="role-tag">{{ form.gender === 2 ? '她' : '他' }} · {{ form.city || '城市未填' }}</div>

          <!-- SVG 进度环 -->
          <div class="ring-box">
            <svg width="150" height="150" viewBox="0 0 150 150">
              <defs>
                <linearGradient id="ringGrad" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#c9784f" />
                  <stop offset="55%" stop-color="#b5573b" />
                  <stop offset="100%" stop-color="#8f3f2a" />
                </linearGradient>
              </defs>
              <circle cx="75" cy="75" r="62" fill="none" stroke="#efe4d2" stroke-width="12" />
              <circle cx="75" cy="75" r="62" fill="none" stroke="url(#ringGrad)" stroke-width="12"
                      stroke-linecap="round" :stroke-dasharray="CIRC" :stroke-dashoffset="dashOffset"
                      transform="rotate(-90 75 75)" class="ring-arc" />
              <text x="75" y="70" text-anchor="middle" class="ring-num">{{ pct }}</text>
              <text x="75" y="92" text-anchor="middle" class="ring-label">资料完善度</text>
            </svg>
            <div class="ring-comment">{{ ringComment }}</div>
          </div>

          <div class="check-list">
            <div v-for="f in fields" :key="f.key" class="cl-item">
              <span :class="f.done ? 'ok' : 'no'">{{ f.done ? '✓' : '○' }}</span>{{ f.label }}
            </div>
          </div>
        </div>

        <div class="page-card stat-card fade-up" style="animation-delay:.08s">
          <b><el-icon><Location /></el-icon> 我的足迹</b>
          <div class="st-row"><span><el-icon><Star /></el-icon> 收藏档案</span><b>{{ favCount }}</b></div>
          <div class="st-row"><span><el-icon><Reading /></el-icon> 已读指南</span><b>{{ readCount }}</b></div>
          <div class="st-row"><span><Lamp /> 点亮回家灯</span><b>{{ wishCount }}</b></div>
          <el-button text type="primary" @click="$router.push('/mycases')">查看我的寻亲数据 »</el-button>
        </div>
      </el-col>

      <!-- 右侧:资料编辑 -->
      <el-col :span="17">
        <div class="page-card fade-up" style="animation-delay:.04s; position: relative">
          <div v-if="dirty" class="dirty-flag">● 有未保存的修改</div>
          <b>个人资料设置</b>
          <el-form :model="form" label-width="100px" style="margin-top: 18px">
            <el-form-item label="头像">
              <el-upload class="avatar-uploader" action="/api/upload" name="file"
                         :headers="uploadHeaders" :show-file-list="false" :on-success="onUploaded"
                         accept="image/jpeg,image/png">
                <el-avatar v-if="form.avatar" :size="80" :src="form.avatar" />
                <el-button v-else :icon="'Plus'">上传</el-button>
              </el-upload>
              <span style="margin-left: 12px; color: #909399; font-size: 13px">支持 jpg/png,5MB 以内</span>
              <el-button v-if="form.avatar" text type="danger" size="small" style="margin-left: 8px"
                         @click="form.avatar = ''">移除</el-button>
            </el-form-item>
            <el-form-item label="账号">
              <el-input :model-value="form.username" disabled />
              <span class="tip">账号为登录凭证,不可修改</span>
            </el-form-item>
            <el-form-item label="昵称" required>
              <el-input v-model="form.nickname" maxlength="20" show-word-limit />
            </el-form-item>
            <el-form-item label="性别">
              <el-radio-group v-model="form.gender">
                <el-radio-button :value="1">男</el-radio-button>
                <el-radio-button :value="2">女</el-radio-button>
                <el-radio-button :value="0">保密</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="出生年份">
              <el-input-number v-model="form.birthYear" :min="1940" :max="2010" controls-position="right" />
              <span class="tip" v-if="form.birthYear">{{ nowYear - form.birthYear }} 岁</span>
            </el-form-item>
            <el-form-item label="所在城市">
              <el-input v-model="form.city" maxlength="30" placeholder="如:广东广州" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" maxlength="11" placeholder="11 位手机号" />
              <span class="tip" :class="phoneOk ? 'ok' : 'bad'" v-if="form.phone">
                {{ phoneOk ? '✓ 手机号格式正确' : '✗ 手机号格式不正确' }}
              </span>
            </el-form-item>
            <el-form-item label="联系说明">
              <el-input v-model="form.aboutMe" type="textarea" :rows="4" maxlength="500" show-word-limit
                        placeholder="方便联系您的时间段、联系方式偏好等,例如:白天上班请短信,晚 8 点后可通话" />
            </el-form-item>
            <el-form-item label="账号安全">
              <el-alert type="info" :closable="false" show-icon style="width: 100%">
                <template #title>忘记密码?可在登录页点击「找回密码」,凭账号 + 注册手机号验证后重置</template>
              </el-alert>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="saving" :disabled="!dirty" @click="save">保存修改</el-button>
              <el-button plain :disabled="!dirty" @click="revert">撤销改动</el-button>
              <el-button plain @click="$router.push('/mycases')">我的寻亲数据</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getToken, setAuth, getUser } from '../utils/auth'

const form = ref({})
const original = ref({})
const loading = ref(true)
const saving = ref(false)
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
const nowYear = new Date().getFullYear()

const dirty = computed(() => JSON.stringify(form.value) !== JSON.stringify(original.value))

/* 完善度:8 个可填字段逐项计分,权重不同 */
const FIELD_WEIGHTS = [
  ['nickname', 15], ['avatar', 10], ['gender', 10], ['birthYear', 15],
  ['city', 10], ['phone', 15], ['aboutMe', 15], ['username', 10]
]
const pct = computed(() => {
  let s = 0
  for (const [k, w] of FIELD_WEIGHTS) {
    const v = form.value[k]
    if (v !== null && v !== undefined && String(v) !== '' && !(k === 'gender' && v === 0)) s += w
  }
  return Math.min(100, s)
})
const CIRC = 2 * Math.PI * 62
const dashOffset = computed(() => CIRC * (1 - pct.value / 100))
const ringComment = computed(() => {
  const p = pct.value
  if (p >= 90) return '资料非常完整'
  if (p >= 70) return '再补充联系说明就更棒了'
  if (p >= 40) return '补全电话与城市,方便他人核实线索'
  return '完善资料能提升线索对接效率'
})
const fields = computed(() => [
  { key: 'nickname', label: '昵称', done: !!form.value.nickname },
  { key: 'avatar', label: '头像', done: !!form.value.avatar },
  { key: 'gender', label: '性别', done: form.value.gender === 1 || form.value.gender === 2 },
  { key: 'birthYear', label: '出生年份', done: !!form.value.birthYear },
  { key: 'city', label: '所在城市', done: !!form.value.city },
  { key: 'phone', label: '联系电话', done: phoneOk.value },
  { key: 'aboutMe', label: '联系说明', done: (form.value.aboutMe || '').length >= 10 },
  { key: 'username', label: '账号', done: true }
])

const phoneOk = computed(() => /^1[3-9]\d{9}$/.test(form.value.phone || ''))

/* 足迹统计(本地) */
const favCount = computed(() => JSON.parse(localStorage.getItem('fav_cases') || '[]').length)
const readCount = computed(() => JSON.parse(localStorage.getItem('guide_read') || '[]').length)
const wishCount = ref(0)

async function load() {
  try {
    form.value = await request.get('/api/users/me')
    original.value = JSON.parse(JSON.stringify(form.value))
    // 我点亮的灯统计:拉取我发布的档案,汇总其获得的回家灯数
    try {
      const recs = await request.get('/api/cases/my')
      wishCount.value = (recs || []).reduce((a, c) => a + (c.wishCount || 0), 0)
    } catch { wishCount.value = 0 }
  } finally {
    loading.value = false
  }
}

function onUploaded(res) {
  if (res.code === 0) {
    form.value.avatar = res.data
    ElMessage.success('头像已更新,记得点击保存')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

function revert() {
  form.value = JSON.parse(JSON.stringify(original.value))
  ElMessage.info('已撤销未保存的改动')
}

async function save() {
  if (form.value.phone && !phoneOk.value) {
    ElMessage.warning('手机号格式不正确,请检查后再保存')
    return
  }
  saving.value = true
  try {
    await request.put('/api/users/me', form.value)
    original.value = JSON.parse(JSON.stringify(form.value))
    // 同步导航栏显示
    setAuth(getToken(), { ...getUser(), nickname: form.value.nickname, avatar: form.value.avatar })
    ElMessage.success('保存成功,资料完善度 ' + pct.value + '%')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.id-card { text-align: center; }
.meta { font-size: 12px; color: #b1b3b8; }
.role-tag { display: inline-block; margin-top: 8px; background: #f6efe3; color: #8f3f2a;
  font-size: 12.5px; border-radius: 12px; padding: 3px 12px; }
.ring-box { margin: 10px auto 4px; width: 150px; }
.ring-arc { transition: stroke-dashoffset 0.8s ease; }
.ring-num { font-size: 30px; font-weight: 800; fill: #b5573b; }
.ring-label { font-size: 11px; fill: #7d6b5c; }
.ring-comment { font-size: 12px; color: #7d6b5c; margin-top: 4px; }
.check-list { text-align: left; margin-top: 12px; }
.cl-item { font-size: 12.5px; color: #6f5641; padding: 3px 0; display: flex; gap: 6px; align-items: center; }
.cl-item .ok { color: var(--brand-pine, #3f6152); font-weight: 700; width: 14px; text-align: center; }
.cl-item .no { color: #c9b79c; width: 14px; text-align: center; }
.stat-card .st-row { display: flex; justify-content: space-between; font-size: 13.5px; color: #6f5641; padding: 6px 0; }
.dirty-flag {
  position: absolute; top: 14px; right: 18px; font-size: 12px; color: #e6a23c;
  background: #fdf6ec; border-radius: 10px; padding: 2px 10px; animation: blink 1.6s infinite;
}
@keyframes blink { 50% { opacity: 0.55; } }
.tip { margin-left: 10px; font-size: 12px; color: #909399; }
.tip.ok { color: #2c9c58; }
.tip.bad { color: #f56c6c; }
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed #d9d9d9;
  border-radius: 50%;
  padding: 4px;
  cursor: pointer;
}
</style>
