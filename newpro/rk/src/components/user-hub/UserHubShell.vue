<template>
  <div class="user-hub-shell">
    <section class="shell-hero">
      <div class="hero-copy">
        <span v-if="eyebrow" class="hero-eyebrow">{{ eyebrow }}</span>
        <h1 class="hero-title">{{ title }}</h1>
        <p v-if="description" class="hero-description">{{ description }}</p>
        <div v-if="metaItems.length" class="hero-meta">
          <span v-for="item in metaItems" :key="item" class="hero-meta-item">{{ item }}</span>
        </div>
      </div>
      <div v-if="hasActions" class="hero-actions">
        <slot name="actions" />
      </div>
    </section>

    <section v-if="statItems.length" class="shell-stats">
      <article
        v-for="item in statItems"
        :key="item.label"
        class="stat-card"
        :class="item.tone ? `is-${item.tone}` : ''"
      >
        <span class="stat-label">{{ item.label }}</span>
        <strong class="stat-value">{{ item.value }}</strong>
        <span v-if="item.helper" class="stat-helper">{{ item.helper }}</span>
      </article>
    </section>

    <section v-if="hasFilters" class="shell-filters">
      <slot name="filters" />
    </section>

    <section class="shell-body" :class="{ 'has-aside': hasAside }">
      <div class="shell-main">
        <slot />
      </div>
      <aside v-if="hasAside" class="shell-aside">
        <slot name="aside" />
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

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
  statItems: {
    type: Array,
    default: () => []
  }
})

const slots = useSlots()

const hasActions = computed(() => !!slots.actions)
const hasFilters = computed(() => !!slots.filters)
const hasAside = computed(() => !!slots.aside)
</script>

<style lang="scss" scoped>
.user-hub-shell {
  display: grid;
  gap: 18px;
}

.shell-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 24px;
  padding: 28px 30px;
  border: 1px solid rgba(17, 57, 92, 0.12);
  border-radius: 24px;
  background:
    linear-gradient(135deg, rgba(239, 246, 255, 0.96) 0%, rgba(247, 250, 252, 0.98) 52%, rgba(255, 255, 255, 1) 100%),
    radial-gradient(circle at top right, rgba(37, 99, 235, 0.16), transparent 34%);
  box-shadow: 0 20px 48px rgba(15, 53, 87, 0.08);
}

.hero-copy {
  display: grid;
  gap: 10px;
  min-width: 0;
}

.hero-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: #24548b;
}

.hero-title {
  margin: 0;
  font-size: 30px;
  line-height: 1.08;
  color: #102a43;
}

.hero-description {
  margin: 0;
  max-width: 760px;
  color: #4f6477;
  line-height: 1.7;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-meta-item {
  padding: 7px 12px;
  border-radius: 999px;
  border: 1px solid rgba(18, 71, 117, 0.12);
  background: rgba(255, 255, 255, 0.84);
  color: #30506c;
  font-size: 12px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.shell-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #e5edf5;
  background: #ffffff;
  box-shadow: 0 14px 34px rgba(15, 53, 87, 0.06);
}

.stat-card.is-info {
  background: linear-gradient(180deg, #f4f9ff 0%, #ffffff 100%);
}

.stat-card.is-success {
  background: linear-gradient(180deg, #f3fbf6 0%, #ffffff 100%);
}

.stat-card.is-warning {
  background: linear-gradient(180deg, #fff9ef 0%, #ffffff 100%);
}

.stat-card.is-danger {
  background: linear-gradient(180deg, #fff6f4 0%, #ffffff 100%);
}

.stat-label {
  color: #6b7d8f;
  font-size: 13px;
}

.stat-value {
  font-size: 30px;
  line-height: 1;
  color: #102a43;
}

.stat-helper {
  color: #7d8d9d;
  font-size: 12px;
}

.shell-filters {
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid #e6edf5;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(15, 53, 87, 0.05);
}

.shell-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
}

.shell-body.has-aside {
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: start;
}

.shell-main,
.shell-aside {
  display: grid;
  gap: 18px;
  min-width: 0;
}

@media (max-width: 1200px) {
  .shell-body.has-aside {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 960px) {
  .shell-hero {
    flex-direction: column;
    padding: 22px;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .shell-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .user-hub-shell {
    gap: 14px;
  }

  .shell-hero {
    gap: 16px;
    padding: 18px 16px;
    border-radius: 18px;
  }

  .hero-copy {
    gap: 8px;
  }

  .shell-stats {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-meta {
    gap: 8px;
  }

  .hero-title {
    font-size: 22px;
    line-height: 1.18;
  }

  .hero-description {
    font-size: 13px;
    line-height: 1.6;
  }

  .hero-meta-item {
    padding: 6px 10px;
    font-size: 11px;
  }

  .stat-card {
    padding: 14px;
    border-radius: 16px;
  }

  .stat-value {
    font-size: 24px;
  }

  .shell-filters {
    padding: 14px;
    border-radius: 16px;
  }
}
</style>
