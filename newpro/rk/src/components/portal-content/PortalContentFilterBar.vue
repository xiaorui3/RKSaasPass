<template>
  <section class="portal-content-filter-bar">
    <div class="filter-bar-head">
      <div class="filter-copy">
        <p v-if="eyebrow" class="filter-eyebrow">{{ eyebrow }}</p>
        <h2>{{ title }}</h2>
        <p v-if="description" class="filter-description">{{ description }}</p>
      </div>

      <div v-if="$slots.actions" class="filter-actions">
        <slot name="actions" />
      </div>
    </div>

    <div class="filter-controls">
      <slot />
    </div>

    <div v-if="tags.length || $slots.summary" class="filter-summary">
      <div v-if="tags.length" class="summary-tags">
        <span
          v-for="tag in tags"
          :key="`${tag.label}-${tag.value}`"
          class="summary-tag"
        >
          <strong>{{ tag.label }}</strong>
          <span>{{ tag.value }}</span>
        </span>
      </div>

      <div v-if="$slots.summary" class="summary-extra">
        <slot name="summary" />
      </div>
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
  tags: {
    type: Array,
    default: () => []
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-content-filter-bar {
  display: grid;
  gap: 18px;
  padding: 24px;
  border-radius: 24px;
  border: 1px solid rgba(215, 208, 198, 0.88);
  background:
    radial-gradient(circle at top right, rgba(192, 107, 62, 0.12), transparent 28%),
    linear-gradient(180deg, rgba(255, 255, 255, 0.96) 0%, rgba(246, 240, 230, 0.98) 100%);
  box-shadow: $shadow-sm;
}

.filter-bar-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: end;
}

.filter-copy h2 {
  margin: 0;
  font-size: clamp(24px, 2.4vw, 32px);
  line-height: 1.15;
  color: $text-primary;
}

.filter-eyebrow {
  margin: 0 0 10px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: $secondary-color;
}

.filter-description {
  margin: 12px 0 0;
  max-width: 760px;
  font-size: 15px;
  line-height: 1.75;
  color: $text-secondary;
}

.filter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.filter-controls {
  display: grid;
  gap: 16px;
}

.filter-summary {
  display: grid;
  gap: 12px;
  padding-top: 14px;
  border-top: 1px solid rgba(215, 208, 198, 0.72);
}

.summary-tags,
.summary-extra {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.summary-tag {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.86);
  border: 1px solid rgba(215, 208, 198, 0.92);
  color: $text-secondary;
  font-size: 13px;
}

.summary-tag strong {
  color: $text-primary;
  font-weight: 700;
}

@media (max-width: $breakpoint-md) {
  .portal-content-filter-bar {
    padding: 20px;
    border-radius: 20px;
  }

  .filter-bar-head {
    flex-direction: column;
    align-items: flex-start;
  }
}

@media (max-width: 640px) {
  .portal-content-filter-bar {
    gap: 14px;
    padding: 16px;
    border-radius: 18px;
  }

  .filter-copy h2 {
    font-size: 22px;
    line-height: 1.2;
  }

  .filter-description {
    margin-top: 10px;
    font-size: 14px;
    line-height: 1.65;
  }

  .filter-summary {
    gap: 10px;
    padding-top: 12px;
  }

  .summary-tag {
    min-height: 32px;
    padding: 0 12px;
    font-size: 12px;
  }
}
</style>
