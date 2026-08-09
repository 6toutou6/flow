<template>
  <el-dialog :title="title" :visible.sync="dialogVisible" width="760px" :close-on-click-modal="false" append-to-body @close="handleClose">
    <div class="picker-filter">
      <input v-model="filter.realName" class="filter-input" placeholder="姓名" @keyup.enter="loadUsers">
      <input v-model="filter.deptName" class="filter-input" placeholder="部门" @keyup.enter="loadUsers">
      <button class="btn-search" @click="loadUsers">查询</button>
    </div>
    <el-table
      v-loading="loading"
      ref="userTable"
      :data="userList"
      height="360"
      row-key="id"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="45" :selectable="isSelectable" />
      <el-table-column prop="empNo" label="员工号" width="110" />
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="deptName" label="部门" />
      <el-table-column prop="phone" label="手机号" width="140" />
    </el-table>
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
import { getUserList } from '@/api/sysuser'

export default {
  name: 'UserPicker',
  props: {
    visible: { type: Boolean, default: false },
    /** 弹窗标题（可选） */
    title: { type: String, default: '选择处理人（可多选）' },
    /** 已选处理人ID列表，这些行禁用勾选避免重复 */
    excludeIds: { type: Array, default: () => [] }
  },
  data() {
    return {
      loading: false,
      userList: [],
      filter: { realName: '', deptName: '' },
      selection: []
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
        this.filter = { realName: '', deptName: '' }
        this.$nextTick(() => {
          this.loadUsers()
        })
      }
    }
  },
  methods: {
    isSelectable(row) {
      // 已在已选列表中的禁用勾选，避免重复添加
      return !this.excludeIds.includes(row.id)
    },
    async loadUsers() {
      this.loading = true
      try {
        const res = await getUserList({ page: 1, limit: 200, status: 1, ...this.filter })
        this.userList = res.data.records || []
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
      this.$emit('confirm', this.selection.map(u => ({ ...u })))
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
.picker-filter { display: flex; gap: 8px; margin-bottom: 12px; }
.filter-input { height: 32px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; flex: 1;
  &:focus { border-color: $primary; }
}
.btn-search { padding: 0 16px; height: 32px; border: none; border-radius: 4px; background: $primary; color: #fff; cursor: pointer; font-size: 13px; }
.dialog-footer { display: flex; justify-content: space-between; align-items: center; }
.picker-count { font-size: 13px; color: $primary; font-weight: 600; }
</style>
