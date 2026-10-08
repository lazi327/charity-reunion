<template>
  <div class="home">
    <!-- 跨版首屏：左纪实大图 + 右家书 -->
    <div class="spread fade-up">
      <div v-tappable class="hero-photo" @click="banners[0] && goBanner(banners[0])">
        <div class="scene"><div class="door"></div><div class="lamp"></div></div>
        <img v-if="banners[0]?.image" :src="banners[0].image" :alt="banners[0].title" class="real" />
        <div class="ph-caption" v-if="!banners[0]?.image">
          <b>{{ banners[0]?.title || '那盏为你留的灯' }}</b>
          <span>纯公益寻亲平台 · 免费发布 · 志愿者协助核实</span>
        </div>
      </div>

      <div class="hero-letter">
        <div class="vline">家 书 · 等 你 回 来</div>
        <div class="letter-main">
          <div class="eyebrow">把 寻 人 写 成 一 封 家 书</div>
          <h1 class="hero-h">每个走失的名字，<br>都值得被<span class="hl">认真找回来</span></h1>
          <p class="hero-p">每个走失的人背后，都有一桌没动筷的饭。<br>我们帮你把消息送到对的人眼前，也陪你把门留住。</p>
          <div class="hero-cta">
            <router-link to="/cases/new" class="cta">我要寻找亲人</router-link>
            <router-link to="/cases" class="cta ghost">检索寻亲档案 →</router-link>
          </div>

          <div class="stat-strip">
            <div class="st"><b>{{ anim.caseCount }}</b><span>寻亲档案</span></div>
            <div class="st"><b>{{ anim.wishTotal }}</b><span>回家灯</span></div>
            <div class="st"><b>{{ anim.viewTotal }}</b><span>次关注</span></div>
            <div class="st"><b>+{{ anim.newCases7d }}</b><span>近7天</span></div>
          </div>
          <button class="lamp-btn" :disabled="lampCooling" @click="lightRandomLamp">
            <Lamp /> {{ lampCooling ? `灯塔冷却 ${lampCooling}s` : '为走散的人点亮一盏回家灯' }}
          </button>
        </div>
      </div>
    </div>

    <!-- 功能入口：社论式编号行 -->
    <div class="entries fade-up" style="animation-delay:.08s">
      <div v-tappable class="entry" v-for="(e, i) in entries" :key="e.path" @click="$router.push(e.path)">
        <span class="no">0{{ i + 1 }}</span>
        <div class="en-body">
          <div class="en-name">{{ e.title }}</div>
          <div class="en-desc">{{ e.desc }}</div>
        </div>
        <span class="en-go">→</span>
      </div>
    </div>

    <div class="cols fade-up" style="animation-delay:.14s">
      <!-- 最新寻亲 -->
      <div class="page-card col-main">
        <div class="card-head">
          <b class="sec-title">最新寻亲信息</b>
          <el-link type="primary" :underline="false" @click="$router.push('/cases')">查看全部 →</el-link>
        </div>
        <div class="cases">
          <div v-tappable class="case-card lift" v-for="c in latest" :key="c.id" @click="$router.push('/cases/' + c.id)">
            <div class="case-photo">
              <el-image v-if="c.photo" :src="c.photo" fit="cover" style="width:100%;height:100%" />
              <div v-else class="case-avatar" :style="{ background: avatarGrad(c.name) }">{{ c.name?.[0] }}</div>
              <span class="lost-days" :class="{ urgent: daysSince(c.lostDate, c.createTime) < 30 }">
                走失 {{ daysSince(c.lostDate, c.createTime) }} 天
              </span>
            </div>
            <div class="case-info">
              <div class="case-name">{{ c.name }}
                <span class="gd">{{ c.gender === 2 ? '女' : '男' }}</span>
              </div>
              <div class="case-meta">{{ c.relation }} · {{ c.lostPlace }}</div>
              <div class="case-foot" @click.stop>
                <span class="wish-btn" :class="{ on: c.wished }" @click="toggleWish(c)"><Lamp /> {{ c.wishCount || 0 }}</span>
                <span class="view-count">关注 {{ c.viewCount || 0 }}</span>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="!latest.length" description="暂无已发布的寻亲信息" :image-size="70" />
      </div>

      <!-- 右栏：接力动态 + 热度榜 + 防骗 -->
      <div class="col-side">
        <div class="page-card feed-card">
          <b class="sec-title">寻亲接力实时动态</b>
          <el-carousel height="150px" direction="vertical" :interval="3500" indicator-position="none" :autoplay="feed.length > 1">
            <el-carousel-item v-for="(f, i) in feed" :key="i">
              <div class="feed-item">
                <span class="feed-icon"><el-icon><Promotion /></el-icon></span>
                <div><div class="feed-text">{{ f.text }}</div><div class="feed-time">{{ f.time }}</div></div>
              </div>
            </el-carousel-item>
          </el-carousel>
        </div>

        <div class="page-card" style="margin-top:14px">
          <b class="sec-title">失踪地区热度榜</b>
          <div class="heat-list">
            <div v-for="(h, i) in heat" :key="h.name" class="heat-row">
              <span class="heat-rank" :class="'r' + (i + 1)">{{ i + 1 }}</span>
              <span class="heat-name">{{ h.name }}</span>
              <div class="heat-bar"><div class="heat-fill" :style="{ width: (h.value / heatMax * 100) + '%' }"></div></div>
              <span class="heat-val">{{ h.value }}</span>
            </div>
            <div v-if="!heat.length" style="color:#a99;font-size:13px">暂无数据</div>
          </div>
        </div>

        <div class="anti-fraud">
          <b><el-icon><Warning /></el-icon> 防骗提醒</b>
          <p>凡是索要“寻亲费 / DNA比对费 / 保证金”的都是诈骗！本平台全程免费，线索核实由公安与志愿者完成。</p>
        </div>
      </div>
    </div>

    <!-- 飞行动画孔明灯 -->
    <teleport to="body">
      <div v-for="f in flyers" :key="f.id" class="flying-lamp"
           :style="{ left: f.x + 'px', top: f.y + 'px', '--dx': f.dx + 'px', '--dy': f.dy + 'px' }"><Lamp /></div>
    </teleport>
  </div>
</template>

<script setup>
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const router = useRouter()
const banners = ref([])
const latest = ref([])
const feed = ref([])
const heat = ref([])
const heatMax = ref(1)
const lampCooling = ref(0)
const flyers = ref([])
const anim = ref({ caseCount: 0, viewTotal: 0, wishTotal: 0, newCases7d: 0 })
let coolTimer = null

const entries = [
  { title: '发布寻亲信息', desc: '免费发布，审核后全网扩散', path: '/cases/new' },
  { title: '检索寻亲档案', desc: '地区 / 类型 / 年份多维筛选', path: '/cases' },
  { title: '亲缘雷达', desc: '五维算法智能比对档案', path: '/radar' },
  { title: '寻亲问答', desc: '经验互助，为家人答疑', path: '/questions' }
]

/* count-up 缓动 */
function countUp(key, to) {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) { anim.value[key] = to; return }
  const start = performance.now()
  const dur = 1100
  const from = anim.value[key] || 0
  function step(now) {
    const p = Math.min(1, (now - start) / dur)
    anim.value[key] = Math.round(from + (to - from) * (1 - Math.pow(1 - p, 3)))
    if (p < 1) requestAnimationFrame(step)
  }
  requestAnimationFrame(step)
}

/* 走失天数：优先走失日期，否则用发布日 */
function daysSince(lostDate, createTime) {
  const d = lostDate || (createTime ? createTime.slice(0, 10) : null)
  if (!d) return '?'
  const diff = Math.floor((Date.now() - new Date(d).getTime()) / 86400000)
  return Math.max(0, diff)
}

/* 姓名首字暖土渐变头像 */
function avatarGrad(name) {
  const grads = [
    'linear-gradient(135deg,#c9784f,#8f3f2a)', 'linear-gradient(135deg,#d98a4f,#b5573b)',
    'linear-gradient(135deg,#6f3320,#b5573b)', 'linear-gradient(135deg,#3f6152,#6f3320)',
    'linear-gradient(135deg,#c98a3d,#b5573b)'
  ]
  let h = 0
  for (const ch of (name || '亲')) h = (h * 31 + ch.codePointAt(0)) % 997
  return grads[h % grads.length]
}

/* 点灯（toggle） */
async function toggleWish(c, evt) {
  try {
    const r = await request.post(`/api/cases/${c.id}/wish`)
    c.wishCount = Number(r.wishCount)
    c.wished = r.mine
    if (r.mine) {
      launchLamp(evt || window.event)
      ElMessage.success({ message: '已为这条寻亲档案点亮回家灯，愿早日团圆', duration: 2000 })
    }
  } catch { /* 拦截器已提示 */ }
}

/* 孔明灯飞行动画 */
let flyerSeq = 0
function launchLamp(evt) {
  const x = evt?.clientX ?? window.innerWidth / 2
  const y = evt?.clientY ?? window.innerHeight / 2
  const id = ++flyerSeq
  flyers.value.push({ id, x, y, dx: (Math.random() - 0.5) * 160, dy: -window.innerHeight - 80 })
  setTimeout(() => { flyers.value = flyers.value.filter(f => f.id !== id) }, 2600)
}

/* 随机点灯按钮 */
async function lightRandomLamp() {
  const candidates = latest.value.filter(c => !c.wished)
  if (!candidates.length) {
    ElMessage.info('当前页档案都已被你点过灯，去寻亲档案页看看吧')
    router.push('/cases')
    return
  }
  const pick = candidates[Math.floor(Math.random() * candidates.length)]
  await toggleWish({ ...pick, __update: true })
  const target = latest.value.find(c => c.id === pick.id)
  if (target) { target.wished = true; target.wishCount = (target.wishCount || 0) + 1 }
  countUp('wishTotal', (anim.value.wishTotal || 0) + 1)
  lampCooling.value = 5
  clearInterval(coolTimer)
  coolTimer = setInterval(() => {
    lampCooling.value -= 1
    if (lampCooling.value <= 0) clearInterval(coolTimer)
  }, 1000)
}

function goBanner(b) {
  if (!b.linkUrl) return
  if (b.linkUrl.startsWith('http')) window.open(b.linkUrl)
  else router.push(b.linkUrl)
}

onMounted(async () => {
  try {
    const [bs, summary, cases, fd, ht] = await Promise.all([
      request.get('/api/public/banners'),
      request.get('/api/public/summary'),
      request.get('/api/public/latest-cases'),
      request.get('/api/public/feed'),
      request.get('/api/public/region-heat')
    ])
    banners.value = bs
    latest.value = cases
    feed.value = fd
    heat.value = ht
    heatMax.value = Math.max(1, ...ht.map(h => h.value))
    countUp('caseCount', summary.caseCount || 0)
    countUp('viewTotal', summary.viewTotal || 0)
    countUp('wishTotal', summary.wishTotal || 0)
    countUp('newCases7d', summary.newCases7d || 0)
  } catch { /* 忽略 */ }
})

onUnmounted(() => clearInterval(coolTimer))
</script>

<style scoped>
.home { padding-bottom: 16px; }
.display { font-family: var(--font-display); }

/* —— 跨版首屏 —— */
.spread {
  display: grid; grid-template-columns: 1.35fr 1fr; gap: 0;
  background: #fffdf8; border: 1px solid var(--paper-line);
  border-radius: 12px; overflow: hidden; box-shadow: var(--card-shadow);
}
.hero-photo { position: relative; min-height: 460px; cursor: pointer;
  background: linear-gradient(150deg, #c9784f 0%, #b5573b 42%, #8f3f2a 78%, #6f3320 100%); }
.hero-photo .scene { position: absolute; inset: 0; }
.hero-photo .door { position: absolute; right: 16%; bottom: 0; width: 190px; height: 320px;
  background: linear-gradient(180deg, #f7e4bb, #e9b877); border-radius: 95px 95px 0 0;
  box-shadow: 0 -20px 80px 30px rgba(247, 228, 187, .55); opacity: .92; }
.hero-photo .lamp { position: absolute; right: 26%; top: 14%; width: 24px; height: 32px; border-radius: 50%;
  background: #ffd98a; box-shadow: 0 0 40px 14px rgba(255, 217, 138, .85); }
.hero-photo .real { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.ph-caption { position: absolute; left: 26px; bottom: 22px; color: #fff; z-index: 2;
  text-shadow: 0 2px 12px rgba(0, 0, 0, .45); }
.ph-caption b { display: block; font-family: var(--font-display); font-size: 19px; letter-spacing: 2px; }
.ph-caption span { font-size: 12px; letter-spacing: 3px; opacity: .92; }

.hero-letter { padding: 30px 34px 30px 30px; display: flex; gap: 20px; }
.vline { writing-mode: vertical-rl; font-family: var(--font-display); font-size: 14px; letter-spacing: 7px;
  color: var(--brand); border-left: 1px solid var(--cream-2); padding-left: 12px; align-self: flex-start; }
.letter-main { flex: 1; }
.eyebrow { font-size: 12px; letter-spacing: 5px; color: var(--pine); margin-bottom: 16px; }
.hero-h { font-family: var(--font-display); font-weight: 600; font-size: 34px; line-height: 1.4; letter-spacing: 1px; color: var(--ink); margin: 0; }
.hero-h .hl { color: var(--brand-deep); }
.hero-p { margin-top: 16px; font-size: 14px; line-height: 1.95; color: var(--ink-soft); }
.hero-cta { margin-top: 22px; display: flex; gap: 14px; }
.cta { text-decoration: none; display: inline-flex; align-items: center; background: var(--brand); color: #fff;
  font-size: 14px; letter-spacing: 1px; padding: 12px 26px; border-radius: 4px; box-shadow: 0 8px 22px rgba(143, 63, 42, .28); transition: .2s; }
.cta:hover { background: var(--brand-deep); transform: translateY(-2px); }
.cta.ghost { background: transparent; color: var(--ink); border: 1px solid var(--paper-line); box-shadow: none; }
.cta.ghost:hover { border-color: var(--brand); color: var(--brand-deep); }

.stat-strip { margin-top: 24px; display: flex; gap: 26px; padding-top: 16px; border-top: 1px solid var(--cream-2); }
.st b { display: block; font-family: var(--font-display); font-size: 24px; color: var(--brand-deep); line-height: 1; }
.st span { font-size: 11px; color: var(--ink-soft); letter-spacing: 1px; }
.lamp-btn { margin-top: 16px; width: 100%; height: 40px; border: none; border-radius: 4px;
  background: var(--grad-lamp); color: #6f3320; font-weight: 700; font-size: 14px; cursor: pointer;
  box-shadow: 0 6px 18px rgba(217, 138, 79, .4); transition: transform .2s; }
.lamp-btn:hover:not(:disabled) { transform: scale(1.02); }
.lamp-btn:disabled { opacity: .6; cursor: wait; }

/* —— 功能入口：社论编号行 —— */
.entries { display: grid; grid-template-columns: repeat(4, 1fr); gap: 0; margin-top: 16px;
  background: #fffdf8; border: 1px solid var(--paper-line); border-radius: 12px; overflow: hidden; }
.entry { display: flex; align-items: center; gap: 14px; padding: 20px 22px; cursor: pointer;
  border-right: 1px solid var(--cream-2); transition: background .25s; }
.entry:last-child { border-right: none; }
.entry:hover { background: var(--cream); }
.entry .no { font-family: var(--font-display); font-size: 22px; color: var(--brand); font-weight: 700; }
.en-name { font-family: var(--font-display); font-weight: 600; font-size: 16px; color: var(--ink); letter-spacing: 1px; }
.en-desc { font-size: 12px; color: var(--ink-soft); margin-top: 3px; }
.en-go { margin-left: auto; color: var(--brand); font-size: 18px; opacity: 0; transition: .25s; }
.entry:hover .en-go { opacity: 1; }

/* —— 双栏 —— */
.cols { display: grid; grid-template-columns: 1fr 320px; gap: 16px; margin-top: 16px; }
.col-main { height: 100%; }
.sec-title { font-family: var(--font-display); color: var(--brand-deep); font-size: 16px; letter-spacing: 1px; }
.card-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.cases { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.case-card { border: 1px solid var(--paper-line); border-radius: 8px; overflow: hidden; cursor: pointer; background: #fff; }
.case-photo { position: relative; height: 120px; }
.case-avatar { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center;
  color: #fff; font-size: 40px; font-weight: 800; font-family: var(--font-display); }
.lost-days { position: absolute; top: 8px; right: 6px; background: rgba(58, 47, 40, .72); color: #ffe9b0;
  font-size: 11px; padding: 2px 8px; border-radius: 3px; }
.lost-days.urgent { background: var(--brand); color: #fff; animation: pulse 1.4s infinite; }
@keyframes pulse { 50% { transform: scale(1.08); } }
.case-info { padding: 10px 12px; }
.case-name { font-family: var(--font-display); font-weight: 700; font-size: 15px; display: flex; align-items: center; gap: 6px; color: var(--ink); }
.case-name .gd { font-family: var(--font-body); font-size: 11px; color: var(--brand-deep); border: 1px solid var(--brand); border-radius: 3px; padding: 0 5px; }
.case-meta { font-size: 12px; color: var(--ink-soft); margin: 5px 0 8px; }
.case-foot { display: flex; justify-content: space-between; font-size: 12.5px; color: var(--ink-soft); }
.wish-btn { cursor: pointer; padding: 2px 8px; border-radius: 3px; transition: all .2s; }
.wish-btn:hover { background: var(--cream); transform: scale(1.06); }
.wish-btn.on { background: var(--grad-lamp); color: #6f3320; font-weight: 700; }

/* 接力动态 */
.feed-card :deep(.el-carousel__container) { margin-top: 10px; }
.feed-item { display: flex; gap: 10px; align-items: flex-start; padding: 6px 2px; }
.feed-icon { font-size: 17px; flex: none; color: var(--brand); }
.feed-text { font-size: 12.5px; color: #55483d; line-height: 1.5; }
.feed-time { font-size: 11px; color: var(--ink-soft); margin-top: 2px; }

/* 热度榜 */
.heat-list { margin-top: 12px; }
.heat-row { display: flex; align-items: center; gap: 8px; margin-bottom: 9px; }
.heat-rank { width: 20px; height: 20px; border-radius: 4px; flex: none; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700; background: var(--cream-2); color: var(--ink-soft); font-family: var(--font-display); }
.heat-rank.r1 { background: var(--brand); color: #fff; }
.heat-rank.r2 { background: var(--clay-soft); color: #fff; }
.heat-rank.r3 { background: var(--brand-gold); color: #fff; }
.heat-name { width: 52px; flex: none; font-size: 13px; color: var(--ink); }
.heat-bar { flex: 1; height: 8px; background: var(--cream-2); border-radius: 4px; overflow: hidden; }
.heat-fill { height: 100%; border-radius: 4px; background: var(--grad-sunrise); transition: width 1.2s cubic-bezier(0.2, 0.8, 0.2, 1); }
.heat-val { width: 24px; text-align: right; font-size: 12px; color: var(--ink-soft); }

/* 防骗 */
.anti-fraud { margin-top: 14px; border-radius: 10px; padding: 14px 16px; background: #fff6ea; border: 1px dashed #d9a55f; }
.anti-fraud b { color: #a8631f; font-size: 13.5px; font-family: var(--font-display); }
.anti-fraud p { font-size: 12px; color: #7a5a3c; line-height: 1.7; margin: 6px 0 0; }

/* 飞行孔明灯 */
:global(.flying-lamp) { position: fixed; z-index: 3000; font-size: 26px; color: #d98a4f; pointer-events: none; animation: fly-up 2.5s ease-in forwards; }
@keyframes fly-up {
  0% { transform: translate(0, 0) scale(0.6); opacity: 0; }
  12% { transform: translate(calc(var(--dx) * 0.05), -20px) scale(1.15); opacity: 1; }
  100% { transform: translate(var(--dx), var(--dy)) scale(0.7) rotate(12deg); opacity: 0; }
}

@media (max-width: 1100px) {
  .spread { grid-template-columns: 1fr; }
  .hero-photo { min-height: 260px; }
  .cols { grid-template-columns: 1fr; }
  .cases { grid-template-columns: repeat(2, 1fr); }
  .entries { grid-template-columns: repeat(2, 1fr); }
  .entry { border-bottom: 1px solid var(--cream-2); }
}
</style>
