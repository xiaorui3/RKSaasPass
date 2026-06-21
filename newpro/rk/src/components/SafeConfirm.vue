<template>
  <el-dialog v-model="visible" title="危险操作确认" width="420px" :close-on-click-modal="false">
    <div class="safe-confirm">
      <el-alert type="error" :closable="false" show-icon>
        <template #title>{{ message }}</el-alert>
      <p class="confirm-hint">请输入 <strong>{{ requiredText }}</strong> 以确认操作</p>
      <el-input v-model="inputText" :placeholder="`请输入 ${requiredText}`" />
    </div>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="danger" :disabled="inputText !== requiredText" @click="handleConfirm">确认{{ actionLabel }}</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'

const visible = ref(false)
const message = ref('')
const requiredText = ref('')
const actionLabel = ref('')
const inputText = ref('')
let resolveFn = null

function show(msg, text, label = '删除') {
  message.value = msg
  requiredText.value = text
  actionLabel.value = label
  inputText.value = ''
  visible.value = true
  return new Promise(resolve => { resolveFn = resolve })
}

function handleConfirm() {
  visible.value = false
  resolveFn?.(true)
}

defineExpose({ show })
</script>

<style scoped>
.safe-confirm { padding: 8px 0; }
.confirm-hint { margin: 16px 0 12px; font-size: 14px; color: #606266; }
</style>
