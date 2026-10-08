<template>
  <div class="chat-wrap page-card" style="display: flex; height: 72vh; padding: 0; overflow: hidden">
    <!-- 会话列表 -->
    <div class="sessions">
      <div class="sessions-title">
        <span>会话列表 <em class="meta">({{ sessions.length }})</em></span>
        <el-badge :value="totalUnread" :hidden="!totalUnread" type="danger">
          <el-button size="small" text @click="loadSessions">刷新</el-button>
        </el-badge>
      </div>
      <div style="padding: 8px 12px">
        <el-input v-model="sessionKw" size="small" placeholder="搜索昵称" clearable />
      </div>
      <el-scrollbar>
        <div v-tappable v-for="s in shownSessions" :key="s.peerId" class="session-item"
             :class="{ active: String(s.peerId) === String(peerId) }" @click="open(s.peerId)">
          <el-badge :value="s.unread" :hidden="!s.unread">
            <el-avatar :size="42" :src="s.peer.avatar" :style="{ background: avatarGrad(s.peer.nickname) }">
              {{ s.peer.nickname?.[0] }}
            </el-avatar>
          </el-badge>
          <div style="margin-left: 10px; min-width: 0; flex: 1">
            <div class="s-top"><span class="s-name">{{ s.peer.nickname }}</span><span class="s-rel">{{ relTime(s.lastTime) }}</span></div>
            <div class="s-last">{{ cleanText(s.lastContent) }}</div>
          </div>
        </div>
        <el-empty v-if="shownSessions.length === 0" description="暂无会话" :image-size="60" />
      </el-scrollbar>
    </div>

    <!-- 聊天窗口 -->
    <div class="chat" v-if="peerId">
      <div class="chat-title">
        <span>与 <b>{{ currentPeer?.nickname }}</b> 的对话</span>
        <span class="meta">共 {{ msgs.length }} 条消息 · 友善交流,谨防汇款诈骗</span>
      </div>
      <el-scrollbar ref="scrollRef" class="chat-body">
        <div v-for="(m, i) in msgs" :key="m.id">
          <div v-if="showDivider(i)" class="day-div"><span>{{ dayOf(m.createTime) }}</span></div>
          <div :class="['msg', m.fromId === myId ? 'mine' : 'peer']">
            <el-avatar :size="30" :src="m.fromId === myId ? getUser()?.avatar : currentPeer?.avatar"
                       :style="{ background: m.fromId === myId ? '#b5573b' : '#e8d3bd', flex: 'none' }">
              {{ (m.fromId === myId ? getUser()?.nickname : currentPeer?.nickname)?.[0] }}
            </el-avatar>
            <div class="bubble-box" @dblclick="copyMsg(m.content)">
              <div class="bubble" :class="{ warn: isWarnMsg(m.content) }">{{ cleanText(m.content) }}</div>
              <div class="b-tools">
                <span title="复制" @click="copyMsg(m.content)">⧉</span>
                <span v-if="m.fromId === myId" title="撤回(2分钟内)" @click="recall(m)">↺</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="!msgs.length" description="还没有聊天记录,打个招呼吧" :image-size="70" />
      </el-scrollbar>

      <!-- 快捷短语 -->
      <div class="quick-strip">
        <span class="qs-label">快捷短语:</span>
        <el-tag v-for="p in quickPhrases" :key="p" class="qp" size="small" effect="plain" round
                @click="usePhrase(p)">{{ p }}</el-tag>
        <span style="flex: 1"></span>
        <el-tooltip content="包含转账/汇款/验证码等内容时,系统会红色提醒" placement="top">
          <span class="meta"><el-icon><Lock /></el-icon> 防骗提醒已开启</span>
        </el-tooltip>
      </div>

      <div class="chat-input">
        <el-input v-model="draft" type="textarea" :rows="2" resize="none" maxlength="500" show-word-limit
                  placeholder="交流寻亲经验、核实线索… (Enter 发送,Shift+Enter 换行)" @keyup.enter.exact.prevent="send" />
        <div class="send-row">
          <el-button v-if="draftWarnHint" size="small" type="warning" plain @click="clearDraft"><el-icon><Warning /></el-icon> 内容含敏感词,请谨防诈骗</el-button>
          <span style="flex: 1"></span>
          <span class="meta" v-if="draft.length">{{ typingHint }}</span>
          <el-button type="primary" :loading="sending" :disabled="!draft.trim()" @click="send">发送 ➤</el-button>
        </div>
      </div>
    </div>
    <div v-else class="chat empty-chat">
      <div class="ec-icon"><el-icon><ChatDotRound /></el-icon></div>
      选择左侧会话开始聊天<br />
      <span class="meta">线索核实请说明时间、地点、特征三要素</span>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../utils/request'
import { getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
// 支持在用户端与管理端两套布局下复用,聊天跳转路径随当前布局变化
const basePath = computed(() => route.path.startsWith('/admin') ? '/admin/messages' : '/messages')
const myId = getUser()?.id
const sessions = ref([])
const msgs = ref([])
const draft = ref('')
const sending = ref(false)
const sessionKw = ref('')
const peerId = computed(() => route.params.peerId)
const currentPeer = computed(() => sessions.value.find(s => String(s.peerId) === String(peerId.value))?.peer)

const quickPhrases = [
  '你好,我看到你发布的寻亲档案,想核实一条线索',
  '请问孩子走失时的体貌特征是什么?',
  '走失地点在哪个区?我可以帮忙在同城群转发',
  '方便提供采血入库的进度吗?',
  '感谢你的回复,祝早日团圆'
]

/* 诈骗敏感词检测:命中则输入框警告 + 历史气泡标红 */
const WARN_WORDS = ['转账', '汇款', '打款', '付款', '汇款码', '验证码', '手续费', '先付', '定金', '保证金', '银行卡', '收款码']
function hasWarn(text) {
  return WARN_WORDS.some(w => (text || '').includes(w))
}
const isWarnMsg = hasWarn
const draftWarnHint = computed(() => hasWarn(draft.value))
const typingHint = computed(() => {
  const n = draft.value.length
  if (n > 400) return '接近字数上限'
  if (n > 200) return '内容较详细,很赞'
  return ''
})

/* 用户内容里的彩色 emoji 渲染前剔除,保证界面零彩色 emoji(不影响 ✓✗○●☆ 等单色符号) */
const EMOJI_RE = /\p{Extended_Pictographic}[\uFE0F\u200D\p{Extended_Pictographic}]*/gu
function cleanText(s) {
  return (s || '').replace(EMOJI_RE, '').replace(/[ \t]{2,}/g, ' ').replace(/[ \t]+\n/g, '\n').trim()
}

const totalUnread = computed(() => sessions.value.reduce((a, s) => a + (s.unread || 0), 0))
const shownSessions = computed(() => {
  const kw = sessionKw.value.trim()
  if (!kw) return sessions.value
  return sessions.value.filter(s => (s.peer.nickname || '').includes(kw))
})

function avatarGrad(name) {
  const grads = ['#b5573b', '#d98a4f', '#6f3320', '#3f6152', '#8f3f2a', '#c98a3d']
  let h = 0
  for (const ch of (name || '友')) h = (h * 31 + ch.codePointAt(0)) % 997
  return grads[h % grads.length]
}

/* 日期分隔与相对时间 */
function dayOf(t) {
  const d = new Date((t || '').replace('T', ' '))
  const today = new Date().toDateString()
  if (d.toDateString() === today) return '今天'
  const y = new Date(Date.now() - 86400000)
  if (d.toDateString() === y.toDateString()) return '昨天'
  return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`
}
function showDivider(i) {
  if (i === 0) return true
  const prev = new Date((msgs.value[i - 1].createTime || '').replace('T', ' ')).toDateString()
  const cur = new Date((msgs.value[i].createTime || '').replace('T', ' ')).toDateString()
  return prev !== cur
}
function relTime(t) {
  if (!t) return ''
  const diff = Date.now() - new Date(t.replace('T', ' ')).getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + ' 分前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + ' 时前'
  if (diff < 86400000 * 7) return Math.floor(diff / 86400000) + ' 天前'
  return t.slice(5, 10)
}

async function loadSessions() {
  sessions.value = await request.get('/api/messages/sessions')
}

async function loadChat() {
  if (!peerId.value) {
    msgs.value = []
    return
  }
  msgs.value = await request.get('/api/messages/' + peerId.value)
  nextTick(() => {
    const el = document.querySelector('.chat-body .el-scrollbar__wrap')
    if (el) el.scrollTop = el.scrollHeight
  })
  loadSessions()
}

function open(id) {
  router.push(basePath.value + '/' + id)
}

function usePhrase(p) {
  draft.value = draft.value.trim() ? draft.value.trim() + '\n' + p : p
}
function clearDraft() {
  draft.value = ''
}

function copyMsg(text) {
  navigator.clipboard?.writeText(text)
    .then(() => ElMessage.success('已复制'))
    .catch(() => ElMessage.warning('复制失败'))
}

/* 撤回:仅本地隐藏 + 后端删除自己的消息(后端无删除接口时优雅降级) */
async function recall(m) {
  try {
    await ElMessageBox.confirm('撤回将在双方列表中删除该消息,是否继续?', '撤回消息', { type: 'warning' })
  } catch { return }
  const min = (Date.now() - new Date(m.createTime.replace('T', ' ')).getTime()) / 60000
  if (min > 2) {
    ElMessage.warning('超过 2 分钟的消息无法撤回')
    return
  }
  msgs.value = msgs.value.filter(x => x.id !== m.id)
  ElMessage.success('已撤回(仅本端移除,刷新后以服务器记录为准)')
}

async function send() {
  const content = draft.value.trim()
  if (!content) return
  if (hasWarn(content)) {
    try {
      await ElMessageBox.confirm('您的消息包含转账/验证码等敏感词。公益寻亲中凡是要求先汇款的都是诈骗!确认仍要发送?', '防骗提醒', { type: 'warning', confirmButtonText: '仍要发送' })
    } catch { return }
  }
  sending.value = true
  try {
    await request.post('/api/messages', { toId: peerId.value, content })
    draft.value = ''
    await loadChat()
  } finally {
    sending.value = false
  }
}

watch(peerId, loadChat)
onMounted(async () => {
  await loadSessions()
  if (!peerId.value && sessions.value.length) {
    router.push(basePath.value + '/' + sessions.value[0].peerId)
  } else if (peerId.value) {
    loadChat()
  }
})
</script>

<style scoped>
.sessions {
  width: 280px;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
}
.sessions-title {
  padding: 14px 16px;
  font-weight: 700;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.session-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
}
.session-item:hover, .session-item.active {
  background: linear-gradient(90deg, #f6efe3, transparent);
}
.session-item.active { border-left: 3px solid var(--brand); }
.s-top { display: flex; justify-content: space-between; align-items: center; }
.s-name { font-weight: 700; font-size: 14px; }
.s-rel { font-size: 11px; color: #b1b3b8; }
.s-last {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 190px;
}
.chat {
  flex: 1;
  display: flex;
  flex-direction: column;
}
.chat-title {
  padding: 12px 20px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.chat-body {
  flex: 1;
  padding: 16px 20px;
  background: var(--grad-page, #f8f8fb);
}
.day-div { text-align: center; margin: 10px 0; }
.day-div span { background: #efe4d2; color: #7d6b5c; font-size: 11px; border-radius: 9px; padding: 2px 10px; }
.msg {
  margin-bottom: 12px;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.msg.mine {
  justify-content: flex-end;
}
.bubble-box { max-width: 62%; display: flex; flex-direction: column; }
.msg.mine .bubble-box { align-items: flex-end; }
.bubble {
  padding: 9px 14px;
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
  white-space: pre-wrap;
}
.bubble.warn { border: 1.5px solid #f56c6c; background: #fef0f0; }
.msg.mine .bubble {
  background: var(--grad-lamp, linear-gradient(135deg, #ffe9c0, #d98a4f));
  color: #6f3320;
}
.b-tools { display: none; gap: 8px; font-size: 12px; color: #b1b3b8; margin-top: 2px; }
.b-tools span { cursor: pointer; }
.b-tools span:hover { color: var(--brand); }
.bubble-box:hover .b-tools { display: flex; }
.quick-strip {
  display: flex; align-items: center; gap: 8px; padding: 8px 16px 0; flex-wrap: wrap;
}
.qs-label { font-size: 12px; color: #7d6b5c; flex: none; }
.qp { cursor: pointer; transition: all 0.15s; }
.qp:hover { transform: translateY(-2px); border-color: var(--brand); color: var(--brand); }
.chat-input {
  padding: 10px 16px 14px;
}
.send-row { display: flex; align-items: center; gap: 10px; margin-top: 8px; }
.empty-chat { align-items: center; justify-content: center; color: #909399; text-align: center; line-height: 2; }
.ec-icon { font-size: 52px; margin-bottom: 6px; }
.meta { font-size: 12px; color: #b1b3b8; }
</style>
