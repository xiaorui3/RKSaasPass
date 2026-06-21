<template>
  <div class="avatar-cropper">
    <div class="avatar-preview" @click="triggerUpload">
      <el-avatar :size="80" :src="previewUrl">
        {{ currentName?.charAt(0) || '?' }}
      </el-avatar>
      <div class="avatar-overlay">更换头像</div>
    </div>
    <input ref="fileInput" type="file" accept="image/*" style="display:none;" @change="handleFileSelect" />

    <el-dialog v-model="showCropper" title="裁剪头像" width="400px">
      <div class="cropper-container">
        <img ref="imgRef" :src="imgSrc" style="max-width:100%;" />
      </div>
      <template #footer>
        <el-button @click="showCropper = false">取消</el-button>
        <el-button type="primary" @click="cropAndSave">确认裁剪</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { uploadManagedFile } from '@/utils/fileUpload'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import Cropper from 'cropperjs'
import 'cropperjs/dist/cropper.css'

const props = defineProps({
  currentAvatar: { type: String, default: '' },
  currentName: { type: String, default: '' }
})
const emit = defineEmits(['update'])

const fileInput = ref(null)
const imgRef = ref(null)
const imgSrc = ref('')
const showCropper = ref(false)
const previewUrl = ref(props.currentAvatar)
let cropper = null

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileSelect(e) {
  const file = e.target.files?.[0]
  if (!file) return
  if (file.size > 5 * 1024 * 1024) {
    return ElMessage.warning('图片大小不能超过5MB')
  }
  const reader = new FileReader()
  reader.onload = (ev) => {
    imgSrc.value = ev.target.result
    showCropper.value = true
    setTimeout(() => {
      if (cropper) cropper.destroy()
      cropper = new Cropper(imgRef.value, {
        aspectRatio: 1,
        viewMode: 1,
        dragMode: 'move',
        autoCropArea: 0.9,
      })
    }, 100)
  }
  reader.readAsDataURL(file)
  e.target.value = ''
}

async function cropAndSave() {
  if (!cropper) return
  const canvas = cropper.getCroppedCanvas({ width: 200, height: 200 })
  canvas.toBlob(async (blob) => {
    const file = new File([blob], 'avatar.jpg', { type: 'image/jpeg' })
    try {
      const { storedValue, url } = await uploadManagedFile({ file }, 'avatar')
      previewUrl.value = resolveMediaUrl(storedValue || url)
      emit('update', storedValue)
      ElMessage.success('头像更新成功')
    } catch {
      ElMessage.error('上传失败')
    }
    showCropper.value = false
    cropper?.destroy()
    cropper = null
  }, 'image/jpeg')
}
</script>

<style scoped>
.avatar-preview { position: relative; cursor: pointer; display: inline-block; }
.avatar-overlay {
  position: absolute; inset: 0; background: rgba(0,0,0,0.5); color: #fff;
  display: flex; align-items: center; justify-content: center;
  border-radius: 50%; opacity: 0; transition: opacity 0.2s; font-size: 12px;
}
.avatar-preview:hover .avatar-overlay { opacity: 1; }
.cropper-container { max-height: 400px; overflow: hidden; }
</style>
