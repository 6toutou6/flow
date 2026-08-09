<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">问题详情</h3>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">问题编号</label>
            <input class="form-input disabled" :value="data.issueNo" disabled />
          </div>
          <div class="form-group">
            <label class="form-label">问题标题</label>
            <input class="form-input disabled" :value="data.title" disabled />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">负责部门</label>
            <input class="form-input disabled" :value="data.department" disabled />
          </div>
          <div class="form-group">
            <label class="form-label">整改时限</label>
            <input class="form-input disabled" :value="data.deadline" :class="{ 'text-error': data.status === 'overdue' }" disabled />
          </div>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">优先级</label>
            <div class="form-value-chip">
              <span :class="'priority-chip priority-' + data.priority">{{ data.priorityText }}</span>
            </div>
          </div>
          <div class="form-group">
            <label class="form-label">状态</label>
            <div class="form-value-chip">
              <span :class="'status-chip status-' + data.status">{{ data.statusText }}</span>
            </div>
          </div>
        </div>
        <div class="form-group">
          <label class="form-label">问题描述</label>
          <textarea class="form-textarea disabled" :value="data.description" disabled></textarea>
        </div>
        <div class="form-row">
          <div class="form-group">
            <label class="form-label">创建时间</label>
            <input class="form-input disabled" :value="formatDateTime(data.createdAt)" disabled />
          </div>
          <div class="form-group">
            <label class="form-label">更新时间</label>
            <input class="form-input disabled" :value="formatDateTime(data.updatedAt)" disabled />
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-primary" @click="handleClose">确定</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DetailModal',
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
    formatDateTime(dateTime) {
      if (!dateTime) return ''
      const date = new Date(dateTime)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
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
  padding: 24px;
  max-height: 60vh;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 20px 24px;
  border-top: 1px solid #CBD5E1;
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
}

.form-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #CBD5E1;
  border-radius: 3px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;

  &.disabled {
    background-color: #faf9f9;
    color: #1b1c1c;
    cursor: not-allowed;
  }

  &.text-error {
    color: #DC2626;
  }
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #CBD5E1;
  border-radius: 3px;
  font-size: 14px;
  min-height: 100px;
  resize: vertical;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;

  &.disabled {
    background-color: #faf9f9;
    color: #1b1c1c;
    cursor: not-allowed;
  }
}

.form-value-chip {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  border: 1px solid #CBD5E1;
  border-radius: 3px;
  background-color: #faf9f9;
}

.status-chip {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  border: 1px solid transparent;
}

.status-pending {
  background-color: rgba(51, 65, 85, 0.1);
  border-color: #334155;
  color: #334155;
}

.status-rectifying {
  background-color: rgba(125, 84, 0, 0.1);
  border-color: #7d5400;
  color: #7d5400;
}

.status-overdue {
  background-color: rgba(220, 38, 38, 0.08);
  border-color: #DC2626;
  color: #DC2626;
}

.status-completed {
  background-color: rgba(21, 128, 61, 0.1);
  border-color: #15803D;
  color: #15803D;
}

.priority-chip {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  display: inline-flex;
  align-items: center;
  border: 1px solid transparent;
}

.priority-high {
  background-color: rgba(220, 38, 38, 0.08);
  border-color: #DC2626;
  color: #DC2626;
}

.priority-medium {
  background-color: rgba(125, 84, 0, 0.1);
  border-color: #7d5400;
  color: #7d5400;
}

.priority-low {
  background-color: rgba(51, 65, 85, 0.1);
  border-color: #334155;
  color: #334155;
}

.btn-primary {
  padding: 10px 24px;
  background-color: #334155;
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
