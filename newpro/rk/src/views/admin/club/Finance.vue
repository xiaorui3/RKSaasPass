<template>
  <div class="finance-page">
    <div class="finance-header">
      <div>
        <h1>财务管理</h1>
        <p>面向高校社团经费监管的总账、预算、报销、审批、报表和风险预警工作台。</p>
      </div>
      <div class="header-actions">
        <el-button type="success" @click="handleAdd('income')">
          <el-icon><Plus /></el-icon>收入入账
        </el-button>
        <el-button type="danger" @click="handleAdd('expense')">
          <el-icon><Minus /></el-icon>支出/报销
        </el-button>
        <el-button @click="refreshAll">刷新</el-button>
      </div>
    </div>

    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card">
          <span>当前余额</span>
          <strong class="blue">{{ formatMoney(accountInfo.balance) }}</strong>
          <small>可用资金账户</small>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card">
          <span>累计收入</span>
          <strong class="green">{{ formatMoney(accountInfo.totalIncome) }}</strong>
          <small>拨款、会费、赞助</small>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card">
          <span>累计支出</span>
          <strong class="red">{{ formatMoney(accountInfo.totalExpense) }}</strong>
          <small>活动、物资、报销</small>
        </div>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <div class="metric-card">
          <span>预算使用率</span>
          <strong>{{ dashboard.budgetUsageRate || 0 }}%</strong>
          <small>待审 {{ formatMoney(dashboard.pendingAmount) }}</small>
        </div>
      </el-col>
    </el-row>

    <div class="module-grid">
      <button
        v-for="module in financeModuleCards"
        :key="module.key"
        class="module-card"
        :class="{ active: activeTab === module.key }"
        type="button"
        @click="activeTab = module.key"
      >
        <span class="module-title">{{ module.title }}</span>
        <strong>{{ module.count }}</strong>
        <small>{{ module.status }} · {{ formatMoney(module.amount) }}</small>
        <em>{{ module.subtitle }}</em>
      </button>
    </div>

    <el-row :gutter="16" class="finance-panels">
      <el-col v-if="activeTab === 'dashboard'" :xs="24" :lg="15">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>资金趋势与预算执行</span>
              <el-tag effect="plain">企业财务驾驶舱</el-tag>
            </div>
          </template>
          <div ref="trendChartRef" class="finance-chart" />
        </el-card>
      </el-col>
      <el-col v-if="activeTab === 'dashboard'" :xs="24" :lg="9">
        <el-card shadow="never" class="chart-card">
          <template #header>
            <div class="card-header">
              <span>风险预警</span>
              <el-tag type="warning" effect="plain">内控</el-tag>
            </div>
          </template>
          <div v-if="dashboard.riskAlerts.length" class="risk-list">
            <el-alert
              v-for="alert in dashboard.riskAlerts"
              :key="alert.title"
              :type="alert.type || 'info'"
              :title="alert.title"
              :description="alert.message"
              show-icon
              :closable="false"
            />
          </div>
          <el-empty v-else description="暂无风险预警" />
        </el-card>
      </el-col>
    </el-row>

    <el-tabs v-model="activeTab" class="finance-tabs">
      <el-tab-pane label="总账台账" name="ledger" />
      <el-tab-pane label="财务工作台" name="dashboard" />
      <el-tab-pane label="预算控制" name="budget" />
      <el-tab-pane label="经费拨款" name="allocation" />
      <el-tab-pane label="报销审批" name="reimbursement" />
      <el-tab-pane label="资金账户" name="fund" />
      <el-tab-pane label="凭证中心" name="voucher" />
      <el-tab-pane label="往来付款" name="payable" />
      <el-tab-pane label="资产台账" name="asset" />
      <el-tab-pane label="合规税务" name="tax" />
      <el-tab-pane label="电子档案" name="archive" />
      <el-tab-pane label="审计日志" name="audit" />
      <el-tab-pane label="财务报表" name="report" />
    </el-tabs>

    <el-card v-if="activeTab === 'ledger' || activeTab === 'reimbursement' || activeTab === 'allocation'" class="mt-20">
      <template #header>
        <div class="card-header">
          <span>{{ activeTab === 'reimbursement' ? '报销审批' : activeTab === 'allocation' ? '经费拨款' : '总账台账' }}</span>
          <el-tag effect="plain">收支、凭证、审核、入账</el-tag>
        </div>
      </template>

      <el-form :inline="true" class="search-form">
        <el-form-item label="关键词">
          <el-input v-model.trim="filterKeyword" clearable placeholder="单号/标题/说明" @keyup.enter="handleFilterChange" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="filterType" placeholder="全部类型" clearable @change="handleFilterChange">
            <el-option label="收入" value="income" />
            <el-option label="支出" value="expense" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="handleFilterChange">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务">
          <el-select v-model="filterBusinessType" placeholder="全部业务" clearable @change="handleFilterChange">
            <el-option label="收入入账" value="INCOME" />
            <el-option label="支出付款" value="EXPENSE" />
            <el-option label="报销" value="REIMBURSEMENT" />
            <el-option label="预算" value="BUDGET" />
            <el-option label="资产采购" value="ASSET" />
            <el-option label="往来付款" value="PAYMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="期间">
          <el-date-picker v-model="filterPeriod" type="month" value-format="YYYY-MM" placeholder="会计期间" @change="handleFilterChange" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleFilterChange">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="dataList" v-loading="loading" stripe>
        <el-table-column prop="recordNo" label="单据编号" min-width="150" show-overflow-tooltip />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag :type="row.type === 'income' ? 'success' : 'danger'">
              {{ row.type === 'income' ? '收入' : '支出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">
            <span :class="row.type === 'income' ? 'amount-income' : 'amount-expense'">
              {{ row.type === 'income' ? '+' : '-' }}{{ row.amount }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="category" label="分类" width="100">
          <template #default="{ row }">
            {{ categoryMap[row.category] || row.category }}
          </template>
        </el-table-column>
        <el-table-column prop="businessType" label="业务类型" width="120">
          <template #default="{ row }">{{ businessTypeMap[row.businessType] || row.businessType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="budgetItem" label="预算项目" min-width="120" show-overflow-tooltip />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="凭证" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.proofImageUrl"
              :src="resolveMediaUrl(row.proofImageUrl)"
              :preview-src-list="[resolveMediaUrl(row.proofImageUrl)]"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 4px; cursor: pointer;"
              preview-teleported
            />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tag || 'info'">
              {{ statusMap[row.status]?.label || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button text type="success" @click="handleApprove(row)">通过</el-button>
              <el-button text type="danger" @click="handleReject(row)">驳回</el-button>
            </template>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.page"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="fetchList"
      />
    </el-card>

    <el-row v-if="activeTab === 'budget'" :gutter="16" class="mt-20">
      <el-col :xs="24" :md="8" v-for="item in enterpriseBudgetCards" :key="item.name">
        <el-card shadow="never" class="budget-card">
          <span>{{ item.name }}</span>
          <strong>{{ formatMoney(item.amount) }}</strong>
          <el-progress :percentage="item.rate" :status="item.rate > 85 ? 'warning' : 'success'" />
          <small>{{ item.desc }}</small>
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="activeTab === 'budget'" class="mt-20" shadow="never">
      <template #header>
        <div class="card-header">
          <span>预算专表</span>
          <el-button type="primary" text @click="createEnterpriseFinanceDocument('budget')">登记预算</el-button>
        </div>
      </template>
      <el-table :data="budgetRows" stripe empty-text="暂无预算专表数据">
        <el-table-column prop="budgetNo" label="预算编号" min-width="150" />
        <el-table-column prop="budgetName" label="预算名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="budgetType" label="类型" width="120" />
        <el-table-column prop="totalAmount" label="预算金额" width="130">
          <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column prop="usedAmount" label="已用" width="120">
          <template #default="{ row }">{{ formatMoney(row.usedAmount) }}</template>
        </el-table-column>
        <el-table-column prop="availableAmount" label="可用" width="120">
          <template #default="{ row }">{{ formatMoney(row.availableAmount) }}</template>
        </el-table-column>
        <el-table-column prop="ownerName" label="负责人" width="120" />
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'voucher'" class="mt-20" shadow="never">
      <template #header>
        <div class="card-header">
          <span>凭证中心</span>
          <div class="card-actions">
            <el-tag effect="plain">自动分录</el-tag>
            <el-button type="primary" text @click="createEnterpriseFinanceDocument('voucher')">登记凭证</el-button>
          </div>
        </div>
      </template>
      <el-table :data="voucherRows" stripe empty-text="暂无已入账凭证">
        <el-table-column prop="voucherNo" label="凭证号" min-width="150" />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="summary" label="摘要" min-width="180" show-overflow-tooltip />
        <el-table-column prop="debitSubject" label="借方科目" min-width="150" />
        <el-table-column prop="creditSubject" label="贷方科目" min-width="150" />
        <el-table-column prop="amount" label="金额" width="120">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="影像" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.proofImageUrl"
              :src="resolveMediaUrl(row.proofImageUrl)"
              :preview-src-list="[resolveMediaUrl(row.proofImageUrl)]"
              fit="cover"
              style="width: 42px; height: 42px; border-radius: 4px; cursor: pointer;"
              preview-teleported
            />
            <span v-else class="text-muted">无</span>
          </template>
        </el-table-column>
        <el-table-column prop="postingStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.postingStatus) === 1 ? 'success' : Number(row.postingStatus) === 2 ? 'danger' : 'warning'">
              {{ Number(row.postingStatus) === 1 ? '已过账' : Number(row.postingStatus) === 2 ? '已冲销' : '未过账' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="originalVoucherId" label="原凭证" width="100">
          <template #default="{ row }">{{ row.originalVoucherId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="reverseReason" label="冲销原因" min-width="150" show-overflow-tooltip />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.id && Number(row.postingStatus) === 0" text type="success" @click="handlePostVoucher(row)">过账</el-button>
            <el-button v-if="row.id && Number(row.postingStatus) === 1" text type="warning" @click="handleRequestReverseVoucher(row)">申请冲销</el-button>
            <el-button v-if="row.id && Number(row.postingStatus) === 1" text type="danger" @click="handleReverseVoucher(row)">直接冲销</el-button>
            <span v-if="!row.id || Number(row.postingStatus) === 2" class="text-muted">--</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'payable'" class="mt-20" shadow="never">
      <template #header><span>往来付款</span></template>
      <el-table :data="payableRows" stripe empty-text="暂无待付款往来">
        <el-table-column prop="recordNo" label="单据编号" min-width="150" />
        <el-table-column prop="budgetItem" label="往来对象/预算项目" min-width="180" show-overflow-tooltip />
        <el-table-column prop="title" label="事项" min-width="180" show-overflow-tooltip />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="amount" label="待付金额" width="130">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tag || 'info'">{{ statusMap[row.status]?.label || '未知' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'asset'" class="mt-20" shadow="never">
      <template #header><span>资产台账</span></template>
      <el-table :data="assetRows" stripe empty-text="暂无资产或物资采购记录">
        <el-table-column prop="recordNo" label="资产单号" min-width="150" />
        <el-table-column prop="title" label="资产名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="budgetItem" label="归口项目" min-width="160" />
        <el-table-column prop="amount" label="原值" width="120">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="预计月折旧" width="130">
          <template #default="{ row }">{{ formatMoney(Number(row.amount || 0) / 36) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '已入账' : '待确认' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'tax'" class="mt-20" shadow="never">
      <template #header><span>合规税务</span></template>
      <el-table :data="complianceRows" stripe empty-text="暂无合规检查项">
        <el-table-column prop="recordNo" label="单据编号" min-width="150" />
        <el-table-column prop="title" label="事项" min-width="180" show-overflow-tooltip />
        <el-table-column prop="checkItem" label="检查项" min-width="160" />
        <el-table-column prop="risk" label="风险" width="110">
          <template #default="{ row }">
            <el-tag :type="row.risk === '高' ? 'danger' : row.risk === '中' ? 'warning' : 'success'">{{ row.risk }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="suggestion" label="处理建议" min-width="220" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'archive'" class="mt-20" shadow="never">
      <template #header><span>电子档案</span></template>
      <el-table :data="archiveRows" stripe empty-text="暂无凭证影像归档">
        <el-table-column prop="recordNo" label="档案编号" min-width="150" />
        <el-table-column prop="title" label="档案标题" min-width="180" show-overflow-tooltip />
        <el-table-column prop="period" label="归档期间" width="110" />
        <el-table-column prop="category" label="分类" width="110">
          <template #default="{ row }">{{ categoryMap[row.category] || row.category }}</template>
        </el-table-column>
        <el-table-column label="影像" width="90">
          <template #default="{ row }">
            <el-image
              :src="resolveMediaUrl(row.proofImageUrl)"
              :preview-src-list="[resolveMediaUrl(row.proofImageUrl)]"
              fit="cover"
              style="width: 42px; height: 42px; border-radius: 4px; cursor: pointer;"
              preview-teleported
            />
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'audit'" class="mt-20" shadow="never">
      <template #header><span>审计日志</span></template>
      <el-table :data="auditRows" stripe empty-text="暂无财务审计记录">
        <el-table-column prop="time" label="时间" min-width="160" />
        <el-table-column prop="action" label="动作" min-width="130" />
        <el-table-column prop="target" label="对象" min-width="180" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人" width="110" />
        <el-table-column prop="risk" label="风险" width="110">
          <template #default="{ row }">
            <el-tag :type="row.risk === '大额待审' ? 'warning' : 'success'">{{ row.risk }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="result" label="结果" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="subsection-toolbar mt-20">
        <strong>审批轨迹</strong>
        <el-tag effect="plain">报销/反结账/科目治理</el-tag>
      </div>
      <el-table :data="approvalActionRows" stripe empty-text="暂无审批动作记录">
        <el-table-column prop="createTime" label="时间" min-width="150" show-overflow-tooltip />
        <el-table-column prop="businessType" label="业务类型" width="130" />
        <el-table-column prop="businessId" label="业务ID" width="90" />
        <el-table-column prop="action" label="动作" width="110" />
        <el-table-column prop="fromApproverId" label="原审批人" width="100" />
        <el-table-column prop="toApproverId" label="新审批人" width="100" />
        <el-table-column prop="statusAfter" label="结果状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.statusAfter]?.tag || 'info'">{{ statusMap[row.statusAfter]?.label || row.statusAfter || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opinion" label="意见" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="subsection-toolbar mt-20">
        <strong>高风险审批</strong>
        <el-tag type="warning" effect="plain">凭证冲销 / 期间反结账</el-tag>
      </div>
      <el-table :data="governanceApprovalRows" stripe empty-text="暂无高风险审批记录">
        <el-table-column prop="createTime" label="申请时间" min-width="150" show-overflow-tooltip />
        <el-table-column prop="businessType" label="业务" width="140" />
        <el-table-column prop="businessId" label="业务ID" width="90" />
        <el-table-column prop="action" label="动作" width="130" />
        <el-table-column prop="fromApproverId" label="申请人" width="100" />
        <el-table-column prop="toApproverId" label="审批人" width="100" />
        <el-table-column prop="statusAfter" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="Number(row.statusAfter) === 0 ? 'warning' : 'success'">
              {{ Number(row.statusAfter) === 0 ? '待审批' : '已通过' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="opinion" label="原因/意见" min-width="180" show-overflow-tooltip />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="Number(row.statusAfter) === 0 && row.action === 'REVERSE_REQUEST'"
              text
              type="success"
              @click="handleApproveReverseRequest(row)"
            >
              通过
            </el-button>
            <el-button
              v-if="Number(row.statusAfter) === 0 && row.action === 'REOPEN_REQUEST'"
              text
              type="success"
              @click="handleApproveReopenRequest(row)"
            >
              通过
            </el-button>
            <span v-if="Number(row.statusAfter) !== 0" class="text-muted">--</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'fund' || activeTab === 'allocation'" class="mt-20" shadow="never">
      <template #header><span>{{ activeTab === 'allocation' ? '经费拨款与资金入账' : '资金账户与对账' }}</span></template>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="账户余额">{{ formatMoney(accountInfo.balance) }}</el-descriptions-item>
        <el-descriptions-item label="累计收入">{{ formatMoney(accountInfo.totalIncome) }}</el-descriptions-item>
        <el-descriptions-item label="累计支出">{{ formatMoney(accountInfo.totalExpense) }}</el-descriptions-item>
        <el-descriptions-item label="待审金额">{{ formatMoney(dashboard.pendingAmount) }}</el-descriptions-item>
        <el-descriptions-item label="预算使用率">{{ dashboard.budgetUsageRate || 0 }}%</el-descriptions-item>
        <el-descriptions-item label="入账规则">审核通过后自动入账</el-descriptions-item>
        <el-descriptions-item v-if="activeTab === 'allocation'" label="拨款监管">支持按财政拨款、学校支持、赞助收入分类追踪</el-descriptions-item>
        <el-descriptions-item v-if="activeTab === 'allocation'" label="监管重点">关注预算占用、余额留存和大额支出审批</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="activeTab === 'ledger'" class="mt-20" shadow="never">
      <template #header><span>企业总账专表</span></template>
      <el-table :data="dedicatedLedgerRows" stripe empty-text="暂无企业总账数据">
        <el-table-column prop="documentNo" label="单据号" min-width="150" />
        <el-table-column prop="ledgerType" label="账簿" width="120" />
        <el-table-column prop="businessType" label="业务" width="130" />
        <el-table-column prop="period" label="期间" width="100" />
        <el-table-column prop="debitAmount" label="借方" width="120">
          <template #default="{ row }">{{ formatMoney(row.debitAmount) }}</template>
        </el-table-column>
        <el-table-column prop="creditAmount" label="贷方" width="120">
          <template #default="{ row }">{{ formatMoney(row.creditAmount) }}</template>
        </el-table-column>
        <el-table-column prop="balanceAmount" label="余额影响" width="120">
          <template #default="{ row }">{{ formatMoney(row.balanceAmount) }}</template>
        </el-table-column>
        <el-table-column prop="summary" label="摘要" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="subsection-toolbar mt-20">
        <strong>会计科目</strong>
        <el-button type="primary" text @click="handleCreateFinanceSubject">新增默认科目</el-button>
      </div>
      <el-table :data="subjectTreeRows" row-key="subjectCode" default-expand-all stripe empty-text="暂无会计科目">
        <el-table-column prop="subjectCode" label="科目编码" width="110" />
        <el-table-column prop="subjectName" label="科目名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="subjectType" label="科目类型" width="120" />
        <el-table-column prop="parentSubjectName" label="上级科目" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.parentSubjectName || row.parentCode || '顶级科目' }}</template>
        </el-table-column>
        <el-table-column prop="direction" label="方向" width="90" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">{{ Number(row.status) === 1 ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" @click="handleEditFinanceSubject(row)">编辑</el-button>
            <el-button text @click="handleToggleFinanceSubjectStatus(row)">{{ Number(row.status) === 1 ? '停用' : '启用' }}</el-button>
            <el-button text type="danger" @click="handleDeleteFinanceSubject(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'allocation'" class="mt-20" shadow="never">
      <template #header>
        <div class="card-header">
          <span>拨款专表</span>
          <el-button type="primary" text @click="createEnterpriseFinanceDocument('allocation')">登记拨款</el-button>
        </div>
      </template>
      <el-table :data="allocationRows" stripe empty-text="暂无拨款专表数据">
        <el-table-column prop="allocationNo" label="拨款编号" min-width="150" />
        <el-table-column prop="sourceName" label="资金来源" min-width="160" show-overflow-tooltip />
        <el-table-column prop="sourceType" label="来源类型" width="130" />
        <el-table-column prop="receiverTenantId" label="接收租户" width="110" />
        <el-table-column prop="amount" label="拨款金额" width="130">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="arrivalStatus" label="到账" width="90">
          <template #default="{ row }">
            <el-tag :type="Number(row.arrivalStatus) === 1 ? 'success' : 'warning'">{{ Number(row.arrivalStatus) === 1 ? '已到账' : '待确认' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="180" show-overflow-tooltip />
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'reimbursement'" class="mt-20" shadow="never">
      <template #header>
        <div class="card-header">
          <span>报销专表</span>
          <div class="card-actions">
            <el-radio-group v-model="reimbursementScope" size="small">
              <el-radio-button label="all">全部报销</el-radio-button>
              <el-radio-button label="mine">待我审批</el-radio-button>
            </el-radio-group>
            <el-button type="primary" text @click="createEnterpriseFinanceDocument('reimbursement')">登记报销</el-button>
          </div>
        </div>
      </template>
      <div class="subsection-toolbar">
        <strong>待我审批 {{ enterpriseFinance.pendingApprovalCount || pendingApprovalReimbursementRows.length }} 单</strong>
        <el-tag effect="plain">金额 {{ formatMoney(enterpriseFinance.pendingApprovalAmount || 0) }}</el-tag>
      </div>
      <div class="approval-template-panel">
        <div class="subsection-toolbar">
          <strong>审批模板</strong>
          <div class="card-actions">
            <el-button size="small" type="primary" plain @click="handleCreateDefaultApprovalTemplate">生成默认模板</el-button>
            <el-button size="small" @click="fetchEnterpriseFinance">刷新模板</el-button>
          </div>
        </div>
        <el-table :data="approvalTemplateRows" size="small" stripe empty-text="暂无审批模板">
          <el-table-column prop="templateName" label="模板名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="businessType" label="业务类型" width="130" />
          <el-table-column prop="minAmount" label="起始金额" width="120">
            <template #default="{ row }">{{ formatMoney(row.minAmount || 0) }}</template>
          </el-table-column>
          <el-table-column prop="maxAmount" label="最高金额" width="120">
            <template #default="{ row }">{{ row.maxAmount ? formatMoney(row.maxAmount) : '不限' }}</template>
          </el-table-column>
          <el-table-column prop="nodes" label="审批节点" min-width="220">
            <template #default="{ row }">
              <el-tag
                v-for="node in row.nodes || []"
                :key="`${row.id}-${node.stepNo}-${node.approverId}`"
                class="node-tag"
                effect="plain"
              >
                {{ node.stepNo }}.{{ node.nodeName || '审批' }} {{ node.approverName || node.approverId || '未指定' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="Number(row.status) === 1 ? 'success' : 'info'">{{ Number(row.status) === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="110" fixed="right">
            <template #default="{ row }">
              <el-button v-if="Number(row.status) !== 1" size="small" text type="primary" @click="handleEnableApprovalTemplate(row)">启用</el-button>
              <span v-else class="text-muted">当前使用</span>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="approvalNodeRows.length" class="template-node-summary">
          当前租户共 {{ approvalNodeRows.length }} 个审批节点，报销单提交后会自动进入第一个节点。
        </div>
      </div>
      <el-table :data="visibleReimbursementRows" stripe empty-text="暂无报销专表数据">
        <el-table-column prop="reimbursementNo" label="报销编号" min-width="150" />
        <el-table-column prop="applicantName" label="申请人" width="120" />
        <el-table-column prop="expenseSubject" label="费用科目" min-width="150" show-overflow-tooltip />
        <el-table-column prop="amount" label="报销金额" width="130">
          <template #default="{ row }">{{ formatMoney(row.amount) }}</template>
        </el-table-column>
        <el-table-column prop="invoiceCount" label="票据数" width="90" />
        <el-table-column prop="currentApproverId" label="当前审批人" width="110">
          <template #default="{ row }">{{ row.currentApproverId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.tag || 'info'">{{ statusMap[row.status]?.label || '未知' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="180" show-overflow-tooltip />
        <el-table-column label="审批操作" width="260" fixed="right">
          <template #default="{ row }">
            <template v-if="Number(row.status) === 0">
              <el-button text type="success" @click="handleApproveReimbursement(row)">通过</el-button>
              <el-button text type="danger" @click="handleRejectReimbursement(row)">驳回</el-button>
              <el-button text @click="handleTransferReimbursement(row)">转交</el-button>
              <el-button text @click="handleAddSignReimbursement(row)">加签</el-button>
            </template>
            <el-button v-if="Number(row.status) !== 3" text type="info" @click="handleWithdrawReimbursement(row)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card v-if="activeTab === 'report'" class="mt-20" shadow="never">
      <template #header>
        <div class="card-header">
          <span>财务报表</span>
          <div class="card-actions">
            <el-button @click="fetchReport">生成报表</el-button>
            <el-button @click="handleExportFinanceReport">导出报表</el-button>
            <el-button type="warning" @click="handleClosePeriod(false)">期间结账</el-button>
            <el-button type="warning" plain @click="handleRequestReopenPeriod">申请反结账</el-button>
            <el-button type="danger" plain @click="handleReopenPeriod">直接反结账</el-button>
            <el-button type="primary" plain @click="handleCreateReconciliation">生成对账</el-button>
          </div>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :xs="24" :md="8">
          <el-statistic title="报表收入" :value="Number(report.income || 0)" :precision="2" />
        </el-col>
        <el-col :xs="24" :md="8">
          <el-statistic title="报表支出" :value="Number(report.expense || 0)" :precision="2" />
        </el-col>
        <el-col :xs="24" :md="8">
          <el-statistic title="待审金额" :value="Number(report.pending || 0)" :precision="2" />
        </el-col>
      </el-row>
      <el-table :data="report.categoryBreakdown || []" stripe class="mt-20">
        <el-table-column prop="category" label="分类" />
        <el-table-column prop="income" label="收入" />
        <el-table-column prop="expense" label="支出" />
        <el-table-column prop="pending" label="待审" />
      </el-table>

      <el-row :gutter="16" class="mt-20">
        <el-col :xs="24" :lg="12">
          <el-table :data="periodRows" stripe empty-text="暂无结账记录">
            <el-table-column prop="period" label="期间" width="100" />
            <el-table-column prop="incomeAmount" label="收入" width="110">
              <template #default="{ row }">{{ formatMoney(row.incomeAmount) }}</template>
            </el-table-column>
            <el-table-column prop="expenseAmount" label="支出" width="110">
              <template #default="{ row }">{{ formatMoney(row.expenseAmount) }}</template>
            </el-table-column>
            <el-table-column prop="pendingCount" label="未完成" width="90" />
            <el-table-column prop="closeType" label="类型" width="90" />
            <el-table-column prop="closedTime" label="结账时间" min-width="150" show-overflow-tooltip />
          </el-table>
        </el-col>
        <el-col :xs="24" :lg="12">
          <el-table :data="reconciliationRows" stripe empty-text="暂无对账记录">
            <el-table-column prop="reconciliationNo" label="对账号" min-width="150" />
            <el-table-column prop="period" label="期间" width="100" />
            <el-table-column prop="accountBalance" label="账户余额" width="120">
              <template #default="{ row }">{{ formatMoney(row.accountBalance) }}</template>
            </el-table-column>
            <el-table-column prop="ledgerBalance" label="账簿余额" width="120">
              <template #default="{ row }">{{ formatMoney(row.ledgerBalance) }}</template>
            </el-table-column>
            <el-table-column prop="differenceAmount" label="差异" width="100">
              <template #default="{ row }">{{ formatMoney(row.differenceAmount) }}</template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="Number(row.status) === 1 ? 'success' : 'warning'">{{ Number(row.status) === 1 ? '平衡' : '待核对' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </el-card>

    <!-- 新增记录对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="560px" :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="类型" prop="type">
          <el-select v-model="formData.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="收入" value="income" />
            <el-option label="支出" value="expense" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="formData.amount"
            :min="0.01"
            :precision="2"
            :step="100"
            placeholder="请输入金额"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="formData.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="会费" value="dues" />
            <el-option label="赞助" value="sponsorship" />
            <el-option label="财政拨款" value="grant" />
            <el-option label="报销" value="reimbursement" />
            <el-option label="活动费" value="activity" />
            <el-option label="物资" value="supplies" />
            <el-option label="其他" value="other" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务类型">
          <el-select v-model="formData.businessType" placeholder="请选择业务类型" style="width: 100%">
            <el-option label="收入入账" value="INCOME" />
            <el-option label="支出付款" value="EXPENSE" />
            <el-option label="报销" value="REIMBURSEMENT" />
            <el-option label="预算" value="BUDGET" />
            <el-option label="资产采购" value="ASSET" />
            <el-option label="往来付款" value="PAYMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="预算项目">
          <el-input v-model="formData.budgetItem" placeholder="如：活动经费、物资采购、竞赛支持" maxlength="100" />
        </el-form-item>
        <el-form-item label="会计期间">
          <el-date-picker v-model="formData.period" type="month" value-format="YYYY-MM" placeholder="请选择期间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model="formData.title" placeholder="请输入标题" maxlength="100" />
        </el-form-item>
        <el-form-item label="说明" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入说明"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="凭证图片">
          <el-upload
            class="proof-uploader"
            :auto-upload="false"
            :show-file-list="true"
            :limit="1"
            accept="image/*"
            :on-change="handleFileChange"
            :on-remove="handleFileRemove"
          >
            <el-button type="primary" size="small">选择图片</el-button>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 驳回原因对话框 -->
    <el-dialog v-model="rejectDialogVisible" title="驳回原因" width="400px" :close-on-click-modal="false">
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入驳回原因"
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="approverDialogVisible"
      :title="approverDialogTitle"
      width="520px"
      :close-on-click-modal="false"
      @closed="handleApproverDialogClosed"
    >
      <el-form label-width="100px">
        <el-form-item label="审批人候选">
          <el-select
            v-model="approverForm.nextApproverId"
            filterable
            remote
            reserve-keyword
            clearable
            :remote-method="loadFinanceApproverCandidates"
            :loading="approverLoading"
            placeholder="搜索姓名或账号，选择审批人"
            style="width: 100%"
          >
            <el-option
              v-for="candidate in approverCandidates"
              :key="candidate.nextApproverId"
              :label="candidate.label"
              :value="candidate.nextApproverId"
            >
              <div class="approver-option">
                <strong>{{ candidate.displayName }}</strong>
                <span>{{ candidate.username || '无账号' }} · {{ candidate.roleName || '未分配角色' }}</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input
            v-model.trim="approverForm.opinion"
            type="textarea"
            :rows="3"
            maxlength="200"
            show-word-limit
            placeholder="请输入审批意见"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelApproverSelection">取消</el-button>
        <el-button type="primary" @click="confirmApproverSelection">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Minus } from '@element-plus/icons-vue'
import {
  getFinanceAccount,
  getFinanceDashboard,
  getFinanceEnterpriseOverview,
  getFinanceReportSummary,
  getFinanceRecords,
  createFinanceRecord,
  getFinanceBudgets,
  createFinanceBudget,
  getFinanceAllocations,
  createFinanceAllocation,
  getFinanceReimbursements,
  getFinancePendingApprovalReimbursements,
  createFinanceReimbursement,
  getFinanceVouchers,
  createFinanceVoucher,
  postFinanceVoucher,
  reverseFinanceVoucher,
  requestReverseFinanceVoucher,
  approveReverseFinanceVoucher,
  getFinanceLedger,
  getFinanceAuditLogs,
  getFinancePeriods,
  closeFinancePeriod,
  reopenFinancePeriod,
  requestReopenFinancePeriod,
  approveReopenFinancePeriod,
  getFinanceGovernanceApprovals,
  getFinanceReconciliations,
  createFinanceReconciliation,
  getFinanceSubjects,
  createFinanceSubject,
  updateFinanceSubject,
  deleteFinanceSubject,
  updateFinanceSubjectStatus,
  getFinanceApprovalTemplates,
  createFinanceApprovalTemplate,
  enableFinanceApprovalTemplate,
  createDefaultFinanceApprovalTemplate,
  approveFinanceReimbursement,
  rejectFinanceReimbursement,
  transferFinanceReimbursement,
  addSignFinanceReimbursement,
  withdrawFinanceReimbursement,
  exportFinanceReport,
  approveFinanceRecord,
  rejectFinanceRecord
} from '@/api/finance'
import { getUserPage } from '@/api/user'
import {
  buildFinanceAuditRows,
  buildFinanceDashboardSummary,
  buildFinanceModuleCards,
  buildFinanceVoucherRows,
  normalizeFinanceRecord,
  toFinanceApiType
} from '@/utils/adminFinance'
import { uploadManagedFile } from '@/utils/fileUpload'
import { resolveMediaUrl } from '@/utils/mediaUrl'
import { echarts } from '@/utils/echarts'

const route = useRoute()
const financeRouteTabMap = {
  '/admin/finance': 'dashboard',
  '/admin/finance/budget': 'budget',
  '/admin/finance/allocation': 'allocation',
  '/admin/finance/reimbursement': 'reimbursement',
  '/admin/finance/voucher': 'voucher',
  '/admin/finance/ledger': 'ledger',
  '/admin/finance/report': 'report',
  '/admin/finance/audit': 'audit'
}

const statusMap = {
  0: { label: '待审核', tag: 'warning' },
  1: { label: '已通过', tag: 'success' },
  2: { label: '已驳回', tag: 'danger' }
}

statusMap[3] = { label: '已撤回', tag: 'info' }

const categoryMap = {
  dues: '会费',
  sponsorship: '赞助',
  grant: '财政拨款',
  reimbursement: '报销',
  activity: '活动费',
  supplies: '物资',
  other: '其他'
}

const businessTypeMap = {
  INCOME: '收入入账',
  EXPENSE: '支出付款',
  REIMBURSEMENT: '报销',
  BUDGET: '预算',
  PAYMENT: '付款',
  ASSET: '资产采购'
}

const loading = ref(false)
const submitLoading = ref(false)
const dataList = ref([])
const filterType = ref('')
const filterStatus = ref(null)
const filterBusinessType = ref('')
const filterPeriod = ref('')
const filterKeyword = ref('')
const activeTab = ref(financeRouteTabMap[route.path] || 'dashboard')
const reimbursementScope = ref('all')
const trendChartRef = ref(null)
const report = ref({})
let trendChart = null

const accountInfo = reactive({
  balance: '0.00',
  totalIncome: '0.00',
  totalExpense: '0.00'
})
const dashboard = reactive({
  pendingAmount: 0,
  budgetUsageRate: 0,
  categoryBreakdown: [],
  monthlyTrend: [],
  riskAlerts: [],
  recentRecords: []
})
const enterpriseFinance = reactive({
  budgets: [],
  allocations: [],
  reimbursements: [],
  vouchers: [],
  ledger: [],
  auditLogs: [],
  periods: [],
  reconciliations: [],
  subjects: [],
  approvalActions: [],
  governanceApprovals: [],
  approvalTemplates: [],
  approvalTemplateNodes: [],
  pendingApprovalReimbursements: [],
  pendingApprovalCount: 0,
  pendingApprovalAmount: 0,
  budgetTotal: 0,
  budgetUsed: 0,
  pendingReimbursement: 0,
  allocationTotal: 0
})

const pagination = reactive({
  page: 1,
  size: 10,
  total: 0
})

// Dialog state
const dialogVisible = ref(false)
const formRef = ref(null)
const formData = reactive({
  type: 'income',
  amount: null,
  category: '',
  businessType: '',
  budgetItem: '',
  period: '',
  title: '',
  description: ''
})
const proofFile = ref(null)

const formRules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }]
}

const dialogTitle = computed(() => formData.type === 'income' ? '新增收入' : '新增支出')

const approverDialogVisible = ref(false)
const approverDialogTitle = ref('选择审批人')
const approverCandidates = ref([])
const approverLoading = ref(false)
const approverResolve = ref(null)
const approverForm = reactive({
  nextApproverId: null,
  opinion: ''
})

const budgetCards = computed(() => {
  const categories = dashboard.categoryBreakdown || []
  const activity = categories.find((item) => item.category === 'activity') || {}
  const supplies = categories.find((item) => item.category === 'supplies') || {}
  const reimbursement = categories.find((item) => item.category === 'reimbursement') || {}
  return [
    { name: '活动经费', amount: activity.expense || 0, rate: Math.min(100, Math.round(Number(activity.expense || 0) / Math.max(Number(accountInfo.totalIncome || 1), 1) * 100)), desc: '讲座、培训、比赛执行费用' },
    { name: '物资采购', amount: supplies.expense || 0, rate: Math.min(100, Math.round(Number(supplies.expense || 0) / Math.max(Number(accountInfo.totalIncome || 1), 1) * 100)), desc: '设备、耗材、宣传物料' },
    { name: '报销池', amount: reimbursement.pending || 0, rate: Math.min(100, Math.round(Number(reimbursement.pending || 0) / Math.max(Number(accountInfo.balance || 1), 1) * 100)), desc: '成员待审批报销单据' }
  ]
})

const enterpriseBudgetCards = computed(() => {
  if (budgetRows.value.length) {
    const total = budgetRows.value.reduce((sum, item) => sum + Number(item.totalAmount || 0), 0)
    const used = budgetRows.value.reduce((sum, item) => sum + Number(item.usedAmount || 0), 0)
    const occupied = budgetRows.value.reduce((sum, item) => sum + Number(item.occupiedAmount || 0), 0)
    return [
      { name: '预算总额', amount: total, rate: total ? Math.min(100, Math.round((used / total) * 100)) : 0, desc: '所有专表预算汇总' },
      { name: '已执行', amount: used, rate: total ? Math.min(100, Math.round((used / total) * 100)) : 0, desc: '已报销、已付款和已入账金额' },
      { name: '已占用', amount: occupied, rate: total ? Math.min(100, Math.round((occupied / total) * 100)) : 0, desc: '审批中或已冻结金额' }
    ]
  }
  return budgetCards.value
})

const financeModuleCards = computed(() => buildFinanceModuleCards({
  account: accountInfo,
  records: dataList.value
}))

const normalizeEnterpriseRow = (row = {}) => ({
  ...row,
  budgetNo: row.budgetNo || row.budget_no,
  budgetName: row.budgetName || row.budget_name,
  budgetType: row.budgetType || row.budget_type,
  totalAmount: row.totalAmount ?? row.total_amount,
  occupiedAmount: row.occupiedAmount ?? row.occupied_amount,
  usedAmount: row.usedAmount ?? row.used_amount,
  availableAmount: row.availableAmount ?? row.available_amount,
  allocationNo: row.allocationNo || row.allocation_no,
  sourceName: row.sourceName || row.source_name,
  sourceType: row.sourceType || row.source_type,
  receiverTenantId: row.receiverTenantId ?? row.receiver_tenant_id,
  arrivalStatus: row.arrivalStatus ?? row.arrival_status,
  reimbursementNo: row.reimbursementNo || row.reimbursement_no,
  applicantName: row.applicantName || row.applicant_name,
  expenseSubject: row.expenseSubject || row.expense_subject,
  invoiceCount: row.invoiceCount ?? row.invoice_count,
  currentApproverId: row.currentApproverId ?? row.current_approver_id,
  approvalTemplateId: row.approvalTemplateId ?? row.approval_template_id,
  currentStepNo: row.currentStepNo ?? row.current_step_no,
  voucherNo: row.voucherNo || row.voucher_no,
  sourceId: row.sourceId ?? row.source_id,
  postingStatus: row.postingStatus ?? row.posting_status,
  reverseVoucherId: row.reverseVoucherId ?? row.reverse_voucher_id,
  originalVoucherId: row.originalVoucherId ?? row.original_voucher_id,
  reverseReason: row.reverseReason || row.reverse_reason,
  ledgerType: row.ledgerType || row.ledger_type,
  documentNo: row.documentNo || row.document_no,
  businessType: row.businessType || row.business_type,
  debitAmount: row.debitAmount ?? row.debit_amount,
  creditAmount: row.creditAmount ?? row.credit_amount,
  balanceAmount: row.balanceAmount ?? row.balance_amount,
  closeType: row.closeType || row.close_type,
  incomeAmount: row.incomeAmount ?? row.income_amount,
  expenseAmount: row.expenseAmount ?? row.expense_amount,
  voucherCount: row.voucherCount ?? row.voucher_count,
  ledgerCount: row.ledgerCount ?? row.ledger_count,
  pendingCount: row.pendingCount ?? row.pending_count,
  closedBy: row.closedBy ?? row.closed_by,
  closedTime: row.closedTime || row.closed_time,
  reconciliationNo: row.reconciliationNo || row.reconciliation_no,
  accountBalance: row.accountBalance ?? row.account_balance,
  ledgerBalance: row.ledgerBalance ?? row.ledger_balance,
  differenceAmount: row.differenceAmount ?? row.difference_amount,
  handlerId: row.handlerId ?? row.handler_id,
  riskLevel: row.riskLevel || row.risk_level,
  subjectCode: row.subjectCode || row.subject_code,
  subjectName: row.subjectName || row.subject_name,
  subjectType: row.subjectType || row.subject_type,
  parentCode: row.parentCode || row.parent_code,
  direction: row.direction,
  statusAfter: row.statusAfter ?? row.status_after,
  businessId: row.businessId ?? row.business_id,
  fromApproverId: row.fromApproverId ?? row.from_approver_id,
  toApproverId: row.toApproverId ?? row.to_approver_id,
  actionId: row.actionId ?? row.action_id ?? row.id,
  templateName: row.templateName || row.template_name,
  minAmount: row.minAmount ?? row.min_amount,
  maxAmount: row.maxAmount ?? row.max_amount,
  stepNo: row.stepNo ?? row.step_no,
  nodeName: row.nodeName || row.node_name,
  approverId: row.approverId ?? row.approver_id,
  approverName: row.approverName || row.approver_name,
  approverRole: row.approverRole || row.approver_role,
  nodes: Array.isArray(row.nodes) ? row.nodes.map(normalizeEnterpriseRow) : [],
  opinion: row.opinion,
  createTime: row.createTime || row.create_time
})

const budgetRows = computed(() => enterpriseFinance.budgets.map(normalizeEnterpriseRow))
const allocationRows = computed(() => enterpriseFinance.allocations.map(normalizeEnterpriseRow))
const reimbursementRows = computed(() => enterpriseFinance.reimbursements.map(normalizeEnterpriseRow))
const pendingApprovalReimbursementRows = computed(() => enterpriseFinance.pendingApprovalReimbursements.map(normalizeEnterpriseRow))
const visibleReimbursementRows = computed(() => reimbursementScope.value === 'mine'
  ? pendingApprovalReimbursementRows.value
  : reimbursementRows.value
)
const dedicatedVoucherRows = computed(() => enterpriseFinance.vouchers.map(normalizeEnterpriseRow))
const dedicatedLedgerRows = computed(() => enterpriseFinance.ledger.map(normalizeEnterpriseRow))
const dedicatedAuditRows = computed(() => enterpriseFinance.auditLogs.map(normalizeEnterpriseRow))
const periodRows = computed(() => enterpriseFinance.periods.map(normalizeEnterpriseRow))
const reconciliationRows = computed(() => enterpriseFinance.reconciliations.map(normalizeEnterpriseRow))
const subjectRows = computed(() => enterpriseFinance.subjects.map(normalizeEnterpriseRow))
const subjectParentOptions = computed(() => subjectRows.value.map((row) => ({
  label: `${row.subjectCode} ${row.subjectName}`,
  value: row.subjectCode,
  subjectName: row.subjectName
})))
const subjectTreeRows = computed(() => {
  const rows = subjectRows.value.map((row) => ({ ...row, children: [] }))
  const byCode = new Map(rows.map((row) => [row.subjectCode, row]))
  const roots = []
  rows.forEach((row) => {
    const parent = row.parentCode ? byCode.get(row.parentCode) : null
    row.parentSubjectName = parent?.subjectName || ''
    if (parent && parent.subjectCode !== row.subjectCode) {
      parent.children.push(row)
    } else {
      roots.push(row)
    }
  })
  return roots
})
const approvalActionRows = computed(() => enterpriseFinance.approvalActions.map(normalizeEnterpriseRow))
const governanceApprovalRows = computed(() => enterpriseFinance.governanceApprovals.map(normalizeEnterpriseRow))
const approvalTemplateRows = computed(() => enterpriseFinance.approvalTemplates.map(normalizeEnterpriseRow))
const approvalNodeRows = computed(() => enterpriseFinance.approvalTemplateNodes.map(normalizeEnterpriseRow))

const voucherRows = computed(() => {
  if (dedicatedVoucherRows.value.length) {
    return dedicatedVoucherRows.value.map((row) => ({
      id: row.id,
      voucherNo: row.voucherNo,
      period: row.period,
      summary: row.summary,
      debitSubject: row.sourceType || '业务凭证',
      creditSubject: Number(row.postingStatus) === 1 ? '已过账' : Number(row.postingStatus) === 2 ? '已冲销' : '未过账',
      amount: row.amount,
      postingStatus: row.postingStatus,
      reverseVoucherId: row.reverseVoucherId,
      originalVoucherId: row.originalVoucherId,
      reverseReason: row.reverseReason,
      proofImageUrl: row.proofImageUrl
    }))
  }
  return buildFinanceVoucherRows(dataList.value)
})

const payableRows = computed(() => dataList.value.filter((record) =>
  record.businessType === 'PAYMENT' || (record.type === 'expense' && Number(record.status) === 0)
))

const assetRows = computed(() => dataList.value.filter((record) =>
  record.businessType === 'ASSET' || record.category === 'supplies'
))

const complianceRows = computed(() => dataList.value
  .filter((record) => Number(record.status) === 0 || !record.proofImageUrl || Number(record.amount || 0) >= 1000)
  .map((record) => ({
    ...record,
    checkItem: !record.proofImageUrl ? '缺少凭证影像' : Number(record.amount || 0) >= 1000 ? '大额支出复核' : '待审核单据',
    risk: !record.proofImageUrl || Number(record.amount || 0) >= 3000 ? '高' : Number(record.amount || 0) >= 1000 ? '中' : '低',
    suggestion: !record.proofImageUrl ? '请补充票据或审批附件后再入账' : '按社团经费制度完成二次复核'
  })))

const archiveRows = computed(() => dataList.value.filter((record) => record.proofImageUrl))

const auditRows = computed(() => {
  if (dedicatedAuditRows.value.length) {
    return dedicatedAuditRows.value.map((row) => ({
      time: row.createTime,
      action: row.action,
      target: `${row.businessType || '-'} #${row.businessId || '-'}`,
      operator: row.operatorName || row.operatorId || '-',
      risk: row.riskLevel || 'LOW',
      result: row.result
    }))
  }
  return buildFinanceAuditRows(dataList.value)
})

// Reject dialog
const rejectDialogVisible = ref(false)
const rejectReason = ref('')
const currentRejectId = ref(null)

const fetchAccount = async () => {
  try {
    const res = await getFinanceAccount()
    if (res.code === 200 && res.data) {
      accountInfo.balance = res.data.balance ?? '0.00'
      accountInfo.totalIncome = res.data.totalIncome ?? '0.00'
      accountInfo.totalExpense = res.data.totalExpense ?? '0.00'
    }
  } catch (e) {
    console.error('获取财务账户失败:', e)
  }
}

function applyDashboard(data = {}) {
  if (data.account) {
    accountInfo.balance = data.account.balance ?? accountInfo.balance
    accountInfo.totalIncome = data.account.totalIncome ?? accountInfo.totalIncome
    accountInfo.totalExpense = data.account.totalExpense ?? accountInfo.totalExpense
  }
  const fallback = buildFinanceDashboardSummary({
    account: accountInfo,
    records: data.recentRecords || dataList.value
  })
  dashboard.pendingAmount = data.pendingAmount ?? fallback.pendingAmount
  dashboard.budgetUsageRate = Number(data.budgetUsageRate ?? fallback.budgetUsageRate)
  dashboard.categoryBreakdown = data.categoryBreakdown || fallback.categoryBreakdown
  dashboard.monthlyTrend = data.monthlyTrend || []
  dashboard.riskAlerts = data.riskAlerts || fallback.riskAlerts
  dashboard.recentRecords = data.recentRecords || []
  renderTrendChart()
}

const fetchDashboard = async () => {
  try {
    const res = await getFinanceDashboard()
    if (res.code === 200 && res.data) {
      applyDashboard(res.data)
    }
  } catch (e) {
    applyDashboard()
  }
}

const fetchEnterpriseFinance = async () => {
  try {
    const overviewRes = await getFinanceEnterpriseOverview()
    if (overviewRes.code === 200 && overviewRes.data) {
      enterpriseFinance.budgets = overviewRes.data.budgets || []
      enterpriseFinance.allocations = overviewRes.data.allocations || []
      enterpriseFinance.reimbursements = overviewRes.data.reimbursements || []
      enterpriseFinance.vouchers = overviewRes.data.vouchers || []
      enterpriseFinance.ledger = overviewRes.data.ledger || []
      enterpriseFinance.auditLogs = overviewRes.data.auditLogs || []
      enterpriseFinance.periods = overviewRes.data.periods || []
      enterpriseFinance.reconciliations = overviewRes.data.reconciliations || []
      enterpriseFinance.subjects = overviewRes.data.subjects || []
      enterpriseFinance.approvalActions = overviewRes.data.approvalActions || []
      enterpriseFinance.governanceApprovals = overviewRes.data.governanceApprovals || []
      enterpriseFinance.approvalTemplates = overviewRes.data.approvalTemplates || []
      enterpriseFinance.approvalTemplateNodes = overviewRes.data.approvalTemplateNodes || []
      enterpriseFinance.pendingApprovalReimbursements = overviewRes.data.pendingApprovalReimbursements || []
      enterpriseFinance.pendingApprovalCount = overviewRes.data.pendingApprovalCount || 0
      enterpriseFinance.pendingApprovalAmount = overviewRes.data.pendingApprovalAmount || 0
      enterpriseFinance.budgetTotal = overviewRes.data.budgetTotal || 0
      enterpriseFinance.budgetUsed = overviewRes.data.budgetUsed || 0
      enterpriseFinance.pendingReimbursement = overviewRes.data.pendingReimbursement || 0
      enterpriseFinance.allocationTotal = overviewRes.data.allocationTotal || 0
      return
    }
  } catch (error) {
    console.warn('load finance enterprise overview failed:', error)
  }

  const requests = [
    getFinanceBudgets().then((res) => { enterpriseFinance.budgets = res.code === 200 ? (res.data || []) : [] }),
    getFinanceAllocations().then((res) => { enterpriseFinance.allocations = res.code === 200 ? (res.data || []) : [] }),
    getFinanceReimbursements().then((res) => { enterpriseFinance.reimbursements = res.code === 200 ? (res.data || []) : [] }),
    getFinanceVouchers().then((res) => { enterpriseFinance.vouchers = res.code === 200 ? (res.data || []) : [] }),
    getFinanceLedger().then((res) => { enterpriseFinance.ledger = res.code === 200 ? (res.data || []) : [] }),
    getFinanceAuditLogs().then((res) => { enterpriseFinance.auditLogs = res.code === 200 ? (res.data || []) : [] }),
    getFinancePeriods().then((res) => { enterpriseFinance.periods = res.code === 200 ? (res.data || []) : [] }),
    getFinanceReconciliations().then((res) => { enterpriseFinance.reconciliations = res.code === 200 ? (res.data || []) : [] }),
    getFinanceSubjects().then((res) => { enterpriseFinance.subjects = res.code === 200 ? (res.data || []) : [] }),
    getFinanceGovernanceApprovals().then((res) => { enterpriseFinance.governanceApprovals = res.code === 200 ? (res.data || []) : [] }),
    getFinanceApprovalTemplates().then((res) => {
      const rows = res.code === 200 ? (res.data || []) : []
      enterpriseFinance.approvalTemplates = rows
      enterpriseFinance.approvalTemplateNodes = rows.flatMap((row) => row.nodes || [])
    }),
    getFinancePendingApprovalReimbursements().then((res) => {
      const rows = res.code === 200 ? (res.data || []) : []
      enterpriseFinance.pendingApprovalReimbursements = rows
      enterpriseFinance.pendingApprovalCount = rows.length
      enterpriseFinance.pendingApprovalAmount = rows.reduce((sum, row) => sum + Number(row.amount || 0), 0)
    })
  ]
  await Promise.allSettled(requests)
}

const fetchList = async () => {
  loading.value = true
  try {
    const params = {
      page: pagination.page,
      size: pagination.size
    }
    if (filterType.value) {
      params.type = toFinanceApiType(filterType.value)
    }
    if (filterStatus.value !== null && filterStatus.value !== '') {
      params.status = filterStatus.value
    }
    if (filterBusinessType.value) {
      params.businessType = filterBusinessType.value
    }
    if (filterPeriod.value) {
      params.period = filterPeriod.value
    }
    if (filterKeyword.value) {
      params.keyword = filterKeyword.value
    }
    const res = await getFinanceRecords(params)
    if (res.code === 200 && res.data) {
      const records = res.data.records || res.data || []
      dataList.value = records.map(normalizeFinanceRecord)
      pagination.total = res.data.total || records.length
    } else {
      dataList.value = []
      pagination.total = 0
    }
  } catch (e) {
    console.error('获取财务记录失败:', e)
    ElMessage.error('获取财务记录失败')
    dataList.value = []
  } finally {
    loading.value = false
  }
}

const handleFilterChange = () => {
  pagination.page = 1
  fetchList()
}

const resetFilter = () => {
  filterType.value = ''
  filterStatus.value = null
  filterBusinessType.value = ''
  filterPeriod.value = ''
  filterKeyword.value = ''
  pagination.page = 1
  fetchList()
}

const handleAdd = (type) => {
  formData.type = type
  formData.amount = null
  formData.category = ''
  formData.businessType = type === 'income' ? 'INCOME' : 'EXPENSE'
  formData.budgetItem = ''
  formData.period = new Date().toISOString().slice(0, 7)
  formData.title = ''
  formData.description = ''
  proofFile.value = null
  dialogVisible.value = true
}

const handleFileChange = (file) => {
  proofFile.value = file.raw || null
}

const handleFileRemove = () => {
  proofFile.value = null
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    const payload = {
      type: toFinanceApiType(formData.type),
      amount: formData.amount,
      category: formData.category,
      businessType: formData.businessType,
      budgetItem: formData.budgetItem,
      period: formData.period,
      title: formData.title,
      description: formData.description
    }

    // Upload proof image if provided
    if (proofFile.value) {
      try {
        const { storedValue } = await uploadManagedFile({ file: proofFile.value }, 'finance')
        payload.proofImageUrl = storedValue
      } catch (e) {
        console.error('凭证图片上传失败:', e)
        ElMessage.warning('凭证图片上传失败，记录将不含凭证图片')
      }
    }

    const res = await createFinanceRecord(payload)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      dialogVisible.value = false
      refreshAll()
    } else {
      ElMessage.error(res.msg || '创建失败')
    }
  } catch (e) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    submitLoading.value = false
  }
}

const handleApprove = async (row) => {
  await ElMessageBox.confirm('确定通过该财务记录吗？', '提示', { type: 'success' })
  try {
    const res = await approveFinanceRecord(row.id)
    if (res.code === 200) {
      ElMessage.success('审核通过')
      refreshAll()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

const handleReject = (row) => {
  currentRejectId.value = row.id
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

const createEnterpriseFinanceDocument = async (type) => {
  const nowPeriod = new Date().toISOString().slice(0, 7)
  const actions = {
    budget: () => createFinanceBudget({
      budgetName: `社团经费预算 ${nowPeriod}`,
      budgetType: 'MONTHLY',
      period: nowPeriod,
      totalAmount: 10000,
      ownerName: '财务管理员',
      remark: '后台快速登记预算'
    }),
    allocation: () => createFinanceAllocation({
      sourceType: 'SCHOOL_GRANT',
      sourceName: '学校社团专项经费',
      amount: 5000,
      arrivalStatus: 0,
      period: nowPeriod,
      remark: '后台快速登记拨款'
    }),
    reimbursement: () => createFinanceReimbursement({
      applicantName: '社团成员',
      expenseSubject: '活动执行费用',
      amount: 800,
      invoiceCount: 1,
      period: nowPeriod,
      remark: '后台快速登记报销'
    }),
    voucher: () => createFinanceVoucher({
      period: nowPeriod,
      sourceType: 'MANUAL',
      summary: '后台快速登记凭证',
      amount: 800,
      postingStatus: 0
    })
  }
  const action = actions[type]
  if (!action) return
  try {
    const res = await action()
    if (res.code === 200) {
      ElMessage.success('企业财务单据已登记')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '企业财务单据登记失败')
  } catch (error) {
    ElMessage.error(error.message || '企业财务单据登记失败')
  }
}

const currentFinancePeriod = () => filterPeriod.value || new Date().toISOString().slice(0, 7)

const handlePostVoucher = async (row) => {
  await ElMessageBox.confirm(`确定过账凭证 ${row.voucherNo || row.id} 吗？`, '凭证过账', { type: 'success' })
  try {
    const res = await postFinanceVoucher(row.id)
    if (res.code === 200) {
      ElMessage.success('凭证已过账')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '凭证过账失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '凭证过账失败')
    }
  }
}

const handleReverseVoucher = async (row) => {
  const prompt = await ElMessageBox.prompt(`请输入冲销原因：${row.voucherNo || row.id}`, '凭证冲销', {
    confirmButtonText: '冲销',
    cancelButtonText: '取消',
    inputValue: '业务调整冲销',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入冲销原因'
  }).catch(() => null)
  if (!prompt) return
  try {
    const res = await reverseFinanceVoucher(row.id, { remark: prompt.value })
    if (res.code === 200) {
      ElMessage.success('凭证已冲销')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '凭证冲销失败')
  } catch (error) {
    ElMessage.error(error.message || '凭证冲销失败')
  }
}

const handleRequestReverseVoucher = async (row) => {
  const prompt = await ElMessageBox.prompt(`请输入 ${row.voucherNo || row.id} 冲销申请原因`, '申请凭证冲销', {
    confirmButtonText: '提交申请',
    cancelButtonText: '取消',
    inputValue: '业务调整申请冲销',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入冲销申请原因'
  }).catch(() => null)
  if (!prompt) return
  try {
    const res = await requestReverseFinanceVoucher(row.id, { reason: prompt.value })
    if (res.code === 200) {
      ElMessage.success('凭证冲销申请已提交')
      await fetchEnterpriseFinance()
      activeTab.value = 'audit'
      return
    }
    ElMessage.error(res.msg || '凭证冲销申请失败')
  } catch (error) {
    ElMessage.error(error.message || '凭证冲销申请失败')
  }
}

const handleApproveReverseRequest = async (row) => {
  await ElMessageBox.confirm(`确定通过凭证冲销申请 #${row.actionId || row.id} 吗？`, '审批凭证冲销', { type: 'warning' })
  try {
    const res = await approveReverseFinanceVoucher(row.actionId || row.id, { opinion: '审批通过' })
    if (res.code === 200) {
      ElMessage.success('凭证冲销审批已通过')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '凭证冲销审批失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '凭证冲销审批失败')
    }
  }
}

const handleClosePeriod = async (force = false) => {
  const period = currentFinancePeriod()
  const message = force
    ? `确定强制结账 ${period} 吗？强制结账会保留审计风险记录。`
    : `确定结账 ${period} 吗？系统会检查未审核单据和未过账凭证。`
  await ElMessageBox.confirm(message, '期间结账', { type: force ? 'warning' : 'info' })
  try {
    const res = await closeFinancePeriod({
      period,
      force,
      remark: force ? '后台强制结账' : '后台期间结账'
    })
    if (res.code === 200) {
      ElMessage.success(force ? '期间已强制结账' : '期间已结账')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '期间结账失败')
  } catch (error) {
    if (!force && error !== 'cancel') {
      const confirmForce = await ElMessageBox.confirm('普通结账未完成，是否改为强制结账并保留审计记录？', '强制结账', { type: 'warning' }).catch(() => false)
      if (confirmForce) {
        await handleClosePeriod(true)
      }
      return
    }
    if (error !== 'cancel') {
      ElMessage.error(error.message || '期间结账失败')
    }
  }
}

const handleReopenPeriod = async () => {
  const period = currentFinancePeriod()
  const prompt = await ElMessageBox.prompt(`请输入 ${period} 反结账原因`, '期间反结账', {
    confirmButtonText: '反结账',
    cancelButtonText: '取消',
    inputValue: '后台反结账调整',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入反结账原因'
  }).catch(() => null)
  if (!prompt) return
  try {
    const res = await reopenFinancePeriod({ period, reason: prompt.value })
    if (res.code === 200) {
      ElMessage.success('期间已反结账')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '反结账失败')
  } catch (error) {
    ElMessage.error(error.message || '反结账失败')
  }
}

const handleRequestReopenPeriod = async () => {
  const period = currentFinancePeriod()
  const prompt = await ElMessageBox.prompt(`请输入 ${period} 反结账申请原因`, '申请期间反结账', {
    confirmButtonText: '提交申请',
    cancelButtonText: '取消',
    inputValue: '后台申请反结账调整',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入反结账申请原因'
  }).catch(() => null)
  if (!prompt) return
  try {
    const res = await requestReopenFinancePeriod({ period, reason: prompt.value })
    if (res.code === 200) {
      ElMessage.success('反结账申请已提交')
      await fetchEnterpriseFinance()
      activeTab.value = 'audit'
      return
    }
    ElMessage.error(res.msg || '反结账申请失败')
  } catch (error) {
    ElMessage.error(error.message || '反结账申请失败')
  }
}

const handleApproveReopenRequest = async (row) => {
  await ElMessageBox.confirm(`确定通过反结账申请 #${row.actionId || row.id} 吗？`, '审批期间反结账', { type: 'warning' })
  try {
    const res = await approveReopenFinancePeriod(row.actionId || row.id, { opinion: '审批通过' })
    if (res.code === 200) {
      ElMessage.success('反结账审批已通过')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '反结账审批失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '反结账审批失败')
    }
  }
}

const resolveSubjectParentCode = (value, subjectCode) => {
  const parentCode = String(value || '').trim()
  if (!parentCode) {
    return ''
  }
  if (parentCode === subjectCode) {
    ElMessage.error('上级科目不能指向自己')
    return null
  }
  const exists = subjectParentOptions.value.some((option) => option.value === parentCode)
  if (!exists) {
    ElMessage.error('上级科目不存在')
    return null
  }
  return parentCode
}

const handleCreateFinanceSubject = async () => {
  const codePrompt = await ElMessageBox.prompt('请输入科目编码', '新增会计科目', {
    confirmButtonText: '下一步',
    cancelButtonText: '取消',
    inputValue: `5${Date.now().toString().slice(-3)}`,
    inputPattern: /\S+/,
    inputErrorMessage: '请输入科目编码'
  }).catch(() => null)
  if (!codePrompt) return
  const namePrompt = await ElMessageBox.prompt('请输入科目名称', '新增会计科目', {
    confirmButtonText: '新增',
    cancelButtonText: '取消',
    inputValue: '社团专项费用',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入科目名称'
  }).catch(() => null)
  if (!namePrompt) return
  const parentPrompt = await ElMessageBox.prompt('请输入上级科目编码，可留空作为顶级科目', '新增会计科目', {
    confirmButtonText: '新增',
    cancelButtonText: '取消',
    inputValue: '',
    inputPlaceholder: subjectParentOptions.value.slice(0, 3).map((item) => item.label).join(' / ')
  }).catch(() => null)
  if (!parentPrompt) return
  const parentCode = resolveSubjectParentCode(parentPrompt.value, codePrompt.value)
  if (parentCode === null) return
  try {
    const res = await createFinanceSubject({
      subjectCode: codePrompt.value,
      subjectName: namePrompt.value,
      subjectType: 'EXPENSE',
      parentCode,
      direction: 'DEBIT',
      status: 1,
      remark: '后台新增科目'
    })
    if (res.code === 200) {
      ElMessage.success('会计科目已保存')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '会计科目保存失败')
  } catch (error) {
    ElMessage.error(error.message || '会计科目保存失败')
  }
}

const handleEditFinanceSubject = async (row) => {
  const codePrompt = await ElMessageBox.prompt('请输入科目编码', '编辑会计科目', {
    confirmButtonText: '下一步',
    cancelButtonText: '取消',
    inputValue: row.subjectCode || '',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入科目编码'
  }).catch(() => null)
  if (!codePrompt) return
  const namePrompt = await ElMessageBox.prompt('请输入科目名称', '编辑会计科目', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputValue: row.subjectName || '',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入科目名称'
  }).catch(() => null)
  if (!namePrompt) return
  const parentPrompt = await ElMessageBox.prompt('请输入上级科目编码，可留空作为顶级科目', '编辑会计科目', {
    confirmButtonText: '保存',
    cancelButtonText: '取消',
    inputValue: row.parentCode || '',
    inputPlaceholder: subjectParentOptions.value
      .filter((item) => item.value !== row.subjectCode)
      .slice(0, 3)
      .map((item) => item.label)
      .join(' / ')
  }).catch(() => null)
  if (!parentPrompt) return
  const parentCode = resolveSubjectParentCode(parentPrompt.value, codePrompt.value)
  if (parentCode === null) return
  try {
    const res = await updateFinanceSubject(row.id, {
      subjectCode: codePrompt.value,
      subjectName: namePrompt.value,
      subjectType: row.subjectType || 'EXPENSE',
      parentCode,
      direction: row.direction || 'DEBIT',
      status: Number(row.status ?? 1),
      remark: '后台编辑科目'
    })
    if (res.code === 200) {
      ElMessage.success('会计科目已更新')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '会计科目更新失败')
  } catch (error) {
    ElMessage.error(error.message || '会计科目更新失败')
  }
}

const handleDeleteFinanceSubject = async (row) => {
  await ElMessageBox.confirm(`确定删除会计科目 ${row.subjectCode || ''} ${row.subjectName || ''} 吗？`, '删除会计科目', { type: 'warning' })
  try {
    const res = await deleteFinanceSubject(row.id)
    if (res.code === 200) {
      ElMessage.success('会计科目已删除')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '会计科目删除失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '会计科目删除失败')
    }
  }
}

const handleToggleFinanceSubjectStatus = async (row) => {
  const nextStatus = Number(row.status) === 1 ? 0 : 1
  try {
    const res = await updateFinanceSubjectStatus(row.id, {
      status: nextStatus,
      remark: nextStatus === 1 ? '后台启用科目' : '后台停用科目'
    })
    if (res.code === 200) {
      ElMessage.success(nextStatus === 1 ? '科目已启用' : '科目已停用')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '科目状态更新失败')
  } catch (error) {
    ElMessage.error(error.message || '科目状态更新失败')
  }
}

const handleCreateDefaultApprovalTemplate = async () => {
  try {
    const res = await createDefaultFinanceApprovalTemplate({
      templateName: '默认报销审批模板',
      amount: 0
    })
    if (res.code === 200) {
      ElMessage.success('审批模板已生成并启用')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '审批模板生成失败')
  } catch (error) {
    ElMessage.error(error.message || '审批模板生成失败')
  }
}

const handleEnableApprovalTemplate = async (row) => {
  await ElMessageBox.confirm(`确定启用审批模板 ${row.templateName || row.id} 吗？`, '审批模板', { type: 'info' })
  try {
    const res = await enableFinanceApprovalTemplate(row.id)
    if (res.code === 200) {
      ElMessage.success('审批模板已启用')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '审批模板启用失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '审批模板启用失败')
    }
  }
}

const handleApproveReimbursement = async (row) => {
  await ElMessageBox.confirm(`确定通过报销单 ${row.reimbursementNo || row.id} 吗？`, '报销审批', { type: 'success' })
  try {
    const res = await approveFinanceReimbursement(row.id, { opinion: '后台审批通过' })
    if (res.code === 200) {
      ElMessage.success('报销已通过')
      await refreshAll()
      return
    }
    ElMessage.error(res.msg || '报销审批失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '报销审批失败')
    }
  }
}

const handleRejectReimbursement = async (row) => {
  const prompt = await ElMessageBox.prompt(`请输入报销单 ${row.reimbursementNo || row.id} 驳回原因`, '报销驳回', {
    confirmButtonText: '驳回',
    cancelButtonText: '取消',
    inputValue: '材料不完整',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入驳回原因'
  }).catch(() => null)
  if (!prompt) return
  try {
    const res = await rejectFinanceReimbursement(row.id, { reason: prompt.value })
    if (res.code === 200) {
      ElMessage.success('报销已驳回')
      await refreshAll()
      return
    }
    ElMessage.error(res.msg || '报销驳回失败')
  } catch (error) {
    ElMessage.error(error.message || '报销驳回失败')
  }
}

const normalizeApproverCandidate = (row = {}) => {
  const rawApproverId = row.authUserId ?? row.auth_user_id ?? row.id
  const nextApproverId = Number(rawApproverId)
  if (!Number.isFinite(nextApproverId) || nextApproverId <= 0) {
    return null
  }
  const displayName = row.name || row.realName || row.real_name || row.nickname || row.nickName || row.username || `用户${nextApproverId}`
  const username = row.username || row.mobile || row.email || ''
  const roleName = row.roleName || row.role_name || ''
  const tenantName = row.tenantName || row.tenant_name || ''
  return {
    ...row,
    nextApproverId,
    displayName,
    username,
    roleName,
    tenantName,
    label: [displayName, username, roleName, tenantName].filter(Boolean).join(' / ')
  }
}

const readUserPageRecords = (data) => {
  if (Array.isArray(data?.records)) return data.records
  if (Array.isArray(data?.list)) return data.list
  if (Array.isArray(data?.rows)) return data.rows
  return Array.isArray(data) ? data : []
}

const loadFinanceApproverCandidates = async (keyword = '') => {
  approverLoading.value = true
  try {
    const trimmedKeyword = String(keyword || '').trim()
    const res = await getUserPage({
      page: 1,
      size: 500,
      status: 1
    })
    const rows = res.code === 200 ? readUserPageRecords(res.data) : []
    const seen = new Set()
    approverCandidates.value = rows
      .map(normalizeApproverCandidate)
      .filter((candidate) => {
        if (!candidate || !trimmedKeyword) {
          return Boolean(candidate)
        }
        return candidate.label.toLowerCase().includes(trimmedKeyword.toLowerCase())
      })
      .filter((candidate) => {
        if (!candidate || seen.has(candidate.nextApproverId)) {
          return false
        }
        seen.add(candidate.nextApproverId)
        return true
      })
  } catch (error) {
    approverCandidates.value = []
    ElMessage.error(error.message || '审批人候选加载失败')
  } finally {
    approverLoading.value = false
  }
}

const askNextApproverId = async (title, defaultOpinion) => {
  approverDialogTitle.value = title
  approverForm.nextApproverId = null
  approverForm.opinion = defaultOpinion || ''
  await loadFinanceApproverCandidates()
  approverDialogVisible.value = true
  return new Promise((resolve) => {
    approverResolve.value = resolve
  })
}

const resolveApproverDialog = (payload) => {
  const resolver = approverResolve.value
  approverResolve.value = null
  if (resolver) {
    resolver(payload)
  }
}

const cancelApproverSelection = () => {
  approverDialogVisible.value = false
  resolveApproverDialog(null)
}

const confirmApproverSelection = () => {
  if (!approverForm.nextApproverId) {
    ElMessage.warning('请选择审批人')
    return
  }
  if (!approverForm.opinion) {
    ElMessage.warning('请输入审批意见')
    return
  }
  const payload = {
    nextApproverId: Number(approverForm.nextApproverId),
    opinion: approverForm.opinion
  }
  approverDialogVisible.value = false
  resolveApproverDialog(payload)
}

const handleApproverDialogClosed = () => {
  resolveApproverDialog(null)
}

const handleTransferReimbursement = async (row) => {
  const payload = await askNextApproverId('报销审批转交', '后台审批转交')
  if (!payload) return
  try {
    const res = await transferFinanceReimbursement(row.id, payload)
    if (res.code === 200) {
      ElMessage.success('报销审批已转交')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '报销转交失败')
  } catch (error) {
    ElMessage.error(error.message || '报销转交失败')
  }
}

const handleAddSignReimbursement = async (row) => {
  const payload = await askNextApproverId('报销审批加签', '后台审批加签')
  if (!payload) return
  try {
    const res = await addSignFinanceReimbursement(row.id, payload)
    if (res.code === 200) {
      ElMessage.success('报销审批已加签')
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '报销加签失败')
  } catch (error) {
    ElMessage.error(error.message || '报销加签失败')
  }
}

const handleWithdrawReimbursement = async (row) => {
  const prompt = await ElMessageBox.prompt(`请输入报销单 ${row.reimbursementNo || row.id} 撤回原因`, '报销撤回', {
    confirmButtonText: '撤回',
    cancelButtonText: '取消',
    inputValue: '申请人撤回调整',
    inputPattern: /\S+/,
    inputErrorMessage: '请输入撤回原因'
  }).catch(() => null)
  if (!prompt) return
  try {
    const res = await withdrawFinanceReimbursement(row.id, { reason: prompt.value })
    if (res.code === 200) {
      ElMessage.success('报销已撤回')
      await refreshAll()
      return
    }
    ElMessage.error(res.msg || '报销撤回失败')
  } catch (error) {
    ElMessage.error(error.message || '报销撤回失败')
  }
}

const handleCreateReconciliation = async () => {
  const period = currentFinancePeriod()
  await ElMessageBox.confirm(`确定生成 ${period} 的资金对账记录吗？`, '资金对账', { type: 'info' })
  try {
    const res = await createFinanceReconciliation({
      period,
      remark: '后台生成资金对账'
    })
    if (res.code === 200) {
      const difference = Number(res.data?.differenceAmount || 0)
      ElMessage[difference === 0 ? 'success' : 'warning'](difference === 0 ? '对账平衡' : `对账存在差异：${formatMoney(difference)}`)
      await fetchEnterpriseFinance()
      return
    }
    ElMessage.error(res.msg || '对账失败')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '对账失败')
    }
  }
}

const handleExportFinanceReport = async () => {
  try {
    const period = filterPeriod.value
    const blob = await exportFinanceReport({ period })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `finance-report-${period || 'all'}.csv`
    document.body.appendChild(link)
    link.click()
    link.remove()
    URL.revokeObjectURL(url)
    ElMessage.success('财务报表已导出')
  } catch (error) {
    ElMessage.error(error.message || '财务报表导出失败')
  }
}

const confirmReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入驳回原因')
    return
  }
  try {
    const res = await rejectFinanceRecord(currentRejectId.value, rejectReason.value)
    if (res.code === 200) {
      ElMessage.success('已驳回')
      rejectDialogVisible.value = false
      refreshAll()
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

function formatMoney(value) {
  return Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function renderTrendChart() {
  nextTick(() => {
    if (!trendChartRef.value) return
    if (!trendChart) {
      trendChart = echarts.init(trendChartRef.value)
    }
    const trend = dashboard.monthlyTrend?.length ? dashboard.monthlyTrend : [{ period: '当前', income: accountInfo.totalIncome, expense: accountInfo.totalExpense }]
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['收入', '支出'] },
      grid: { left: 36, right: 20, top: 42, bottom: 30 },
      xAxis: { type: 'category', data: trend.map((item) => item.period) },
      yAxis: { type: 'value' },
      series: [
        { name: '收入', type: 'bar', data: trend.map((item) => Number(item.income || 0)), itemStyle: { color: '#18a058' } },
        { name: '支出', type: 'bar', data: trend.map((item) => Number(item.expense || 0)), itemStyle: { color: '#d03050' } }
      ]
    })
    trendChart.resize()
  })
}

async function fetchReport() {
  const res = await getFinanceReportSummary({ period: filterPeriod.value })
  if (res.code === 200) {
    report.value = res.data || {}
    ElMessage.success('报表已生成')
  }
}

async function refreshAll() {
  await fetchAccount()
  await fetchList()
  await fetchDashboard()
  await fetchEnterpriseFinance()
}

watch(
  () => route.path,
  (path) => {
    const nextTab = financeRouteTabMap[path]
    if (nextTab) {
      activeTab.value = nextTab
    }
  },
  { immediate: true }
)

onMounted(() => {
  fetchAccount()
  fetchList()
  fetchDashboard()
  fetchEnterpriseFinance()
  window.addEventListener('resize', handleResize)
})

function handleResize() {
  trendChart?.resize()
}

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
  trendChart = null
})
</script>

<style lang="scss" scoped>
.finance-page {
  padding: 2px;

  .mt-20 {
    margin-top: 20px;
  }

  .finance-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 18px;
    margin-bottom: 18px;
    padding: 20px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #fff;
  }

  .finance-header h1 {
    margin: 0;
    font-size: 24px;
    color: #1f2937;
  }

  .finance-header p {
    margin: 8px 0 0;
    color: #64748b;
  }

  .stat-row {
    row-gap: 16px;
  }

  .metric-card {
    min-height: 118px;
    padding: 18px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #fff;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
  }

  .metric-card span,
  .metric-card small {
    color: #64748b;
  }

  .metric-card strong {
    margin: 10px 0;
    font-size: 28px;
    line-height: 1.1;
    color: #1f2937;
  }

  .metric-card .blue { color: #2563eb; }
  .metric-card .green { color: #16a34a; }
  .metric-card .red { color: #dc2626; }

  .finance-panels {
    margin-top: 16px;
    row-gap: 16px;
  }

  .module-grid {
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 12px;
    margin-top: 16px;
  }

  .module-card {
    min-height: 136px;
    padding: 16px;
    text-align: left;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #fff;
    color: #1f2937;
    cursor: pointer;
    display: grid;
    gap: 7px;
    transition: border-color 0.2s, box-shadow 0.2s, transform 0.2s;
  }

  .module-card:hover,
  .module-card.active {
    border-color: var(--rk-primary);
    box-shadow: 0 10px 24px rgba(31, 58, 52, 0.12);
    transform: translateY(-2px);
  }

  .module-card.active {
    background: linear-gradient(180deg, #ffffff 0%, #f3faf7 100%);
  }

  .module-card .module-title {
    font-weight: 700;
    color: #1f2937;
  }

  .module-card strong {
    font-size: 24px;
    line-height: 1;
  }

  .module-card small,
  .module-card em {
    font-style: normal;
    color: #64748b;
    line-height: 1.35;
  }

  .finance-chart {
    height: 310px;
    width: 100%;
  }

  .chart-card {
    min-height: 390px;
  }

  .risk-list {
    display: grid;
    gap: 10px;
  }

  .finance-tabs {
    margin-top: 18px;
    padding: 0 12px;
    border: 1px solid #dcdfe6;
    border-radius: 8px;
    background: #fff;
  }

  .budget-card {
    min-height: 160px;
  }

  .budget-card :deep(.el-card__body) {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .budget-card strong {
    font-size: 26px;
    color: #1f2937;
  }

  .budget-card span,
  .budget-card small {
    color: #64748b;
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    gap: 12px;
  }

  .header-actions {
    display: flex;
    gap: 12px;
  }

  .search-form {
    margin-bottom: 20px;
  }

  .amount-income {
    color: #67c23a;
    font-weight: 600;
  }

  .amount-expense {
    color: #f56c6c;
    font-weight: 600;
  }

  .text-muted {
    color: #c0c4cc;
    font-size: 13px;
  }

  .approver-option {
    display: flex;
    flex-direction: column;
    gap: 2px;
    line-height: 1.35;
  }

  .approver-option strong {
    color: #1f2937;
    font-weight: 600;
  }

  .approver-option span {
    color: #64748b;
    font-size: 12px;
  }

  .approval-template-panel {
    margin: 12px 0 18px;
    padding: 12px;
    border: 1px solid #e5e7eb;
    border-radius: 8px;
    background: #f8fafc;
  }

  .node-tag {
    margin: 2px 6px 2px 0;
  }

  .template-node-summary {
    margin-top: 10px;
    color: #64748b;
    font-size: 13px;
  }

  .el-pagination {
    margin-top: 20px;
    justify-content: flex-end;
  }

  @media (max-width: 960px) {
    .finance-header {
      flex-direction: column;
    }

    .header-actions {
      width: 100%;
      flex-wrap: wrap;
    }

    .module-grid {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }
  }

  @media (max-width: 640px) {
    .module-grid {
      grid-template-columns: minmax(0, 1fr);
    }
  }
}
</style>
