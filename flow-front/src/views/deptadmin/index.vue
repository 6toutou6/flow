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
              <span class="active">系统管理员</span>
            </nav>
            <h3 class="page-heading">系统管理员管理</h3>
          </div>
        </div>

        <!-- 统计卡 -->
        <section class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon icon-total"><i class="el-icon-user" /></div>
            <div class="stat-body">
              <div class="stat-label">管理员总数</div>
              <div class="stat-value">{{ stats.adminCount || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-dept"><i class="el-icon-office-building" /></div>
            <div class="stat-body">
              <div class="stat-label">覆盖部门数</div>
              <div class="stat-value">{{ stats.deptCount || 0 }}</div>
            </div>
          </div>
        </section>

        <!-- Filter Section -->
        <section class="filter-section">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">管理员姓名</label>
              <input v-model="filters.adminName" class="filter-input" placeholder="请输入姓名">
            </div>
            <div class="filter-item">
              <label class="filter-label">用户号</label>
              <input v-model="filters.adminYstId" class="filter-input" placeholder="请输入用户号">
            </div>
            <div class="filter-item">
              <label class="filter-label">部门名称</label>
              <input v-model="filters.deptName" class="filter-input" placeholder="请输入部门名称">
            </div>
          </div>
          <div class="filter-actions">
            <div class="filter-actions-left">
              <button class="btn-primary" @click="openCreateModal">
                <i class="el-icon-plus" />
                新增管理员
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
                <th>管理员姓名</th>
                <th>用户号</th>
                <th>部门</th>
                <th>部门邮箱</th>
                <th>创建时间</th>
                <th>修改时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in filteredList" :key="row.adminYstId + row.deptId" class="hover-row">
                <td class="font-bold">{{ row.adminName }}</td>
                <td class="font-mono">{{ row.adminYstId }}</td>
                <td>{{ row.deptName || row.deptId }}</td>
                <td>{{ row.deptEmail || '—' }}</td>
                <td>{{ row.createTime }}</td>
                <td>{{ row.modifiedTime }}</td>
                <td class="text-right">
                  <button class="action-link" @click="openEditModal(row)">编辑</button>
                  <button class="action-link text-error" @click="deleteConfirm(row)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && filteredList.length === 0">
                <td colspan="7" class="text-center" style="padding: 32px; color: #999;">暂无数据</td>
              </tr>
            </tbody>
          </table>
          <div class="pagination">
            <span class="pagination-info">共计 {{ filteredList.length }} 条管理员</span>
          </div>
        </div>
      </section>
    </main>

    <!-- AddEditModal: 新增/编辑 -->
    <AddEditModal
      :visible="modalVisible"
      :is-edit="isEdit"
      :form-data="formData"
      @close="closeModal"
      @submit="handleFormSubmit"
    />

    <!-- DeleteModal: 删除确认 -->
    <DeleteModal
      :visible="deleteVisible"
      :data="deleteData"
      @close="deleteVisible = false"
      @confirm="handleDeleteConfirm"
    />
  </div>
</template>

<script>
import { getDeptAdminList, addDeptAdmin, updateDeptAdmin, deleteDeptAdmin } from '@/service/base/DeptAdminService'
import AddEditModal from './components/AddEditModal.vue'
import DeleteModal from './components/DeleteModal.vue'

export default {
  name: 'DeptAdmin',
  components: { AddEditModal, DeleteModal },
  data() {
    return {
      loading: false,
      adminList: [],
      stats: {},
      filters: {
        adminName: '',
        adminYstId: '',
        deptName: ''
      },
      modalVisible: false,
      isEdit: false,
      formData: {},
      deleteVisible: false,
      deleteData: {}
    }
  },
  computed: {
    filteredList() {
      const { adminName, adminYstId, deptName } = this.filters
      const kw = (s) => (s || '').toLowerCase().trim()
      return this.adminList.filter(row => {
        if (adminName && !kw(row.adminName).includes(kw(adminName))) return false
        if (adminYstId && !kw(row.adminYstId).includes(kw(adminYstId))) return false
        if (deptName && !kw(row.deptName).includes(kw(deptName))) return false
        return true
      })
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const res = await getDeptAdminList()
        this.adminList = res.data || []
        const depts = new Set(this.adminList.map(r => r.deptId))
        this.stats = {
          adminCount: this.adminList.length,
          deptCount: depts.size
        }
      } catch (e) {
        console.error('获取部门管理员列表失败:', e)
      } finally {
        this.loading = false
      }
    },
    handleSearch() { this.fetchData() },
    resetFilters() {
      this.filters = { adminName: '', adminYstId: '', deptName: '' }
      this.fetchData()
    },
    openCreateModal() {
      this.isEdit = false
      this.formData = {}
      this.modalVisible = true
    },
    openEditModal(row) {
      this.isEdit = true
      this.formData = { ...row }
      this.modalVisible = true
    },
    closeModal() {
      this.modalVisible = false
    },
    async handleFormSubmit(formData) {
      try {
        if (this.isEdit) {
          await updateDeptAdmin(formData)
        } else {
          await addDeptAdmin(formData)
        }
        this.closeModal()
        this.fetchData()
        this.$message.success(this.isEdit ? '修改成功' : '添加成功')
      } catch (error) {
        console.error('提交失败:', error)
        this.$message.error((error && error.message) || '提交失败')
      }
    },
    deleteConfirm(row) {
      this.deleteData = row
      this.deleteVisible = true
    },
    async handleDeleteConfirm(body) {
      try {
        await deleteDeptAdmin(body)
        this.deleteVisible = false
        this.fetchData()
        this.$message.success('删除成功')
      } catch (error) {
        console.error('删除失败:', error)
        this.$message.error((error && error.message) || '删除失败')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.dashboard-container {
  display: flex;
  min-height: 100vh;
  background-color: var(--color-primary-surface);
  color: #1b1c1c;
}
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: var(--color-primary); font-weight: 600; }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }

.stats-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px;
  @media (max-width: 600px) { grid-template-columns: 1fr; }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid #CBD5E1; border-radius: 3px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 48px; height: 48px; border-radius: 3px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: var(--color-primary); }
  &.icon-dept { background: #727786; }
}
.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #757575; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }

.btn-primary { display: flex; align-items: center; padding: 10px 24px; background-color: var(--color-primary); color: white; border-radius: 3px; font-weight: 600; font-size: 13px; box-shadow: 0 1px 2px rgba(0,0,0,0.1); transition: all 0.2s; cursor: pointer; border: none;
  &:hover { opacity: 0.9; }
}
.filter-section { background: white; border: 1px solid #CBD5E1; border-radius: 3px; padding: 16px; margin-bottom: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(1, 1fr); gap: 16px;
  @media (min-width: 768px) { grid-template-columns: repeat(3, 1fr); }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-select, .filter-input { height: 36px; border: 1px solid #CBD5E1; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; transition: all 0.2s;
  &:focus { border-color: var(--color-primary); box-shadow: 0 0 0 1px rgba(var(--color-primary-rgb), 0.2); }
}
.filter-actions { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(var(--color-primary-rgb), 0.08); }
.filter-actions-left { display: flex; gap: 8px; }
.filter-actions-right { display: flex; gap: 8px; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid #CBD5E1; border-radius: 4px; font-size: 13px; color: var(--color-primary); background: white; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 4px; font-size: 13px; font-weight: bold; color: white; background: var(--color-primary); cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { background: rgba(var(--color-primary-rgb), 0.9); }
}
.table-card { background-color: white; border: 1px solid #CBD5E1; border-radius: 3px; overflow: hidden; }
.table-loading-wrapper { position: relative; }
.loading-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(255, 255, 255, 0.9); display: flex; align-items: center; justify-content: center; z-index: 10; border-radius: 3px; }
.loading-spinner { text-align: center; color: var(--color-primary);
  .el-icon-loading { font-size: 40px; display: block; margin-bottom: 8px; }
  p { font-size: 14px; color: #606266; margin: 0; }
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spinning { animation: spin 1s linear infinite; }
.data-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 12px 16px; font-weight: 700; color: #414755; background-color: var(--color-primary-light); border-bottom: 1px solid #CBD5E1; }
  td { padding: 12px 16px; border-bottom: 1px solid #CBD5E1; }
  .hover-row:hover { background-color: var(--color-primary-light); }
}
.font-mono { font-family: monospace; font-size: 14px; }
.font-bold { font-weight: 700; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.text-error { color: #DC2626; }
.action-link { color: var(--color-primary); background: none; border: none; cursor: pointer; font-size: 14px; margin-right: 8px;
  &:hover { text-decoration: underline; }
}
.pagination { display: flex; justify-content: space-between; align-items: center; padding: 16px; background-color: #faf9f9; border-top: 1px solid #CBD5E1; }
.pagination-info { font-size: 12px; color: #414755; }
</style>
