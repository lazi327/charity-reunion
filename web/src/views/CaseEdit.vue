<template>
  <div v-loading="loading">
    <!-- 顶部引导横幅 -->
    <div class="ce-hero fade-up">
      <div>
        <h2><el-icon v-if="isEdit"><EditPen /></el-icon><Lamp v-else /> {{ isEdit ? '编辑寻亲档案' : '发布寻亲信息' }}</h2>
        <p>信息越完整,亲缘雷达定位越精准,团圆机会越大 · 本平台纯公益免费,任何以"买线索/捞人"为名的汇款都是诈骗</p>
      </div>
      <div class="hero-ring" :style="ringStyle">
        <span>{{ recognition }}</span>
        <em>识别度</em>
      </div>
    </div>

    <!-- 步骤条 -->
    <div class="page-card steps-card fade-up" style="animation-delay:.05s">
      <div v-for="(s, i) in steps" :key="i" class="step" :class="{ on: step === i, done: step > i }" @click="goto(i)">
        <span class="s-dot">
          <template v-if="step > i">✓</template><template v-else>{{ i + 1 }}</template>
        </span>
        <span class="s-label"><el-icon><component :is="s.icon" /></el-icon> {{ s.title }}</span>
        <i v-if="i < steps.length - 1" class="s-line"></i>
      </div>
      <div class="step-progress"><i :style="{ width: (step + 1) / steps.length * 100 + '%' }"></i></div>
    </div>

    <el-row :gutter="16">
      <el-col :span="16">
        <div class="page-card form-card fade-up" style="animation-delay:.1s">
          <!-- ① 基本信息 -->
          <template v-if="step === 0">
            <h3 class="sec-h">谁在找 · 找谁</h3>
            <el-form :model="form" label-position="top">
              <el-form-item label="寻亲类型" required>
                <div class="rel-grid">
                  <div v-for="r in relations" :key="r.name" class="rel-opt" :class="{ on: form.relation === r.name }"
                       @click="form.relation = r.name">
                    <span class="ro-icon"><el-icon><component :is="r.icon" /></el-icon></span>
                    <b>{{ r.name }}</b>
                    <em>{{ r.tip }}</em>
                  </div>
                </div>
              </el-form-item>
              <el-form-item label="失踪人照片(近期照最佳,有照片的档案被认出概率高 3 倍)">
                <el-upload class="photo-uploader" drag action="/api/upload" name="file"
                           :headers="uploadHeaders" :show-file-list="false" :on-success="onUploaded"
                           :before-upload="beforeUpload">
                  <template v-if="form.photo">
                    <el-image :src="form.photo" fit="cover" class="up-preview" />
                    <div class="up-mask">
                      <span>点击或拖拽<br />更换照片</span>
                    </div>
                  </template>
                  <template v-else>
                    <div class="up-empty">
                      <div class="ue-icon"><el-icon><Camera /></el-icon></div>
                      拖拽照片到此处,或点击上传<br />
                      <em>支持 jpg / png,5MB 以内</em>
                    </div>
                  </template>
                </el-upload>
                <el-button v-if="form.photo" text type="danger" size="small" @click="form.photo = ''">✕ 移除照片</el-button>
              </el-form-item>
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="失踪人姓名" required>
                    <el-input v-model="form.name" maxlength="20" placeholder="全名或小名均可" clearable />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="性别" required>
                    <el-radio-group v-model="form.gender" size="large">
                      <el-radio-button :value="1">男</el-radio-button>
                      <el-radio-button :value="2">女</el-radio-button>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
            </el-form>
          </template>

          <!-- ② 时间与地点 -->
          <template v-if="step === 1">
            <h3 class="sec-h">何时 · 何地走失</h3>
            <el-form :model="form" label-position="top">
              <el-row :gutter="16">
                <el-col :span="12">
                  <el-form-item label="出生年份">
                    <el-input-number v-model="form.birthYear" :min="1900" :max="2025" controls-position="right" style="width: 100%" />
                    <div class="auto-tip" v-if="form.birthYear">今年约 {{ 2026 - form.birthYear }} 岁 · 雷达按此做高斯年龄比对</div>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="失踪日期">
                    <el-date-picker v-model="form.lostDate" type="date" value-format="YYYY-MM-DD"
                                    placeholder="选择日期" style="width: 100%" :disabled-date="noFuture" />
                    <div class="auto-tip" v-if="form.lostDate">已走失 {{ daysLost }} 天({{ (daysLost / 365).toFixed(1) }} 年)</div>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="失踪地区" required>
                <el-input v-model="form.lostPlace" maxlength="50" placeholder="格式:省 市,如 广东广州" clearable>
                  <template #append>
                    <el-tag v-if="detectedProvince" size="small" effect="dark" style="border: none">已识别 {{ detectedProvince }}省</el-tag>
                    <span v-else style="font-size: 12px">省+市</span>
                  </template>
                </el-input>
                <div class="auto-tip">雷达按"同省 100 分 / 邻省 65 分 / 同大区 40 分"计算地理邻近度,请填写规范省份名</div>
                <div class="prov-chips">
                  <span v-for="p in hotProvinces" :key="p" class="pc" :class="{ on: (form.lostPlace || '').includes(p) }"
                        @click="form.lostPlace = p + (form.lostPlace && !form.lostPlace.startsWith(p) ? ' ' + form.lostPlace.replace(/^[^\s]+/, '') : '')">{{ p }}</span>
                </div>
              </el-form-item>
            </el-form>
          </template>

          <!-- ③ 特征与联系 -->
          <template v-if="step === 2">
            <h3 class="sec-h">长什么样 · 怎么联系</h3>
            <el-form :model="form" label-position="top">
              <el-form-item label="体貌特征(越详细越容易被认出)" required>
                <div class="tag-insert">
                  <span class="ti-label"><el-icon><MagicStick /></el-icon> 一键插入特征词:</span>
                  <el-tag v-for="t in featureWords" :key="t" size="small" effect="plain" round class="ti"
                          :class="{ used: (form.features || '').includes(t) }" @click="insertFeature(t)">{{ t }}</el-tag>
                </div>
                <el-input ref="featRef" v-model="form.features" type="textarea" :rows="4" maxlength="1000" show-word-limit
                          placeholder="身高、脸型、口音、胎记伤疤、走失时衣着等…如:走失时6岁,圆脸,左眉尾有一道小疤,说普通话带广州口音" />
                <el-progress :percentage="Math.min(100, Math.round((form.features || '').length / 40 * 100))" :stroke-width="6"
                             :show-text="false" :color="featColor" style="margin-top: 6px" />
                <div class="auto-tip">{{ featComment }}</div>
              </el-form-item>
              <el-form-item label="失踪经过(选填,但一段真实经过往往最能打动知情者)">
                <el-input v-model="form.story" type="textarea" :rows="4" maxlength="2000" show-word-limit
                          placeholder="何时何地以何种方式走失,当时有无同行者…" />
              </el-form-item>
              <el-form-item label="联系电话(选填,仅档案详情页对浏览者展示)">
                <el-input v-model="form.contactPhone" maxlength="11" placeholder="便于知情者直接联系您" />
                <div class="auto-tip" :class="{ bad: form.contactPhone && !phoneOk }" v-if="form.contactPhone">
                  {{ phoneOk ? '✓ 手机号格式正确' : '✗ 请输入 11 位手机号' }}
                </div>
              </el-form-item>
              <el-form-item>
                <el-checkbox v-model="agreed">我承诺信息真实,并已知悉:任何索要钱财的"线索交易"均与本平台无关</el-checkbox>
              </el-form-item>
            </el-form>
          </template>

          <!-- 底部导航 -->
          <div class="nav-bar">
            <el-button v-if="step > 0" size="large" @click="step--">← 上一步</el-button>
            <span style="flex: 1"></span>
            <el-button v-if="step < 2" type="primary" size="large" @click="next">下一步 →</el-button>
            <el-button v-else type="primary" size="large" :loading="saving" :disabled="!agreed" @click="save">
              <Lamp /> {{ isEdit ? '保存并提交审核' : '提交发布(需审核)' }}
            </el-button>
            <el-button size="large" text @click="$router.back()">返回</el-button>
          </div>
        </div>
      </el-col>

      <!-- 右侧:实时预览 -->
      <el-col :span="8">
        <div class="page-card preview-card fade-up" style="animation-delay:.16s">
          <b class="grad-text"><el-icon><View /></el-icon> 档案大厅效果预览</b>
          <div class="pv-card">
            <div class="pv-photo">
              <el-image v-if="form.photo" :src="form.photo" fit="cover" class="pv-img" />
              <div v-else class="pv-avatar" :style="{ background: avatarGrad(form.name) }">{{ form.name?.[0] || '?' }}</div>
              <span class="pv-days" :class="{ urgent: daysLost > 0 && daysLost <= 30 }">走失 {{ daysLost }} 天</span>
            </div>
            <div class="pv-body">
              <div class="pv-name">{{ form.name || '姓名待填' }}
                <el-tag size="small" effect="plain" round>{{ form.relation || '寻亲类型' }}</el-tag>
              </div>
              <div class="pv-meta">{{ form.gender === 2 ? '女' : '男' }} · {{ form.birthYear ? 2026 - form.birthYear + '岁' : '?岁' }} · {{ form.lostPlace || '失踪地待填' }}</div>
              <div class="pv-feat">{{ (form.features || '体貌特征描述将展示在这里…').slice(0, 60) }}</div>
              <div class="pv-foot"><span><Lamp /> 0 盏回家灯</span><span><el-icon><View /></el-icon> 0</span></div>
            </div>
          </div>
          <div class="pv-tip">这就是审核通过后,全国寻亲家庭看到的样子</div>
        </div>

        <div class="page-card score-card fade-up" style="animation-delay:.2s">
          <b><el-icon><Histogram /></el-icon> 识别度分解(雷达打分同源维度)</b>
          <div v-for="d in scoreDetail" :key="d.label" class="sd-row">
            <span class="sd-label"><el-icon><component :is="d.icon" /></el-icon> {{ d.label }}</span>
            <el-progress :percentage="d.pct" :stroke-width="8" style="flex: 1"
                         :color="d.pct >= 80 ? '#3f6152' : d.pct >= 40 ? '#d98a4f' : '#b5573b'" />
          </div>
          <p class="sc-tip">识别度 ≥ 70 的档案,在亲缘雷达检索中平均多获得 4 倍曝光</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'
import request from '../utils/request'
import { getToken } from '../utils/auth'

const route = useRoute()
const router = useRouter()
const relations = [
  { name: '寻子女', icon: 'Avatar', tip: '寻找走失的孩子' },
  { name: '寻父母', icon: 'UserFilled', tip: '寻找年迈的父母' },
  { name: '寻配偶', icon: 'Female', tip: '寻找走失的爱人' },
  { name: '寻兄弟姐妹', icon: 'User', tip: '寻找手足亲人' },
  { name: '寻其他亲属', icon: 'Postcard', tip: '爷爷/奶奶/姑舅等' }
]
const steps = [
  { icon: 'User', title: '基本信息' },
  { icon: 'MapLocation', title: '时间地点' },
  { icon: 'Connection', title: '特征与联系' }
]
const featureWords = ['圆脸', '瓜子脸', '高个子', '矮个子', '疤痕', '痣/胎记', '龅牙缺牙', '戴眼镜', '方言口音', '跛脚步态', '智力障碍', '文身']
const hotProvinces = ['广东', '四川', '河南', '湖南', '江苏', '浙江', '山东', '安徽', '广西', '云南', '福建', '湖北']
const PROVINCES = ['北京', '天津', '河北', '山西', '内蒙古', '辽宁', '吉林', '黑龙江', '上海', '江苏', '浙江',
  '安徽', '福建', '江西', '山东', '河南', '湖北', '湖南', '广东', '广西', '海南', '重庆', '四川', '贵州',
  '云南', '西藏', '陕西', '甘肃', '青海', '宁夏', '新疆']

const isEdit = computed(() => !!route.params.id)
const loading = ref(false)
const saving = ref(false)
const step = ref(0)
const agreed = ref(false)
const uploadHeaders = computed(() => ({ Authorization: 'Bearer ' + getToken() }))
const form = ref({ relation: '寻子女', gender: 1 })

/* 走失天数 */
const daysLost = computed(() => {
  if (!form.value.lostDate) return 0
  return Math.max(0, Math.floor((Date.now() - new Date(form.value.lostDate).getTime()) / 86400000))
})
const noFuture = (d) => d.getTime() > Date.now()

/* 省份识别 */
const detectedProvince = computed(() => PROVINCES.find(p => (form.value.lostPlace || '').startsWith(p)) || '')

/* 特征文本质量 */
const featColor = computed(() => {
  const n = (form.value.features || '').length
  return n >= 40 ? '#3f6152' : n >= 15 ? '#d98a4f' : '#b5573b'
})
const featComment = computed(() => {
  const n = (form.value.features || '').length
  if (n >= 60) return '描述非常细致,极易被认出'
  if (n >= 40) return '足够详细,可补充走失时衣着更好'
  if (n >= 15) return '再补充脸型/疤痕/口音等辨识点'
  if (n > 0) return '描述偏简略,建议 40 字以上'
  return ''
})

const phoneOk = computed(() => /^1[3-9]\d{9}$/.test(form.value.contactPhone || ''))

/* 识别度:与档案质量挂钩的本地评分(参考雷达五维) */
const scoreDetail = computed(() => {
  const f = form.value
  const featLen = (f.features || '').length
  return [
    { icon: 'PriceTag', label: '姓名', pct: f.name ? 100 : 0 },
    { icon: 'Picture', label: '照片', pct: f.photo ? 100 : 0 },
    { icon: 'Calendar', label: '出生年份', pct: f.birthYear ? 100 : 0 },
    { icon: 'Timer', label: '失踪日期', pct: f.lostDate ? 100 : 0 },
    { icon: 'MapLocation', label: '失踪地区', pct: f.lostPlace ? (detectedProvince.value ? 100 : 60) : 0 },
    { icon: 'Connection', label: '体貌特征', pct: Math.min(100, Math.round(featLen / 40 * 100)) },
    { icon: 'Message', label: '联系电话', pct: f.contactPhone ? (phoneOk.value ? 100 : 50) : 0 }
  ]
})
const recognition = computed(() => {
  const d = scoreDetail.value
  const weights = [18, 15, 10, 10, 15, 27, 5]
  return Math.round(d.reduce((a, x, i) => a + x.pct * weights[i], 0) / 100)
})
const ringStyle = computed(() => {
  const r = recognition.value
  const color = r >= 70 ? '#f7cf6a' : r >= 40 ? '#e0a06a' : '#d98a5f'
  return { background: `conic-gradient(${color} ${r * 3.6}deg, rgba(255,255,255,0.25) 0deg)` }
})

function avatarGrad(name) {
  const grads = ['linear-gradient(135deg,#b5573b,#8f3f2a)', 'linear-gradient(135deg,#d98a4f,#b5573b)',
    'linear-gradient(135deg,#6f3320,#8f3f2a)', 'linear-gradient(135deg,#3f6152,#6f3320)']
  let h = 0
  for (const ch of (name || '亲')) h = (h * 31 + ch.codePointAt(0)) % 997
  return grads[h % grads.length]
}

function insertFeature(t) {
  const word = t.replace('/胎记', '')
  if ((form.value.features || '').includes(word)) return
  form.value.features = form.value.features?.trim()
    ? form.value.features.replace(/[,，]?\s*$/, '') + ',' + word
    : word
}

function beforeUpload(file) {
  const okType = /image\/(jpeg|png)/.test(file.type)
  if (!okType) ElMessage.error('仅支持 jpg / png 格式')
  if (file.size > 5 * 1024 * 1024) ElMessage.error('图片不能超过 5MB')
  return okType && file.size <= 5 * 1024 * 1024
}

function onUploaded(res) {
  if (res.code === 0) {
    form.value.photo = res.data
    ElMessage.success('照片已上传,预览见右侧卡片')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

/* 步骤校验 */
function goto(i) { step.value = i }
function next() {
  const f = form.value
  if (step.value === 0) {
    if (!f.name?.trim()) return ElMessage.warning('请填写失踪人姓名')
    if (!f.relation) return ElMessage.warning('请选择寻亲类型')
  }
  if (step.value === 1) {
    if (!f.lostPlace?.trim()) return ElMessage.warning('请填写失踪地区')
  }
  if (step.value === 2) {
    if ((f.features || '').trim().length < 10) return ElMessage.warning('体貌特征至少写 10 个字,这是被认出的关键')
  }
  step.value = Math.min(2, step.value + 1)
}

async function save() {
  const f = form.value
  if (!f.name?.trim()) { step.value = 0; return ElMessage.warning('请填写失踪人姓名') }
  if ((f.features || '').trim().length < 10) { step.value = 2; return ElMessage.warning('体貌特征太简略') }
  if (f.contactPhone && !phoneOk.value) { step.value = 2; return ElMessage.warning('联系电话格式不正确') }
  saving.value = true
  try {
    if (isEdit.value) {
      await request.put('/api/cases/' + route.params.id, f)
    } else {
      await request.post('/api/cases', f)
    }
    ElMessage.success(`识别度 ${recognition.value} 分,已提交,管理员审核通过后即公开`)
    router.push('/mycases')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  if (!isEdit.value) return
  loading.value = true
  try {
    form.value = await request.get('/api/cases/' + route.params.id)
    agreed.value = true
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.ce-hero {
  display: flex; justify-content: space-between; align-items: center; gap: 20px;
  background: var(--grad-hero); border-radius: 16px; padding: 20px 28px; color: #fff;
  box-shadow: 0 10px 30px rgba(120, 80, 50, 0.3);
}
.ce-hero h2 { margin: 0 0 6px; font-size: 22px; letter-spacing: 1px; }
.ce-hero p { margin: 0; font-size: 12.5px; opacity: 0.92; line-height: 1.7; max-width: 720px; }
.hero-ring {
  width: 92px; height: 92px; border-radius: 50%; flex: none; position: relative;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
}
.hero-ring span {
  width: 70px; height: 70px; border-radius: 50%; background: #7a3a24;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 800; color: #f7cf6a;
}
.hero-ring em { position: absolute; bottom: 4px; font-style: normal; font-size: 11px; opacity: 0.9; }

.steps-card { display: flex; margin-top: 14px; padding: 14px 20px; position: relative; overflow: hidden; }
.step {
  flex: 1; display: flex; align-items: center; gap: 8px; cursor: pointer;
  color: #a8968a; transition: color 0.2s; user-select: none;
}
.step.on { color: var(--brand-deep); font-weight: 700; }
.step.done { color: #3f6152; }
.s-dot {
  width: 28px; height: 28px; border-radius: 50%; flex: none;
  display: flex; align-items: center; justify-content: center;
  background: #efe4d2; font-size: 13px; font-weight: 700; transition: all 0.25s;
}
.step.on .s-dot { background: var(--grad-hero); color: #fff; box-shadow: 0 3px 10px rgba(181, 87, 59, 0.4); transform: scale(1.1); }
.step.done .s-dot { background: #dfeae2; color: #3f6152; }
.s-line { flex: 1; height: 2px; background: #efe4d2; margin: 0 10px; }
.step.done .s-line { background: linear-gradient(90deg, #3f6152, #efe4d2); }
.step-progress { position: absolute; left: 0; bottom: 0; height: 3px; background: #f2e8d9; width: 100%; }
.step-progress i { display: block; height: 100%; background: var(--grad-hero); transition: width 0.4s; }

.sec-h { margin: 2px 0 16px; font-size: 16px; color: #6f4634; }
.rel-grid { display: flex; gap: 10px; flex-wrap: wrap; }
.rel-opt {
  border: 1.5px solid #e4d8c4; border-radius: 12px; padding: 10px 14px; width: 132px;
  cursor: pointer; transition: all 0.2s; text-align: center; background: #fff;
}
.rel-opt:hover { border-color: var(--brand); transform: translateY(-2px); }
.rel-opt.on { border-color: var(--brand); background: linear-gradient(135deg, #f6efe3, #f0e2d2); box-shadow: 0 4px 12px rgba(181, 87, 59, 0.18); }
.rel-opt .ro-icon { font-size: 22px; display: block; }
.rel-opt b { font-size: 13.5px; color: #3a2f28; }
.rel-opt em { font-style: normal; font-size: 11px; color: #9c8b7c; display: block; margin-top: 2px; }

.photo-uploader :deep(.el-upload-dragger) { border-radius: 12px; padding: 0; width: 220px; }
.up-empty { padding: 30px 10px; color: #7d6b5c; font-size: 13px; }
.ue-icon { font-size: 36px; margin-bottom: 6px; }
.up-empty em { font-style: normal; font-size: 11.5px; color: #b3a496; display: block; margin-top: 6px; }
.up-preview { width: 220px; height: 220px; display: block; }
.up-mask {
  position: absolute; inset: 0; background: rgba(120, 80, 50, 0.55); color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 13px;
  opacity: 0; transition: opacity 0.25s; border-radius: 12px; text-align: center; line-height: 1.7;
}
.photo-uploader :deep(.el-upload-dragger:hover) .up-mask { opacity: 1; }

.auto-tip { font-size: 12px; color: #3f6152; margin-top: 4px; line-height: 1.6; width: 100%; }
.auto-tip.bad { color: #f56c6c; }
.prov-chips { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 8px; }
.pc {
  font-size: 12px; border: 1px solid #e4d8c4; border-radius: 12px; padding: 1px 10px;
  color: #6f5641; cursor: pointer; transition: all 0.15s; user-select: none;
}
.pc:hover { border-color: var(--brand); color: var(--brand); }
.pc.on { background: var(--grad-hero); color: #fff; border-color: transparent; }

.tag-insert { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-bottom: 8px; width: 100%; }
.ti-label { font-size: 12px; color: #7d6b5c; }
.ti { cursor: pointer; transition: all 0.15s; }
.ti:hover { transform: translateY(-2px); border-color: var(--brand); color: var(--brand); }
.ti.used { opacity: 0.45; pointer-events: none; text-decoration: line-through; }

.nav-bar { display: flex; align-items: center; gap: 10px; margin-top: 8px; }

.preview-card { position: sticky; top: 84px; }
.pv-card {
  border: 1px solid #e4d8c4; border-radius: 12px; overflow: hidden; margin-top: 12px;
  box-shadow: 0 4px 14px rgba(120, 80, 50, 0.1);
}
.pv-photo { position: relative; }
.pv-img { width: 100%; height: 190px; display: block; }
.pv-avatar {
  height: 190px; display: flex; align-items: center; justify-content: center;
  font-size: 64px; color: #fff; font-weight: 700;
}
.pv-days {
  position: absolute; right: 8px; top: 8px; background: rgba(0, 0, 0, 0.5); color: #fff;
  font-size: 11px; border-radius: 10px; padding: 2px 9px;
}
.pv-days.urgent { background: #b5573b; }
.pv-body { padding: 10px 12px; }
.pv-name { font-weight: 700; display: flex; align-items: center; gap: 6px; }
.pv-meta { font-size: 12px; color: #909399; margin-top: 4px; }
.pv-feat {
  font-size: 12px; color: #7d6b5c; margin-top: 6px; line-height: 1.6; min-height: 36px;
}
.pv-foot { display: flex; justify-content: space-between; font-size: 12px; color: #b1b3b8; margin-top: 6px; }
.pv-tip { font-size: 11.5px; color: #b3a496; text-align: center; margin-top: 8px; }

.score-card { margin-top: 16px; }
.sd-row { display: flex; align-items: center; gap: 10px; margin-top: 10px; }
.sd-label { font-size: 12.5px; color: #6f5641; width: 88px; flex: none; }
.sc-tip { font-size: 11.5px; color: #7d6b5c; margin: 12px 0 0; }
</style>
