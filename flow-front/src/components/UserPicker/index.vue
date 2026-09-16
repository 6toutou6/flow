<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    :width="perUserTaskName ? '1000px' : '760px'"
    :top="perUserTaskName ? '6vh' : '15vh'"
    :close-on-click-modal="false"
    append-to-body
    @close="handleClose"
  >
    <div class="picker-filter">
      <input v-model="filter.userName" class="filter-input" placeholder="姓名/用户号" @keyup.enter="loadUsers">
      <input v-model="filter.deptName" class="filter-input" placeholder="部门" @keyup.enter="loadUsers">
      <button class="btn-search" @click="loadUsers">查询</button>
    </div>
    <!-- 可选：统一的任务名称前缀（多人时自动拼姓名） -->
    <div v-if="showTaskName" class="task-name-row">
      <span class="tn-label">任务名称</span>
      <input v-model="taskName" class="filter-input" placeholder="留空则用系统默认名称（多人时自动拼姓名）">
    </div>

    <!-- 主体：逐人命名模式用左右布局，选人时右边就能看到并直接填写各自的任务名，不会被忽略 -->
    <div class="picker-main" :class="{ 'is-split': perUserTaskName }">
      <div class="pm-left">
        <el-table
          ref="userTable"
          v-loading="loading"
          :data="userList"
          height="380"
          row-key="yyytId"
          @selection-change="handleSelectionChange"
        >
          <el-table-column type="selection" width="45" :selectable="isSelectable" />
          <el-table-column prop="yyytId" label="用户号" :width="perUserTaskName ? 100 : 120" />
          <el-table-column prop="userName" label="姓名" :width="perUserTaskName ? 80 : 110" />
          <el-table-column prop="deptName" label="部门" />
          <el-table-column v-if="!perUserTaskName" prop="phone" label="手机号" width="140" />
        </el-table>
      </div>

      <!-- 逐人设置任务名称：勾选几人就列几行，各自命名 -->
      <div v-if="perUserTaskName" class="pm-right">
        <div class="pnl-head">
          <i class="el-icon-edit-outline" /> 逐人任务名称
          <span class="pnl-sub">留空即用默认名</span>
        </div>
        <div v-if="selection.length === 0" class="pnl-empty">
          <i class="el-icon-mouse" />
          <p>勾选左侧人员后<br>在这里为各自填写任务名称</p>
        </div>
        <div v-else class="pnl-body">
          <div v-for="u in selection" :key="u.yyytId" class="pnl-row">
            <span class="pnl-user" :title="u.userName + ' · ' + (u.deptName || '无部门')">{{ u.userName }}</span>
            <span class="pnl-uid">{{ u.yyytId }}</span>
            <input
              v-model="taskNames[u.yyytId]"
              class="filter-input pnl-input"
              :placeholder="defaultNameOf(u)"
            >
          </div>
        </div>
      </div>
    </div>

    <div slot="footer" class="dialog-footer">
      <span class="picker-count">本次已选：{{ selection.length }} 人</span>
      <div>
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :disabled="selection.length === 0" @click="handleConfirm">确定（{{ selection.length }}）</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script>
import { searchUsers } from '@/service/base/UserService'

export default {
  name: 'UserPicker',
  props: {
    visible: { type: Boolean, default: false },
    /** 弹窗标题（可选） */
    title: { type: String, default: '选择处理人（可多选）' },
    /** 已选处理人用户号列表，这些行禁用勾选避免重复 */
    excludeIds: { type: Array, default: () => [] },
    /** 是否显示「任务名称」输入框（仅新增期次人员等场景需要） */
    showTaskName: { type: Boolean, default: false },
    /** 是否逐人设置任务名称（勾选的人各列一行、各自填写，优先于 showTaskName 的统一前缀） */
    perUserTaskName: { type: Boolean, default: false }
  },
  data() {
    return {
      loading: false,
      userList: [],
      filter: { userName: '', deptName: '' },
      selection: [],
      taskName: '',
      // 逐人任务名：{ 用户号: 任务名 }
      taskNames: {}
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.selection = []
        this.filter = { userName: '', deptName: '' }
        this.taskName = ''
        this.taskNames = {}
        this.$nextTick(() => {
          this.loadUsers()
        })
      }
    }
  },
  methods: {
    isSelectable(row) {
      // 已在已选列表中的禁用勾选，避免重复添加
      return !this.excludeIds.includes(row.yyytId)
    },
    /** 默认任务名（与后端 memberTaskNameOf 的规则一致，仅作输入框占位提示） */
    defaultNameOf(u) {
      return `下发给${u.userName}的任务`
    },
    async loadUsers() {
      this.loading = true
      try {
        const params = { userName: this.filter.userName || null }
        const res = await searchUsers(params)
        let list = res.data || []
        // 部门过滤（searchUsers 不支持部门条件，前端过滤）
        if (this.filter.deptName) {
          list = list.filter(u => (u.deptName || '').indexOf(this.filter.deptName) >= 0)
        }
        this.userList = list
      } catch (e) {
        console.error(e)
      } finally {
        this.loading = false
      }
    },
    handleSelectionChange(rows) {
      this.selection = rows || []
    },
    handleConfirm() {
      if (this.selection.length === 0) return
      // 逐人模式：把各自填的任务名挂到用户对象上；否则沿用统一的 taskName（第二参数）
      const users = this.selection.map(u => {
        const item = { ...u }
        if (this.perUserTaskName) item.taskName = (this.taskNames[u.yyytId] || '').trim()
        return item
      })
      this.$emit('confirm', users, this.perUserTaskName ? '' : (this.taskName || '').trim())
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
// 内容区固定高度：切换「已选/未选」「逐人/统一」时弹窗不跳动（小屏用 max-height 兜底内部滚动）
::v-deep .el-dialog__body { height: 424px; max-height: 70vh; overflow-y: auto; }
.picker-filter { display: flex; gap: 8px; margin-bottom: 12px; }
.task-name-row { display: flex; align-items: center; gap: 8px; margin-bottom: 12px;
  .tn-label { font-size: 13px; color: #606266; flex: 0 0 auto; }
}
.filter-input { height: 32px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; flex: 1;
  &:focus { border-color: $primary; }
}
.btn-search { padding: 0 16px; height: 32px; border: none; border-radius: 4px; background: $primary; color: #fff; cursor: pointer; font-size: 13px; flex: 0 0 auto; }
// 主体：逐人命名模式左右分栏（左选人 / 右填任务名），其余场景单列
.picker-main { display: flex; gap: 12px; align-items: flex-start;
  .pm-left { flex: 1; min-width: 0; }
  .pm-right { flex: 1; display: flex; flex-direction: column; }
  // 逐人命名模式：左侧定宽（够放用户号/姓名/部门），右侧把剩余宽度都给任务名输入框
  &.is-split .pm-left { flex: 0 0 456px; }
}
.pnl-head { display: flex; align-items: center; gap: 4px; padding: 8px 12px; background: #f5f7fa; border: 1px solid #e4e7ed; border-bottom: none; border-radius: 4px 4px 0 0; font-size: 13px; font-weight: 600; color: #414755;
  i { color: $primary; }
  .pnl-sub { margin-left: auto; font-size: 12px; font-weight: 400; color: #909399; }
}
.pnl-empty { height: 347px; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 6px; border: 1px solid #e4e7ed; border-radius: 0 0 4px 4px; background: #fafbfc; color: #b6bfcc;
  i { font-size: 26px; }
  p { margin: 0; font-size: 12px; line-height: 1.7; text-align: center; }
}
.pnl-body { height: 347px; overflow-y: auto; border: 1px solid #e4e7ed; border-radius: 0 0 4px 4px; }
// 每人一行：姓名 + 用户号（都定宽）+ 任务名输入框（占满剩余），部门只在 tooltip 里给
.pnl-row { display: flex; align-items: center; gap: 8px; padding: 5px 10px; border-bottom: 1px solid #f0f2f5;
  &:last-child { border-bottom: none; }
  &:hover { background: #f7fafd; }
  .pnl-user { flex: 0 0 52px; font-size: 12.5px; font-weight: 600; color: #414755; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .pnl-uid { flex: 0 0 78px; font-size: 11.5px; color: #9aa5b4; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
  .pnl-input { flex: 1; min-width: 0; height: 28px; font-size: 12px; }
}
.dialog-footer { display: flex; justify-content: space-between; align-items: center; }
.picker-count { font-size: 13px; color: $primary; font-weight: 600; }
</style>
