<template>
  <div class="about-page design-about-reference-page" data-testid="portal-about-page">
    <section class="about-hero design-about-hero">
      <div class="container about-hero__inner">
        <div class="hero-copy">
          <p class="hero-eyebrow">关于我们</p>
          <h1>社团状况</h1>
          <p>{{ config.pageDescription }}</p>
          <div class="hero-actions">
            <a class="hero-link primary" :href="`mailto:${config.contactEmail}`">联系社团</a>
            <router-link class="hero-link secondary" to="/history">查看历程</router-link>
          </div>
        </div>

        <div class="hero-stats design-about-stats-grid">
          <article v-for="metric in heroMetrics" :key="metric.label">
            <strong>{{ metric.value }}</strong>
            <span>{{ metric.label }}</span>
          </article>
        </div>
      </div>
    </section>

    <main class="container about-shell">
      <section class="surface-panel intro-panel">
        <div class="section-heading">
          <p>组织概况</p>
          <h2>{{ config.pageTitle || '社团状况' }}</h2>
          <span>概况内容由当前租户配置驱动，页面只负责统一呈现。</span>
        </div>

        <div class="narrative-copy">
          <p v-for="(paragraph, index) in config.introParagraphs" :key="`intro-${index}`">
            {{ paragraph }}
          </p>
        </div>
      </section>

      <aside class="surface-panel contact-panel">
        <div class="section-heading compact">
          <p>联系渠道</p>
          <h2>对外联系信息</h2>
          <span>面向意向成员、合作伙伴和活动参与者的统一联系入口。</span>
        </div>

        <div class="contact-list">
          <article>
            <span>邮箱</span>
            <strong>{{ config.contactEmail }}</strong>
          </article>
          <article>
            <span>地址</span>
            <strong>{{ config.contactAddress }}</strong>
          </article>
        </div>

        <router-link class="contact-route" to="/contact">打开联系窗口</router-link>
      </aside>

      <section class="surface-panel mission-panel">
        <div class="section-heading">
          <p>能力模块</p>
          <h2>{{ config.missionTitle }}</h2>
          <span>通过结构化卡片梳理社团方向，让对外展示更清晰。</span>
        </div>

        <div class="mission-grid">
          <article v-for="(card, index) in missionCards" :key="card.title" class="mission-card">
            <span class="mission-index">{{ index + 1 }}</span>
            <h3>{{ card.title }}</h3>
            <p>{{ card.description }}</p>
          </article>
        </div>
      </section>

      <section class="surface-panel history-panel" data-testid="portal-about-history">
        <div class="section-heading">
          <p>社团历程</p>
          <h2>里程碑时间轴</h2>
          <span>按时间顺序展示组织延续、项目成长和当前运行阶段。</span>
        </div>

        <div class="history-list">
          <article v-for="item in orderedHistory" :key="`${item.year}-${item.title}`" class="history-card">
            <div class="history-year">{{ item.year }}</div>
            <div class="history-copy">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </div>
          </article>
        </div>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { useRoute } from 'vue-router'
import { getPublicClubProfileConfig } from '@/api/club-profile'

const route = useRoute()

const defaultConfig = {
  pageTitle: '社团状况',
  pageDescription: '通过一页结构化门户了解社团方向、运行模式与长期成果。',
  introTitle: '概况总览',
  introParagraphs: [
    '社团门户将组织概况、项目实践、公告通知与公开内容整合在同一套前台体验中。',
    '本页按租户独立配置，并以清晰的信息层级面向外部访问者展示。'
  ],
  history: [
    {
      year: '2022',
      title: '完成基础建设',
      description: '建立社团初始工作流、运行原则与成员协作方式。'
    },
    {
      year: '2024',
      title: '内容与活动扩展',
      description: '围绕新闻、活动与作品输出形成更稳定的公开传播和内部协作模块。'
    },
    {
      year: '2026',
      title: '门户体验升级',
      description: '对外门户升级为更统一、更具组织感的展示界面。'
    }
  ],
  missionTitle: '运行重点',
  missionCards: [
    {
      title: '项目交付',
      description: '把想法推进为真实项目，用稳定协作形成可见成果。'
    },
    {
      title: '活动协同',
      description: '让活动、审核与发布流程在组织内部保持一致。'
    },
    {
      title: '对外传播',
      description: '把社团故事、里程碑与成果沉淀为长期可复用的对外资源。'
    }
  ],
  contactEmail: 'contact@rk-web.org',
  contactAddress: 'Heihe University'
}

const config = reactive({
  ...defaultConfig
})

function toNonEmptyArray(value, fallback) {
  if (!Array.isArray(value)) {
    return fallback
  }

  const normalized = value.filter(Boolean)
  return normalized.length ? normalized : fallback
}

function applyConfig(data) {
  config.pageTitle = data.pageTitle || defaultConfig.pageTitle
  config.pageDescription = data.pageDescription || defaultConfig.pageDescription
  config.introTitle = data.introTitle || defaultConfig.introTitle
  config.introParagraphs = toNonEmptyArray(data.introParagraphs, defaultConfig.introParagraphs)
  config.history = toNonEmptyArray(data.history, defaultConfig.history)
  config.missionTitle = data.missionTitle || defaultConfig.missionTitle
  config.missionCards = toNonEmptyArray(data.missionCards, defaultConfig.missionCards)
  config.contactEmail = data.contactEmail || defaultConfig.contactEmail
  config.contactAddress = data.contactAddress || defaultConfig.contactAddress
}

async function fetchConfig() {
  try {
    const tenantId = route.query.tenantId || localStorage.getItem('tenantId') || 1
    const res = await getPublicClubProfileConfig(tenantId)
    if (res.code === 200 && res.data) {
      applyConfig(res.data)
    }
  } catch (error) {
    console.error('获取社团概况配置失败:', error)
  }
}

const missionCards = computed(() => {
  return Array.isArray(config.missionCards) ? config.missionCards : defaultConfig.missionCards
})

const orderedHistory = computed(() => {
  const source = Array.isArray(config.history) ? config.history : defaultConfig.history
  return [...source]
    .filter((item) => item && (item.year || item.title || item.description))
    .sort((left, right) => String(left.year || '').localeCompare(String(right.year || '')))
})

const heroMetrics = computed(() => {
  return [
    {
      label: '历程节点',
      value: String(orderedHistory.value.length)
    },
    {
      label: '重点模块',
      value: String(missionCards.value.length)
    },
    {
      label: '租户范围',
      value: route.query.tenantId ? String(route.query.tenantId) : '默认'
    }
  ]
})

onMounted(() => {
  fetchConfig()
})
</script>

<style lang="scss" scoped>
@use '../styles/variables.scss' as *;

.about-page {
  min-height: 100vh;
  padding-bottom: 64px;
  background:
    linear-gradient(180deg, rgba(47, 111, 237, 0.06) 0%, rgba(247, 250, 255, 0.92) 28%, #fff 100%);
}

.about-hero {
  padding: 28px 0 0;
}

.about-hero__inner {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, 0.7fr);
  gap: 32px;
  align-items: end;
  padding: 52px 64px;
  border-radius: 28px;
  color: #fff;
  background:
    radial-gradient(circle at 76% 26%, rgba(255, 255, 255, 0.28), transparent 22%),
    linear-gradient(135deg, #3567ff 0%, #1d86ff 100%);
  box-shadow: 0 24px 60px rgba(39, 105, 255, 0.22);
}

.hero-eyebrow {
  margin: 0 0 12px;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  opacity: 0.82;
}

.hero-copy h1 {
  margin: 0;
  font-size: clamp(34px, 4vw, 56px);
  line-height: 1.08;
}

.hero-copy p:not(.hero-eyebrow) {
  max-width: 680px;
  margin: 18px 0 0;
  font-size: 17px;
  line-height: 1.8;
  opacity: 0.86;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 26px;
}

.hero-link,
.contact-route {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 44px;
  padding: 0 18px;
  border-radius: 999px;
  font-size: 14px;
  font-weight: 700;
  text-decoration: none;
}

.hero-link.primary {
  background: #fff;
  color: #2368f5;
}

.hero-link.secondary {
  border: 1px solid rgba(255, 255, 255, 0.34);
  color: #fff;
}

.hero-stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.hero-stats article {
  min-height: 108px;
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.2);
}

.hero-stats strong,
.hero-stats span {
  display: block;
}

.hero-stats strong {
  font-size: 30px;
  line-height: 1;
}

.hero-stats span {
  margin-top: 10px;
  font-size: 13px;
  opacity: 0.78;
}

.about-shell {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(280px, 0.45fr);
  gap: 24px;
  padding-top: 26px;
}

.surface-panel {
  min-width: 0;
  padding: 26px;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid rgba(226, 232, 240, 0.9);
  box-shadow: 0 18px 44px rgba(31, 58, 120, 0.08);
}

.mission-panel,
.history-panel {
  grid-column: 1 / -1;
}

.section-heading {
  margin-bottom: 22px;
}

.section-heading.compact {
  margin-bottom: 18px;
}

.section-heading p {
  margin: 0 0 10px;
  color: #2f6fed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
}

.section-heading h2 {
  margin: 0;
  color: $text-primary;
  font-size: 26px;
  line-height: 1.25;
}

.section-heading span {
  display: block;
  margin-top: 10px;
  color: $text-secondary;
  line-height: 1.75;
}

.narrative-copy {
  display: grid;
  gap: 14px;
}

.narrative-copy p,
.mission-card p,
.history-copy p {
  margin: 0;
  color: $text-secondary;
  line-height: 1.85;
}

.contact-panel {
  align-self: start;
}

.contact-list {
  display: grid;
  gap: 12px;
}

.contact-list article {
  display: grid;
  gap: 8px;
  padding: 16px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.82);
}

.contact-list span {
  color: #2f6fed;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.12em;
}

.contact-list strong {
  word-break: break-word;
  color: $text-primary;
  font-size: 15px;
}

.contact-route {
  width: 100%;
  margin-top: 16px;
  background: #2f6fed;
  color: #fff;
}

.mission-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.mission-card {
  display: grid;
  gap: 12px;
  min-height: 210px;
  padding: 20px;
  border-radius: 18px;
  background: #f8fbff;
  border: 1px solid rgba(226, 232, 240, 0.82);
}

.mission-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 38px;
  height: 38px;
  border-radius: 50%;
  background: #eff6ff;
  color: #2f6fed;
  font-weight: 800;
}

.mission-card h3,
.history-copy h3 {
  margin: 0;
  color: $text-primary;
  font-size: 19px;
  line-height: 1.35;
}

.history-list {
  display: grid;
  gap: 16px;
}

.history-card {
  display: grid;
  grid-template-columns: 132px minmax(0, 1fr);
  gap: 20px;
  align-items: start;
  padding: 20px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(226, 232, 240, 0.88);
}

.history-year {
  width: fit-content;
  padding: 8px 16px;
  border-radius: 999px;
  background: #eff6ff;
  color: #2f6fed;
  font-size: 22px;
  font-weight: 800;
  line-height: 1;
}

@media (max-width: $breakpoint-lg) {
  .about-hero__inner,
  .about-shell,
  .mission-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: $breakpoint-md) {
  .about-hero__inner {
    margin: 0 16px;
    padding: 32px 24px;
  }

  .hero-stats,
  .history-card {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .about-page {
    padding-bottom: 36px;
  }

  .about-hero {
    padding-top: 18px;
  }

  .about-hero__inner {
    margin: 0 12px;
    padding: 24px 18px;
    border-radius: 20px;
  }

  .hero-copy h1 {
    font-size: 28px;
  }

  .hero-copy p:not(.hero-eyebrow) {
    font-size: 14px;
    line-height: 1.68;
  }

  .hero-actions {
    margin-top: 20px;
  }

  .hero-link,
  .contact-route {
    min-height: 40px;
    padding: 0 14px;
    font-size: 13px;
  }

  .about-shell {
    gap: 16px;
    padding-top: 18px;
  }

  .surface-panel {
    padding: 18px;
    border-radius: 18px;
  }

  .section-heading h2 {
    font-size: 21px;
  }

  .mission-card {
    min-height: 0;
    padding: 16px;
  }

  .history-card {
    gap: 12px;
    padding: 16px;
  }

  .history-year {
    font-size: 18px;
  }
}
</style>
