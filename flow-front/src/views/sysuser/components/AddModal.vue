<template>
  <BaseModal
    :visible="visible"
    :title="isEdit ? '编辑用户' : '新增用户'"
    @close="handleClose"
  >
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">用户名 <span class="required">*</span></label>
        <input class="form-input" v-model="localFormData.username" placeholder="请输入登录账号" />
      </div>
      <div class="form-group">
        <label class="form-label">员工号 <span class="required">*</span></label>
        <input class="form-input" v-model="localFormData.empNo" placeholder="请输入员工号" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">员工姓名 <span class="required">*</span></label>
        <input class="form-input" v-model="localFormData.realName" placeholder="请输入员工姓名" />
      </div>
      <div class="form-group">
        <label class="form-label">手机号</label>
        <input class="form-input" v-model="localFormData.phone" placeholder="请输入手机号" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">部门ID</label>
        <input class="form-input" v-model="localFormData.deptId" type="number" placeholder="请输入部门ID" />
      </div>
      <div class="form-group">
        <label class="form-label">部门名称</label>
        <input class="form-input" v-model="localFormData.deptName" placeholder="请输入部门名称" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">
          密码
          <span v-if="!isEdit" class="required">*</span>
          <span v-else class="form-tip">（留空则不修改）</span>
        </label>
        <input class="form-input" v-model="localFormData.password" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密码'" />
      </div>
      <div class="form-group">
        <label class="form-label">状态 <span class="required">*</span></label>
        <select class="form-select" v-model="localFormData.status">
          <option :value="1">正常</option>
          <option :value="0">禁用</option>
        </select>
      </div>
    </div>

    <template #footer>
      <button class="btn btn-secondary" @click="handleClose">取消</button>
      <button class="btn btn-primary" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建用户' }}</button>
    </template>
  </BaseModal>
</template>

<script>
import BaseModal from '@/components/BaseModal'

export default {
  name: 'AddModal',
  components: { BaseModal },
  props: {
    visible:  { type: Boolean, default: false },
    isEdit:   { type: Boolean, default: false },
    formData: { type: Object,  default: () => ({}) }
  },
  data() {
    return { localFormData: {} }
  },
  watch: {
    formData: {
      handler(val) { this.localFormData = { ...val } },
      immediate: true,
      deep: true
    }
  },
  methods: {
    handleClose() { this.$emit('close') },
    handleSubmit() {
      if (!this.localFormData.username || !this.localFormData.empNo || !this.localFormData.realName) {
        this.$emit('error', '请填写用户名、员工号、员工姓名')
        return
      }
      if (!this.isEdit && !this.localFormData.password) {
        this.$emit('error', '新增用户必须填写密码')
        return
      }
      if (this.localFormData.deptId !== null && this.localFormData.deptId !== '' && this.localFormData.deptId !== undefined) {
        this.localFormData.deptId = Number(this.localFormData.deptId)
      } else {
        this.localFormData.deptId = null
      }
      this.$emit('submit', { ...this.localFormData })
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/common.scss";

.form-row          { @include form-row; }
.form-group        { @include form-group; }
.form-label        { @include form-label;
  margin-bottom: $space-2;
  .form-tip       { color: #999; font-weight: 400; font-size: $font-size-xs; }
}
.form-input,
.form-select       { @include form-input; }

.btn {
  @include btn-base;
  &.btn-primary   { @include btn-primary; }
  &.btn-secondary { @include btn-secondary; }
}
</style>
