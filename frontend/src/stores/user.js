import { defineStore } from 'pinia'

/**
 * 登录态:token + 用户信息,localStorage 持久化,刷新页面不丢失
 */
export const useUserStore = defineStore('user', {
  state: () => ({
    token: localStorage.getItem('token') || '',
    user: JSON.parse(localStorage.getItem('user') || 'null')
  }),
  getters: {
    isLogin: (s) => !!s.token,
    role: (s) => (s.user ? s.user.role : ''),
    realName: (s) => (s.user ? s.user.realName : '')
  },
  actions: {
    setLogin(token, user) {
      this.token = token
      this.user = user
      localStorage.setItem('token', token)
      localStorage.setItem('user', JSON.stringify(user))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem('token')
      localStorage.removeItem('user')
    }
  }
})
