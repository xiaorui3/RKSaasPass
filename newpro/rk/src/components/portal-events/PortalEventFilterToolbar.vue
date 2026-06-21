<template>
  <section class="portal-event-filter-toolbar">
    <div class="toolbar-top">
      <div class="toolbar-copy">
        <p v-if="eyebrow" class="toolbar-eyebrow">{{ eyebrow }}</p>
        <h2 class="toolbar-title">{{ title }}</h2>
        <p v-if="description" class="toolbar-description">{{ description }}</p>
      </div>

      <div v-if="$slots.actions" class="toolbar-actions">
        <slot name="actions" />
      </div>
    </div>

    <div v-if="summary.length" class="toolbar-summary">
      <article v-for="item in summary" :key="item.label" class="summary-card">
        <p class="summary-value">{{ item.value }}</p>
        <p class="summary-label">{{ item.label }}</p>
        <p v-if="item.hint" class="summary-hint">{{ item.hint }}</p>
      </article>
    </div>

    <div class="toolbar-controls">
      <slot />
    </div>
  </section>
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
  },
  summary: {
    type: Array,
    default: () => []
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-event-filter-toolbar {
  display: flex;
  flex-direction: column;
  gap: 24px;
  padding: 28px;
  border-radius: 24px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(255, 255, 255, 0.92)),
    linear-gradient(135deg, rgba($primary-color, 0.08), rgba($accent-color, 0.06));
  border: 1px solid rgba($primary-color, 0.08);
  box-shadow: 0 24px 48px rgba(24, 33, 31, 0.08);
}

.toolbar-top {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: flex-start;
}

.toolbar-copy {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 760px;
}

.toolbar-eyebrow,
.toolbar-title,
.toolbar-description,
.summary-value,
.summary-label,
.summary-hint {
  margin: 0;
}

.toolbar-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: $accent-color;
}

.toolbar-title {
  font-size: clamp(1.45rem, 2vw, 2rem);
  color: $text-primary;
}

.toolbar-description {
  font-size: 14px;
  line-height: 1.8;
  color: $text-secondary;
}

.toolbar-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  justify-content: flex-end;
}

.toolbar-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
}

.summary-card {
  padding: 18px 20px;
  border-radius: 18px;
  background: rgba($bg-white, 0.95);
  border: 1px solid rgba($primary-color, 0.08);
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
  color: $text-primary;
}

.summary-label {
  margin-top: 8px;
  font-size: 13px;
  color: $text-secondary;
}

.summary-hint {
  margin-top: 6px;
  font-size: 12px;
  color: $text-light;
}

.toolbar-controls {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

@media (max-width: $breakpoint-md) {
  .portal-event-filter-toolbar {
    padding: 22px;
    border-radius: 20px;
  }

  .toolbar-top {
    flex-direction: column;
  }

  .toolbar-actions {
    justify-content: flex-start;
  }
}
</style>
