<template>
  <div>
    <!-- 顶部英雄条 -->
    <div class="qa-hero fade-up">
      <div>
        <h2>寻亲问答互助</h2>
        <p>提问 → 互助 → 点赞沉淀经验:DNA 采血、报案立案、采血流程、寻亲技巧,都在这里抱团解答</p>
      </div>
      <div class="hero-num">
        <div><b>{{ total }}</b><span>个问题</span></div>
        <div><b>{{ hotScore }}</b><span>热度指数</span></div>
      </div>
    </div>

    <!-- 筛选工具条 -->
    <div class="page-card tool-row fade-up" style="animation-delay:.06s">
      <el-input v-model="query.keyword" placeholder="搜索问题标题 / 描述关键字" clearable style="max-width: 300px"
                @keyup.enter="reload" @clear="reload" />
      <div class="cat-chips">
        <button v-for="t in ['全部', ...cats]" :key="t" class="chip" :class="{ on: query.cat === t }"
                @click="pickCat(t)">{{ t }}</button>
      </div>
      <el-segmented v-model="query.sort" :options="[{ label: '最新', value: 'new' }, { label: '最热', value: 'hot' }]"
                    @change="reload" />
      <el-button type="primary" @click="openDialog">＋ 我要提问</el-button>
    </div>

    <el-empty v-if="!loading && list.length === 0" description="暂无符合条件的问题,来提第一个问题吧">
      <el-button type="primary" plain @click="resetQuery">清空条件</el-button>
    </el-empty>

    <div v-loading="loading">
      <div v-tappable class="page-card q-row lift fade-up" v-for="(q, idx) in list" :key="q.id"
           :style="{ animationDelay: (idx % 6) * 0.05 + 's' }" @click="goDetail(q)">
        <div class="q-stats">
          <div><b>{{ q.answerCount || 0 }}</b><br><span class="meta">回复</span></div>
          <div style="margin-top: 6px"><b>{{ q.viewCount || 0 }}</b><br><span class="meta">浏览</span></div>
          <div class="q-score" :class="scoreClass(q)" :title="'热度=赞×3+回复×5+浏览'">{{ heat(q) }}</div>
        </div>
        <div style="flex: 1; min-width: 0">
          <div class="q-title">
            <el-tag v-if="q._cat && q._cat !== '综合'" size="small" :color="catColor(q._cat)" effect="dark" round
                    style="border: none; color: #fff">{{ q._cat }}</el-tag>
            <span v-html="hl(q.title)"></span>
            <el-tag v-if="(q.answerCount || 0) === 0" size="small" type="warning" effect="plain">待解答</el-tag>
            <el-tag v-if="isResolved(q)" size="small" type="success" effect="plain">已收录共识</el-tag>
          </div>
          <div class="q-content" v-html="hl(q.content)"></div>
          <div class="meta">{{ q.userName }} · {{ fmt(q.createTime) }} · 最后回复热度系数 {{ decay(q) }}</div>
        </div>
        <div class="q-side" @click.stop>
          <button class="vote-btn" :class="{ on: q.voted }" @click="vote(q, $event)" :title="q.voted ? '取消点赞' : '觉得有用就点个赞'">
            <span class="vb-icon"><svg viewBox="0 0 16 16" width="15" height="15" aria-hidden="true"><path d="M8 13.5C3.5 9.8 1.2 7.4 1.2 4.9 1.2 3 2.7 1.6 4.5 1.6c1.3 0 2.6.7 3.5 2 .9-1.3 2.2-2 3.5-2 1.8 0 3.3 1.4 3.3 3.3 0 2.5-2.3 4.9-6.8 8.6z" :fill="q.voted ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="1.2" stroke-linejoin="round"/></svg></span>
            <b>{{ q.voteCount || 0 }}</b>
          </button>
          <el-button size="small" type="primary" plain @click="goDetail(q)">进入讨论</el-button>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > query.size" layout="sizes, prev, pager, next, jumper" :total="total"
                   v-model:page-size="query.size" :page-sizes="[10, 20, 50]"
                   v-model:current-page="query.page" @current-change="load" @size-change="reload"
                   style="justify-content: center; margin: 12px 0" />

    <!-- 提问弹窗 -->
    <el-dialog v-model="dialog" title="发布寻亲问答" width="560px">
      <el-form label-position="top">
        <el-form-item label="问题分类" required>
          <div class="cat-picker">
            <span v-tappable v-for="t in cats" :key="t" class="tag-opt" :class="{ on: form.cat === t }"
                  :style="form.cat === t ? { background: catColor(t), borderColor: 'transparent' } : {}"
                  @click="form.cat = t">{{ t }}</span>
          </div>
        </el-form-item>
        <el-form-item label="问题标题" required>
          <el-input v-model="form.title" maxlength="120" show-word-limit
                    placeholder="一句话概括,如:采血入库需要带什么材料?" />
        </el-form-item>
        <el-form-item label="问题描述(至少 10 字)" required>
          <el-input v-model="form.content" type="textarea" :rows="5" maxlength="1000" show-word-limit
                    placeholder="说明您的情况与困惑,便于其他寻亲者和志愿者解答…" />
        </el-form-item>
        <div class="quality-bar">
          <span>描述质量评估:</span>
          <el-progress :percentage="quality" :stroke-width="8" style="flex: 1"
                       :color="quality > 70 ? '#67c23a' : quality > 40 ? '#d98a4f' : '#b5573b'" />
          <em>{{ qualityTip }}</em>
        </div>
      </el-form>
      <template #footer>
        <span class="meta" style="float: left; line-height: 32px">标题分类将展示在列表中,方便同类经验归集</span>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="!canSubmit" @click="submit">发布提问</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const cats = ['DNA采血', '报案立案', '寻亲技巧', '采血流程', '政策信息', '综合']
const CAT_COLORS = { 'DNA采血': '#b5573b', '报案立案': '#6f3320', '寻亲技巧': '#3f6152', '采血流程': '#d98a4f', '政策信息': '#c98a3d', '综合': '#909399' }

const list = ref([])
const total = ref(0)
const loading = ref(false)
const dialog = ref(false)
const submitting = ref(false)
const form = ref({ title: '', content: '', cat: '综合' })
const query = ref({ page: 1, size: 10, keyword: '', sort: 'new', cat: '全部' })

/* 热度指数:当前页平均热度,给页面一个整体氛围分 */
const hotScore = computed(() => {
  if (!list.value.length) return 0
  const s = list.value.reduce((acc, q) => acc + heat(q), 0)
  return Math.round(s / list.value.length)
})

/* 后端不存分类,用标题+描述关键词本地归类,与展示前缀 [分类] 双保险 */
const CAT_RULES = {
  'DNA采血': ['dna', '采血', '血样', '入库', '血库', '亲缘'],
  '报案立案': ['报案', '立案', '公安', '警察', '派出所', '刑侦'],
  '寻亲技巧': ['找', '寻', '怎么', '如何', '经验', '技巧', '扩散', '转发'],
  '采血流程': ['流程', '材料', '证件', '手续', '去哪', '怎么办'],
  '政策信息': ['政策', '补贴', '免费', '规定', '通知', '文件']
}
function classify(q) {
  const m = /^\s*\[([^\]]+)\]/.exec(q.title || '')
  if (m && CAT_RULES.hasOwnProperty(m[1])) return m[1]
  const text = ((q.title || '') + ' ' + (q.content || '')).toLowerCase()
  let best = '综合', bestHit = 0
  for (const [cat, kws] of Object.entries(CAT_RULES)) {
    const hit = kws.filter(k => text.includes(k)).length
    if (hit > bestHit) { best = cat; bestHit = hit }
  }
  return best
}

/* 热度公式与后端一致:赞*3+回复*5+浏览 */
function heat(q) {
  return (q.voteCount || 0) * 3 + (q.answerCount || 0) * 5 + (q.viewCount || 0)
}
function scoreClass(q) {
  const h = heat(q)
  return h >= 30 ? 's-hot' : h >= 12 ? 's-mid' : 's-low'
}
/* 时间衰减系数:越新的问题潜力分越高(展示用微算法) */
function decay(q) {
  const days = Math.max(0, (Date.now() - new Date((q.createTime || '').replace('T', ' '))) / 86400000)
  return (1 / (1 + days / 30)).toFixed(2)
}
function isResolved(q) {
  return (q.voteCount || 0) >= 3 || (q.answerCount || 0) >= 5
}

function catColor(cat) { return CAT_COLORS[cat] || '#909399' }
function fmt(t) { return (t || '').replace('T', ' ').slice(0, 16) }
function hl(text) {
  const esc = (text || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const kw = query.value.keyword.trim()
  if (!kw) return esc.replace(/^(\[[^\]]+\])/, '<mark class="kw cat">$1</mark>')
  return esc.replace(new RegExp(kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi'), '<mark class="kw">$&</mark>')
}

function pickCat(t) {
  query.value.cat = t
  reload()
}

async function load() {
  loading.value = true
  try {
    const p = { page: query.value.page, size: query.value.size, sort: query.value.sort }
    if (query.value.keyword) p.keyword = query.value.keyword
    const data = await request.get('/api/questions', { params: p })
    let records = data.records.map(q => ({ ...q, _cat: classify(q) }))
    if (query.value.cat !== '全部') {
      records = records.filter(q => q._cat === query.value.cat)
    }
    list.value = records
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function reload() {
  query.value.page = 1
  load()
}
function resetQuery() {
  query.value = { page: 1, size: 10, keyword: '', sort: 'new', cat: '全部' }
  load()
}

/* 分类本地筛选时前端分页会失真,统一按页取即可(数据量小,可接受) */
function goDetail(q) {
  router.push('/questions/' + q.id)
}

async function vote(q, e) {
  const data = await request.post(`/api/questions/${q.id}/vote`)
  q.voteCount = data.voteCount
  q.voted = data.mine
  // 点赞心跳动画
  const btn = e.currentTarget
  btn.classList.remove('beat'); void btn.offsetWidth; btn.classList.add('beat')
  ElMessage({ message: data.mine ? '已点赞,感谢你的认可' : '已取消点赞', duration: 1500 })
}

/* 质量评估:长度/问号/数字关键字 */
const quality = computed(() => {
  const t = form.value.title.trim(), c = form.value.content.trim()
  let s = 0
  s += Math.min(t.length / 10, 1) * 30
  s += Math.min(c.length / 50, 1) * 40
  if (/[?？]/.test(t + c)) s += 15
  if (form.value.cat !== '综合') s += 15
  return Math.round(s)
})
const qualityTip = computed(() => {
  const q = quality.value
  if (q >= 85) return '描述清晰,容易获得解答'
  if (q >= 60) return '再补充时间/地点/经过会更好'
  if (q >= 30) return '描述偏简略,建议展开说明'
  return '请认真填写标题与描述'
})
const canSubmit = computed(() => form.value.title.trim() && form.value.content.trim().length >= 10)

function openDialog() {
  dialog.value = true
}

async function submit() {
  submitting.value = true
  try {
    const cat = form.value.cat && form.value.cat !== '综合' ? `[${form.value.cat}] ` : ''
    await request.post('/api/questions', { title: cat + form.value.title.trim(), content: form.value.content.trim() })
    ElMessage.success('提问成功,欢迎邀请其他寻亲者作答')
    dialog.value = false
    form.value = { title: '', content: '', cat: '综合' }
    reload()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.qa-hero {
  display: flex; justify-content: space-between; align-items: center;
  background: var(--grad-sunrise); border-radius: 16px; padding: 20px 28px; color: #fff;
  box-shadow: 0 8px 24px rgba(181, 87, 59, 0.25);
}
.qa-hero h2 { margin: 0 0 6px; font-size: 22px; letter-spacing: 1px; }
.qa-hero p { margin: 0; font-size: 12.5px; opacity: 0.92; }
.hero-num { display: flex; gap: 24px; text-align: center; }
.hero-num b { font-size: 26px; display: block; }
.hero-num span { font-size: 12px; opacity: 0.85; }

.tool-row { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; margin-top: 14px; }
.cat-chips { display: flex; gap: 6px; flex-wrap: wrap; flex: 1; }
.chip {
  border: 1px solid #e4d8c4; background: #fff; border-radius: 15px; padding: 3px 12px;
  font-size: 12.5px; color: #6f5641; cursor: pointer; transition: all 0.18s;
}
.chip:hover { border-color: var(--brand); color: var(--brand); }
.chip.on { background: var(--grad-hero); color: #fff; border-color: transparent; }

.q-row { display: flex; align-items: center; gap: 18px; margin-top: 12px; cursor: pointer; }
.q-stats { text-align: center; color: #606266; font-size: 13px; flex: none; width: 62px; }
.q-score {
  margin-top: 6px; border-radius: 8px; font-size: 12px; font-weight: 700; padding: 2px 0; color: #fff;
}
.s-hot { background: linear-gradient(135deg, #b5573b, #d98a4f); }
.s-mid { background: linear-gradient(135deg, #d98a4f, #c98a3d); }
.s-low { background: #e4d8c4; color: #7d6b5c; }
.q-title { font-size: 16px; font-weight: 700; color: #303133; display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.q-content {
  font-size: 13px; color: #909399; margin: 6px 0;
  display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
.q-side { flex: none; display: flex; flex-direction: column; align-items: center; gap: 10px; }
.vote-btn {
  display: flex; align-items: center; gap: 6px; border: 1px solid #e4d8c4; background: #fffdf8;
  border-radius: 16px; padding: 5px 14px; cursor: pointer; font-size: 13px; color: #8f3f2a; transition: all 0.18s;
}
.vote-btn:hover { transform: translateY(-2px); box-shadow: 0 4px 10px rgba(181, 87, 59, 0.2); }
.vote-btn.on { background: var(--grad-hero); color: #fff; border-color: transparent; }
.vb-icon { font-size: 15px; display: inline-flex; }
.vote-btn.beat { animation: beat 0.45s ease; }
@keyframes beat {
  0% { transform: scale(1); } 30% { transform: scale(1.25); } 60% { transform: scale(0.92); } 100% { transform: scale(1); }
}
.meta { font-size: 12px; color: #b1b3b8; }
.tag-opt {
  border: 1px solid #e4d8c4; border-radius: 14px; padding: 3px 12px; font-size: 12.5px;
  color: #6f5641; cursor: pointer; transition: all 0.18s; user-select: none;
}
.tag-opt.on { color: #fff; }
.cat-picker { display: flex; flex-wrap: wrap; gap: 8px; }
.quality-bar { display: flex; align-items: center; gap: 10px; font-size: 12.5px; color: #6f5641; }
.quality-bar em { font-style: normal; font-size: 12px; color: #7d6b5c; white-space: nowrap; }
</style>
<style>
mark.kw { background: #ffe3a3; color: #8a5a00; border-radius: 3px; padding: 0 2px; }
mark.kw.cat { background: transparent; color: inherit; padding: 0; }
</style>
