<template>
  <section class="portal-hero">
    <div class="container">
      <div class="hero-shell" :class="{ 'has-aside': hasAside }">
        <div class="hero-main">
          <p v-if="eyebrow" class="hero-eyebrow">{{ eyebrow }}</p>
          <h1>{{ title }}</h1>
          <p v-if="description" class="hero-description">{{ description }}</p>

          <div v-if="$slots.actions" class="hero-actions">
            <slot name="actions" />
          </div>

          <div v-if="metrics.length" class="hero-metrics">
            <article v-for="metric in metrics" :key="metric.label" class="metric-card">
              <p class="metric-label">{{ metric.label }}</p>
              <p class="metric-value">{{ metric.value }}</p>
              <p v-if="metric.detail" class="metric-detail">{{ metric.detail }}</p>
            </article>
          </div>
        </div>

        <aside v-if="hasAside" class="hero-aside">
          <slot name="aside">
            <p v-if="bandEyebrow" class="aside-eyebrow">{{ bandEyebrow }}</p>
            <h2 v-if="bandTitle">{{ bandTitle }}</h2>
            <p v-if="bandDescription" class="aside-description">{{ bandDescription }}</p>
          </slot>
        </aside>
      </div>
    </div>
  </section>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
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
  metrics: {
    type: Array,
    default: () => []
  },
  bandEyebrow: {
    type: String,
    default: ''
  },
  bandTitle: {
    type: String,
    default: ''
  },
  bandDescription: {
    type: String,
    default: ''
  }
})

const slots = defineSlots()

const hasAside = computed(() => {
  return Boolean(slots.aside || props.bandEyebrow || props.bandTitle || props.bandDescription)
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-hero {
  padding: 28px 0 10px;
}

.hero-shell {
  display: grid;
  gap: 24px;
  background:
    radial-gradient(circle at top right, rgba(192, 107, 62, 0.15), transparent 34%),
    linear-gradient(180deg, #f6f0e6 0%, #fbf7f0 100%);
  border: 1px solid $border-light;
  border-radius: 28px;
  padding: 34px;
  box-shadow: $shadow-sm;
}

.hero-shell.has-aside {
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  align-items: stretch;
}

.hero-main h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 52px);
  line-height: 1.1;
  color: $text-primary;
}

.hero-eyebrow,
.aside-eyebrow {
  margin: 0 0 12px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: $secondary-color;
}

.hero-description,
.aside-description {
  margin: 18px 0 0;
  max-width: 780px;
  font-size: 16px;
  line-height: 1.85;
  color: $text-secondary;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 24px;
}

.hero-metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 14px;
  margin-top: 28px;
}

.metric-card {
  min-height: 116px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  border: 1px solid rgba(215, 208, 198, 0.8);
}

.metric-label {
  margin: 0 0 8px;
  font-size: 12px;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: $text-light;
}

.metric-value {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  color: $text-primary;
}

.metric-detail {
  margin: 10px 0 0;
  font-size: 13px;
  line-height: 1.7;
  color: $text-secondary;
}

.hero-aside {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 16px;
  padding: 24px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(31, 58, 52, 0.97) 0%, rgba(22, 42, 37, 0.96) 100%);
  color: $text-white;
  box-shadow: $shadow-base;
}

.hero-aside h2 {
  margin: 0;
  font-size: 28px;
  line-height: 1.2;
}

.hero-aside .aside-eyebrow,
.hero-aside .aside-description {
  color: rgba(255, 255, 255, 0.8);
}

@media (max-width: $breakpoint-lg) {
  .hero-shell.has-aside {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .portal-hero {
    padding-top: 18px;
  }

  .hero-shell {
    border-radius: 22px;
    padding: 24px;
  }

  .hero-main h1 {
    font-size: 32px;
  }

  .hero-aside {
    padding: 20px;
  }
}

@media (max-width: 640px) {
  .portal-hero {
    padding: 14px 0 6px;
  }

  .hero-shell {
    gap: 16px;
    padding: 18px 16px;
    border-radius: 18px;
  }

  .hero-main h1 {
    font-size: 26px;
    line-height: 1.15;
  }

  .hero-description,
  .aside-description {
    margin-top: 14px;
    font-size: 14px;
    line-height: 1.65;
  }

  .hero-actions {
    gap: 10px;
    margin-top: 18px;
  }

  .hero-metrics {
    grid-template-columns: 1fr;
    gap: 10px;
    margin-top: 20px;
  }

  .metric-card {
    min-height: 0;
    padding: 14px;
    border-radius: 16px;
  }

  .metric-value {
    font-size: 24px;
  }

  .metric-detail {
    font-size: 12px;
    line-height: 1.6;
  }

  .hero-aside {
    gap: 12px;
    padding: 16px;
    border-radius: 18px;
  }

  .hero-aside h2 {
    font-size: 22px;
  }
}
</style>
