<template>
  <div class="correct-container">
    <main class="main-content">
      <div class="page-header">
        <nav class="breadcrumb">
          <span>首页</span>
          <span>/</span>
          <span class="active">整改人员确认</span>
        </nav>
        <h3 class="page-heading">整改人员确认</h3>
      </div>

      <section class="form-section">
        <div class="form-card">
          <div class="form-header">
            <h4>问题信息确认</h4>
            <span class="form-tip">请确认问题是否属实，并填写相关整改信息</span>
          </div>

          <el-form :model="form" ref="formRef" label-width="100px" class="form-body">
            <el-row :gutter="16">
              <el-col :span="12">
                <el-form-item label="问题编号" prop="issueNo">
                  <el-input v-model="form.issueNo" placeholder="请输入问题编号" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="问题标题" prop="title">
                  <el-input v-model="form.title" placeholder="请输入问题标题" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="问题是否属实" prop="isTrue">
              <el-radio-group v-model="form.isTrue">
                <el-radio :value="true">属实</el-radio>
                <el-radio :value="false">不属实</el-radio>
              </el-radio-group>
            </el-form-item>

            <el-form-item v-if="form.isTrue === false" label="不属实说明" prop="reason">
              <el-textarea v-model="form.reason" :rows="4" placeholder="请说明问题不属实的原因" />
            </el-form-item>

            <template v-if="form.isTrue === true">
              <el-form-item label="问题原因" prop="reason">
                <el-textarea v-model="form.reason" :rows="4" placeholder="请详细说明问题产生的原因" />
              </el-form-item>

              <el-form-item label="整改计划" prop="plan">
                <el-textarea v-model="form.plan" :rows="4" placeholder="请制定整改计划，包括整改措施、时间安排等" />
              </el-form-item>

              <el-form-item label="情况说明">
                <el-textarea v-model="form.description" :rows="3" placeholder="请补充说明相关情况" />
              </el-form-item>

              <el-form-item label="附件材料">
                <div class="upload-area">
                  <el-upload
                    class="upload-list"
                    :file-list="form.attachments"
                    :auto-upload="false"
                    :on-change="handleFileChange"
                    :on-remove="handleFileRemove"
                    :on-preview="handleFilePreview"
                    list-type="text"
                  >
                    <el-button size="small" type="primary">点击上传附件</el-button>
                  </el-upload>
                </div>
              </el-form-item>

              <el-form-item label="是否需要审批">
                <el-radio-group v-model="form.needApproval">
                  <el-radio :value="true">需要</el-radio>
                  <el-radio :value="false">不需要</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item v-if="form.needApproval === true" label="审批人员" prop="approvers">
                <div class="approver-section">
                  <div class="approver-selected">
                    <el-tag
                      v-for="(approver, idx) in form.approvers"
                      :key="idx"
                      closable
                      @close="removeApprover(idx)"
                      class="approver-tag"
                    >
                      <span>{{ approver.name }}</span>
                      <span class="approver-dept">({{ approver.department }})</span>
                    </el-tag>
                    <el-button
                      v-if="form.approvers.length < 3"
                      type="dashed"
                      size="small"
                      @click="openApproverModal"
                    >
                      <i class="el-icon-plus"></i>
                      添加审批人员
                    </el-button>
                  </div>
                  <div v-if="form.approvers.length === 0" class="empty-hint">
                    <span>请添加审批人员</span>
                  </div>
                </div>
              </el-form-item>
            </template>
          </el-form>

          <div class="form-footer">
            <el-button type="default" @click="resetForm">重置</el-button>
            <el-button
              type="primary"
              :disabled="!canSubmit"
              :loading="loading"
              @click="handleSubmit"
            >
              提交确认
            </el-button>
          </div>
        </div>
      </section>

      <section class="records-section">
        <div class="records-card">
          <div class="records-header">
            <h4>整改记录列表</h4>
            <div class="records-actions">
              <el-button type="text" @click="refreshRecords">
                <i class="el-icon-refresh"></i>
              </el-button>
            </div>
          </div>

          <div class="filter-bar">
            <el-select v-model="filterStatus" placeholder="全部状态" class="filter-select">
              <el-option label="全部" value="" />
              <el-option label="待确认" value="pending" />
              <el-option label="已确认属实" value="confirmed" />
              <el-option label="不属实" value="rejected" />
              <el-option label="整改中" value="processing" />
              <el-option label="已完成" value="completed" />
            </el-select>
            <el-select v-model="filterIsTrue" placeholder="是否属实" class="filter-select">
              <el-option label="全部" value="" />
              <el-option label="属实" :value="true" />
              <el-option label="不属实" :value="false" />
            </el-select>
            <el-input
              v-model="filterPersonName"
              placeholder="搜索整改人员"
              class="filter-input"
              @keyup.enter="fetchRecords"
            />
            <el-button type="primary" @click="fetchRecords">搜索</el-button>
          </div>

          <el-table :data="records" border stripe class="records-table">
            <el-table-column prop="issueNo" label="问题编号" width="150" />
            <el-table-column prop="title" label="问题标题" min-width="200" />
            <el-table-column prop="personName" label="整改人员" width="120" />
            <el-table-column prop="isTrueText" label="是否属实" width="100">
              <template slot-scope="scope">
                <el-tag :type="scope.row.isTrue ? 'success' : 'danger'">
                  {{ scope.row.isTrueText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="statusText" label="状态" width="120">
              <template slot-scope="scope">
                <el-tag :type="getStatusType(scope.row.status)">
                  {{ scope.row.statusText }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="创建时间" width="180" />
            <el-table-column label="操作" width="150">
              <template slot-scope="scope">
                <el-button type="text" size="small" @click="viewRecord(scope.row)">
                  查看详情
                </el-button>
                <el-button type="text" size="small" @click="deleteRecord(scope.row)">
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            @size-change="handleSizeChange"
            @current-change="handleCurrentChange"
            :current-page="pagination.page"
            :page-sizes="[10, 20, 50]"
            :page-size="pagination.limit"
            :total="pagination.total"
            layout="total, sizes, prev, pager, next, jumper"
            class="pagination"
          />
        </div>
      </section>
    </main>

    <el-dialog
      title="选择审批人员"
      :visible.sync="approverModalVisible"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="filter-row">
        <el-input
          v-model="approverFilters.name"
          placeholder="搜索姓名"
          class="filter-input"
          @keyup.enter="fetchApprovers"
        />
        <el-select v-model="approverFilters.department" placeholder="全部部门" class="filter-select">
          <el-option label="全部部门" value="" />
          <el-option label="安全管理部" value="安全管理部" />
          <el-option label="质量管控部" value="质量管控部" />
          <el-option label="技术研发中心" value="技术研发中心" />
          <el-option label="高层管理" value="高层管理" />
        </el-select>
        <el-button type="primary" @click="fetchApprovers">搜索</el-button>
      </div>

      <el-table :data="approverList" border class="approver-table">
        <el-table-column width="50">
          <template slot-scope="scope">
            <el-checkbox
              :checked="isApproverSelected(scope.row.id)"
              @change="toggleApprover(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="department" label="部门" width="120" />
        <el-table-column prop="role" label="角色" width="150" />
      </el-table>

      <div slot="footer">
        <el-button @click="approverModalVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmApproverSelection">确认选择</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="整改详情"
      :visible.sync="detailModalVisible"
      width="700px"
      :close-on-click-modal="false"
    >
      <div v-if="currentRecord" class="detail-content">
        <div class="detail-row">
          <span class="detail-label">问题编号</span>
          <span class="detail-value">{{ currentRecord.issueNo }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">问题标题</span>
          <span class="detail-value">{{ currentRecord.title }}</span>
        </div>
        <div class="detail-row">
          <span class="detail-label">是否属实</span>
          <el-tag :type="currentRecord.isTrue ? 'success' : 'danger'">
            {{ currentRecord.isTrueText }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">问题原因</span>
          <p class="detail-text">{{ currentRecord.reason }}</p>
        </div>
        <div v-if="currentRecord.isTrue" class="detail-row">
          <span class="detail-label">整改计划</span>
          <p class="detail-text">{{ currentRecord.plan }}</p>
        </div>
        <div v-if="currentRecord.description" class="detail-row">
          <span class="detail-label">情况说明</span>
          <p class="detail-text">{{ currentRecord.description }}</p>
        </div>
        <div v-if="currentRecord.attachments && currentRecord.attachments.length > 0" class="detail-row">
          <span class="detail-label">附件材料</span>
          <div class="detail-attachments">
            <el-tag
              v-for="(file, idx) in currentRecord.attachments"
              :key="idx"
              type="info"
            >
              {{ file.name }} ({{ file.size }})
            </el-tag>
          </div>
        </div>
        <div v-if="currentRecord.needApproval && currentRecord.approvers && currentRecord.approvers.length > 0" class="detail-row">
          <span class="detail-label">审批人员</span>
          <div class="detail-approvers">
            <el-tag
              v-for="(approver, idx) in currentRecord.approvers"
              :key="idx"
              type="primary"
            >
              {{ approver.name }} ({{ approver.department }})
            </el-tag>
          </div>
        </div>
        <div class="detail-row">
          <span class="detail-label">状态</span>
          <el-tag :type="getStatusType(currentRecord.status)">
            {{ currentRecord.statusText }}
          </el-tag>
        </div>
        <div class="detail-row">
          <span class="detail-label">创建时间</span>
          <span class="detail-value">{{ currentRecord.createdAt }}</span>
        </div>
      </div>

      <div slot="footer">
        <el-button @click="detailModalVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="确认删除"
      :visible.sync="deleteModalVisible"
      width="300px"
      :close-on-click-modal="false"
    >
      <p>确定要删除这条整改记录吗？</p>

      <div slot="footer">
        <el-button @click="deleteModalVisible = false">取消</el-button>
        <el-button type="danger" :loading="deleteLoading" @click="confirmDelete">确认删除</el-button>
      </div>
    </el-dialog>

    <el-dialog
      title="提交成功"
      :visible.sync="successModalVisible"
      width="400px"
    >
      <div class="success-content">
        <i class="el-icon-circle-check success-icon"></i>
        <h4>{{ successTitle }}</h4>
        <p>{{ successMessage }}</p>
      </div>

      <div slot="footer">
        <el-button type="primary" @click="closeSuccessModal">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createCorrectRecord,
  getApproverList,
  getCorrectRecords,
  deleteCorrectRecord
} from '@/api/correctPerson'

export default {
  name: 'CorrectPerson',
  data() {
    return {
      loading: false,
      deleteLoading: false,
      form: {
        issueNo: '',
        title: '',
        isTrue: undefined,
        reason: '',
        plan: '',
        description: '',
        attachments: [],
        needApproval: false,
        approvers: []
      },
      approverModalVisible: false,
      approverList: [],
      approverFilters: {
        name: '',
        department: ''
      },
      detailModalVisible: false,
      currentRecord: null,
      deleteModalVisible: false,
      recordToDelete: null,
      successModalVisible: false,
      successTitle: '',
      successMessage: '',
      records: [],
      pagination: {
        page: 1,
        limit: 10,
        total: 0
      },
      filterStatus: '',
      filterIsTrue: '',
      filterPersonName: ''
    }
  },
  computed: {
    canSubmit() {
      if (!this.form.issueNo || !this.form.title) return false
      if (this.form.isTrue === undefined) return false
      if (this.form.isTrue === false) {
        return !!this.form.reason
      }
      if (!this.form.reason || !this.form.plan) return false
      if (this.form.needApproval && this.form.approvers.length === 0) return false
      return true
    }
  },
  mounted() {
    this.fetchRecords()
  },
  methods: {
    getStatusType(status) {
      const map = {
        pending: 'warning',
        confirmed: 'primary',
        rejected: 'danger',
        processing: 'info',
        completed: 'success'
      }
      return map[status] || 'info'
    },
    async fetchApprovers() {
      const res = await getApproverList({
        page: 1,
        limit: 20,
        name: this.approverFilters.name,
        department: this.approverFilters.department
      })
      this.approverList = res.data.items
    },
    openApproverModal() {
      this.approverFilters = { name: '', department: '' }
      this.fetchApprovers()
      this.approverModalVisible = true
    },
    isApproverSelected(id) {
      return this.form.approvers.some(a => a.id === id)
    },
    toggleApprover(person) {
      if (this.form.approvers.length >= 3 && !this.isApproverSelected(person.id)) {
        this.$message.warning('最多只能选择3位审批人员')
        return
      }
      const idx = this.form.approvers.findIndex(a => a.id === person.id)
      if (idx > -1) {
        this.form.approvers.splice(idx, 1)
      } else {
        this.form.approvers.push({
          id: person.id,
          name: person.name,
          department: person.department,
          role: person.role
        })
      }
    },
    confirmApproverSelection() {
      this.approverModalVisible = false
    },
    removeApprover(idx) {
      this.form.approvers.splice(idx, 1)
    },
    handleFileChange(file, fileList) {
      this.form.attachments = fileList
    },
    handleFileRemove(file, fileList) {
      this.form.attachments = fileList
    },
    handleFilePreview(file) {
      this.$message.info(`预览文件: ${file.name}`)
    },
    resetForm() {
      this.form = {
        issueNo: '',
        title: '',
        isTrue: undefined,
        reason: '',
        plan: '',
        description: '',
        attachments: [],
        needApproval: false,
        approvers: []
      }
      if (this.$refs.formRef) {
        this.$refs.formRef.resetFields()
      }
    },
    async handleSubmit() {
      this.loading = true
      try {
        const data = {
          ...this.form,
          status: this.form.isTrue ? 'confirmed' : 'rejected'
        }
        await createCorrectRecord(data)
        this.successTitle = this.form.isTrue ? '问题已确认属实' : '问题已确认不属实'
        this.successMessage = this.form.isTrue
          ? '整改计划已提交，等待审批人员审核'
          : '问题不属实，已结束流程'
        this.successModalVisible = true
        this.resetForm()
        this.fetchRecords()
      } catch (error) {
        this.$message.error('提交失败')
      } finally {
        this.loading = false
      }
    },
    closeSuccessModal() {
      this.successModalVisible = false
    },
    async fetchRecords() {
      const params = {
        page: this.pagination.page,
        limit: this.pagination.limit,
        status: this.filterStatus,
        personName: this.filterPersonName
      }
      if (this.filterIsTrue !== '') {
        params.isTrue = this.filterIsTrue
      }
      const res = await getCorrectRecords(params)
      this.records = res.data.items
      this.pagination.total = res.data.total
    },
    refreshRecords() {
      this.pagination.page = 1
      this.fetchRecords()
    },
    handleSizeChange(val) {
      this.pagination.limit = val
      this.pagination.page = 1
      this.fetchRecords()
    },
    handleCurrentChange(val) {
      this.pagination.page = val
      this.fetchRecords()
    },
    viewRecord(record) {
      this.currentRecord = record
      this.detailModalVisible = true
    },
    deleteRecord(record) {
      this.recordToDelete = record
      this.deleteModalVisible = true
    },
    async confirmDelete() {
      this.deleteLoading = true
      try {
        await deleteCorrectRecord(this.recordToDelete.id)
        this.$message.success('删除成功')
        this.deleteModalVisible = false
        this.fetchRecords()
      } catch (error) {
        this.$message.error('删除失败')
      } finally {
        this.deleteLoading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.correct-container {
  min-height: 100vh;
  background-color: #fcf9f8;
}

.main-content {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #5b403d;

  .active {
    font-weight: bold;
    color: #a20513;
  }
}

.page-heading {
  font-size: 20px;
  font-weight: 600;
  color: #a20513;
  margin: 0;
}

.form-section {
  max-width: 800px;
  margin: 0 auto 24px;
}

.form-card {
  background: white;
  border: 1px solid #e4beba;
  border-radius: 8px;
  overflow: hidden;
}

.form-header {
  padding: 16px 20px;
  background: #fcf9f8;
  border-bottom: 1px solid #e4beba;

  h4 {
    font-size: 16px;
    font-weight: bold;
    color: #a20513;
    margin: 0 0 4px 0;
  }

  .form-tip {
    font-size: 12px;
    color: #757575;
  }
}

.form-body {
  padding: 20px;
}

.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid #e4beba;
  background: #fcf9f8;
}

.upload-area {
  border: 1px dashed #e4beba;
  border-radius: 4px;
  padding: 16px;
  background: #fcf9f8;
}

.approver-section {
  margin-top: 8px;
}

.approver-selected {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.approver-tag {
  .approver-dept {
    color: #757575;
    font-size: 11px;
    margin-left: 4px;
  }
}

.empty-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #c8b3b0;
}

.records-section {
  max-width: 1200px;
  margin: 0 auto;
}

.records-card {
  background: white;
  border: 1px solid #e4beba;
  border-radius: 8px;
  overflow: hidden;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #fcf9f8;
  border-bottom: 1px solid #e4beba;

  h4 {
    font-size: 16px;
    font-weight: bold;
    color: #a20513;
    margin: 0;
  }
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  background: #fcf9f8;
  border-bottom: 1px solid #e4beba;

  .filter-select {
    width: 120px;
  }

  .filter-input {
    width: 180px;
  }
}

.records-table {
  margin: 0;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
}

.filter-row {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;

  .filter-input {
    flex: 1;
  }

  .filter-select {
    width: 140px;
  }
}

.approver-table {
  margin: 0;
}

.detail-content {
  padding: 10px 0;
}

.detail-row {
  display: flex;
  margin-bottom: 16px;

  &:last-child {
    margin-bottom: 0;
  }

  .detail-label {
    width: 100px;
    font-weight: 500;
    color: #5b403d;
    flex-shrink: 0;
  }

  .detail-value {
    flex: 1;
    color: #333;
  }

  .detail-text {
    flex: 1;
    color: #333;
    margin: 0;
    line-height: 1.6;
    white-space: pre-wrap;
  }

  .detail-attachments,
  .detail-approvers {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    flex: 1;
  }
}

.success-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;

  .success-icon {
    font-size: 48px;
    color: #67c23a;
    margin-bottom: 12px;
  }

  h4 {
    font-size: 18px;
    font-weight: bold;
    color: #333;
    margin: 0 0 8px 0;
  }

  p {
    font-size: 14px;
    color: #666;
    margin: 0;
  }
}

@media (max-width: 768px) {
  .filter-bar {
    flex-wrap: wrap;
  }

  .detail-row {
    flex-direction: column;

    .detail-label {
      margin-bottom: 4px;
    }
  }
}
</style>