<template>
  <div v-loading="loading">
    <div class="gt-wrap" v-if="g">
      <!-- 正文 -->
      <div class="page-card article-col fade-up">
        <div class="art-head">
          <h2 class="art-title">{{ plainTitle }}</h2>
          <div class="meta">
            发布于 {{ (g.createTime || '').slice(0, 10) }} · 全文约 {{ charCount }} 字 · 预计阅读 {{ readMinutes }} 分钟
          </div>
        </div>

        <!-- 阅读工具条 -->
        <div class="tool-strip">
          <span class="ts-label">字号</span>
          <el-button-group size="small">
            <el-button v-for="fs in [14, 16, 18, 20]" :key="fs" :type="fontSize === fs ? 'primary' : ''"
                       @click="setFontSize(fs)">{{ fs }}</el-button>
          </el-button-group>
          <el-button size="small" plain @click="toggleRead">
            {{ isRead ? '✓ 已收藏到已读' : '☆ 标记已读' }}
          </el-button>
          <el-button size="small" plain @click="copyArticle"><el-icon><DocumentCopy /></el-icon> 复制全文</el-button>
          <el-button size="small" plain @click="printArticle"><el-icon><Printer /></el-icon> 打印 / 存 PDF</el-button>
          <span style="flex: 1"></span>
          <span class="progress-text">{{ Math.round(scrollPct) }}%</span>
        </div>

        <el-divider />
        <div ref="bodyEl" class="rich-body" :style="{ fontSize: fontSize + 'px' }" v-html="g.content"></div>

        <el-divider />
        <div class="end-bar">
          <span class="meta">读完了?把这篇转给需要的寻亲家庭,扩散越快希望越大 <Lamp /></span>
          <el-button type="primary" plain @click="copyLink">复制本文链接</el-button>
          <el-button @click="$router.push('/guides')">返回指南书架</el-button>
        </div>
      </div>

      <!-- 侧边目录 TOC -->
      <aside class="gt-side fade-up" style="animation-delay:.1s">
        <div class="page-card toc-card">
          <b><el-icon><Memo /></el-icon> 本章目录</b>
          <el-empty v-if="!toc.length" description="正文暂无小标题" :image-size="50" />
          <a v-for="(t, i) in toc" :key="t.id" class="toc-item" :class="{ lv3: t.level === 3, active: activeId === t.id }"
             :href="'#' + t.id" @click.prevent="jumpTo(t.id)">{{ t.text }}</a>
          <template v-if="toc.length">
            <el-divider />
            <div class="toc-progress">
              <b class="grad-text">{{ Math.round(scrollPct) }}%</b>
              <span class="meta">阅读进度</span>
            </div>
          </template>
        </div>
      </aside>
    </div>

    <!-- 顶部阅读进度条 -->
    <div class="read-bar" :style="{ width: scrollPct + '%' }"></div>
    <el-backtop :right="30" :bottom="30" />
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const route = useRoute()
const g = ref(null)
const loading = ref(true)
const bodyEl = ref(null)
const toc = ref([])
const activeId = ref('')
const fontSize = ref(Number(localStorage.getItem('guide_font') || 16))
const scrollPct = ref(0)

const plainTitle = computed(() => (g.value?.title || '').replace(/^\s*\[[^\]]+\]\s*/, ''))
const charCount = computed(() => {
  if (!g.value?.content) return 0
  return g.value.content.replace(/<[^>]+>/g, '').replace(/\s/g, '').length
})
/* 阅读时长:中文 350 字/分钟,图片每个加 20 秒 */
const readMinutes = computed(() => {
  const imgs = (g.value?.content || '').match(/<img/g)?.length || 0
  return Math.max(1, Math.round((charCount.value / 350) + imgs * (20 / 60)))
})
const isRead = computed(() => {
  const ids = JSON.parse(localStorage.getItem('guide_read') || '[]')
  return g.value && ids.includes(g.value.id)
})

/* 从渲染后的 DOM 抽取 h2/h3 生成目录锚点 */
function buildToc() {
  if (!bodyEl.value) return
  const heads = bodyEl.value.querySelectorAll('h2, h3')
  const arr = []
  heads.forEach((h, i) => {
    if (!h.id) h.id = 'sec-' + (i + 1) + '-' + Date.now() % 100000
    arr.push({ id: h.id, text: h.textContent.trim(), level: Number(h.tagName[1]) })
  })
  toc.value = arr
}

function jumpTo(id) {
  document.getElementById(id)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  activeId.value = id
}

function onScroll() {
  const doc = document.documentElement
  const total = doc.scrollHeight - doc.clientHeight
  scrollPct.value = total > 0 ? Math.min(100, (doc.scrollTop / total) * 100) : 0
  // 高亮当前可视区章节
  let cur = ''
  for (const t of toc.value) {
    const el = document.getElementById(t.id)
    if (el && el.getBoundingClientRect().top < 140) cur = t.id
  }
  if (cur) activeId.value = cur
}

function setFontSize(fs) {
  fontSize.value = fs
  localStorage.setItem('guide_font', String(fs))
}

function toggleRead() {
  let ids = JSON.parse(localStorage.getItem('guide_read') || '[]')
  if (ids.includes(g.value.id)) {
    ids = ids.filter(x => x !== g.value.id)
    ElMessage.info('已取消已读标记')
  } else {
    ids.push(g.value.id)
    ElMessage.success('已标记已读,指南书架会显示 ✓')
  }
  localStorage.setItem('guide_read', JSON.stringify(ids))
}

function plainText() {
  const d = document.createElement('div')
  d.innerHTML = g.value.content || ''
  return `${g.value.title}\n\n${d.textContent.trim()}`
}
function copyArticle() {
  navigator.clipboard?.writeText(plainText())
    .then(() => ElMessage.success('全文已复制,可粘贴到群里分享'))
    .catch(() => ElMessage.warning('复制失败,请手动选择正文'))
}
function copyLink() {
  navigator.clipboard?.writeText(location.origin + '/guides/' + g.value.id)
    .then(() => ElMessage.success('链接已复制'))
}
function printArticle() {
  const w = window.open('', '_blank')
  if (!w) { ElMessage.warning('浏览器拦截了新窗口'); return }
  w.document.write(`<html><head><title>${g.value.title}</title>
    <style>body{font-family:sans-serif;max-width:720px;margin:20px auto;line-height:1.9}img{max-width:100%}</style>
    </head><body><h1>${g.value.title}</h1>${g.value.content}</body></html>`)
  w.document.close()
  w.focus()
  setTimeout(() => w.print(), 300)
}

onMounted(async () => {
  try {
    g.value = await request.get('/api/guides/' + route.params.id)
    await nextTick()
    buildToc()
    window.addEventListener('scroll', onScroll, { passive: true })
    onScroll()
  } finally {
    loading.value = false
  }
})
onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))
</script>

<style scoped>
.gt-wrap { display: grid; grid-template-columns: 1fr 260px; gap: 16px; align-items: start; }
.art-head { text-align: center; }
.art-title { font-size: 22px; margin: 0 0 8px; letter-spacing: 1px; }
.meta { font-size: 12px; color: #b1b3b8; }
.tool-strip { display: flex; align-items: center; gap: 10px; margin-top: 14px; flex-wrap: wrap;
  background: #f6efe3; border-radius: 10px; padding: 8px 12px; }
.ts-label { font-size: 12px; color: #7d6b5c; }
.progress-text { font-size: 13px; font-weight: 700; color: #8f3f2a; }
.read-bar { position: fixed; top: 0; left: 0; height: 3px; background: var(--grad-hero); z-index: 2000; transition: width 0.15s; }
.gt-side { position: sticky; top: 84px; }
.toc-card { max-height: calc(100vh - 120px); overflow: auto; }
.toc-item {
  display: block; font-size: 13px; color: #6f5641; text-decoration: none; padding: 6px 8px;
  border-radius: 6px; border-left: 3px solid transparent; transition: all 0.18s; line-height: 1.5;
}
.toc-item.lv3 { padding-left: 20px; font-size: 12.5px; color: #7d6b5c; }
.toc-item:hover { background: #f6efe3; color: var(--brand); }
.toc-item.active { border-left-color: var(--brand); background: #f6efe3; color: var(--brand-deep); font-weight: 700; }
.toc-progress { text-align: center; }
.toc-progress b { font-size: 24px; display: block; }
.end-bar { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.rich-body { line-height: 1.9; color: #303133; transition: font-size 0.2s; }
.rich-body :deep(img) { max-width: 100%; border-radius: 8px; }
.rich-body :deep(h2) { font-size: 1.25em; margin: 20px 0 8px; color: #8f3f2a; scroll-margin-top: 80px; }
.rich-body :deep(h3) { font-size: 1.1em; margin: 16px 0 6px; color: #6f3320; scroll-margin-top: 80px; }
.rich-body :deep(blockquote) {
  border-left: 4px solid #b5573b; margin: 10px 0; padding: 6px 14px;
  color: #606266; background: #f6efe3; border-radius: 0 8px 8px 0;
}
</style>
