import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getToken, clearAuth } from './auth'

const request = axios.create({ baseURL: '', timeout: 15000 })

request.interceptors.request.use(cfg => {
  const token = getToken()
  if (token) cfg.headers.Authorization = 'Bearer ' + token
  return cfg
})

request.interceptors.response.use(
  res => {
    const data = res.data
    if (data.code === 0) return data.data
    if (data.code === 401) {
      clearAuth()
      ElMessage.warning('请先登录')
      window.location.href = window.location.pathname.startsWith('/admin') ? '/admin/login' : '/login'
      return Promise.reject(data.msg)
    }
    // code=2: 账号锁定,交由登录页自行处理(倒计时/提示),不弹全局错误
    if (data.code === 2) return Promise.reject(data)
    ElMessage.error(data.msg || '请求失败')
    return Promise.reject(data.msg)
  },
  err => {
    ElMessage.error('网络错误,请确认后端服务已启动')
    return Promise.reject(err)
  }
)

export default request
