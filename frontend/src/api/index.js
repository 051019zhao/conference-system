import request, { download } from './request'

// ================= 公开接口 =================
export const getConference = () => request.get('/public/conference')
export const getPublicExperts = () => request.get('/public/experts')
export const getTopics = () => request.get('/public/topics')
export const downloadTopicPpt = (id, name, ext = 'pptx') =>
  download(`/public/topic/${id}/ppt`, `${name || '专题报告'}.${ext}`)
export const getKeynotes = () => request.get('/public/keynotes')
export const getSchedule = () => request.get('/public/schedule')
export const getNotifications = () => request.get('/public/notifications')
export const getNotificationDetail = (id) => request.get(`/public/notification/${id}`)
export const downloadNotificationPdf = (id, title) =>
  download(`/public/notification/${id}/pdf`, `${title || '会议通知'}.pdf`)
export const getAcceptedPapers = () => request.get('/public/accepted-papers')
export const downloadAcceptedPaper = (id, title) =>
  download(`/public/accepted-paper/${id}/download`, `${title || '录用论文'}.pdf`)
export const getMaterials = () => request.get('/public/materials')
export const downloadMaterial = (id) => download(`/public/material/${id}/download`)
export const getPublicStats = () => request.get('/public/stats')

// ================= AI 增强 =================
export const aiChat = (message, history) => request.post('/public/ai/chat', { message, history })
export const startAiPrecheck = (paperId) => request.post(`/admin/paper/${paperId}/ai-precheck`)
export const getAiPrecheck = (paperId) => request.get(`/paper/${paperId}/ai-precheck`)

// ================= 认证 =================
export const sendCode = (email) => request.post('/auth/send-code', { email })
export const register = (data) => request.post('/auth/register', data)
export const login = (data) => request.post('/auth/login', data)
export const forgotPassword = (data) => request.post('/auth/forgot-password', data)
export const getMe = () => request.get('/auth/me')
export const updateProfileApi = (data) => request.put('/account/profile', data)
export const getMyExpertProfile = () => request.get('/account/expert-profile')
export const updateMyExpertProfile = (data) => request.put('/account/expert-profile', data)
export const changePasswordApi = (data) => request.put('/account/password', data)
export const uploadAvatarApi = (formData) =>
  request.post('/account/avatar', formData, { headers: { 'Content-Type': 'multipart/form-data' } })

// ================= 作者投稿 =================
export const submitPaper = (formData, draft = false) =>
  request.post(`/paper/submit?draft=${draft}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const updateDraftApi = (id, formData) =>
  request.post(`/paper/draft/${id}`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const submitDraftApi = (id) => request.post(`/paper/draft/${id}/submit`)
export const getMyPapers = () => request.get('/paper/my')
export const getMyPaperTrash = () => request.get('/paper/my/trash')
export const movePaperToTrash = (id) => request.delete(`/paper/${id}`)
export const restorePaperFromTrash = (id) => request.post(`/paper/${id}/restore`)
export const reopenWithdrawnPaper = (id) => request.post(`/paper/${id}/reopen`)
export const getPaperDetail = (id) => request.get(`/paper/${id}`)
export const submitFinal = (id, formData) =>
  request.post(`/paper/${id}/final`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const withdrawPaper = (id) => request.post(`/paper/${id}/withdraw`)
export const downloadPaperFile = (id, type) => download(`/paper/${id}/file?type=${type}`)
export const addSupplementApi = (id, formData) =>
  request.post(`/paper/${id}/supplement`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const deleteSupplementApi = (fileId) => request.delete(`/paper/file/${fileId}`)
export const downloadFileById = (fileId) => download(`/paper/file/${fileId}/download`)
export const getPaperFileBlob = (fileId) => request.get(`/paper/file/${fileId}/download`, { responseType: 'blob' })

// ================= 专家评审 =================
export const getReviewTasks = () => request.get('/review/tasks')
export const submitReviewApi = (reviewId, data) => request.post(`/review/${reviewId}/submit`, data)

// ================= 管理后台 =================
export const getAdminStats = () => request.get('/admin/stats')
export const getUsers = (params) => request.get('/admin/users', { params })
export const createUserApi = (data) => request.post('/admin/user', data)
export const importExpertsApi = (formData) =>
  request.post('/admin/experts/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const updateUserApi = (id, data) => request.put(`/admin/user/${id}`, data)
export const toggleUserStatus = (id, status) => request.put(`/admin/user/${id}/status`, null, { params: { status } })
export const reviewRegisterApi = (id, approve) =>
  request.put(`/admin/user/${id}/review`, null, { params: { approve } })
export const resetPassword = (id) => request.put(`/admin/user/${id}/password`)
export const getExpertsLite = () => request.get('/admin/experts-lite')

export const getAdminPapers = (params) => request.get('/admin/papers', { params })
export const exportPapersCsv = (params) => {
  const q = new URLSearchParams()
  for (const [k, v] of Object.entries(params || {})) {
    if (v !== undefined && v !== null && v !== '') q.append(k, v)
  }
  return download('/admin/papers/export?' + q.toString(), '论文列表.csv')
}
export const firstPassPaper = (id) => request.post(`/admin/paper/${id}/first-pass`)
export const rejectPaper = (id) => request.post(`/admin/paper/${id}/reject`)
export const assignExperts = (id, expertIds) => request.post(`/admin/paper/${id}/assign`, { expertIds })
export const autoAssignExperts = (id) => request.post(`/admin/paper/${id}/auto-assign`)
export const publishReviewPlans = (paperIds) => request.post('/admin/review-plans/publish', { paperIds })
export const decidePaper = (id, result) => request.post(`/admin/paper/${id}/decide`, { result })
export const getOperationLogs = (params) => request.get('/admin/logs', { params })

export const createTopic = (data) => request.post('/admin/topic', data)
export const updateTopicApi = (id, data) => request.put(`/admin/topic/${id}`, data)
export const uploadTopicPptApi = (id, formData) =>
  request.post(`/admin/topic/${id}/ppt`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const deleteTopicApi = (id) => request.delete(`/admin/topic/${id}`)

export const createKeynote = (data) => request.post('/admin/keynote', data)
export const updateKeynoteApi = (id, data) => request.put(`/admin/keynote/${id}`, data)
export const deleteKeynoteApi = (id) => request.delete(`/admin/keynote/${id}`)

export const createSchedule = (data) => request.post('/admin/schedule', data)
export const updateScheduleApi = (id, data) => request.put(`/admin/schedule/${id}`, data)
export const deleteScheduleApi = (id) => request.delete(`/admin/schedule/${id}`)

export const getAdminNotifications = () => request.get('/admin/notifications')
export const createNotificationApi = (data) => request.post('/admin/notification', data)
export const updateNotificationApi = (id, data) => request.put(`/admin/notification/${id}`, data)
export const uploadNotificationPdfApi = (id, formData) =>
  request.post(`/admin/notification/${id}/pdf`, formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const publishNotificationApi = (id) => request.post(`/admin/notification/${id}/publish`)
export const deleteNotificationApi = (id) => request.delete(`/admin/notification/${id}`)

export const uploadMaterial = (formData) =>
  request.post('/admin/material', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const updateMaterialApi = (id, data) => request.put(`/admin/material/${id}`, data)
export const deleteMaterialApi = (id) => request.delete(`/admin/material/${id}`)

export const updateConferenceApi = (data) => request.put('/admin/conference', data)
