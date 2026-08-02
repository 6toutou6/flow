<template>
  <el-dialog
    :title="isEdit ? '编辑模板' : '新建模板'"
    :visible.sync="dialogVisible"
    width="560px"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <el-form ref="form" :model="form" :rules="rules" label-width="110px" class="tpl-form">
      <el-form-item label="模板名称" prop="templateName">
        <el-input v-model="form.templateName" placeholder="请输入流程模板名称" maxlength="100" show-word-limit />
      </el-form-item>
      <el-form-item label="模板分类" prop="category">
        <el-input v-model="form.category" placeholder="如：信息采集、材料上报" maxlength="50" />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'TemplateFormModal',
  props: {
    visible: { type: Boolean, default: false },
    isEdit: { type: Boolean, default: false },
    formData: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      submitting: false,
      form: this.buildForm(this.formData),
      rules: {
        templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    dialogVisible: {
      get() { return this.visible },
      set(val) { this.$emit('update:visible', val) }
    }
  },
  watch: {
    visible(val) {
      if (val) this.form = this.buildForm(this.formData)
    }
  },
  methods: {
    buildForm(data) {
      return {
        id: data.id || null,
        templateName: data.templateName || '',
        category: data.category || '',
        status: data.status == null ? 1 : data.status
      }
    },
    handleClose() {
      this.dialogVisible = false
      this.$refs.form && this.$refs.form.clearValidate()
    },
    handleSubmit() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.submitting = true
        this.$emit('submit', { ...this.form }, () => {
          this.submitting = false
        }, () => {
          this.submitting = false
        })
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.tpl-form {
  ::v-deep .el-form-item__label { font-weight: 500; color: #414755; }
}
.rule-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  &:last-child { margin-bottom: 0; }
  .rule-text { font-size: 14px; color: #606266; }
}
.dialog-footer { text-align: right; }
</style>
