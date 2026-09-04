<template>
  <div class="dashboard-container">
    <main class="main-content">
      <section class="page-content">
        <!-- 页头 -->
        <div class="page-header">
          <div class="header-left">
            <div class="hl-row1">
              <button class="btn-back" @click="goBack"><i class="el-icon-arrow-left" /> 返回</button>
              <nav class="breadcrumb">
                <span class="link" @click="goBack">任务处理</span>
                <span>/</span>
                <span class="active">{{ flowTitle }}</span>
              </nav>
            </div>
            <h3 class="page-heading">{{ flowTitle }}</h3>
          </div>
        </div>

        <!-- 任务上下文 -->
        <section class="tip-bar">
          <i class="el-icon-s-claim" />
          <template v-if="task && task.taskName">任务：{{ task.taskName }}</template>
          <template v-if="periodName"> · 期次：{{ periodName }}</template>
          <template v-if="todo && todo.nodeName"> · 当前节点：<b>{{ todo.nodeName }}</b></template>
          <template v-if="overdueTag"><span class="sep">·</span><span class="overdue-tag"><i class="el-icon-alarm-clock" /> {{ overdueTag }}</span></template>
        </section>

        <!-- 主体：左(任务信息+流程链+办理表单) + 右(操作历史) -->
        <div v-loading="loading" class="detail-body">
          <template v-if="!loading && detail">
            <div class="process-wrap">
              <div class="pd-left">
                <!-- 任务信息 -->
                <div class="info-section">
                  <div class="section-title"><i class="el-icon-document" /> 任务信息</div>
                  <div class="info-grid">
                    <div class="info-item">
                      <span class="info-label">任务名称</span>
                      <span class="info-value">{{ todo && todo.taskName }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">所属模板</span>
                      <span class="info-value">{{ todo && todo.templateName || '—' }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">当前节点</span>
                      <span class="info-value">{{ todo && todo.nodeName }}</span>
                    </div>
                    <div v-if="taskDesc" class="info-item info-item-full">
                      <span class="info-label">任务说明</span>
                      <span class="info-value">{{ taskDesc }}</span>
                    </div>
                    <div class="info-item">
                      <span class="info-label">截止时间</span>
                      <span class="info-value">
                        {{ taskEndTime || '—' }}
                        <span v-if="isTaskOverdue" class="pd-overdue"><i class="el-icon-alarm-clock" /> 已超期</span>
                        <span v-else-if="overdueFinished" class="pd-overdue"><i class="el-icon-alarm-clock" /> 超期完成</span>
                      </span>
                    </div>
                  </div>
                  <!-- 超期提示（软性标记：仅提示，仍可正常处理） -->
                  <div v-if="isTaskOverdue" class="pd-overdue-bar">
                    <i class="el-icon-warning-outline" />
                    <span>该任务已超过期次截止时间，<b>仍可正常处理</b>，提交后节点将标注「超期处理」。</span>
                  </div>
                  <!-- 任务基础信息（模板级字段，创建人下发时赋值，处理人可见） -->
                  <template v-if="templateFieldRows.length > 0">
                    <div class="tpl-header"><i class="el-icon-collection" /> 任务基础信息</div>
                    <div class="tpl-grid">
                      <div v-for="r in templateFieldRows" :key="r.id" class="tpl-item">
                        <span class="tpl-label">
                          {{ r.label }}
                          <span v-if="r.role === 2" class="tpl-handler-note"><i class="el-icon-user" /> {{ r.roleTip || '处理人填写' }}</span>
                          <span v-else class="tpl-creator-note"><i class="el-icon-s-custom" /> 创建人填写</span>
                        </span>
                        <span class="tpl-value">{{ r.value || '—' }}</span>
                      </div>
                    </div>
                  </template>
                </div>

                <!-- 完整流程链（统一组件：横/竖切换，点击节点查看表单与操作记录） -->
                <!-- 整任务已完成：完成提示紧跟流程链展示 -->
                <div v-if="isReadonly && taskFinished" class="task-done-banner">
                  <i class="el-icon-circle-check" />
                  <span>您已处理完成该任务，可查看下方流程链与操作记录</span>
                </div>
                <FlowChain :task-detail="detail" title="流程链" hint="点击节点可展开查看表单与操作记录" />

                <!-- 处理人填写的任务基础信息（fieldRole=2，不依附节点，随本节点提交；置于流程字段前填写） -->
                <div v-if="!isReadonly && handlerBaseFields.length > 0" class="form-section bd-form-section">
                  <div class="section-title">
                    <i class="el-icon-collection" /> 任务基础信息
                    <el-tooltip :content="`在「${todo && todo.nodeName}」节点由处理人填写，仅此处填写，随提交保存`" placement="top">
                      <span class="role-hint-icon role-handler"><i class="el-icon-user" /></span>
                    </el-tooltip>
                    <span class="chain-hint">绑定当前节点「{{ todo && todo.nodeName }}」，仅此处填写，随提交保存</span>
                  </div>

                  <!-- 节点填写说明（仅当本节点无动态字段时在此展示，避免与动态表单卡片重复） -->
                  <div v-if="currentFields.length === 0 && currentGuideNode && hasGuide(currentGuideNode)" class="guide-fold" :class="{ open: guideExpanded }">
                    <div class="guide-fold-head" @click="guideExpanded = !guideExpanded">
                      <i class="el-icon-info guide-fold-flag" />
                      <span class="guide-fold-title">填写说明</span>
                      <span v-if="!guideExpanded" class="guide-fold-preview">点击展开查看本节点填写要求与参考文件</span>
                      <span v-else class="guide-fold-preview">点击收回</span>
                      <i :class="guideExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" class="guide-fold-arrow" />
                    </div>
                    <div v-show="guideExpanded" class="guide-fold-body">
                      <div v-if="currentGuideNode.guideText" class="guide-text">{{ currentGuideNode.guideText }}</div>
                      <div v-if="guideFileNames(currentGuideNode).length > 0" class="guide-files">
                        <div class="guide-files-title"><i class="el-icon-paperclip" /> 说明文件<span class="chain-hint">可预览 / 下载</span></div>
                        <AttachField readonly :value="currentGuideNode.guideFiles" />
                      </div>
                    </div>
                  </div>

                  <el-form ref="baseForm" :model="handlerBaseForm" label-position="top" class="process-form">
                    <el-form-item
                      v-for="f in handlerBaseFields"
                      :key="f.id"
                      :label="f.fieldLabel"
                      :required="f.required === 1"
                    >
                      <el-input v-if="f.fieldType === 'text'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
                      <el-input v-else-if="f.fieldType === 'textarea'" v-model="handlerBaseForm[f.id]" type="textarea" :rows="3" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
                      <el-input-number v-else-if="f.fieldType === 'number'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '请输入'" controls-position="right" style="width: 100%" />
                      <el-date-picker v-else-if="f.fieldType === 'date'" v-model="handlerBaseForm[f.id]" type="date" placeholder="选择日期" value-format="yyyy-MM-dd" style="width: 100%" />
                      <el-radio-group v-else-if="f.fieldType === 'radio'" v-model="handlerBaseForm[f.id]">
                        <el-radio v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
                      </el-radio-group>
                      <el-checkbox-group v-else-if="f.fieldType === 'checkbox'" v-model="handlerBaseForm[f.id]">
                        <el-checkbox v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-checkbox>
                      </el-checkbox-group>
                      <AttachField v-else-if="f.fieldType === 'file' || f.fieldType === 'image'" v-model="handlerBaseForm[f.id]" :field-type="f.fieldType" :biz-id="bizIdFor(f)" :ref="'af_' + f.id" />
                      <el-input v-else v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || (f.fieldType === 'image' ? '请输入图片名称' : '请输入文件名称')" />
                      <div v-if="f.fieldTips" class="field-tip">{{ f.fieldTips }}</div>
                    </el-form-item>
                  </el-form>
                </div>

                <!-- 动态表单（流程节点字段）：填写说明并入本卡片，可展开/收回 -->
                <div v-if="!isReadonly && currentFields.length > 0" class="form-section">
                  <div class="section-title">
                    <span class="cur-stage-tag">当前阶段</span>{{ todo && todo.nodeName }}
                    <span v-if="todo && todo.nodeTips" class="node-tip-inline"><i class="el-icon-bell" /> {{ todo.nodeTips }}</span>
                    <span v-if="isRefill" class="refill-tag"><i class="el-icon-refresh-left" /> 已回填上次数据，可修改后重新提交</span>
                  </div>

                  <!-- 本节点处理人（可能多人；处理人填写处上方直接可见） -->
                  <div v-if="currentNodeHandlersText" class="cur-handlers">
                    <i class="el-icon-user" />
                    <span class="cur-handlers-label">本节点处理人：</span>
                    <span class="cur-handlers-val">{{ currentNodeHandlersText }}</span>
                    <span v-if="isCurrentUserHandler" class="cur-handlers-you">（含您）</span>
                  </div>

                  <!-- 节点填写说明（设计器配置，折叠面板，默认展开） -->
                  <div v-if="currentGuideNode && hasGuide(currentGuideNode)" class="guide-fold" :class="{ open: guideExpanded }">
                    <div class="guide-fold-head" @click="guideExpanded = !guideExpanded">
                      <i class="el-icon-info guide-fold-flag" />
                      <span class="guide-fold-title">填写说明</span>
                      <span v-if="!guideExpanded" class="guide-fold-preview">点击展开查看本节点填写要求与参考文件</span>
                      <span v-else class="guide-fold-preview">点击收回</span>
                      <i :class="guideExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" class="guide-fold-arrow" />
                    </div>
                    <div v-show="guideExpanded" class="guide-fold-body">
                      <div v-if="currentGuideNode.guideText" class="guide-text">{{ currentGuideNode.guideText }}</div>
                      <div v-if="guideFileNames(currentGuideNode).length > 0" class="guide-files">
                        <div class="guide-files-title"><i class="el-icon-paperclip" /> 说明文件<span class="chain-hint">可预览 / 下载</span></div>
                        <AttachField readonly :value="currentGuideNode.guideFiles" />
                      </div>
                    </div>
                  </div>

                  <el-form ref="processForm" :model="formData" label-position="top" class="process-form">
                    <el-form-item
                      v-for="f in currentFields"
                      :key="f.id"
                      :label="f.fieldLabel"
                      :required="f.required === 1"
                    >
                      <el-input v-if="f.fieldType === 'text'" v-model="formData[f.id]" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
                      <el-input v-else-if="f.fieldType === 'textarea'" v-model="formData[f.id]" type="textarea" :rows="3" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" />
                      <el-input-number v-else-if="f.fieldType === 'number'" v-model="formData[f.id]" :placeholder="f.placeholder || '请输入'" controls-position="right" style="width: 100%" />
                      <el-date-picker v-else-if="f.fieldType === 'date'" v-model="formData[f.id]" type="date" placeholder="选择日期" value-format="yyyy-MM-dd" style="width: 100%" />
                      <el-radio-group v-else-if="f.fieldType === 'radio'" v-model="formData[f.id]">
                        <el-radio v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
                      </el-radio-group>
                      <el-checkbox-group v-else-if="f.fieldType === 'checkbox'" v-model="formData[f.id]">
                        <el-checkbox v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-checkbox>
                      </el-checkbox-group>
                      <AttachField v-else-if="f.fieldType === 'file' || f.fieldType === 'image'" v-model="formData[f.id]" :field-type="f.fieldType" :biz-id="bizIdFor(f)" :ref="'af_' + f.id" />
                      <el-input v-else v-model="formData[f.id]" :placeholder="f.placeholder || (f.fieldType === 'image' ? '请输入图片名称' : '请输入文件名称')" />
                      <div v-if="f.fieldTips" class="field-tip">{{ f.fieldTips }}</div>
                    </el-form-item>
                  </el-form>
                </div>

                <div v-else-if="!isReadonly && handlerBaseFields.length === 0 && !loading" class="form-section">
                  <div class="section-title"><span class="cur-stage-tag">当前阶段</span>{{ todo && todo.nodeName }}</div>

                  <!-- 节点填写说明（折叠面板，默认展开） -->
                  <div v-if="currentGuideNode && hasGuide(currentGuideNode)" class="guide-fold" :class="{ open: guideExpanded }">
                    <div class="guide-fold-head" @click="guideExpanded = !guideExpanded">
                      <i class="el-icon-info guide-fold-flag" />
                      <span class="guide-fold-title">填写说明</span>
                      <span v-if="!guideExpanded" class="guide-fold-preview">点击展开查看本节点填写要求与参考文件</span>
                      <span v-else class="guide-fold-preview">点击收回</span>
                      <i :class="guideExpanded ? 'el-icon-arrow-up' : 'el-icon-arrow-down'" class="guide-fold-arrow" />
                    </div>
                    <div v-show="guideExpanded" class="guide-fold-body">
                      <div v-if="currentGuideNode.guideText" class="guide-text">{{ currentGuideNode.guideText }}</div>
                      <div v-if="guideFileNames(currentGuideNode).length > 0" class="guide-files">
                        <div class="guide-files-title"><i class="el-icon-paperclip" /> 说明文件<span class="chain-hint">可预览 / 下载</span></div>
                        <AttachField readonly :value="currentGuideNode.guideFiles" />
                      </div>
                    </div>
                  </div>

                  <div class="empty-form">该节点无需填写字段</div>
                </div>

                <!-- 完成节点提示（仅办理到最后一个节点时展示） -->
                <div v-if="!isReadonly && isEndNode" class="next-section">
                  <div class="section-title">完成节点</div>
                  <div class="end-tip"><i class="el-icon-success" /> 当前为结束节点，提交后任务将标记为已完成</div>
                </div>
              </div><!-- /pd-left -->

              <!-- 右侧：完整操作历史（通过/退回步骤） -->
              <div class="pd-right">
                <div class="section-title">操作历史 <span class="chain-hint">完整的通过/退回记录</span></div>
                <div v-if="allHistory.length > 0" class="history-timeline">
                  <div
                    v-for="(h, hi) in allHistory"
                    :key="hi"
                    class="tl-item"
                    :class="h.action === 1 ? 'tl-reject' : 'tl-pass'"
                  >
                    <div class="tl-dot" />
                    <div class="tl-body">
                      <div class="tl-node">{{ h.nodeName }}</div>
                      <div class="tl-head">
                        <span class="tl-badge">{{ h.action === 1 ? '退回' : '通过' }}</span>
                        <span class="tl-user"><i class="el-icon-user" /> {{ h.handlerName || '—' }}</span>
                      </div>
                      <div class="tl-time"><i class="el-icon-time" /> {{ h.handleTime || '—' }} <span v-if="nodeOverdue(h.handleTime)" class="pd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span></div>
                      <div v-if="h.action === 0 && h.passComment" class="tl-comment">通过意见：{{ h.passComment }}</div>
                      <div v-if="h.action === 1 && h.rejectReason" class="tl-reason">退回原因：{{ h.rejectReason }}</div>
                    </div>
                  </div>
                  <!-- 任务已全部完成：时间线末尾补完成节点 -->
                  <div v-if="taskFinished" class="tl-item tl-done">
                    <div class="tl-dot"><i class="el-icon-check" /></div>
                    <div class="tl-body">
                      <div class="tl-node">流程完成</div>
                      <div class="tl-head">
                        <span class="tl-badge">完成</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div v-else class="history-empty">暂无操作记录</div>
              </div>
            </div>

            <!-- 悬浮操作区（固定在右下角；页面底部留白，滚到底时不遮挡内容） -->
            <div v-if="!isReadonly" class="operate-float">
              <span class="operate-tip"><i class="el-icon-edit-outline" /> 填写完成后点击「通过并流转」提交至下一节点</span>
              <div class="operate-actions">
                <el-button :loading="drafting" @click="onDraftClick">
                  <i class="el-icon-document-add" /> 暂存
                </el-button>
                <el-button v-if="!isStartNode && processedNodes.length > 0" type="warning" :loading="submitting && isRejecting" @click="onRejectClick">
                  <i class="el-icon-back" /> 退回
                </el-button>
                <el-button type="primary" :loading="submitting && !isRejecting" @click="onPassClick">
                  <i class="el-icon-check" /> {{ isEndNode ? '提交完成' : '通过并流转' }}
                </el-button>
              </div>
            </div>
          </template>

          <div v-else-if="!loading" class="empty-state">
            <i class="el-icon-view" />
            <p>任务数据加载失败，请返回后重试</p>
          </div>
        </div>

        <!-- 悬浮操作区占位（预留同等高度，保证滚动到底时悬浮按钮不遮挡最后的内容） -->
        <div v-if="!isReadonly" class="operate-spacer" />

        <!-- 退回目标选择弹窗 -->
        <RejectTargetModal
          :visible="rejectTargetVisible"
          :processed-nodes="processedNodes"
          @confirm="onPickRejectTarget"
          @close="rejectTargetVisible = false"
        />

        <!-- 通过/退回确认弹窗（通过时在弹窗内选择下一处理人） -->
        <ConfirmActionModal
          :visible="confirmVisible"
          :action="confirmAction"
          :summary="confirmSummary"
          :end-node="isEndNode"
          :next-handler-tip="nextHandlerTip"
          :loading="submitting"
          @confirm="onConfirmSubmit"
          @close="confirmVisible = false"
        />
      </section>
    </main>
  </div>
</template>

<script>
import RejectTargetModal from './components/RejectTargetModal.vue'
import { stableBizId, formatNodeHandlers } from '@/utils'
import ConfirmActionModal from './components/ConfirmActionModal.vue'
import AttachField from '@/components/AttachField.vue'
import FlowChain from '@/components/FlowChain.vue'
import { getTaskDetail, submitTask, saveDraftTask } from '@/service/sys/TaskService'

export default {
  name: 'TaskProcessDetail',
  components: { RejectTargetModal, ConfirmActionModal, AttachField, FlowChain },
  data() {
    return {
      loading: true,
      submitting: false,
      drafting: false,
      /** 当前任务详情（TaskDetailVO） */
      detail: null,
      /** 当前待办（由详情 + 路由参数组装；todoStatus=1 表示只读） */
      todo: null,
      // 表单
      formData: {},
      /** 处理人填写的任务基础字段值（fieldRole=2） */
      handlerBaseForm: {},
      /** 节点填写说明折叠面板是否展开（默认展开） */
      guideExpanded: true,
      isRejecting: false,
      // 子弹窗
      rejectTargetVisible: false,
      confirmVisible: false,
      confirmAction: 'pass',
      confirmSummary: '',
      pendingRejectToNodeId: null,
      pendingRejectReason: null
    }
  },
  computed: {
    /** 页面标题：只读为任务详情，否则处理任务 */
    flowTitle() {
      return this.isReadonly ? '任务详情' : '处理任务'
    },
    task() {
      return this.detail && this.detail.task ? this.detail.task : null
    },
    periodName() {
      return this.$route.query.periodName || ''
    },
    currentFields() {
      return this.detail && this.detail.currentNodeFields ? this.detail.currentNodeFields : []
    },
    /** 当前处理节点的填写说明（设计器配置的节点填写说明，模板链中匹配） */
    currentGuideNode() {
      if (!this.detail || !this.todo) return null
      const tplNodes = this.detail.templateNodes || []
      const cid = this.todo.currentNodeId
      if (cid != null) {
        const hit = tplNodes.find(n => n.id === cid)
        if (hit) return hit
      }
      // 悬空/重建兜底：按节点名匹配
      return tplNodes.find(n => this.todo.nodeName && n.nodeName === this.todo.nodeName) || null
    },
    /** 处理人填写的任务基础字段配置（fieldRole=2，不依附节点） */
    handlerBaseFields() {
      return (this.detail && this.detail.handlerBaseFields) || []
    },
    isStartNode() {
      return this.todo && this.todo.nodeType === 1
    },
    isEndNode() {
      return this.todo && this.todo.nodeType === 3
    },
    /** 只读模式：该用户已处理完成（todoStatus=1），仅查看详情 */
    isReadonly() {
      return !this.todo || this.todo.todoStatus === 1
    },
    /** 任务说明（下发时填写，处理人可见；detail.task 优先，兼容待办项带说明的情况） */
    taskDesc() {
      return (this.task && this.task.taskDesc) || (this.todo && this.todo.taskDesc) || ''
    },
    /** 任务截止时间（超期软性标记判断依据） */
    taskEndTime() {
      return (this.task && this.task.endTime) || (this.todo && this.todo.endTime) || ''
    },
    /** 任务完成时间：任务已结束时取最后一个已处理节点时间，缺失时兜底任务更新时间 */
    taskFinishTime() {
      if (!this.taskFinished || !this.detail) return null
      const handled = (this.detail.taskNodes || []).filter(n => n.submitStatus === 1 && n.handleTime)
      if (handled.length === 0) return (this.task && this.task.updateTime) || null
      return handled.reduce((m, n) => (new Date(n.handleTime) > new Date(m) ? n.handleTime : m), handled[0].handleTime)
    },
    /** 办理中是否已超过截止时间（仅未结束任务软性标识：仍可正常处理，提交后节点标「超期处理」） */
    isTaskOverdue() {
      if (this.taskFinished || !this.taskEndTime) return false
      const end = new Date(String(this.taskEndTime).replace(/-/g, '/'))
      return !isNaN(end.getTime()) && new Date() > end
    },
    /** 已完成任务是否「超期完成」：完成时间晚于截止时间 */
    overdueFinished() {
      if (!this.taskFinished || !this.taskEndTime || !this.taskFinishTime) return false
      const end = new Date(String(this.taskEndTime).replace(/-/g, '/'))
      const fin = new Date(String(this.taskFinishTime).replace(/-/g, '/'))
      return !isNaN(end.getTime()) && !isNaN(fin.getTime()) && fin.getTime() > end.getTime()
    },
    /** 超期标签文案：办理中已超期 → 已超期；已完成且完成晚于截止 → 超期完成；否则空 */
    overdueTag() {
      if (this.isTaskOverdue) return '已超期'
      if (this.overdueFinished) return '超期完成'
      return ''
    },
    /** 是否回填了上次表单数据（仅「被退回后重做」场景显示：当前节点存在退回记录 action=1） */
    isRefill() {
      const tns = (this.detail && this.detail.taskNodes) || []
      const curNodeId = this.todo && this.todo.currentNodeId
      if (!curNodeId) return false
      return tns.some(n => n.nodeId === curNodeId && n.action === 1)
    },
    /** 当前节点待办处理人（可能多人：并行分支/多选，带用户号） */
    currentNodeHandlersText() {
      const tns = (this.detail && this.detail.taskNodes) || []
      const curNodeId = this.todo && this.todo.currentNodeId
      if (!curNodeId) return ''
      return formatNodeHandlers(tns.filter(n => n.nodeId === curNodeId), true)
    },
    /** 当前登录人是否也是本节点处理人之一 */
    isCurrentUserHandler() {
      const tns = (this.detail && this.detail.taskNodes) || []
      const curNodeId = this.todo && this.todo.currentNodeId
      const me = this.currentUserId
      if (!curNodeId || !me) return false
      return tns.some(n => n.nodeId === curNodeId && n.submitStatus === 0 && n.handlerUserId === me)
    },
    /** 任务基础信息（模板级字段：创建人下发的值 + 处理人在各节点填写的汇总值，处理人只读可见） */
    templateFieldRows() {
      if (!this.detail) return []
      const fields = this.detail.templateFields || []
      const data = this.detail.templateData || {}
      const hbData = this.detail.handlerBaseData || {}
      const tplNodes = this.detail.templateNodes || []
      // 处理人填写字段（fieldRole=2）：回溯实际提交该字段的任务节点，标注「哪个节点由谁填写」
      const filledNodes = (this.detail.taskNodes || []).filter(tn => tn.submitStatus === 1 && Array.isArray(tn.baseDataList))
      return fields.map(f => {
        // 处理人填写字段值来自各节点提交汇总；创建人填写字段来自下发值
        const map = f.fieldRole === 2 ? hbData : data
        const node = f.fieldRole === 2 ? tplNodes.find(n => n.id === f.bindNodeId) : null
        let srcText = null
        if (f.fieldRole === 2) {
          const hit = filledNodes.filter(tn => tn.baseDataList.some(b => String(b.fieldId) === String(f.id)))
            .sort((a, b) => (a.taskNodeId || 0) - (b.taskNodeId || 0))
          const src = hit[hit.length - 1]
          if (src) srcText = `「${src.nodeName}」节点由 ${src.handlerName}${src.handlerUserId ? ' ' + src.handlerUserId : ''} 填写`
        }
        return {
          id: f.id,
          label: f.fieldLabel,
          role: f.fieldRole === 2 ? 2 : 1,
          roleTip: f.fieldRole === 2
            ? (srcText || (node ? `在「${node.nodeName}」节点由处理人填写` : '由处理人填写'))
            : '创建人填写',
          value: map[f.id] !== undefined && map[f.id] !== null ? String(map[f.id]) : ''
        }
      })
    },
    /** 当前登录用户ID（用于按处理人定位待办节点） */
    currentUserId() {
      const ui = this.$store.getters.userInfo || {}
      return ui.id != null ? ui.id : null
    },
    /** 可退回的目标节点：当前处理人已 done 且 sortNum < 当前节点的节点（按 nodeId 去重） */
    processedNodes() {
      if (!this.detail || !this.todo) return []
      const taskNodes = this.detail.taskNodes || []
      const myUserId = this.currentUserId
      const currentSort = this.todo.currentNodeId ? this.findSortNum(this.todo.currentNodeId) : null
      const map = {}
      taskNodes.forEach(tn => {
        if (myUserId != null && tn.handlerUserId !== myUserId) return
        if (tn.submitStatus !== 1) return // 仅已处理节点
        if (tn.nodeId === this.todo.currentNodeId) return // 排除当前节点
        if (currentSort !== null && tn.sortNum !== null && tn.sortNum >= currentSort) return // 仅前置节点
        if (!map[tn.nodeId] || tn.taskNodeId > map[tn.nodeId].taskNodeId) {
          map[tn.nodeId] = tn
        }
      })
      return Object.values(map).sort((a, b) => (a.sortNum || 0) - (b.sortNum || 0))
    },
    /** 任务是否已全部完成（操作历史时间线末尾补完成节点） */
    taskFinished() {
      return this.task && this.task.status === '已结束'
    },
    /** 完整操作历史：任务全部已提交的通过/退回记录（任务绑定，按时间正序） */
    allHistory() {
      if (!this.detail) return []
      const tns = this.detail.taskNodes || []
      return tns
        // 仅真实提交记录（排除“任一完成即可”自动完成的无表单分支）
        .filter(tn => tn.submitStatus === 1 && tn.formRecordId != null)
        .sort((a, b) => (a.taskNodeId || 0) - (b.taskNodeId || 0))
    },
    /** 下一节点处理人提示（通过确认弹窗顶部提示，取自模板节点配置） */
    nextHandlerTip() {
      const gn = this.currentGuideNode
      return (gn && (gn.nextHandlerTip || gn.next_handler_tip)) || ''
    }
  },
  created() {
    this.taskId = this.$route.query.taskId || null
    this.periodName = this.$route.query.periodName || ''
    this.fetchDetail()
  },
  methods: {
    /** 附件上传的业务id：任务节点 + 字段 唯一（同一节点多个文件/图片字段互不串档） */
    bizIdFor(f) {
      const tn = this.todo && this.todo.taskNodeId
      if (!tn) return null
      // attach.biz_id 仅 varchar(32)：taskNodeId+fieldKey 超长，用稳定短码
      return stableBizId(tn + ':' + ((f && (f.fieldKey || f.id)) || 'f'))
    },
    async fetchDetail() {
      if (!this.taskId) {
        this.$router.replace('/task-process/index')
        return
      }
      this.loading = true
      try {
        const res = await getTaskDetail(this.taskId)
        this.detail = res.data
        this.buildTodo()
        this.$nextTick(() => this.initForm())
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '任务加载失败')
      } finally {
        this.loading = false
      }
    },
    /** 依据路由参数（mode=view 表示已处理回看）与当前登录人定位待办，组装 todo */
    buildTodo() {
      const d = this.detail
      if (!d) return
      const me = this.currentUserId
      const tnQuery = this.$route.query.tn || ''
      const viewMode = this.$route.query.mode === 'view'
      const tns = d.taskNodes || []
      // 定位待办节点：优先路由指定节点（列表行对应的节点），其次当前登录人的待处理节点
      let target = tnQuery ? tns.find(n => n.taskNodeId === tnQuery) : null
      if (!target) {
        target = tns.find(n => n.handlerUserId === me && n.submitStatus === 0) || null
      }
      const pending = target && target.submitStatus === 0 && (!tnQuery || target.taskNodeId === tnQuery)
      const taskName = (d.task && d.task.taskName) || ''
      const templateName = this.$route.query.templateName || ''
      if (viewMode || !pending) {
        // 只读回看：todoStatus=1（不渲染办理表单）
        const curId = d.task ? d.task.currentNodeId : null
        const tpl = d.templateNodes || []
        const tplNode = tpl.find(n => n.id === curId) || null
        const nodeName = (tplNode && tplNode.nodeName)
          || (target && target.nodeName)
          || (tns.length ? tns[tns.length - 1].nodeName : '')
        const nodeType = (tplNode && tplNode.nodeType) || (target && target.nodeType) || 2
        this.todo = {
          taskId: this.taskId,
          taskNodeId: null,
          todoStatus: 1,
          currentNodeId: (tplNode && tplNode.id) || curId,
          nodeName,
          nodeType,
          taskName,
          templateName
        }
      } else {
        // 处理中：todoStatus=0（渲染办理表单）
        this.todo = {
          taskId: this.taskId,
          taskNodeId: target.taskNodeId,
          todoStatus: 0,
          currentNodeId: target.nodeId,
          nodeName: target.nodeName,
          nodeType: target.nodeType,
          taskName,
          templateName,
          nodeTips: (target && (target.nodeTips || target.node_tips)) || ''
        }
      }
    },
    /** 节点处理时间是否超过任务截止时间（超期处理软性标记：仅标注，不影响流程） */
    nodeOverdue(timeStr) {
      if (!timeStr || !this.taskEndTime) return false
      const t = new Date(String(timeStr).replace(/-/g, '/'))
      const end = new Date(String(this.taskEndTime).replace(/-/g, '/'))
      return !isNaN(t.getTime()) && !isNaN(end.getTime()) && t > end
    },
    parseEnum(str) {
      try { return JSON.parse(str) || [] } catch (e) { return [] }
    },
    /** 节点是否有填写说明（文字或文件） */
    hasGuide(node) {
      return !!(node && (node.guideText || this.guideFileNames(node).length > 0))
    },
    /** 解析节点说明文件列表（guideFiles 为 JSON 字符串，兼容 attach 格式 {fileName}、旧格式 {name} 或纯文件名） */
    guideFileNames(node) {
      const g = node && node.guideFiles
      if (!g) return []
      try {
        const arr = JSON.parse(g)
        if (!Array.isArray(arr)) return []
        return arr.map(x => (typeof x === 'string' ? x : (x && (x.fileName || x.name)) || '')).filter(Boolean)
      } catch (e) {
        return []
      }
    },
    findSortNum(nodeId) {
      const tplNodes = (this.detail && this.detail.templateNodes) || []
      const t = tplNodes.find(t => t.id === nodeId)
      return t ? t.sortNum : null
    },
    /** 初始化表单数据（优先回填退回时的上次数据） */
    initForm() {
      if (!this.detail) return
      const fields = this.currentFields
      const data = {}
      // 先建空结构
      fields.forEach(f => {
        if (f.fieldType === 'checkbox') data[f.id] = []
        else data[f.id] = ''
      })
      // 回填上次数据（退回重填）
      const cf = this.detail.currentFormData
      if (Array.isArray(cf) && cf.length > 0) {
        cf.forEach(fd => {
          if (fd.fieldId == null) return
          const field = fields.find(f => f.id === fd.fieldId)
          if (!field) return
          if (field.fieldType === 'checkbox') {
            data[fd.fieldId] = fd.fieldValue ? String(fd.fieldValue).split(',') : []
          } else {
            data[fd.fieldId] = fd.fieldValue
          }
        })
      }
      this.formData = data
      // 处理人填写的任务基础字段：初始化 + 回填本人最近一次提交（退回重做）
      const base = {}
      this.handlerBaseFields.forEach(f => {
        if (f.fieldType === 'checkbox') base[f.id] = []
        else base[f.id] = ''
      })
      const cb = this.detail.currentBaseData || {}
      Object.keys(cb).forEach(fid => {
        const field = this.handlerBaseFields.find(f => f.id === Number(fid) || f.id === fid)
        if (!field) return
        if (field.fieldType === 'checkbox') {
          base[field.id] = cb[fid] ? String(cb[fid]).split(',') : []
        } else {
          base[field.id] = cb[fid]
        }
      })
      this.handlerBaseForm = base
      this.guideExpanded = true
      this.isRejecting = false
      this.pendingRejectToNodeId = null
      this.pendingRejectReason = null
    },
    /** 表单必填校验（通过和退回共用） */
    validateForm() {
      for (const f of this.currentFields) {
        if (f.required === 1) {
          const val = this.formData[f.id]
          const empty = (f.fieldType === 'checkbox') ? (!val || val.length === 0) : (val === null || val === undefined || val === '')
          if (empty) {
            this.$message.warning(`字段「${f.fieldLabel}」为必填项`)
            return false
          }
        }
      }
      return true
    },
    /** 处理人填写任务基础字段必填校验（退回不校验） */
    validateBaseForm() {
      for (const f of this.handlerBaseFields) {
        if (f.required === 1) {
          const val = this.handlerBaseForm[f.id]
          const empty = (f.fieldType === 'checkbox') ? (!val || val.length === 0) : (val === null || val === undefined || val === '')
          if (empty) {
            this.$message.warning(`任务基础字段「${f.fieldLabel}」为必填项`)
            return false
          }
        }
      }
      return true
    },
    /** 点击通过：校验后打开确认弹窗（弹窗内选择下一处理人）；先提醒未上传的文件 */
    async onPassClick() {
      if (!this.validateForm()) return
      if (!this.validateBaseForm()) return
      if (!(await this.confirmUnconfirmedFiles('提交'))) return
      this.isRejecting = false
      this.confirmAction = 'pass'
      this.confirmSummary = this.isEndNode ? '提交后任务将标记为已完成' : '确认后将流转至下一节点，请在弹窗中选择处理人'
      this.confirmVisible = true
    },
    /** 点击退回：选目标节点+原因，再弹确认（退回不校验表单，可不填）；先提醒未上传的文件 */
    async onRejectClick() {
      if (this.processedNodes.length === 0) {
        this.$message.warning('没有可退回的节点')
        return
      }
      if (!(await this.confirmUnconfirmedFiles('退回'))) return
      this.rejectTargetVisible = true
    },
    /** 暂存（保存草稿，不校验必填、不流转） */
    async onDraftClick() {
      if (!(await this.confirmUnconfirmedFiles('暂存'))) return
      const payload = this.buildPayload()
      payload.action = 'draft'
      this.drafting = true
      try {
        await saveDraftTask(payload)
        this.$message.success('已暂存，可随时继续填写')
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '暂存失败')
      } finally {
        this.drafting = false
      }
    },
    /** 收集所有 AttachField 中「已选择但未确认上传」的文件名，提交/退回/暂存前提醒 */
    unconfirmedFiles() {
      const names = []
      Object.keys(this.$refs).forEach(k => {
        if (!k.startsWith('af_')) return
        const insts = Array.isArray(this.$refs[k]) ? this.$refs[k] : [this.$refs[k]]
        insts.forEach(inst => {
          if (inst && inst.pendingFiles && inst.pendingFiles.length > 0) {
            inst.pendingFiles.forEach(pf => names.push(pf.name || pf.fileName))
          }
        })
      })
      return names
    },
    /** 若有未上传文件则弹确认；用户取消返回 false 中止操作 */
    async confirmUnconfirmedFiles(actionLabel) {
      const names = this.unconfirmedFiles()
      if (names.length === 0) return true
      try {
        await this.$confirm(
          `有 ${names.length} 个文件已选择但未上传：${names.join('、')}。${actionLabel}后这些文件不会被保存，是否仍要继续？`,
          '文件未上传',
          { confirmButtonText: `仍要${actionLabel}`, cancelButtonText: '先上传', type: 'warning' }
        )
        return true
      } catch (e) {
        return false
      }
    },
    onPickRejectTarget(payload) {
      const { nodeId, reason } = payload || {}
      this.rejectTargetVisible = false
      this.pendingRejectToNodeId = nodeId
      this.pendingRejectReason = reason
      const target = this.processedNodes.find(n => n.nodeId === nodeId)
      const targetName = target ? target.nodeName : '目标节点'
      this.isRejecting = true
      this.confirmAction = 'reject'
      this.confirmSummary = `将退回到节点「${targetName}」\n退回原因：${reason}\n表单将回填上次数据可修改重交`
      this.confirmVisible = true
    },
    /** 组装表单与基础字段数据（暂存/提交共用） */
    buildPayload() {
      const formDataList = this.currentFields.map(f => {
        let val = this.formData[f.id]
        if (f.fieldType === 'checkbox' && Array.isArray(val)) val = val.join(',')
        return {
          fieldId: f.id,
          fieldKey: f.fieldKey,
          fieldValue: val == null ? '' : String(val)
        }
      })
      // 处理人填写的任务基础字段（fieldRole=2）
      const baseData = {}
      this.handlerBaseFields.forEach(f => {
        let val = this.handlerBaseForm[f.id]
        if (val === undefined || val === null || val === '') return
        if (f.fieldType === 'checkbox' && Array.isArray(val)) val = val.join(',')
        baseData[f.id] = String(val)
      })
      return {
        taskId: this.todo.taskId,
        taskNodeId: this.todo.taskNodeId,
        formData: formDataList,
        baseData
      }
    },
    /** 确认提交：组装 payload 并提交 */
    async onConfirmSubmit(modalPayload) {
      const isReject = this.confirmAction === 'reject'
      const payload = this.buildPayload()
      payload.action = isReject ? 'reject' : 'pass'
      if (isReject) {
        payload.rejectToNodeId = this.pendingRejectToNodeId
        payload.rejectReason = this.pendingRejectReason
      } else {
        payload.passComment = (modalPayload && modalPayload.passComment) || ''
        if (!this.isEndNode) {
          payload.nextHandlerIds = (modalPayload && modalPayload.nextHandlerIds) || []
        }
      }
      this.submitting = true
      this.confirmVisible = false
      try {
        await submitTask(payload)
        const isEnd = this.isEndNode
        this.$message.success(isReject ? '已退回到目标节点，表单已回填上次数据' : (isEnd ? '已提交，任务已完成' : '提交成功，已流转至下一节点'))
        this.goBack()
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '提交失败')
        this.submitting = false
      }
    },
    /** 返回任务处理列表（进入列表时自动刷新） */
    goBack() {
      this.$router.push('/task-process/index')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background-color: var(--color-primary-surface); color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 14px; width: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.header-left { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.hl-row1 { display: flex; align-items: center; gap: 14px; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 0;
  .active { color: $primary; font-weight: 600; }
  .link { color: $primary; cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.btn-back { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; background: transparent; border: none; color: var(--color-primary); cursor: pointer; font-size: 13px; transition: background .2s;
  &:hover { background: var(--color-primary-light); }
}
// 上下文条
.tip-bar { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
  b { color: $primary; font-weight: 700; }
  .sep { color: #94A3B8; }
  .overdue-tag { display: inline-flex; align-items: center; gap: 3px; padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; color: #fff; background: #D97706; }
}
.empty-state { text-align: center; padding: 60px 20px; color: #bbb;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
.detail-body { min-height: 300px; }
// 左右布局
.process-wrap { display: flex; gap: 16px; align-items: flex-start; }
.pd-left { flex: 1.5; min-width: 0; display: flex; flex-direction: column; gap: 12px; }
.pd-right { flex: 1; min-width: 0; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; position: sticky; top: 16px; }
// 卡片与标题
.info-section, .form-section, .next-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 18px; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 0; }
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 32px; }
.info-item { display: flex; flex-direction: column; gap: 6px; min-width: 0;
  &.info-item-full { grid-column: 1 / -1; }
}
.info-label { font-size: 12px; color: #999; }
.info-value { font-size: 14px; color: #1b1c1c; font-weight: 500; word-break: break-all; line-height: 1.5; white-space: pre-wrap; }
// 超期软性标记
.pd-overdue { display: inline-flex; align-items: center; gap: 3px; padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 700; color: #fff; background: #D97706; vertical-align: 1px; white-space: nowrap; }
.pd-overdue-bar { display: flex; align-items: center; gap: 6px; margin-top: 12px; padding: 8px 12px; background: #FEF3C7; border: 1px dashed #D97706; border-radius: 3px; font-size: 13px; color: #64748B; line-height: 1.5;
  i { color: #D97706; font-size: 15px; }
  b { color: #D97706; font-weight: 700; }
}
.tpl-header { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: $primary; margin: 18px 0 10px; padding-top: 14px; border-top: 1px dashed $border; }
.tpl-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 32px; }
.tpl-item { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.tpl-label { font-size: 12px; color: #999; display: inline-flex; align-items: center; gap: 4px; }
.tpl-value { font-size: 14px; color: var(--color-primary); font-weight: 500; word-break: break-all; line-height: 1.5; white-space: pre-wrap; }
.tpl-handler-note { display: inline-flex; align-items: center; gap: 3px; font-size: 11px; font-weight: 400; color: var(--color-primary); background: rgba(var(--color-primary-rgb), 0.08); padding: 0 6px; border-radius: 3px; vertical-align: 1px; }
.tpl-creator-note { display: inline-flex; align-items: center; gap: 3px; font-size: 11px; font-weight: 400; color: #8a93a5; background: #F1F3F6; padding: 0 6px; border-radius: 3px; vertical-align: 1px; }
.node-tip-inline { font-size: 13px; color: #fff; background: $primary; padding: 2px 10px; border-radius: 4px; font-weight: 500;
  i { margin-right: 3px; }
}
.refill-tag { font-size: 12px; color: #B45309; background: rgba(180, 83, 9,0.1); padding: 2px 8px; border-radius: 4px; font-weight: 500; }
.cur-handlers { display: flex; align-items: center; flex-wrap: wrap; gap: 4px; padding: 7px 12px; margin-bottom: 10px; background: var(--color-primary-light); border: 1px dashed var(--color-primary); border-radius: 3px; font-size: 13px;
  i { color: $primary; font-size: 14px; }
}
.cur-handlers-label { color: #999; }
.cur-handlers-val { color: var(--color-primary); font-weight: 600; }
.cur-handlers-you { color: #B45309; font-weight: 600; }
.cur-stage-tag { font-size: 11px; color: #fff; background: $primary; padding: 2px 8px; border-radius: 4px; font-weight: 700; letter-spacing: .5px; }
.end-tip { display: flex; align-items: center; gap: 6px; padding: 12px 14px; background: #E8F5EC; border: 1px dashed #15803D; border-radius: 3px; font-size: 13px; color: #146C3A;
  i { font-size: 16px; }
}
.empty-form { text-align: center; padding: 24px 0; color: #bbb; font-size: 13px; }
.field-tip { font-size: 12px; color: #999; margin-top: 4px; line-height: 1.5; }
.role-hint-icon { width: 18px; height: 18px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; cursor: help; font-size: 12px;
  &.role-handler { background: rgba(180, 83, 9,0.12); color: #B45309; }
}
// 悬浮操作区（固定在底部居中）
.operate-float { position: fixed; left: 50%; bottom: 20px; transform: translateX(-50%); z-index: 30; display: flex; flex-direction: row; align-items: center; gap: 18px; background: #fff; border: 1px solid $border; border-radius: 6px; padding: 10px 16px; box-shadow: 0 6px 20px rgba(15, 23, 42, 0.14); max-width: calc(100vw - 40px); }
.operate-tip { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #8a93a5; line-height: 1.4; white-space: nowrap;
  i { font-size: 14px; color: $primary; }
}
.operate-actions { display: flex; gap: 8px; }
// 悬浮操作区占位：高度对齐悬浮面板，保证滚动到底时最后的内容不被遮挡
.operate-spacer { height: 88px; }
// 完成提示（整任务已完成，展示在流程链旁）
.task-done-banner { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: #E8F5EC; border: 1px solid rgba(21,128,61,0.4); border-radius: 3px; font-size: 13px; color: #146C3A; line-height: 1.5;
  i { font-size: 16px; color: #15803D; }
}
// 办理表单紧凑排版：标题与输入更近、字段之间更紧凑
.process-form ::v-deep .el-form-item { margin-bottom: 12px; }
.process-form ::v-deep .el-form-item__label { padding-bottom: 2px; line-height: 1.6; }
.process-form ::v-deep .el-form-item__content { line-height: 1; }
// 节点填写说明（设计器配置，可展开/收回）
.guide-fold { margin-bottom: 12px; border: 1px dashed rgba(180, 83, 9,0.4); border-radius: 3px; background: #EFF6FF; overflow: hidden;
  .guide-fold-head { display: flex; align-items: center; gap: 6px; padding: 10px 14px; cursor: pointer; user-select: none;
    &:hover { background: rgba(180, 83, 9,0.06); }
  }
  .guide-fold-flag { color: #B45309; font-size: 15px; }
  .guide-fold-title { font-size: 13px; font-weight: 700; color: var(--color-primary-hover); }
  .guide-fold-preview { flex: 1; min-width: 0; font-size: 12px; color: #64748B; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-left: 4px; }
  .guide-fold-arrow { margin-left: auto; color: #B45309; font-size: 13px; flex-shrink: 0; transition: transform 0.2s; }
  .guide-fold-body { padding: 0 14px 12px; }
}
.guide-text { font-size: 13px; color: var(--color-primary); line-height: 1.7; white-space: pre-wrap; word-break: break-all; background: #fff; border: 1px dashed rgba(180, 83, 9,0.3); border-radius: 2px; padding: 10px 12px; }
.guide-files { display: flex; flex-direction: column; align-items: stretch; gap: 6px; margin-top: 8px;
  .attach-field { width: 100%; }
}
.guide-files-title { font-size: 12px; color: #B45309; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; }
// 右侧操作历史时间线
.history-timeline { position: relative; padding-left: 18px;
  &::before { content: ''; position: absolute; left: 5px; top: 4px; bottom: 4px; width: 2px; background: #CBD5E1; }
}
.tl-item { position: relative; padding-bottom: 16px;
  &:last-child { padding-bottom: 0; }
}
.tl-dot { position: absolute; left: -18px; top: 4px; width: 12px; height: 12px; border-radius: 50%; border: 2px solid #fff; box-shadow: 0 0 0 1px rgba(0,0,0,0.1); }
.tl-pass .tl-dot { background: #15803D; }
.tl-reject .tl-dot { background: #B45309; }
.tl-done .tl-dot { background: #15803D; width: 16px; height: 16px; left: -20px; display: flex; align-items: center; justify-content: center; color: #fff; font-size: 10px; box-shadow: 0 0 0 1px rgba(21, 128, 61,0.4); }
.tl-body { padding: 8px 10px; border-radius: 2px; background: #fafafa; font-size: 12px; }
.tl-node { font-size: 13px; font-weight: 700; color: #1b1c1c; margin-bottom: 4px; }
.tl-head { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.tl-badge { padding: 3px 7px; border-radius: 3px; font-weight: 700; font-size: 11px; color: #fff; flex-shrink: 0; }
.tl-pass .tl-badge { background: #15803D; }
.tl-reject .tl-badge { background: #B45309; }
.tl-done .tl-badge { background: #15803D; }
.tl-done .tl-node { color: #15803D; }
.tl-user { color: #414755; i { margin-right: 2px; } }
.tl-time { color: #999; i { margin-right: 2px; } }
.tl-comment { margin-top: 5px; color: #15803D; line-height: 1.5; word-break: break-all; white-space: pre-wrap; }
.tl-reason { margin-top: 5px; color: #B45309; line-height: 1.5; word-break: break-all; white-space: pre-wrap; }
.history-empty { font-size: 13px; color: #bbb; text-align: center; padding: 32px 0; }
</style>
