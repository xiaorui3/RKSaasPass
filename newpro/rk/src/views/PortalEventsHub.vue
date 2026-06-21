<template>
  <div class="portal-hub">
    <section class="portal-hub-hero">
      <div>
        <span class="portal-hub-kicker">Events Hub</span>
        <h1>活动赛事</h1>
        <p>把活动、比赛和日历入口收拢到一个页面，报名、详情、评论等原流程继续走原页面。</p>
      </div>
      <router-link v-if="portalSettings.showActivities !== false" class="portal-hub-action" to="/calendar">活动日历</router-link>
    </section>

    <section class="portal-hub-grid">
      <article v-if="portalSettings.showActivities !== false" class="portal-hub-card portal-hub-card-wide">
        <div class="portal-hub-card-head">
          <h2>近期活动</h2>
          <router-link to="/activities">全部活动</router-link>
        </div>
        <div v-if="loading.activities" class="portal-hub-empty">正在加载</div>
        <router-link
          v-for="item in activities"
          :key="`activity-${item.id}`"
          class="portal-event-row"
          :to="`/activities/${item.id}`"
        >
          <div>
            <strong>{{ item.title || item.activityName || '未命名活动' }}</strong>
            <span>{{ item.location || item.activityLocation || '地点待定' }}</span>
          </div>
          <em>{{ formatDate(item.startTime || item.activityStartTime || item.createTime) }}</em>
        </router-link>
        <div v-if="!loading.activities && activities.length === 0" class="portal-hub-empty">暂无活动</div>
      </article>

      <article v-if="portalSettings.showCompetitions !== false" class="portal-hub-card portal-hub-card-wide">
        <div class="portal-hub-card-head">
          <h2>比赛报名</h2>
          <router-link to="/competition">全部比赛</router-link>
        </div>
        <div v-if="loading.competitions" class="portal-hub-empty">正在加载</div>
        <router-link
          v-for="item in competitions"
          :key="`competition-${item.id}`"
          class="portal-event-row"
          :to="`/competition/${item.id}`"
        >
          <div>
            <strong>{{ item.title || item.name || '未命名比赛' }}</strong>
            <span>{{ item.level || item.competitionType || '公开赛道' }}</span>
          </div>
          <em>{{ formatDate(item.competitionStartTime || item.startTime || item.createTime) }}</em>
        </router-link>
        <div v-if="!loading.competitions && competitions.length === 0" class="portal-hub-empty">暂无比赛</div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { getActivityPage } from '@/api/activity'
import { getPublishedCompetitions } from '@/api/competition'
import { useTenantSelfServiceStore } from '@/stores/tenantSelfService'

const tenantSelfServiceStore = useTenantSelfServiceStore()
const portalSettings = computed(() => tenantSelfServiceStore.portalSettings)
const activities = ref([])
const competitions = ref([])
const loading = reactive({
  activities: true,
  competitions: true
})

function pickRecords(data) {
  if (Array.isArray(data)) return data
  return data?.records || data?.list || data?.rows || []
}

function formatDate(value) {
  if (!value) return '时间待定'
  return String(value).slice(0, 10)
}

async function loadEvents() {
  loading.activities = true
  loading.competitions = true
  try {
    await tenantSelfServiceStore.loadPublicConfig()
    const requests = [
      portalSettings.value.showActivities !== false
        ? getActivityPage({ page: 1, size: 8 })
        : Promise.resolve({ data: [] }),
      portalSettings.value.showCompetitions !== false
        ? getPublishedCompetitions('competition_start', 'asc')
        : Promise.resolve({ data: [] })
    ]
    const [activityRes, competitionRes] = await Promise.allSettled(requests)
    activities.value = activityRes.status === 'fulfilled' ? pickRecords(activityRes.value.data).slice(0, 8) : []
    competitions.value = competitionRes.status === 'fulfilled' ? pickRecords(competitionRes.value.data).slice(0, 8) : []
  } finally {
    loading.activities = false
    loading.competitions = false
  }
}

onMounted(loadEvents)
</script>

<style lang="scss" scoped>
.portal-hub {
  display: grid;
  gap: 22px;
}

.portal-hub-hero,
.portal-hub-card {
  border: 1px solid rgba(194, 214, 242, 0.86);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 14px 36px rgba(46, 92, 150, 0.08);
}

.portal-hub-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 28px;

  h1 {
    margin: 6px 0 10px;
    color: #10284f;
    font-size: 30px;
  }

  p {
    margin: 0;
    color: #62738f;
  }
}

.portal-hub-kicker {
  color: #1473ff;
  font-size: 13px;
  font-weight: 900;
}

.portal-hub-action {
  flex: 0 0 auto;
  padding: 12px 18px;
  border-radius: 8px;
  background: #1473ff;
  color: #fff;
  font-weight: 900;
  text-decoration: none;
}

.portal-hub-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.portal-hub-card {
  padding: 20px;
}

.portal-hub-card-head,
.portal-event-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.portal-hub-card-head {
  margin-bottom: 14px;

  h2 {
    margin: 0;
    color: #162c52;
    font-size: 18px;
  }

  a {
    color: #1473ff;
    font-weight: 800;
    text-decoration: none;
  }
}

.portal-event-row {
  padding: 14px 0;
  border-top: 1px solid #edf2fa;
  color: #213857;
  text-decoration: none;

  div {
    min-width: 0;
    display: grid;
    gap: 5px;
  }

  strong {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  span,
  em {
    color: #8492a8;
    font-size: 13px;
    font-style: normal;
  }
}

.portal-hub-empty {
  padding: 18px 0;
  color: #8492a8;
}

@media (max-width: 760px) {
  .portal-hub-hero,
  .portal-event-row {
    display: grid;
  }

  .portal-hub-grid {
    grid-template-columns: 1fr;
  }
}
</style>
