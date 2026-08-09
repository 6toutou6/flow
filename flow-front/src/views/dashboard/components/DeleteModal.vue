<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content modal-sm">
      <div class="modal-header">
        <h3 class="modal-title">确认删除</h3>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <div class="warning-icon">
          <i class="el-icon-delete"></i>
        </div>
        <p class="delete-message">确定要删除问题 <span class="font-bold">{{ data.title }}</span> 吗？</p>
        <p class="delete-submessage">此操作无法撤销，请谨慎操作。</p>
      </div>
      <div class="modal-footer">
        <button class="btn-secondary" @click="handleClose">取消</button>
        <button class="btn-danger" @click="handleConfirm">确认删除</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DeleteModal',
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
      this.$emit('confirm', this.data.id)
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

.warning-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  background-color: rgba(220, 38, 38, 0.08);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;

  .el-icon-delete {
    font-size: 36px;
    color: #DC2626;
  }
}

.delete-message {
  font-size: 16px;
  color: #1b1c1c;
  margin-bottom: 8px;
}

.delete-submessage {
  font-size: 14px;
  color: #727786;
}

.font-bold {
  font-weight: 700;
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

.btn-danger {
  padding: 10px 24px;
  background-color: #DC2626;
  color: white;
  border-radius: 3px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background-color: #93000a;
  }
}
</style>
