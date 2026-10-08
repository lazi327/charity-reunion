<template>
  <div>
    <!-- 检索面板 -->
    <div class="page-card filter-panel fade-up">
      <div class="quick-row">
        <div class="rel-chips">
          <span class="chip-label">寻亲类型</span>
          <button v-for="r in ['全部', ...relations]" :key="r" class="chip" :class="{ on: rel(r) }"
                  @click="pickRelation(r)">{{ r }}</button>
        </div>
        <el-segmented v-model="query.sort" :options="sortOptions" @change="reload" />
        <el-button-group>
          <el-button :type="view === 'grid' ? 'primary' : ''" :icon="Grid" @click="view = 'grid'" />
          <el-button :type="view === 'list' ? 'primary' : ''" :icon="List" @click="view = 'list'" />
        </el-button-group>
      </div>

      <div class="search-row">
        <el-input v-model="query.keyword" placeholder="姓名 / 体貌特征 / 地区关键字,回车检索" clearable
                  style="max-width: 320px" @keyup.enter="reload" @clear="reload" />
        <el-select v-model="query.place" placeholder="失踪地区" clearable filterable style="width: 150px" @change="reload">
          <el-option v-for="p in places" :key="p" :label="p" :value="p" />
        </el-select>
        <el-select v-model="query.gender" placeholder="性别" clearable style="width: 96px" @change="reload">
          <el-option label="男" :value="1" />
          <el-option label="女" :value="2" />
        </el-select>
        <el-button type="primary" @click="reload">检 索</el-button>
        <el-button @click="showAdvance = !showAdvance">
          {{ showAdvance ? '收起' : '高级筛选' }} <el-icon><component :is="showAdvance ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
        </el-button>
        <el-button type="primary" plain @click="$router.push('/cases/new')">＋ 发布寻亲信息</el-button>
        <el-badge :value="favIds.size" :hidden="!favIds.size" class="fav-badge">
          <el-button :type="favOnly ? 'warning' : ''" @click="toggleFavOnly"><el-icon><Star /></el-icon> 只看收藏</el-button>
        </el-badge>
      </div>

      <el-collapse-transition>
        <div v-show="showAdvance" class="advance-row">
          <div class="adv-item">
            <span>出生年份区间:{{ birthRange[0] }} - {{ birthRange[1] }}</span>
            <el-slider v-model="birthRange" range :min="1940" :max="2015" :step="1" size="small"
                       style="width: 240px" @change="reload" />
          </div>
          <div class="adv-item">
            <span>失踪年份区间:{{ lostRange[0] }} - {{ lostRange[1] }}</span>
            <el-slider v-model="lostRange" range :min="1940" :max="2026" :step="1" size="small"
                       style="width: 240px" @change="reload" />
          </div>
          <el-button link type="primary" @click="resetAll"> 清空全部条件</el-button>
        </div>
      </el-collapse-transition>

      <div class="result-stat">
        共 <b>{{ total }}</b> 条档案符合条件
        <template v-if="activeFilterCount"> · 已启用 {{ activeFilterCount }} 个筛选条件</template>
        · 用时 {{ costMs }}ms
      </div>
    </div>

    <el-empty v-if="!loading && cases.length === 0" description="暂无符合条件的寻亲信息,试试放宽筛选条件">
      <el-button type="primary" plain @click="resetAll">清空条件重新检索</el-button>
    </el-empty>

    <!-- 卡片视图 -->
    <el-row v-if="view === 'grid'" :gutter="16" style="margin-top: 4px" v-loading="loading">
      <el-col :span="6" v-for="(c, idx) in shown" :key="c.id" style="margin-bottom: 16px">
        <div v-tappable class="case-card lift fade-up" :style="{ animationDelay: (idx % 8) * 0.04 + 's' }"
             @click="$router.push('/cases/' + c.id)">
          <div class="photo-wrap">
            <el-image v-if="c.photo" :src="c.photo" fit="cover" class="photo" />
            <div v-else class="photo-empty" :style="{ background: avatarGrad(c.name) }">{{ c.name?.[0] }}</div>
            <el-tag v-if="c.topFlag" size="small" type="warning" effect="dark" class="top-tag">置顶</el-tag>
            <button class="fav-star" :class="{ on: favIds.has(c.id) }" @click.stop="toggleFav(c.id)" title="收藏">
              <el-icon><StarFilled v-if="favIds.has(c.id)" /><Star v-else /></el-icon>
            </button>
            <span class="day-badge" :class="{ urgent: daysOf(c) <= 30 }">走失 {{ daysOf(c) }} 天</span>
          </div>
          <div class="body">
            <div class="name" v-html="hl(c.name)"></div>
            <div class="meta">{{ c.relation }} · {{ ageOf(c) || '?' }}岁 · <span v-html="hl(c.lostPlace)"></span></div>
            <div class="meta ellipsis" v-html="hl(c.features || '')"></div>
            <div class="foot" @click.stop>
              <span v-tappable class="wish-btn" :class="{ on: c.wished }" @click="toggleWish(c)"><Lamp /> {{ c.wishCount || 0 }}</span>
              <span><el-icon><View /></el-icon> {{ c.viewCount || 0 }}</span>
              <span class="publisher">{{ c.publisherName }}</span>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>

    <!-- 列表视图 -->
    <div v-else class="page-card" v-loading="loading" style="margin-top: 4px">
      <div v-tappable v-for="c in shown" :key="c.id" class="list-row" @click="$router.push('/cases/' + c.id)">
        <el-image v-if="c.photo" :src="c.photo" fit="cover" class="list-photo" />
        <div v-else class="list-photo list-avatar" :style="{ background: avatarGrad(c.name) }">{{ c.name?.[0] }}</div>
        <div class="list-main">
          <div class="list-title">
            <b v-html="hl(c.name)"></b>
            <el-tag size="small" effect="plain" round>{{ c.relation }}</el-tag>
            <el-tag size="small" :type="c.gender === 2 ? 'danger' : 'primary'" effect="plain">{{ c.gender === 2 ? '女' : '男' }}</el-tag>
            <el-tag v-if="c.topFlag" size="small" type="warning" effect="dark">置顶</el-tag>
            <span class="day-badge inline" :class="{ urgent: daysOf(c) <= 30 }">走失 {{ daysOf(c) }} 天</span>
          </div>
          <div class="meta"><span v-html="hl(c.lostPlace)"></span> · 失踪于 {{ c.lostDate || '未填' }} · {{ ageOf(c) || '?' }}岁 · 发布人 {{ c.publisherName }}</div>
          <div class="meta ellipsis1" v-html="hl(c.features || '')"></div>
        </div>
        <div class="list-side" @click.stop>
          <span class="wish-btn" :class="{ on: c.wished }" @click="toggleWish(c)"><Lamp /> {{ c.wishCount || 0 }}</span>
          <span class="meta"><el-icon><View /></el-icon> {{ c.viewCount || 0 }}</span>
          <button class="fav-star inline" :class="{ on: favIds.has(c.id) }" @click="toggleFav(c.id)">
            <el-icon><StarFilled v-if="favIds.has(c.id)" /><Star v-else /></el-icon>
          </button>
        </div>
      </div>
    </div>

    <el-pagination v-if="total > query.size" layout="sizes, prev, pager, next, jumper" :total="total"
                   v-model:page-size="query.size" :page-sizes="[8, 12, 24, 48]"
                   v-model:current-page="query.page" @current-change="load" @size-change="reload"
                   style="justify-content: center; margin: 12px 0" />
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import request from '../utils/request'
import { Grid, List } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

const cases = ref([])
const total = ref(0)
const loading = ref(false)
const costMs = ref(0)
const places = ref([])
const view = ref('grid')
const showAdvance = ref(false)
const relations = ['寻子女', '寻父母', '寻配偶', '寻兄弟姐妹', '寻其他亲属']
const sortOptions = [
  { label: '最新', value: 'new' },
  { label: '热度', value: 'hot' },
  { label: '关注', value: 'view' },
  { label: '点灯', value: 'wish' }
]
const query = ref({ page: 1, size: 12, relation: '', place: '', gender: null, keyword: '', sort: 'new' })
const birthRange = ref([1940, 2015])
const lostRange = ref([1940, 2026])

/* ---------- 本地收藏 ---------- */
const favIds = ref(new Set(JSON.parse(localStorage.getItem('cf_fav_cases') || '[]')))
const favOnly = ref(false)
function toggleFav(id) {
  const s = new Set(favIds.value)
  s.has(id) ? s.delete(id) : s.add(id)
  favIds.value = s
  localStorage.setItem('cf_fav_cases', JSON.stringify([...s]))
}
function toggleFavOnly() {
  favOnly.value = !favOnly.value
  if (favOnly.value) {
    if (!favIds.value.size) ElMessage.info('还没有收藏,点击卡片右上角 ☆ 收藏寻亲档案')
    query.value.page = 1
  }
  load()
}

const shown = computed(() => favOnly.value ? cases.value.filter(c => favIds.value.has(c.id)) : cases.value)
const activeFilterCount = computed(() => {
  const q = query.value
  let n = 0
  if (q.relation) n++
  if (q.place) n++
  if (q.gender) n++
  if (q.keyword) n++
  if (birthRange.value[0] > 1940 || birthRange.value[1] < 2015) n++
  if (lostRange.value[0] > 1940 || lostRange.value[1] < 2026) n++
  return n
})

function rel(r) {
  return (query.value.relation || '全部') === r
}
function pickRelation(r) {
  query.value.relation = r === '全部' ? '' : r
  reload()
}

/* ---------- 数据加载 ---------- */
async function load() {
  loading.value = true
  const t0 = performance.now()
  try {
    const q = { ...query.value, sort: query.value.sort || 'new' }
    if (birthRange.value[0] > 1940) q.minBirthYear = birthRange.value[0]
    if (birthRange.value[1] < 2015) q.maxBirthYear = birthRange.value[1]
    if (lostRange.value[0] > 1940) q.minLostYear = lostRange.value[0]
    if (lostRange.value[1] < 2026) q.maxLostYear = lostRange.value[1]
    delete q.sort
    if (query.value.sort !== 'new') q.sort = query.value.sort
    Object.keys(q).forEach(k => { if (q[k] === null || q[k] === '') delete q[k] })
    const data = await request.get('/api/cases', { params: q })
    cases.value = data.records
    total.value = data.total
  } finally {
    costMs.value = Math.round(performance.now() - t0)
    loading.value = false
  }
}
function reload() {
  query.value.page = 1
  load()
}
function resetAll() {
  query.value = { page: 1, size: 12, relation: '', place: '', gender: null, keyword: '', sort: query.value.sort }
  birthRange.value = [1940, 2015]
  lostRange.value = [1940, 2026]
  favOnly.value = false
  reload()
}

/* ---------- 工具 ---------- */
function ageOf(c) {
  return c.birthYear ? new Date().getFullYear() - c.birthYear : null
}
function daysOf(c) {
  const d = c.lostDate || (c.createTime ? c.createTime.slice(0, 10) : null)
  if (!d) return 0
  return Math.max(0, Math.floor((Date.now() - new Date(d).getTime()) / 86400000))
}
function avatarGrad(name) {
  const grads = ['linear-gradient(135deg,#b5573b,#8f3f2a)', 'linear-gradient(135deg,#d98a4f,#b5573b)',
    'linear-gradient(135deg,#6f3320,#8f3f2a)', 'linear-gradient(135deg,#3f6152,#6f3320)']
  let h = 0
  for (const ch of (name || '亲')) h = (h * 31 + ch.codePointAt(0)) % 997
  return grads[h % grads.length]
}
/* 关键字高亮(先转义防 XSS) */
function hl(text) {
  const safe = (text || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
  const kw = (query.value.keyword || '').trim()
  if (!kw) return safe
  return safe.replace(new RegExp(kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi'),
    m => `<mark class="kw-hl">${m}</mark>`)
}

async function toggleWish(c) {
  try {
    const r = await request.post(`/api/cases/${c.id}/wish`)
    c.wishCount = Number(r.wishCount)
    c.wished = r.mine
  } catch { /* 忽略 */ }
}

onMounted(async () => {
  load()
  try { places.value = await request.get('/api/cases/places') } catch { /* 忽略 */ }
})
</script>

<style scoped>
.filter-panel { margin-bottom: 16px; }
.quick-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 10px; }
.rel-chips { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
.chip-label { font-size: 13px; color: #7d6b5c; margin-right: 4px; }
.chip {
  border: 1px solid #e4d8c4; background: #fff; color: #6f5641;
  border-radius: 16px; padding: 4px 14px; font-size: 13px; cursor: pointer;
  transition: all 0.2s;
}
.chip:hover { border-color: var(--brand); color: var(--brand); }
.chip.on { background: var(--grad-hero); color: #fff; border-color: transparent; }
.search-row { display: flex; align-items: center; gap: 10px; margin-top: 14px; flex-wrap: wrap; }
.advance-row {
  display: flex; gap: 28px; align-items: center; margin-top: 14px;
  padding-top: 14px; border-top: 1px dashed #e4d8c4; flex-wrap: wrap;
}
.adv-item { display: flex; align-items: center; gap: 12px; font-size: 13px; color: #6f5641; }
.result-stat { margin-top: 12px; font-size: 12.5px; color: #7d6b5c; }
.result-stat b { color: var(--brand-deep); }

.case-card { background: #fff; border-radius: 12px; overflow: hidden; cursor: pointer; border: 1px solid #e4d8c4; }
.photo-wrap { position: relative; height: 160px; }
.photo { width: 100%; height: 100%; }
.photo-empty {
  width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 52px; font-weight: 800;
}
.top-tag { position: absolute; top: 8px; left: 8px; }
.fav-star {
  position: absolute; top: 6px; right: 6px; border: none; background: rgba(255, 255, 255, 0.8);
  width: 28px; height: 28px; border-radius: 50%; cursor: pointer; font-size: 15px;
  transition: transform 0.2s;
}
.fav-star:hover { transform: scale(1.2) rotate(15deg); }
.fav-star.on { background: #fff3d6; }
.fav-star.inline { position: static; }
.day-badge {
  position: absolute; bottom: 8px; right: 8px;
  background: rgba(47, 43, 51, 0.62); color: #ffe9b0;
  font-size: 11px; padding: 2px 8px; border-radius: 10px;
}
.day-badge.inline { position: static; margin-left: 6px; }
.day-badge.urgent { background: rgba(181, 87, 59, 0.88); color: #fff; }
.body { padding: 10px 12px; }
.name { font-size: 16px; font-weight: 700; }
.meta { font-size: 12px; color: #7d6b5c; margin-top: 4px; }
.ellipsis { display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; min-height: 34px; }
.ellipsis1 { display: -webkit-box; -webkit-line-clamp: 1; -webkit-box-orient: vertical; overflow: hidden; }
.foot { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #7d6b5c; margin-top: 6px; }
.publisher { max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.wish-btn { cursor: pointer; padding: 2px 8px; border-radius: 10px; transition: all 0.2s; }
.wish-btn:hover { background: #fff3e0; transform: scale(1.08); }
.wish-btn.on { background: var(--grad-lamp); color: #7a4200; font-weight: 700; }

.list-row {
  display: flex; gap: 14px; padding: 14px 6px; border-bottom: 1px dashed #e4d8c4; cursor: pointer;
  transition: background 0.2s;
}
.list-row:hover { background: #f6efe3; }
.list-photo { width: 92px; height: 72px; border-radius: 10px; flex: none; object-fit: cover; }
.list-avatar { display: flex; align-items: center; justify-content: center; color: #fff; font-size: 28px; font-weight: 800; }
.list-main { flex: 1; min-width: 0; }
.list-title { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; font-size: 15px; }
.list-side { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; flex: none; }
.fav-badge { margin-left: 4px; }
</style>

<style>
/* 关键字高亮(非 scoped,作用于 v-html) */
.kw-hl { background: linear-gradient(transparent 60%, #ffd76e 60%); color: inherit; padding: 0 1px; border-radius: 2px; }
</style>
