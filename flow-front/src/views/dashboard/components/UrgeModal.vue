<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content modal-sm">
      <div class="modal-header">
        <h3 class="modal-title">催办确认</h3>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <div class="urge-icon">
          <i class="el-icon-bell"></i>
        </div>
        <p class="urge-message">确定要对问题 <span class="font-bold">{{ data.title }}</span> 进行催办吗？</p>
        <p class="urge-submessage">系统将向 <span class="dept-name">{{ data.department }}</span> 发送催办通知。</p>
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="handleClose">取消</button>
        <button class="btn-primary" @click="handleConfirm">确认催办</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UrgeModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    data: {
      type: Object,
      default: () => ({})
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
    handleConfirm() {
      this.$emit('confirm', this.data)
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
  border-radius: 3px;
  width: 90%;
  max-width: 400px;
  max-height: 90vh;
  overflow: hidden;
  animation: slideIn 0.3s ease;

  &.modal-sm {
    max-width: 400px;
  }
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
  border-bottom: 1px solid #CBD5E1;
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
  padding: 32px 24px;
  text-align: center;
}

.urge-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background-color: rgba(0, 87, 194, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  .el-icon-bell {
    font-size: 36px;
    color: var(--color-primary);
  }
}

.urge-message {
  font-size: 16px;
  color: #1b1c1c;
  margin-bottom: 8px;
}

.urge-submessage {
  font-size: 14px;
  color: #727786;
}

.font-bold {
  font-weight: 700;
}

.dept-name {
  color: var(--color-primary);
  font-weight: 600;
}

.modal-footer {
  display: flex;
  justify-content: center;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #CBD5E1;
  background-color: #faf9f9;
}

.btn-secondary {
  padding: 10px 24px;
  background-color: #faf9f9;
  color: #414755;
  border-radius: 3px;
  font-weight: 600;
  border: 1px solid #CBD5E1;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: #efeded;
  }
}

.btn-primary {
  padding: 10px 24px;
  background-color: var(--color-primary);
  color: white;
  border-radius: 3px;
  font-weight: 700;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    opacity: 0.9;
  }
}
</style>
