<template>
  <div v-if="visible" class="modal-mask" @click.self="handleClose">
    <div class="modal-panel">
      <div class="modal-header">
        <h3>{{ isEdit ? '编辑部门管理员' : '新增部门管理员' }}</h3>
        <button class="modal-close" @click="handleClose"><i class="el-icon-close" /></button>
      </div>
      <div class="modal-body">
        <!-- 管理员（UserPicker 单选） -->
        <div class="form-item">
          <label class="form-label">管理员 <span class="required">*</span></label>
          <div class="user-row">
            <input
              v-model="displayUser"
              class="form-input readonly"
              placeholder="点击右侧按钮选择管理员"
              readonly
            >
            <button
              class="btn-pick"
              :disabled="isEdit"
              :title="isEdit ? '编辑时管理员不可变更，可删除后重新新增' : '选择管理员'"
              @click="openPicker"
            >
              <i class="el-icon-search" /> 选择
            </button>
          </div>
        </div>

        <!-- 部门（按 dept_admin 为准，弹窗选择） -->
        <div class="form-item">
          <label class="form-label">部门 <span class="required">*</span></label>
          <div class="user-row">
            <input
              v-model="displayDept"
              class="form-input readonly"
              placeholder="点击右侧按钮选择部门"
              readonly
            >
            <button
              class="btn-pick"
              :disabled="isEdit"
              :title="isEdit ? '编辑时部门不可变更，可删除后重新新增' : '选择部门'"
              @click="openDeptPicker"
            >
              <i class="el-icon-search" /> 选择
            </button>
          </div>
        </div>

        <!-- 部门名称（随部门选择自动带出） -->
        <div class="form-item">
          <label class="form-label">部门名称</label>
          <input v-model="localForm.deptName" class="form-input readonly" readonly>
        </div>

        <!-- 部门邮箱 -->
        <div class="form-item">
          <label class="form-label">部门邮箱</label>
          <input v-model="localForm.deptEmail" class="form-input" placeholder="请输入部门邮箱地址（可选）">
        </div>

        <!-- 邮箱密码 -->
        <div class="form-item">
          <label class="form-label">邮箱密码</label>
          <input v-model="localForm.deptEmailPwd" class="form-input" type="password" placeholder="请输入部门邮箱密码（可选）">
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-cancel" @click="handleClose">取消</button>
        <button class="btn-confirm" :disabled="!canSubmit" @click="handleSubmit">
          {{ isEdit ? '保存' : '确定' }}
        </button>
      </div>
    </div>

    <!-- 管理员选择弹窗（单选） -->
    <UserPicker
      :visible="pickerVisible"
      title="选择管理员（单选）"
      @confirm="confirmPick"
      @close="pickerVisible = false"
    />
    <!-- 部门选择弹窗（单选，仿机构树） -->
    <DeptPickerDialog
      :visible="deptPickerVisible"
      title="选择部门"
      :current-dept-id="localForm.deptId"
      @confirm="confirmDept"
      @close="deptPickerVisible = false"
    />
  </div>
</template>

<script>
import UserPicker from '@/components/UserPicker'
import DeptPickerDialog from './DeptPickerDialog.vue'

export default {
  name: 'DeptAdminAddEditModal',
  components: { UserPicker, DeptPickerDialog },
  props: {
    visible: { type: Boolean, default: false },
    isEdit: { type: Boolean, default: false },
    /** 编辑回显数据（DeptAdmin 行） */
    formData: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      pickerVisible: false,
      deptPickerVisible: false,
      localForm: {
        adminYstId: '',
        adminName: '',
        deptId: '',
        deptName: '',
        deptEmail: '',
        deptEmailPwd: ''
      }
    }
  },
  computed: {
    displayUser() {
      if (this.localForm.adminYstId) {
        return `${this.localForm.adminName}（${this.localForm.adminYstId}）`
      }
      return ''
    },
    displayDept() {
      return this.localForm.deptName || ''
    },
    canSubmit() {
      return !!(this.localForm.adminYstId && this.localForm.deptId)
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.localForm = {
          adminYstId: this.formData.adminYstId || '',
          adminName: this.formData.adminName || '',
          deptId: this.formData.deptId ? String(this.formData.deptId) : '',
          deptName: this.formData.deptName || '',
          deptEmail: this.formData.deptEmail || '',
          deptEmailPwd: this.formData.deptEmailPwd || ''
        }
      }
    }
  },
  methods: {
    openPicker() {
      if (this.isEdit) return
      this.pickerVisible = true
    },
    confirmPick(selection) {
      this.pickerVisible = false
      if (!selection || !selection.length) return
      const u = selection[0]
      this.localForm.adminYstId = u.yyytId
      this.localForm.adminName = u.userName
    },
    openDeptPicker() {
      if (this.isEdit) return
      this.deptPickerVisible = true
    },
    confirmDept(dept) {
      this.deptPickerVisible = false
      if (!dept || !dept.deptId) return
      this.localForm.deptId = String(dept.deptId)
      this.localForm.deptName = dept.deptName || ''
    },
    handleSubmit() {
      this.$emit('submit', { ...this.localForm })
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
  width: 520px; max-height: 90vh; overflow-y: auto; background: #fff; border-radius: 3px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.18);
}
.modal-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 16px 20px; border-bottom: 1px solid #CBD5E1;
  h3 { margin: 0; font-size: 16px; color: #1b1c1c; }
  .modal-close { background: none; border: none; font-size: 16px; color: #727786; cursor: pointer; }
}
.modal-body { padding: 20px; display: flex; flex-direction: column; gap: 14px; }
.form-item { display: flex; flex-direction: column; gap: 4px; }
.form-label { font-size: 13px; color: #414755; }
.required { color: #DC2626; }
.user-row { display: flex; gap: 8px; }
.form-input, .form-select {
  height: 36px; border: 1px solid #CBD5E1; border-radius: 4px; padding: 0 10px;
  font-size: 13px; outline: none; box-sizing: border-box; width: 100%;
  &:focus { border-color: var(--color-primary); box-shadow: 0 0 0 1px rgba(var(--color-primary-rgb), 0.2); }
}
.form-input.readonly { background: #f6f7f8; color: #606266; }
.btn-pick {
  flex-shrink: 0; height: 36px; padding: 0 14px; border: 1px solid var(--color-primary);
  border-radius: 4px; background: var(--color-primary-light); color: var(--color-primary);
  font-size: 13px; cursor: pointer;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
.modal-footer {
  display: flex; justify-content: flex-end; gap: 8px; padding: 14px 20px; border-top: 1px solid #CBD5E1;
}
.btn-cancel {
  height: 36px; padding: 0 18px; border: 1px solid #CBD5E1; border-radius: 4px; background: #fff;
  font-size: 13px; color: #414755; cursor: pointer;
  &:hover { background: var(--color-primary-light); }
}
.btn-confirm {
  height: 36px; padding: 0 18px; border: none; border-radius: 4px; background: var(--color-primary);
  color: #fff; font-size: 13px; font-weight: 600; cursor: pointer;
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
</style>
