<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">
          <span class="round-badge">第{{ round.roundNo }}期</span>
          <span class="title-text">整改记录</span>
        </h3>
        <span class="modal-subtitle">{{ issueTitle }}</span>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <!-- 期次信息汇总 -->
        <div class="round-info-card">
          <div class="round-info-meta">
            <div class="meta-block">
              <span class="meta-label">下发时间</span>
              <span class="meta-value">{{ round.publishTime }}</span>
            </div>
            <div class="meta-block">
              <span class="meta-label">截止时间</span>
              <span class="meta-value">{{ round.deadline }}</span>
            </div>
            <div class="meta-block">
              <span class="meta-label">期次状态</span>
              <span class="round-status-tag" :class="'round-status-' + round.status">{{ round.statusText }}</span>
            </div>
            <div class="meta-block">
              <span class="meta-label">整改人数</span>
              <span class="meta-value">{{ round.totalPeople }} 人</span>
            </div>
            <div class="meta-block">
              <span class="meta-label">已完成</span>
              <span class="meta-value">{{ round.completedPeople }} 人</span>
            </div>
            <div class="meta-block">
              <span class="meta-label">完成率</span>
              <span class="meta-value">{{ round.completionRate }}%</span>
            </div>
          </div>
        </div>

        <div class="person-tip">
          <i class="el-icon el-icon-info"></i>
          <span>该期次下每位整改人员的提交详情</span>
        </div>

        <!-- 人员整改详情表 -->
        <div class="record-table">
          <div class="record-table-header">
            <span class="col-name">姓名</span>
            <span class="col-dept">部门</span>
            <span class="col-role">角色</span>
            <span class="col-status">提交状态</span>
            <span class="col-time">提交时间</span>
            <span class="col-remark">整改说明</span>
            <span class="col-attach">附件</span>
            <span class="col-action">操作</span>
          </div>
          <div v-if="personRecords.length === 0" class="record-empty">
            <i class="el-icon-warning-outline"></i>
            <span>暂无人员数据</span>
          </div>
          <div v-for="(record, idx) in personRecords" :key="idx" class="record-table-row">
            <span class="col-name">
              <span class="person-avatar">{{ record.name.charAt(0) }}</span>
              <span class="person-name">{{ record.name }}</span>
            </span>
            <span class="col-dept">{{ record.department }}</span>
            <span class="col-role">
              <span class="role-tag">{{ record.role }}</span>
            </span>
            <span class="col-status">
              <span class="submit-tag" :class="'submit-' + record.tagClass">{{ record.statusText }}</span>
            </span>
            <span class="col-time">{{ record.submitTime }}</span>
            <span class="col-remark">{{ record.remark }}</span>
            <span class="col-attach">
              <div v-if="record.attachments && record.attachments.length > 0" class="attachment-list">
                <a v-for="(file, fIdx) in record.attachments" :key="fIdx" class="attachment-link" :href="file.url" target="_blank">
                  <i class="el-icon el-icon-document"></i>
                  <span class="attachment-name">{{ file.name }}</span>
                  <span class="attachment-size">{{ file.size }}</span>
                </a>
              </div>
              <span v-else class="no-attachment">-</span>
            </span>
            <span class="col-action">
              <button class="btn-detail" @click="openDetailModal(record)">
                <i class="el-icon el-icon-view"></i> 查看详情
              </button>
            </span>
          </div>
        </div>
      </div>
      <div class="modal-footer">
        <button class="btn-modal-close" @click="handleClose">关闭</button>
      </div>

      <!-- 人员详情弹窗 -->
      <PersonRoundDetailModal
        :visible="detailModalVisible"
        :person-info="currentDetailPerson"
        :round-info="currentDetailRound"
        @close="closeDetailModal"
      />
    </div>
  </div>
</template>

<script>
import PersonRoundDetailModal from './PersonRoundDetailModal.vue'

export default {
  name: 'RoundRecordModal',
  components: { PersonRoundDetailModal },
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    issueTitle: {
      type: String,
      default: ''
    },
    round: {
      type: Object,
      default: () => ({})
    },
    persons: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      detailModalVisible: false,
      currentDetailPerson: {},
      currentDetailRound: {}
    }
  },
  watch: {
    visible() {
      this.detailModalVisible = false
    }
  },
  computed: {
    personRecords() {
      if (!this.round || !this.round.roundNo) return []
      return this.persons.map(person => {
        const pr = (person.rounds || []).find(r => r.roundNo === this.round.roundNo) || {}
        return {
          name: person.name,
          department: person.department,
          role: person.role,
          statusText: pr.statusText || '未开始',
          tagClass: pr.tagClass || 'pending',
          submitTime: pr.submitTime || '-',
          remark: pr.remark || '-',
          attachments: pr.attachments || [],
          approvalFlow: pr.approvalFlow || []
        }
      })
    },
    submittedCount() {
      return this.round.completedPeople || this.personRecords.filter(r => r.tagClass !== 'pending').length
    },
    pendingCount() {
      const total = this.round.totalPeople || this.personRecords.length
      return total - this.submittedCount
    },
    completionRate() {
      const total = this.round.totalPeople || this.personRecords.length
      if (total === 0) return 0
      return Math.round((this.submittedCount / total) * 100)
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
    openDetailModal(record) {
      this.currentDetailPerson = {
        name: record.name,
        department: record.department,
        role: record.role
      }
      this.currentDetailRound = {
        roundNo: this.round.roundNo,
        publishTime: this.round.publishTime,
        deadline: this.round.deadline,
        roundStatus: this.round.status,
        roundStatusText: this.round.statusText,
        statusText: record.statusText,
        tagClass: record.tagClass,
        submitTime: record.submitTime,
        remark: record.remark,
        attachments: record.attachments,
        approvalFlow: record.approvalFlow,
        submitActions: record.submitActions
      }
      this.detailModalVisible = true
    },
    closeDetailModal() {
      this.detailModalVisible = false
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
  z-index: 200;
  animation: fadeIn 0.2s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.modal-content {
  background: white;
  border-radius: 4px;
  width: 92%;
  max-width: 1100px;
  max-height: 85vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  padding: 20px 24px;
  border-bottom: 1px solid #CBD5E1;
  position: relative;

  .modal-title {
    font-size: 18px;
    font-weight: bold;
    color: var(--color-primary);
    margin: 0;
    display: flex;
    align-items: center;
    gap: 8px;
  }

  .title-text {
    font-size: 18px;
  }

  .modal-subtitle {
    display: block;
    font-size: 13px;
    color: #757575;
    margin-top: 6px;
  }

  .modal-close {
    position: absolute;
    top: 16px;
    right: 16px;
    width: 32px;
    height: 32px;
    border: none;
    background: none;
    border-radius: 50%;
    cursor: pointer;
    font-size: 16px;
    color: var(--color-primary);
    transition: all 0.2s;

    &:hover {
      background: var(--color-primary-light);
      color: var(--color-primary);
    }
  }
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* 期次信息卡片 */
.round-info-card {
  background: var(--color-primary-surface);
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
  border-radius: 3px;
  padding: 16px;
  margin-bottom: 16px;
}

.round-info-meta {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.meta-block {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.meta-label {
  font-size: 12px;
  color: #757575;
}

.meta-value {
  font-size: 14px;
  font-weight: bold;
  color: #1b1c1c;
}

.round-info-summary {
  display: flex;
  gap: 12px;
}

.summary-item {
  flex: 1;
  text-align: center;
}

.summary-label {
  display: block;
  font-size: 12px;
  color: #757575;
  margin-bottom: 4px;
}

.summary-value {
  display: block;
  font-size: 22px;
  font-weight: bold;
  color: #1b1c1c;

  &.summary-green { color: #15803D; }
  &.summary-orange { color: #EA580C; }
  &.summary-red { color: var(--color-primary); }
}

.round-badge {
  display: inline-block;
  padding: 3px 10px;
  background: rgba(var(--color-primary-rgb), 0.1);
  color: var(--color-primary);
  border-radius: 4px;
  font-size: 13px;
  font-weight: bold;
}

.round-status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
  width: fit-content;

  &.round-status-completed {
    background: #f0fdf4;
    color: #15803D;
    border: 1px solid #bbf7d0;
  }

  &.round-status-progress {
    background: var(--color-primary-surface);
    color: var(--color-primary);
    border: 1px solid #CBD5E1;
  }

  &.round-status-pending {
    background: #f5f5f5;
    color: #616161;
    border: 1px solid #e0e0e0;
  }
}

.person-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(var(--color-primary-rgb), 0.05);
  padding: 8px 12px;
  border-radius: 2px;
  margin-bottom: 16px;

  i {
    font-size: 14px;
  }
}

/* 人员整改详情表 */
.record-table {
  border: 1px solid rgba(var(--color-primary-rgb), 0.1);
  border-radius: 2px;
  overflow: hidden;
  background: white;
}

.record-table-header {
  display: grid;
  grid-template-columns: 1fr 1fr 0.8fr 0.9fr 1.1fr 1.4fr 1.2fr 0.8fr;
  gap: 8px;
  padding: 10px 12px;
  background: var(--color-primary-light);
  font-size: 12px;
  font-weight: bold;
  color: var(--color-primary);
}

.record-table-row {
  display: grid;
  grid-template-columns: 1fr 1fr 0.8fr 0.9fr 1.1fr 1.4fr 1.2fr 0.8fr;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid rgba(var(--color-primary-rgb), 0.08);
  font-size: 13px;
  color: #1b1c1c;
  align-items: center;
  transition: background 0.2s;

  &:hover {
    background: var(--color-primary-surface);
  }
}

.record-empty {
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

.col-name {
  display: flex;
  align-items: center;
  gap: 8px;
}

.person-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: rgba(var(--color-primary-rgb), 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: bold;
  color: var(--color-primary);
  flex-shrink: 0;
}

.person-name {
  font-size: 13px;
  font-weight: bold;
  color: #1b1c1c;
}

.col-dept {
  font-size: 13px;
  color: var(--color-primary);
}

.role-tag {
  display: inline-block;
  padding: 2px 6px;
  background: rgba(var(--color-primary-rgb), 0.08);
  color: var(--color-primary);
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
}

.col-time {
  font-size: 12px;
  color: #757575;
}

.col-remark {
  font-size: 12px;
  color: #1b1c1c;
  line-height: 1.5;
}

/* 附件列 */
.col-attach {
  font-size: 12px;
}

.attachment-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.attachment-link {
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--color-primary);
  text-decoration: none;
  font-size: 11px;
  transition: color 0.2s;

  &:hover {
    text-decoration: underline;
    color: var(--color-primary-deep);
  }

  i {
    font-size: 12px;
  }
}

.attachment-name {
  flex: 1;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.attachment-size {
  color: #757575;
  font-size: 10px;
}

.no-attachment {
  color: #94A3B8;
  font-size: 12px;
}

/* 操作列 */
.col-action {
  text-align: center;
}

.btn-detail {
  padding: 4px 10px;
  background: rgba(var(--color-primary-rgb), 0.05);
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  font-size: 12px;
  color: var(--color-primary);
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: rgba(var(--color-primary-rgb), 0.1);
    border-color: var(--color-primary);
  }

  i {
    font-size: 12px;
  }
}

/* 提交状态标签 */
.submit-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;

  &.submit-verified {
    background: #f0fdf4;
    color: #15803D;
    border: 1px solid #bbf7d0;
  }

  &.submit-processing {
    background: #FFFBEB;
    color: #EA580C;
    border: 1px solid #FED7AA;
  }

  &.submit-pending {
    background: #f5f5f5;
    color: #616161;
    border: 1px solid #e0e0e0;
  }
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #CBD5E1;
  display: flex;
  justify-content: flex-end;
}

.btn-modal-close {
  padding: 8px 24px;
  border: 1px solid #CBD5E1;
  border-radius: 4px;
  font-size: 14px;
  color: var(--color-primary);
  background: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: var(--color-primary-light);
    color: var(--color-primary);
    border-color: var(--color-primary);
  }
}
</style>
