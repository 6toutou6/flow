<template>
  <el-dialog
    :title="title"
    :visible.sync="dialogVisible"
    width="520px"
    append-to-body
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="dept-picker">
      <el-input
        v-model="filterText"
        placeholder="输入部门名称快速过滤"
        clearable
        size="small"
        prefix-icon="el-icon-search"
      />
      <div class="dept-tree-box">
        <el-tree
          ref="tree"
          node-key="deptId"
          :data="treeData"
          :props="{ label: 'deptName', isLeaf: 'leaf' }"
          :filter-node-method="filterNode"
          highlight-current
          :current-node-key="currentKey"
          @node-click="handleNodeClick"
        />
        <div v-if="loading" class="dept-loading"><i class="el-icon-loading" /> 加载中...</div>
        <div v-else-if="treeData.length === 0" class="dept-empty">暂无可选部门，请先在系统中维护部门</div>
      </div>
      <div class="dept-selected">
        <span>已选择：</span>
        <b>{{ selected ? selected.deptName : '请点击左侧部门' }}</b>
      </div>
    </div>
    <span slot="footer" class="dept-dialog-footer">
      <el-button size="small" @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" size="small" :disabled="!selected" @click="confirm">确定</el-button>
    </span>
  </el-dialog>
</template>

<script>
import { getDeptOptions } from '@/service/base/DeptAdminService'

/**
 * 部门单选弹窗（仿 gzfb 机构树弹窗：搜索过滤 + 树选择 + 已选择回显）
 * 部门清单以 dept_admin 登记部门为准（getDeptOptions）。
 * 用法：
 *   <DeptPickerDialog :visible="visible" title="选择部门" @confirm="onDept" @close="visible=false" />
 *   onDept(dept) { dept.deptId / dept.deptName ... }
 */
export default {
  name: 'DeptPickerDialog',
  props: {
    visible: { type: Boolean, default: false },
    title: { type: String, default: '选择部门' },
    /** 回显当前已选部门 id（打开时树中高亮） */
    currentDeptId: { type: [String, Number], default: '' }
  },
  data() {
    return {
      loading: false,
      filterText: '',
      treeData: [],
      selected: null
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { if (!val) this.$emit('close') }
    },
    currentKey() {
      return this.currentDeptId === '' || this.currentDeptId === null ? undefined : String(this.currentDeptId)
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.selected = null
        this.filterText = ''
        this.loadDepts()
      }
    },
    filterText(val) {
      if (!this.$refs.tree) return
      this.$refs.tree.filter(val)
    }
  },
  methods: {
    async loadDepts() {
      this.loading = true
      try {
        const res = await getDeptOptions()
        this.treeData = (res.data || []).map(d => ({
          deptId: String(d.deptId),
          deptName: d.deptName
        }))
        // 打开时若已有回显部门，直接置为已选
        if (this.currentKey && this.treeData.some(d => d.deptId === this.currentKey)) {
          this.selected = this.treeData.find(d => d.deptId === this.currentKey) || null
        }
        this.$nextTick(() => {
          if (this.$refs.tree && this.currentKey) {
            this.$refs.tree.setCurrentKey(this.currentKey)
          }
        })
      } catch (e) {
        console.error('获取部门选项失败:', e)
        this.treeData = []
      } finally {
        this.loading = false
      }
    },
    filterNode(value, data) {
      if (!value) return true
      return String(data.deptName).indexOf(value) !== -1 || String(data.deptId).indexOf(value) !== -1
    },
    handleNodeClick(data) {
      this.selected = data
    },
    confirm() {
      if (!this.selected) return
      this.$emit('confirm', { ...this.selected })
      this.dialogVisible = false
    },
    handleClose() {
      this.selected = null
    }
  }
}
</script>

<style lang="scss" scoped>
.dept-picker { display: flex; flex-direction: column; gap: 12px; }
.dept-tree-box { position: relative; max-height: 320px; overflow-y: auto; border: 1px solid #dcdfe6; border-radius: 4px; padding: 8px; }
.dept-loading, .dept-empty { text-align: center; color: #999; font-size: 13px; padding: 24px 0; }
.dept-selected { font-size: 13px; color: #606266; padding: 8px 10px; background: #F5F7FA; border: 1px solid #dcdfe6; border-radius: 4px;
  b { color: var(--color-primary); font-weight: 600; }
}
.dept-dialog-footer { display: flex; justify-content: flex-end; }
</style>
