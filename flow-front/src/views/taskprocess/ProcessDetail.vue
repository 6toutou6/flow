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
                <template v-if="isFromTaskLink">
                  <span class="link" @click="goTaskManage">任务管理</span>
                  <span>/</span>
                  <span class="link" @click="goBack">期次任务关联</span>
                </template>
                <span v-else class="link" @click="goBack">任务处理</span>
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
          <template v-if="templateName"> · 模板：{{ templateName }}</template>
          <template v-if="todo && todo.nodeName"> · 当前节点：<b>{{ todo.nodeName }}</b></template>
          <template v-if="taskEndTime"> · 截止：{{ taskEndTime }}</template>
          <template v-if="overdueTag"><span class="sep">·</span><span class="overdue-tag"><i class="el-icon-alarm-clock" /> {{ overdueTag }}</span></template>
        </section>

        <!-- 主体：左(任务信息+流程链+办理表单) + 右(操作历史) -->
        <div v-loading="loading" class="detail-body">
          <template v-if="!loading && detail">
            <!-- 完成节点提示（办理到最后一个节点时置顶展示，进页面即见，不用滚到底部） -->
            <div v-if="!isReadonly && isEndNode" class="top-end-tip">
              <div class="end-tip"><i class="el-icon-success" /> 当前为结束节点，提交后任务将标记为已完成</div>
            </div>
            <!-- 顶部通栏：任务说明 / 超期提示 / 任务基础信息（对齐「期次人员-流程详情」：信息横贯顶部，两栏留给流程链+表单与操作历史） -->
            <div class="pd-top">
              <!-- 任务说明（下发时填写，处理人可见） -->
              <div v-if="taskDesc" class="pd-desc">
                <span class="pd-desc-label">任务说明</span>
                <span class="pd-desc-text">{{ taskDesc }}</span>
              </div>
              <!-- 超期提示（软性标记：仅提示，仍可正常处理） -->
              <div v-if="isTaskOverdue" class="pd-overdue-bar">
                <i class="el-icon-warning-outline" />
                <span>该任务已超过期次截止时间，<b>仍可正常处理</b>，提交后节点将标注「超期处理」。</span>
              </div>
              <!-- 任务基础信息（模板级字段，创建人下发时赋值，处理人可见） -->
              <div v-if="templateFieldRows.length > 0" class="pd-tpl">
                <div class="tpl-header"><i class="el-icon-collection" /> 任务基础信息 <span class="chain-hint">创建人下发时赋值，处理人节点填写同步展示</span></div>
                <div class="tpl-grid">
                  <div v-for="r in templateFieldRows" :key="r.id" class="tpl-item" :class="{ 'tpl-wide': r.longText }">
                    <span class="tpl-label">
                      {{ r.label }}
                      <span v-if="r.role === 2" class="tpl-handler-note"><i class="el-icon-user" /> {{ r.roleTip || '处理人填写' }}</span>
                      <span v-else class="tpl-creator-note"><i class="el-icon-s-custom" /> 创建人填写</span>
                    </span>
                    <span class="tpl-value">{{ r.value || '—' }}</span>
                  </div>
                </div>
              </div>
            </div>

            <div class="process-wrap">
              <div class="pd-left">
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
                      v-show="isFieldVisible(f)"
                    >
                      <el-input v-if="f.fieldType === 'text'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" :disabled="isFieldReadonly(f)" />
                      <el-input v-else-if="f.fieldType === 'textarea'" v-model="handlerBaseForm[f.id]" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" :disabled="isFieldReadonly(f)" />
                      <el-input-number v-else-if="f.fieldType === 'number'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '请输入'" controls-position="right" style="width: 100%" :disabled="isFieldReadonly(f)" />
                      <el-date-picker v-else-if="f.fieldType === 'date'" v-model="handlerBaseForm[f.id]" type="date" placeholder="选择日期" value-format="yyyy-MM-dd" style="width: 100%" :disabled="isFieldReadonly(f)" />
                      <el-radio-group v-else-if="f.fieldType === 'radio'" v-model="handlerBaseForm[f.id]" :disabled="isFieldReadonly(f)">
                        <el-radio v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
                      </el-radio-group>
                      <el-checkbox-group v-else-if="f.fieldType === 'checkbox'" v-model="handlerBaseForm[f.id]" :disabled="isFieldReadonly(f)">
                        <el-checkbox v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-checkbox>
                      </el-checkbox-group>
                      <el-input v-else-if="f.fieldType === 'user'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '输入用户号'" :disabled="isFieldReadonly(f)" />
                      <el-input v-else-if="f.fieldType === 'dept'" v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || '输入部门'" :disabled="isFieldReadonly(f)" />
                      <AttachField v-else-if="f.fieldType === 'file' || f.fieldType === 'image'" :ref="'af_' + f.id" v-model="handlerBaseForm[f.id]" :field-type="f.fieldType" :biz-id="bizIdFor(f)" :readonly="isFieldReadonly(f)" />
                      <el-input v-else v-model="handlerBaseForm[f.id]" :placeholder="f.placeholder || (f.fieldType === 'image' ? '请输入图片名称' : '请输入文件名称')" :disabled="isFieldReadonly(f)" />
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
                      v-show="isFieldVisible(f)"
                    >
                      <el-input v-if="f.fieldType === 'text'" v-model="formData[f.id]" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" :disabled="isFieldReadonly(f)" />
                      <el-input v-else-if="f.fieldType === 'textarea'" v-model="formData[f.id]" type="textarea" :autosize="{ minRows: 2, maxRows: 6 }" :placeholder="f.placeholder || '请输入'" :maxlength="f.maxLength || undefined" :disabled="isFieldReadonly(f)" />
                      <el-input-number v-else-if="f.fieldType === 'number'" v-model="formData[f.id]" :placeholder="f.placeholder || '请输入'" controls-position="right" style="width: 100%" :disabled="isFieldReadonly(f)" />
                      <el-date-picker v-else-if="f.fieldType === 'date'" v-model="formData[f.id]" type="date" placeholder="选择日期" value-format="yyyy-MM-dd" style="width: 100%" :disabled="isFieldReadonly(f)" />
                      <el-radio-group v-else-if="f.fieldType === 'radio'" v-model="formData[f.id]" :disabled="isFieldReadonly(f)">
                        <el-radio v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-radio>
                      </el-radio-group>
                      <el-checkbox-group v-else-if="f.fieldType === 'checkbox'" v-model="formData[f.id]" :disabled="isFieldReadonly(f)">
                        <el-checkbox v-for="opt in parseEnum(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-checkbox>
                      </el-checkbox-group>
                      <el-input v-else-if="f.fieldType === 'user'" v-model="formData[f.id]" :placeholder="f.placeholder || '输入用户号'" :disabled="isFieldReadonly(f)" />
                      <el-input v-else-if="f.fieldType === 'dept'" v-model="formData[f.id]" :placeholder="f.placeholder || '输入部门'" :disabled="isFieldReadonly(f)" />
                      <AttachField v-else-if="f.fieldType === 'file' || f.fieldType === 'image'" :ref="'af_' + f.id" v-model="formData[f.id]" :field-type="f.fieldType" :biz-id="bizIdFor(f)" :readonly="isFieldReadonly(f)" />
                      <el-input v-else v-model="formData[f.id]" :placeholder="f.placeholder || (f.fieldType === 'image' ? '请输入图片名称' : '请输入文件名称')" :disabled="isFieldReadonly(f)" />
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

              </div><!-- /pd-left -->

              <!-- 右侧：关联我的任务（来源期次）+ 操作历史 -->
              <div class="pd-right">
                <div class="section-title">关联我的任务 <span class="chain-hint">我创建的任务期次关联到了本任务</span></div>
                <div v-if="linkIns.length === 0" class="history-empty">暂无任务期次关联本任务</div>
                <div v-else class="collect-list">
                  <div v-for="lk in linkIns" :key="lk.id" class="collect-card">
                    <div class="cc-head">
                      <span class="cc-name"><i class="el-icon-link" /> {{ lk.sourceDispatchName || '某任务' }}<template v-if="lk.sourcePeriodName"> / {{ lk.sourcePeriodName }}</template></span>
                    </div>
                    <div v-if="lk.remark" class="cc-remark" :title="lk.remark"><i class="el-icon-chat-line-square" /> {{ lk.remark }}</div>
                    <div class="cc-meta"><template v-if="lk.createTime">关联时间：{{ lk.createTime }}</template><template v-else>&nbsp;</template></div>
                    <div class="cc-foot">
                      <span class="cc-status cc-wait">在来源任务的期次下可查看/解除此关联</span>
                    </div>
                  </div>
                </div>

                <div class="section-title" style="margin-top: 16px">操作历史 <span class="chain-hint">完整的通过/退回记录</span></div>
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
                        <span class="tl-user"><i class="el-icon-user" /> {{ handlerText(h) }}</span>
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
                <el-button @click="transferVisible = true">
                  <i class="el-icon-sort" /> 转办
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

        <!-- 转办选人弹窗 -->
        <UserPicker
          :visible="transferVisible"
          title="选择转办人（单选）"
          @confirm="onPickTransferTarget"
          @close="transferVisible = false"
        />

        <!-- 通过/退回确认弹窗（通过时在弹窗内选择下一处理人） -->
        <ConfirmActionModal
          :visible="confirmVisible"
          :action="confirmAction"
          :summary="confirmSummary"
          :end-node="isEndNode"
          :next-handler-tip="nextHandlerTip"
          :default-next-handlers="lastChosenNextHandlers"
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
import { stableBizId, formatNodeHandlers, formatHandlerWithTransfer } from '@/utils'
import ConfirmActionModal from './components/ConfirmActionModal.vue'
import AttachField from '@/components/AttachField.vue'
import FlowChain from '@/components/FlowChain.vue'
import UserPicker from '@/components/UserPicker'
import { getTaskDetail, submitTask, saveDraftTask, transferTask } from '@/service/sys/TaskService'
import { getTaskLinksByTarget } from '@/service/sys/FlowDispatchService'
import { NODE_TYPE, evalCondAll } from '@/constants/dict'

export default {
  name: 'TaskProcessDetail',
  components: { RejectTargetModal, ConfirmActionModal, AttachField, FlowChain, UserPicker },
  data() {
    return {
      loading: true,
      submitting: false,
      drafting: false,
      /** 转办选人弹窗 */
      transferVisible: false,
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
      pendingRejectReason: null,
      // 任务关联：被哪些任务期次关联到我（反链展示）
      linkIns: []
    }
  },
  computed: {
    /** 页面标题：只读为任务详情，否则处理任务 */
    flowTitle() {
      return this.isReadonly ? '任务详情' : '处理任务'
    },
    /** 是否从「期次任务关联」页进入：决定面包屑完整路径与返回目标 */
    isFromTaskLink() {
      const back = this.$route.query.back || ''
      return back.indexOf('/flow-dispatch/task-link') >= 0
    },
    task() {
      return this.detail && this.detail.task ? this.detail.task : null
    },
    periodName() {
      return this.$route.query.periodName || ''
    },
    /** 流程模板名称（详情数据优先，兼容路由带参） */
    templateName() {
      return (this.task && this.task.templateName) ||
        (this.todo && this.todo.templateName) ||
        this.$route.query.templateName ||
        ''
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
      return this.todo && this.todo.nodeType === NODE_TYPE.START
    },
    isEndNode() {
      return this.todo && this.todo.nodeType === NODE_TYPE.END
    },
    /** 只读模式：该用户已处理完成（todoStatus=1），仅查看详情 */
    isReadonly() {
      return !this.todo || this.todo.todoStatus === 1
    },
    /** 字段联动求值上下文：当前节点字段 + 任务基础字段的 fieldKey → value */
    allFieldValues() {
      const values = {}
      const all = [...(this.currentFields || []), ...(this.handlerBaseFields || [])]
      for (const f of all) {
        if (!f.fieldKey) continue
        const v = this.formData[f.id] != null ? this.formData[f.id] : this.handlerBaseForm[f.id]
        values[f.fieldKey] = v == null ? '' : String(v)
      }
      return values
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
            .sort((a, b) => String(a.taskNodeId || '').localeCompare(String(b.taskNodeId || '')))
          const src = hit[hit.length - 1]
          if (src) srcText = `「${src.nodeName}」节点由 ${formatHandlerWithTransfer(src)} 填写`
        }
        return {
          id: f.id,
          label: f.fieldLabel,
          role: f.fieldRole === 2 ? 2 : 1,
          roleTip: f.fieldRole === 2
            ? (srcText || (node ? `在「${node.nodeName}」节点由处理人填写` : '由处理人填写'))
            : '创建人填写',
          value: map[f.id] !== undefined && map[f.id] !== null ? String(map[f.id]) : '',
          // 多行文本值过长（>60 字或含换行）时该项独占整行，避免挤在半列内显示
          longText: f.fieldType === 'textarea' && (() => {
            const v = map[f.id]
            if (v === undefined || v === null) return false
            const s = String(v)
            return s.length > 60 || s.indexOf('\n') >= 0
          })()
        }
      })
    },
    /** 可退回的目标节点：sortNum < 当前节点的已处理节点（按 nodeId 聚合，节点内全部处理人都展示，实际处理人高亮） */
    processedNodes() {
      if (!this.detail || !this.todo) return []
      const taskNodes = this.detail.taskNodes || []
      const currentSort = this.todo.currentNodeId ? this.findSortNum(this.todo.currentNodeId) : null
      // 按 nodeId 聚合（不去重用户，所有 task_node 都计入）
      const groups = {}
      taskNodes.forEach(tn => {
        if (tn.submitStatus !== 1) return // 仅已处理节点
        if (tn.nodeId === this.todo.currentNodeId) return // 排除当前节点
        if (currentSort != null && tn.sortNum != null && tn.sortNum >= currentSort) return // 仅前置节点
        if (!groups[tn.nodeId]) groups[tn.nodeId] = []
        groups[tn.nodeId].push(tn)
      })
      return Object.keys(groups)
        .map(nodeId => {
          const records = groups[nodeId]
          // 实际处理人：nextHandlerUserId/formRecordId 任一非空者为真正点击提交的人
          // 兄弟节点同步标记完成时这两个字段均不动，只有当前处理人记录保留原值
          const actualRecord = records.find(r => r.formRecordId || r.nextHandlerUserId) ||
            records.reduce((a, b) => (a.taskNodeId > b.taskNodeId ? a : b))
          const allHandlerNames = records.map(r => r.handlerName).filter(Boolean)
          return {
            nodeId,
            nodeName: records[0].nodeName,
            nodeType: records[0].nodeType,
            sortNum: records[0].sortNum,
            taskNodeId: actualRecord.taskNodeId,
            // 实际处理人（展示时高亮）
            handlerName: actualRecord.handlerName,
            handlerUserId: actualRecord.handlerUserId,
            handleTime: actualRecord.handleTime,
            // 全部处理人（按「、」连接）
            allHandlerNames,
            handlerCount: allHandlerNames.length
          }
        })
        .sort((a, b) => (a.sortNum || 0) - (b.sortNum || 0))
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
        .sort((a, b) => String(a.taskNodeId || '').localeCompare(String(b.taskNodeId || '')))
    },
    /** 下一节点处理人提示（通过确认弹窗顶部提示，取自模板节点配置） */
    nextHandlerTip() {
      const gn = this.currentGuideNode
      return (gn && (gn.nextHandlerTip || gn.next_handler_tip)) || ''
    },
    /** 退回重做后再次提交：回填本节点上次「真实通过流转」所分配的**全部**下一节点处理人。
     *  判定「真实流转」：submit=1 action=0 且带表单留痕(formRecordId)或带下一处理人(nextHandlerUserId)——
     *  排除同节点并行分支被自动标记完成、无实际流转的空记录；多条取处理时间最新。
     *  回填三档：① 后端落库全集 JSON nextHandlerIds（新提交，含多选整组）；
     *  ② 旧数据无全集 → 从该次流转实际创建的下一节点(sort+1)各分支反推当时分配的全部处理人；
     *  ③ 仍无 → 单值 nextHandlerUserId；无历史（首次流转）为空由处理人手动选择 */
    lastChosenNextHandlers() {
      if (this.isEndNode || !this.detail || !this.todo) return []
      const curId = this.todo.currentNodeId
      if (!curId) return []
      const tns = (this.detail.taskNodes) || []
      const nameOf = {}
      tns.forEach(n => {
        if (n.handlerUserId && n.handlerName) nameOf[n.handlerUserId] = n.handlerName
      })
      const merged = new Map()
      const add = (id, name) => {
        const uid = String(id || '').trim()
        if (uid && !merged.has(uid)) {
          merged.set(uid, { id: uid, userName: name || nameOf[uid] || uid, yyytId: uid })
        }
      }
      const history = tns
        .filter(n => n.nodeId === curId && n.submitStatus === 1 && n.action === 0 && (n.formRecordId || n.nextHandlerUserId))
        .sort((a, b) => String(b.handleTime || '').localeCompare(String(a.handleTime || '')))
      if (history.length === 0) return []
      const last = history[0]
      // ① 后端全集 JSON（多选整组）
      this.parseNextHandlerIds(last.nextHandlerIds).forEach(h => add(h.id, h.userName))
      // ② 旧数据无全集 → 本次流转实际创建的下游分支（下一节点）全部处理人
      if (merged.size === 0) {
        const curSort = history[0].sortNum
        tns.forEach(n => {
          if (curSort != null && n.sortNum === curSort + 1 && n.handlerUserId) {
            add(n.handlerUserId, nameOf[n.handlerUserId])
          }
        })
      }
      // ③ 单值回退
      if (merged.size === 0 && last.nextHandlerUserId) {
        add(last.nextHandlerUserId, last.nextHandlerName)
      }
      return Array.from(merged.values())
    }
  },
  created() {
    this.taskId = this.$route.query.taskId || null
    this.fetchDetail()
  },
  methods: {
    /** 处理人展示：交接过的节点显示实际经办人「原处理人 工号（现 接手人 工号）」 */
    handlerText(node, fallback) {
      return formatHandlerWithTransfer(node, fallback)
    },
    /** 解析后端落库的下一处理人全集 JSON（[{id,name}]）→ [{id,userName,yyytId}]；非法/空返回 [] */
    parseNextHandlerIds(json) {
      if (!json) return []
      try {
        const arr = JSON.parse(json)
        if (!Array.isArray(arr)) return []
        return arr.map(x => {
          const uid = (x && (x.id || x.yyytId)) || ''
          if (!uid) return null
          return { id: uid, userName: (x && (x.name || x.userName)) || uid, yyytId: uid }
        }).filter(Boolean)
      } catch (e) {
        return []
      }
    },
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
        this.loadLinkIns()
      } catch (e) {
        console.error(e)
        this.$notifyError(e, '任务加载失败')
      } finally {
        this.loading = false
      }
    },
    /** 加载「被哪些我创建的任务期次关联到本任务」（反链展示） */
    async loadLinkIns() {
      if (!this.taskId) return
      try {
        const res = await getTaskLinksByTarget(this.taskId)
        this.linkIns = (res && res.data) || []
      } catch (e) {
        console.error(e)
        this.linkIns = []
      }
    },
    /** 依据路由参数（mode=view 表示已处理回看）与当前登录人定位待办，组装 todo */
    buildTodo() {
      const d = this.detail
      if (!d) return
      const tnQuery = this.$route.query.tn || ''
      const viewMode = this.$route.query.mode === 'view'
      const tns = d.taskNodes || []
      // 定位待办节点：优先路由指定节点（列表行对应的节点），其次后端返回的当前登录人在当前节点的待办
      // （提交后停留本页刷新、无 tn 参数时据此自动定位继续办理；由后端判断，前端不做身份判断）
      let target = tnQuery ? tns.find(n => n.taskNodeId === tnQuery) : null
      if (!target && d.myPendingTaskNodeId) {
        target = tns.find(n => n.taskNodeId === d.myPendingTaskNodeId) || null
      }
      const pending = target && target.submitStatus === 0 && (!tnQuery || target.taskNodeId === tnQuery)
      const taskName = (d.task && d.task.taskName) || ''
      const templateName = this.$route.query.templateName || ''
      if (viewMode || !pending) {
        // 只读回看：todoStatus=1（不渲染办理表单）
        const curId = d.task ? d.task.currentNodeId : null
        const tpl = d.templateNodes || []
        const tplNode = tpl.find(n => n.id === curId) || null
        const nodeName = (tplNode && tplNode.nodeName) ||
          (target && target.nodeName) ||
          (tns.length ? tns[tns.length - 1].nodeName : '')
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
    /** 字段显隐：visible_when 条件不满足则隐藏 */
    isFieldVisible(f) {
      if (!f || !f.visibleWhen) return true
      return evalCondAll(f.visibleWhen, this.allFieldValues)
    },
    /** 字段只读：只读模式，或 editable_when 条件满足 */
    isFieldReadonly(f) {
      if (this.isReadonly) return true
      if (f && f.editableWhen) return evalCondAll(f.editableWhen, this.allFieldValues)
      return false
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
        const res = await saveDraftTask(payload)
        if (!res || res.code !== 200) return
        this.$message.success('已暂存，可随时继续填写')
      } catch (e) {
        console.error(e)
        this.$notifyError(e, '暂存失败')
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
    /** 转办：把当前待办转给选中人（单选取第一个） */
    async onPickTransferTarget(users) {
      this.transferVisible = false
      const target = users && users[0]
      if (!target || !target.yyytId) return
      try {
        await this.$confirm(`确认将待办转办给「${target.userName}」？`, '转办确认', { type: 'warning' })
        const res = await transferTask({ taskNodeId: this.todo.taskNodeId, targetUserId: target.yyytId })
        if (!res || res.code !== 200) return
        this.$message.success('转办成功')
        this.$router.replace('/task-process/index')
      } catch (e) {
        if (e !== 'cancel') this.$notifyError(e, '转办失败')
      }
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
        const res = await submitTask(payload)
        if (!res || res.code !== 200) return
        const isEnd = this.isEndNode
        let msg
        if (isReject) {
          msg = '已退回到目标节点，表单已回填上次数据'
        } else if (isEnd) {
          msg = '已提交，任务已完成'
        } else {
          msg = '提交成功，已流转至下一节点'
        }
        this.$message.success(msg)
        // 提交成功：停留本页，清除 tn/mode 后重新加载最新状态——
        // 本人仍在本任务有新的待办（流转至本人下游 / 退回到本人重做）则自动继续办理，否则转为只读回看，不再跳回列表
        // 注意：当前 vue-router 的 replace 不返回 Promise，用 onComplete 回调保证路由已更新后再拉详情
        const q = { taskId: this.taskId }
        if (this.$route.query.taskName) q.taskName = this.$route.query.taskName
        if (this.$route.query.templateName) q.templateName = this.$route.query.templateName
        if (this.$route.query.periodName) q.periodName = this.$route.query.periodName
        // 保留来源页，提交后停留本页时返回仍回到进入前的页面
        if (this.$route.query.back) q.back = this.$route.query.back
        // 停留本页重新加载最新流程/表单状态；先复位按钮 loading（若本人仍有新待办将续显示办理表单，按钮须恢复可点）
        this.submitting = false
        this.$router.replace({ path: '/task-process/detail', query: q }, () => this.fetchDetail())
      } catch (e) {
        console.error(e)
        this.$notifyError(e, '提交失败')
        this.submitting = false
      }
    },
    /** 返回来源页：路由带 back（从期次任务关联等页面进入）则回原页，否则回任务处理列表 */
    goBack() {
      let back = this.$route.query.back
      if (back) {
        // 兼容 hash 模式可能带上的 # 前缀，避免被当成仅 hash 变化而不跳转
        if (back.charAt(0) === '#') back = back.slice(1)
        this.$router.push(back)
        return
      }
      this.$router.push('/task-process/index')
    },
    /** 面包屑「任务管理」：回到任务管理列表（从期次任务关联页进入时的上级） */
    goTaskManage() {
      this.$router.push('/flow-dispatch/index')
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
// 顶部通栏：任务说明 / 超期提示 / 任务基础信息（对齐「期次人员-流程详情」页顶部横条结构）
.pd-top { display: flex; flex-direction: column; gap: 12px; }
.pd-top + .process-wrap { margin-top: 16px; }
.pd-desc { display: flex; gap: 12px; align-items: flex-start; padding: 10px 14px; background: var(--color-primary-light); border: 1px dashed rgba(var(--color-primary-rgb), 0.4); border-radius: 3px; font-size: 13px;
  .pd-desc-label { width: 90px; color: #757575; flex-shrink: 0; line-height: 1.6; }
  .pd-desc-text { flex: 1; color: var(--color-primary); line-height: 1.6; white-space: pre-wrap; word-break: break-all; }
}
.pd-tpl { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 18px; }
// 左右布局：左(流程链+办理表单)宽 3 / 右(操作历史)窄 1，参考期次人员-流程详情
.process-wrap { display: flex; gap: 16px; align-items: flex-start; }
.pd-left { flex: 3; min-width: 0; display: flex; flex-direction: column; gap: 12px; }
.pd-right { flex: 1; min-width: 0; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; }
// 卡片与标题
.form-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px 18px; }
// 结束节点提示：置顶展示（与下方通栏信息保持间距）
.top-end-tip { margin-bottom: 14px; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 12px; display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 0; }
// 超期软性标记
.pd-overdue { display: inline-flex; align-items: center; gap: 3px; padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 700; color: #fff; background: #D97706; vertical-align: 1px; white-space: nowrap; }
.pd-overdue-bar { display: flex; align-items: center; gap: 6px; padding: 8px 12px; background: #FEF3C7; border: 1px dashed #D97706; border-radius: 3px; font-size: 13px; color: #64748B; line-height: 1.5;
  i { color: #D97706; font-size: 15px; }
  b { color: #D97706; font-weight: 700; }
}
.tpl-header { display: flex; align-items: center; gap: 6px; font-size: 13px; font-weight: 700; color: $primary; margin: 0 0 12px; }
.tpl-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px 32px; }
.tpl-item { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.tpl-item.tpl-wide { grid-column: 1 / -1; }
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
// 任务关联（被哪些期次关联到我）反链卡片
.collect-list { display: flex; flex-direction: column; gap: 10px; }
.collect-card { border: 1px solid #e4e7ed; border-radius: 4px; padding: 10px 12px;
  &.collect-done { border-color: rgba(21, 128, 61, 0.4); }
}
.cc-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; }
.cc-name { font-size: 13px; font-weight: 700; color: #1b1c1c; display: inline-flex; align-items: center; gap: 4px;
  i { color: $primary; }
}
.cc-meta { font-size: 12px; color: #8a93a5; margin-top: 4px; }
.cc-remark { font-size: 12px; color: #6b7280; background: #f7f8fa; border-left: 2px solid rgba(var(--color-primary-rgb), 0.35); border-radius: 2px; padding: 4px 8px; margin-top: 6px; line-height: 1.5;
  i { color: var(--color-primary); margin-right: 3px; }
}
.cc-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 8px; }
.cc-status { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600;
  &.cc-ok { background: rgba(21, 128, 61, 0.12); color: #15803D; }
  &.cc-wait { background: rgba(180, 83, 9, 0.12); color: #B45309; }
}
</style>
