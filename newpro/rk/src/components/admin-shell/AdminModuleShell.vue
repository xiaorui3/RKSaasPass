<template>
  <div class="admin-module-shell">
    <section class="module-hero">
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

    <section v-if="statItems.length" class="module-stats">
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

    <section v-if="hasFilters" class="module-filters">
      <slot name="filters" />
    </section>

    <section class="module-body" :class="{ 'has-aside': hasAside }">
      <div class="module-main">
        <slot />
      </div>
      <aside v-if="hasAside" class="module-aside">
        <slot name="aside" />
      </aside>
    </section>
  </div>
</template>

<script setup>
import { computed, useSlots } from 'vue'

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
.admin-module-shell {
  display: grid;
  gap: 18px;
}

.module-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  padding: 24px 26px;
  border: 1px solid rgba(20, 84, 123, 0.12);
  border-radius: 20px;
  background:
    radial-gradient(circle at top right, rgba(39, 117, 202, 0.14), transparent 36%),
    linear-gradient(135deg, #fbfdff 0%, #f4f8fc 55%, #eef3f8 100%);
  box-shadow: 0 16px 40px rgba(15, 53, 87, 0.08);
}

.hero-copy {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.hero-eyebrow {
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
  color: #245c90;
}

.hero-title {
  margin: 0;
  font-size: 28px;
  line-height: 1.1;
  color: #102a43;
}

.hero-description {
  margin: 0;
  max-width: 760px;
  color: #52667a;
  line-height: 1.6;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.hero-meta-item {
  padding: 7px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.82);
  border: 1px solid rgba(27, 76, 120, 0.12);
  color: #31506a;
  font-size: 12px;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
}

.module-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.stat-card {
  display: grid;
  gap: 8px;
  padding: 18px;
  border-radius: 18px;
  border: 1px solid #e4ebf2;
  background: #ffffff;
  box-shadow: 0 14px 32px rgba(15, 53, 87, 0.06);
}

.stat-card.is-warning {
  background: linear-gradient(180deg, #fffaf0 0%, #ffffff 100%);
}

.stat-card.is-success {
  background: linear-gradient(180deg, #f3fbf6 0%, #ffffff 100%);
}

.stat-card.is-danger {
  background: linear-gradient(180deg, #fff6f4 0%, #ffffff 100%);
}

.stat-card.is-info {
  background: linear-gradient(180deg, #f4f9ff 0%, #ffffff 100%);
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

.module-filters {
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid #e6edf5;
  background: #ffffff;
  box-shadow: 0 10px 30px rgba(15, 53, 87, 0.05);
}

.module-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 18px;
}

.module-body.has-aside {
  grid-template-columns: minmax(0, 1fr) 300px;
  align-items: start;
}

.module-main,
.module-aside {
  display: grid;
  gap: 18px;
  min-width: 0;
}

@media (max-width: 1200px) {
  .module-body.has-aside {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 960px) {
  .module-hero {
    padding: 20px;
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .module-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .admin-module-shell {
    gap: 14px;
  }

  .module-hero {
    gap: 14px;
    padding: 18px 16px;
    border-radius: 18px;
  }

  .module-stats {
    grid-template-columns: minmax(0, 1fr);
  }

  .hero-title {
    font-size: 22px;
    line-height: 1.18;
  }

  .hero-description {
    font-size: 13px;
    line-height: 1.58;
  }

  .hero-meta {
    gap: 8px;
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

  .module-filters {
    padding: 14px;
    border-radius: 16px;
  }
}
</style>
