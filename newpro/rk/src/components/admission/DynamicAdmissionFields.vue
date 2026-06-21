<template>
  <template v-for="field in visibleFields" :key="field.key">
    <el-form-item :label="field.label" :prop="field.key">
      <el-input
        v-if="isTextLike(field)"
        v-model="model[field.key]"
        :type="field.type === 'number' ? 'number' : 'text'"
        :placeholder="field.placeholder || `请输入${field.label}`"
      />

      <el-input
        v-else-if="field.type === 'textarea'"
        v-model="model[field.key]"
        type="textarea"
        :rows="4"
        :placeholder="field.placeholder || `请输入${field.label}`"
      />

      <el-select
        v-else-if="field.type === 'select'"
        v-model="model[field.key]"
        style="width: 100%"
        :placeholder="field.placeholder || `请选择${field.label}`"
      >
        <el-option
          v-for="option in field.options || []"
          :key="`${field.key}-${option.value}`"
          :label="option.label"
          :value="option.value"
        />
      </el-select>

      <el-radio-group
        v-else-if="field.type === 'radio'"
        v-model="model[field.key]"
      >
        <el-radio
          v-for="option in field.options || []"
          :key="`${field.key}-${option.value}`"
          :label="option.value"
        >
          {{ option.label }}
        </el-radio>
      </el-radio-group>

      <el-checkbox-group
        v-else-if="field.type === 'checkbox'"
        v-model="model[field.key]"
      >
        <el-checkbox
          v-for="option in field.options || []"
          :key="`${field.key}-${option.value}`"
          :label="option.value"
        >
          {{ option.label }}
        </el-checkbox>
      </el-checkbox-group>

      <el-date-picker
        v-else-if="field.type === 'date'"
        v-model="model[field.key]"
        type="date"
        style="width: 100%"
        value-format="YYYY-MM-DD"
        :placeholder="field.placeholder || `请选择${field.label}`"
      />

      <el-input
        v-else
        v-model="model[field.key]"
        :placeholder="field.placeholder || `请输入${field.label}`"
      />
    </el-form-item>
  </template>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  fields: {
    type: Array,
    default: () => []
  },
  model: {
    type: Object,
    required: true
  },
  excludeKeys: {
    type: Array,
    default: () => []
  }
})

const visibleFields = computed(() => {
  const excluded = new Set(props.excludeKeys || [])
  return (props.fields || []).filter(field => field?.enabled !== false && !excluded.has(field.key))
})

const isTextLike = (field) => ['text', 'url', 'number'].includes(field.type)
</script>
