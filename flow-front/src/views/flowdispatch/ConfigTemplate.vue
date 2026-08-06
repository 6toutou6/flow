<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div>
            <nav class="breadcrumb">
              <span class="link" @click="goBack">任务管理</span>
              <span>/</span>
              <span class="active">下发配置模板</span>
            </nav>
            <h3 class="page-heading">下发配置模板</h3>
          </div>
          <button class="btn-create" @click="openCreate"><i class="el-icon-plus" /> 新增模板</button>
        </div>

        <!-- 提示条 -->
        <section class="tip-bar">
          <i class="el-icon-info" />
          模板保存后可长期复用；新建/编辑任务时选中模板即可一键拉取配置，避免反复配置导致错误。
        </section>

        <!-- 筛选区 -->
        <section class="filter-section">
          <div class="filter-grid">
            <div class="filter-item">
              <label class="filter-label">关键词</label>
              <input v-model="keyword" class="filter-input" placeholder="配置名称 / 备注" @keyup.enter="fetchList">
            </div>
          </div>
          <div class="filter-actions">
            <div class="filter-actions-right">
              <button class="btn-reset" :disabled="loading" @click="resetFilter">重置</button>
              <button class="btn-search" :disabled="loading" @click="fetchList">
                <i v-if="loading" class="el-icon-loading" /><span v-else>查询</span>
              </button>
            </div>
          </div>
        </section>

        <!-- 模板列表 -->
        <div v-if="!loading && list.length === 0" class="empty-state">
          <i class="el-icon-setting" />
          <p>暂无配置模板，点击右上角「新增模板」创建一套常用配置</p>
        </div>
        <div v-else class="tpl-card">
          <div v-if="loading" class="loading-bar"><i class="el-icon-loading" /> 加载中...</div>
          <table v-else class="tpl-table">
            <thead>
              <tr>
                <th>配置名称</th>
                <th>周期</th>
                <th>触发日</th>
                <th class="text-center">截止</th>
                <th class="text-center">催办</th>
                <th>备注</th>
                <th>更新时间</th>
                <th class="text-right">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="t in list" :key="t.id" class="hover-row">
                <td class="font-bold">{{ t.configName }}</td>
                <td>{{ cycleText(t) }}</td>
                <td>{{ cycleDayText(t) }}</td>
                <td class="text-center">触发后 {{ t.deadlineDays || '—' }} 天</td>
                <td class="text-center">{{ t.urgeDays ? '提前 ' + t.urgeDays + ' 天' : '—' }}</td>
                <td class="remark-cell" :title="t.remark">{{ t.remark || '—' }}</td>
                <td>{{ t.updateTime || '—' }}</td>
                <td class="text-right">
                  <button class="action-link" @click="openEdit(t)"><i class="el-icon-edit" /> 编辑</button>
                  <button class="action-link text-error" @click="onDelete(t)"><i class="el-icon-delete" /> 删除</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </main>
  </div>
</template>

<script>
import { getConfigTemplates, deleteConfigTemplate } from '@/api/flowDispatch'

export default {
  name: 'FlowDispatchConfigTemplate',
  data() {
    return {
      loading: false,
      keyword: '',
      list: []
    }
  },
  created() {
    this.fetchList()
  },
  methods: {
    async fetchList() {
      this.loading = true
      try {
        const res = await getConfigTemplates(this.keyword)
        this.list = res.data || []
      } catch (e) {
        console.error(e)
        this.list = []
      } finally {
        this.loading = false
      }
    },
    resetFilter() {
      this.keyword = ''
      this.fetchList()
    },
    cycleText(row) {
      return { 1: '每周', 2: '每月', 3: '每季度', 4: '单次下发' }[row.cycleType] || '—'
    },
    cycleDayText(row) {
      if (row.cycleType === 4) return '—'
      if (row.cycleType === 1) return ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][row.cycleDay] || '—'
      return `每月 ${row.cycleDay} 号`
    },
    openCreate() {
      this.$router.push('/flow-dispatch/config-template/edit')
    },
    openEdit(row) {
      this.$router.push({ path: '/flow-dispatch/config-template/edit', query: { id: row.id }})
    },
    onDelete(t) {
      this.$confirm(`确定删除配置模板「${t.configName}」吗？已拉取到任务上的配置不受影响。`, '删除确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        deleteConfigTemplate(t.id).then(res => {
          this.$message.success(res.message || '删除成功')
          this.fetchList()
        }).catch(e => {
          this.$message.error((e && e.message) || '删除失败')
        })
      }).catch(() => {})
    },
    goBack() {
      this.$router.push('/flow-dispatch/index')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
$border: #e4beba;
.dashboard-container { display: flex; min-height: 100vh; background-color: #F5F7FA; font-family: 'Inter', sans-serif; color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; max-width: 1080px; margin: 0 auto; width: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
  .link { color: $primary; cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}
.page-heading { font-size: 30px; line-height: 38px; font-weight: 600; color: #1b1c1c; }
.btn-create { display: flex; align-items: center; gap: 5px; padding: 10px 22px; background: $primary; color: #fff; border: none; border-radius: 8px; font-weight: 600; font-size: 13px; cursor: pointer; box-shadow: 0 2px 8px rgba(197,48,48,0.25); transition: all .2s;
  &:hover { opacity: 0.9; transform: translateY(-1px); }
}
.tip-bar { display: flex; align-items: center; gap: 8px; background: #FFF5F5; border: 1px solid $border; color: #8a4b46; font-size: 13px; border-radius: 10px; padding: 10px 14px;
  i { color: $primary; }
}
.filter-section { background: #fff; border: 1px solid $border; border-radius: 10px; padding: 16px; box-shadow: 0 1px 4px rgba(0,0,0,0.03); }
.filter-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.filter-item { display: flex; flex-direction: column; gap: 4px; }
.filter-label { font-size: 13px; color: #757575; }
.filter-input { height: 36px; border: 1px solid #dcdfe6; border-radius: 6px; padding: 0 10px; font-size: 13px; outline: none; background: #fff; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(197,48,48,0.12); }
}
.filter-actions { display: flex; justify-content: flex-end; align-items: center; gap: 8px; margin-top: 16px; padding-top: 16px; border-top: 1px solid rgba(228,190,186,0.3); }
.filter-actions-right { display: flex; gap: 8px; }
.btn-reset { padding: 0 16px; height: 36px; border: 1px solid $border; border-radius: 6px; font-size: 13px; color: #5b403d; background: #fff; cursor: pointer;
  &:hover { background: #f6f3f2; }
}
.btn-search { padding: 0 16px; height: 36px; border: none; border-radius: 6px; font-size: 13px; font-weight: bold; color: #fff; background: $primary; cursor: pointer; display: flex; align-items: center; gap: 4px;
  &:hover { opacity: 0.9; }
}
.empty-state { text-align: center; padding: 60px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 10px;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
.loading-bar { display: flex; align-items: center; gap: 6px; justify-content: center; padding: 16px; color: $primary; font-size: 13px; }
.tpl-card { background: #fff; border: 1px solid $border; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,0.03); }
.tpl-table { width: 100%; text-align: left; border-collapse: collapse;
  th { padding: 10px 12px; font-weight: 700; color: #414755; background: #FAFAFA; border-bottom: 1px solid $border; font-size: 13px; }
  td { padding: 10px 12px; border-bottom: 1px solid $border; font-size: 13px; }
  tbody tr:last-child td { border-bottom: none; }
  .hover-row:hover { background: #FFF5F5; }
}
.font-bold { font-weight: 700; }
.text-center { text-align: center; }
.text-right { text-align: right; }
.remark-cell { max-width: 220px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.action-link { color: $primary; background: none; border: none; cursor: pointer; font-size: 13px; margin-left: 8px;
  &:hover { text-decoration: underline; }
}
.text-error { color: #ba1a1a; }
</style>
