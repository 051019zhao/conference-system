/**
 * 全流程冒烟测试(19 步):node scripts/smoke-test.mjs
 * 覆盖:注册→待审核拦截→管理员审核→登录→草稿→编辑(带PDF)→正式投稿→
 *       伪造PDF拦截→初审→自动分配→五维评审→终审→终稿→录用公示→评审隔离→审计链→头像BYTEA
 * 前置:后端运行于 localhost:8080,数据库为初始种子状态
 */
import { writeFileSync, readFileSync } from 'node:fs'

const BASE = 'http://localhost:8080'
const results = []
const ok = (m) => results.push(`PASS  ${m}`)
const bad = (m) => results.push(`FAIL  ${m}`)

async function api(method, url, body, token) {
  const headers = {}
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined && body !== null && !(body instanceof FormData)) {
    headers['Content-Type'] = 'application/json'
  }
  const res = await fetch(BASE + url, {
    method,
    headers,
    body: body === undefined || body === null ? undefined
      : body instanceof FormData ? body : JSON.stringify(body)
  })
  const text = await res.text()
  try {
    return { status: res.status, json: JSON.parse(text) }
  } catch {
    return { status: res.status, json: null, text }
  }
}

  const minimalPdf = () => new Blob(['%PDF-1.4\n1 0 obj<</Type/Catalog>>endobj\n%%EOF'], { type: 'application/pdf' })
  const fakePdf = () => new Blob(['this is not a pdf'], { type: 'application/pdf' })
  const zipBytes = new Uint8Array([0x50, 0x4B, 0x03, 0x04, 1, 2, 3, 4, 5, 6])
const tinyPng = Buffer.from(
  'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==', 'base64')

const paperPayload = () => ({
  title: '基于openGauss的会议系统测试论文',
  titleEn: 'A Conference System Based on openGauss',
  abstractText: '这是一篇用于全流程自动化测试的论文摘要,关键词命中自动分配。',
  abstractEn: 'A test paper for the conference review workflow.',
  keywords: '数据库,查询优化,会议系统',
  keywordsEn: 'database,query optimization,conference system',
  topicId: 1,
  authors: [
    { name: '测试作者', pinyin: 'Ceshizuozhe', affiliation: '华南理工大学', postalCode: '510000', email: 'smoke@test.com', isCorresponding: true },
    { name: '第二作者', pinyin: 'Dierzuozhe', affiliation: '中山大学', postalCode: '510275', email: '', isCorresponding: false }
  ]
})

const run = async () => {
  const ts = Date.now() % 100000
  const username = `smoke${ts}`
  const email = `smoke${ts}@test.com`

  // 1 发送验证码(dev 模式直接返回)
  const r1 = await api('POST', '/api/auth/send-code', { email })
  const code = r1.json?.data?.devCode
  code ? ok(`1 send-code devCode=${code}`) : bad(`1 send-code: ${r1.text}`)

  // 2 注册(待审核)
  const r2 = await api('POST', '/api/auth/register', {
    username, realName: '测试作者', email, password: 'Smoke@123', code
  })
  r2.json?.code === 200 ? ok('2 注册成功(待审核状态)') : bad(`2 注册: ${r2.text}`)

  // 3 待审核账号登录被拒
  const r3 = await api('POST', '/api/auth/login', { username, password: 'Smoke@123' })
  r3.json?.code !== 200 && /审核/.test(r3.json?.msg || '')
    ? ok('3 待审核账号登录被正确拦截') : bad(`3 应被拦截: ${r3.text}`)

  // 4 管理员审核通过
  const rAdmin = await api('POST', '/api/auth/login', { username: 'admin', password: 'Admin@123' })
  const admin = rAdmin.json?.data?.token
  const r4a = await api('GET', `/api/admin/users?keyword=${username}`, null, admin)
  const uid = r4a.json?.data?.list?.[0]?.id
  const r4 = await api('PUT', `/api/admin/user/${uid}/review?approve=true`, null, admin)
  r4.json?.code === 200 ? ok(`4 管理员通过注册审核(uid=${uid})`) : bad(`4 审核: ${r4.text}`)

  // 5 审核后登录
  const r5 = await api('POST', '/api/auth/login', { username, password: 'Smoke@123' })
  const author = r5.json?.data?.token
  r5.json?.code === 200 && author ? ok('5 审核通过后登录成功') : bad(`5 登录: ${r5.text}`)

  // 6 存草稿(暂不上传主稿,同时上传证明材料)
  const fd6 = new FormData()
  fd6.append('paper', new Blob([JSON.stringify(paperPayload())], { type: 'application/json' }))
  fd6.append('supplements', new Blob([zipBytes], { type: 'application/zip' }), '初始证明.zip')
  const r6 = await api('POST', '/api/paper/submit?draft=true', fd6, author)
  const pid = r6.json?.data
  pid ? ok(`6 存草稿成功 paperId=${pid}`) : bad(`6 草稿: ${r6.text}`)

  // 7 编辑草稿(带合法 PDF 主稿与两份证明材料)
  const fd7 = new FormData()
  fd7.append('paper', new Blob([JSON.stringify(paperPayload())], { type: 'application/json' }))
  fd7.append('file', minimalPdf(), 'test.pdf')
  fd7.append('supplements', new Blob([zipBytes], { type: 'application/zip' }), '补充数据.zip')
  fd7.append('supplements', minimalPdf(), '证明.pdf')
  const r7 = await api('POST', `/api/paper/draft/${pid}`, fd7, author)
  const files7 = (await api('GET', `/api/paper/${pid}`, null, author)).json?.data?.files || []
  const supplements7 = files7.filter(f => f.fileType === 'SUPPLEMENT')
  r7.json?.code === 200 && supplements7.length === 3 && supplements7.every(f => f.isCurrent)
    ? ok('7 编辑草稿时上传主稿和两份证明材料，旧证明材料仍有效')
    : bad(`7 编辑或证明材料记录异常: ${r7.text}`)

  // 8 草稿正式提交
  const r8 = await api('POST', `/api/paper/draft/${pid}/submit`, null, author)
  r8.json?.code === 200 ? ok('8 草稿正式投稿(待初审)') : bad(`8 提交: ${r8.text}`)

  // 9 伪造 PDF 被魔数校验拦截
  const fd9 = new FormData()
  fd9.append('paper', new Blob([JSON.stringify(paperPayload())], { type: 'application/json' }))
  fd9.append('file', fakePdf(), 'fake.pdf')
  const r9 = await api('POST', '/api/paper/submit', fd9, author)
  r9.json?.code !== 200 && /PDF/.test(r9.json?.msg || '')
    ? ok('9 伪造 PDF 被内容级校验拦截') : bad(`9 应拦截: ${r9.text}`)

  // 10 初审通过
  const r10 = await api('POST', `/api/admin/paper/${pid}/first-pass`, null, admin)
  r10.json?.code === 200 ? ok('10 初审通过(进入外审)') : bad(`10 初审: ${r10.text}`)

  // 11 自动生成方案后由管理员补足两位专家，再发布任务
  const r11 = await api('POST', `/api/admin/paper/${pid}/auto-assign`, null, admin)
  const assigned = r11.json?.data || []
  assigned.length >= 1
    ? ok(`11 自动生成评审方案: ${assigned.map((e) => e.realName).join('、')}`)
    : bad(`11 分配: ${r11.text}`)

  const expertList = (await api('GET', '/api/admin/experts-lite', null, admin)).json?.data || []
  const reviewerIds = ['张伟', '李静'].map(name => expertList.find(e => e.realName === name)?.userId)
  const r11b = await api('POST', `/api/admin/paper/${pid}/assign`, { expertIds: reviewerIds }, admin)
  r11b.json?.code === 200 ? ok('11b 管理员调整方案为两位专家') : bad(`11b 调整方案: ${r11b.text}`)
  const r11c = await api('POST', '/api/admin/review-plans/publish', { paperIds: [pid] }, admin)
  r11c.json?.data?.publishedIds?.includes(pid)
    ? ok('11c 管理员发布评审方案') : bad(`11c 发布方案: ${r11c.text}`)

  // 12 两位专家分别提交评审，全部返回后才允许终审
  for (const reviewer of ['expert01', 'expert02']) {
    const loginResult = await api('POST', '/api/auth/login', { username: reviewer, password: 'Expert@123' })
    const token = loginResult.json?.data?.token
    const tasks = (await api('GET', '/api/review/tasks', null, token)).json?.data || []
    const task = tasks.find(t => t.paperId === pid)
    if (!task) { bad(`12 ${reviewer} 未收到评审任务`); continue }
    const submitted = await api('POST', `/api/review/${task.reviewId}/submit`, {
      overallComment: '结构完整,实验充分,建议录用。',
      revisionComment: '建议补充与相关工作的对比实验。',
      rubric: { topicConformity: 'VERY_WELL', topicCoverage: 'BALANCED', innovation: 'HIGH',
        integrity: 'OUTSTANDING', literary: 'TOTAL', technicalDepth: 'FIELD',
        presentation: 'SATISFACTORY', organization: 'SATISFACTORY' },
      recommendationGrade: 'ACCEPT', rejectionReason: null,
      result: 'ACCEPT', recommendJournal: true
    }, token)
    submitted.json?.code === 200 ? ok(`12 ${reviewer} 提交评审`) : bad(`12 ${reviewer} 评审: ${submitted.text}`)
  }
  const returned = (await api('GET', '/api/paper/my', null, author)).json?.data?.find(p => p.id === pid)
  returned?.submittedCount === returned?.reviewCount && returned?.reviewCount >= 2
    ? ok('12 评审意见已全部返回') : bad('12 评审意见未全部返回')
  const unpublished12 = await fetch(`${BASE}/api/public/accepted-paper/${pid}/download`)
  unpublished12.headers.get('content-type')?.includes('application/json')
    ? ok('12a 未录用论文不可公开下载') : bad('12a 未录用论文被公开下载')

  // 13 终审录用
  const r13 = await api('POST', `/api/admin/paper/${pid}/decide`, { result: 'ACCEPTED' }, admin)
  r13.json?.code === 200 ? ok('13 终审:录用') : bad(`13 终审: ${r13.text}`)
  const authorPapers13 = (await api('GET', '/api/paper/my', null, author)).json?.data || []
  authorPapers13.some(p => p.id === pid && p.status === 'ACCEPTED')
    ? ok('13a 作者端可见录用状态') : bad('13a 作者端未看到录用结果')
  const publicMain13 = await fetch(`${BASE}/api/public/accepted-paper/${pid}/download`)
  publicMain13.status === 200 && (await publicMain13.text()).startsWith('%PDF-')
    ? ok('13b 匿名用户可下载已录用论文主稿') : bad(`13b 公开下载失败: ${publicMain13.status}`)

  // 14 作者提交终稿
  const fd14 = new FormData()
  fd14.append('file', new Blob(['%PDF-1.4\nFINAL_VERSION\n%%EOF'], { type: 'application/pdf' }), 'final.pdf')
  const r14 = await api('POST', `/api/paper/${pid}/final`, fd14, author)
  r14.json?.code === 200 ? ok('14 作者提交终稿') : bad(`14 终稿: ${r14.text}`)
  const publicFinal14 = await fetch(`${BASE}/api/public/accepted-paper/${pid}/download`)
  publicFinal14.status === 200 && (await publicFinal14.text()).includes('FINAL_VERSION')
    ? ok('14a 提交终稿后公开下载自动切换为终稿') : bad(`14a 终稿公开下载异常: ${publicFinal14.status}`)

  // 15 录用公示(string_agg 聚合作者)
  const r15 = await api('GET', '/api/public/accepted-papers')
  const found = (r15.json?.data || []).find((p) => p.id === pid)
  found ? ok(`15 录用公示可见(作者串: ${found.authors})`) : bad(`15 公示: ${r15.text}`)

  // 16 评审隔离:作者视角匿名(无姓名无ID)
  const r16 = await api('GET', `/api/paper/${pid}`, null, author)
  const revs = r16.json?.data?.reviews || []
  revs.length >= 1 && !revs[0].expertName && !revs[0].expertId
    ? ok('16 作者视角评审完全匿名(姓名与ID均剥离)') : bad(`16 隔离: ${JSON.stringify(revs[0] || {})}`)

  // 17 审计日志链完整
  const r17 = await api('GET', '/api/admin/logs?page=1&size=50', null, admin)
  const logs = r17.json?.data?.list || []
  const acts = logs.map((l) => l.action).join(',')
  const auditOk = acts.includes('REVIEW') && acts.includes('DECIDE') && acts.includes('REGISTER_REVIEW')
  auditOk
    ? ok(`17 审计日志链完整(共${logs.length}条)`) : bad(`17 审计: ${acts}`)

  // 18/19 头像 BYTEA 存取
  const fd18 = new FormData()
  fd18.append('file', new Blob([tinyPng], { type: 'image/png' }), 'avatar.png')
  const r18 = await api('POST', '/api/account/avatar', fd18, author)
  r18.json?.code === 200 ? ok('18 头像上传(BYTEA 入库)') : bad(`18 头像: ${r18.text}`)
  const res19 = await fetch(`${BASE}/api/public/avatar/${uid}`)
  res19.status === 200 && res19.headers.get('content-type')?.includes('image/png')
    ? ok('19 头像读取接口返回 image/png') : bad(`19 读取: ${res19.status}`)

  // 20 附属资料状态规则:终稿已提交的论文不允许再传附属资料
  const fd20b = new FormData()
  fd20b.append('file', new Blob([zipBytes], { type: 'application/zip' }), 'data.zip')
  const r20 = await api('POST', `/api/paper/${pid}/supplement`, fd20b, author)
  r20.json?.code !== 200 && /评审流程|状态/.test(r20.json?.msg || '')
    ? ok('20 终稿提交后上传附属资料被状态规则拦截') : bad(`20 应拦截: ${r20.text}`)

  // 21 新论文:草稿+zip附属资料+下载+换主稿版本
  const fd21 = new FormData()
  fd21.append('paper', new Blob([JSON.stringify(paperPayload())], { type: 'application/json' }))
  fd21.append('file', minimalPdf(), 'v1.pdf')
  const r21 = await api('POST', '/api/paper/submit?draft=true', fd21, author)
  const pid2 = r21.json?.data
  pid2 ? ok(`21 新草稿(带主稿)pid=${pid2}`) : bad(`21 草稿: ${r21.text}`)

  const fd22 = new FormData()
  fd22.append('file', new Blob([zipBytes], { type: 'application/zip' }), '实验数据.zip')
  const r22 = await api('POST', `/api/paper/${pid2}/supplement`, fd22, author)
  r22.json?.code === 200 ? ok('22 zip 附属资料上传成功(PK 魔数通过)') : bad(`22 附属: ${r22.text}`)

  const fd23 = new FormData()
  fd23.append('file', new Blob(['not a zip at all'], { type: 'application/zip' }), 'fake.zip')
  const r23 = await api('POST', `/api/paper/${pid2}/supplement`, fd23, author)
  r23.json?.code !== 200 && /不符/.test(r23.json?.msg || '')
    ? ok('23 伪造 zip 被魔数校验拦截') : bad(`23 应拦截: ${r23.text}`)

  const r24 = await api('GET', `/api/paper/${pid2}`, null, author)
  const files = r24.json?.data?.files || []
  const mainCount = files.filter((f) => f.fileType === 'MAIN').length
  const supCount = files.filter((f) => f.fileType === 'SUPPLEMENT').length
  mainCount === 1 && supCount === 1
    ? ok('24 附件账本返回主稿+附属资料') : bad(`24 账本: ${JSON.stringify(files.map((f) => f.fileType))}`)

  // 25 换主稿 → 版本链 v1 保留 v2 当前
  const fd25 = new FormData()
  fd25.append('paper', new Blob([JSON.stringify(paperPayload())], { type: 'application/json' }))
  fd25.append('file', minimalPdf(), 'v2.pdf')
  const r25 = await api('POST', `/api/paper/draft/${pid2}`, fd25, author)
  const files2 = (await api('GET', `/api/paper/${pid2}`, null, author)).json?.data?.files || []
  const mains = files2.filter((f) => f.fileType === 'MAIN').sort((a, b) => a.versionNo - b.versionNo)
  r25.json?.code === 200 && mains.length === 2 && mains[1].isCurrent && !mains[0].isCurrent
    ? ok('25 主稿版本链:v1 保留、v2 为当前版') : bad(`25 版本: ${JSON.stringify(mains.map((m) => [m.versionNo, m.isCurrent]))}`)

  const supFile = files2.find((f) => f.fileType === 'SUPPLEMENT')
  const res26 = await fetch(`${BASE}/api/paper/file/${supFile.id}/download`,
    { headers: { Authorization: `Bearer ${author}` } })
  res26.status === 200
    ? ok('26 附属资料按附件ID下载成功') : bad(`26 下载: ${res26.status}`)

  // 27 第二篇论文:草稿提交+初审(为 AI 预审准备 REVIEWING 状态)
  const r27a = await api('POST', `/api/paper/draft/${pid2}/submit`, null, author)
  const r27 = await api('POST', `/api/admin/paper/${pid2}/first-pass`, null, admin)
  r27.json?.code === 200 && r27a.json?.code === 200
    ? ok('27 第二篇论文进入外审(供AI预审)') : bad(`27 外审: ${r27.text}`)

  // 28 AI 预审:触发(异步)+ 轮询至 READY(真实调用智谱 GLM,最长约 90 秒)
  const r28 = await api('POST', `/api/admin/paper/${pid2}/ai-precheck`, null, admin)
  let aiOk = false
  let aiStatus = r28.json?.code === 200 ? 'STARTED' : `TRIGGER_FAIL:${r28.text}`
  if (r28.json?.code === 200) {
    for (let t = 0; t < 18; t++) {
      await new Promise((s) => setTimeout(s, 5000))
      const rc = await api('GET', `/api/paper/${pid2}/ai-precheck`, null, admin)
      aiStatus = rc.json?.data?.status || 'NONE'
      if (aiStatus === 'READY') {
        const content = rc.json.data.content || ''
        aiOk = content.includes('【') && content.includes('参考')
        break
      }
      if (aiStatus === 'FAILED') break
    }
  }
  aiOk ? ok('28 AI 预审生成成功(结构化报告已落库)') : bad(`28 AI 预审: ${aiStatus}`)

  // 29 权限:作者请求 AI 预审应被拒绝(内部参考对作者保密)
  const r29 = await api('GET', `/api/paper/${pid2}/ai-precheck`, null, author)
  r29.status === 403 || /仅对评审专家/.test(r29.json?.msg || '')
    ? ok('29 AI 预审对作者保密(权限拦截生效)') : bad(`29 应拦截: ${r29.text}`)

  // 30 智能客服:基于数据库事实回答投稿截止日期
  const r30 = await api('POST', '/api/public/ai/chat', {
    message: '请问论文投稿的截止时间是什么时候?',
    history: []
  })
  const reply = r30.json?.data?.reply || ''
  r30.json?.code === 200 && reply.length > 5
    ? ok(`30 智能客服回答(数据库事实注入): ${reply.slice(0, 60)}…`)
    : bad(`30 客服: ${r30.text}`)

  const pass = results.filter((r) => r.startsWith('PASS')).length
  const total = results.filter((r) => /^(PASS|FAIL)/.test(r)).length
  results.push('', `=== 结果: ${pass}/${total} 通过 ===`)
  console.log(results.join('\n'))
  process.exit(pass >= total ? 0 : 1)
}

run().catch((e) => {
  results.push(`FATAL ${e.message}`)
  console.log(results.join('\n'))
  process.exit(1)
})
