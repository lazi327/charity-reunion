<template>
  <div class="admin-login-bg">
    <!-- 扫描线装饰 -->
    <div class="scan-line" aria-hidden="true"></div>
    <div class="login-box" :class="{ shake: shaking }" @animationend="shaking = false">
      <div class="title"><svg class="title-dove" viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor" aria-hidden="true"><path d="M23 6.6c-1.6.7-2.6 1.1-4 1.4 1.2-.8 1.7-1.6 2-2.7-1 .6-2 .9-3.2 1.1C16.6 5.4 15.5 5 14.3 5c-2.3 0-4.2 1.9-4.2 4.2 0 .3 0 .6.1.9C6.9 10 3.9 8.3 2 5.7c-.4.6-.6 1.3-.6 2.1 0 1.5.7 2.7 1.9 3.5-.7 0-1.3-.2-1.9-.5v.1c0 2 1.4 3.7 3.3 4.1-.3.1-.7.1-1.1.1-.3 0-.5 0-.8-.1.5 1.6 2 2.8 3.8 2.8-1.4 1.1-3.1 1.7-5 1.7-.3 0-.7 0-1-.1 1.8 1.2 4 1.8 6.2 1.8 7.5 0 11.6-6.2 11.6-11.6v-.5c.8-.6 1.5-1.3 2.1-2.1z"/></svg> 公益寻亲平台</div>
      <div class="subtitle">管理员后台登录</div>
      <form @submit.prevent="doLogin">
        <el-input v-model="form.username" placeholder="管理员账号" size="large" :prefix-icon="User" />
        <el-input v-model="form.password" type="password" placeholder="密码" size="large"
                  show-password :prefix-icon="Lock" style="margin-top: 14px" @keyup="detectCaps" />
        <div class="caps-hint" :class="{ show: capsOn }"><el-icon><Warning /></el-icon> 大写锁定已开启</div>
        <div class="captcha-row">
          <el-input v-model="form.captchaCode" placeholder="算式答案" size="large" :prefix-icon="Key"
                    maxlength="4" @keyup.enter="doLogin" />
          <img v-if="captcha.img" :src="captcha.img" alt="验证码" class="captcha-img" @click="loadCaptcha" />
        </div>
        <div v-if="lockLeft > 0" class="lock-banner">
          <el-icon><Lock /></el-icon> 连续输错次数过多,<b>{{ lockLeft }}</b> 秒后允许再次尝试
        </div>
        <button class="submit-btn" :disabled="loading || lockLeft > 0" type="submit" style="margin-top: 14px">
          <span v-if="lockLeft > 0">锁定中 {{ lockLeft }}s</span>
          <span v-else-if="loading">验证中…</span>
          <span v-else>登录后台</span>
        </button>
        <div style="margin-top: 12px; text-align: center">
          <router-link to="/login" style="color: #909399; font-size: 13px">← 返回寻亲者登录</router-link>
        </div>
      </form>
      <el-alert type="info" :closable="false" style="margin-top: 16px" title="演示账号:admin / admin123"
                @click="fillDemo" class="demo-alert" />
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Key } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { setAuth } from '../../utils/auth'

const router = useRouter()
const form = ref({ username: '', password: '', captchaId: '', captchaCode: '' })
const captcha = ref({ id: '', img: '' })
const loading = ref(false)
const shaking = ref(false)
const capsOn = ref(false)
const lockLeft = ref(0)
let lockTimer = null

async function loadCaptcha() {
  try {
    captcha.value = await request.get('/api/auth/captcha')
    form.value.captchaId = captcha.value.id
  } catch { /* 忽略 */ }
}

function detectCaps(e) {
  capsOn.value = e.getModifierState && e.getModifierState('CapsLock')
}

function fillDemo() {
  form.value.username = 'admin'
  form.value.password = 'admin123'
  form.value.captchaCode = ''
  loadCaptcha()
}

function startLockCountdown(seconds) {
  lockLeft.value = seconds
  clearInterval(lockTimer)
  lockTimer = setInterval(() => {
    lockLeft.value -= 1
    if (lockLeft.value <= 0) clearInterval(lockTimer)
  }, 1000)
}

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
    if (data.user.role !== 'ADMIN') {
      ElMessage.error('该账号不是管理员,请使用寻亲者入口登录')
      shaking.value = true
      form.value.captchaCode = ''
      loadCaptcha()
      return
    }
    setAuth(data.token, data.user)
    router.push('/admin')
  } catch (err) {
    shaking.value = true
    if (err && typeof err === 'object' && err.code === 2) {
      startLockCountdown(err.data?.lockSeconds || 300)
    }
    form.value.captchaCode = ''
    loadCaptcha()
  } finally {
    loading.value = false
  }
}

onMounted(loadCaptcha)
onBeforeUnmount(() => clearInterval(lockTimer))
</script>

<style scoped>
.admin-login-bg {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f2d3d 0%, #364f6b 100%);
  position: relative;
  overflow: hidden;
}
.scan-line {
  position: absolute; left: 0; right: 0; height: 90px; top: -90px;
  background: linear-gradient(180deg, transparent, rgba(122, 180, 255, 0.12), transparent);
  animation: scan 5s linear infinite;
}
@keyframes scan { to { top: 110vh; } }
.login-box {
  width: 420px;
  background: rgba(255, 255, 255, 0.96);
  border-radius: 14px;
  padding: 36px 32px;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.35);
  position: relative;
  z-index: 2;
}
.login-box.shake { animation: card-shake 0.45s; }
@keyframes card-shake {
  0%, 100% { transform: translateX(0); }
  20% { transform: translateX(-12px); }
  40% { transform: translateX(10px); }
  60% { transform: translateX(-6px); }
  80% { transform: translateX(4px); }
}
.title { font-size: 22px; font-weight: 700; text-align: center; color: #b5573b; }
.subtitle { text-align: center; color: #999; margin: 8px 0 24px; }
.captcha-row { display: flex; gap: 10px; margin-top: 14px; align-items: center; }
.captcha-row .el-input { flex: 1; }
.captcha-img {
  width: 130px; height: 40px; border-radius: 6px;
  border: 1px solid #dcdfe6; cursor: pointer;
}
.caps-hint { font-size: 12px; color: #e6a23c; height: 0; overflow: hidden; transition: height 0.2s; }
.caps-hint.show { height: 18px; }
.lock-banner {
  margin-top: 12px;
  background: #fdecec; border-radius: 8px; padding: 8px 12px;
  font-size: 13px; color: #c44;
}
.submit-btn {
  width: 100%; height: 44px; border: none; border-radius: 8px;
  background: linear-gradient(90deg, #b5573b, #8f3f2a);
  color: #fff; font-size: 15px; font-weight: 700; letter-spacing: 4px;
  cursor: pointer; transition: all 0.25s;
}
.submit-btn:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(181, 87, 59, 0.4); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.demo-alert { cursor: pointer; }
</style>
