export const rubricFields = [
  { key: 'topicConformity', label: '主题契合度', options: [
    ['VERY_WELL', '非常契合会议主题'], ['FAIRLY', '基本契合'], ['POORLY', '契合度较低']
  ] },
  { key: 'topicCoverage', label: '主题覆盖', options: [
    ['BALANCED', '内容全面且平衡'], ['MISSING', '重要信息缺失或讨论肤浅'], ['OVERSTRESSED', '部分内容过度展开']
  ] },
  { key: 'innovation', label: '创新性', options: [
    ['HIGH', '高度创新'], ['SUFFICIENT', '较有创新'], ['SLIGHT', '略有创新'], ['NONE', '缺少创新']
  ] },
  { key: 'integrity', label: '完整性', options: [
    ['POOR', '较差'], ['FAIR', '一般'], ['GOOD', '良好'], ['OUTSTANDING', '优秀']
  ] },
  { key: 'literary', label: '文字表达', options: [
    ['TOTAL', '完全易懂'], ['MOSTLY', '基本易懂'], ['PARTIAL', '部分难懂'], ['INACCESSIBLE', '难以理解']
  ] },
  { key: 'technicalDepth', label: '技术深度', options: [
    ['SUPERFICIAL', '较为肤浅'], ['NON_SPECIALIST', '适合非专业人士'],
    ['FIELD', '适合领域一般从业者'], ['EXPERT', '仅适合领域专家']
  ] },
  { key: 'presentation', label: '表达与英语', options: [
    ['SATISFACTORY', '满意'], ['IMPROVE', '需要改进'], ['POOR', '较差']
  ] },
  { key: 'organization', label: '整体组织', options: [
    ['SATISFACTORY', '满意'], ['IMPROVE', '可以改进'], ['POOR', '较差']
  ] }
]

export const recommendationOptions = {
  ACCEPT: [['STRONG_ACCEPT', '强烈建议录用'], ['ACCEPT', '建议录用'], ['WEAK_ACCEPT', '倾向录用']],
  REJECT: [['STRONG_REJECT', '强烈建议退稿'], ['REJECT', '建议退稿'], ['WEAK_REJECT', '倾向退稿']]
}

export const rejectionReasons = [
  ['QUALITY', '质量或创新性不足'], ['REWRITE', '需要大幅重写'],
  ['TOPIC', '主题与会议不符'], ['OTHER', '其他原因']
]

export function rubricEntries(json) {
  if (!json) return []
  try {
    const values = JSON.parse(json)
    return rubricFields.map(field => ({
      label: field.label,
      value: field.options.find(([key]) => key === values[field.key])?.[1] || '-'
    }))
  } catch {
    return []
  }
}

export function recommendationText(value) {
  return Object.values(recommendationOptions).flat().find(([key]) => key === value)?.[1] || value || '-'
}
