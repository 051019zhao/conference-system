<template>
  <div>
    <el-button class="chat-fab" type="primary" size="large" @click="visible = true" aria-label="打开会议助手">
      <el-icon :size="18"><ChatDotRound /></el-icon><span>会议助手</span>
    </el-button>

    <el-drawer v-model="visible" title="会议助手" size="380px" :append-to-body="true">
      <div class="chat-box">
        <el-alert type="info" :closable="false" show-icon style="margin-bottom: 10px"
                  title="根据本会议资料提供智能问答，回答仅供参考" />
        <div ref="listRef" class="msg-list">
          <div v-for="(m, i) in messages" :key="i" class="msg-row" :class="m.role">
            <div class="bubble">{{ m.content }}</div>
          </div>
          <div v-if="loading" class="msg-row assistant">
            <div class="bubble typing">思考中…</div>
          </div>
        </div>
        <div class="input-row">
          <el-input v-model="input" placeholder="例如:投稿什么时候截止?" @keyup.enter="send"
                    :disabled="loading" clearable />
          <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../api/request'

const visible = ref(false)
const loading = ref(false)
const input = ref('')
const listRef = ref(null)
const messages = ref([
  { role: 'assistant', content: '你好!我是会议智能助手,可以回答会期、投稿截止时间、专题方向、投稿流程等问题。请问有什么可以帮你?' }
])

async function scrollToBottom() {
  await nextTick()
  if (listRef.value) listRef.value.scrollTop = listRef.value.scrollHeight
}

async function send() {
  const text = input.value.trim()
  if (!text || loading.value) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  scrollToBottom()
  try {
    // 只传最近 6 轮,服务端同样会截断
    const history = messages.value.slice(-7, -1).map((m) => ({ role: m.role, content: m.content }))
    const data = await request.post('/public/ai/chat', { message: text, history })
    messages.value.push({ role: 'assistant', content: data.reply || '(空回复)' })
  } catch (e) {
    // request.js 已弹错误提示,这里补一条气泡便于追溯
    messages.value.push({ role: 'assistant', content: '(请求失败,请稍后再试)' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}
</script>

<style scoped>
.chat-fab {
  position: fixed;
  right: 24px;
  bottom: 32px;
  z-index: 2000;
  box-shadow: 0 7px 20px rgba(24, 59, 73, 0.25);
  border-radius: 4px;
  padding: 0 16px;
  height: 44px;
  gap: 6px;
}
.chat-box {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
}
.msg-row {
  display: flex;
  margin-bottom: 10px;
}
.msg-row.user {
  justify-content: flex-end;
}
.bubble {
  max-width: 82%;
  padding: 8px 12px;
  border-radius: 10px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}
.msg-row.assistant .bubble {
  background: #f4f4f5;
  color: #303133;
}
.msg-row.user .bubble {
  background: #245466;
  color: #fff;
}
.typing {
  color: #909399;
}
.input-row {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}
</style>
