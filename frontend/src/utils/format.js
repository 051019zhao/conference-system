/** 展示格式化工具 */

export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').slice(0, 19)
}

export function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

export function formatSize(bytes) {
  if (!bytes && bytes !== 0) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

export const paperStatusText = {
  DRAFT: '草稿',
  SUBMITTED: '待初审',
  REVIEWING: '外审中',
  REVIEW_RETURNED: '评审意见已返回',
  ACCEPTED: '已录用',
  REJECTED: '已退稿',
  FINAL_SUBMITTED: '终稿已提交',
  WITHDRAWN: '已撤回'
}

export const paperStatusTag = {
  DRAFT: 'info',
  SUBMITTED: 'info',
  REVIEWING: 'warning',
  REVIEW_RETURNED: 'success',
  ACCEPTED: 'success',
  REJECTED: 'danger',
  FINAL_SUBMITTED: 'success',
  WITHDRAWN: 'info'
}

/** 用户账号状态:2 待管理员审核 / 1 正常 / 0 禁用 */
export const userStatusText = { 0: '禁用', 1: '正常', 2: '待审核' }

/** 评审表五维评分的中文标签 */
export const scoreDimensions = [
  { key: 'scoreFit', label: '主题契合度' },
  { key: 'scoreInnovation', label: '创新性' },
  { key: 'scoreComplete', label: '完整性' },
  { key: 'scoreDepth', label: '技术深度' },
  { key: 'scoreClarity', label: '表达质量' }
]

export const roleText = {
  ADMIN: '管理员',
  EXPERT: '专家',
  AUTHOR: '作者'
}
