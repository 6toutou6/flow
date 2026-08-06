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
.table-container {
  min-height: 100vh;
  background-color: #fcf9f8;
}

.main-content {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5b403d;

  .active {
    font-weight: bold;
    color: #a20513;
  }
}

.page-heading {
  font-size: 20px;
  font-weight: 600;
  color: #a20513;
  margin: 0;
}

.stats-section {
  margin-bottom: 16px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;

  @media (max-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (max-width: 480px) {
    grid-template-columns: repeat(1, 1fr);
  }
}

.stat-card {
  background: white;
  padding: 20px;
  border-radius: 8px;
  border: 1px solid #e4beba;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.2s;
  cursor: pointer;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-total .stat-icon {
  background: linear-gradient(135deg, #a20513, #c91827);
}

.stat-pending .stat-icon {
  background: linear-gradient(135deg, #616161, #9e9e9e);
}

.stat-progress .stat-icon {
  background: linear-gradient(135deg, #8f1d1d, #C53030);
}

.stat-closed .stat-icon {
  background: linear-gradient(135deg, #166534, #22c55e);
}

.stat-content {
  flex: 1;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1b1c1c;
  margin: 0;
}

.stat-label {
  font-size: 12px;
  color: #757575;
  margin: 4px 0 0;
}

.stat-card.active {
  border-color: #a20513;
  background: rgba(162, 5, 19, 0.05);
  box-shadow: 0 0 0 2px rgba(162, 5, 19, 0.2);

  .stat-icon {
    transform: scale(1.1);
  }

  .stat-value {
    color: #a20513;
  }

  .stat-label {
    color: #a20513;
  }
}

.filter-section {
  background: white;
  border: 1px solid #e4beba;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}

.filter-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 16px;

  @media (min-width: 768px) {
    grid-template-columns: repeat(2, 1fr);
  }

  @media (min-width: 1024px) {
    grid-template-columns: repeat(5, 1fr);
  }
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-label {
  font-size: 13px;
  color: #757575;
}

.filter-select,
.filter-input {
  height: 36px;
  border: 1px solid #e4beba;
  border-radius: 4px;
  padding: 0 8px;
  font-size: 13px;
  outline: none;
  transition: all 0.2s;

  &:focus {
    border-color: #a20513;
    box-shadow: 0 0 0 1px rgba(162, 5, 19, 0.2);
  }
}

.status-btns {
  display: flex;
  gap: 4px;
}

.status-btn {
  padding: 0 8px;
  height: 36px;
  border: 1px solid #e4beba;
  border-radius: 4px;
  font-size: 13px;
  color: #5b403d;
  background: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    border-color: #a20513;
    color: #a20513;
  }

  &.active {
    border-color: #a20513;
    color: #a20513;
    background: rgba(162, 5, 19, 0.05);
  }
}

.filter-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(228, 190, 186, 0.3);
}

.btn-reset {
  padding: 0 16px;
  height: 36px;
  border: 1px solid #e4beba;
  border-radius: 4px;
  font-size: 13px;
  color: #5b403d;
  background: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f6f3f2;
  }
}

.btn-search {
  padding: 0 16px;
  height: 36px;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-weight: bold;
  color: white;
  background: #a20513;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: rgba(162, 5, 19, 0.9);
  }
}

.accordion-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
  min-height: 120px;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  background: white;
  border: 1px dashed #e4beba;
  border-radius: 8px;
  color: #5b403d;

  i {
    font-size: 40px;
    color: #e4beba;
    margin-bottom: 12px;
  }

  p {
    margin: 0;
    font-size: 14px;
  }
}

.accordion-item {
  background: white;
  border: 1px solid #e4beba;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover:not(.active) {
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  }
}

.accordion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-left: 4px solid #a20513;

  &:hover {
    background: #f6f3f2;
  }
}

.accordion-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.priority-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;

  &.priority-high {
    background: #FFEBEE;
    color: #a20513;
    border: 1px solid #a20513;
  }

  &.priority-medium {
    background: #fff3e0;
    color: #e65100;
    border: 1px solid #ffab91;
  }

  &.priority-low {
    background: #f5f5f5;
    color: #616161;
    border: 1px solid #e0e0e0;
  }
}

.accordion-title {
  font-size: 16px;
  font-weight: bold;
  color: #1b1c1c;
  margin: 0;
}

.accordion-meta {
  display: flex;
  gap: 16px;
  font-size: 14px;
  color: #5b403d;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.accordion-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.status-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;

  &.status-progress {
    background: #FEF0F0;
    color: #C53030;
    border: 1px solid #E4BEBA;
  }

  &.status-closed {
    background: #f0fdf4;
    color: #166534;
    border: 1px solid #bbf7d0;
  }

  &.status-pending {
    background: #f5f5f5;
    color: #616161;
    border: 1px solid #e0e0e0;
  }
}

.chevron-icon {
  transition: transform 0.2s;
}

.accordion-content {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease;
}

.accordion-item.active .accordion-content {
  max-height: 2000px;
}

.accordion-body {
  padding: 24px;
  background: #fcf9f8;
  border-top: 1px solid rgba(228, 190, 186, 0.5);
}

.content-main {
  display: grid;
  grid-template-columns: 1fr;
  gap: 16px;
  margin-bottom: 20px;

  @media (min-width: 1024px) {
    grid-template-columns: 2fr 1fr;
  }
}

.content-label {
  font-size: 12px;
  font-weight: bold;
  letter-spacing: 0.05em;
  color: #5b403d;
  margin-bottom: 4px;
  text-transform: uppercase;
}

.content-text {
  font-size: 14px;
  color: #1b1c1c;
  line-height: 1.6;
  margin: 0;
}

.info-row {
  display: flex;
  gap: 32px;
}

.content-value {
  font-size: 14px;
  font-weight: bold;
  color: #1b1c1c;
  margin: 0;
}

/* 下发期次列表 */
.rounds-section {
  background: white;
  border: 1px solid #e4beba;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.round-count {
  font-size: 12px;
  font-weight: normal;
  color: #a20513;
  margin-left: 8px;
}

.rounds-table {
  border: 1px solid rgba(228, 190, 186, 0.4);
  border-radius: 6px;
  overflow: hidden;
}

.rounds-table-header {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1.2fr 0.8fr 1.5fr 0.8fr 1fr;
  gap: 8px;
  padding: 10px 12px;
  background: #f6f3f2;
  font-size: 12px;
  font-weight: bold;
  color: #5b403d;
}

.rounds-table-row {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1.2fr 0.8fr 1.5fr 0.8fr 1fr;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid rgba(228, 190, 186, 0.3);
  font-size: 13px;
  color: #1b1c1c;
  align-items: center;
  transition: background 0.2s;

  &:hover {
    background: #fcf9f8;
  }

  &.selected {
    background: #a20513;
    color: white;

    .col-time,
    .col-deadline {
      color: rgba(255, 255, 255, 0.8);
    }

    .round-badge {
      background: rgba(255, 255, 255, 0.2);
      color: white;
    }

    .round-status-tag {
      background: rgba(255, 255, 255, 0.2);
      color: white;
      border-color: rgba(255, 255, 255, 0.3);
    }

    .link-btn {
      background: white;
      color: #a20513;
      border-color: white;

      &:hover {
        background: rgba(255, 255, 255, 0.9);
      }
    }

    .rate-bar {
      background: rgba(255, 255, 255, 0.2);
    }

    .rate-fill {
      background: white !important;
    }

    .rate-text {
      color: rgba(255, 255, 255, 0.7);
    }

    .col-people {
      color: white;

      i {
        color: rgba(255, 255, 255, 0.8);
      }
    }
  }
}

.round-badge {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(162, 5, 19, 0.1);
  color: #a20513;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.col-time, .col-deadline {
  font-size: 13px;
  color: #5b403d;
}

.col-people {
  font-size: 13px;
  color: #1b1c1c;

  i {
    margin-right: 4px;
    color: #a20513;
  }
}

.col-rate {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rate-bar {
  width: 100%;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.rate-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;

  &.rate-completed {
    background: #166534;
  }

  &.rate-progress {
    background: #a20513;
  }

  &.rate-pending {
    background: #e0e0e0;
  }
}

.rate-text {
  font-size: 11px;
  color: #757575;
}

.round-status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;

  &.round-status-completed {
    background: #f0fdf4;
    color: #166534;
    border: 1px solid #bbf7d0;
  }

  &.round-status-progress {
    background: #FEF0F0;
    color: #C53030;
    border: 1px solid #E4BEBA;
  }

  &.round-status-pending {
    background: #f5f5f5;
    color: #616161;
    border: 1px solid #e0e0e0;
  }
}

/* 操作列 - 查看整改记录 */
.col-action {
  text-align: left;
}

.link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #a20513;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  color: #a20513;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;

  &:hover {
    background: #a20513;
    color: white;
  }
}

/* 底部操作 */
.accordion-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 16px;
  border-top: 1px solid rgba(228, 190, 186, 0.3);
}

.footer-actions {
  display: flex;
  gap: 16px;
}

.footer-right {
  display: flex;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: bold;
  cursor: pointer;
  border: none;
  background: none;
  transition: color 0.2s;

  &.edit {
    color: #a20513;

    &:hover {
      text-decoration: underline;
    }
  }

  &.suspend,
  &.cancel {
    color: #5b403d;

    &:hover {
      color: #a20513;
    }
  }
}

.btn-person {
  padding: 6px 14px;
  border: 1px solid #a20513;
  border-radius: 4px;
  font-size: 13px;
  font-weight: bold;
  color: #a20513;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: rgba(162, 5, 19, 0.05);
  }
}

.pagination-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  border-top: 1px solid rgba(228, 190, 186, 0.3);
  font-size: 13px;
  color: #5b403d;
}

.footer-info {
  margin: 0;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  min-width: 32px;
  height: 32px;
  padding: 0 8px;
  border: 1px solid transparent;
  background: none;
  border-radius: 4px;
  font-size: 13px;
  color: #5b403d;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  justify-content: center;

  &:hover:not(:disabled):not(.active) {
    border-color: #e4beba;
    background: #f6f3f2;
  }

  &.active {
    background: #a20513;
    color: white;
    font-weight: bold;
  }

  &:disabled {
    color: #c8b3b0;
    cursor: not-allowed;
  }
}

/* 人员弹窗样式已迁移至 components/PersonModal.vue */

/* Toast */
.toast-notification {
  position: fixed;
  bottom: 24px;
  right: 24px;
  background: white;
  border-left: 4px solid #166534;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 16px;
  z-index: 100;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    transform: translateY(96px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.toast-notification .el-icon-circle-check {
  color: #166534;
}

.toast-title {
  font-size: 14px;
  font-weight: bold;
  color: #1b1c1c;
  margin: 0;
}

.toast-message {
  font-size: 13px;
  color: #5b403d;
  margin: 4px 0 0;
}

.detail-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #a20513;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  color: #a20513;
  background: white;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  margin-right: 12px;

  &:hover {
    background: #a20513;
    color: white;
  }
}
</style>
