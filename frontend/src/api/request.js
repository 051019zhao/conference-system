import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import router from '../router'

/**
 * axios 封装:自动携带 Token、统一拆包 Result、统一错误提示、401 自动登出
 */
const request = axios.create({ baseURL: '/api', timeout: 60000 })

request.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) {
    config.headers.Authorization = 'Bearer ' + store.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    // 文件流直接返回
    if (response.config.responseType === 'blob') {
      return response.data
    }
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    ElMessage.error(res.msg || '请求失败')
    return Promise.reject(new Error(res.msg))
  },
  (error) => {
    if (error.response && error.response.status === 401) {
      useUserStore().logout()
      ElMessage.error('未登录或登录已过期')
      router.push('/login')
    } else {
      const msg = (error.response && error.response.data && error.response.data.msg) || '网络异常,请稍后重试'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  }
)

export default request

/** 带 Token 的文件下载:blob 保存为本地文件 */
export async function download(url, fileName) {
  const blob = await request.get(url, { responseType: 'blob' })
  const link = document.createElement('a')
  const objectUrl = URL.createObjectURL(blob)
  link.href = objectUrl
  link.download = fileName || 'download'
  document.body.appendChild(link)
  link.click()
  link.remove()
  setTimeout(() => URL.revokeObjectURL(objectUrl), 1000)
}
