<template>
  <section class="portal-event-hero">
    <div class="container hero-shell">
      <div class="hero-copy">
        <p v-if="eyebrow" class="hero-eyebrow">{{ eyebrow }}</p>
        <div v-if="badges.length" class="hero-badges">
          <span
            v-for="badge in badges"
            :key="`${badge.label}-${badge.tone || 'default'}`"
            class="hero-badge"
            :class="badge.tone ? `tone-${badge.tone}` : ''"
          >
            {{ badge.label }}
          </span>
        </div>
        <h1 class="hero-title">{{ title }}</h1>
        <p v-if="description" class="hero-description">{{ description }}</p>
        <div v-if="metaItems.length" class="hero-meta">
          <span v-for="item in metaItems" :key="item" class="hero-meta-item">{{ item }}</span>
        </div>
        <div v-if="stats.length" class="hero-stats">
          <article v-for="stat in stats" :key="stat.label" class="hero-stat">
            <p class="hero-stat-value">{{ stat.value }}</p>
            <p class="hero-stat-label">{{ stat.label }}</p>
            <p v-if="stat.hint" class="hero-stat-hint">{{ stat.hint }}</p>
          </article>
        </div>
        <div v-if="$slots.actions" class="hero-actions">
          <slot name="actions" />
        </div>
      </div>

      <aside v-if="$slots.aside" class="hero-aside">
        <slot name="aside" />
      </aside>
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
  metaItems: {
    type: Array,
    default: () => []
  },
  stats: {
    type: Array,
    default: () => []
  },
  badges: {
    type: Array,
    default: () => []
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-event-hero {
  position: relative;
  overflow: hidden;
  padding: 36px 0 28px;
  background:
    radial-gradient(circle at top left, rgba($accent-color, 0.18), transparent 36%),
    radial-gradient(circle at bottom right, rgba(#ffffff, 0.18), transparent 28%),
    linear-gradient(135deg, $primary-dark 0%, $primary-color 50%, $primary-light 100%);
  color: $text-white;
}

.portal-event-hero::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.06) 1px, transparent 1px);
  background-size: 72px 72px;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.95), transparent);
  pointer-events: none;
}

.hero-shell {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(280px, 0.9fr);
  gap: 28px;
  align-items: stretch;
}

.hero-copy {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.hero-eyebrow {
  margin: 0;
  font-size: 12px;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: rgba(255, 255, 255, 0.74);
}

.hero-badges {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-badge {
  display: inline-flex;
  align-items: center;
  border-radius: $border-radius-full;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255, 255, 255, 0.12);
  border: 1px solid rgba(255, 255, 255, 0.18);
}

.hero-badge.tone-warm {
  background: rgba($accent-color, 0.22);
}

.hero-badge.tone-success {
  background: rgba($success-color, 0.2);
}

.hero-badge.tone-info {
  background: rgba($info-color, 0.25);
}

.hero-title {
  margin: 0;
  font-size: clamp(2rem, 3vw, 3.3rem);
  line-height: 1.12;
}

.hero-description {
  max-width: 760px;
  margin: 0;
  font-size: 16px;
  line-height: 1.8;
  color: rgba(255, 255, 255, 0.84);
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-meta-item {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 12px;
  border-radius: $border-radius-full;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.85);
  background: rgba(12, 20, 18, 0.24);
  border: 1px solid rgba(255, 255, 255, 0.12);
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 16px;
}

.hero-stat {
  padding: 18px 18px 16px;
  border-radius: $border-radius-lg;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  box-shadow: 0 16px 40px rgba(8, 14, 13, 0.16);
}

.hero-stat-value,
.hero-stat-label,
.hero-stat-hint {
  margin: 0;
}

.hero-stat-value {
  font-size: 28px;
  font-weight: 700;
}

.hero-stat-label {
  margin-top: 8px;
  font-size: 13px;
  color: rgba(255, 255, 255, 0.82);
}

.hero-stat-hint {
  margin-top: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.62);
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.hero-aside {
  display: flex;
  min-height: 100%;
}

@media (max-width: $breakpoint-lg) {
  .hero-shell {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .portal-event-hero {
    padding: 28px 0 24px;
  }

  .hero-copy {
    gap: 16px;
  }

  .hero-stats {
    grid-template-columns: 1fr;
  }
}
</style>
