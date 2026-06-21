<template>
  <article class="portal-content-feature-card" :class="featureClasses">
    <div v-if="$slots.media" class="feature-media">
      <slot name="media" />
    </div>

    <div class="feature-main">
      <p v-if="eyebrow" class="feature-eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p v-if="description" class="feature-description">{{ description }}</p>

      <div v-if="$slots.meta" class="feature-meta">
        <slot name="meta" />
      </div>

      <div v-if="$slots.default" class="feature-body">
        <slot />
      </div>

      <div v-if="$slots.actions" class="feature-actions">
        <slot name="actions" />
      </div>
    </div>

    <aside v-if="$slots.aside" class="feature-aside">
      <slot name="aside" />
    </aside>
  </article>
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
  mediaPosition: {
    type: String,
    default: 'start'
  }
})

const featureClasses = computed(() => {
  return {
    'media-end': props.mediaPosition === 'end'
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-content-feature-card {
  display: grid;
  grid-template-columns: minmax(280px, 1fr) minmax(0, 1.08fr) minmax(240px, 0.78fr);
  gap: 22px;
  padding: 22px;
  border-radius: 28px;
  border: 1px solid rgba(215, 208, 198, 0.86);
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.94) 0%, rgba(249, 244, 236, 0.98) 100%);
  box-shadow: $shadow-sm;
}

.portal-content-feature-card.media-end {
  grid-template-columns: minmax(0, 1.08fr) minmax(280px, 1fr) minmax(240px, 0.78fr);
}

.portal-content-feature-card.media-end .feature-media {
  order: 2;
}

.portal-content-feature-card.media-end .feature-main {
  order: 1;
}

.portal-content-feature-card.media-end .feature-aside {
  order: 3;
}

.feature-media {
  min-width: 0;
  min-height: 280px;
  border-radius: 22px;
  overflow: hidden;
  background: $bg-light;
}

.feature-main {
  min-width: 0;
  display: grid;
  align-content: center;
  gap: 16px;
}

.feature-eyebrow {
  margin: 0;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: $secondary-color;
}

.feature-main h3 {
  margin: 0;
  font-size: clamp(28px, 3vw, 40px);
  line-height: 1.1;
  color: $text-primary;
  overflow-wrap: anywhere;
}

.feature-description {
  margin: 0;
  font-size: 15px;
  line-height: 1.82;
  color: $text-secondary;
}

.feature-meta,
.feature-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.feature-body {
  display: grid;
  gap: 14px;
  color: $text-secondary;
}

.feature-aside {
  min-width: 0;
  display: grid;
  align-content: start;
  gap: 14px;
  padding: 20px;
  border-radius: 22px;
  background: linear-gradient(180deg, rgba(31, 58, 52, 0.98) 0%, rgba(22, 42, 37, 0.96) 100%);
  color: $text-white;
  box-shadow: $shadow-base;
}

@media (max-width: $breakpoint-lg) {
  .portal-content-feature-card,
  .portal-content-feature-card.media-end {
    grid-template-columns: 1fr;
  }

  .portal-content-feature-card.media-end .feature-media,
  .portal-content-feature-card.media-end .feature-main,
  .portal-content-feature-card.media-end .feature-aside {
    order: initial;
  }
}

@media (max-width: $breakpoint-md) {
  .portal-content-feature-card {
    padding: 18px;
    border-radius: 22px;
  }

  .feature-media {
    min-height: 220px;
  }
}
</style>
