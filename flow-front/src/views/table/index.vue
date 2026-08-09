<template>
  <div class="table-container">
    <main class="main-content">
      <div class="page-header">
        <nav class="breadcrumb">
          <span>首页</span>
          <span>/</span>
          <span class="active">问题下发管理</span>
        </nav>
        <h3 class="page-heading">问题下发管理</h3>
      </div>

      <section class="stats-section">
        <div class="stats-grid">
          <div class="stat-card stat-total" :class="{ active: activeStat === '全部' }" @click="handleStatCardClick('全部')">
            <div class="stat-icon">
              <i class="el-icon-document"></i>
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.total }}</p>
              <p class="stat-label">总任务数</p>
            </div>
          </div>
          <div class="stat-card stat-pending" :class="{ active: activeStat === '待启动' }" @click="handleStatCardClick('待启动')">
            <div class="stat-icon">
              <i class="el-icon-time"></i>
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.pending }}</p>
              <p class="stat-label">待启动</p>
            </div>
          </div>
          <div class="stat-card stat-progress" :class="{ active: activeStat === '进行中' }" @click="handleStatCardClick('进行中')">
            <div class="stat-icon">
              <i class="el-icon-refresh"></i>
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.progress }}</p>
              <p class="stat-label">进行中</p>
            </div>
          </div>
          <div class="stat-card stat-closed" :class="{ active: activeStat === '已闭环' }" @click="handleStatCardClick('已闭环')">
            <div class="stat-icon">
              <i class="el-icon-circle-check"></i>
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.closed }}</p>
              <p class="stat-label">已闭环</p>
            </div>
          </div>
        </div>
      </section>

      <section class="filter-section">
        <div class="filter-grid">
          <div class="filter-item">
            <label class="filter-label">部门</label>
            <select class="filter-select" v-model="filters.department">
              <option>全部部门</option>
              <option v-for="dept in options.departments" :key="dept">{{ dept }}</option>
            </select>
          </div>
          <div class="filter-item">
            <label class="filter-label">责任人</label>
            <input class="filter-input" v-model="filters.responsible" placeholder="请输入姓名" />
          </div>
          <div class="filter-item">
            <label class="filter-label">整改周期</label>
            <select class="filter-select" v-model="filters.cycle">
              <option>全部</option>
              <option v-for="cycle in options.cycles" :key="cycle">{{ cycle }}</option>
            </select>
          </div>
          <div class="filter-item">
            <label class="filter-label">任务状态</label>
            <div class="status-btns">
              <button class="status-btn" :class="{ active: filters.status === '全部' }" @click="filters.status = '全部'">全部</button>
              <button v-for="status in options.statuses" :key="status" class="status-btn" :class="{ active: filters.status === status }" @click="filters.status = status">{{ status }}</button>
            </div>
          </div>
          <div class="filter-item">
            <label class="filter-label">日期范围</label>
            <input class="filter-input" v-model="filters.date" type="date" />
          </div>
        </div>
        <div class="filter-actions">
          <button class="btn-reset" @click="resetFilters">重置</button>
          <button class="btn-search" @click="handleSearch">查询</button>
        </div>
      </section>

      <section class="accordion-section" v-loading="loading">
        <div v-if="!loading && accordionItems.length === 0" class="empty-state">
          <i class="el-icon-warning-outline"></i>
          <p>暂无符合条件的问题下发任务</p>
        </div>
        <div v-for="(item, index) in accordionItems" :key="index" class="accordion-item" :class="{ active: item.active }">
          <div class="accordion-header" @click="toggleAccordion(index)">
            <div class="accordion-left">
              <span class="priority-tag" :class="'priority-' + item.priority">{{ item.priorityText }}</span>
              <h3 class="accordion-title">{{ item.title }}</h3>
              <div class="accordion-meta">
                <span class="meta-item">{{ item.department }}</span>
                <span class="meta-item">{{ item.cycle }}</span>
              </div>
            </div>
            <div class="accordion-right">
              <button class="detail-btn" @click.stop="openDetailModal(item)">
                <i class="el-icon-info"></i> 查看详情
              </button>
              <span class="status-tag" :class="'status-' + item.status">{{ item.statusText }}</span>
              <i :class="['el-icon', 'chevron-icon', item.active ? 'el-icon-arrow-down' : 'el-icon-arrow-right']"></i>
            </div>
          </div>
          <div class="accordion-content">
            <div class="accordion-body">
              <!-- 问题描述 -->
              <div class="content-main">
                <div>
                  <h4 class="content-label">问题描述</h4>
                  <p class="content-text">{{ item.description }}</p>
                </div>
                <div class="info-row">
                  <div>
                    <h4 class="content-label">周期信息</h4>
                    <p class="content-value">{{ item.cycleInfo }}</p>
                  </div>
                  <div>
                    <h4 class="content-label">下发时间</h4>
                    <p class="content-value">{{ item.createdTime }}</p>
                  </div>
                </div>
              </div>

              <!-- 下发期次列表 -->
              <div class="rounds-section">
                <div class="section-header">
                  <h4 class="content-label">下发期次 <span class="round-count">共 {{ item.rounds.length }} 期</span></h4>
                </div>
                <div class="rounds-table">
                  <div class="rounds-table-header">
                    <span class="col-round">期次</span>
                    <span class="col-time">下发时间</span>
                    <span class="col-deadline">截止时间</span>
                    <span class="col-people">整改人数</span>
                    <span class="col-rate">完成率</span>
                    <span class="col-status">状态</span>
                    <span class="col-action">操作</span>
                  </div>
                  <div v-for="(round, rIndex) in item.rounds" :key="rIndex" class="rounds-table-row" :class="{ selected: selectedTaskIndex === index && selectedRoundIndex === rIndex }">
                    <span class="col-round">
                      <span class="round-badge">第{{ round.roundNo }}期</span>
                    </span>
                    <span class="col-time">{{ round.publishTime }}</span>
                    <span class="col-deadline">{{ round.deadline }}</span>
                    <span class="col-people">
                      <i class="el-icon-user"></i> {{ round.totalPeople }} 人
                    </span>
                    <span class="col-rate">
                      <div class="rate-bar">
                        <div class="rate-fill" :style="{ width: round.completionRate + '%' }" :class="'rate-' + round.status"></div>
                      </div>
                      <span class="rate-text">{{ round.completedPeople }}/{{ round.totalPeople }} ({{ round.completionRate }}%)</span>
                    </span>
                    <span class="col-status">
                      <span class="round-status-tag" :class="'round-status-' + round.status">{{ round.statusText }}</span>
                    </span>
                    <span class="col-action">
                      <button class="link-btn" @click="openRoundRecordModal(round, item, rIndex)">
                        <i class="el-icon-document"></i> 查看整改记录
                      </button>
                    </span>
                  </div>
                </div>
              </div>

              <!-- 底部操作 -->
              <div class="accordion-footer">
                <div class="footer-actions">
                  <button class="action-btn edit">编辑任务</button>
                  <button class="action-btn suspend">暂时挂起</button>
                  <button class="action-btn cancel">终止任务</button>
                </div>
                <div class="footer-right">
                  <button class="btn-person" @click="openPersonModal(item)">
                    <i class="el-icon-user"></i> 查看整改人员
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <footer class="pagination-footer">
        <span class="footer-info">共计 {{ total }} 个整改项目</span>
        <div class="pagination-controls">
          <button class="page-btn" :disabled="currentPage === 1" @click="prevPage">
            <i class="el-icon el-icon-arrow-left"></i>
          </button>
          <button v-for="page in pageList" :key="page" class="page-btn" :class="{ active: currentPage === page }" @click="goToPage(page)">
            {{ page }}
          </button>
          <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage">
            <i class="el-icon el-icon-arrow-right"></i>
          </button>
        </div>
      </footer>
    </main>

    <!-- 整改人员弹窗 -->
    <PersonModal
      :visible="personModalVisible"
      :issue-title="currentIssueTitle"
      :persons="currentPersons"
      :rounds="currentRounds"
      @close="closePersonModal"
    />

    <!-- 期次整改记录弹窗 -->
    <RoundRecordModal
      :visible="roundRecordModalVisible"
      :issue-title="currentIssueTitle"
      :round="currentRound"
      :persons="currentPersons"
      @close="closeRoundRecordModal"
    />

    <div v-if="toastVisible" class="toast-notification">
      <i class="el-icon el-icon-circle-check"></i>
      <div>
        <p class="toast-title">操作成功</p>
        <p class="toast-message">问题已成功发布并同步至责任人。</p>
      </div>
    </div>

    <DetailModal
      :visible="detailModalVisible"
      :detail="currentDetail"
      @close="closeDetailModal"
    />
  </div>
</template>

<script>
import PersonModal from './components/PersonModal.vue'
import RoundRecordModal from './components/RoundRecordModal.vue'
import { getTaskList, getTaskStats, getTaskOptions } from '@/api/issueTask'
import DetailModal from './components/DetailModal.vue'

export default {
  name: 'Table',
  components: { PersonModal, RoundRecordModal, DetailModal },
  data() {
    return {
      loading: false,
      stats: {
        total: 0,
        pending: 0,
        progress: 0,
        closed: 0
      },
      activeStat: '全部',
      filters: {
        department: '全部部门',
        responsible: '',
        cycle: '全部',
        status: '全部',
        date: ''
      },
      accordionItems: [],
      total: 0,
      currentPage: 1,
      pageSize: 5,
      totalPages: 1,
      options: {
        departments: [],
        cycles: [],
        statuses: []
      },
      toastVisible: false,
      personModalVisible: false,
      roundRecordModalVisible: false,
      detailModalVisible: false,
      currentDetail: {},
      currentIssueTitle: '',
      currentPersons: [],
      currentRounds: [],
      currentRound: {},
      selectedTaskIndex: -1,
      selectedRoundIndex: -1
    }
  },
  computed: {
    pageList() {
      const pages = []
      const total = this.totalPages
      const current = this.currentPage
      if (total <= 5) {
        for (let i = 1; i <= total; i++) pages.push(i)
      } else {
        if (current <= 3) {
          pages.push(1, 2, 3, 4, 5)
        } else if (current >= total - 2) {
          pages.push(total - 4, total - 3, total - 2, total - 1, total)
        } else {
          pages.push(current - 2, current - 1, current, current + 1, current + 2)
        }
      }
      return pages
    }
  },
  mounted() {
    this.fetchData()
    this.fetchStats()
    this.fetchOptions()
  },
  methods: {
    async fetchStats() {
      try {
        const response = await getTaskStats()
        this.stats = response.data
      } catch (error) {
        console.error('获取统计数据失败:', error)
      }
    },
    async fetchOptions() {
      try {
        const response = await getTaskOptions()
        this.options = response.data
      } catch (error) {
        console.error('获取筛选选项失败:', error)
      }
    },
    handleStatCardClick(status) {
      this.activeStat = status
      this.filters.status = status
      this.currentPage = 1
      this.fetchData()
    },
    async fetchData() {
      this.loading = true
      try {
        const response = await getTaskList({
          page: this.currentPage,
          limit: this.pageSize,
          ...this.filters
        })
        this.accordionItems = response.data.items
        this.total = response.data.total
        this.totalPages = Math.max(1, Math.ceil(this.total / this.pageSize))
        // 首页默认展开第一条
        if (this.accordionItems.length > 0) {
          this.$set(this.accordionItems[0], 'active', true)
        }
      } catch (error) {
        console.error('获取问题下发任务失败:', error)
      } finally {
        this.loading = false
      }
    },
    toggleAccordion(index) {
      this.$set(this.accordionItems[index], 'active', !this.accordionItems[index].active)
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchData()
    },
    resetFilters() {
      this.filters = {
        department: '全部部门',
        responsible: '',
        cycle: '全部',
        status: '全部',
        date: ''
      }
      this.currentPage = 1
      this.fetchData()
    },
    prevPage() {
      if (this.currentPage > 1) {
        this.currentPage--
        this.fetchData()
      }
    },
    nextPage() {
      if (this.currentPage < this.totalPages) {
        this.currentPage++
        this.fetchData()
      }
    },
    goToPage(page) {
      this.currentPage = page
      this.fetchData()
    },
    showToast() {
      this.toastVisible = true
      setTimeout(() => {
        this.toastVisible = false
      }, 3000)
    },
    openPersonModal(item) {
      const taskIndex = this.accordionItems.indexOf(item)
      this.selectedTaskIndex = taskIndex
      this.selectedRoundIndex = -1
      this.currentIssueTitle = item.title
      this.currentPersons = item.persons
      this.currentRounds = item.rounds
      this.personModalVisible = true
    },
    closePersonModal() {
      this.personModalVisible = false
      this.selectedTaskIndex = -1
      this.selectedRoundIndex = -1
    },
    openRoundRecordModal(round, item, rIndex) {
      const taskIndex = this.accordionItems.indexOf(item)
      this.selectedTaskIndex = taskIndex
      this.selectedRoundIndex = rIndex
      this.currentIssueTitle = item.title
      this.currentPersons = item.persons
      this.currentRound = round
      this.roundRecordModalVisible = true
    },
    closeRoundRecordModal() {
      this.roundRecordModalVisible = false
      this.selectedTaskIndex = -1
      this.selectedRoundIndex = -1
    },
    openDetailModal(item) {
      this.currentDetail = item
      this.detailModalVisible = true
    },
    closeDetailModal() {
      this.detailModalVisible = false
      this.selectedTaskIndex = -1
      this.selectedRoundIndex = -1
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/common.scss";

// -- Layout --
.table-container { @include page-container; }
.main-content    { padding: $space-6; }

// -- Page Header --
.page-header  { @include page-header; }
.breadcrumb   { @include breadcrumb; }
.page-heading { font-size: $font-size-2xl; font-weight: 600; color: var(--color-primary); margin: 0; }

// -- Stats Grid --
.stats-section { margin-bottom: $space-4; }
.stats-grid    { @include stats-grid(4); }
.stat-card     { @include stat-card; }
.stat-icon     { @include stat-icon; border-radius: $radius-lg; }
.stat-content  { @include stat-content; }
.stat-value    { @include stat-value; }
.stat-label    { @include stat-label; }
.stat-total   .stat-icon { background: linear-gradient(135deg, var(--color-primary), var(--color-primary)); }
.stat-pending .stat-icon { background: linear-gradient(135deg, #616161, #9e9e9e); }
.stat-progress.stat-icon { background: linear-gradient(135deg, var(--color-primary-hover), var(--color-primary)); }
.stat-closed  .stat-icon { background: linear-gradient(135deg, #15803D, #22c55e); }

// -- Filter --
.filter-section { @include filter-section; }
.filter-grid    { @include filter-grid; }
.filter-item    { @include filter-item; }
.filter-label   { @include filter-label; }
.filter-select,
.filter-input   { @include filter-input; }
.status-btns    { @include status-btns; }
.status-btn     { @include status-btn; }
.btn-search     { @include btn-search; }
.btn-reset      { @include btn-reset; }

// -- Table --
.data-table { @include data-table; }

// -- Status Tags --
.status-chip    { @include status-chip; display: inline-flex; margin: 0 4px; }
.status-pending { @include chip-pending;   border: 1px solid var(--color-primary); }
.status-active  { @include chip-active;    border: 1px solid var(--color-success); }
.status-closed  { @include chip-closed;    border: 1px solid $text-tertiary; }

// -- Pagination --
.pagination { @include pagination-wrapper; }
</style>
