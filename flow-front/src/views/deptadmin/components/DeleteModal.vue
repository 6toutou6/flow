<template>
  <div v-if="visible" class="modal-mask" @click.self="handleClose">
    <div class="modal-panel">
      <div class="modal-header">
        <h3>删除确认</h3>
        <button class="modal-close" @click="handleClose"><i class="el-icon-close" /></button>
      </div>
      <div class="modal-body">
        <div class="warn-icon"><i class="el-icon-warning-outline" /></div>
        <p class="warn-text">
          确定删除部门管理员
          <b>{{ data.adminName || '未知' }}（{{ data.adminYstId }}）</b>
          —— <b>{{ data.deptName || data.deptId }}</b> 吗？
        </p>
        <p class="warn-sub">删除后该用户将不再拥有本部门的创建（模板/任务）权限。</p>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="handleClose">取消</button>
        <button class="btn-danger" @click="handleConfirm">删除</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeptAdminDeleteModal',
  props: {
    visible: { type: Boolean, default: false },
    data: { type: Object, default: () => ({}) }
  },
  methods: {
    handleConfirm() {
      this.$emit('confirm', {
        adminYstId: this.data.adminYstId,
        deptId: this.data.deptId
      })
    },
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
.modal-mask {
  position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(15, 23, 42, 0.45);
  display: flex; align-items: center; justify-content: center; z-index: 2000;
}
.modal-panel {
  width: 440px; background: #fff; border-radius: 3px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #CBD5E1;
  h3 { margin: 0; font-size: 16px; color: #1b1c1c; }
  .modal-close { background: none; border: none; font-size: 16px; color: #727786; cursor: pointer; }
}
.modal-body { padding: 24px 20px; text-align: center; }
.warn-icon { font-size: 44px; color: #B45309; margin-bottom: 12px; }
.warn-text { font-size: 14px; color: #1b1c1c; line-height: 1.7; margin: 0 0 6px; }
.warn-sub { font-size: 12px; color: #727786; margin: 0; }
.modal-footer {
  display: flex; justify-content: flex-end; gap: 8px; padding: 14px 20px; border-top: 1px solid #CBD5E1;
}
.btn-cancel {
  height: 36px; padding: 0 18px; border: 1px solid #CBD5E1; border-radius: 4px; background: #fff;
  font-size: 13px; color: #414755; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
}
.btn-danger {
  height: 36px; padding: 0 18px; border: none; border-radius: 4px; background: #DC2626;
  color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
  &:hover { opacity: 0.9; }
}
</style>
