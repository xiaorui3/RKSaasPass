<template>
  <div ref="listRef" class="drag-sort-list">
    <slot />
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import Sortable from 'sortablejs'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  handle: { type: String, default: '.drag-handle' },
  animation: { type: Number, default: 150 }
})
const emit = defineEmits(['update:modelValue', 'sort'])
const listRef = ref(null)

onMounted(() => {
  nextTick(() => {
    if (!listRef.value) return
    Sortable.create(listRef.value, {
      animation: props.animation,
      handle: props.handle,
      onEnd(evt) {
        const { oldIndex, newIndex } = evt
        if (oldIndex === newIndex) return
        const list = [...props.modelValue]
        const [moved] = list.splice(oldIndex, 1)
        list.splice(newIndex, 0, moved)
        emit('update:modelValue', list)
        emit('sort', { oldIndex, newIndex, list })
      }
    })
  })
})
</script>

<style scoped>
.drag-sort-list { display: contents; }
</style>
