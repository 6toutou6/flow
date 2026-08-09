<template>
  <div v-if="visible" class="modal-overlay" @click="handleClose">
    <div class="detail-modal" @click.stop>
      <div class="detail-modal-header">
        <h3 class="detail-modal-title">问题详情</h3>
        <button class="detail-modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="detail-modal-body">
        <section class="detail-section">
          <h4 class="detail-section-title">基本信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label class="detail-label">任务名称</label>
              <div class="detail-value">{{ detail.title }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">责任部门</label>
              <div class="detail-value">{{ detail.department }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">优先级</label>
              <span class="priority-tag" :class="'priority-' + detail.priority">{{ detail.priorityText }}</span>
            </div>
            <div class="detail-item">
              <label class="detail-label">任务状态</label>
              <span class="status-tag" :class="'status-' + detail.status">{{ detail.statusText }}</span>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <h4 class="detail-section-title">项目信息</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label class="detail-label">项目类型</label>
              <div class="detail-value">{{ detail.projectType }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">项目来源</label>
              <div class="detail-value">{{ detail.projectSource }}</div>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <h4 class="detail-section-title">内容描述</h4>
          <div class="detail-form-item">
            <label class="detail-label">项目描述</label>
            <textarea class="detail-textarea" readonly :value="detail.description"></textarea>
          </div>
          <div class="detail-form-item">
            <label class="detail-label">项目依据</label>
            <textarea class="detail-textarea" readonly :value="detail.basis"></textarea>
          </div>
          <div class="detail-form-item">
            <label class="detail-label">整改要求</label>
            <textarea class="detail-textarea" readonly :value="detail.request"></textarea>
          </div>
        </section>

        <section class="detail-section">
          <h4 class="detail-section-title">周期与审批设置</h4>
          <div class="detail-grid">
            <div class="detail-item">
              <label class="detail-label">材料提交频率</label>
              <div class="detail-value">{{ detail.cycle }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">周期信息</label>
              <div class="detail-value">{{ detail.cycleInfo }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">下发时间</label>
              <div class="detail-value">{{ detail.createdTime }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">要求完成时间</label>
              <div class="detail-value">{{ detail.finishTime }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">是否需要审批</label>
              <div class="detail-value">{{ detail.needApproval ? '是' : '否' }}</div>
            </div>
            <div class="detail-item">
              <label class="detail-label">提前催办天数</label>
              <div class="detail-value">{{ detail.urgeDays }} 天</div>
            </div>
            <div class="detail-item col-span-2">
              <label class="detail-label">审批提示</label>
              <div class="detail-value">{{ detail.approvalTip }}</div>
            </div>
            <div class="detail-item col-span-2">
              <label class="detail-label">下次下发时间</label>
              <div class="detail-value">{{ detail.nextPublishTime }}</div>
            </div>
          </div>
        </section>

        <section class="detail-section">
          <h4 class="detail-section-title">附件材料</h4>
          <div v-if="detail.attachments && detail.attachments.length > 0" class="detail-attachments">
            <div v-for="(file, idx) in detail.attachments" :key="idx" class="attachment-item">
              <i class="el-icon-document"></i>
              <span class="attachment-name">{{ file.name }}</span>
              <span class="attachment-size">{{ file.size }}</span>
              <button class="attachment-download"><i class="el-icon-download"></i></button>
            </div>
          </div>
          <div v-else class="detail-empty">暂无附件</div>
        </section>
      </div>
      <div class="detail-modal-footer">
        <button class="btn-modal-close" @click="handleClose">关闭</button>
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
    detail: {
      type: Object,
      default: () => ({})
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
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
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.detail-modal {
  width: 90%;
  max-width: 1000px;
  max-height: 85vh;
  background: #ffffff;
  border-radius: 3px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.detail-modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #F8FAFC;
  border-bottom: 1px solid #CBD5E1;
}

.detail-modal-title {
  font-size: 18px;
  font-weight: bold;
  color: #334155;
  margin: 0;
}

.detail-modal-close {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  padding: 0;

  &:hover {
    color: #334155;
  }
}

.detail-modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.detail-section {
  background: #F8FAFC;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  padding: 16px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-section-title {
  font-size: 16px;
  font-weight: bold;
  color: #1b1c1c;
  margin: 0 0 16px 0;
  padding-bottom: 8px;
  border-bottom: 1px solid #CBD5E1;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 4px;

  &.col-span-2 {
    grid-column: span 2;
  }
}

.detail-label {
  font-size: 13px;
  font-weight: 500;
  color: #334155;
}

.detail-value {
  font-size: 14px;
  color: #1b1c1c;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  min-height: 36px;
  display: flex;
  align-items: center;
}

.detail-form-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }
}

.detail-textarea {
  width: 100%;
  min-height: 80px;
  padding: 12px;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  font-size: 14px;
  color: #1b1c1c;
  background: #ffffff;
  resize: vertical;
  box-sizing: border-box;
  line-height: 1.6;

  &:focus {
    outline: none;
    border-color: #334155;
    box-shadow: 0 0 0 2px rgba(51, 65, 85, 0.1);
  }
}

.detail-attachments {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.attachment-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #ffffff;
  border: 1px solid #CBD5E1;
  border-radius: 4px;

  i {
    color: #334155;
  }

  .attachment-name {
    flex: 1;
    font-size: 13px;
    color: #1b1c1c;
  }

  .attachment-size {
    font-size: 12px;
    color: #757575;
  }

  .attachment-download {
    background: none;
    border: none;
    color: #334155;
    cursor: pointer;
    font-size: 16px;
    padding: 0;

    &:hover {
      opacity: 0.7;
    }
  }
}

.detail-empty {
  font-size: 14px;
  color: #757575;
  padding: 12px;
  background: #ffffff;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  text-align: center;
}

.detail-modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid #CBD5E1;
  background: #F8FAFC;
}

.btn-modal-close {
  padding: 8px 24px;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  font-size: 14px;
  color: #334155;
  background: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #F1F5F9;
    border-color: #334155;
    color: #334155;
  }
}
</style>