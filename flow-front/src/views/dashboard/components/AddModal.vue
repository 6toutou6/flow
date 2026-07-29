<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">{{ isEdit ? '编辑问题' : '新增问题' }}</h3>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
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
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="handleClose">取消</button>
        <button class="btn-primary" @click="handleSubmit">{{ isEdit ? '保存修改' : '创建问题' }}</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AddModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    isEdit: {
      type: Boolean,
      default: false
    },
    formData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      localFormData: {}
    }
  },
  watch: {
    formData: {
      handler(val) {
        this.localFormData = { ...val }
      },
      immediate: true,
      deep: true
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
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
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background-color: white;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 90vh;
  overflow: hidden;
  animation: slideIn 0.3s ease;
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #e4beba;
  background-color: #faf9f9;
}

.modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #1b1c1c;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  cursor: pointer;
  color: #414755;
  padding: 4px;
  border-radius: 4px;

  &:hover {
    background-color: #efeded;
    color: #1b1c1c;
  }
}

.modal-body {
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #e4beba;
  background-color: #faf9f9;
}

.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-label {
  font-size: 14px;
  font-weight: 600;
  color: #414755;
  margin-bottom: 8px;
  display: block;

  .required {
    color: #ba1a1a;
  }
}

.form-input,
.form-select {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e4beba;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;

  &:focus {
    border-color: #a20513;
    box-shadow: 0 0 0 2px rgba(162, 5, 19, 0.15);
  }

  &.disabled {
    background-color: #faf9f9;
    color: #888;
    cursor: not-allowed;
  }
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e4beba;
  border-radius: 8px;
  font-size: 14px;
  min-height: 100px;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;

  &:focus {
    border-color: #a20513;
    box-shadow: 0 0 0 2px rgba(162, 5, 19, 0.15);
  }
}

.btn-primary {
  padding: 10px 24px;
  background-color: #a20513;
  color: white;
  border-radius: 8px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    opacity: 0.9;
  }
}

.btn-secondary {
  padding: 10px 24px;
  background-color: #faf9f9;
  color: #414755;
  border-radius: 8px;
  font-weight: 600;
  border: 1px solid #e4beba;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: #efeded;
  }
}
</style>
