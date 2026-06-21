<template>
  <div class="calendar-page">
    <div class="page-hero">
      <div class="container">
        <h1>{{ t('pages.activityCalendar.title') }}</h1>
        <p>{{ t('pages.activityCalendar.subtitle') }}</p>
      </div>
    </div>
    <div class="container page-body">
      <el-calendar v-model="currentDate">
        <template #date-cell="{ data }">
          <div class="calendar-cell" :class="{ 'has-events': getEventsForDate(data.day).length > 0 }">
            <span class="cell-day">{{ data.day.split('-')[2] }}</span>
            <div class="cell-events">
              <div
                v-for="evt in getEventsForDate(data.day).slice(0, 2)"
                :key="`${evt.type}-${evt.id}`"
                class="cell-event"
                :class="`cell-event--${evt.type}`"
                @click.stop="goEvent(evt)"
              >
                <span class="event-type">{{ evt.typeLabel }}</span>
                {{ evt.title }}
              </div>
              <div v-if="getEventsForDate(data.day).length > 2" class="cell-more">
                +{{ getEventsForDate(data.day).length - 2 }} {{ t('pages.activityCalendar.more') }}
              </div>
            </div>
          </div>
        </template>
      </el-calendar>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { getActivityList } from '@/api/activity'
import { getPublishedCompetitions } from '@/api/competition'

const { t } = useI18n()
const router = useRouter()
const currentDate = ref(new Date())
const events = ref([])

function getEventsForDate(dayStr) {
  return events.value.filter((activity) => {
    const start = (activity.startTime || '').substring(0, 10)
    const end = (activity.endTime || activity.startTime || '').substring(0, 10)
    return dayStr >= start && dayStr <= end
  })
}

function goEvent(evt) {
  if (!evt?.id) return
  router.push(evt.type === 'competition' ? `/competition/${evt.id}` : `/activities/${evt.id}`)
}

function normalizeActivities(raw) {
  return raw
    .map((activity) => ({
      id: activity.id,
      type: 'activity',
      typeLabel: t('pages.activityCalendar.activityLabel'),
      title: activity.activityName || activity.title,
      startTime: activity.startTime,
      endTime: activity.endTime
    }))
    .filter((item) => item.id && item.title && item.startTime)
}

function normalizeCompetitions(raw) {
  return raw
    .map((competition) => ({
      id: competition.id,
      type: 'competition',
      typeLabel: t('pages.activityCalendar.competitionLabel'),
      title: competition.title,
      startTime: competition.competitionStart || competition.registrationStart,
      endTime: competition.competitionEnd || competition.registrationEnd || competition.competitionStart || competition.registrationStart
    }))
    .filter((item) => item.id && item.title && item.startTime)
}

onMounted(async () => {
  try {
    const [activityRes, competitionRes] = await Promise.all([
      getActivityList(),
      getPublishedCompetitions('competition_start', 'asc')
    ])
    const activityRaw = Array.isArray(activityRes.data) ? activityRes.data : activityRes.data?.records || []
    const competitionRaw = Array.isArray(competitionRes.data) ? competitionRes.data : competitionRes.data?.records || []
    events.value = [
      ...normalizeActivities(activityRaw),
      ...normalizeCompetitions(competitionRaw)
    ].sort((a, b) => String(a.startTime).localeCompare(String(b.startTime)))
  } catch {
    // keep calendar usable without noisy toast
  }
})
</script>

<style scoped>
.calendar-page { min-height: 60vh; }
.page-hero {
  background: var(--rk-primary, #409eff);
  color: #fff;
  padding: 60px 0 40px;
  text-align: center;
}
.page-hero h1 { font-size: 32px; margin: 0 0 8px; }
.page-hero p { font-size: 16px; opacity: 0.85; margin: 0; }
.page-body { padding: 40px 0 60px; }

.calendar-cell { height: 100%; min-height: 80px; }
.cell-day { font-weight: 600; }
.has-events .cell-day { color: var(--rk-primary, #409eff); }
.cell-events { margin-top: 4px; }
.cell-event {
  font-size: 11px;
  padding: 2px 6px;
  margin-bottom: 2px;
  background: var(--rk-primary, #409eff);
  color: #fff;
  border-radius: 3px;
  cursor: pointer;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cell-event--competition {
  background: #e67e22;
}
.event-type {
  display: inline-block;
  margin-right: 4px;
  font-weight: 700;
}
.cell-more { font-size: 10px; color: #909399; }
</style>
