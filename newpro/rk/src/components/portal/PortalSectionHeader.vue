<template>
  <div class="portal-section-header">
    <div>
      <p v-if="eyebrow" class="section-eyebrow">{{ eyebrow }}</p>
      <h2>{{ title }}</h2>
      <p v-if="description" class="section-description">{{ description }}</p>
    </div>

    <div v-if="$slots.trailing" class="section-trailing">
      <slot name="trailing" />
    </div>
  </div>
</template>

<script setup>
defineProps({
  eyebrow: {
    type: String,
    default: ''
  },
  title: {
    type: String,
    required: true
  },
  description: {
    type: String,
    default: ''
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-section-header {
  display: flex;
  justify-content: space-between;
  align-items: end;
  gap: 16px;
  margin-bottom: 20px;
}

.section-eyebrow {
  margin: 0 0 8px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: $secondary-color;
}

.portal-section-header h2 {
  margin: 0;
  font-size: clamp(24px, 2.6vw, 34px);
  line-height: 1.2;
  color: $text-primary;
}

.section-description {
  margin: 10px 0 0;
  max-width: 760px;
  font-size: 15px;
  line-height: 1.8;
  color: $text-secondary;
}

.section-trailing {
  flex-shrink: 0;
}

@media (max-width: $breakpoint-md) {
  .portal-section-header {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .portal-section-header {
    gap: 12px;
    margin-bottom: 16px;
  }

  .portal-section-header h2 {
    font-size: 22px;
    line-height: 1.25;
  }

  .section-description {
    margin-top: 8px;
    font-size: 14px;
    line-height: 1.65;
  }
}
</style>
