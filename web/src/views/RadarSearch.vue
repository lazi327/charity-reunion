<template>
  <div>
    <div class="radar-hero fade-up">
      <div class="rh-left">
        <h2><el-icon><Aim /></el-icon> 亲缘雷达</h2>
        <p>自研五维匹配引擎:编辑距离姓名比对 · 高斯年龄衰减 · 省份邻接地理图 · Jaccard 特征重合 · 时间分段衰减<br />
           纯本地算法,不经过任何第三方大模型,您的描述数据不出服务器。</p>
      </div>
      <div class="rh-right">
        <div class="dim-legend" v-for="d in dimLegend" :key="d.key">
          <span class="dl-icon"><el-icon><component :is="d.icon" /></el-icon></span>{{ d.label }} <b>{{ d.weight }}%</b>
        </div>
      </div>
    </div>

    <el-row :gutter="16" style="margin-top: 16px">
      <!-- 左:特征输入 -->
      <el-col :span="10">
        <div class="page-card fade-up" style="animation-delay:.06s">
          <b class="grad-text">① 用大白话描述(可选)</b>
          <p class="tip">系统内置本地特征抽取器,可自动识别描述里的性别、年龄、省份、体貌关键词</p>
          <el-input v-model="form.freeText" type="textarea" :rows="5" maxlength="1000" show-word-limit
                    placeholder="例:弟弟张小明,1995年前后出生,男,身高1.7米左右,左眉尾有一道疤,会讲普通话带广州口音,2001年在广州天河区走失…" />
          <div style="display:flex;gap:10px;margin:10px 0 18px">
            <el-button type="primary" plain :loading="extracting" :disabled="!form.freeText.trim()" @click="doExtract">
              <el-icon><MagicStick /></el-icon> 一键抽取特征
            </el-button>
            <el-button text @click="form.freeText = ''">清空描述</el-button>
          </div>

          <b class="grad-text">② 核对/补全特征向量</b>
          <el-form label-position="top" class="vec-form">
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="姓名线索">
                  <el-input v-model="form.nameHint" placeholder="全名或小名,如:小明" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别">
                  <el-radio-group v-model="form.gender">
                    <el-radio-button :value="1">男</el-radio-button>
                    <el-radio-button :value="2">女</el-radio-button>
                    <el-radio-button :value="null">未知</el-radio-button>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="12">
              <el-col :span="12">
                <el-form-item label="出生年份">
                  <el-input-number v-model="form.birthYear" :min="1930" :max="2026" placeholder="如 1995"
                                   style="width: 100%" controls-position="right" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="失踪年份">
                  <el-input-number v-model="form.lostYear" :min="1940" :max="2026" placeholder="如 2001"
                                   style="width: 100%" controls-position="right" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="失踪省份">
              <el-select v-model="form.province" placeholder="选择省份(邻省也会获得邻近分)" clearable filterable style="width: 100%">
                <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
              </el-select>
            </el-form-item>
            <el-form-item label="体貌标签(点击多选)">
              <div class="tag-picker">
                <span v-tappable v-for="t in lexicon" :key="t" class="tag-opt" :class="{ on: form.tags.includes(t) }"
                      @click="switchTag(t)">{{ t }}</span>
              </div>
            </el-form-item>
          </el-form>

          <div class="vec-power">
            特征向量完整度:
            <el-progress :percentage="vectorPower" :stroke-width="10" striped striped-flow
                         :color="vectorPower > 70 ? '#67c23a' : vectorPower > 40 ? '#d98a4f' : '#b5573b'" />
            <span class="vp-tip">{{ powerComment }}</span>
          </div>

          <el-button type="primary" size="large" style="width: 100%; margin-top: 14px" :loading="loading" @click="search">
            <el-icon><Promotion /></el-icon> 启动雷达 · 全库比对
          </el-button>
        </div>
      </el-col>

      <!-- 右:匹配结果 -->
      <el-col :span="14">
        <div class="page-card fade-up" style="min-height: 480px; animation-delay:.12s" v-loading="loading">
          <div class="head-row">
            <b class="grad-text">匹配结果</b>
            <el-tag v-if="engine" size="small" type="warning" effect="plain">{{ engine }}</el-tag>
          </div>

          <div v-if="extracted" class="extracted-chips">
            <span class="ec-label">雷达输入向量:</span>
            <el-tag v-if="extracted.nameHint" size="small" effect="light" round>姓名:{{ extracted.nameHint }}</el-tag>
            <el-tag v-if="extracted.gender" size="small" effect="light" round>{{ extracted.gender === 1 ? '男' : '女' }}</el-tag>
            <el-tag v-if="extracted.birthYear" size="small" effect="light" round>{{ extracted.birthYear }}年生</el-tag>
            <el-tag v-if="extracted.lostYear" size="small" effect="light" round>{{ extracted.lostYear }}年走失</el-tag>
            <el-tag v-if="extracted.province" size="small" effect="light" round>{{ extracted.province }}</el-tag>
            <el-tag v-for="t in extracted.tags" :key="t" size="small" type="info" effect="light" round>{{ t }}</el-tag>
          </div>

          <el-empty v-if="searched && !candidates.length" :image-size="90"
                    description="暂未命中相似档案:可放宽省份、删减标签后重试,或等待新档案入库">
            <el-button type="primary" plain @click="$router.push('/cases')">浏览全部寻亲档案</el-button>
          </el-empty>

          <div v-tappable v-for="(item, i) in candidates" :key="item.case.id" class="result-row lift"
               @click="$router.push('/cases/' + item.case.id)">
            <div class="rank" :class="'r' + (i + 1)">{{ i + 1 }}</div>
            <div class="ring" :style="ringStyle(item.score)"><span>{{ item.score }}</span></div>
            <div style="flex: 1; min-width: 0">
              <div class="r-name">{{ item.case.name }}
                <el-tag size="small" effect="plain">{{ item.case.relation }}</el-tag>
                <el-tag size="small" :type="item.case.gender === 2 ? 'danger' : 'primary'" effect="plain">
                  {{ item.case.gender === 2 ? '女' : '男' }}
                </el-tag>
              </div>
              <div class="meta">{{ item.case.lostPlace }} · 失踪于 {{ item.case.lostDate || '未填' }} · 发布人 {{ item.case.publisherName }}</div>
              <div class="dims">
                <span v-for="d in dimLegend" :key="d.key" class="dim-bar" :title="d.label + ':' + (item.dims[d.key] || 0)">
                  <i :style="{ width: (item.dims[d.key] || 0) + '%', background: d.color }"></i>
                  <em><el-icon><component :is="d.icon" /></el-icon></em>
                </span>
              </div>
              <div class="reasons">{{ item.reasons.join(' · ') }}</div>
            </div>
          </div>

          <div v-if="!searched && !loading" class="idle-hint">
            <div class="idle-icon"><el-icon><Aim /></el-icon></div>
            左侧填写特征或粘贴描述,点击"启动雷达"开始五维比对<br />
            特征越完整,雷达定位越精准
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../utils/request'

const provinces = ['北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江', '上海', '江苏', '浙江',
  '安徽', '福建', '江西', '山东', '河南', '湖北', '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州',
  '云南', '西藏', '陕西', '甘肃', '青海', '宁夏', '新疆']

const dimLegend = [
  { key: 'name', label: '姓名相似', weight: 15, icon: 'PriceTag', color: '#b5573b' },
  { key: 'age', label: '年龄吻合', weight: 20, icon: 'Calendar', color: '#d98a4f' },
  { key: 'geo', label: '地理邻近', weight: 25, icon: 'MapLocation', color: '#6f3320' },
  { key: 'feature', label: '特征重合', weight: 25, icon: 'Connection', color: '#3f6152' },
  { key: 'time', label: '时间接近', weight: 15, icon: 'Timer', color: '#c98a3d' }
]

const form = ref({ freeText: '', nameHint: '', gender: null, birthYear: null, lostYear: null, province: '', tags: [] })
const lexicon = ref([])
const extracting = ref(false)
const loading = ref(false)
const searched = ref(false)
const engine = ref('')
const extracted = ref(null)
const candidates = ref([])

/* 完整度:每个维度是否有输入 */
const vectorPower = computed(() => {
  const f = form.value
  let n = 0
  if (f.nameHint) n++
  if (f.gender) n++
  if (f.birthYear) n++
  if (f.lostYear) n++
  if (f.province) n++
  n = Math.min(n + Math.min(f.tags.length, 2) - (f.tags.length ? 1 : 0), 6)
  return Math.round(Math.max(0, n) / 6 * 100)
})
const powerComment = computed(() => {
  const p = vectorPower.value
  if (p >= 80) return '输入非常完整,比对精度最高'
  if (p >= 50) return '再补充体貌标签和失踪年份会更准'
  if (p > 0) return '特征偏少,结果可能偏宽泛'
  return '尚未输入任何特征'
})

function switchTag(t) {
  const i = form.value.tags.indexOf(t)
  i >= 0 ? form.value.tags.splice(i, 1) : form.value.tags.push(t)
}

function ringStyle(score) {
  const color = score >= 70 ? '#b5573b' : score >= 50 ? '#d98a4f' : '#6f3320'
  return { background: `conic-gradient(${color} ${score * 3.6}deg, #efe4d2 0deg)` }
}

/* 一键抽取:调后端 NLP-lite 抽取器并回填表单 */
async function doExtract() {
  extracting.value = true
  try {
    const ex = await request.post('/api/radar/extract', { text: form.value.freeText })
    if (ex.empty) {
      ElMessage.info('未能从描述中识别出特征,请尝试手动填写下方表单')
      return
    }
    if (ex.nameHint) form.value.nameHint = ex.nameHint
    if (ex.gender) form.value.gender = ex.gender
    if (ex.birthYear) form.value.birthYear = ex.birthYear
    if (ex.lostYear) form.value.lostYear = ex.lostYear
    if (ex.province) form.value.province = ex.province
    if (ex.tags?.length) form.value.tags = Array.from(new Set([...form.value.tags, ...ex.tags]))
    ElMessage.success(`抽取完成:识别出 ${[ex.nameHint, ex.gender, ex.birthYear, ex.lostYear, ex.province].filter(Boolean).length} 项基础特征 + ${ex.tags?.length || 0} 个体貌标签`)
  } finally {
    extracting.value = false
  }
}

async function search() {
  const f = form.value
  const hasAny = f.freeText || f.nameHint || f.gender || f.birthYear || f.lostYear || f.province || f.tags.length
  if (!hasAny) {
    ElMessage.warning('请至少提供一项特征')
    return
  }
  loading.value = true
  try {
    const data = await request.post('/api/radar/match', {
      freeText: f.freeText || undefined,
      nameHint: f.nameHint || undefined,
      gender: f.gender ?? undefined,
      birthYear: f.birthYear ?? undefined,
      lostYear: f.lostYear ?? undefined,
      province: f.province || undefined,
      tags: f.tags
    })
    engine.value = data.engine
    extracted.value = data.extracted
    candidates.value = data.candidates
    searched.value = true
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try { lexicon.value = await request.get('/api/radar/lexicon') } catch { /* 忽略 */ }
})
</script>

<style scoped>
.radar-hero {
  display: flex; justify-content: space-between; align-items: center; gap: 20px;
  background: var(--grad-hero); border-radius: 16px; padding: 22px 28px; color: #fff;
  box-shadow: 0 10px 30px rgba(143, 63, 42, 0.3);
}
.rh-left h2 { margin: 0 0 8px; font-size: 24px; letter-spacing: 2px; }
.rh-left p { margin: 0; font-size: 12.5px; opacity: 0.92; line-height: 1.8; }
.rh-right { display: flex; gap: 14px; flex-wrap: wrap; }
.dim-legend {
  background: rgba(255, 255, 255, 0.14); border-radius: 10px; padding: 8px 14px;
  font-size: 12px; display: flex; align-items: center; gap: 5px;
}
.dim-legend b { color: #ffd76e; }
.tip { font-size: 12px; color: #7d6b5c; margin: 6px 0 8px; }
.vec-form :deep(.el-form-item) { margin-bottom: 12px; }
.tag-picker { display: flex; flex-wrap: wrap; gap: 8px; }
.tag-opt {
  border: 1px solid #e4d8c4; border-radius: 14px; padding: 3px 12px;
  font-size: 12.5px; color: #6f5641; cursor: pointer; transition: all 0.18s; user-select: none;
}
.tag-opt:hover { border-color: var(--brand); color: var(--brand); transform: translateY(-1px); }
.tag-opt.on { background: var(--grad-hero); color: #fff; border-color: transparent; }
.vec-power { font-size: 12.5px; color: #6f5641; }
.vp-tip { font-size: 12px; color: #7d6b5c; margin-top: 4px; display: block; }

.head-row { display: flex; justify-content: space-between; align-items: center; }
.extracted-chips { margin: 10px 0 4px; display: flex; gap: 6px; flex-wrap: wrap; align-items: center; }
.ec-label { font-size: 12px; color: #7d6b5c; }
.result-row {
  display: flex; align-items: center; gap: 12px; padding: 14px 8px;
  border-bottom: 1px dashed #e4d8c4; cursor: pointer; border-radius: 10px;
}
.result-row:hover { background: #f6efe3; }
.rank {
  width: 28px; height: 28px; border-radius: 8px; flex: none;
  display: flex; align-items: center; justify-content: center;
  font-weight: 800; background: #efe4d2; color: #7d6b5c;
}
.rank.r1 { background: var(--grad-hero); color: #fff; }
.rank.r2 { background: linear-gradient(135deg, #d98a4f, #c98a3d); color: #fff; }
.rank.r3 { background: #e2ebe6; color: #3f6152; }
.ring {
  width: 44px; height: 44px; border-radius: 50%; flex: none;
  display: flex; align-items: center; justify-content: center;
}
.ring span {
  width: 34px; height: 34px; border-radius: 50%; background: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 12.5px; font-weight: 800; color: #8f3f2a;
}
.r-name { font-weight: 700; display: flex; align-items: center; gap: 6px; }
.meta { font-size: 12px; color: #909399; margin-top: 3px; }
.dims { display: flex; gap: 6px; margin-top: 6px; }
.dim-bar {
  flex: 1; height: 6px; background: #efe4d2; border-radius: 3px; position: relative; overflow: hidden;
}
.dim-bar i { position: absolute; inset: 0 auto 0 0; border-radius: 3px; transition: width 0.8s; }
.dim-bar em { position: absolute; right: -2px; top: -6px; font-style: normal; font-size: 9px; opacity: 0.75; }
.reasons { font-size: 12px; color: #8f3f2a; margin-top: 5px; }
.idle-hint { text-align: center; color: #7d6b5c; font-size: 13.5px; padding: 60px 0; line-height: 2; }
.idle-icon { font-size: 46px; animation: radar-spin 4s linear infinite; }
@keyframes radar-spin { 50% { transform: rotate(180deg) scale(1.1); } 100% { transform: rotate(360deg); } }
</style>
