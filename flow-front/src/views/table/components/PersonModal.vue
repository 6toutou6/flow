<template>
  <div v-if="visible" class="modal-overlay" @click.self="handleClose">
    <div class="modal-content">
      <div class="modal-header">
        <h3 class="modal-title">整改详情</h3>
        <span class="modal-subtitle">{{ issueTitle }}</span>
        <button class="modal-close" @click="handleClose">
          <i class="el-icon el-icon-close"></i>
        </button>
      </div>
      <div class="modal-body">
        <!-- Tab 切换 -->
        <div class="tab-header">
          <button class="tab-btn" :class="{ active: activeTab === 'persons' }" @click="activeTab = 'persons'">
            <i class="el-icon el-icon-user"></i>
            <span>整改人员</span>
          </button>
        </div>

        <!-- 整改人员 Tab -->
        <div v-if="activeTab === 'persons'" class="persons-section">
          <div class="person-summary">
            <div class="summary-item">
              <span class="summary-label">总人数</span>
              <span class="summary-value">{{ persons.length }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">已提交</span>
              <span class="summary-value summary-green">{{ submittedCount }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">待提交</span>
              <span class="summary-value summary-orange">{{ pendingCount }}</span>
            </div>
            <div class="summary-item">
              <span class="summary-label">完成率</span>
              <span class="summary-value summary-red">{{ completionRate }}%</span>
            </div>
          </div>
          <div class="person-tip">
            <i class="el-icon el-icon-info"></i>
            <span>点击人员卡片可查看其各期次整改计划</span>
          </div>
          <div class="person-list">
            <div v-for="(person, pIndex) in persons" :key="pIndex" class="person-card-wrapper">
              <div class="person-card" :class="{ expanded: expandedPersons.includes(pIndex) }" @click="togglePerson(pIndex)">
                <div class="person-avatar-lg">{{ person.name.charAt(0) }}</div>
                <div class="person-detail">
                  <div class="person-name-row">
                    <span class="person-name-lg">{{ person.name }}</span>
                    <span class="person-role-tag">{{ person.role }}</span>
                  </div>
                  <div class="person-dept">{{ person.department }}</div>
                </div>
                <div class="person-submit-status">
                  <span class="submit-tag" :class="'submit-' + person.tagClass">{{ person.tag }}</span>
                  <span class="submit-text">{{ person.status }}</span>
                </div>
                <i class="el-icon arrow-icon" :class="expandedPersons.includes(pIndex) ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
              </div>
              <!-- 人员各期次整改计划 -->
              <div v-if="expandedPersons.includes(pIndex)" class="person-rounds-detail">
                <div class="detail-title">
                  <i class="el-icon el-icon-document"></i>
                  <span>各期次整改计划（共 {{ getPersonRounds(person).length }} 期）</span>
                </div>
                <div class="detail-table">
                  <div class="detail-table-header">
                    <span class="d-round">期次</span>
                    <span class="d-publish">下发时间</span>
                    <span class="d-deadline">截止时间</span>
                    <span class="d-round-status">期次状态</span>
                    <span class="d-remark">整改说明</span>
                    <span class="d-attach">附件</span>
                    <span class="d-action">操作</span>
                  </div>
                  <div v-for="(round, rIdx) in getPersonRounds(person)" :key="rIdx" class="detail-table-row" :class="{ selected: selectedPersonIndex === pIndex && selectedRoundIndex === rIdx }">
                    <span class="d-round">
                      <span class="round-badge">第{{ round.roundNo }}期</span>
                    </span>
                    <span class="d-publish">{{ round.publishTime }}</span>
                    <span class="d-deadline">{{ round.deadline }}</span>
                    <span class="d-round-status">
                      <span class="round-status-tag" :class="'round-status-' + round.roundStatus">{{ round.roundStatusText }}</span>
                    </span>
                    <span class="d-remark">{{ round.remark }}</span>
                    <span class="d-attach">
                      <div v-if="round.attachments && round.attachments.length > 0" class="attachment-list">
                        <a v-for="(file, fIdx) in round.attachments" :key="fIdx" class="attachment-link" :href="file.url" target="_blank">
                          <i class="el-icon el-icon-document"></i>
                          <span class="attachment-name">{{ file.name }}</span>
                          <span class="attachment-size">{{ file.size }}</span>
                        </a>
                      </div>
                      <span v-else class="no-attachment">-</span>
                    </span>
                    <span class="d-action">
                      <button class="btn-detail" @click="openDetailModal(person, round)">
                        <i class="el-icon el-icon-view"></i> 查看详情
                      </button>
                    </span>
                  </div>
                </div>
              </div>
            </div>
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
  name: 'PersonModal',
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
    persons: {
      type: Array,
      default: () => []
    },
    rounds: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      activeTab: 'persons',
      expandedPersons: [],
      detailModalVisible: false,
      currentDetailPerson: {},
      currentDetailRound: {},
      selectedPersonIndex: -1,
      selectedRoundIndex: -1
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.activeTab = 'persons'
        this.expandedPersons = []
        this.detailModalVisible = false
        this.selectedPersonIndex = -1
        this.selectedRoundIndex = -1
      }
    }
  },
  computed: {
    submittedCount() {
      return this.persons.filter(p => p.status === '已提交').length
    },
    pendingCount() {
      return this.persons.filter(p => p.status === '待提交').length
    },
    completionRate() {
      if (this.persons.length === 0) return 0
      return Math.round((this.submittedCount / this.persons.length) * 100)
    }
  },
  methods: {
    handleClose() {
      this.$emit('close')
    },
    togglePerson(index) {
      const idx = this.expandedPersons.indexOf(index)
      if (idx > -1) {
        this.expandedPersons.splice(idx, 1)
      } else {
        this.expandedPersons.push(index)
      }
    },
    getPersonRounds(person) {
      const personRounds = person.rounds || []
      return this.rounds.map(round => {
        const pr = personRounds.find(r => r.roundNo === round.roundNo) || {}
        return {
          roundNo: round.roundNo,
          publishTime: round.publishTime,
          deadline: round.deadline,
          roundStatus: round.status,
          roundStatusText: round.statusText,
          statusText: pr.statusText || '未开始',
          tagClass: pr.tagClass || 'pending',
          submitTime: pr.submitTime || '-',
          remark: pr.remark || '-',
          attachments: pr.attachments || [],
          approvalFlow: pr.approvalFlow || []
        }
      })
    },
    openDetailModal(person, round) {
      const pIndex = this.persons.indexOf(person)
      const personRounds = this.getPersonRounds(person)
      const rIdx = personRounds.indexOf(round)
      this.selectedPersonIndex = pIndex
      this.selectedRoundIndex = rIdx
      this.currentDetailPerson = {
        name: person.name,
        department: person.department,
        role: person.role
      }
      this.currentDetailRound = round
      this.detailModalVisible = true
    },
    closeDetailModal() {
      this.detailModalVisible = false
      this.selectedPersonIndex = -1
      this.selectedRoundIndex = -1
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
  border-radius: 12px;
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
  border-bottom: 1px solid #e4beba;
  position: relative;

  .modal-title {
    font-size: 18px;
    font-weight: bold;
    color: #a20513;
    margin: 0;
  }

  .modal-subtitle {
    display: block;
    font-size: 13px;
    color: #757575;
    margin-top: 4px;
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
    color: #5b403d;
    transition: all 0.2s;

    &:hover {
      background: #f6f3f2;
      color: #a20513;
    }
  }
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
}

/* Tab 切换 */
.tab-header {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e4beba;
  padding: 0 4px;
}

.tab-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: none;
  background: none;
  font-size: 14px;
  font-weight: bold;
  color: #757575;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;

  &:hover {
    color: #a20513;
  }

  &.active {
    color: #a20513;
    border-bottom-color: #a20513;
  }
}

/* 整改计划 - 期次列表 */
.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.content-label {
  font-size: 13px;
  font-weight: bold;
  letter-spacing: 0.05em;
  color: #5b403d;
  margin: 0;
  text-transform: uppercase;
}

.round-count {
  font-size: 12px;
  font-weight: normal;
  color: #a20513;
  margin-left: 8px;
}

.rounds-table {
  border: 1px solid rgba(228, 190, 186, 0.4);
  border-radius: 6px;
  overflow: hidden;
}

.rounds-table-header {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1.2fr 0.8fr 1.5fr 0.8fr;
  gap: 8px;
  padding: 10px 12px;
  background: #f6f3f2;
  font-size: 12px;
  font-weight: bold;
  color: #5b403d;
}

.rounds-table-row {
  display: grid;
  grid-template-columns: 1fr 1.2fr 1.2fr 0.8fr 1.5fr 0.8fr;
  gap: 8px;
  padding: 10px 12px;
  border-top: 1px solid rgba(228, 190, 186, 0.3);
  font-size: 13px;
  color: #1b1c1c;
  align-items: center;
  transition: background 0.2s;

  &:hover {
    background: #fcf9f8;
  }
}

.round-badge {
  display: inline-block;
  padding: 2px 8px;
  background: rgba(162, 5, 19, 0.1);
  color: #a20513;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.col-time, .col-deadline {
  font-size: 13px;
  color: #5b403d;
}

.col-people {
  font-size: 13px;
  color: #1b1c1c;

  i {
    margin-right: 4px;
    color: #a20513;
  }
}

.col-rate {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.rate-bar {
  width: 100%;
  height: 6px;
  background: #f0f0f0;
  border-radius: 3px;
  overflow: hidden;
}

.rate-fill {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;

  &.rate-completed { background: #166534; }
  &.rate-progress { background: #a20513; }
  &.rate-pending { background: #e0e0e0; }
}

.rate-text {
  font-size: 11px;
  color: #757575;
}

.round-status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;

  &.round-status-completed {
    background: #f0fdf4;
    color: #166534;
    border: 1px solid #bbf7d0;
  }

  &.round-status-progress {
    background: #FEF0F0;
    color: #C53030;
    border: 1px solid #E4BEBA;
  }

  &.round-status-pending {
    background: #f5f5f5;
    color: #616161;
    border: 1px solid #e0e0e0;
  }
}

/* 整改人员 - 汇总 */
.person-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  padding: 16px;
  background: #fcf9f8;
  border-radius: 8px;
  border: 1px solid rgba(228, 190, 186, 0.3);
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
  font-size: 24px;
  font-weight: bold;
  color: #1b1c1c;

  &.summary-green { color: #166534; }
  &.summary-orange { color: #e65100; }
  &.summary-red { color: #a20513; }
}

.person-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #a20513;
  background: rgba(162, 5, 19, 0.05);
  padding: 8px 12px;
  border-radius: 6px;
  margin-bottom: 16px;

  i {
    font-size: 14px;
  }
}

/* 整改人员 - 列表 */
.person-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.person-card-wrapper {
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;

  &:hover {
    box-shadow: 0 2px 8px rgba(162, 5, 19, 0.08);
  }
}

.person-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: white;
  border: 1px solid #e4beba;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;

  &.expanded {
    border-color: #a20513;
    border-bottom-left-radius: 0;
    border-bottom-right-radius: 0;
  }

  &:hover {
    border-color: #a20513;
  }
}

.person-avatar-lg {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(162, 5, 19, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: bold;
  color: #a20513;
  flex-shrink: 0;
}

.person-detail {
  flex: 1;
}

.person-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.person-name-lg {
  font-size: 15px;
  font-weight: bold;
  color: #1b1c1c;
}

.person-role-tag {
  padding: 2px 6px;
  background: rgba(162, 5, 19, 0.08);
  color: #a20513;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;
}

.person-dept {
  font-size: 12px;
  color: #757575;
  margin-top: 2px;
}

.person-submit-status {
  text-align: right;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.arrow-icon {
  color: #757575;
  font-size: 14px;
  transition: transform 0.2s;
}

/* 人员各期次整改情况 */
.person-rounds-detail {
  background: #fcf9f8;
  border: 1px solid #a20513;
  border-top: none;
  border-bottom-left-radius: 8px;
  border-bottom-right-radius: 8px;
  padding: 16px;
}

.detail-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: bold;
  color: #a20513;
  margin-bottom: 12px;

  i {
    font-size: 14px;
  }
}

.detail-table {
  border: 1px solid rgba(228, 190, 186, 0.4);
  border-radius: 6px;
  overflow: hidden;
  background: white;
}

.detail-table-header {
  display: grid;
  grid-template-columns: 0.7fr 1fr 1fr 0.9fr 1.8fr 1.2fr 0.8fr;
  gap: 8px;
  padding: 8px 12px;
  background: #f6f3f2;
  font-size: 12px;
  font-weight: bold;
  color: #5b403d;
}

.detail-table-row {
  display: grid;
  grid-template-columns: 0.7fr 1fr 1fr 0.9fr 1.8fr 1.2fr 0.8fr;
  gap: 8px;
  padding: 8px 12px;
  border-top: 1px solid rgba(228, 190, 186, 0.3);
  font-size: 12px;
  color: #1b1c1c;
  align-items: center;
  transition: background 0.2s;

  &:hover {
    background: #fcf9f8;
  }

  &.selected {
    background: #a20513;
    color: white;

    .d-publish,
    .d-deadline {
      color: rgba(255, 255, 255, 0.8);
    }

    .round-badge {
      background: rgba(255, 255, 255, 0.2);
      color: white;
    }

    .round-status-tag {
      background: rgba(255, 255, 255, 0.2);
      color: white;
      border-color: rgba(255, 255, 255, 0.3);
    }

    .btn-detail {
      background: white;
      color: #a20513;
      border-color: white;

      &:hover {
        background: rgba(255, 255, 255, 0.9);
      }
    }

    .d-remark {
      color: rgba(255, 255, 255, 0.9);
    }

    .attachment-link {
      color: rgba(255, 255, 255, 0.9);

      &:hover {
        color: white;
      }
    }

    .no-attachment {
      color: rgba(255, 255, 255, 0.5);
    }
  }
}

.d-publish, .d-deadline {
  color: #5b403d;
}

.d-remark {
  color: #1b1c1c;
  line-height: 1.5;
}

/* 附件列 */
.d-attach {
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
  color: #a20513;
  text-decoration: none;
  font-size: 11px;
  transition: color 0.2s;

  &:hover {
    text-decoration: underline;
    color: #8a0410;
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
  color: #c8b3b0;
  font-size: 12px;
}

/* 操作列 */
.d-action {
  text-align: center;
}

.btn-detail {
  padding: 4px 10px;
  background: rgba(162, 5, 19, 0.05);
  border: 1px solid #e4beba;
  border-radius: 4px;
  font-size: 12px;
  color: #a20513;
  cursor: pointer;
  transition: all 0.2s;
  display: inline-flex;
  align-items: center;
  gap: 4px;

  &:hover {
    background: rgba(162, 5, 19, 0.1);
    border-color: #a20513;
  }

  i {
    font-size: 12px;
  }
}

/* 提交状态标签（人员卡片共用） */
.submit-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: bold;

  &.submit-verified {
    background: #f0fdf4;
    color: #166534;
    border: 1px solid #bbf7d0;
  }

  &.submit-processing {
    background: #fff3e0;
    color: #e65100;
    border: 1px solid #ffab91;
  }

  &.submit-pending {
    background: #f5f5f5;
    color: #616161;
    border: 1px solid #e0e0e0;
  }
}

.submit-text {
  font-size: 11px;
  color: #757575;
}

.modal-footer {
  padding: 16px 24px;
  border-top: 1px solid #e4beba;
  display: flex;
  justify-content: flex-end;
}

.btn-modal-close {
  padding: 8px 24px;
  border: 1px solid #e4beba;
  border-radius: 4px;
  font-size: 14px;
  color: #5b403d;
  background: white;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    background: #f6f3f2;
    color: #a20513;
    border-color: #a20513;
  }
}
</style>
