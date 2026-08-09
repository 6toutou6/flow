<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">用户详情</h3>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">用户名</label>
            <input class="form-input disabled" :value="data.username" disabled />
          </div>
          <div class="form-group">
            <label class="form-label">员工号</label>
            <input class="form-input disabled" :value="data.empNo" disabled />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">员工姓名</label>
            <input class="form-input disabled" :value="data.realName" disabled />
          </div>
          <div class="form-group">
            <label class="form-label">手机号</label>
            <input class="form-input disabled" :value="data.phone" disabled />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">部门ID</label>
            <input class="form-input disabled" :value="data.deptId" disabled />
          </div>
          <div class="form-group">
            <label class="form-label">部门名称</label>
            <input class="form-input disabled" :value="data.deptName" disabled />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">状态</label>
            <input class="form-input disabled" :value="data.status === 1 ? '正常' : '禁用'" disabled />
          </div>
          <div class="form-group">
            <label class="form-label">创建时间</label>
            <input class="form-input disabled" :value="data.createTime" disabled />
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="handleClose">关闭</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DetailModal',
  props: {
    visible: { type: Boolean, default: false },
    data: { type: Object, default: () => ({}) }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    }
  }
}
</script>

<style lang="scss" scoped>
.modal-overlay { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: rgba(0, 0, 0, 0.5); display: flex; align-items: center; justify-content: center; z-index: 1000; animation: fadeIn 0.2s ease; }
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
.modal-content { background-color: white; border-radius: 3px; width: 90%; max-width: 600px; max-height: 90vh; overflow: hidden; animation: slideIn 0.3s ease; }
@keyframes slideIn { from { opacity: 0; transform: translateY(-20px); } to { opacity: 1; transform: translateY(0); } }
.modal-header { display: flex; justify-content: space-between; align-items: center; padding: 20px 24px; border-bottom: 1px solid #E2E8F0; background-color: var(--color-primary-surface); }
.modal-title { font-size: 20px; font-weight: 700; color: #0F172A; margin: 0; }
.modal-close { background: none; border: none; cursor: pointer; color: var(--color-primary-hover); padding: 4px; border-radius: 4px;
  &:hover { background-color: #E2E8F0; color: #0F172A; }
}
.modal-body { padding: 24px; max-height: 60vh; overflow-y: auto; }
.modal-footer { display: flex; justify-content: flex-end; gap: 12px; padding: 20px 24px; border-top: 1px solid #E2E8F0; background-color: var(--color-primary-surface); }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 16px; }
.form-group { margin-bottom: 16px; }
.form-label { font-size: 14px; font-weight: 600; color: var(--color-primary-hover); margin-bottom: 8px; display: block; }
.form-input { width: 100%; padding: 10px 12px; border: 1px solid #CBD5E1; border-radius: 3px; font-size: 14px; outline: none; box-sizing: border-box;
  &:focus { border-color: var(--color-primary); box-shadow: 0 0 0 3px rgba(var(--color-primary-rgb),0.08); }
  &.disabled { background-color: var(--color-primary-surface); color: #94A3B8; cursor: not-allowed; }
}
.btn-secondary { padding: 10px 24px; background-color: var(--color-primary-surface); color: var(--color-primary-hover); border-radius: 3px; font-weight: 600; border: 1px solid #CBD5E1; cursor: pointer;
  &:hover { background-color: #E2E8F0; }
}
</style>
