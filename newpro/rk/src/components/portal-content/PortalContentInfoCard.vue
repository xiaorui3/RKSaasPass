<template>
  <article class="portal-content-info-card" :class="`tone-${tone}`">
    <div class="info-head">
      <p v-if="eyebrow" class="info-eyebrow">{{ eyebrow }}</p>
      <h3>{{ title }}</h3>
      <p v-if="description" class="info-description">{{ description }}</p>
    </div>

    <div class="info-body">
      <slot />
    </div>
  </article>
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
  tone: {
    type: String,
    default: 'light'
  }
})
</script>

<style lang="scss" scoped>
@use '../../styles/variables.scss' as *;

.portal-content-info-card {
  display: grid;
  gap: 16px;
  padding: 20px;
  border-radius: 22px;
  border: 1px solid rgba(215, 208, 198, 0.88);
  box-shadow: $shadow-sm;
}

.tone-light {
  background: rgba(255, 255, 255, 0.92);
}

.tone-soft {
  background: linear-gradient(180deg, rgba(246, 240, 230, 0.98) 0%, rgba(255, 255, 255, 0.95) 100%);
}

.tone-contrast {
  background: linear-gradient(180deg, rgba(31, 58, 52, 0.98) 0%, rgba(22, 42, 37, 0.96) 100%);
  border-color: rgba(31, 58, 52, 0.72);
  color: $text-white;
}

.info-head h3 {
  margin: 0;
  font-size: 20px;
  line-height: 1.2;
  color: inherit;
}

.info-eyebrow {
  margin: 0 0 8px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  color: inherit;
  opacity: 0.72;
}

.info-description {
  margin: 10px 0 0;
  font-size: 14px;
  line-height: 1.7;
  color: inherit;
  opacity: 0.84;
}

.info-body {
  display: grid;
  gap: 12px;
}

@media (max-width: $breakpoint-md) {
  .portal-content-info-card {
    padding: 18px;
    border-radius: 18px;
  }
}

@media (max-width: 640px) {
  .portal-content-info-card {
    gap: 14px;
    padding: 14px;
    border-radius: 16px;
  }

  .info-head h3 {
    font-size: 18px;
    line-height: 1.3;
  }

  .info-description {
    margin-top: 8px;
    font-size: 13px;
    line-height: 1.6;
  }
}
</style>
