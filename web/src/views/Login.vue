<template>
  <div class="login-page" :class="{ dark: isDark }">
    <!-- 动态粒子背景(鼠标排斥/点击迸发) -->
    <canvas ref="canvasEl" class="bg-canvas" @click="burst"></canvas>

    <!-- 主题切换 -->
    <button class="theme-btn" :title="isDark ? '切换日间模式' : '切换夜间模式'" @click="isDark = !isDark">
      <el-icon><component :is="isDark ? 'Sunny' : 'Moon'" /></el-icon>
    </button>

    <!-- 漂浮孔明灯装饰 -->
    <div class="lanterns" aria-hidden="true">
      <span v-for="i in 6" :key="i" class="lantern" :style="lanternStyle(i)"><Lamp /></span>
    </div>

    <div class="stage">
      <div class="glass-card" :class="{ shake: shaking }" @animationend="shaking = false">
        <!-- 左:品牌面板 -->
        <aside class="brand-side">
          <div class="dove"><svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor" aria-hidden="true"><path d="M23 6.6c-1.6.7-2.6 1.1-4 1.4 1.2-.8 1.7-1.6 2-2.7-1 .6-2 .9-3.2 1.1C16.6 5.4 15.5 5 14.3 5c-2.3 0-4.2 1.9-4.2 4.2 0 .3 0 .6.1.9C6.9 10 3.9 8.3 2 5.7c-.4.6-.6 1.3-.6 2.1 0 1.5.7 2.7 1.9 3.5-.7 0-1.3-.2-1.9-.5v.1c0 2 1.4 3.7 3.3 4.1-.3.1-.7.1-1.1.1-.3 0-.5 0-.8-.1.5 1.6 2 2.8 3.8 2.8-1.4 1.1-3.1 1.7-5 1.7-.3 0-.7 0-1-.1 1.8 1.2 4 1.8 6.2 1.8 7.5 0 11.6-6.2 11.6-11.6v-.5c.8-.6 1.5-1.3 2.1-2.1z"/></svg></div>
          <h1>公益寻亲平台</h1>
          <p class="slogan">纯公益 · 全免费 · 助力团圆</p>

          <div class="stats-row">
            <div class="stat">
              <b>{{ animatedCases }}</b>
              <span>条寻亲信息</span>
            </div>
            <div class="stat-divider"></div>
            <div class="stat">
              <b>{{ animatedViews }}</b>
              <span>次累计关注</span>
            </div>
          </div>

          <el-carousel height="64px" direction="vertical" :autoplay="true" :interval="4000"
                       indicator-position="none" class="tips-carousel">
            <el-carousel-item v-for="t in tips" :key="t">
              <div class="tip-item"><el-icon><component :is="t.icon" /></el-icon> {{ t.text }}</div>
            </el-carousel-item>
          </el-carousel>

          <div class="demo-chips">
            <span class="chips-label">演示账号一键填入:</span>
            <el-tag v-for="d in demoAccounts" :key="d.username" class="chip" effect="light" round
                    @click="fillDemo(d)">{{ d.label }}</el-tag>
          </div>
        </aside>

        <!-- 右:表单面板 -->
        <section class="form-side">
          <el-tabs v-model="tab" class="login-tabs">
            <el-tab-pane label="登录" name="login" />
            <el-tab-pane label="忘记密码" name="forgot" />
          </el-tabs>

          <!-- 登录表单 -->
          <form v-if="tab === 'login'" class="the-form" @submit.prevent="doLogin">
            <el-input v-model="form.username" placeholder="账号" size="large" clearable
                      :prefix-icon="User" @keydown="detectCaps" />
            <div class="caps-hint" :class="{ show: capsOn }"><el-icon><Warning /></el-icon> 大写锁定已开启</div>

            <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                      show-password :prefix-icon="Lock" @keydown="detectCaps" />

            <div class="captcha-row">
              <el-input v-model="form.captchaCode" placeholder="算式答案" size="large" :prefix-icon="Key"
                        maxlength="4" @keyup.enter="doLogin" />
              <img v-if="captcha.img" :src="captcha.img" class="captcha-img" title="点击换一张"
                   alt="验证码" @click="loadCaptcha" />
              <div v-else class="captcha-img placeholder">加载中…</div>
            </div>

            <div class="row-between">
              <el-checkbox v-model="remember" size="small">记住账号</el-checkbox>
              <span class="fail-tip" v-if="failTip">{{ failTip }}</span>
            </div>

            <!-- 锁定倒计时条 -->
            <div v-if="lockLeft > 0" class="lock-banner">
              <el-icon><Lock /></el-icon> 账号已锁定,<b>{{ fmtSeconds(lockLeft) }}</b> 后自动解锁
            </div>

            <button class="submit-btn" :disabled="loading || lockLeft > 0" type="submit">
              <span v-if="lockLeft > 0">锁定中 {{ lockLeft }}s</span>
              <span v-else-if="loading">正在验证…</span>
              <span v-else>登 录</span>
            </button>

            <div class="links">
              <router-link to="/register">注册寻亲账号</router-link>
              <router-link to="/admin/login">管理员入口</router-link>
            </div>
          </form>

          <!-- 找回密码表单 -->
          <form v-else class="the-form" @submit.prevent="doForgot">
            <el-input v-model="fp.username" placeholder="账号" size="large" clearable :prefix-icon="User" />
            <el-input v-model="fp.phone" placeholder="注册时填写的手机号" size="large" clearable
                      :prefix-icon="Iphone" maxlength="11" />
            <div class="captcha-row">
              <el-input v-model="fp.captchaCode" placeholder="算式答案" size="large" :prefix-icon="Key" maxlength="4" />
              <img v-if="captcha.img" :src="captcha.img" class="captcha-img" title="点击换一张"
                   alt="验证码" @click="loadCaptcha" />
            </div>
            <el-alert type="info" :closable="false" class="fp-tip"
                      title="核验通过后,密码将重置为 123456,登录后请尽快修改" />
            <button class="submit-btn" :disabled="fpLoading" type="submit">
              {{ fpLoading ? '核验中…' : '重置密码' }}
            </button>
            <div class="links">
              <a href="javascript:void(0)" @click="tab = 'login'">← 返回登录</a>
              <span></span>
            </div>
          </form>
        </section>
      </div>

      <footer class="foot">让每一次转发,都成为团圆的路 · 走失请立即拨打 110</footer>
    </div>
  </div>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Lock, Key, Iphone } from '@element-plus/icons-vue'
import request from '../utils/request'
import { setAuth } from '../utils/auth'

const router = useRouter()
const tab = ref('login')
const isDark = ref(false)
const loading = ref(false)
const fpLoading = ref(false)
const shaking = ref(false)
const capsOn = ref(false)
const remember = ref(true)
const failTip = ref('')
const lockLeft = ref(0)
const canvasEl = ref(null)

const form = ref({ username: '', password: '', captchaId: '', captchaCode: '' })
const fp = ref({ username: '', phone: '', captchaId: '', captchaCode: '' })
const captcha = ref({ id: '', img: '' })

const summary = ref({ caseCount: 0, viewTotal: 0 })
const animatedCases = ref(0)
const animatedViews = ref(0)

const tips = [
  { icon: 'Warning', text: '亲人走失请立即报警,不必等待 24 小时' },
  { icon: 'Connection', text: '走失儿童父母可到全国公安DNA实验室免费采血入库' },
  { icon: 'Lock', text: '谨防以"寻亲费""找到保证金"为名的诈骗,一律举报' },
  { icon: 'Promotion', text: '在平台发布寻亲档案,让更多好心人提供线索' }
]
const demoAccounts = [
  { label: '寻亲者 s_zhang', username: 's_zhang', password: '123456' },
  { label: '管理员 admin', username: 'admin', password: 'admin123' }
]

/* ---------- 验证码 ---------- */
async function loadCaptcha() {
  try {
    captcha.value = await request.get('/api/auth/captcha')
    form.value.captchaId = captcha.value.id
    fp.value.captchaId = captcha.value.id
  } catch { /* 忽略 */ }
}

/* ---------- 登录 ---------- */
let lockTimer = null
function startLockCountdown(seconds) {
  lockLeft.value = seconds
  clearInterval(lockTimer)
  lockTimer = setInterval(() => {
    lockLeft.value -= 1
    if (lockLeft.value <= 0) {
      clearInterval(lockTimer)
      failTip.value = ''
      loadCaptcha()
    }
  }, 1000)
}
const fmtSeconds = s => `${Math.floor(s / 60)} 分 ${String(s % 60).padStart(2, '0')} 秒`

async function doLogin() {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (!form.value.captchaCode) {
    ElMessage.warning('请填写验证码')
    return
  }
  loading.value = true
  try {
    const data = await request.post('/api/auth/login', form.value)
    if (remember.value) {
      localStorage.setItem('cf_remember_user', form.value.username)
    } else {
      localStorage.removeItem('cf_remember_user')
    }
    setAuth(data.token, data.user)
    ElMessage.success('欢迎回来,' + data.user.nickname)
    router.push(data.user.role === 'ADMIN' ? '/admin' : '/')
  } catch (err) {
    shake()
    failTip.value = '请重新计算验证码'
    if (err && typeof err === 'object' && err.code === 2) {
      // 账号锁定:后端返回剩余秒数
      startLockCountdown(err.data?.lockSeconds || 300)
      failTip.value = ''
    } else {
      failTip.value = String(typeof err === 'string' ? err : err?.msg || '登录失败')
    }
    form.value.captchaCode = ''
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

/* ---------- 找回密码 ---------- */
async function doForgot() {
  if (!fp.value.username || !fp.value.phone || !fp.value.captchaCode) {
    ElMessage.warning('请填写完整信息')
    return
  }
  fpLoading.value = true
  try {
    await request.post('/api/auth/forgot-password', fp.value)
    ElMessageBox.alert('身份核验通过,密码已重置为 123456,请登录后尽快修改。', '重置成功', { type: 'success' })
    form.value.username = fp.value.username
    tab.value = 'login'
  } catch (err) {
    fp.value.captchaCode = ''
    loadCaptcha()
  } finally {
    fpLoading.value = false
  }
}

function fillDemo(d) {
  tab.value = 'login'
  form.value.username = d.username
  form.value.password = d.password
  form.value.captchaCode = ''
  loadCaptcha()
  ElMessage.info(`已填入 ${d.label},请计算验证码后登录`)
}

function shake() {
  shaking.value = false
  requestAnimationFrame(() => { shaking.value = true })
}

function detectCaps(e) {
  capsOn.value = e.getModifierState && e.getModifierState('CapsLock')
}

/* ---------- 数字滚动 ---------- */
function countUp(targetRef, to) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { targetRef.value = to; return }
  const start = performance.now()
  const dur = 1200
  function step(now) {
    const p = Math.min(1, (now - start) / dur)
    const ease = 1 - Math.pow(1 - p, 3)
    targetRef.value = Math.round(to * ease)
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

/* ---------- 粒子背景 ---------- */
let raf = 0
function initCanvas() {
  const canvas = canvasEl.value
  const ctx = canvas.getContext('2d')
  const mouse = { x: -9999, y: -9999 }
  let W = 0, H = 0
  const COLORS_LIGHT = ['224, 90, 109', '166, 193, 238', '251, 194, 235', '255, 255, 255']
  let dots = []

  function resize() {
    W = canvas.width = window.innerWidth
    H = canvas.height = window.innerHeight
    const n = Math.min(110, Math.floor(W * H / 16000))
    dots = Array.from({ length: n }, () => ({
      x: Math.random() * W, y: Math.random() * H,
      vx: (Math.random() - 0.5) * 0.4, vy: (Math.random() - 0.5) * 0.4,
      r: 1 + Math.random() * 2.4,
      c: COLORS_LIGHT[Math.floor(Math.random() * COLORS_LIGHT.length)]
    }))
  }
  resize()
  window.addEventListener('resize', resize)

  function onMove(e) { mouse.x = e.clientX; mouse.y = e.clientY }
  function onLeave() { mouse.x = -9999; mouse.y = -9999 }
  window.addEventListener('mousemove', onMove)
  window.addEventListener('mouseout', onLeave)
  canvas._burst = (e) => {
    for (const d of dots) {
      const dx = d.x - e.clientX, dy = d.y - e.clientY
      const dist = Math.hypot(dx, dy)
      if (dist < 180 && dist > 0) {
        const f = (180 - dist) / 180 * 6
        d.vx += dx / dist * f
        d.vy += dy / dist * f
      }
    }
  }

  function tick() {
    ctx.clearRect(0, 0, W, H)
    const dark = document.querySelector('.login-page')?.classList.contains('dark')
    for (const d of dots) {
      // 鼠标斥力
      const dx = d.x - mouse.x, dy = d.y - mouse.y
      const dist = Math.hypot(dx, dy)
      if (dist < 120 && dist > 0) {
        const f = (120 - dist) / 120 * 0.6
        d.vx += dx / dist * f
        d.vy += dy / dist * f
      }
      // 阻尼回稳
      d.vx *= 0.985; d.vy *= 0.985
      d.x += d.vx; d.y += d.vy
      if (d.x < -10) d.x = W + 10; if (d.x > W + 10) d.x = -10
      if (d.y < -10) d.y = H + 10; if (d.y > H + 10) d.y = -10

      ctx.beginPath()
      ctx.arc(d.x, d.y, d.r, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(${d.c},${dark ? 0.75 : 0.55})`
      ctx.fill()
    }
    // 近距连线
    for (let i = 0; i < dots.length; i++) {
      for (let j = i + 1; j < dots.length; j++) {
        const a = dots[i], b = dots[j]
        const dist = Math.hypot(a.x - b.x, a.y - b.y)
        if (dist < 110) {
          ctx.beginPath()
          ctx.moveTo(a.x, a.y)
          ctx.lineTo(b.x, b.y)
          ctx.strokeStyle = `rgba(181,87,59,${(1 - dist / 110) * (dark ? 0.25 : 0.16)})`
          ctx.lineWidth = 1
          ctx.stroke()
        }
      }
    }
    raf = requestAnimationFrame(tick)
  }
  tick()

  canvas._cleanup = () => {
    window.removeEventListener('resize', resize)
    window.removeEventListener('mousemove', onMove)
    window.removeEventListener('mouseout', onLeave)
  }
}

function burst(e) {
  canvasEl.value?._burst?.(e)
}

/* ---------- 孔明灯随机漂浮参数 ---------- */
function lanternStyle(i) {
  const seed = [23, 58, 74, 12, 88, 43]
  return {
    left: seed[i - 1] + '%',
    animationDuration: (14 + i * 3) + 's',
    animationDelay: (i * 2.2) + 's',
    fontSize: (16 + (i % 3) * 6) + 'px',
    opacity: 0.5 + (i % 3) * 0.15
  }
}

watch(isDark, v => localStorage.setItem('cf_login_theme', v ? 'dark' : 'light'))

onMounted(async () => {
  isDark.value = localStorage.getItem('cf_login_theme') === 'dark'
  const saved = localStorage.getItem('cf_remember_user')
  if (saved) form.value.username = saved
  initCanvas()
  loadCaptcha()
  try {
    summary.value = await request.get('/api/public/summary')
    countUp(animatedCases, summary.value.caseCount || 0)
    countUp(animatedViews, summary.value.viewTotal || 0)
  } catch { /* 忽略 */ }
})

onBeforeUnmount(() => {
  cancelAnimationFrame(raf)
  clearInterval(lockTimer)
  canvasEl.value?._cleanup?.()
})
</script>

<style scoped>
.login-page {
  --bg1: #f6efe3; --bg2: #efe4d2; --bg3: #e8d3bd;
  --card-bg: rgba(255, 255, 255, 0.82);
  --text: #333; --sub: #888;
  --side-grad: linear-gradient(160deg, #b5573b 0%, #8f3f2a 100%);
  min-height: 100vh;
  position: relative;
  background: linear-gradient(135deg, var(--bg1) 0%, var(--bg2) 40%, var(--bg3) 100%);
  transition: filter 0.5s;
  overflow: hidden;
}
.login-page.dark {
  filter: saturate(0.9);
  --card-bg: rgba(30, 33, 48, 0.86);
  --text: #e6e6ee; --sub: #9aa0b5;
  --side-grad: linear-gradient(160deg, #4a1f2d 0%, #33244a 100%);
}
.bg-canvas {
  position: absolute; inset: 0;
  width: 100%; height: 100%;
  cursor: crosshair;
}
.theme-btn {
  position: absolute; top: 20px; right: 24px; z-index: 10;
  width: 44px; height: 44px; border-radius: 50%;
  border: none; cursor: pointer; font-size: 20px;
  background: rgba(255, 255, 255, 0.55);
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.12);
  transition: transform 0.3s;
}
.theme-btn:hover { transform: rotate(20deg) scale(1.12); }

/* 孔明灯 */
.lanterns { position: absolute; inset: 0; pointer-events: none; }
.lantern {
  position: absolute; bottom: -60px;
  animation: float-up linear infinite;
}
@keyframes float-up {
  0%   { transform: translateY(0) rotate(-4deg); }
  50%  { transform: translateY(-55vh) translateX(30px) rotate(5deg); }
  100% { transform: translateY(-115vh) translateX(-20px) rotate(-3deg); opacity: 0; }
}

.stage {
  position: relative; z-index: 2;
  min-height: 100vh;
  display: flex; flex-direction: column;
  align-items: center; justify-content: center;
  padding: 24px;
}

/* 玻璃卡片 */
.glass-card {
  display: flex;
  width: 880px; max-width: 96vw;
  border-radius: 22px; overflow: hidden;
  background: var(--card-bg);
  backdrop-filter: blur(18px);
  box-shadow: 0 18px 60px rgba(60, 20, 60, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.5);
}
.glass-card.shake { animation: card-shake 0.45s; }
@keyframes card-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-14px); }
  40% { transform: translateX(12px); }
  60% { transform: translateX(-8px); }
  80% { transform: translateX(6px); }
}

/* 左侧品牌 */
.brand-side {
  width: 340px; flex-shrink: 0;
  background: var(--side-grad);
  color: #fff;
  padding: 36px 28px;
  display: flex; flex-direction: column;
  position: relative;
}
.dove {
  font-size: 42px;
  animation: dove-fly 3.2s ease-in-out infinite;
}
@keyframes dove-fly {
  0%, 100% { transform: translateY(0) rotate(-6deg); }
  50% { transform: translateY(-10px) rotate(6deg); }
}
.brand-side h1 { font-size: 26px; margin: 10px 0 6px; letter-spacing: 2px; }
.slogan { font-size: 13px; opacity: 0.85; margin: 0 0 22px; }
.stats-row { display: flex; align-items: center; margin-bottom: 20px; }
.stat b { font-size: 26px; display: block; }
.stat span { font-size: 12px; opacity: 0.8; }
.stat-divider { width: 1px; height: 34px; background: rgba(255, 255, 255, 0.4); margin: 0 22px; }
.tips-carousel {
  background: rgba(255, 255, 255, 0.14);
  border-radius: 10px; padding: 4px 10px; margin-bottom: 20px;
}
.tip-item { font-size: 12.5px; line-height: 1.7; display: flex; align-items: center; height: 56px; }
.demo-chips { margin-top: auto; font-size: 12px; }
.chips-label { opacity: 0.8; display: block; margin-bottom: 8px; }
.chip { cursor: pointer; margin-right: 8px; transition: transform 0.2s; }
.chip:hover { transform: translateY(-2px); }

/* 右侧表单 */
.form-side { flex: 1; padding: 30px 38px 34px; color: var(--text); }
.login-tabs :deep(.el-tabs__item) { font-size: 16px; font-weight: 600; }
.the-form { display: flex; flex-direction: column; gap: 12px; margin-top: 6px; }
.caps-hint {
  font-size: 12px; color: #e6a23c; height: 0; overflow: hidden;
  transition: height 0.2s; margin-top: -8px;
}
.caps-hint.show { height: 18px; }
.captcha-row { display: flex; gap: 10px; align-items: center; }
.captcha-row .el-input { flex: 1; }
.captcha-img {
  width: 130px; height: 46px; border-radius: 8px;
  border: 1px solid #dcdfe6; cursor: pointer; flex-shrink: 0;
  transition: transform 0.15s;
}
.captcha-img:hover { transform: scale(1.05); }
.captcha-img.placeholder {
  display: flex; align-items: center; justify-content: center;
  font-size: 12px; color: #aaa; background: #f5f7fa;
}
.row-between { display: flex; justify-content: space-between; align-items: center; min-height: 24px; }
.fail-tip { font-size: 12px; color: var(--brand-deep); }
.lock-banner {
  background: #fdecec; border: 1px solid #f7c8c8; color: #c44;
  border-radius: 8px; padding: 8px 12px; font-size: 13px;
  animation: pulse-bg 1.6s infinite;
}
@keyframes pulse-bg { 50% { background: #fbd9d9; } }
.submit-btn {
  height: 46px; border: none; border-radius: 10px;
  background: linear-gradient(90deg, #b5573b, #8f3f2a);
  color: #fff; font-size: 16px; font-weight: 700; letter-spacing: 6px;
  cursor: pointer; transition: all 0.25s;
  box-shadow: 0 6px 18px rgba(181, 87, 59, 0.35);
}
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 10px 24px rgba(181, 87, 59, 0.45); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; letter-spacing: 2px; }
.links { display: flex; justify-content: space-between; font-size: 13px; }
.links a { color: var(--brand-deep); text-decoration: none; }
.fp-tip { margin: 0; }

.foot {
  margin-top: 22px; font-size: 12.5px; color: rgba(80, 60, 80, 0.75);
  letter-spacing: 1px;
}
.dark .foot { color: rgba(230, 230, 238, 0.6); }

/* 窄屏:上下堆叠 */
@media (max-width: 780px) {
  .glass-card { flex-direction: column; width: 420px; }
  .brand-side { width: auto; padding: 24px; }
  .tips-carousel { display: none; }
}
</style>
