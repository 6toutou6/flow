<template>
  <div class="dashboard-container">
    <!-- Main Content -->
    <main class="main-content">
      <!-- Page Content -->
      <section class="page-content">
        <!-- Page Header -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>系统首页</span>
              <span>/</span>
              <span class="active">问题管理</span>
            </nav>
            <h3 class="page-heading">问题列表管理</h3>
          </div>
        </div>

        <!-- Stats Cards -->
        <div class="stats-grid">
          <div class="stat-card stat-total" :class="{ active: filters.status === '全部状态' }" @click="handleStatCardClick('全部状态')">
            <div class="stat-icon">
              <i class="el-icon-document" />
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.total }}</p>
              <p class="stat-label">总问题数</p>
            </div>
          </div>
          <div class="stat-card stat-pending" :class="{ active: filters.status === '待处理' }" @click="handleStatCardClick('待处理')">
            <div class="stat-icon">
              <i class="el-icon-time" />
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.pending }}</p>
              <p class="stat-label">待处理</p>
            </div>
          </div>
          <div class="stat-card stat-rectifying" :class="{ active: filters.status === '整改中' }" @click="handleStatCardClick('整改中')">
            <div class="stat-icon">
              <i class="el-icon-refresh" />
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.rectifying }}</p>
              <p class="stat-label">整改中</p>
            </div>
          </div>
          <div class="stat-card stat-overdue" :class="{ active: filters.status === '已逾期' }" @click="handleStatCardClick('已逾期')">
            <div class="stat-icon">
              <i class="el-icon-warning" />
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.overdue }}</p>
              <p class="stat-label">已逾期</p>
            </div>
          </div>
          <div class="stat-card stat-completed" :class="{ active: filters.status === '已完成' }" @click="handleStatCardClick('已完成')">
            <div class="stat-icon">
              <i class="el-icon-circle-check" />
            </div>
            <div class="stat-content">
              <p class="stat-value">{{ stats.completed }}</p>
              <p class="stat-label">已完成</p>
            </div>
          </div>
        </div>

        <!-- Filter Section -->
        <section class="filter-section">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">状态筛选</label>
              <select v-model="filters.status" class="filter-select">
                <option>全部状态</option>
                <option>待处理</option>
                <option>整改中</option>
                <option>已逾期</option>
                <option>已完成</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">所属部门</label>
              <select v-model="filters.department" class="filter-select">
                <option>所有部门</option>
                <option>生产技术部</option>
                <option>安全环保部</option>
                <option>人力资源部</option>
                <option>财务部</option>
                <option>综合管理部</option>
                <option>信息技术部</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">优先级</label>
              <select v-model="filters.priority" class="filter-select">
                <option>全部</option>
                <option>紧急</option>
                <option>中</option>
                <option>低</option>
              </select>
            </div>
            <div class="filter-item">
              <label class="filter-label">column1</label>
              <input v-model="filters.column1" class="filter-input" placeholder="请输入">
            </div>
            <div class="filter-item">
              <label class="filter-label">column2</label>
              <input v-model="filters.column2" class="filter-input" placeholder="请输入">
            </div>
          </div>
          <div class="filter-actions">
            <div class="filter-actions-left">
              <button class="btn-primary" @click="openCreateModal">
                <i class="el-icon-plus" />
                问题下发
              </button>
              <button class="btn-import" :disabled="loading || batchLoading" @click="handleImport">
                <i v-if="batchLoading" class="el-icon-loading" />
                <template v-else>
                  <i class="el-icon-upload" />
                  <span>批量导入</span>
                </template>
              </button>
              <button class="btn-export" :disabled="loading || batchLoading" @click="handleExport">
                <i v-if="batchLoading" class="el-icon-loading" />
                <template v-else>
                  <i class="el-icon-download" />
                  <span>批量导出</span>
                </template>
              </button>
            </div>
            <div class="filter-actions-right">
              <button class="btn-reset" :disabled="loading || batchLoading" @click="resetFilters">重置</button>
              <button class="btn-search" :disabled="loading || batchLoading" @click="handleSearch">
                <i v-if="loading" class="el-icon-loading" />
                <span v-else>查询</span>
              </button>
            </div>
          </div>
        </section>

        <!-- Data Table -->
        <div class="table-card table-loading-wrapper">
          <!-- Loading Overlay -->
          <div v-if="loading" class="loading-overlay">
            <div class="loading-spinner">
              <i class="el-icon-loading spinning" />
              <p>加载中...</p>
            </div>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>问题编号</th>
                <th>问题标题</th>
                <th>负责部门</th>
                <th>整改时限</th>
                <th>优先级</th>
                <th class="text-center">状态</th>
                <th>column1</th>
                <th>column2</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="issue in issueList" :key="issue.id" class="hover-row">
                <td class="font-mono">{{ issue.issueNo }}</td>
                <td class="font-bold">{{ issue.title }}</td>
                <td>{{ issue.department }}</td>
                <td :class="{ 'text-error': issue.status === 'overdue' }">
                  {{ issue.deadline }}
                  <span v-if="issue.status === 'overdue'" class="overdue-tag">(逾期)</span>
                </td>
                <td>
                  <span :class="'priority-chip priority-' + issue.priority">{{ issue.priorityText }}</span>
                </td>
                <td class="text-center">
                  <span :class="'status-chip status-' + issue.status">{{ issue.statusText }}</span>
                </td>
                <td>{{ issue.column1 }}</td>
                <td>{{ issue.column2 }}</td>
                <td class="text-right">
                  <button class="action-link" @click="openDetailModal(issue)">详情</button>
                  <button class="action-link" :class="{ disabled: issue.status === 'completed' }" :disabled="issue.status === 'completed'" @click="openEditModal(issue)">编辑</button>
                  <button v-if="issue.status === 'pending'" class="action-link text-error font-bold" @click="urgeIssue(issue)">
                    <i class="el-icon-bell text-icon" />催办
                  </button>
                  <button v-if="issue.status === 'completed'" class="action-link text-error" @click="deleteIssueConfirm(issue)">删除</button>
                </td>
              </tr>
            </tbody>
          </table>
          <!-- Pagination -->
          <div class="pagination">
            <span class="pagination-info">共计 {{ total }} 条整改问题</span>
            <div class="pagination-controls">
              <button class="page-btn" :disabled="currentPage === 1" @click="prevPage">
                <i class="el-icon-arrow-left" />
              </button>
              <button v-for="page in pageList" :key="page" class="page-btn" :class="{ active: currentPage === page }" @click="goToPage(page)">
                {{ page }}
              </button>
              <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage">
                <i class="el-icon-arrow-right" />
              </button>
              <select v-model="pageSize" class="page-size-select" @change="handlePageSizeChange">
                <option :value="10">10 条/页</option>
                <option :value="20">20 条/页</option>
                <option :value="50">50 条/页</option>
              </select>
            </div>
          </div>
        </div>

        <!-- Stats Overview -->
        <section class="stats-section">
          <div class="stats-card stats-progress">
            <h4 class="stats-title">整改进度可视化</h4>
            <div class="progress-bar">
              <div class="progress-segment bg-completed" :style="{ width: completedPercent + '%' }" />
              <div class="progress-segment bg-rectifying" :style="{ width: rectifyingPercent + '%' }" />
              <div class="progress-segment bg-pending" :style="{ width: pendingPercent + '%' }" />
              <div class="progress-segment bg-overdue" :style="{ width: overduePercent + '%' }" />
            </div>
            <div class="progress-legend">
              <div class="legend-item"><span class="legend-dot bg-completed" />已完成 ({{ completedPercent }}%)</div>
              <div class="legend-item"><span class="legend-dot bg-rectifying" />整改中 ({{ rectifyingPercent }}%)</div>
              <div class="legend-item"><span class="legend-dot bg-pending" />待处理 ({{ pendingPercent }}%)</div>
              <div class="legend-item"><span class="legend-dot bg-overdue" />已逾期 ({{ overduePercent }}%)</div>
            </div>
          </div>
        </section>
      </section>
    </main>

    <!-- AddModal: 新增/编辑 -->
    <AddModal
      :visible="modalVisible"
      :is-edit="isEdit"
      :form-data="formData"
      @close="closeModal"
      @submit="handleFormSubmit"
      @error="handleFormError"
    />

    <!-- DetailModal: 详情 -->
    <DetailModal
      :visible="detailVisible"
      :data="detailData"
      @close="detailVisible = false"
    />

    <!-- DeleteModal: 删除确认 -->
    <DeleteModal
      :visible="deleteVisible"
      :data="deleteIssueData"
      @close="deleteVisible = false"
      @confirm="handleDeleteConfirm"
    />

    <!-- UrgeModal: 催办确认 -->
    <UrgeModal
      :visible="urgeVisible"
      :data="urgeIssueData"
      @close="urgeVisible = false"
      @confirm="handleUrgeConfirm"
    />

    <!-- ImportModal: 批量导入 -->
    <ImportModal
      :visible="importVisible"
      @close="importVisible = false"
      @import="handleImportFile"
    />
  </div>
</template>

<script>
import {
  getIssueList,
  createIssue,
  updateIssue,
  deleteIssue,
  getIssueStats
} from '@/api/issue'
import AddModal from './components/AddModal.vue'
import DetailModal from './components/DetailModal.vue'
import DeleteModal from './components/DeleteModal.vue'
import UrgeModal from './components/UrgeModal.vue'
import ImportModal from './components/ImportModal.vue'

export default {
  name: 'Dashboard',
  components: {
    AddModal,
    DetailModal,
    DeleteModal,
    UrgeModal,
    ImportModal
  },
  data() {
    return {
      loading: false,
      batchLoading: false,
      issueList: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      totalPages: 1,
      filters: {
        status: '全部状态',
        department: '所有部门',
        priority: '全部',
        column1: '',
        column2: '',
        column3: '',
        column4: '',
        column5: '',
        column6: ''
      },
      stats: {
        total: 0,
        pending: 0,
        rectifying: 0,
        overdue: 0,
        completed: 0
      },
      modalVisible: false,
      isEdit: false,
      formData: {
        id: '',
        issueNo: '',
        title: '',
        department: '',
        deadline: '',
        priority: 'medium',
        status: 'pending',
        description: ''
      },
      detailVisible: false,
      detailData: {},
      deleteVisible: false,
      deleteIssueData: {},
      urgeVisible: false,
      urgeIssueData: {},
      importVisible: false
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
    },
    completedPercent() {
      return this.stats.total ? Math.round((this.stats.completed / this.stats.total) * 100) : 0
    },
    rectifyingPercent() {
      return this.stats.total ? Math.round((this.stats.rectifying / this.stats.total) * 100) : 0
    },
    pendingPercent() {
      return this.stats.total ? Math.round((this.stats.pending / this.stats.total) * 100) : 0
    },
    overduePercent() {
      return this.stats.total ? Math.round((this.stats.overdue / this.stats.total) * 100) : 0
    }
  },
  mounted() {
    this.fetchData()
    this.fetchStats()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const response = await getIssueList({
          page: this.currentPage,
          limit: this.pageSize,
          ...this.filters
        })
        this.issueList = response.data.items
        this.total = response.data.total
        this.totalPages = Math.ceil(this.total / this.pageSize)
      } catch (error) {
        console.error('获取问题列表失败:', error)
      } finally {
        this.loading = false
      }
    },
    async fetchStats() {
      try {
        const response = await getIssueStats()
        this.stats = response.data
      } catch (error) {
        console.error('获取统计数据失败:', error)
      }
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
    handlePageSizeChange() {
      this.currentPage = 1
      this.fetchData()
    },
    handleSearch() {
      this.currentPage = 1
      this.fetchData()
    },
    handleStatCardClick(status) {
      this.filters.status = status
      this.currentPage = 1
      this.fetchData()
    },
    resetFilters() {
      this.filters = {
        status: '全部状态',
        department: '所有部门',
        priority: '全部',
        column1: '',
        column2: '',
        column3: '',
        column4: '',
        column5: '',
        column6: ''
      }
      this.currentPage = 1
      this.fetchData()
    },
    handleImport() {
      this.importVisible = true
    },
    handleImportFile(file) {
      console.log('选择的文件:', file.name)
      alert(`已选择文件: ${file.name}`)
      this.fetchData()
    },
    handleExport() {
      this.batchLoading = true
      // 模拟导出延迟
      setTimeout(() => {
        // 导出为 Excel 或 CSV
        const headers = ['问题编号', '问题标题', '负责部门', '整改时限', '优先级', '状态']
        const rows = this.issueList.map(item => [
          item.issueNo,
          item.title,
          item.department,
          item.deadline,
          item.priorityText,
          item.statusText
        ])

        let csvContent = 'data:text/csv;charset=utf-8,'
        csvContent += headers.join(',') + '\n'
        rows.forEach(row => {
          csvContent += row.join(',') + '\n'
        })

        const link = document.createElement('a')
        link.href = encodeURI(csvContent)
        link.download = '问题列表_' + new Date().toISOString().split('T')[0] + '.csv'
        link.click()
        this.batchLoading = false
      }, 500)
    },
    openCreateModal() {
      this.isEdit = false
      this.formData = {
        id: '',
        issueNo: '自动生成',
        title: '',
        department: '',
        deadline: '',
        priority: 'medium',
        status: 'pending',
        description: ''
      }
      this.modalVisible = true
    },
    openEditModal(issue) {
      this.isEdit = true
      this.formData = {
        id: issue.id,
        issueNo: issue.issueNo,
        title: issue.title,
        department: issue.department,
        deadline: issue.deadline,
        priority: issue.priority,
        status: issue.status,
        description: issue.description || ''
      }
      this.modalVisible = true
    },
    closeModal() {
      this.modalVisible = false
    },
    async handleFormSubmit(formData) {
      try {
        if (this.isEdit) {
          await updateIssue(formData.id, formData)
        } else {
          await createIssue(formData)
        }
        this.closeModal()
        this.fetchData()
        this.fetchStats()
        alert(this.isEdit ? '修改成功' : '创建成功')
      } catch (error) {
        console.error('提交失败:', error)
        alert('操作失败')
      }
    },
    handleFormError(message) {
      alert(message)
    },
    openDetailModal(issue) {
      this.detailData = issue
      this.detailVisible = true
    },
    deleteIssueConfirm(issue) {
      this.deleteIssueData = issue
      this.deleteVisible = true
    },
    async handleDeleteConfirm(id) {
      try {
        await deleteIssue(id)
        this.deleteVisible = false
        this.fetchData()
        this.fetchStats()
        alert('删除成功')
      } catch (error) {
        console.error('删除失败:', error)
        alert('删除失败')
      }
    },
    urgeIssue(issue) {
      this.urgeIssueData = issue
      this.urgeVisible = true
    },
    handleUrgeConfirm(issue) {
      alert(`已向 ${issue.department} 发送催办通知`)
      this.urgeVisible = false
    },
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background-color: #F5F7FA;
  font-family: 'Inter', sans-serif;
  color: #1b1c1c;
}

.main-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.page-content {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
}

.breadcrumb {
  display: flex;
  gap: 8px;
  font-size: 12px;
  line-height: 20px;
  color: #414755;
  margin-bottom: 8px;

  .active {
    color: #a20513;
    font-weight: 600;
  }
}

.page-heading {
  font-size: 30px;
  line-height: 38px;
  font-weight: 600;
  color: #1b1c1c;
}

.btn-primary {
  display: flex;
  align-items: center;
  padding: 10px 24px;
  background-color: #a20513;
  color: white;
  border-radius: 8px;
  font-weight: 600;
  font-size: 13px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.1);
  transition: all 0.2s;
  cursor: pointer;
  border: none;

  &:hover {
    opacity: 0.9;
  }

  &:active {
    transform: scale(0.95);
  }
}

.btn-secondary {
  padding: 10px 24px;
  background-color: #faf9f9;
  color: #414755;
  border-radius: 8px;
  font-weight: 600;
  border: 1px solid #e4beba;
  transition: all 0.2s;
  cursor: pointer;

  &:hover {
    background-color: #efeded;
  }
}

.btn-danger {
  padding: 10px 24px;
  background-color: #ba1a1a;
  color: white;
  border-radius: 8px;
  font-weight: 600;
  border: none;
  transition: all 0.2s;
  cursor: pointer;

  &:hover {
    background-color: #93000a;
  }
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}

.stat-card {
  background-color: white;
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
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
  }

  &.active {
    background-color: #a20513;
    border-color: #a20513;

    .stat-icon {
      background: linear-gradient(135deg, rgba(255,255,255,0.2), rgba(255,255,255,0.4)) !important;
      color: white;
    }

    .stat-value,
    .stat-label {
      color: white;
    }
  }
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: white;
}

.stat-total .stat-icon { background: linear-gradient(135deg, #a20513, #c91827); }
.stat-pending .stat-icon { background: linear-gradient(135deg, #a20513, #ffdad6); }
.stat-rectifying .stat-icon { background: linear-gradient(135deg, #7d5400, #ffba45); }
.stat-overdue .stat-icon { background: linear-gradient(135deg, #ba1a1a, #ffdad6); }
.stat-completed .stat-icon { background: linear-gradient(135deg, #266d00, #85fa51); }

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
  color: #414755;
  margin: 4px 0 0;
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

.filter-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(228, 190, 186, 0.3);
}

.filter-actions-left {
  display: flex;
  gap: 8px;
}

.filter-actions-right {
  display: flex;
  gap: 8px;
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
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: rgba(162, 5, 19, 0.9);
  }
}

.btn-import {
  padding: 0 16px;
  height: 36px;
  border: 1px solid #67C23A;
  border-radius: 4px;
  font-size: 13px;
  font-weight: bold;
  color: white;
  background: #67C23A;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: #5aad32;
  }
}

.btn-export {
  padding: 0 16px;
  height: 36px;
  border: 1px solid #E6A23C;
  border-radius: 4px;
  font-size: 13px;
  font-weight: bold;
  color: white;
  background: #E6A23C;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: #d3922f;
  }
}

.table-card {
  background-color: white;
  border: 1px solid #e4beba;
  border-radius: 8px;
  overflow: hidden;
}

.table-loading-wrapper {
  position: relative;
}

.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  border-radius: 8px;
}

.loading-spinner {
  text-align: center;
  color: #C53030;

  .el-icon-loading {
    font-size: 40px;
    display: block;
    margin-bottom: 8px;
  }

  p {
    font-size: 14px;
    color: #606266;
    margin: 0;
  }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.spinning,
.loading-icon {
  animation: spin 1s linear infinite;
}

.data-table {
  width: 100%;
  text-align: left;
  border-collapse: collapse;

  th {
    padding: 12px 16px;
    font-weight: 700;
    color: #414755;
    background-color: #FAFAFA;
    border-bottom: 1px solid #e4beba;
  }

  td {
    padding: 12px 16px;
    border-bottom: 1px solid #e4beba;
  }

  .hover-row:hover {
    background-color: #FFF5F5;
  }
}

.font-mono {
  font-family: monospace;
  font-size: 14px;
}

.font-bold {
  font-weight: 700;
}

.text-center {
  text-align: center;
}

.text-right {
  text-align: right;
}

.text-error {
  color: #ba1a1a;
}

.status-chip {
  padding: 2px 8px;
  border-radius: 2px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  border: 1px solid transparent;
}

.status-pending {
  background-color: rgba(162, 5, 19, 0.1);
  border-color: #a20513;
  color: #a20513;
}

.status-rectifying {
  background-color: rgba(125, 84, 0, 0.1);
  border-color: #7d5400;
  color: #7d5400;
}

.status-overdue {
  background-color: rgba(186, 26, 26, 0.1);
  border-color: #ba1a1a;
  color: #ba1a1a;
}

.status-completed {
  background-color: rgba(38, 109, 0, 0.1);
  border-color: #266d00;
  color: #266d00;
}

.priority-chip {
  padding: 2px 8px;
  border-radius: 2px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  border: 1px solid transparent;
}

.priority-high {
  background-color: rgba(186, 26, 26, 0.1);
  border-color: #ba1a1a;
  color: #ba1a1a;
}

.priority-medium {
  background-color: rgba(125, 84, 0, 0.1);
  border-color: #7d5400;
  color: #7d5400;
}

.priority-low {
  background-color: rgba(162, 5, 19, 0.1);
  border-color: #a20513;
  color: #a20513;
}

.overdue-tag {
  font-size: 10px;
  margin-left: 4px;
  padding: 1px 4px;
  background-color: #ffdad6;
  border-radius: 2px;
}

.action-link {
  color: #a20513;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  margin-right: 8px;

  &:hover {
    text-decoration: underline;
  }

  &.disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }
}

.text-icon {
  font-size: 16px;
  vertical-align: middle;
}

.pagination {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  background-color: #faf9f9;
  border-top: 1px solid #e4beba;
}

.pagination-info {
  font-size: 12px;
  color: #414755;
}

.pagination-controls {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-btn {
  width: 32px;
  height: 32px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.2s;

  &:hover:not(.active):not(:disabled) {
    background-color: #efeded;
  }

  &.active {
    background-color: #a20513;
    color: white;
    font-weight: 700;
  }

  &:disabled {
    opacity: 0.3;
    cursor: not-allowed;
  }
}

.page-size-select {
  margin-left: 16px;
  background: transparent;
  border: 1px solid #727786;
  border-radius: 4px;
  font-size: 12px;
  padding: 4px;
}

.stats-section {
  padding: 0 24px 24px;
}

.stats-card {
  background-color: white;
  padding: 24px;
  border: 1px solid #e4beba;
  border-radius: 8px;
}

.stats-progress {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.stats-title {
  font-size: 16px;
  line-height: 24px;
  font-weight: 600;
  margin-bottom: 16px;
}

.progress-bar {
  height: 12px;
  background-color: #efeded;
  border-radius: 9999px;
  overflow: hidden;
  display: flex;
}

.progress-segment {
  height: 100%;
}

.progress-legend {
  display: flex;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 12px;
  font-weight: 700;
  color: #414755;
}

.legend-item {
  display: flex;
  align-items: center;
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  margin-right: 8px;
}

.bg-completed { background-color: #266d00; }
.bg-rectifying { background-color: #7d5400; }
.bg-pending { background-color: #a20513; }
.bg-overdue { background-color: #ba1a1a; }

</style>
