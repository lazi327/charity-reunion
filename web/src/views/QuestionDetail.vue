<template>
  <div v-loading="loading">
    <div class="qd-wrap" v-if="q">
      <div class="page-card main-col fade-up">
        <div class="q-head">
          <el-tag v-if="q._cat" size="small" :color="catColor(q._cat)" effect="dark" style="border: none; color: #fff">
            {{ q._cat }}
          </el-tag>
          <el-tag v-if="q.status === 1" type="danger" size="small">该问题已被管理员屏蔽</el-tag>
        </div>
        <h2 style="margin: 6px 0 10px">{{ q.title }}</h2>
        <div class="meta">
          提问者:{{ q.userName }} · {{ fmt(q.createTime) }} · {{ q.viewCount }} 次浏览 ·
          <span class="heat-tag" :class="heatClass">社区热度 {{ heat }}</span>
        </div>
        <el-divider />
        <p class="q-body">{{ q.content }}</p>

        <div class="act-bar">
          <button class="vote-big" :class="{ on: q.voted }" @click="vote">
            <svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true" style="vertical-align:-2px"><path d="M8 13.5C3.5 9.8 1.2 7.4 1.2 4.9 1.2 3 2.7 1.6 4.5 1.6c1.3 0 2.6.7 3.5 2 .9-1.3 2.2-2 3.5-2 1.8 0 3.3 1.4 3.3 3.3 0 2.5-2.3 4.9-6.8 8.6z" :fill="q.voted ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/></svg> {{ q.voted ? '已赞' : '觉得有用' }} · <b>{{ q.voteCount || 0 }}</b>
          </button>
          <el-button plain size="default" @click="copyLink"><el-icon><Link /></el-icon> 复制问题链接</el-button>
          <el-button v-if="!isMine" plain size="default" @click="invite"><el-icon><Message /></el-icon> 私信邀请志愿者回答</el-button>
          <el-button text @click="$router.back()">返回列表</el-button>
        </div>

        <el-divider />
        <div class="ans-head">
          <b>全部回复({{ answers.length }})</b>
          <el-segmented v-model="ansSort" :options="[{ label: '时间序', value: 'time' }, { label: '长答优先', value: 'len' }]" size="small" />
        </div>
        <el-empty v-if="answers.length === 0" description="还没有人回复,快来分享经验吧" :image-size="60" />
        <transition-group name="ans">
          <div v-for="(a, i) in sortedAnswers" :key="a.id" class="answer-row" :class="{ first: i === 0 && sortedAnswers.length > 1 }">
            <el-avatar :size="38" :style="{ background: avatarGrad(a.userName), flex: 'none' }">{{ a.userName?.[0] }}</el-avatar>
            <div style="flex: 1">
              <div class="a-name">
                {{ a.userName }}
                <el-tag v-if="a.userId === q.userId" size="small" type="warning" effect="plain">提问者</el-tag>
                <el-tag v-if="i === 0 && ansSort === 'len' && a.content.length > 80" size="small" type="success" effect="plain">详实回答</el-tag>
              </div>
              <p class="a-body">{{ a.content }}</p>
              <div class="meta">{{ fmt(a.createTime) }} · {{ a.content.length }} 字</div>
            </div>
          </div>
        </transition-group>

        <el-divider />
        <div v-if="!isMine">
          <b>我来回答</b>
          <div class="quick-chips">
            <span class="qc-label">快捷开头:</span>
            <el-tag v-for="t in replyTemplates" :key="t.label" class="qc" effect="plain" round @click="draft = t.text">
              {{ t.label }}
            </el-tag>
          </div>
          <el-input v-model="draft" type="textarea" :rows="4" maxlength="1000" show-word-limit style="margin-top: 10px"
                    placeholder="分享您的寻亲经验、政策信息或建议…" />
          <div class="send-row">
            <el-progress v-if="draft.length" :percentage="Math.min(100, Math.round(draft.length / 500 * 100))"
                         :stroke-width="6" :show-text="false" style="width: 120px" />
            <span class="meta" v-if="draft.trim().length < 5">回复至少 5 个字</span>
            <span style="flex: 1"></span>
            <el-button type="primary" :loading="sending" :disabled="draft.trim().length < 5" @click="send">提交回答</el-button>
          </div>
        </div>
        <el-alert v-else type="info" :closable="false" title="这是您自己发布的问题,可在下方查看其他用户的解答,也可补充描述后等待更多人看到" />
      </div>

      <!-- 侧栏:问题名片摘要 -->
      <aside class="qd-side fade-up" style="animation-delay:.1s">
        <div class="page-card">
          <b class="grad-text">问题概况</b>
          <div class="side-item"><span>回复数</span><b>{{ answers.length }}</b></div>
          <div class="side-item"><span>浏览量</span><b>{{ q.viewCount || 0 }}</b></div>
          <div class="side-item"><span>点赞数</span><b>{{ q.voteCount || 0 }}</b></div>
          <div class="side-item"><span>提出天数</span><b>{{ daysAgo }} 天</b></div>
          <el-divider />
          <div class="side-score" :style="ringStyle">
            <span>{{ heat }}</span>
            <em>综合热度</em>
          </div>
          <p class="side-tip">热度 = 点赞×3 + 回复×5 + 浏览,与列表页最热排序同源</p>
        </div>
        <div class="page-card" style="margin-top: 16px">
          <b><el-icon><InfoFilled /></el-icon> 提问小课堂</b>
          <p class="side-p">· 一句话把「想解决什么」写进标题</p>
          <p class="side-p">· 描述补充时间、地点、已做的尝试</p>
          <p class="side-p">· 得到解答后给回答者点个赞,经验能帮到更多人</p>
        </div>
      </aside>
    </div>
  </div>
  <el-backtop :right="30" :bottom="30" />
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'
import { getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const q = ref(null)
const answers = ref([])
const draft = ref('')
const loading = ref(true)
const sending = ref(false)
const ansSort = ref('time')

const isMine = computed(() => q.value && String(q.value.userId) === String(getUser()?.id))
const heat = computed(() => {
  if (!q.value) return 0
  return (q.value.voteCount || 0) * 3 + (q.value.answerCount || 0) * 5 + (q.value.viewCount || 0)
})
const heatClass = computed(() => heat.value >= 30 ? 'h-hot' : heat.value >= 12 ? 'h-mid' : 'h-low')
const daysAgo = computed(() => {
  if (!q.value?.createTime) return 0
  return Math.max(0, Math.floor((Date.now() - new Date(q.value.createTime.replace('T', ' '))) / 86400000))
})
const ringStyle = computed(() => {
  const p = Math.min(100, heat.value)
  const color = heat.value >= 30 ? '#b5573b' : heat.value >= 12 ? '#d98a4f' : '#6f3320'
  return { background: `conic-gradient(${color} ${p * 3.6}deg, #efe4d2 0deg)` }
})

const sortedAnswers = computed(() => {
  const arr = [...answers.value]
  if (ansSort.value === 'len') arr.sort((a, b) => b.content.length - a.content.length)
  return arr
})

const CAT_COLORS = { 'DNA采血': '#b5573b', '报案立案': '#6f3320', '寻亲技巧': '#3f6152', '采血流程': '#d98a4f', '政策信息': '#c98a3d' }
function classify(title) {
  const m = /^\s*\[([^\]]+)\]/.exec(title || '')
  return m && CAT_COLORS[m[1]] ? m[1] : ''
}
function catColor(cat) { return CAT_COLORS[cat] || '#909399' }

const replyTemplates = [
  { label: '亲身经历', text: '我/我家亲戚经历过类似情况,当时的做法是:' },
  { label: '政策要点', text: '根据公安部儿童失踪信息紧急发布平台的规定,' },
  { label: 'DNA相关', text: '采血入库是免费的,可携带身份证到全国任意公安 DNA 实验室采集,' },
  { label: '提醒防骗', text: '提醒各位家长:凡是要求先汇款"买线索""捞人"的都是骗子,请务必核实!' }
]

function avatarGrad(name) {
  const grads = ['#b5573b', '#d98a4f', '#6f3320', '#3f6152', '#8f3f2a', '#c98a3d']
  let h = 0
  for (const ch of (name || '友')) h = (h * 31 + ch.codePointAt(0)) % 997
  return grads[h % grads.length]
}
function fmt(t) { return (t || '').replace('T', ' ').slice(0, 16) }

async function load() {
  loading.value = true
  try {
    q.value = await request.get('/api/questions/' + route.params.id)
    q.value._cat = classify(q.value.title)
    answers.value = await request.get(`/api/questions/${route.params.id}/answers`)
  } finally {
    loading.value = false
  }
}

async function vote() {
  const data = await request.post(`/api/questions/${route.params.id}/vote`)
  q.value.voteCount = data.voteCount
  q.value.voted = data.mine
  ElMessage({ message: data.mine ? '感谢你的点赞' : '已取消点赞', duration: 1500 })
}

async function send() {
  sending.value = true
  try {
    await request.post(`/api/questions/${route.params.id}/answers`, { content: draft.value.trim() })
    ElMessage.success('回答已发布,感谢您帮助其他寻亲家庭')
    draft.value = ''
    load()
  } finally {
    sending.value = false
  }
}

function copyLink() {
  const link = location.origin + '/questions/' + route.params.id
  navigator.clipboard?.writeText(link).then(() => ElMessage.success('链接已复制,快粘贴到群里扩散'))
    .catch(() => ElMessage.warning('复制失败,请手动复制地址栏'))
}

function invite() {
  if (!q.value) return
  router.push('/messages/' + q.value.userId)
}

onMounted(load)
</script>

<style scoped>
.qd-wrap { display: grid; grid-template-columns: 1fr 300px; gap: 16px; align-items: start; }
.q-head { display: flex; gap: 8px; }
.q-body { color: #606266; line-height: 1.9; white-space: pre-wrap; }
.act-bar { display: flex; gap: 10px; align-items: center; flex-wrap: wrap; margin-top: 14px; }
.vote-big {
  border: 1px solid #e4d8c4; background: #fffdf8; color: #8f3f2a; border-radius: 20px;
  padding: 7px 18px; font-size: 14px; cursor: pointer; transition: all 0.2s;
}
.vote-big:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(181, 87, 59, 0.25); }
.vote-big.on { background: var(--grad-hero); color: #fff; border-color: transparent; }
.ans-head { display: flex; justify-content: space-between; align-items: center; }
.answer-row { display: flex; gap: 12px; padding: 14px 0; border-bottom: 1px dashed #e4d8c4; }
.answer-row:first-of-type { border-top: 1px dashed #e4d8c4; }
.answer-row.first { background: linear-gradient(90deg, #fdf6ee, transparent); border-radius: 8px; padding-left: 8px; }
.a-name { font-weight: 700; font-size: 14px; display: flex; align-items: center; gap: 6px; }
.a-body { color: #606266; line-height: 1.8; margin-top: 4px; white-space: pre-wrap; }
.quick-chips { margin: 10px 0 0; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.qc-label { font-size: 12px; color: #7d6b5c; }
.qc { cursor: pointer; transition: transform 0.15s; }
.qc:hover { transform: translateY(-2px); }
.send-row { display: flex; align-items: center; gap: 12px; margin-top: 10px; }
.heat-tag { font-weight: 700; }
.h-hot { color: var(--brand-deep); }
.h-mid { color: #d98a4f; }
.h-low { color: #909399; }
.meta { font-size: 12px; color: #b1b3b8; }
.qd-side { position: sticky; top: 84px; }
.side-item { display: flex; justify-content: space-between; font-size: 13.5px; color: #6f5641; padding: 5px 0; }
.side-score {
  width: 84px; height: 84px; border-radius: 50%; margin: 8px auto 6px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.side-score span { width: 64px; height: 64px; border-radius: 50%; background: #fff; display: flex; align-items: center; justify-content: center; font-weight: 800; color: #8f3f2a; font-size: 18px; }
.side-score em { position: absolute; margin-top: 62px; font-style: normal; font-size: 11px; color: #7d6b5c; }
.side-tip { font-size: 11.5px; color: #7d6b5c; text-align: center; margin: 4px 0 0; }
.side-p { font-size: 12.5px; color: #6f5641; margin: 6px 0; line-height: 1.7; }
.ans-enter-active { transition: all 0.4s ease; }
.ans-enter-from { opacity: 0; transform: translateY(14px); }
</style>
