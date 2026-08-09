<template>
  <BaseModal
    :visible="visible"
    :title="isEdit ? '编辑问题' : '新增问题'"
    @close="handleClose"
  >
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">问题标题 <span class="required">*</span></label>
        <input class="form-input" v-model="localFormData.title" placeholder="请输入问题标题" />
      </div>
      <div class="form-group">
        <label class="form-label">问题编号</label>
        <input class="form-input disabled" :value="localFormData.issueNo" disabled />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">负责部门 <span class="required">*</span></label>
        <select class="form-select" v-model="localFormData.department">
          <option value="">请选择部门</option>
          <option value="生产技术部">生产技术部</option>
          <option value="安全环保部">安全环保部</option>
          <option value="人力资源部">人力资源部</option>
          <option value="财务部">财务部</option>
          <option value="综合管理部">综合管理部</option>
          <option value="信息技术部">信息技术部</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">整改时限 <span class="required">*</span></label>
        <input class="form-input" v-model="localFormData.deadline" type="date" />
      </div>
    </div>
    <div class="form-row">
      <div class="form-group">
        <label class="form-label">优先级 <span class="required">*</span></label>
        <select class="form-select" v-model="localFormData.priority">
          <option value="high">紧急</option>
          <option value="medium">中</option>
          <option value="low">低</option>
        </select>
      </div>
      <div class="form-group">
        <label class="form-label">状态 <span class="required">*</span></label>
        <select class="form-select" v-model="localFormData.status">
          <option value="pending">待处理</option>
          <option value="rectifying">整改中</option>
          <option value="completed">已完成</option>
        </select>
      </div>
    </div>
    <div class="form-group">
      <label class="form-label">问题描述</label>
      <textarea class="form-textarea" v-model="localFormData.description" placeholder="请输入问题描述"></textarea>
    </div>

    <template #footer>
      <button class="btn btn-secondary" @click="handleClose">取消</button>
      <button class="btn btn-primary" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建问题' }}</button>
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
      if (!this.localFormData.title || !this.localFormData.department || !this.localFormData.deadline) {
        this.$emit('error', '请填写必填项')
        return
      }
      this.$emit('submit', { ...this.localFormData })
    }
  }
}
</script>

<style lang="scss" scoped>
@import "~@/styles/common.scss";

.form-row   { @include form-row; }
.form-group { @include form-group; }
.form-label { @include form-label;
  margin-bottom: $space-2;
}

.form-input,
.form-select {
  @include form-input;
}

.form-textarea { @include form-textarea; }

.btn {
  @include btn-base;
  &.btn-primary   { @include btn-primary; }
  &.btn-secondary { @include btn-secondary; }
}
</style>
