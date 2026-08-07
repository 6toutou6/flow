<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span>系统首页</span>
              <span>/</span>
              <span class="active">模板管理</span>
            </nav>
            <h3 class="page-heading">流程模板管理</h3>
          </div>
        </div>

        <!-- 统计卡 -->
        <section class="stats-grid">
          <div class="stat-card">
            <div class="stat-icon icon-total"><i class="el-icon-document" /></div>
            <div class="stat-body">
              <div class="stat-label">模板总数</div>
              <div class="stat-value">{{ stats.total || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-active"><i class="el-icon-circle-check" /></div>
            <div class="stat-body">
              <div class="stat-label">启用中流程</div>
              <div class="stat-value">{{ stats.active || 0 }}</div>
            </div>
          </div>
          <div class="stat-card">
            <div class="stat-icon icon-rate"><i class="el-icon-data-line" /></div>
            <div class="stat-body">
              <div class="stat-label">本月更新率</div>
              <div class="stat-value">{{ stats.monthlyUpdateRate != null ? stats.monthlyUpdateRate + '%' : '0%' }}</div>
            </div>
          </div>
        </section>

        <!-- 筛选区 -->
        <section class="filter-section">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">模板名称</label>
              <input v-model="filters.templateName" class="filter-input" placeholder="请输入模板名称" @keyup.enter="handleSearch">
            </div>
            <div class="filter-item">
              <label class="filter-label">分类</label>
              <input v-model="filters.category" class="filter-input" placeholder="请输入分类" @keyup.enter="handleSearch">
            </div>
            <div class="filter-item">
              <label class="filter-label">状态</label>
              <select v-model="filters.status" class="filter-select">
                <option value="">全部状态</option>
                <option :value="1">启用</option>
                <option :value="0">停用</option>
              </select>
            </div>
          </div>
          <div class="filter-actions">
            <div class="filter-actions-left">
              <button class="btn-primary" @click="openCreateModal">
                <i class="el-icon-plus" /> 新建模板
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

        <!-- 表格 -->
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
                <th>模板名称</th>
                <th>分类</th>
                <th>版本</th>
                <th>创建人</th>
                <th>创建部门</th>
                <th class="text-center">状态</th>
                <th>最后更新</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in list" :key="row.id" class="hover-row">
                <td class="font-bold">
                  {{ row.templateName }}
                  <span v-if="row.isSample === 1" class="sample-tag">样例</span>
                </td>
                <td>{{ row.category || '—' }}</td>
                <td><span class="version-tag">v{{ row.version }}</span></td>
                <td>{{ row.creatorName || '—' }}</td>
                <td>{{ row.deptName || '—' }}</td>
                <td class="text-center">
                  <el-switch
                    :value="row.status === 1"
                    :disabled="isSampleLocked(row)"
                    active-color="#C53030"
                    inactive-color="#dcdfe6"
                    @change="handleToggleStatus(row)"
                  />
                </td>
                <td>{{ row.updateTime || row.createTime }}</td>
                <td class="text-right">
                  <template v-if="isSuperAdmin">
                    <button class="action-link" @click="handleToggleSample(row)">{{ row.isSample === 1 ? '取消样例' : '设为样例' }}</button>
                  </template>
                  <button class="action-link" :disabled="isSampleLocked(row)" :title="isSampleLocked(row) ? '样例模板不可修改，可复制后使用' : ''" @click="goDesigner(row)">设计流程</button>
                  <button class="action-link" :disabled="isSampleLocked(row)" :title="isSampleLocked(row) ? '样例模板不可修改，可复制后使用' : ''" @click="openEditModal(row)">编辑</button>
                  <button class="action-link" @click="handleCopy(row)">复制</button>
                  <button class="action-link text-error" :disabled="isSampleLocked(row)" :title="isSampleLocked(row) ? '样例模板不可删除' : ''" @click="handleDelete(row)">删除</button>
                </td>
              </tr>
              <tr v-if="!loading && list.length === 0">
                <td colspan="8" class="text-center" style="padding: 32px; color: #999;">暂无模板，点击右上角「新建模板」开始</td>
              </tr>
            </tbody>
          </table>
          <!-- 分页 -->
          <div class="pagination">
            <span class="pagination-info">共计 {{ total }} 条模板</span>
            <div class="pagination-controls">
              <button class="page-btn" :disabled="currentPage === 1" @click="prevPage"><i class="el-icon-arrow-left" /></button>
              <button v-for="page in pageList" :key="page" class="page-btn" :class="{ active: currentPage === page }" @click="goToPage(page)">{{ page }}</button>
              <button class="page-btn" :disabled="currentPage === totalPages" @click="nextPage"><i class="el-icon-arrow-right" /></button>
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

    <TemplateFormModal
      :visible.sync="modalVisible"
      :is-edit="isEdit"
      :form-data="formData"
      @submit="handleFormSubmit"
    />
  </div>
</template>

<script>
import { getTemplateList, addTemplate, updateTemplate, toggleTemplateStatus, toggleTemplateSample, copyTemplate, deleteTemplate, getTemplateStats } from '@/api/template'
import TemplateFormModal from './components/TemplateFormModal.vue'

export default {
  name: 'FlowTemplate',
  components: { TemplateFormModal },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      totalPages: 1,
      stats: {},
      filters: { templateName: '', category: '', status: '' },
      modalVisible: false,
      isEdit: false,
      formData: {}
    }
  },
  computed: {
    isSuperAdmin() {
      return !!(this.$store.state.user.userInfo && this.$store.state.user.userInfo.superAdmin)
    },
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
    /** 样例锁定：非超管用户对样例模板不可改/删（复制除外） */
    isSampleLocked(row) {
      return !this.isSuperAdmin && row.isSample === 1
    },
    async handleToggleSample(row) {
      try {
        await toggleTemplateSample(row.id)
        this.$message.success(row.isSample === 1 ? '已取消样例' : '已设为样例')
        this.fetchData()
        this.fetchStats()
      } catch (e) {
        this.$message.error((e && e.message) || '操作失败')
      }
    },
    async fetchData() {
      this.loading = true
      try {
        const res = await getTemplateList({ page: this.currentPage, limit: this.pageSize, ...this.filters })
        this.list = res.data.records
        this.total = res.data.total
        this.totalPages = Math.ceil(this.total / this.pageSize) || 1
      } catch (e) {
        console.error('获取模板列表失败:', e)
      } finally {
        this.loading = false
      }
    },
    async fetchStats() {
      try {
        const res = await getTemplateStats()
        this.stats = res.data || {}
      } catch (e) {
        console.error('获取统计失败:', e)
      }
    },
    prevPage() { if (this.currentPage > 1) { this.currentPage--; this.fetchData() } },
    nextPage() { if (this.currentPage < this.totalPages) { this.currentPage++; this.fetchData() } },
    goToPage(page) { this.currentPage = page; this.fetchData() },
    handlePageSizeChange() { this.currentPage = 1; this.fetchData() },
    handleSearch() { this.currentPage = 1; this.fetchData() },
    resetFilters() {
      this.filters = { templateName: '', category: '', status: '' }
      this.currentPage = 1
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
    async handleFormSubmit(formData, stopLoading, close) {
      try {
        if (formData.id) {
          await updateTemplate(formData)
          this.$message.success('修改成功')
        } else {
          await addTemplate(formData)
          this.$message.success('创建成功')
        }
        this.modalVisible = false
        this.fetchData()
        this.fetchStats()
      } catch (e) {
        stopLoading && stopLoading()
      }
    },
    async handleToggleStatus(row) {
      try {
        await toggleTemplateStatus(row.id)
        this.$message.success('操作成功')
        this.fetchData()
        this.fetchStats()
      } catch (e) {
        console.error(e)
      }
    },
    goDesigner(row) {
      this.$router.push({ path: '/form-designer/index', query: { templateId: row.id, name: row.templateName }})
    },
    async handleCopy(row) {
      try {
        await this.$confirm(`确认复制模板「${row.templateName}」？`, '复制模板', { type: 'warning' })
        await copyTemplate(row.id)
        this.$message.success('复制成功')
        this.fetchData()
        this.fetchStats()
      } catch (e) {
        if (e !== 'cancel') console.error(e)
      }
    },
    async handleDelete(row) {
      try {
        await this.$confirm(`确认删除模板「${row.templateName}」？删除后无法恢复。`, '删除模板', { type: 'warning', confirmButtonText: '删除', confirmButtonClass: 'el-button--danger' })
        await deleteTemplate(row.id)
        this.$message.success('删除成功')
        this.fetchData()
        this.fetchStats()
      } catch (e) {
        if (e !== 'cancel') console.error(e)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
.dashboard-container { display: flex; min-height: 100vh; background-color: #F5F7FA; font-family: 'Inter', sans-serif; color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
}
.page-heading { font-size: 30px; line-height: 38px; font-weight: 600; color: #1b1c1c; }

// 统计卡
.stats-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width: 900px) { grid-template-columns: 1fr; }
}
.stat-card { display: flex; align-items: center; gap: 16px; background: #fff; border: 1px solid #e4beba; border-radius: 8px; padding: 18px 20px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.stat-icon { width: 48px; height: 48px; border-radius: 8px; display: flex; align-items: center; justify-content: center; font-size: 24px; color: #fff; flex-shrink: 0;
  &.icon-total { background: $primary; }
  &.icon-active { background: #266d00; }
  &.icon-rate { background: #8f1d1d; }
}
.stat-body { flex: 1; }
.stat-label { font-size: 13px; color: #757575; margin-bottom: 4px; }
.stat-value { font-size: 28px; font-weight: 700; color: #1b1c1c; line-height: 1.1; }

// 筛选
.filter-section { background: #fff; border: 1px solid #e4beba; border-radius: 8px; padding: 16px; }
.filter-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px;
  @media (max-width: 768px) { grid-template-columns: 1fr; }
}
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-select, .filter-input { height: 36px; border: 1px solid #e4beba; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; transition: all 0.2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 1px rgba(197, 48, 48, 0.2); }
}
.filter-actions { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(228, 190, 186, 0.3); }
.filter-actions-left, .filter-actions-right { display: flex; gap: 8px; }
.btn-primary { display: flex; align-items: center; padding: 10px 24px; background: $primary; color: #fff; border-radius: 8px; font-weight: 600; font-size: 13px; box-shadow: 0 1px 2px rgba(0,0,0,0.1); transition: all 0.2s; cursor: pointer; border: none;
  &:hover { opacity: 0.9; }
}
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid #e4beba; border-radius: 4px; font-size: 13px; color: #5b403d; background: #fff; cursor: pointer;
  &:hover { background: #f6f3f2; }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 4px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
}

// 表格
.table-card { background: #fff; border: 1px solid #e4beba; border-radius: 8px; overflow: hidden; position: relative; }
.loading-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; background: rgba(255,255,255,0.9); display: flex; align-items: center; justify-content: center; z-index: 10; }
.loading-spinner { text-align: center; color: $primary;
  .el-icon-loading { font-size: 40px; display: block; margin-bottom: 8px; }
  p { font-size: 14px; color: #606266; margin: 0; }
}
@keyframes spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
.spinning { animation: spin 1s linear infinite; }
.data-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 12px 16px; font-weight: 700; color: #414755; background: #FAFAFA; border-bottom: 1px solid #e4beba; }
  td { padding: 12px 16px; border-bottom: 1px solid #e4beba; }
  .hover-row:hover { background: #FFF5F5; }
}
.font-bold { font-weight: 700; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.text-error { color: #ba1a1a; }
.version-tag { display: inline-block; padding: 2px 8px; background: rgba(197, 48, 48, 0.1); color: $primary; border-radius: 4px; font-size: 12px; font-weight: 600; }
.sample-tag { display: inline-block; padding: 2px 8px; background: rgba(183, 121, 31, 0.14); color: #b7791f; border-radius: 4px; font-size: 12px; font-weight: 600; margin-left: 6px; }
.action-link { color: $primary; background: none; border: none; cursor: pointer; font-size: 14px; margin-right: 8px;
  &:hover { text-decoration: underline; }
  &:disabled { color: #bbb; cursor: not-allowed; text-decoration: none; }
}
.pagination { display: flex; justify-content: space-between; align-items: center; padding: 16px; background: #faf9f9; border-top: 1px solid #e4beba; }
.pagination-info { font-size: 12px; color: #414755; }
.pagination-controls { display: flex; align-items: center; gap: 4px; }
.page-btn { width: 32px; height: 32px; border-radius: 4px; display: flex; align-items: center; justify-content: center; background: transparent; border: none; cursor: pointer; font-size: 14px; transition: background-color 0.2s;
  &:hover:not(.active):not(:disabled) { background: #efeded; }
  &.active { background: $primary; color: #fff; font-weight: 700; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}
.page-size-select { margin-left: 16px; background: transparent; border: 1px solid #727786; border-radius: 4px; font-size: 12px; padding: 4px; }
</style>
