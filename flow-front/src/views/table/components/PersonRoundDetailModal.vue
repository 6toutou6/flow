<template>
  <div v-if="visible" class="modal-overlay" @click="handleClose">
    <div class="modal-content" @click.stop>
      <div class="modal-header">
        <h3 class="modal-title">整改详情</h3>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <!-- 左侧：整改内容 -->
        <div class="left-panel">
          <!-- 基本信息卡片 -->
          <div class="info-card">
            <div class="info-header">
              <div class="person-info">
                <span class="person-avatar-lg">{{ personInfo.name.charAt(0) }}</span>
                <div>
                  <h4>{{ personInfo.name }}</h4>
                  <p>{{ personInfo.department }} · {{ personInfo.role }}</p>
                </div>
              </div>
              <div class="round-info">
                <span class="round-badge-lg">第{{ roundInfo.roundNo }}期</span>
                <span class="submit-tag" :class="'submit-' + roundInfo.tagClass">{{ roundInfo.statusText }}</span>
              </div>
            </div>
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">下发时间</span>
                <span class="info-value">{{ roundInfo.publishTime }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">截止时间</span>
                <span class="info-value">{{ roundInfo.deadline }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">提交时间</span>
                <span class="info-value">{{ roundInfo.submitTime }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">期次状态</span>
                <span class="round-status-tag" :class="'round-status-' + roundInfo.roundStatus">{{ roundInfo.roundStatusText }}</span>
              </div>
            </div>
          </div>

          <!-- 整改说明 -->
          <div class="section-card">
            <h4 class="section-title">整改说明</h4>
            <p class="remark-text">{{ roundInfo.remark }}</p>
          </div>

          <!-- 附件列表 -->
          <div v-if="roundInfo.attachments && roundInfo.attachments.length > 0" class="section-card">
            <h4 class="section-title">附件材料</h4>
            <div class="attachment-grid">
              <a v-for="(file, idx) in roundInfo.attachments" :key="idx" class="attachment-card" :href="file.url" target="_blank">
                <i class="el-icon-document"></i>
                <span class="attachment-title">{{ file.name }}</span>
                <span class="attachment-meta">{{ file.size }}</span>
              </a>
            </div>
          </div>
        </div>

        <!-- 右侧：审批流程 -->
        <div class="right-panel">
          <div class="flow-card">
            <h4 class="flow-title">审批流程</h4>
            <div v-if="roundInfo.approvalFlow && roundInfo.approvalFlow.length > 0" class="approval-flow">
              <div v-for="(step, idx) in roundInfo.approvalFlow" :key="idx" class="approval-step">
                <div class="step-line">
                  <span class="step-icon" :class="'step-' + step.status">
                    <i v-if="step.status === 'completed'" class="el-icon-circle-check"></i>
                    <i v-else-if="step.status === 'pending'" class="el-icon-circle"></i>
                  </span>
                  <span v-if="idx < roundInfo.approvalFlow.length - 1" class="step-connector"></span>
                </div>
                <div class="step-content">
                  <span class="step-action">{{ step.action }}</span>
                  <span class="step-actor">{{ step.actor }}</span>
                  <span class="step-time">{{ step.time }}</span>
                  <span class="step-comment">{{ step.comment }}</span>
                </div>
              </div>
            </div>
            <div v-else class="flow-empty">
              <i class="el-icon-warning-outline"></i>
              <span>暂无审批流程记录</span>
            </div>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-modal-close" @click="handleClose">关闭</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'PersonRoundDetailModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    personInfo: {
      type: Object,
      default: () => ({})
    },
    roundInfo: {
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
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 4px;
  width: 92%;
  max-width: 960px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    opacity: 0;
    transform: translateY(20px);
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
  padding: 16px 20px;
  border-bottom: 1px solid rgba(var(--color-primary-rgb), 0.1);
}

.modal-title {
  font-size: 18px;
  font-weight: bold;
  color: var(--color-primary);
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  font-size: 20px;
  color: #999;
  cursor: pointer;
  transition: color 0.2s;

  &:hover {
    color: var(--color-primary);
  }
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: flex;
  gap: 16px;
}

.left-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.right-panel {
  width: 360px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.modal-footer {
  padding: 14px 20px;
  border-top: 1px solid rgba(var(--color-primary-rgb), 0.1);
  display: flex;
  justify-content: flex-end;
}

.btn-modal-close {
  padding: 8px 20px;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  font-size: 14px;
  color: var(--color-primary);
  background: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-primary-light);
    border-color: var(--color-primary);
    color: var(--color-primary);
  }
}

/* 基本信息卡片 */
.info-card {
  background: var(--color-primary-surface);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
  border-radius: 3px;
  padding: 16px;
}

.info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 14px;
  border-bottom: 1px solid rgba(var(--color-primary-rgb), 0.08);
  margin-bottom: 14px;
}

.person-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.person-avatar-lg {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--color-primary), #c71530);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
}

.person-info h4 {
  font-size: 16px;
  font-weight: bold;
  color: #1b1c1c;
  margin: 0 0 4px 0;
}

.person-info p {
  font-size: 13px;
  color: #757575;
  margin: 0;
}

.round-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.round-badge-lg {
  padding: 6px 12px;
  background: rgba(var(--color-primary-rgb), 0.1);
  color: var(--color-primary);
  border-radius: 4px;
  font-size: 13px;
  font-weight: bold;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #757575;
}

.info-value {
  font-size: 13px;
  color: #1b1c1c;
}

/* 区块卡片 */
.section-card {
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
  border-radius: 3px;
  padding: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: bold;
  color: var(--color-primary);
  margin: 0 0 12px 0;
  padding-left: 8px;
  border-left: 3px solid var(--color-primary);
}

.remark-text {
  font-size: 13px;
  color: #1b1c1c;
  line-height: 1.6;
  margin: 0;
}

/* 附件网格 */
.attachment-grid {
  display: grid;
  grid-template-columns: repeat(1, 1fr);
  gap: 8px;
}

.attachment-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px;
  background: var(--color-primary-surface);
  border-radius: 2px;
  text-decoration: none;
  color: var(--color-primary);
  transition: all 0.2s;

  &:hover {
    background: rgba(var(--color-primary-rgb), 0.05);
  }

  i {
    font-size: 20px;
  }
}

.attachment-title {
  flex: 1;
  font-size: 13px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-meta {
  font-size: 11px;
  color: #757575;
}

/* 流程卡片 */
.flow-card {
  background: var(--color-primary-surface);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
  border-radius: 3px;
  padding: 16px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.flow-title {
  font-size: 14px;
  font-weight: bold;
  color: var(--color-primary);
  margin: 0 0 16px 0;
  padding-left: 8px;
  border-left: 3px solid var(--color-primary);
}

.flow-empty {
  padding: 32px 12px;
  text-align: center;
  font-size: 13px;
  color: #757575;

  i {
    font-size: 28px;
    color: #CBD5E1;
    display: block;
    margin-bottom: 8px;
  }
}

/* 审批流程 */
.approval-flow {
  display: flex;
  flex-direction: column;
}

.approval-step {
  display: flex;
  gap: 12px;
}

.step-line {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 24px;
  flex-shrink: 0;
}

.step-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;

  &.step-completed {
    background: var(--color-primary);
    color: white;
  }

  &.step-pending {
    background: var(--color-primary-light);
    color: #94A3B8;
    border: 1px solid #CBD5E1;
  }
}

.step-connector {
  flex: 1;
  width: 2px;
  background: #CBD5E1;
  margin: 4px 0;
}

.step-content {
  flex: 1;
  padding-bottom: 16px;
}

.step-action {
  display: block;
  font-size: 13px;
  font-weight: bold;
  color: #1b1c1c;
  margin-bottom: 4px;
}

.step-actor {
  display: inline-block;
  font-size: 12px;
  color: #757575;
  margin-right: 12px;
}

.step-time {
  display: inline-block;
  font-size: 12px;
  color: #757575;
}

.step-comment {
  display: block;
  font-size: 12px;
  color: #757575;
  margin-top: 4px;
  padding-left: 12px;
  border-left: 2px solid #CBD5E1;
}

.submit-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;

  &.submit-verified {
    background: #ECFDF5;
    color: #15803D;
    border: 1px solid #DCFCE7;
  }

  &.submit-processing {
    background: #FFFBEB;
    color: #EA580C;
    border: 1px solid #FDE68A;
  }

  &.submit-pending {
    background: #f5f5f5;
    color: #757575;
    border: 1px solid #e0e0e0;
  }
}

.round-status-tag {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;

  &.round-status-completed {
    background: #ECFDF5;
    color: #15803D;
  }

  &.round-status-progress {
    background: #FFFBEB;
    color: #EA580C;
  }

  &.round-status-pending {
    background: #f5f5f5;
    color: #757575;
  }
}

@media (max-width: 768px) {
  .modal-body {
    flex-direction: column;
  }
  .right-panel {
    width: 100%;
  }
}
</style>