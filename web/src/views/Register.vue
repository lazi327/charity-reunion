<template>
  <div class="reg-bg">
    <div class="lanterns" aria-hidden="true">
      <span v-for="i in 5" :key="i" class="lantern" :style="{ left: [15, 38, 62, 80, 50][i - 1] + '%', animationDuration: (16 + i * 3) + 's', animationDelay: i * 2 + 's' }"><Lamp /></span>
    </div>

    <div class="reg-box">
      <div class="title">注册寻亲账号</div>
      <div class="sub">免费发布寻亲信息 · 提供线索 · 参与问答</div>

      <el-form :model="form" label-width="90px" class="reg-form">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="登录账号" required>
              <el-input v-model="form.username" placeholder="3-20位字母/数字/下划线" clearable
                        :loading="nameChecking" @input="onNameInput">
                <template #suffix>
                  <span v-if="nameState === 'checking'" class="name-tip checking">查重中…</span>
                  <span v-else-if="nameState === 'ok'" class="name-tip ok">✓ 可用</span>
                  <span v-else-if="nameState === 'taken'" class="name-tip bad">✗ 已被占用</span>
                  <span v-else-if="nameState === 'bad'" class="name-tip bad">格式不对</span>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="昵称" required>
              <el-input v-model="form.nickname" placeholder="如:张父、李姐" maxlength="20" show-word-limit />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="密码" required>
              <el-input v-model="form.password" type="password" show-password placeholder="至少 6 位" />
            </el-form-item>
            <!-- 密码强度计 -->
            <div class="strength-wrap">
              <div class="strength-bars">
                <span v-for="i in 4" :key="i" class="bar"
                      :class="{ on: strength.score >= i, ['lv' + strength.level]: true }"></span>
              </div>
              <span class="strength-text" :class="'lv' + strength.level">{{ strength.label }}</span>
            </div>
            <div class="strength-rules">
              <span v-for="r in strengthRules" :key="r.key" :class="{ hit: r.test(form.password) }">{{ r.label }}</span>
            </div>
          </el-col>
          <el-col :span="12">
            <el-form-item label="确认密码" required>
              <el-input v-model="form.confirm" type="password" show-password placeholder="再输入一次"
                        :class="{ 'is-error-line': form.confirm && form.confirm !== form.password }" />
            </el-form-item>
            <div class="match-tip" v-if="form.confirm">
              <span v-if="form.confirm === form.password" class="ok">✓ 两次输入一致</span>
              <span v-else class="bad">✗ 两次输入不一致</span>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="性别" required>
              <el-radio-group v-model="form.gender">
                <el-radio :value="1">男</el-radio>
                <el-radio :value="2">女</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" placeholder="11位手机号,便于知情者联系" maxlength="11" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="出生年份">
              <el-input-number v-model="form.birthYear" :min="1940" :max="2010" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在城市">
              <el-input v-model="form.city" placeholder="如:广州" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="联系说明">
          <el-input v-model="form.aboutMe" type="textarea" :rows="3" maxlength="500" show-word-limit
                    placeholder="方便联系您的时间段、联系方式偏好等" />
        </el-form-item>

        <el-form-item>
          <el-checkbox v-model="agree">
            我已阅读并承诺遵守<a v-tappable class="pledge-link" @click.prevent="showPledge">《公益使用承诺书》</a>:信息真实,不用于任何商业目的
          </el-checkbox>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" size="large" :loading="loading" :disabled="!canSubmit" @click="submit">
            {{ canSubmit ? '提交注册' : '请先完善必填项' }}
          </el-button>
          <el-button size="large" @click="$router.push('/login')">已有账号,去登录</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import request from '../utils/request'

const router = useRouter()
const loading = ref(false)
const agree = ref(false)
const form = ref({ gender: 1, birthYear: 1990, username: '', password: '', confirm: '' })

/* ---------- 用户名实时查重(防抖 400ms) ---------- */
const nameState = ref('')          // '' | checking | ok | taken | bad
const nameChecking = ref(false)
let nameTimer = null
function onNameInput(val) {
  clearTimeout(nameTimer)
  const v = (val || '').trim()
  if (!v) { nameState.value = ''; return }
  if (!/^[A-Za-z0-9_]{3,20}$/.test(v)) {
    nameState.value = 'bad'
    return
  }
  nameState.value = 'checking'
  nameChecking.value = true
  nameTimer = setTimeout(async () => {
    try {
      const usable = await request.get('/api/auth/check-username', { params: { username: v } })
      nameState.value = usable ? 'ok' : 'taken'
    } finally {
      nameChecking.value = false
    }
  }, 400)
}

/* ---------- 密码强度:长度/大小写/数字/符号 各1分 ---------- */
const strengthRules = [
  { key: 'len', label: '8位以上', test: v => v.length >= 8 },
  { key: 'case', label: '大小写混合', test: v => /[a-z]/.test(v) && /[A-Z]/.test(v) },
  { key: 'num', label: '含数字', test: v => /\d/.test(v) },
  { key: 'sym', label: '含符号', test: v => /[^A-Za-z0-9]/.test(v) }
]
const strength = computed(() => {
  const v = form.value.password || ''
  let s = 0
  if (v.length >= 6) s++
  if (v.length >= 10) s++
  if ((/[a-z]/.test(v) && /[A-Z]/.test(v)) || (/[a-zA-Z]/.test(v) && /\d/.test(v))) s++
  if (/[^A-Za-z0-9]/.test(v)) s++
  const levels = ['', '弱', '中', '良', '强']
  return { score: s, level: s, label: v ? '强度:' + levels[s] : '强度:—' }
})

/* ---------- 提交条件 ---------- */
const canSubmit = computed(() => {
  const f = form.value
  return /^[A-Za-z0-9_]{3,20}$/.test((f.username || '').trim())
    && nameState.value === 'ok'
    && (f.password || '').length >= 6
    && f.password === f.confirm
    && (f.nickname || '').trim()
    && (!f.phone || /^1\d{10}$/.test(f.phone))
    && agree.value
})

function showPledge() {
  ElMessageBox.alert(
    '一、本平台为纯公益寻亲服务,发布的一切信息须真实有效;<br>' +
    '二、不得利用平台从事任何商业、诈骗、骚扰行为;<br>' +
    '三、走失情况请第一时间报警,平台信息仅作辅助;<br>' +
    '四、我们依法保护您的个人隐私,联系方式仅对您信任的对象展示。',
    '公益使用承诺书',
    { dangerouslyUseHTMLString: true, confirmButtonText: '我已知悉' }
  )
}

async function submit() {
  if (!canSubmit.value) {
    ElMessage.warning('请检查表单:账号需可用且通过查重、密码两次一致、勾选承诺书')
    return
  }
  loading.value = true
  try {
    const { confirm, ...body } = form.value
    await request.post('/api/auth/register', body)
    ElMessage.success('注册成功,请登录')
    router.push('/login')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.reg-bg {
  min-height: 100vh;
  background: linear-gradient(135deg, #f6efe3 0%, #efe4d2 40%, #e8d3bd 100%);
  display: flex;
  justify-content: center;
  padding: 40px 16px;
  position: relative;
  overflow: hidden;
}
.lanterns { position: absolute; inset: 0; pointer-events: none; }
.lantern { position: absolute; bottom: -60px; font-size: 22px; animation: float-up linear infinite; }
@keyframes float-up {
  to { transform: translateY(-115vh) translateX(40px); opacity: 0; }
}
.reg-box {
  width: 760px;
  height: fit-content;
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(14px);
  border-radius: 18px;
  padding: 32px 36px;
  box-shadow: 0 18px 60px rgba(60, 20, 60, 0.2);
  position: relative;
  z-index: 2;
}
.title { font-size: 24px; font-weight: 700; color: #b5573b; text-align: center; }
.sub { text-align: center; color: #7d6b5c; font-size: 13px; margin: 6px 0 22px; }
.reg-form :deep(.el-input__suffix) { display: flex; align-items: center; }
.name-tip { font-size: 12px; white-space: nowrap; }
.name-tip.ok { color: #67c23a; }
.name-tip.bad { color: #f56c6c; }
.name-tip.checking { color: #909399; animation: blink 1s infinite; }
@keyframes blink { 50% { opacity: 0.4; } }

.strength-wrap { display: flex; align-items: center; gap: 8px; margin: -6px 0 4px 90px; }
.strength-bars { display: flex; gap: 4px; }
.bar { width: 34px; height: 5px; border-radius: 3px; background: #e4e7ed; transition: background 0.3s; }
.bar.on.lv1 { background: #f56c6c; }
.bar.on.lv2 { background: #e6a23c; }
.bar.on.lv3 { background: #409eff; }
.bar.on.lv4 { background: #67c23a; }
.strength-text { font-size: 12px; color: #909399; }
.strength-text.lv1 { color: #f56c6c; }
.strength-text.lv2 { color: #e6a23c; }
.strength-text.lv3 { color: #409eff; }
.strength-text.lv4 { color: #67c23a; }
.strength-rules { display: flex; gap: 12px; margin: 0 0 8px 90px; font-size: 12px; color: #b3b6bd; }
.strength-rules .hit { color: #67c23a; }
.strength-rules .hit::before { content: '✓ '; }
.match-tip { margin-left: 90px; font-size: 12px; height: 18px; }
.match-tip .ok { color: #67c23a; }
.match-tip .bad { color: #f56c6c; }
.pledge-link { color: var(--brand-deep); cursor: pointer; text-decoration: underline; }
</style>
