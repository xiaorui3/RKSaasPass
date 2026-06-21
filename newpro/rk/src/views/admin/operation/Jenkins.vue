<template>
  <div class="admin-page jenkins-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <div>
            <h1>Jenkins 鎺у埗鍙?/h1>
            <p>閫氳繃鍚庣浠ｇ悊瑙﹀彂鍐呯綉 Jenkins锛屽彧瀵圭鎴?1 寮€鏀俱€傝繖閲屼細鏄剧ず鏋勫缓妯″紡銆侀槦鍒椼€佽繘搴﹀拰鏃ュ織銆?/p>
          </div>
          <el-button v-if="canUseJenkins" type="primary" :loading="loading" @click="fetchOverview">
            鍒锋柊浠诲姟
          </el-button>
        </div>
      </template>

      <el-alert
        v-if="!canUseJenkins"
        type="warning"
        :closable="false"
        show-icon
        title="Jenkins 鏋勫缓鑳藉姏褰撳墠浠呭绉熸埛 1 寮€鏀俱€?
      />

      <template v-else>
        <el-alert
          type="info"
          :closable="false"
          show-icon
          class="source-alert"
          :title="sourceLabel || '閫氳繃鍚庣浠ｇ悊鍚屾 Jenkins锛屽唴缃?Jenkins 鍦板潃涓嶄細鏆撮湶鍒板叕缃戙€?"
        />

        <el-card shadow="never" class="build-card">
          <template #header>
            <div class="section-header">
              <span>瑙﹀彂鏋勫缓</span>
              <div class="section-actions">
                <el-button size="small" plain @click="openServiceDialog()">鏂板寰湇鍔?/el-button>
                <el-tag :type="buildForm.imageMode === 'registry' ? 'warning' : 'success'" effect="plain">
                  {{ imageModeLabel(buildForm.imageMode) }}
                </el-tag>
              </div>
            </div>
          </template>

          <el-alert
            class="mode-alert"
            :type="buildForm.imageMode === 'registry' ? 'warning' : 'success'"
            :closable="false"
            show-icon
            :title="imageModeDescription"
          />

          <el-form :model="buildForm" label-width="120px" class="build-form">
            <el-form-item label="娴佹按绾?>
              <el-select v-model="buildForm.jobName" filterable placeholder="璇烽€夋嫨涓绘祦姘寸嚎">
                <el-option
                  v-for="job in triggerableJobs"
                  :key="job.name"
                  :label="job.name"
                  :value="job.name"
                />
              </el-select>
              <span class="form-hint">寤鸿鍙Е鍙戜富娴佹按绾匡紝鍏蜂綋鏈嶅姟閫氳繃 SERVICES 鎺у埗銆?/span>
            </el-form-item>

            <el-form-item label="浠ｇ爜浠撳簱">
              <el-radio-group v-model="buildForm.gitSource" @change="handleGitSourceChange">
                <el-radio-button
                  v-for="source in gitSources"
                  :key="source.source"
                  :label="source.source"
                >
                  {{ source.label }}
                </el-radio-button>
              </el-radio-group>
              <span class="form-hint">
                浼氫綔涓?Jenkins 鍙傛暟 <code>GIT_SOURCE</code> 浼犲叆锛寋{ selectedGitSource?.repositoryUrl || '浠撳簱鍦板潃鐢卞悗绔厤缃? }}銆?
              </span>
            </el-form-item>

            <el-form-item label="鐩爣鍒嗘敮">
              <el-select
                v-model="buildForm.targetBranch"
                filterable
                allow-create
                default-first-option
                :loading="branchLoading"
                placeholder="璇烽€夋嫨鎴栬緭鍏ョ洰鏍囧垎鏀?
              >
                <el-option
                  v-for="branch in branchOptions"
                  :key="branch"
                  :label="branch"
                  :value="branch"
                />
              </el-select>
              <span class="form-hint">{{ branchHint }}</span>
              <el-alert
                v-if="branchFallback"
                class="branch-fallback-alert"
                type="warning"
                :closable="false"
                show-icon
                title="褰撳墠鍒嗘敮鍒楄〃鏄厹搴曠粨鏋滐紝涓嶆槸瀹炴椂杩滅▼鍒嗘敮銆傝鍏堝埛鏂版垨鎵嬪姩纭鐩爣鍒嗘敮銆?
              />
            </el-form-item>

            <el-form-item label="鏈嶅姟鑼冨洿">
              <el-select
                v-model="buildForm.services"
                multiple
                filterable
                allow-create
                default-first-option
                placeholder="璇烽€夋嫨瑕侀儴缃茬殑鏈嶅姟"
              >
                <el-option
                  v-for="service in serviceOptions"
                  :key="service.serviceCode || service"
                  :label="serviceLabel(service)"
                  :value="service.serviceCode || service"
                />
              </el-select>
              <span class="form-hint">浼氫綔涓?Jenkins 鍙傛暟 <code>SERVICES</code> 浼犲叆锛屼緥濡?rk-user,frontend銆?/span>
            </el-form-item>

            <el-form-item label="闀滃儚妯″紡">
              <el-radio-group v-model="buildForm.imageMode">
                <el-radio-button label="local">鏈湴鏋勫缓鐩存帴閮ㄧ讲</el-radio-button>
                <el-radio-button label="registry">鎺ㄩ€侀樋閲屼簯闀滃儚浠撳簱</el-radio-button>
              </el-radio-group>
              <span class="form-hint">浼氫綔涓?Jenkins 鍙傛暟 <code>RK_K8S_IMAGE_MODE</code> 浼犲叆銆?/span>
            </el-form-item>

            <el-form-item label="閮ㄧ讲鍚庣紑">
              <el-input v-model.trim="buildForm.deployJobSuffix" placeholder="閫氬父鐣欑┖" />
            </el-form-item>

            <el-form-item label="SSH 涓绘満">
              <el-input v-model.trim="buildForm.remoteHost" placeholder="渚嬪鍐呯綉 IP 鎴栧叕缃?SSH 鍦板潃" />
              <span class="form-hint">浼氫綔涓?Jenkins 鍙傛暟 <code>RK_REMOTE_HOST</code> 浼犲叆锛屽繀椤绘樉寮忓～鍐欑洰鏍囨満鍣ㄣ€?/span>
            </el-form-item>

            <el-form-item label="SSH 绔彛">
              <el-input v-model.trim="buildForm.remotePort" placeholder="渚嬪 22" />
              <span class="form-hint">浼氫綔涓?Jenkins 鍙傛暟 <code>RK_REMOTE_PORT</code> 浼犲叆銆?/span>
            </el-form-item>

            <el-form-item label="SSH 鐢ㄦ埛">
              <el-input v-model.trim="buildForm.remoteUser" placeholder="root" />
              <span class="form-hint">浼氫綔涓?Jenkins 鍙傛暟 <code>RK_REMOTE_USER</code> 浼犲叆銆?/span>
            </el-form-item>

            <el-form-item label="SSH 瀵嗙爜">
              <el-input v-model="buildForm.remotePassword" type="password" show-password placeholder="浠呮湰娆¤Е鍙戜娇鐢? />
              <span class="form-hint">浼氫綔涓?Jenkins 鍙傛暟 <code>RK_REMOTE_PASSWORD</code> 浼犲叆锛屼笉浼氫繚瀛樺埌鏋勫缓鍘嗗彶灞曠ず涓€?/span>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" :loading="triggerLoading" @click="handleTrigger()">
                瑙﹀彂鏋勫缓骞舵煡鐪嬭繘搴?
              </el-button>
              <el-button :disabled="!activeBuild" :loading="statusLoading" @click="fetchBuildStatus">
                鍒锋柊褰撳墠杩涘害
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>

        <el-card v-if="buildStatus" shadow="never" class="status-card">
          <template #header>
            <div class="section-header">
              <span>褰撳墠鏋勫缓杩涘害</span>
              <el-tag :type="buildStatusTag(buildStatus.status)">
                {{ buildStatusLabel(buildStatus.status) }}
              </el-tag>
            </div>
          </template>

          <el-descriptions :column="3" border>
            <el-descriptions-item label="浠诲姟">{{ buildStatus.jobName || activeBuild?.jobName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="闃熷垪鍙?>{{ buildStatus.queueId || activeBuild?.queueId || '-' }}</el-descriptions-item>
            <el-descriptions-item label="鏋勫缓鍙?>{{ buildStatus.buildNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="鏈嶅姟">{{ buildStatus.services || activeBuild?.services || '-' }}</el-descriptions-item>
            <el-descriptions-item label="浠ｇ爜浠撳簱">
              {{ buildStatus.gitSourceLabel || activeBuild?.gitSourceLabel || gitSourceLabel(buildStatus.gitSource || activeBuild?.gitSource) }}
            </el-descriptions-item>
            <el-descriptions-item label="鍒嗘敮">{{ buildStatus.targetBranch || activeBuild?.targetBranch || '-' }}</el-descriptions-item>
            <el-descriptions-item label="闀滃儚妯″紡">
              {{ imageModeLabel(buildStatus.imageMode || activeBuild?.imageMode) }}
            </el-descriptions-item>
          </el-descriptions>

          <el-progress
            class="build-progress"
            :percentage="buildProgress(buildStatus)"
            :status="buildStatus.status === 'failed' ? 'exception' : buildStatus.status === 'success' ? 'success' : undefined"
          />

          <el-alert
            class="status-message"
            :closable="false"
            show-icon
            :type="buildStatus.status === 'failed' ? 'error' : 'info'"
            :title="buildStatus.message || '绛夊緟 Jenkins 杩斿洖鏋勫缓鐘舵€?"
          />

          <el-alert
            v-if="buildStatus.preflightSummary || activeBuild?.preflightSummary"
            class="status-message"
            :closable="false"
            show-icon
            type="warning"
            title="Preflight disk snapshot"
          >
            <pre class="build-log preflight-log">{{ buildStatus.preflightSummary || activeBuild?.preflightSummary }}</pre>
          </el-alert>

          <div class="log-header">
            <span>鏃ュ織灏鹃儴</span>
            <el-tag effect="plain">鑷姩杞涓細鎸佺画鍒锋柊</el-tag>
          </div>
          <pre class="build-log">{{ buildStatus.logTail || '鏆傛椂娌℃湁鏃ュ織锛屽彲鑳借繕鍦ㄦ帓闃熴€? }}</pre>
        </el-card>

        <el-card shadow="never" class="tutorial-card">
          <template #header>
            <div class="section-header">
              <span>瑙﹀彂鏋勫缓鏁欑▼</span>
              <el-tag type="info" effect="plain">鍙皟鐢ㄥ唴缃?Jenkins</el-tag>
            </div>
          </template>
          <el-steps direction="vertical" :active="5" finish-status="success" class="build-steps">
            <el-step title="閫夋嫨涓绘祦姘寸嚎">
              <template #description>
                褰撳墠鎺ㄨ崘瑙﹀彂 <code>rk-web-cloud-master-new</code>锛屽悗绔細璋冪敤
                <code>/buildWithParameters</code>锛屼笉鏄祻瑙堝櫒鐩存帴璁块棶 Jenkins銆?
              </template>
            </el-step>
            <el-step title="閫夋嫨浠ｇ爜浠撳簱鍜屽垎鏀?>
              <template #description>
                <code>GIT_SOURCE=local</code> 琛ㄧず鏈湴 Gogs锛?code>GIT_SOURCE=gitee</code> 琛ㄧず Gitee锛?
                鍒囨崲浠撳簱鍚庝細鑷姩璇诲彇杩滅▼鍒嗘敮锛屽啀鎶?<code>TARGET_BRANCH</code> 涓€璧蜂紶缁?Jenkins銆?
              </template>
            </el-step>
            <el-step title="閫夋嫨鏈嶅姟鑼冨洿">
              <template #description>
                <code>SERVICES</code> 鎺у埗鏈閮ㄧ讲鏈嶅姟锛屼緥濡?<code>rk-user</code>銆?
                <code>frontend</code> 鎴?<code>rk-user,frontend</code>銆?
              </template>
            </el-step>
            <el-step title="鏈湴鏋勫缓鐩存帴閮ㄧ讲">
              <template #description>
                <code>RK_K8S_IMAGE_MODE=local</code> 浼氬湪 K8s 鏋勫缓鑺傜偣鎵撻暅鍍忥紝骞跺鍏ュ悇鑺傜偣 containerd 鍚庢粴鍔ㄩ儴缃层€?
              </template>
            </el-step>
            <el-step title="鎺ㄩ€侀樋閲屼簯闀滃儚骞舵媺鍙栭儴缃?>
              <template #description>
                <code>RK_K8S_IMAGE_MODE=registry</code> 浼氭帹閫佸埌闃块噷浜戦暅鍍忎粨搴擄紝K8s 鍐嶆媺鍙栭暅鍍忛儴缃诧紝閫傚悎闇€瑕侀暅鍍忕暀妗ｇ殑鍙戝竷銆?
              </template>
            </el-step>
          </el-steps>
        </el-card>

        <el-card shadow="never" class="registry-card">
          <template #header>
            <div class="section-header">
              <span>寰湇鍔℃敞鍐?/span>
              <div class="section-actions">
                <el-button :loading="serviceLoading" @click="fetchServiceRegistry">鍒锋柊鏈嶅姟</el-button>
                <el-button type="primary" @click="openServiceDialog()">鏂板寰湇鍔?/el-button>
              </div>
            </div>
          </template>
          <el-table :data="serviceRegistry" v-loading="serviceLoading" stripe empty-text="鏆傛棤寰湇鍔￠厤缃?>
            <el-table-column prop="serviceCode" label="鏈嶅姟缂栫爜" min-width="130" />
            <el-table-column prop="displayName" label="鍚嶇О" min-width="140" />
            <el-table-column prop="gitSource" label="浠撳簱鏉ユ簮" width="100" />
            <el-table-column prop="branchName" label="榛樿鍒嗘敮" min-width="140" />
            <el-table-column prop="modulePath" label="妯″潡璺緞" min-width="180" show-overflow-tooltip />
            <el-table-column prop="workloadName" label="K8s 宸ヤ綔璐熻浇" min-width="180" show-overflow-tooltip />
            <el-table-column label="鐘舵€? width="90">
              <template #default="{ row }">
                <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '鍚敤' : '鍋滅敤' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="鎿嶄綔" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="openServiceDialog(row)">缂栬緫</el-button>
                <el-button size="small" type="danger" plain @click="handleDeleteService(row)">鍒犻櫎</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="history-card">
          <template #header>
            <div class="section-header">
              <span>鏋勫缓鍘嗗彶</span>
              <el-button :loading="historyLoading" @click="fetchBuildHistory">鍒锋柊鍘嗗彶</el-button>
            </div>
          </template>
          <el-form :inline="true" :model="historyFilters" class="history-filters">
            <el-form-item label="鏈嶅姟">
              <el-select v-model="historyFilters.serviceCode" clearable filterable placeholder="鍏ㄩ儴鏈嶅姟" style="width: 180px">
                <el-option
                  v-for="service in serviceOptions"
                  :key="service.serviceCode || service"
                  :label="serviceLabel(service)"
                  :value="service.serviceCode || service"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="浠撳簱">
              <el-select v-model="historyFilters.gitSource" clearable placeholder="鍏ㄩ儴浠撳簱" style="width: 140px">
                <el-option label="鏈湴 Gogs" value="local" />
                <el-option label="Gitee" value="gitee" />
              </el-select>
            </el-form-item>
            <el-form-item label="鍒嗘敮">
              <el-input v-model.trim="historyFilters.branchName" clearable placeholder="杈撳叆鍒嗘敮" style="width: 180px" />
            </el-form-item>
            <el-form-item label="鎿嶄綔浜?>
              <el-input v-model.trim="historyFilters.operatorKeyword" clearable placeholder="鐢ㄦ埛ID鎴栧悕绉? style="width: 160px" />
            </el-form-item>
            <el-form-item label="鐘舵€?>
              <el-select v-model="historyFilters.status" clearable placeholder="鍏ㄩ儴鐘舵€? style="width: 140px">
                <el-option label="鎺掗槦涓? value="queued" />
                <el-option label="鏋勫缓涓? value="running" />
                <el-option label="鎴愬姛" value="success" />
                <el-option label="澶辫触" value="failed" />
                <el-option label="宸插彇娑? value="canceled" />
              </el-select>
            </el-form-item>
            <el-form-item label="鏃堕棿">
              <el-date-picker
                v-model="historyFilters.timeRange"
                type="datetimerange"
                start-placeholder="寮€濮嬫椂闂?
                end-placeholder="缁撴潫鏃堕棿"
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 340px"
              />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="fetchBuildHistory">鏌ヨ</el-button>
            </el-form-item>
          </el-form>
          <el-table :data="buildHistory" v-loading="historyLoading" stripe empty-text="鏆傛棤鏋勫缓鍘嗗彶">
            <el-table-column prop="triggerTime" label="瑙﹀彂鏃堕棿" min-width="160" />
            <el-table-column prop="jobName" label="娴佹按绾? min-width="190" show-overflow-tooltip />
            <el-table-column prop="services" label="鏈嶅姟" min-width="160" show-overflow-tooltip />
            <el-table-column prop="branchName" label="鍒嗘敮" min-width="140" />
            <el-table-column label="鐘舵€? width="100">
              <template #default="{ row }">
                <el-tag :type="buildStatusTag(row.status)">{{ buildStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="鏋勫缓鍙? width="110">
              <template #default="{ row }">
                <el-link v-if="row.buildUrl" :href="row.buildUrl" target="_blank" type="primary">
                  #{{ row.buildNumber || '-' }}
                </el-link>
                <span v-else>#{{ row.buildNumber || '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="failureReason" label="澶辫触鍘熷洜" min-width="220" show-overflow-tooltip />
          </el-table>
        </el-card>

        <el-table :data="jobs" v-loading="loading" stripe empty-text="鏆傛棤 Jenkins 浠诲姟">
          <el-table-column prop="name" label="浠诲姟鍚嶇О" min-width="220" />
          <el-table-column label="鐘舵€? width="120">
            <template #default="{ row }">
              <el-tag :type="jenkinsTagType(row.status)">
                {{ jenkinsStatusLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="鏈€鏂版瀯寤? width="120">
            <template #default="{ row }">
              {{ row.lastBuildNumber ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column label="鏈€杩戝畬鎴? width="120">
            <template #default="{ row }">
              {{ row.lastCompletedBuildNumber ?? '-' }}
            </template>
          </el-table-column>
          <el-table-column prop="url" label="浠诲姟鍦板潃" min-width="260" show-overflow-tooltip />
          <el-table-column label="鎿嶄綔" width="190" fixed="right">
            <template #default="{ row }">
              <el-button
                size="small"
                type="primary"
                :disabled="!isTriggerableJob(row)"
                :loading="triggeringJob === row.name"
                @click="handleTrigger(row)"
              >
                鎸変笂鏂归厤缃Е鍙?
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-dialog v-model="serviceRegistryDialogVisible" title="寰湇鍔℃敞鍐? width="720px">
          <el-form :model="serviceForm" label-width="120px">
            <el-form-item label="鏈嶅姟缂栫爜">
              <el-input v-model.trim="serviceForm.serviceCode" placeholder="渚嬪 rk-user" />
            </el-form-item>
            <el-form-item label="鏄剧ず鍚嶇О">
              <el-input v-model.trim="serviceForm.displayName" placeholder="渚嬪 鐢ㄦ埛鏈嶅姟" />
            </el-form-item>
            <el-form-item label="鏈嶅姟绫诲瀷">
              <el-select v-model="serviceForm.serviceType">
                <el-option label="鍚庣寰湇鍔? value="backend" />
                <el-option label="鍓嶇绔欑偣" value="frontend" />
              </el-select>
            </el-form-item>
            <el-form-item label="浠撳簱鏉ユ簮">
              <el-select v-model="serviceForm.gitSource">
                <el-option label="鏈湴 Gogs" value="local" />
                <el-option label="Gitee" value="gitee" />
              </el-select>
            </el-form-item>
            <el-form-item label="榛樿鍒嗘敮">
              <el-input v-model.trim="serviceForm.branchName" />
            </el-form-item>
            <el-form-item label="妯″潡璺緞">
              <el-input v-model.trim="serviceForm.modulePath" placeholder="渚嬪 rk-user 鎴?newpro/rk" />
            </el-form-item>
            <el-form-item label="鏋勫缓妯″紡">
              <el-input v-model.trim="serviceForm.buildMode" placeholder="渚嬪 maven-docker / npm-docker" />
            </el-form-item>
            <el-form-item label="闀滃儚鍚?>
              <el-input v-model.trim="serviceForm.imageName" placeholder="rk-web/rk-user" />
            </el-form-item>
            <el-form-item label="鍛藉悕绌洪棿">
              <el-input v-model.trim="serviceForm.namespaceName" />
            </el-form-item>
            <el-form-item label="璐熻浇绫诲瀷">
              <el-select v-model="serviceForm.workloadType">
                <el-option label="StatefulSet" value="StatefulSet" />
                <el-option label="Deployment" value="Deployment" />
              </el-select>
            </el-form-item>
            <el-form-item label="宸ヤ綔璐熻浇">
              <el-input v-model.trim="serviceForm.workloadName" placeholder="渚嬪 rk-server-rk-user" />
            </el-form-item>
            <el-form-item label="瀹瑰櫒鍚?>
              <el-input v-model.trim="serviceForm.containerName" />
            </el-form-item>
            <el-form-item label="鍋ュ悍妫€鏌?>
              <el-input v-model.trim="serviceForm.healthCheckPath" placeholder="/actuator/health" />
            </el-form-item>
            <el-form-item label="璧勬簮闄愬埗">
              <el-input v-model.trim="serviceForm.resourceLimits" placeholder="cpu=500m,memory=640Mi" />
            </el-form-item>
            <el-form-item label="鎺掑簭">
              <el-input-number v-model="serviceForm.sortOrder" :min="1" :max="999" />
            </el-form-item>
            <el-form-item label="鍚敤">
              <el-switch v-model="serviceForm.enabled" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="serviceRegistryDialogVisible = false">鍙栨秷</el-button>
            <el-button type="primary" :loading="serviceSaving" @click="handleSaveService">淇濆瓨</el-button>
          </template>
        </el-dialog>
      </template>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  deleteOpsService,
  getJenkinsBuildStatus,
  getJenkinsBuildHistory,
  getJenkinsGitBranches,
  getJenkinsGitSources,
  getJenkinsOverview,
  listOpsServices,
  saveOpsService,
  triggerJenkinsJob
} from '@/api/admin-ops'
import { useUserStore } from '@/stores/user'
import { canAccessJenkinsOps, normalizeJenkinsJobs } from '@/utils/adminOpsTaskCenter'

const fallbackServiceOptions = [
  'rk-gateway',
  'rk-auth',
  'rk-user',
  'rk-file',
  'rk-message',
  'rk-content',
  'rk-activity',
  'rk-search',
  'rk-pay',
  'rk-trade',
  'rk-exam',
  'rk-data',
  'frontend'
]

const fallbackGitSources = [
  {
    source: 'local',
    label: '鏈湴 Gogs',
    repositoryUrl: 'https://github.com/example/rk-web.git',
    defaultBranch: 'cloud-master-new',
    defaultSource: true
  },
  {
    source: 'gitee',
    label: 'Gitee',
    repositoryUrl: 'https://github.com/example/rk-web',
    defaultBranch: 'cloud-master-new',
    defaultSource: false
  }
]

const userStore = useUserStore()
const canUseJenkins = computed(() => canAccessJenkinsOps(userStore.tenantId))
const loading = ref(false)
const triggerLoading = ref(false)
const statusLoading = ref(false)
const branchLoading = ref(false)
const serviceLoading = ref(false)
const serviceSaving = ref(false)
const historyLoading = ref(false)
const jobs = ref([])
const serviceRegistry = ref([])
const buildHistory = ref([])
const gitSources = ref([...fallbackGitSources])
const branchOptions = ref([])
const branchMessage = ref('')
const branchFallback = ref(false)
const sourceLabel = ref('')
const triggeringJob = ref('')
const activeBuild = ref(null)
const buildStatus = ref(null)
const serviceRegistryDialogVisible = ref(false)
let pollTimer = null

const buildForm = reactive({
  jobName: '',
  gitSource: 'local',
  targetBranch: 'cloud-master-new',
  services: ['rk-user'],
  imageMode: 'local',
  deployJobSuffix: '',
  remoteHost: '',
  remotePort: '',
  remoteUser: 'root',
  remotePassword: ''
})

const historyFilters = reactive({
  serviceCode: '',
  branchName: '',
  gitSource: '',
  status: '',
  operatorKeyword: '',
  timeRange: []
})

const serviceForm = reactive({
  id: null,
  serviceCode: '',
  displayName: '',
  serviceType: 'backend',
  gitSource: 'local',
  branchName: 'cloud-master-new',
  modulePath: '',
  buildMode: 'maven-docker',
  imageName: '',
  namespaceName: 'shetuanguanlixitong',
  workloadType: 'StatefulSet',
  workloadName: '',
  containerName: '',
  healthCheckPath: '',
  resourceLimits: '',
  enabled: true,
  sortOrder: 100
})

const triggerableJobs = computed(() => jobs.value.filter(isTriggerableJob))
const serviceOptions = computed(() => serviceRegistry.value.length ? serviceRegistry.value.filter((item) => item.enabled) : fallbackServiceOptions)
const selectedGitSource = computed(() => {
  return gitSources.value.find((source) => source.source === buildForm.gitSource) || fallbackGitSources[0]
})
const branchHint = computed(() => {
  if (branchLoading.value) {
    return '姝ｅ湪璇诲彇杩滅▼鍒嗘敮...'
  }
  if (branchMessage.value) {
    return branchMessage.value
  }
  return `褰撳墠鏉ユ簮锛?{gitSourceLabel(buildForm.gitSource)}锛屽彲閫夋嫨杩滅▼鍒嗘敮锛屼篃鍙互鎵嬪姩杈撳叆鏂板垎鏀€俙
})
const imageModeDescription = computed(() => {
  if (buildForm.imageMode === 'registry') {
    return '褰撳墠閫夋嫨闃块噷浜戦暅鍍忎粨搴撴ā寮忥細Jenkins 浼氭墦鍖呴暅鍍忋€佹帹閫侀樋閲屼簯浠撳簱锛岀劧鍚?K8s 鎷夊彇鏂伴暅鍍忛儴缃层€?
  }
  return '褰撳墠閫夋嫨鏈湴鏋勫缓鐩存帴閮ㄧ讲锛欽enkins 浼氬湪 K8s 鏋勫缓鑺傜偣鐢熸垚闀滃儚锛屽苟鐩存帴瀵煎叆闆嗙兢鑺傜偣閮ㄧ讲銆?
})

function isTriggerableJob(row) {
  return String(row?.name || '').startsWith('rk-web-cloud-master-new')
}

function imageModeLabel(mode) {
  return mode === 'registry' ? '闃块噷浜戦暅鍍忎粨搴撴ā寮? : '鏈湴鏋勫缓鐩存帴閮ㄧ讲'
}

function gitSourceLabel(source) {
  return gitSources.value.find((item) => item.source === source)?.label || (source === 'gitee' ? 'Gitee' : '鏈湴 Gogs')
}

function serviceLabel(service) {
  if (typeof service === 'string') {
    return service
  }
  return service.displayName ? `${service.displayName}锛?{service.serviceCode}锛塦 : service.serviceCode
}

function jenkinsTagType(status) {
  if (status === 'success') return 'success'
  if (status === 'failed') return 'danger'
  if (status === 'running') return 'warning'
  if (status === 'disabled') return 'info'
  return ''
}

function jenkinsStatusLabel(status) {
  return {
    success: '鎴愬姛',
    failed: '澶辫触',
    running: '鏋勫缓涓?,
    disabled: '宸茬鐢?,
    unknown: '鏈煡'
  }[status] || '鏈煡'
}

function buildStatusTag(status) {
  return {
    queued: 'info',
    running: 'warning',
    success: 'success',
    failed: 'danger',
    canceled: 'info',
    unknown: 'info'
  }[status] || 'info'
}

function buildStatusLabel(status) {
  return {
    queued: '鎺掗槦涓?,
    running: '鏋勫缓涓?,
    success: '鎴愬姛',
    failed: '澶辫触',
    canceled: '宸插彇娑?,
    unknown: '鏈煡'
  }[status] || '鏈煡'
}

function buildProgress(status) {
  if (!status) return 0
  if (status.status === 'success') return 100
  if (status.status === 'failed' || status.status === 'canceled') return 100
  if (status.status === 'queued') return 5
  const duration = Number(status.durationMillis || 0)
  const estimated = Number(status.estimatedDurationMillis || 0)
  if (estimated > 0) {
    return Math.max(10, Math.min(95, Math.round((duration / estimated) * 100)))
  }
  return 30
}

function selectDefaultJob() {
  if (buildForm.jobName && jobs.value.some((job) => job.name === buildForm.jobName)) {
    return
  }
  const preferred = jobs.value.find((job) => job.name === 'rk-web-cloud-master-new')
  const fallback = triggerableJobs.value[0]
  buildForm.jobName = preferred?.name || fallback?.name || ''
}

async function fetchGitSources() {
  if (!canUseJenkins.value) {
    return
  }
  try {
    const res = await getJenkinsGitSources()
    if (res.code !== 200 || !Array.isArray(res.data) || !res.data.length) {
      throw new Error(res.msg || '鑾峰彇 Git 浠撳簱鏉ユ簮澶辫触')
    }
    gitSources.value = res.data
    const defaultSource = gitSources.value.find((item) => item.defaultSource) || gitSources.value[0]
    if (!gitSources.value.some((item) => item.source === buildForm.gitSource)) {
      buildForm.gitSource = defaultSource.source
    }
    if (!buildForm.targetBranch) {
      buildForm.targetBranch = defaultSource.defaultBranch || 'cloud-master-new'
    }
  } catch (error) {
    gitSources.value = [...fallbackGitSources]
    branchFallback.value = true
    branchMessage.value = error.message || '鑾峰彇 Git 浠撳簱鏉ユ簮澶辫触锛屽凡浣跨敤鏈湴榛樿閰嶇疆銆?
  }
  await fetchGitBranches()
}

async function fetchGitBranches() {
  if (!canUseJenkins.value) {
    return
  }
  branchLoading.value = true
  try {
    const res = await getJenkinsGitBranches(buildForm.gitSource)
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '鑾峰彇 Git 鍒嗘敮澶辫触')
    }
    branchOptions.value = Array.isArray(res.data.branches) ? res.data.branches : []
    branchFallback.value = Boolean(res.data.fallback)
    branchMessage.value = res.data.message || `${res.data.label || gitSourceLabel(buildForm.gitSource)} 宸茶瘑鍒?${branchOptions.value.length} 涓垎鏀€俙
    if (!branchOptions.value.includes(buildForm.targetBranch)) {
      const preferred = branchOptions.value.find((branch) => branch === 'cloud-master-new')
      buildForm.targetBranch = preferred || branchOptions.value[0] || buildForm.targetBranch || 'cloud-master-new'
    }
  } catch (error) {
    branchOptions.value = []
    branchFallback.value = true
    branchMessage.value = `${gitSourceLabel(buildForm.gitSource)} 鍒嗘敮璇诲彇澶辫触锛?{error.message || '璇锋墜鍔ㄨ緭鍏ョ洰鏍囧垎鏀?}銆俙
  } finally {
    branchLoading.value = false
  }
}

function handleGitSourceChange() {
  fetchGitBranches()
}

async function fetchOverview() {
  if (!canUseJenkins.value) {
    jobs.value = []
    return
  }
  loading.value = true
  try {
    const res = await getJenkinsOverview()
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '鑾峰彇 Jenkins 浠诲姟澶辫触')
    }
    jobs.value = normalizeJenkinsJobs(res.data.jobs || [])
    sourceLabel.value = '閫氳繃鍚庣浠ｇ悊鍚屾 Jenkins锛屽唴缃?Jenkins 鍦板潃宸查殣钘忋€?
    selectDefaultJob()
  } catch (error) {
    ElMessage.error(error.message || '鑾峰彇 Jenkins 浠诲姟澶辫触')
  } finally {
    loading.value = false
  }
}

async function fetchServiceRegistry() {
  if (!canUseJenkins.value) {
    serviceRegistry.value = []
    return
  }
  serviceLoading.value = true
  try {
    const res = await listOpsServices()
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '鑾峰彇寰湇鍔℃敞鍐屽け璐?)
    }
    serviceRegistry.value = res.data
  } catch (error) {
    ElMessage.error(error.message || '鑾峰彇寰湇鍔℃敞鍐屽け璐?)
  } finally {
    serviceLoading.value = false
  }
}

async function fetchBuildHistory() {
  if (!canUseJenkins.value) {
    buildHistory.value = []
    return
  }
  historyLoading.value = true
  try {
    const [startTime, endTime] = Array.isArray(historyFilters.timeRange) ? historyFilters.timeRange : []
    const res = await getJenkinsBuildHistory({
      ...historyFilters,
      startTime,
      endTime
    })
    if (res.code !== 200 || !Array.isArray(res.data)) {
      throw new Error(res.msg || '鑾峰彇鏋勫缓鍘嗗彶澶辫触')
    }
    buildHistory.value = res.data.map(normalizeBuildStatus)
    reconcileActiveBuildFromHistory()
  } catch (error) {
    ElMessage.error(error.message || '鑾峰彇鏋勫缓鍘嗗彶澶辫触')
  } finally {
    historyLoading.value = false
  }
}

function buildPayload() {
  return {
    targetBranch: buildForm.targetBranch || 'cloud-master-new',
    gitSource: buildForm.gitSource || 'local',
    services: buildForm.services.join(','),
    imageMode: buildForm.imageMode,
    deployJobSuffix: buildForm.deployJobSuffix || '',
    remoteHost: buildForm.remoteHost || '',
    remotePort: buildForm.remotePort || '',
    remoteUser: buildForm.remoteUser || 'root',
    remotePassword: buildForm.remotePassword || ''
  }
}

async function handleTrigger(row) {
  const jobName = row?.name || buildForm.jobName
  if (!jobName) {
    ElMessage.warning('璇峰厛閫夋嫨 Jenkins 涓绘祦姘寸嚎')
    return
  }
  if (!isTriggerableJob({ name: jobName })) {
    ElMessage.warning('璇疯Е鍙戜富娴佹按绾匡紝鍗曚釜鏈嶅姟閫氳繃 SERVICES 鍙傛暟鎺у埗')
    return
  }
  if (!buildForm.services.length) {
    ElMessage.warning('璇疯嚦灏戦€夋嫨涓€涓湇鍔?)
    return
  }
  if (!buildForm.remoteHost || !buildForm.remotePort || !buildForm.remoteUser || !buildForm.remotePassword) {
    ElMessage.warning('璇峰～鍐?SSH 涓绘満銆佺鍙ｃ€佺敤鎴峰拰瀵嗙爜')
    return
  }
  triggeringJob.value = jobName
  triggerLoading.value = true
  try {
    const res = await triggerJenkinsJob(jobName, buildPayload())
    if (res.code !== 200 || !res.data?.accepted) {
      throw new Error(res.msg || res.data?.message || '瑙﹀彂 Jenkins 浠诲姟澶辫触')
    }
    activeBuild.value = normalizeBuildStatus(res.data)
    buildStatus.value = {
      ...activeBuild.value,
      status: 'queued',
      building: true,
      logTail: '',
      message: res.data.message || 'Jenkins 鏋勫缓宸茶繘鍏ラ槦鍒?
    }
    ElMessage.success(`宸茶Е鍙?${jobName}锛屼粨搴擄細${res.data.gitSourceLabel || gitSourceLabel(buildForm.gitSource)}锛屾ā寮忥細${imageModeLabel(res.data.imageMode)}`)
    startBuildPolling()
    await fetchOverview()
    await fetchBuildHistory()
  } catch (error) {
    ElMessage.error(error.message || '瑙﹀彂 Jenkins 浠诲姟澶辫触')
  } finally {
    triggeringJob.value = ''
    triggerLoading.value = false
  }
}

async function fetchBuildStatus() {
  if (!activeBuild.value) {
    return
  }
  statusLoading.value = true
  try {
    const params = {
      queueId: activeBuild.value.queueId || undefined,
      buildNumber: buildStatus.value?.buildNumber || undefined,
      tailLines: 160
    }
    const res = await getJenkinsBuildStatus(activeBuild.value.jobName, params)
    if (res.code !== 200 || !res.data) {
      throw new Error(res.msg || '鑾峰彇 Jenkins 鏋勫缓杩涘害澶辫触')
    }
    buildStatus.value = normalizeBuildStatus({
      ...activeBuild.value,
      ...res.data
    })
    activeBuild.value = {
      ...activeBuild.value,
      ...buildStatus.value
    }
    if (!isBuildInProgress(buildStatus.value)) {
      stopBuildPolling()
      await fetchOverview()
      await fetchBuildHistory()
    }
  } catch (error) {
    stopBuildPolling()
    ElMessage.error(error.message || '鑾峰彇 Jenkins 鏋勫缓杩涘害澶辫触')
  } finally {
    statusLoading.value = false
  }
}

function normalizeBuildStatus(row = {}) {
  const normalized = { ...row }
  const status = String(normalized.status || '').toLowerCase()
  const result = String(normalized.result || '').toUpperCase()
  const building = normalized.building === true || normalized.building === 'true'
  if (!building && (status === 'running' || status === 'queued')) {
    if (result === 'SUCCESS') {
      normalized.status = 'success'
    } else if (result === 'ABORTED') {
      normalized.status = 'canceled'
    } else if (result || normalized.finishTime) {
      normalized.status = 'failed'
    } else {
      normalized.status = status
    }
  } else if (status) {
    normalized.status = status
  }
  normalized.building = building && ['queued', 'running'].includes(String(normalized.status || '').toLowerCase())
  return normalized
}

function isBuildInProgress(row = {}) {
  return row.building === true || ['queued', 'running'].includes(String(row.status || '').toLowerCase())
}

function reconcileActiveBuildFromHistory() {
  if (!activeBuild.value && !buildStatus.value) {
    return
  }
  const activeQueueId = activeBuild.value?.queueId || buildStatus.value?.queueId
  const activeBuildNumber = activeBuild.value?.buildNumber || buildStatus.value?.buildNumber
  const matched = buildHistory.value.find((row) => {
    return (activeQueueId && row.queueId === activeQueueId)
      || (activeBuildNumber && Number(row.buildNumber) === Number(activeBuildNumber))
  })
  if (!matched || isBuildInProgress(matched)) {
    return
  }
  buildStatus.value = normalizeBuildStatus({
    ...activeBuild.value,
    ...buildStatus.value,
    ...matched
  })
  activeBuild.value = {
    ...activeBuild.value,
    ...buildStatus.value
  }
  stopBuildPolling()
}

function resetServiceForm(row = {}) {
  Object.assign(serviceForm, {
    id: row.id || null,
    serviceCode: row.serviceCode || '',
    displayName: row.displayName || '',
    serviceType: row.serviceType || 'backend',
    gitSource: row.gitSource || 'local',
    branchName: row.branchName || 'cloud-master-new',
    modulePath: row.modulePath || '',
    buildMode: row.buildMode || 'maven-docker',
    imageName: row.imageName || '',
    namespaceName: row.namespaceName || 'shetuanguanlixitong',
    workloadType: row.workloadType || 'StatefulSet',
    workloadName: row.workloadName || '',
    containerName: row.containerName || '',
    healthCheckPath: row.healthCheckPath || '',
    resourceLimits: row.resourceLimits || '',
    enabled: row.enabled !== false,
    sortOrder: row.sortOrder || 100
  })
}

function openServiceDialog(row) {
  resetServiceForm(row || {})
  serviceRegistryDialogVisible.value = true
}

async function handleSaveService() {
  if (!serviceForm.serviceCode || !serviceForm.displayName) {
    ElMessage.warning('璇峰～鍐欐湇鍔＄紪鐮佸拰鏄剧ず鍚嶇О')
    return
  }
  serviceSaving.value = true
  try {
    const res = await saveOpsService({ ...serviceForm })
    if (res.code !== 200) {
      throw new Error(res.msg || '淇濆瓨寰湇鍔″け璐?)
    }
    ElMessage.success('寰湇鍔￠厤缃凡淇濆瓨')
    serviceRegistryDialogVisible.value = false
    await fetchServiceRegistry()
  } catch (error) {
    ElMessage.error(error.message || '淇濆瓨寰湇鍔″け璐?)
  } finally {
    serviceSaving.value = false
  }
}

async function handleDeleteService(row) {
  try {
    await ElMessageBox.confirm(`纭鍒犻櫎寰湇鍔?${row.serviceCode} 鍚楋紵`, '鍒犻櫎寰湇鍔?, {
      confirmButtonText: '鍒犻櫎',
      cancelButtonText: '鍙栨秷',
      type: 'warning'
    })
  } catch {
    return
  }
  const res = await deleteOpsService(row.id)
  if (res.code === 200 && res.data) {
    ElMessage.success('宸插垹闄?)
    await fetchServiceRegistry()
    return
  }
  ElMessage.error(res.msg || '鍒犻櫎澶辫触')
}

function startBuildPolling() {
  stopBuildPolling()
  fetchBuildStatus()
  pollTimer = window.setInterval(fetchBuildStatus, 5000)
}

function stopBuildPolling() {
  if (pollTimer) {
    window.clearInterval(pollTimer)
    pollTimer = null
  }
}

onMounted(() => {
  fetchGitSources()
  fetchOverview()
  fetchServiceRegistry()
  fetchBuildHistory()
})

onBeforeUnmount(() => {
  stopBuildPolling()
})
</script>

<style scoped>
.jenkins-page {
  display: grid;
  gap: 20px;
}

.card-header,
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.card-header h1 {
  margin: 0;
  font-size: 24px;
}

.card-header p {
  margin: 8px 0 0;
  color: #909399;
}

.source-alert,
.build-card,
.status-card,
.tutorial-card,
.registry-card,
.history-card {
  margin-bottom: 16px;
}

.mode-alert {
  margin-bottom: 16px;
}

.section-header {
  align-items: center;
  font-weight: 700;
}

.section-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.history-filters {
  margin-bottom: 12px;
}

.build-form {
  max-width: 980px;
}

.build-form .el-select {
  width: 100%;
}

.form-hint {
  display: block;
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.branch-fallback-alert {
  margin-top: 8px;
}

.form-hint code,
.tutorial-card code {
  padding: 2px 6px;
  border-radius: 6px;
  background: #f5f7fa;
  color: #303133;
  font-family: Consolas, "Liberation Mono", monospace;
}

.build-progress {
  margin: 16px 0;
}

.status-message {
  margin-bottom: 16px;
}

.log-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 8px;
  font-weight: 700;
}

.build-log {
  max-height: 420px;
  overflow: auto;
  margin: 0;
  padding: 14px;
  border-radius: 10px;
  background: #111827;
  color: #d1fae5;
  font-family: Consolas, "Liberation Mono", monospace;
  font-size: 12px;
  line-height: 1.55;
  white-space: pre-wrap;
}

.build-steps {
  margin-top: 4px;
}

@media (max-width: 768px) {
  .card-header,
  .section-header,
  .log-header {
    flex-direction: column;
    align-items: flex-start;
  }

  :deep(.el-descriptions__body .el-descriptions__table) {
    display: block;
    overflow-x: auto;
  }
}
</style>
