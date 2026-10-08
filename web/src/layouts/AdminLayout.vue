<template>
  <el-container style="min-height: 100vh">
    <el-aside width="220px" class="aside">
      <div class="brand"><svg viewBox="0 0 24 24" width="1em" height="1em" fill="currentColor" aria-hidden="true" style="vertical-align:-0.15em"><path d="M23 6.6c-1.6.7-2.6 1.1-4 1.4 1.2-.8 1.7-1.6 2-2.7-1 .6-2 .9-3.2 1.1C16.6 5.4 15.5 5 14.3 5c-2.3 0-4.2 1.9-4.2 4.2 0 .3 0 .6.1.9C6.9 10 3.9 8.3 2 5.7c-.4.6-.6 1.3-.6 2.1 0 1.5.7 2.7 1.9 3.5-.7 0-1.3-.2-1.9-.5v.1c0 2 1.4 3.7 3.3 4.1-.3.1-.7.1-1.1.1-.3 0-.5 0-.8-.1.5 1.6 2 2.8 3.8 2.8-1.4 1.1-3.1 1.7-5 1.7-.3 0-.7 0-1-.1 1.8 1.2 4 1.8 6.2 1.8 7.5 0 11.6-6.2 11.6-11.6v-.5c.8-.6 1.5-1.3 2.1-2.1z"/></svg> 寻亲管理后台</div>
      <el-menu :default-active="$route.path" router background-color="#1f2d3d"
               text-color="#c0c4cc" active-text-color="#ffd04b">
        <el-menu-item index="/admin/dashboard"><el-icon><DataLine /></el-icon>统计分析</el-menu-item>
        <el-menu-item index="/admin/banners"><el-icon><Picture /></el-icon>轮播图管理</el-menu-item>
        <el-menu-item index="/admin/users"><el-icon><User /></el-icon>用户管理</el-menu-item>
        <el-menu-item index="/admin/cases"><el-icon><Search /></el-icon>寻亲信息管理</el-menu-item>
        <el-menu-item index="/admin/clues"><el-icon><Position /></el-icon>寻亲线索管理</el-menu-item>
        <el-menu-item index="/admin/guides"><el-icon><Reading /></el-icon>寻亲指南管理</el-menu-item>
        <el-menu-item index="/admin/questions"><el-icon><ChatDotRound /></el-icon>寻亲问答管理</el-menu-item>
        <el-menu-item index="/admin/messages"><el-icon><Message /></el-icon>我的消息</el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header style="background: #fff; display: flex; align-items: center; justify-content: flex-end; box-shadow: 0 1px 4px rgba(0,0,0,.08)">
        <span style="margin-right: 12px">{{ user?.nickname }}</span>
        <el-button size="small" @click="logout">退出</el-button>
      </el-header>
      <el-main style="background: #f6f7fb">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { getUser, clearAuth } from '../utils/auth'
import { useRouter } from 'vue-router'

const router = useRouter()
const user = getUser()

function logout() {
  clearAuth()
  router.push('/admin/login')
}
</script>

<style scoped>
.aside {
  background: #1f2d3d;
}
.brand {
  color: #fff;
  font-size: 17px;
  font-weight: 700;
  padding: 18px 20px;
}
</style>
