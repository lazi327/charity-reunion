<template>
  <div v-loading="loading">
    <!-- 书架式头部 -->
    <div class="gd-hero fade-up">
      <div>
        <h2><el-icon><Reading /></el-icon> 寻亲指南 · 知识书架</h2>
        <p>由平台管理员与志愿者整理的寻亲知识与政策指引 · 共 {{ guides.length }} 篇 · 已读 {{ readIds.size }} 篇</p>
      </div>
      <el-input v-model="keyword" placeholder="在标题中检索指南" clearable style="max-width: 260px" />
    </div>

    <el-empty v-if="!loading && shown.length === 0" description="没有匹配的指南,换个关键字试试">
      <el-button type="primary" plain @click="keyword = ''">清空检索</el-button>
    </el-empty>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="8" v-for="(g, i) in shown" :key="g.id">
        <div v-tappable class="gd-card lift fade-up" :style="{ animationDelay: (i % 9) * 0.05 + 's' }" @click="$router.push('/guides/' + g.id)">
          <div class="gd-cover" :style="{ background: coverGrad(g.id) }">
            <el-image v-if="g.cover" :src="g.cover" fit="cover" class="cover-img" />
            <template v-else>
              <span class="cover-icon"><el-icon><component :is="bookIcon(g.title)" /></el-icon></span>
              <span class="cover-idx">NO.{{ String(g.id).padStart(2, '0') }}</span>
            </template>
            <span v-if="readIds.has(g.id)" class="read-badge">✓ 已读</span>
          </div>
          <div class="gd-body">
            <div class="gd-title" v-html="hl(g.title)"></div>
            <div class="gd-meta">
              <span>{{ (g.createTime || '').slice(0, 10) }} 发布</span>
              <span class="est" :title="'按正文长度估算,进入详情页可见精确值'">约 {{ estMinutes(g.title) }} 分钟</span>
            </div>
            <div class="gd-tags">
              <el-tag v-for="t in tagsOf(g.title)" :key="t" size="small" effect="light" round class="gt">{{ t }}</el-tag>
            </div>
          </div>
          <div class="gd-progress"><i :style="{ width: readIds.has(g.id) ? '100%' : '8%' }"></i></div>
        </div>
      </el-col>
    </el-row>

    <div class="page-card tip-card fade-up" v-if="guides.length">
      <b><el-icon><Guide /></el-icon> 阅读顺序建议</b>
      <p v-for="(s, i) in readingPath" :key="i" class="path-row">
        <span class="step">{{ i + 1 }}</span>{{ s.text }}<em>{{ s.tip }}</em>
      </p>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'

const guides = ref([])
const loading = ref(true)
const keyword = ref('')
const readIds = ref(new Set(JSON.parse(localStorage.getItem('guide_read') || '[]')))

/* 标题检索 + 关键字高亮(先转义防 XSS) */
const shown = computed(() => {
  const kw = keyword.value.trim()
  if (!kw) return guides.value
  return guides.value.filter(g => (g.title || '').includes(kw))
})
function hl(text) {
  const esc = (text || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const kw = keyword.value.trim()
  if (!kw) return esc
  return esc.replace(new RegExp(kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi'), '<mark>$&</mark>')
}

/* 封面渐变:按 id 散列到 6 套主题渐变,稳定不闪 */
const GRADS = [
  'linear-gradient(135deg,#b5573b,#8f3f2a)', 'linear-gradient(135deg,#d98a4f,#b5573b)',
  'linear-gradient(135deg,#6f3320,#8f3f2a)', 'linear-gradient(135deg,#3f6152,#6f3320)',
  'linear-gradient(135deg,#c98a3d,#d98a4f)', 'linear-gradient(135deg,#8f3f2a,#6f3320)'
]
function coverGrad(id) { return GRADS[id % GRADS.length] }

/* 按标题关键词挑书本图标,纯本地小算法 */
const ICON_RULES = [
  [['dna', '采血', '血'], 'Connection'], [['报案', '公安', '警', '立案'], 'Stamp'],
  [['政策', '规定', '补贴'], 'Document'], [['技巧', '方法', '怎么', '经验'], 'Opportunity'],
  [['儿童', '孩子', '宝贝', ' kid'], 'Avatar'], [['防拐', '骗', '安全'], 'Lock']
]
function bookIcon(title) {
  const t = (title || '').toLowerCase()
  for (const [kws, icon] of ICON_RULES) if (kws.some(k => t.includes(k))) return icon
  return 'Reading'
}
/* 标题关键字自动打标签 */
function tagsOf(title) {
  const tags = []
  const t = title || ''
  if (/采血|dna/i.test(t)) tags.push('DNA')
  if (/报案|公安|警察/.test(t)) tags.push('报警')
  if (/政策|规定|通知/.test(t)) tags.push('政策')
  if (/技巧|经验|方法/.test(t)) tags.push('技巧')
  if (/防|骗/.test(t)) tags.push('防骗')
  if (!tags.length) tags.push('通识')
  return tags
}
/* 列表页无正文,用标题长度给个趣味估算区间;详情页有精确值 */
function estMinutes(title) {
  return 3 + ((title || '').length % 5)
}

/* 阅读路径建议:从现有指南标题中匹配排序 */
const readingPath = computed(() => {
  const order = [
    { kw: ['报案', '立案', '公安'], text: '第一时间报警立案,掌握黄金时间', tip: '走失 24 小时内是黄金期,报警不需要等 24 小时' },
    { kw: ['采血', 'dna'], text: '到公安 DNA 实验室免费采血入库', tip: '父母双方血样都采集,比对概率更高' },
    { kw: ['技巧', '经验', '方法'], text: '学习扩散与寻亲技巧', tip: '转发到走失地同城群,扩散越快希望越大' },
    { kw: ['政策', '规定'], text: '了解相关政策法规', tip: '' }
  ]
  const res = []
  for (const o of order) {
    const hit = guides.value.find(g => o.kw.some(k => (g.title || '').toLowerCase().includes(k)))
    if (hit) res.push({ text: `${o.text}：《${hit.title}》`, tip: o.tip })
  }
  if (!res.length) res.push({ text: '按发布时间从上往下阅读即可', tip: '' })
  return res.slice(0, 4)
})

onMounted(async () => {
  try {
    guides.value = await request.get('/api/guides')
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.gd-hero {
  display: flex; justify-content: space-between; align-items: center; gap: 16px; flex-wrap: wrap;
  background: var(--grad-sunrise); border-radius: 16px; padding: 22px 28px; color: #fff;
  box-shadow: 0 8px 22px rgba(232, 132, 60, 0.28);
}
.gd-hero h2 { margin: 0 0 6px; font-size: 22px; letter-spacing: 1px; }
.gd-hero p { margin: 0; font-size: 12.5px; opacity: 0.92; }
.gd-card {
  background: #fff; border-radius: 14px; overflow: hidden; cursor: pointer; margin-bottom: 16px;
  box-shadow: 0 2px 10px rgba(143, 63, 42, 0.08); position: relative;
}
.gd-cover { height: 140px; position: relative; display: flex; align-items: center; justify-content: center; }
.cover-img { width: 100%; height: 100%; }
.cover-icon { font-size: 52px; color: #fff; filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.2)); }
.cover-idx { position: absolute; right: 12px; bottom: 8px; color: rgba(255, 255, 255, 0.85); font-size: 12px; font-weight: 700; letter-spacing: 1px; }
.read-badge {
  position: absolute; left: 10px; top: 10px; background: rgba(255, 255, 255, 0.9);
  color: #2c9c58; font-size: 11.5px; border-radius: 10px; padding: 2px 9px; font-weight: 700;
}
.gd-body { padding: 12px 14px 10px; }
.gd-title { font-weight: 700; font-size: 15.5px; color: #303133; min-height: 44px; line-height: 1.5; }
.gd-meta { display: flex; justify-content: space-between; font-size: 12px; color: #b1b3b8; margin-top: 6px; }
.est { color: #6f3320; }
.gd-tags { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }
.gt { background: #f6efe3; color: #8f3f2a; border: none; }
.gd-progress { height: 4px; background: #efe4d2; }
.gd-progress i { display: block; height: 100%; background: var(--grad-hero); transition: width 0.6s; }
.tip-card { margin-top: 6px; }
.path-row { display: flex; align-items: center; gap: 10px; font-size: 13.5px; color: #444; margin: 10px 0; }
.path-row em { font-style: normal; font-size: 12px; color: #7d6b5c; margin-left: auto; }
.step {
  width: 22px; height: 22px; border-radius: 50%; background: var(--grad-hero); color: #fff;
  display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex: none;
}
</style>
<style>
.gd-card mark { background: #ffe3a3; color: #8a5a00; border-radius: 3px; padding: 0 2px; }
</style>
