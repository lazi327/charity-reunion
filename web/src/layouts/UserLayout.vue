<template>
  <div>
    <el-container>
      <el-header class="nav-header">
        <div class="brand" @click="$router.push('/')">
          <span class="brand-dove"><svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor" aria-hidden="true"><path d="M23 6.6c-1.6.7-2.6 1.1-4 1.4 1.2-.8 1.7-1.6 2-2.7-1 .6-2 .9-3.2 1.1C16.6 5.4 15.5 5 14.3 5c-2.3 0-4.2 1.9-4.2 4.2 0 .3 0 .6.1.9C6.9 10 3.9 8.3 2 5.7c-.4.6-.6 1.3-.6 2.1 0 1.5.7 2.7 1.9 3.5-.7 0-1.3-.2-1.9-.5v.1c0 2 1.4 3.7 3.3 4.1-.3.1-.7.1-1.1.1-.3 0-.5 0-.8-.1.5 1.6 2 2.8 3.8 2.8-1.4 1.1-3.1 1.7-5 1.7-.3 0-.7 0-1-.1 1.8 1.2 4 1.8 6.2 1.8 7.5 0 11.6-6.2 11.6-11.6v-.5c.8-.6 1.5-1.3 2.1-2.1z"/></svg></span>
          <span class="brand-text">公益寻亲平台</span>
        </div>
        <el-menu mode="horizontal" :default-active="activeMenu" router class="nav-menu">
          <el-menu-item index="/home"><span class="mi"><el-icon><House /></el-icon>首页</span></el-menu-item>
          <el-menu-item index="/cases"><span class="mi"><el-icon><Search /></el-icon>寻亲档案</span></el-menu-item>
          <el-menu-item index="/radar"><span class="mi"><el-icon><Aim /></el-icon>亲缘雷达</span></el-menu-item>
          <el-menu-item index="/questions"><span class="mi"><el-icon><ChatDotRound /></el-icon>寻亲问答</span></el-menu-item>
          <el-menu-item index="/guides"><span class="mi"><el-icon><Reading /></el-icon>寻亲指南</span></el-menu-item>
          <el-menu-item index="/messages">
            <el-badge :value="unread" :hidden="unread === 0" :offset="[6, 12]">
              <span class="mi"><el-icon><Message /></el-icon>我的消息</span>
            </el-badge>
          </el-menu-item>
        </el-menu>
        <el-dropdown @command="onCommand">
          <span class="user-chip">
            <el-avatar :size="32" :src="user?.avatar" class="avatar">{{ user?.nickname?.[0] }}</el-avatar>
            <span style="margin: 0 6px">{{ user?.nickname }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="mycases"><el-icon><Folder /></el-icon>我的寻亲档案</el-dropdown-item>
              <el-dropdown-item command="new"><el-icon><EditPen /></el-icon>发布寻亲信息</el-dropdown-item>
              <el-dropdown-item command="radar"><el-icon><Aim /></el-icon>亲缘雷达</el-dropdown-item>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item command="logout" divided><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <!-- 滚动公告条 -->
      <div class="notice-bar" v-if="notices.length">
        <span class="notice-tag"><el-icon><Bell /></el-icon> 寻亲广播</span>
        <div class="notice-viewport">
          <div class="notice-scroll" :style="{ animationDuration: notices.length * 5 + 's' }">
            <span v-for="(n, i) in [...notices, ...notices]" :key="i" class="notice-item">{{ n }}</span>
          </div>
        </div>
      </div>
      <el-main style="max-width: 1200px; margin: 0 auto; width: 100%">
        <router-view v-slot="{ Component }">
          <transition name="page" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { getUser, clearAuth } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const user = ref(getUser())
const unread = ref(0)
const notices = ref([])
const activeMenu = computed(() => '/' + (route.path.split('/')[1] || 'home'))

let timer = null
let noticeTimer = null
async function loadUnread() {
  try {
    unread.value = Number(await request.get('/api/messages/unread')) || 0
  } catch { /* 忽略 */ }
}

/* 公告 = 平台数据 + 接力动态合成 */
async function loadNotices() {
  try {
    const [summary, feed] = await Promise.all([
      request.get('/api/public/summary'),
      request.get('/api/public/feed')
    ])
    const list = [`平台已汇聚 ${summary.caseCount || 0} 份寻亲档案、${summary.wishTotal || 0} 盏回家灯,近7天新增 ${summary.newCases7d || 0} 份`]
    feed.slice(0, 6).forEach(f => list.push(`${f.text}`))
    list.push(' 亲人走失请立即报警,不必等待 24 小时;谨防以"寻亲费"为名的诈骗')
    notices.value = list
  } catch { /* 忽略 */ }
}

function onCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  if (cmd === 'mycases') router.push('/mycases')
  if (cmd === 'new') router.push('/cases/new')
  if (cmd === 'radar') router.push('/radar')
  if (cmd === 'logout') {
    clearAuth()
    router.push('/login')
  }
}

onMounted(() => {
  loadUnread()
  loadNotices()
  timer = setInterval(loadUnread, 30000)
  noticeTimer = setInterval(loadNotices, 120000)
})
onUnmounted(() => {
  clearInterval(timer)
  clearInterval(noticeTimer)
})
</script>

<style scoped>
.nav-header {
  display: flex;
  align-items: center;
  background: rgba(255, 253, 248, 0.92);
  backdrop-filter: blur(12px);
  box-shadow: 0 2px 14px rgba(120, 80, 50, 0.10);
  padding: 0 20px;
  border-bottom: 2px solid var(--brand);
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-right: 24px;
  cursor: pointer;
  white-space: nowrap;
}
.brand-dove {
  font-size: 24px;
  color: var(--brand);
  display: inline-flex;
  animation: dove-sway 3s ease-in-out infinite;
}
@keyframes dove-sway {
  0%, 100% { transform: rotate(-8deg) translateY(0); }
  50% { transform: rotate(8deg) translateY(-3px); }
}
.brand-text {
  font-family: var(--font-display);
  font-size: 21px;
  font-weight: 700;
  color: var(--brand-deep);
  letter-spacing: 3px;
}
.nav-menu {
  flex: 1;
  background: transparent !important;
  border: none !important;
}
.nav-menu :deep(.el-menu-item) {
  font-weight: 600;
  color: var(--ink-soft);
}
.mi { display: inline-flex; align-items: center; gap: 6px; }
.mi .el-icon { font-size: 16px; color: var(--brand); }
.nav-menu :deep(.el-menu-item.is-active) {
  color: var(--brand-deep);
  background: linear-gradient(180deg, transparent 68%, rgba(181, 87, 59, 0.14) 68%);
}
.user-chip {
  display: flex;
  align-items: center;
  cursor: pointer;
  outline: none;
}
.avatar {
  background: var(--brand);
  color: #fff;
  font-weight: 700;
}

/* 公告条 */
.notice-bar {
  display: flex;
  align-items: center;
  background: linear-gradient(90deg, rgba(217, 138, 79, 0.16), rgba(181, 87, 59, 0.12), rgba(63, 97, 82, 0.10));
  overflow: hidden;
  height: 34px;
}
.notice-tag {
  flex: none;
  padding: 0 12px;
  font-size: 12.5px;
  font-weight: 700;
  color: #fff;
  background: var(--brand);
  height: 100%;
  display: flex;
  align-items: center;
  gap: 5px;
  border-radius: 0 16px 16px 0;
}
.notice-viewport { flex: 1; overflow: hidden; white-space: nowrap; }
.notice-scroll {
  display: inline-block;
  animation: marquee linear infinite;
  will-change: transform;
}
.notice-item {
  font-size: 12.5px;
  color: #6f5641;
  margin-right: 60px;
}
@keyframes marquee {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}

/* 路由切换过渡 */
.page-enter-active { transition: opacity 0.25s, transform 0.25s; }
.page-leave-active { transition: opacity 0.15s; }
.page-enter-from { opacity: 0; transform: translateY(10px); }
.page-leave-to { opacity: 0; }
</style>
