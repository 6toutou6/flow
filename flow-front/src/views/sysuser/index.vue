<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- Page Header -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>系统首页</span>
              <span>/</span>
              <span class="active">用户管理</span>
            </nav>
            <h3 class="page-heading">系统用户管理</h3>
          </div>
        </div>

        <!-- 统计卡 -->
        <section class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon icon-total"><i class="el-icon-user" /></div>
            <div class="stat-body">
              <div class="stat-label">用户总数</div>
              <div class="stat-value">{{ stats.total || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-normal"><i class="el-icon-circle-check" /></div>
            <div class="stat-body">
              <div class="stat-label">正常用户</div>
              <div class="stat-value">{{ stats.normalCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-disabled"><i class="el-icon-circle-close" /></div>
            <div class="stat-body">
              <div class="stat-label">禁用用户</div>
              <div class="stat-value">{{ stats.disabledCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-dept"><i class="el-icon-office-building" /></div>
            <div class="stat-body">
              <div class="stat-label">部门数</div>
              <div class="stat-value">{{ stats.deptCount || 0 }}</div>
            </div>
          </div>
        </section>

        <!-- Filter Section -->
        <section class="filter-section">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">用户名</label>
              <input v-model="filters.username" class="filter-input" placeholder="请输入用户名">
            </div>
            <div class="filter-item">
              <label class="filter-label">员工号</label>
              <input v-model="filters.empNo" class="filter-input" placeholder="请输入员工号">
            </div>
            <div class="filter-item">
              <label class="filter-label">员工姓名</label>
              <input v-model="filters.realName" class="filter-input" placeholder="请输入员工姓名">
            </div>
            <div class="filter-item">
              <label class="filter-label">部门名称</label>
              <input v-model="filters.deptName" class="filter-input" placeholder="请输入部门名称">
            </div>
            <div class="filter-item">
              <label class="filter-label">状态</label>
              <select v-model="filters.status" class="filter-select">
                <option value="">全部状态</option>
                <option :value="1">正常</option>
                <option :value="0">禁用</option>
              </select>
            </div>
          </div>
          <div class="filter-actions">
            <div class="filter-actions-left">
              <button class="btn-primary" @click="openCreateModal">
                <i class="el-icon-plus" />
                新增用户
              </button>
            </div>
            <div class="filter-actions-right">
              <button class="btn-reset" :disabled="loading" @click="resetFilters">重置</button>
              <button class="btn-search" :disabled="loading" @click="handleSearch">
                <i v-if="loading" class="el-icon-loading" />
                <span v-else>查询</span>
              </button>
            </div>
          </div>
        </section>

        <!-- Data Table -->
        <div class="table-card table-loading-wrapper">
          <div v-if="loading" class="loading-overlay">
            <div class="loading-spinner">
              <i class="el-icon-loading spinning" />
              <p>加载中...</p>
            </div>
          </div>
          <table class="data-table">
            <thead>
              <tr>
                <th>员工号</th>
                <th>用户名</th>
                <th>员工姓名</th>
                <th>部门ID</th>
                <th>部门名称</th>
                <th>手机号</th>
                <th class="text-center">状态</th>
                <th>创建时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in userList" :key="user.id" class="hover-row">
                <td class="font-mono">{{ user.empNo }}</td>
                <td class="font-bold">{{ user.username }}</td>
                <td>{{ user.realName }}</td>
                <td>{{ user.deptId }}</td>
                <td>{{ user.deptName }}</td>
                <td>{{ user.phone }}</td>
                <td class="text-center">
                  <span :class="user.status === 1 ? 'status-chip status-normal' : 'status-chip status-disabled'">
                    {{ user.status === 1 ? '正常' : '禁用' }}
                  </span>
                </td>
                <td>{{ user.createTime }}</td>
                <td class="text-right">
                  <button class="action-link" @click="openDetailModal(user)">详情</button>
                  <button class="action-link" @click="openEditModal(user)">编辑</button>
                  <button class="action-link text-error" @click="deleteUserConfirm(user)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && userList.length === 0">
                <td colspan="9" class="text-center" style="padding: 32px; color: #999;">暂无数据</td>
              </tr>
            </tbody>
          </table>
          <!-- Pagination -->
          <div class="pagination">
            <span class="pagination-info">共计 {{ total }} 条用户</span>
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
      :data="deleteUserData"
      @close="deleteVisible = false"
      @confirm="handleDeleteConfirm"
    />
  </div>
</template>

<script>
import { getUserList, getSysUserStats, addUser, updateUser, deleteUser } from '@/api/sysuser'
import AddModal from './components/AddModal.vue'
import DetailModal from './components/DetailModal.vue'
import DeleteModal from './components/DeleteModal.vue'

export default {
  name: 'SysUser',
  components: { AddModal, DetailModal, DeleteModal },
  data() {
    return {
      loading: false,
      userList: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      totalPages: 1,
      // 统计卡
      stats: {},
      filters: {
        username: '',
        empNo: '',
        realName: '',
        deptName: '',
        status: ''
      },
      modalVisible: false,
      isEdit: false,
      formData: {},
      detailVisible: false,
      detailData: {},
      deleteVisible: false,
      deleteUserData: {}
    }
  },
  computed: {
    pageList() {
      const pages = []
      const total = this.totalPages
      const current = this.currentPage
      if (total <= 5) {
        for (let i = 1; i <= total; i++) pages.push(i)
      } else if (current <= 3) {
        pages.push(1, 2, 3, 4, 5)
      } else if (current >= total - 2) {
        pages.push(total - 4, total - 3, total - 2, total - 1, total)
      } else {
        pages.push(current - 2, current - 1, current, current + 1, current + 2)
      }
      return pages
    }
  },
  mounted() {
    this.fetchData()
    this.fetchStats()
  },
  methods: {
    async fetchStats() {
      try {
        const res = await getSysUserStats()
        this.stats = res.data || {}
      } catch (e) {
        console.error('获取用户统计失败:', e)
      }
    },
    async fetchData() {
      this.loading = true
      try {
        const response = await getUserList({
          page: this.currentPage,
          limit: this.pageSize,
          ...this.filters
        })
        this.userList = response.data.records
        this.total = response.data.total
        this.totalPages = Math.ceil(this.total / this.pageSize) || 1
      } catch (error) {
        console.error('获取用户列表失败:', error)
      } finally {
        this.loading = false
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
    resetFilters() {
      this.filters = { username: '', empNo: '', realName: '', deptName: '', status: '' }
      this.currentPage = 1
      this.fetchData()
    },
    openCreateModal() {
      this.isEdit = false
      this.formData = {
        id: null,
        username: '',
        empNo: '',
        realName: '',
        deptId: null,
        deptName: '',
        password: '',
        phone: '',
        status: 1
      }
      this.modalVisible = true
    },
    openEditModal(user) {
      this.isEdit = true
      this.formData = { ...user, password: '' }
      this.modalVisible = true
    },
    closeModal() {
      this.modalVisible = false
    },
    async handleFormSubmit(formData) {
      try {
        if (this.isEdit) {
          await updateUser(formData)
        } else {
          await addUser(formData)
        }
        this.closeModal()
        this.fetchData()
        this.$message.success(this.isEdit ? '修改成功' : '创建成功')
      } catch (error) {
        console.error('提交失败:', error)
      }
    },
    handleFormError(message) {
      this.$message.warning(message)
    },
    openDetailModal(user) {
      this.detailData = user
      this.detailVisible = true
    },
    deleteUserConfirm(user) {
      this.deleteUserData = user
      this.deleteVisible = true
    },
    async handleDeleteConfirm(id) {
      try {
        await deleteUser(id)
        this.deleteVisible = false
        this.fetchData()
        this.$message.success('删除成功')
      } catch (error) {
        console.error('删除失败:', error)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background-color: #F8FAFC;
  
  color: #1b1c1c;
}
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: #334155; font-weight: 600; }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px;
  @media (max-width: 1100px) { grid-template-columns: repeat(2, 1fr); }
  @media (max-width: 600px) { grid-template-columns: 1fr; }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid #CBD5E1; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 48px; height: 48px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: #334155; }
  &.icon-normal { background: #15803D; }
  &.icon-disabled { background: #B45309; }
  &.icon-dept { background: #727786; }
}
.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #757575; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }

.btn-primary { display: flex; align-items: center; padding: 10px 24px; background-color: #334155; color: white; border-radius: 3px; font-weight: 600; font-size: 13px; box-shadow: 0 1px 2px rgba(0,0,0,0.1); transition: all 0.2s; cursor: pointer; border: none;
  &:hover { opacity: 0.9; }
}
.filter-section { background: white; border: 1px solid #CBD5E1; border-radius: 3px; padding: 16px; margin-bottom: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(1, 1fr); gap: 16px;
  @media (min-width: 768px) { grid-template-columns: repeat(3, 1fr); }
  @media (min-width: 1024px) { grid-template-columns: repeat(5, 1fr); }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-select, .filter-input { height: 36px; border: 1px solid #CBD5E1; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; transition: all 0.2s;
  &:focus { border-color: #334155; box-shadow: 0 0 0 1px rgba(51, 65, 85, 0.2); }
}
.filter-actions { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(51, 65, 85, 0.08); }
.filter-actions-left { display: flex; gap: 8px; }
.filter-actions-right { display: flex; gap: 8px; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid #CBD5E1; border-radius: 4px; font-size: 13px; color: #334155; background: white; cursor: pointer;
  &:hover { background: #F1F5F9; }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 4px; font-size: 13px; font-weight: bold; color: white; background: #334155; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { background: rgba(51, 65, 85, 0.9); }
}
.table-card { background-color: white; border: 1px solid #CBD5E1; border-radius: 3px; overflow: hidden; }
.table-loading-wrapper { position: relative; }
.loading-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(255, 255, 255, 0.9); display: flex; align-items: center; justify-content: center; z-index: 10; border-radius: 3px; }
.loading-spinner { text-align: center; color: #334155;
  .el-icon-loading { font-size: 40px; display: block; margin-bottom: 8px; }
  p { font-size: 14px; color: #606266; margin: 0; }
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spinning { animation: spin 1s linear infinite; }
.data-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 12px 16px; font-weight: 700; color: #414755; background-color: #F1F5F9; border-bottom: 1px solid #CBD5E1; }
  td { padding: 12px 16px; border-bottom: 1px solid #CBD5E1; }
  .hover-row:hover { background-color: #F1F5F9; }
}
.font-mono { font-family: monospace; font-size: 14px; }
.font-bold { font-weight: 700; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.text-error { color: #DC2626; }
.status-chip { padding: 2px 8px; border-radius: 4px; font-size: 12px; font-weight: 600; display: inline-flex; align-items: center; border: 1px solid transparent; }
.status-normal { background-color: rgba(21, 128, 61, 0.1); border-color: #15803D; color: #15803D; }
.status-disabled { background-color: rgba(114, 119, 134, 0.1); border-color: #727786; color: #727786; }
.action-link { color: #334155; background: none; border: none; cursor: pointer; font-size: 14px; margin-right: 8px;
  &:hover { text-decoration: underline; }
}
.pagination { display: flex; justify-content: space-between; align-items: center; padding: 16px; background-color: #faf9f9; border-top: 1px solid #CBD5E1; }
.pagination-info { font-size: 12px; color: #414755; }
.pagination-controls { display: flex; align-items: center; gap: 4px; }
.page-btn { width: 32px; height: 32px; border-radius: 4px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; cursor: pointer; font-size: 14px; transition: background-color 0.2s;
  &:hover:not(.active):not(:disabled) { background-color: #efeded; }
  &.active { background-color: #334155; color: white; font-weight: 700; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.page-size-select { margin-left: 16px; background: transparent; border: 1px solid #727786; border-radius: 4px; font-size: 12px; padding: 4px; }
</style>
