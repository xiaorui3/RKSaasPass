<template>
  <div class="portal-hub">
    <section class="portal-hub-hero">
      <div>
        <span class="portal-hub-kicker">Community Hub</span>
        <h1>社团与成员</h1>
        <p>把社团概况、加入社团、校友和联系入口聚合起来，方便新用户先了解再参与。</p>
      </div>
      <router-link v-if="admissionSettings.allowJoinApplication !== false" class="portal-hub-action" :to="isLoggedIn ? '/join' : '/register'">
        {{ isLoggedIn ? '加入其他租户' : '注册加入' }}
      </router-link>
    </section>

    <section class="portal-hub-grid">
      <router-link class="portal-hub-tile" to="/about">
        <span>About</span>
        <strong>社团概况</strong>
        <p>查看社团介绍、方向、组织结构和租户主页信息。</p>
      </router-link>
      <router-link v-if="admissionSettings.allowJoinApplication !== false" class="portal-hub-tile" to="/join">
        <span>Join</span>
        <strong>加入我们</strong>
        <p>进入入社申请或跨租户申请流程。</p>
      </router-link>
      <router-link v-if="portalSettings.showAlumni !== false" class="portal-hub-tile" to="/alumni">
        <span>Alumni</span>
        <strong>校友风采</strong>
        <p>查看各届校友与社团发展沉淀。</p>
      </router-link>
      <router-link class="portal-hub-tile" to="/contact">
        <span>Contact</span>
        <strong>联系社团</strong>
        <p>提交留言、查看联系方式和反馈入口。</p>
      </router-link>
    </section>

    <section class="portal-hub-card">
      <div class="portal-hub-card-head">
        <h2>社团概况摘要</h2>
        <router-link to="/about">查看详情</router-link>
      </div>
      <p class="portal-hub-summary">{{ summary }}</p>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/stores/user'
import { useTenantSelfServiceStore } from '@/stores/tenantSelfService'
import { getPublicClubProfileConfig } from '@/api/club-profile'

const userStore = useUserStore()
const tenantSelfServiceStore = useTenantSelfServiceStore()
const profile = ref(null)
const isLoggedIn = computed(() => userStore.isLoggedIn)
const portalSettings = computed(() => tenantSelfServiceStore.portalSettings)
const admissionSettings = computed(() => tenantSelfServiceStore.admissionSettings)
const summary = computed(() => {
  const text = profile.value?.summary || profile.value?.description || profile.value?.introduction
  return text || '当前租户可在后台维护社团概况、联系方式、加入说明和展示内容。'
})

onMounted(async () => {
  try {
    await tenantSelfServiceStore.loadPublicConfig(userStore.tenantId)
    const res = await getPublicClubProfileConfig(userStore.tenantId)
    profile.value = res.data || null
  } catch {
    profile.value = null
  }
})
</script>

<style lang="scss" scoped>
.portal-hub {
  display: grid;
  gap: 22px;
}

.portal-hub-hero,
.portal-hub-card,
.portal-hub-tile {
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
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px;
}

.portal-hub-tile {
  display: grid;
  gap: 10px;
  min-height: 190px;
  padding: 20px;
  color: #213857;
  text-decoration: none;

  span {
    color: #1473ff;
    font-size: 12px;
    font-weight: 900;
  }

  strong {
    color: #162c52;
    font-size: 20px;
  }

  p {
    margin: 0;
    color: #6b7b94;
    line-height: 1.7;
  }
}

.portal-hub-card {
  padding: 20px;
}

.portal-hub-card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;

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

.portal-hub-summary {
  margin: 0;
  color: #60718b;
  line-height: 1.8;
}

@media (max-width: 980px) {
  .portal-hub-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .portal-hub-hero {
    display: grid;
    padding: 22px;
  }

  .portal-hub-grid {
    grid-template-columns: 1fr;
  }
}
</style>
