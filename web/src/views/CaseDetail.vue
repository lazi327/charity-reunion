<template>
  <div v-loading="loading">
    <div class="detail-grid" v-if="c">
      <!-- 主档案卡 -->
      <div class="page-card main-card fade-up">
        <div class="head">
          <div class="photo-box">
            <el-image v-if="c.photo" :src="c.photo" fit="cover" class="photo" :preview-src-list="[c.photo]" preview-teleported />
            <div v-else class="photo-empty" :style="{ background: avatarGrad(c.name) }">{{ c.name?.[0] }}</div>
            <div v-tappable class="lamp-count" :class="{ on: c.wished }" @click="toggleWish">
              <Lamp /> <b>{{ c.wishCount || 0 }}</b> 盏回家灯
            </div>
          </div>
          <div class="info">
            <div class="name">
              {{ c.name }}
              <el-tag size="small" :type="c.gender === 1 ? 'primary' : 'danger'" effect="plain">{{ c.gender === 1 ? '男' : '女' }}</el-tag>
              <el-tag size="small" type="warning" effect="plain">{{ c.relation }}</el-tag>
              <el-tag v-if="c.topFlag" size="small" type="danger" effect="dark">置顶</el-tag>
            </div>
            <div class="countdown" :class="{ urgent: daysLost <= 30 && daysLost > 0 }">
              <b>{{ daysLost }}</b><span>天</span>
              <em>已走失 · {{ c.lostDate || '日期未填' }} 失踪于 {{ c.lostPlace }}</em>
            </div>
            <div class="meta">出生年份:{{ c.birthYear || '未填' }}({{ age }} 岁) · 发布人:{{ c.publisherName }} · <el-icon><View /></el-icon> {{ c.viewCount || 0 }} 次浏览</div>
            <div class="meta" v-if="c.contactPhone">联系电话:<b class="phone">{{ c.contactPhone }}</b></div>
            <div class="tag-row" v-if="caseTags.length">
              <el-tag v-for="t in caseTags" :key="t" size="small" effect="light" round class="feat-tag">{{ t }}</el-tag>
            </div>
          </div>
        </div>

        <el-divider />
        <h4 class="sec-title">体貌特征</h4>
        <p class="para">{{ c.features }}</p>
        <template v-if="c.story">
          <h4 class="sec-title">失踪经过</h4>
          <p class="para">{{ c.story }}</p>
        </template>

        <el-divider />
        <div class="action-bar">
          <button class="act lamp" :class="{ on: c.wished }" @click="toggleWish">
            <Lamp /> {{ c.wished ? '已点灯·再点取消' : '点亮回家灯' }}
          </button>
          <button class="act clue" @click="clueDialog = true"><el-icon><Search /></el-icon> 我有线索</button>
          <button class="act poster" @click="makePoster"><el-icon><Picture /></el-icon> 生成转发海报</button>
          <button class="act copy" @click="copyInfo"><el-icon><DocumentCopy /></el-icon> 复制档案文本</button>
          <el-button v-if="!isMine && c.userId" type="primary" plain @click="$router.push('/messages/' + c.userId)"><el-icon><Message /></el-icon> 联系寻亲者</el-button>
          <template v-if="isMine">
            <el-button @click="$router.push('/cases/' + c.id + '/edit')">编辑</el-button>
            <el-button type="danger" plain @click="del">删除</el-button>
          </template>
          <el-button @click="$router.back()">返回</el-button>
        </div>
        <div v-if="c.status === 0" style="margin-top: 12px">
          <el-alert type="warning" :closable="false" title="该档案正在审核中,仅您本人可见,审核通过后公开展示。" />
        </div>
        <div v-else-if="c.status === 2" style="margin-top: 12px">
          <el-alert type="error" :closable="false" title="该档案审核未通过,请修改信息后重新提交审核。" />
        </div>
      </div>

      <!-- 右侧:亲缘雷达相似推荐 -->
      <aside class="side-col">
        <div class="page-card fade-up" style="animation-delay:.1s" v-if="similar.length && !isMine">
          <b class="grad-text"><el-icon><Aim /></el-icon> 亲缘雷达 · 相似档案</b>
          <div class="sim-tip">自研五维匹配引擎自动比对</div>
          <div v-tappable v-for="s in similar" :key="s.case.id" class="sim-card lift" @click="goCase(s.case.id)">
            <div class="sim-ring" :style="ringStyle(s.score)">
              <span>{{ s.score }}</span>
            </div>
            <div class="sim-body">
              <div class="sim-name">{{ s.case.name }}<el-tag size="small" effect="plain" style="margin-left:6px">{{ s.case.relation }}</el-tag></div>
              <div class="meta">{{ s.case.lostPlace }}</div>
              <div class="meta reasons">{{ s.reasons.slice(0, 2).join(' · ') }}</div>
            </div>
          </div>
          <div class="radar-box" v-if="similar[0]">
            <div class="radar-title">Top1 得分分解</div>
            <div ref="radarEl" class="radar-chart"></div>
          </div>
          <el-button text type="primary" @click="$router.push('/radar')">用亲缘雷达全库检索 »</el-button>
        </div>

        <div class="page-card fade-up" style="margin-top: 16px; animation-delay:.16s" v-if="isMine">
          <b><el-icon><Search /></el-icon> 线索信息({{ clues.length }})</b>
          <el-empty v-if="clues.length === 0" description="暂时还没有人提供线索,可生成海报转发扩散" :image-size="60" />
          <div v-for="cl in clues" :key="cl.id" class="clue-row">
            <div style="flex: 1">
              <div style="font-size: 14px; color: #303133">{{ cl.content }}</div>
              <div class="meta" style="margin-top: 4px">{{ cl.userName }} · {{ (cl.createTime || '').replace('T', ' ').slice(0, 16) }}</div>
              <div class="meta" v-if="cl.contact">联络方式:{{ cl.contact }}</div>
            </div>
            <el-tag v-if="cl.status === 1" type="success" size="small">已标有效</el-tag>
            <el-tag v-else-if="cl.status === 2" type="info" size="small">已标虚假</el-tag>
            <el-space v-else>
              <el-button size="small" type="success" plain @click="mark(cl, 1)">有效</el-button>
              <el-button size="small" type="info" plain @click="mark(cl, 2)">虚假</el-button>
            </el-space>
          </div>
        </div>

        <div class="page-card help-card fade-up" style="margin-top: 16px; animation-delay:.2s">
          <b><el-icon><InfoFilled /></el-icon> 团圆小贴士</b>
          <p>· 走失 24 小时内是黄金期,但<b>报警不需要等 24 小时</b></p>
          <p>· 父母可到全国公安 DNA 实验室<b>免费采血入库</b></p>
          <p>· 把海报转发到走失地同城群,扩散越快希望越大</p>
          <el-link type="primary" @click="$router.push('/guides')">阅读寻亲指南 »</el-link>
        </div>
      </aside>
    </div>

    <!-- 提供线索弹窗 -->
    <el-dialog v-model="clueDialog" title="提供线索" width="520px">
      <div class="clue-templates">
        <span class="tpl-label">快捷模板:</span>
        <el-tag v-for="t in clueTemplates" :key="t.label" class="tpl" effect="plain" round @click="clueForm.content = t.text">
          {{ t.label }}
        </el-tag>
      </div>
      <el-form label-position="top" style="margin-top: 10px">
        <el-form-item label="线索描述(至少 5 个字)">
          <el-input v-model="clueForm.content" type="textarea" :rows="4" maxlength="1000" show-word-limit
                    placeholder="请在什么时间、什么地点,见到了什么与您描述相关的情况…" />
        </el-form-item>
        <el-form-item label="您的联络方式(仅档案发布者可见,可不填)">
          <el-input v-model="clueForm.contact" placeholder="手机 / 微信 / 邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clueDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" :disabled="(clueForm.content || '').trim().length < 5" @click="submitClue">
          提交线索
        </el-button>
      </template>
    </el-dialog>

    <!-- 海报预览 -->
    <el-dialog v-model="posterVisible" title="转发海报(右键另存或下载)" width="420px" align-center>
      <img v-if="posterUrl" :src="posterUrl" class="poster-img" alt="分享海报" />
      <template #footer>
        <el-button @click="posterVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadPoster">下载海报</el-button>
      </template>
    </el-dialog>

    <!-- 点灯飞行 -->
    <teleport to="body">
      <div v-for="f in flyers" :key="f.id" class="flying-lamp"
          :style="{ left: f.x + 'px', top: f.y + 'px', '--dx': f.dx + 'px', '--dy': f.dy + 'px' }"><Lamp /></div>
    </teleport>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import request from '../utils/request'
import { getUser } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const c = ref(null)
const clues = ref([])
const similar = ref([])
const loading = ref(true)
const clueDialog = ref(false)
const submitting = ref(false)
const clueForm = ref({ content: '', contact: '' })
const posterVisible = ref(false)
const posterUrl = ref('')
const flyers = ref([])
const radarEl = ref(null)
let radarChart = null

const isMine = computed(() => c.value && String(c.value.userId) === String(getUser()?.id))
const age = computed(() => c.value?.birthYear ? new Date().getFullYear() - c.value.birthYear : '未填')
const daysLost = computed(() => {
  if (!c.value) return 0
  const d = c.value.lostDate || (c.value.createTime ? c.value.createTime.slice(0, 10) : null)
  if (!d) return 0
  return Math.max(0, Math.floor((Date.now() - new Date(d).getTime()) / 86400000))
})

/* 与后端 FeatureExtractor 词典对齐的前端标签推断 */
const TAG_DICT = {
  '高个子': ['高个', '一米七', '1.7', '一米八', '1.8', '身高'], '矮个子': ['矮', '一米六', '1.6', '一米五'],
  '圆脸': ['圆脸'], '瓜子脸': ['瓜子脸', '瘦脸'], '疤痕': ['疤', '烫伤', '烧伤'], '痣': ['痣', '胎记'],
  '龅牙缺牙': ['龅牙', '缺牙', '掉牙', '假牙'], '戴眼镜': ['眼镜', '近视'], '口吃': ['口吃', '结巴'],
  '听力障碍': ['聋', '听力', '耳背'], '智力障碍': ['智力', '痴呆', '精神'], '方言口音': ['口音', '方言', '乡音'],
  '跛脚步态': ['跛', '瘸', '步态', '腿脚'], '文身': ['文身', '纹身'], '断指': ['断指', '缺指']
}
const caseTags = computed(() => {
  if (!c.value) return []
  const text = (c.value.features || '') + ' ' + (c.value.story || '')
  return Object.entries(TAG_DICT).filter(([, kws]) => kws.some(k => text.includes(k))).map(([t]) => t)
})

function avatarGrad(name) {
  const grads = ['linear-gradient(135deg,#b5573b,#8f3f2a)', 'linear-gradient(135deg,#d98a4f,#b5573b)',
    'linear-gradient(135deg,#6f3320,#8f3f2a)', 'linear-gradient(135deg,#3f6152,#6f3320)']
  let h = 0
  for (const ch of (name || '亲')) h = (h * 31 + ch.codePointAt(0)) % 997
  return grads[h % grads.length]
}

function ringStyle(score) {
  const color = score >= 70 ? '#b5573b' : score >= 50 ? '#d98a4f' : '#6f3320'
  return { background: `conic-gradient(${color} ${score * 3.6}deg, #efe4d2 0deg)` }
}

function goCase(id) {
  router.push('/cases/' + id)
  nextTick(() => window.scrollTo({ top: 0, behavior: 'smooth' }))
  setTimeout(load, 60)
}

/* ---------- 点灯 ---------- */
async function toggleWish(evt) {
  try {
    const r = await request.post(`/api/cases/${c.value.id}/wish`)
    c.value.wishCount = Number(r.wishCount)
    c.value.wished = r.mine
    if (r.mine) {
      launchLamp(evt)
      ElMessage.success('已点亮回家灯,愿早日团圆')
    }
  } catch { /* 拦截器已提示 */ }
}
let flyerSeq = 0
function launchLamp(evt) {
  const x = evt?.clientX ?? window.innerWidth / 2
  const y = evt?.clientY ?? window.innerHeight / 2
  const id = ++flyerSeq
  flyers.value.push({ id, x, y, dx: (Math.random() - 0.5) * 180, dy: -window.innerHeight - 80 })
  setTimeout(() => { flyers.value = flyers.value.filter(f => f.id !== id) }, 2600)
}

/* ---------- 分享海报(Canvas 绘制) ---------- */
async function makePoster() {
  const img = new Image()
  if (c.value.photo) {
    img.src = c.value.photo
    await new Promise(res => { img.onload = res; img.onerror = res; setTimeout(res, 1500) })
  }
  const W = 360, H = 560
  const cv = document.createElement('canvas')
  cv.width = W; cv.height = H
  const g = cv.getContext('2d')
  // 背景渐变
  const bg = g.createLinearGradient(0, 0, W, H)
  bg.addColorStop(0, '#b5573b'); bg.addColorStop(0.5, '#8f3f2a'); bg.addColorStop(1, '#6f3320')
  g.fillStyle = bg; g.fillRect(0, 0, W, H)
  // 装饰圆
  g.fillStyle = 'rgba(255,255,255,0.08)'
  g.beginPath(); g.arc(W - 40, 60, 90, 0, 7); g.fill()
  g.beginPath(); g.arc(30, H - 80, 70, 0, 7); g.fill()
  // 照片区
  g.fillStyle = 'rgba(255,255,255,0.92)'
  roundRect(g, 24, 24, W - 48, 250, 16); g.fill()
  if (img.width && c.value.photo) {
    g.save(); roundRect(g, 30, 30, W - 60, 238, 12); g.clip()
    g.drawImage(img, 30, 30, W - 60, 238, 30, 30, W - 60, 238)
    g.restore()
  } else {
    g.fillStyle = '#8f3f2a'; g.font = 'bold 90px sans-serif'; g.textAlign = 'center'
    g.fillText(c.value.name?.[0] || '亲', W / 2, 170)
  }
  // 文本
  g.textAlign = 'center'; g.fillStyle = '#fff'
  g.font = 'bold 30px sans-serif'
  g.fillText(`寻 亲 · ${c.value.name}`, W / 2, 312)
  g.font = '15px sans-serif'; g.fillStyle = '#ffe9b0'
  g.fillText(`${c.value.relation} | ${c.value.gender === 2 ? '女' : '男'} | ${c.value.birthYear || '?'}年出生`, W / 2, 342)
  g.fillStyle = '#fff'; g.font = '15px sans-serif'
  g.fillText(`失踪地:${c.value.lostPlace}`, W / 2, 370)
  g.fillText(`已走失 ${daysLost.value} 天 · ${c.value.lostDate || '日期不详'}`, W / 2, 396)
  // 特征两行
  g.font = '13px sans-serif'; g.fillStyle = 'rgba(255,255,255,0.85)'
  const feat = (c.value.features || '').slice(0, 40)
  g.fillText(feat + (c.value.features?.length > 40 ? '…' : ''), W / 2, 424)
  // 底部
  g.fillStyle = 'rgba(0,0,0,0.22)'; g.fillRect(0, H - 92, W, 92)
  g.fillStyle = '#ffd76e'; g.font = 'bold 17px sans-serif'
  g.fillText('公益寻亲平台 · 纯公益 全免费', W / 2, H - 60)
  g.fillStyle = '#fff'; g.font = '13px sans-serif'
  g.fillText('亲人走失请立即报警,不必等待24小时', W / 2, H - 34)
  g.fillText(`本档案已有 ${c.value.wishCount || 0} 盏回家灯`, W / 2, H - 14)
  posterUrl.value = cv.toDataURL('image/png')
  posterVisible.value = true
}
function roundRect(g, x, y, w, h, r) {
  g.beginPath()
  g.moveTo(x + r, y)
  g.arcTo(x + w, y, x + w, y + h, r); g.arcTo(x + w, y + h, x, y + h, r)
  g.arcTo(x, y + h, x, y, r); g.arcTo(x, y, x + w, y, r)
  g.closePath()
}
function downloadPoster() {
  const a = document.createElement('a')
  a.href = posterUrl.value
  a.download = `寻亲海报_${c.value.name}.png`
  a.click()
  ElMessage.success('海报已下载,请转发到走失地同城群')
}

/* ---------- 复制档案文本 ---------- */
function copyInfo() {
  const text = `【公益寻亲】${c.value.name}(${c.value.relation}),${c.value.gender === 2 ? '女' : '男'},`
    + `${c.value.birthYear || '?'}年出生,${c.value.lostDate || '日期不详'}走失于${c.value.lostPlace}。`
    + `特征:${c.value.features}。已走失${daysLost.value}天,求转发!`
  navigator.clipboard?.writeText(text)
    .then(() => ElMessage.success('档案文本已复制,快去粘贴到微信群/QQ群'))
    .catch(() => ElMessageBox.alert(text, '手动复制以下内容转发'))
}

/* ---------- 数据加载 ---------- */
const clueTemplates = [
  { label: '见过相似的人', text: '我在 (时间) 于 (地点) 见到一位与描述相似的人,' },
  { label: '提供线索地点', text: '据老人/邻居回忆,(地点) 附近多年前曾有类似情况,' },
  { label: '看到转发信息', text: '我在 (平台/群) 看到这条寻亲信息,愿意帮忙扩散,' }
]

async function load() {
  loading.value = true
  try {
    c.value = await request.get('/api/cases/' + route.params.id)
    clues.value = await request.get(`/api/clues/case/${route.params.id}`)
    if (!isMine.value && c.value.status === 1) {
      try {
        const res = await request.get(`/api/radar/similar/${c.value.id}`)
        similar.value = res.candidates
        if (similar.value.length) {
          await nextTick()
          renderRadar(similar.value[0].dims)
        }
      } catch { /* 推荐失败不影响主内容 */ }
    }
  } finally {
    loading.value = false
  }
}

/* 五维得分雷达图 */
function renderRadar(dims) {
  if (!radarEl.value) return
  radarChart?.dispose()
  radarChart = echarts.init(radarEl.value)
  radarChart.setOption({
    radar: {
      indicator: [
        { name: '姓名', max: 100 }, { name: '年龄', max: 100 }, { name: '地理', max: 100 },
        { name: '特征', max: 100 }, { name: '时间', max: 100 }
      ],
      radius: '62%', splitArea: { areaStyle: { color: ['#f6efe3', '#efe4d2'] } },
      axisName: { color: '#8f3f2a', fontSize: 11 }
    },
    series: [{
      type: 'radar',
      data: [{
        value: [dims.name || 0, dims.age || 0, dims.geo || 0, dims.feature || 0, dims.time || 0],
        areaStyle: { color: 'rgba(181,87,59,0.25)' }, lineStyle: { color: '#b5573b' }
      }]
    }]
  })
}

async function submitClue() {
  submitting.value = true
  try {
    await request.post('/api/clues', { caseId: c.value.id, ...clueForm.value })
    ElMessage.success('感谢您的线索,已通知发布人')
    clueDialog.value = false
    clueForm.value = { content: '', contact: '' }
    if (isMine.value) clues.value = await request.get(`/api/clues/case/${route.params.id}`)
  } finally {
    submitting.value = false
  }
}

async function mark(cl, status) {
  await request.put(`/api/clues/${cl.id}/status?status=${status}`)
  cl.status = status
  ElMessage.success(status === 1 ? '已标记为有效线索' : '已标记为虚假线索')
}

async function del() {
  await ElMessageBox.confirm('确定删除该寻亲档案吗?删除后不可恢复。', '删除确认', { type: 'warning' })
  await request.delete('/api/cases/' + c.value.id)
  ElMessage.success('已删除')
  router.push('/mycases')
}

onMounted(load)
onBeforeUnmount(() => radarChart?.dispose())
</script>

<style scoped>
.detail-grid {
  display: grid; grid-template-columns: 1fr 340px; gap: 16px; align-items: start;
}
@media (max-width: 1000px) { .detail-grid { grid-template-columns: 1fr; } }

.head { display: flex; gap: 24px; align-items: flex-start; flex-wrap: wrap; }
.photo-box { position: relative; flex: none; }
.photo { width: 210px; height: 210px; border-radius: 14px; }
.photo-empty {
  width: 210px; height: 210px; border-radius: 14px;
  color: #fff; font-size: 80px; font-weight: 800;
  display: flex; align-items: center; justify-content: center;
}
.lamp-count {
  position: absolute; bottom: -12px; left: 50%; transform: translateX(-50%);
  background: #fff; border: 1px solid #e4d8c4; border-radius: 14px;
  font-size: 12px; padding: 3px 12px; cursor: pointer; white-space: nowrap;
  box-shadow: 0 2px 8px rgba(143, 63, 42, 0.15); transition: all 0.2s;
}
.lamp-count:hover { transform: translateX(-50%) scale(1.06); }
.lamp-count.on { background: var(--grad-lamp); border-color: #e8b04c; color: #7a4200; font-weight: 700; }
.info { flex: 1; min-width: 260px; }
.name { font-size: 24px; font-weight: 700; margin-bottom: 8px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.countdown { margin: 4px 0 10px; color: #6f5641; }
.countdown b { font-size: 34px; color: var(--brand-deep); font-family: 'Arial Black', sans-serif; }
.countdown span { font-size: 15px; font-weight: 700; margin: 0 6px 0 2px; }
.countdown em { font-style: normal; font-size: 12.5px; color: #7d6b5c; display: block; }
.countdown.urgent b { color: #b5573b; animation: blink-num 1.3s infinite; }
@keyframes blink-num { 50% { opacity: 0.55; } }
.meta { color: #909399; font-size: 13px; line-height: 1.9; }
.phone { color: var(--brand-deep); }
.tag-row { margin-top: 8px; display: flex; gap: 6px; flex-wrap: wrap; }
.feat-tag { background: #f6efe3; border-color: #e4d8c4; color: #8f3f2a; }
.sec-title { color: #8f3f2a; margin: 14px 0 8px; }
.para { color: #606266; line-height: 1.8; white-space: pre-wrap; }

.action-bar { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.act {
  border: none; border-radius: 20px; padding: 9px 18px; font-size: 14px; font-weight: 700;
  cursor: pointer; transition: all 0.2s;
}
.act:hover { transform: translateY(-2px); }
.act.lamp { background: var(--grad-lamp); color: #7a4200; box-shadow: 0 4px 14px rgba(232, 132, 60, 0.4); }
.act.lamp.on { background: #fff3d6; border: 1px solid #e8b04c; }
.act.clue { background: var(--grad-hero); color: #fff; box-shadow: 0 4px 14px rgba(143, 63, 42, 0.35); }
.act.poster { background: #f0eadd; color: #6f3320; }
.act.copy { background: #e2ebe6; color: #3f6152; }

/* 相似推荐 */
.sim-tip { font-size: 11.5px; color: #7d6b5c; margin: 4px 0 10px; }
.sim-card {
  display: flex; gap: 12px; align-items: center; border: 1px solid #e4d8c4;
  border-radius: 12px; padding: 10px; margin-bottom: 10px; cursor: pointer; background: #fff;
}
.sim-ring {
  width: 46px; height: 46px; border-radius: 50%; flex: none;
  display: flex; align-items: center; justify-content: center;
}
.sim-ring span {
  width: 36px; height: 36px; border-radius: 50%; background: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px; font-weight: 800; color: #8f3f2a;
}
.sim-body { flex: 1; min-width: 0; }
.sim-name { font-weight: 700; font-size: 14px; }
.reasons { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.radar-box { margin: 6px 0; }
.radar-title { font-size: 12px; color: #7d6b5c; margin-bottom: 2px; }
.radar-chart { height: 200px; }

.clue-row { display: flex; align-items: center; gap: 12px; padding: 12px 0; border-bottom: 1px dashed #eee; }
.clue-row:last-child { border-bottom: none; }
.help-card p { font-size: 12.5px; color: #6f5641; line-height: 1.9; margin: 6px 0; }
.clue-templates { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.tpl-label { font-size: 12.5px; color: #7d6b5c; }
.tpl { cursor: pointer; transition: transform 0.15s; }
.tpl:hover { transform: scale(1.05); }
.poster-img { width: 100%; border-radius: 12px; }

:global(.flying-lamp) {
  position: fixed; z-index: 3000; font-size: 28px; pointer-events: none;
  animation: fly-up 2.5s ease-in forwards;
}
@keyframes fly-up {
  0% { transform: translate(0, 0) scale(0.6); opacity: 0; }
  12% { transform: translate(calc(var(--dx) * 0.05), -20px) scale(1.2); opacity: 1; }
  100% { transform: translate(var(--dx), var(--dy)) scale(0.7) rotate(12deg); opacity: 0; }
}
</style>
