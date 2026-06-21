<template>
  <el-dialog v-model="visible" title="活动报名二维码" width="400px" @opened="generateQR">
    <div class="qrcode-wrapper">
      <div ref="qrcodeRef" class="qrcode-box"></div>
      <p class="qrcode-hint">扫描二维码即可报名参加活动</p>
      <el-button type="primary" @click="downloadQR" style="margin-top: 12px;">下载二维码</el-button>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, nextTick } from 'vue'
import QRCode from 'qrcodejs2-fix'

const props = defineProps({ url: String, title: String })
const visible = defineModel({ default: false })
const qrcodeRef = ref(null)

async function generateQR() {
  await nextTick()
  if (qrcodeRef.value) {
    qrcodeRef.value.innerHTML = ''
    new QRCode(qrcodeRef.value, {
      text: props.url || window.location.href,
      width: 256,
      height: 256,
      colorDark: '#303133',
      colorLight: '#ffffff',
    })
  }
}

function downloadQR() {
  const canvas = qrcodeRef.value?.querySelector('canvas')
  if (canvas) {
    const link = document.createElement('a')
    link.download = `${props.title || '活动'}-报名二维码.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
  }
}
</script>

<style scoped>
.qrcode-wrapper { text-align: center; }
.qrcode-box { display: inline-block; padding: 16px; background: #fff; border: 1px solid #ebeef5; border-radius: 8px; }
.qrcode-hint { margin: 12px 0 0; font-size: 14px; color: #909399; }
</style>
