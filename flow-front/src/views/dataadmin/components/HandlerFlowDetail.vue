<template>
  <div class="flow-detail-wrap">
    <!-- 任务说明（下发时填写，管理员可见） -->
    <div v-if="taskDesc" class="hfd-desc">
      <span class="hfd-desc-label">任务说明</span>
      <span class="hfd-desc-text">{{ taskDesc }}</span>
    </div>
    <!-- 任务基础信息（模板级字段，创建人下发时赋值；处理人各节点填写同步汇总展示） -->
    <div v-if="templateFieldRows.length > 0" class="hfd-tpl">
      <div class="section-title">任务基础信息 <span class="chain-hint">创建人下发时赋值，处理人节点填写同步展示</span></div>
      <div class="tpl-grid">
        <div v-for="r in templateFieldRows" :key="r.id" class="tpl-item">
          <span class="tpl-label">
            {{ r.label }}
            <el-tooltip v-if="r.roleTip" :content="r.roleTip" placement="top">
              <span class="role-hint-icon" :class="r.role === 2 ? 'role-handler' : 'role-creator'">
                <i :class="r.role === 2 ? 'el-icon-user' : 'el-icon-s-custom'" />
              </span>
            </el-tooltip>
          </span>
          <span class="tpl-value">{{ r.value || '—' }}</span>
        </div>
      </div>
    </div>
    <div class="hfd-cols">
    <!-- 左侧：完整流程链（去重：开始到当前节点，不显示退回重复步骤；支持横向/竖向切换，默认横向） -->
    <div v-if="flowChain.length > 0" class="chain-section hfd-left">
      <div class="section-title">
        流程链 <span class="chain-hint">仅显示开始到当前节点；点击节点可展开查看表单与操作记录</span>
        <span class="orient-toggle">
          <span class="orient-btn" :class="{ active: chainOrientation === 'horizontal' }" @click="chainOrientation = 'horizontal'"><i class="el-icon-s-fold" /> 横向</span>
          <span class="orient-btn" :class="{ active: chainOrientation === 'vertical' }" @click="chainOrientation = 'vertical'"><i class="el-icon-s-unfold" /> 竖向</span>
        </span>
      </div>

      <!-- 横向模式：全部节点 chip 轨道，点击已处理节点展开单个节点详情（同时只展开一个） -->
      <template v-if="chainOrientation === 'horizontal'">
        <div class="chain-track-h">
          <template v-for="(item, idx) in flowChain">
            <div
              :key="idx"
              class="chain-chip"
              :class="['chip-' + item.status, { clickable: item.status === 'done', expanded: hExpandedNodeId === item.nodeId, mine: item.isMine }]"
              :title="`${idx + 1}. ${item.nodeName}（${statusLabel(item.status)}）`"
              @click="toggleNodeForm(item)"
            >
              <span class="cc-no">{{ idx + 1 }}</span>
              <span class="cc-name">{{ item.nodeName }}</span>
              <span v-if="item.status === 'current'" class="cc-now">当前</span>
              <span v-if="item.latestDone" class="cc-done-tag"><i class="el-icon-success" /></span>
            </div>
            <span v-if="idx < flowChain.length - 1" :key="'arr-' + idx" class="chain-arrow"><i class="el-icon-right" /></span>
          </template>
        </div>
        <!-- 横向模式下选中节点的详情（点击已处理节点展开，同时只展开一个） -->
        <div v-if="hExpandedItem" class="step-detail" @click.stop>
          <div class="sd-head">
            <span class="sd-no">{{ hExpandedIndex + 1 }}</span>
            <span class="sd-name">{{ hExpandedItem.nodeName }}</span>
            <span class="step-badge" :class="'badge-' + hExpandedItem.status">{{ statusLabel(hExpandedItem.status) }}</span>
            <span v-if="hExpandedItem.status === 'current'" class="cur-stage-tag">当前阶段</span>
            <span v-if="hExpandedItem.latestDone" class="sd-meta"><i class="el-icon-user" /> {{ hExpandedItem.latestDone.handlerName || '—' }}</span>
            <span v-if="hExpandedItem.latestDone" class="sd-meta"><i class="el-icon-time" /> {{ hExpandedItem.latestDone.handleTime || '—' }}</span>
            <span v-if="hExpandedItem.latestDone && nodeOverdue(hExpandedItem.latestDone.handleTime)" class="sd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span>
            <span v-if="hExpandedItem.rejectReason" class="meta-reason" :title="hExpandedItem.rejectReason"><i class="el-icon-warning-outline" /> 退回建议：{{ hExpandedItem.rejectReason }}</span>
          </div>
          <div class="step-expanded">
            <div class="expanded-left">
              <!-- 该节点填写说明（设计器配置，可展开/收回） -->
              <div v-if="hasGuide(hExpandedItem)" class="guide-fold" :class="{ open: !isGuideFolded(hExpandedItem.nodeId) }">
                <div class="guide-fold-head" @click="toggleGuideFold(hExpandedItem.nodeId)">
                  <i class="el-icon-info guide-fold-flag" />
                  <span class="guide-fold-title">填写说明</span>
                  <span v-if="isGuideFolded(hExpandedItem.nodeId)" class="guide-fold-preview">点击展开查看本节点填写要求与参考文件</span>
                  <span v-else class="guide-fold-preview">点击收回</span>
                  <i :class="isGuideFolded(hExpandedItem.nodeId) ? 'el-icon-arrow-down' : 'el-icon-arrow-up'" class="guide-fold-arrow" />
                </div>
                <div v-show="!isGuideFolded(hExpandedItem.nodeId)" class="guide-fold-body">
                  <div v-if="hExpandedItem.guideText" class="guide-text">{{ hExpandedItem.guideText }}</div>
                  <div v-if="guideFileNames(hExpandedItem).length > 0" class="guide-files">
                    <div class="guide-files-title"><i class="el-icon-paperclip" /> 说明文件<span class="chain-hint">可预览 / 下载</span></div>
                    <AttachField readonly :value="hExpandedItem.guideFiles" />
                  </div>
                </div>
              </div>
              <div class="expanded-sub-title">表单数据（最近一次提交）</div>
              <div v-if="hExpandedItem.latestDone && hExpandedItem.latestDone.formDataList && hExpandedItem.latestDone.formDataList.length > 0" class="form-rows">
                <div v-for="(fd, fi) in hExpandedItem.latestDone.formDataList" :key="fi" class="form-row">
                  <span class="fr-label">{{ fd.fieldLabel }}</span>
                  <AttachField v-if="fd.fieldType === 'file' || fd.fieldType === 'image'" :value="fd.fieldValue" :field-type="fd.fieldType" readonly class="fr-value" />
                  <span v-else class="fr-value">{{ fd.fieldValue || '—' }}</span>
                </div>
              </div>
              <div v-else class="form-empty">该节点未填写表单数据</div>
              <!-- 该节点处理人填写的任务基础字段（fieldRole=2） -->
              <div v-if="hExpandedItem.latestDone && hExpandedItem.latestDone.baseDataList && hExpandedItem.latestDone.baseDataList.length > 0" class="bd-block">
                <div class="expanded-sub-title bd-sub-title">
                  任务基础信息
                  <el-tooltip :content="`在「${hExpandedItem.nodeName}」节点由处理人填写`" placement="top">
                    <span class="role-hint-icon role-handler"><i class="el-icon-user" /></span>
                  </el-tooltip>
                </div>
                <div v-for="(bd, bi) in hExpandedItem.latestDone.baseDataList" :key="bi" class="form-row">
                  <span class="fr-label">{{ bd.fieldLabel }}</span>
                  <AttachField v-if="bd.fieldType === 'file' || bd.fieldType === 'image'" :value="bd.fieldValue" :field-type="bd.fieldType" readonly class="fr-value" />
                  <span v-else class="fr-value">{{ bd.fieldValue || '—' }}</span>
                </div>
              </div>
            </div>
            <div class="expanded-right">
              <div class="expanded-sub-title">操作记录</div>
              <div v-if="hExpandedItem.actionHistory.length > 0" class="action-list">
                <div v-for="(act, ai) in hExpandedItem.actionHistory" :key="ai" class="action-item" :class="act.action === 1 ? 'act-reject' : 'act-pass'">
                  <div class="action-head">
                    <span class="action-badge">{{ act.action === 1 ? '退回' : '通过' }}</span>
                    <span class="action-user"><i class="el-icon-user" /> {{ act.handlerName || '—' }}</span>
                    <span class="action-time"><i class="el-icon-time" /> {{ act.handleTime || '—' }}</span>
                    <span v-if="nodeOverdue(act.handleTime)" class="sd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span>
                  </div>
                  <div v-if="act.action === 0 && act.passComment" class="action-comment">意见：{{ act.passComment }}</div>
                  <div v-if="act.action === 1 && act.rejectReason" class="action-reason">原因：{{ act.rejectReason }}</div>
                </div>
              </div>
              <div v-else class="form-empty">暂无操作记录</div>
            </div>
          </div>
        </div>
      </template>

      <!-- 竖向模式：原有卡片式流程链 -->
      <div v-else class="chain-track">
        <div
          v-for="(item, idx) in flowChain"
          :key="idx"
          class="chain-step"
          :class="['st-' + item.status, { clickable: item.status === 'done', expanded: expandedNodeIds.includes(item.nodeId), mine: item.isMine }]"
          @click="toggleNodeForm(item)"
        >
          <div class="step-head">
            <span class="step-no">{{ idx + 1 }}</span>
            <span class="step-name">{{ item.nodeName }}</span>
            <span class="step-badge" :class="'badge-' + item.status">{{ statusLabel(item.status) }}</span>
            <span v-if="item.status === 'current'" class="cur-stage-tag">当前阶段</span>
            <span v-if="item.latestDone" class="mine-tag">{{ item.latestDone.handlerName || '该人员' }}已处理</span>
            <span v-if="item.branchCount > 1" class="branch-tag">{{ item.branchCount }} 分支</span>
          </div>
          <div class="step-meta">
            <template v-if="item.latestDone">
              <span><i class="el-icon-user" /> {{ item.latestDone.handlerName || '—' }}</span>
              <span><i class="el-icon-time" /> {{ item.latestDone.handleTime || '—' }}</span>
              <span v-if="nodeOverdue(item.latestDone.handleTime)" class="sd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span>
            </template>
            <template v-else-if="item.hasPending">
              <span><i class="el-icon-user" /> {{ item.pendingHandlerNames || '待处理' }}</span>
            </template>
            <span v-if="item.rejectReason" class="step-reject-reason"><i class="el-icon-warning-outline" /> 退回建议：{{ item.rejectReason }}</span>
          </div>
          <!-- 展开内容：左表单 + 右操作历史 -->
          <div v-if="item.status === 'done' && expandedNodeIds.includes(item.nodeId)" class="step-expanded" @click.stop>
            <div class="expanded-left">
              <!-- 该节点填写说明（设计器配置，可展开/收回） -->
              <div v-if="hasGuide(item)" class="guide-fold" :class="{ open: !isGuideFolded(item.nodeId) }">
                <div class="guide-fold-head" @click="toggleGuideFold(item.nodeId)">
                  <i class="el-icon-info guide-fold-flag" />
                  <span class="guide-fold-title">填写说明</span>
                  <span v-if="isGuideFolded(item.nodeId)" class="guide-fold-preview">点击展开查看本节点填写要求与参考文件</span>
                  <span v-else class="guide-fold-preview">点击收回</span>
                  <i :class="isGuideFolded(item.nodeId) ? 'el-icon-arrow-down' : 'el-icon-arrow-up'" class="guide-fold-arrow" />
                </div>
                <div v-show="!isGuideFolded(item.nodeId)" class="guide-fold-body">
                  <div v-if="item.guideText" class="guide-text">{{ item.guideText }}</div>
                  <div v-if="guideFileNames(item).length > 0" class="guide-files">
                    <div class="guide-files-title"><i class="el-icon-paperclip" /> 说明文件<span class="chain-hint">可预览 / 下载</span></div>
                    <AttachField readonly :value="item.guideFiles" />
                  </div>
                </div>
              </div>
              <div class="expanded-sub-title">表单数据（最近一次提交）</div>
              <div v-if="item.latestDone && item.latestDone.formDataList && item.latestDone.formDataList.length > 0" class="form-rows">
                <div v-for="(fd, fi) in item.latestDone.formDataList" :key="fi" class="form-row">
                  <span class="fr-label">{{ fd.fieldLabel }}</span>
                  <AttachField v-if="fd.fieldType === 'file' || fd.fieldType === 'image'" :value="fd.fieldValue" :field-type="fd.fieldType" readonly class="fr-value" />
                  <span v-else class="fr-value">{{ fd.fieldValue || '—' }}</span>
                </div>
              </div>
              <div v-else class="form-empty">该节点未填写表单数据</div>
              <!-- 该节点处理人填写的任务基础字段（fieldRole=2） -->
              <div v-if="item.latestDone && item.latestDone.baseDataList && item.latestDone.baseDataList.length > 0" class="bd-block">
                <div class="expanded-sub-title bd-sub-title">
                  任务基础信息
                  <el-tooltip :content="`在「${item.nodeName}」节点由处理人填写`" placement="top">
                    <span class="role-hint-icon role-handler"><i class="el-icon-user" /></span>
                  </el-tooltip>
                </div>
                <div v-for="(bd, bi) in item.latestDone.baseDataList" :key="bi" class="form-row">
                  <span class="fr-label">{{ bd.fieldLabel }}</span>
                  <AttachField v-if="bd.fieldType === 'file' || bd.fieldType === 'image'" :value="bd.fieldValue" :field-type="bd.fieldType" readonly class="fr-value" />
                  <span v-else class="fr-value">{{ bd.fieldValue || '—' }}</span>
                </div>
              </div>
            </div>
            <div class="expanded-right">
              <div class="expanded-sub-title">操作记录</div>
              <div v-if="item.actionHistory.length > 0" class="action-list">
                <div v-for="(act, ai) in item.actionHistory" :key="ai" class="action-item" :class="act.action === 1 ? 'act-reject' : 'act-pass'">
                  <div class="action-head">
                    <span class="action-badge">{{ act.action === 1 ? '退回' : '通过' }}</span>
                    <span class="action-user"><i class="el-icon-user" /> {{ act.handlerName || '—' }}</span>
                    <span class="action-time"><i class="el-icon-time" /> {{ act.handleTime || '—' }}</span>
                    <span v-if="nodeOverdue(act.handleTime)" class="sd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span>
                  </div>
                  <div v-if="act.action === 0 && act.passComment" class="action-comment">意见：{{ act.passComment }}</div>
                  <div v-if="act.action === 1 && act.rejectReason" class="action-reason">原因：{{ act.rejectReason }}</div>
                </div>
              </div>
              <div v-else class="form-empty">暂无操作记录</div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div v-else class="empty-state">
      <i class="el-icon-set-up" />
      <p>暂无流程数据</p>
    </div>

    <!-- 右侧：完整操作历史（通过/退回步骤） -->
    <div class="hfd-right">
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
            <div class="tl-time"><i class="el-icon-time" /> {{ h.handleTime || '—' }} <span v-if="nodeOverdue(h.handleTime)" class="sd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span></div>
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
  </div>
</template>

<script>
import AttachField from '@/components/AttachField.vue'

export default {
  name: 'HandlerFlowDetail',
  components: { AttachField },
  props: {
    /** TaskDetailVO（含 templateNodes + taskNodes） */
    taskDetail: { type: Object, default: null },
    /** 当前选中处理人（用于高亮该人员处理的节点） */
    selectedHandler: { type: Object, default: null }
  },
  data() {
    return {
      /** 流程链展示方向：默认横向，用户可切换为竖向 */
      chainOrientation: 'horizontal',
      /** 竖向模式：已展开的节点 id 集合（竖向可同时展开多个节点详情） */
      expandedNodeIds: [],
      /** 已处理节点中「填写说明」被收起的节点 id 集合（默认全部展开） */
      foldedGuideNodeIds: [],
      /** 横向模式：当前展开详情的节点 id（横向同时只展开一个，点击其他已处理节点会切换） */
      hExpandedNodeId: null
    }
  },
  computed: {
    /** 任务说明（下发时填写） */
    taskDesc() {
      return (this.taskDetail && this.taskDetail.task && this.taskDetail.task.taskDesc) || ''
    },
    /** 任务基础信息（模板级字段：创建人下发的值 + 处理人在各节点填写的汇总值，后台只读可见） */
    templateFieldRows() {
      if (!this.taskDetail) return []
      const fields = this.taskDetail.templateFields || []
      const data = this.taskDetail.templateData || {}
      const hbData = this.taskDetail.handlerBaseData || {}
      const tplNodes = this.taskDetail.templateNodes || []
      return fields.map(f => {
        // 处理人填写字段（fieldRole=2）值来自各节点提交汇总；创建人填写字段来自下发值
        const map = f.fieldRole === 2 ? hbData : data
        const node = f.fieldRole === 2 ? tplNodes.find(n => n.id === f.bindNodeId) : null
        return {
          id: f.id,
          label: f.fieldLabel,
          role: f.fieldRole === 2 ? 2 : 1,
          roleTip: f.fieldRole === 2
            ? (node ? `在「${node.nodeName}」节点由处理人填写` : '由处理人填写')
            : '创建人填写',
          value: map[f.id] !== undefined && map[f.id] !== null ? String(map[f.id]) : ''
        }
      })
    },
    /** 流程链：展示该任务从开始到当前节点的完整流转；节点有多个处理人时全部展示（高亮选中成员） */
    flowChain() {
      if (!this.taskDetail) return []
      const tplNodes = this.taskDetail.templateNodes || []
      const taskNodes = this.taskDetail.taskNodes || []
      const handlerId = this.selectedHandler ? this.selectedHandler.id : null
      // 全部 task_node 按 nodeId 分组（不按人过滤，节点多处理人时都能看到）
      const byNode = {}
      taskNodes.forEach(tn => {
        if (!byNode[tn.nodeId]) byNode[tn.nodeId] = []
        byNode[tn.nodeId].push(tn)
      })
      // 任务走到的最远节点（决定流程链显示到哪）
      let maxSort = -Infinity
      taskNodes.forEach(tn => { if (tn.sortNum != null && tn.sortNum > maxSort) maxSort = tn.sortNum })
      if (maxSort === -Infinity) return []
      return tplNodes
        .filter(tpl => tpl.sortNum != null && tpl.sortNum <= maxSort)
        .map(tpl => {
          const nodes = byNode[tpl.id] || []
          const pendingNodes = nodes.filter(tn => tn.submitStatus === 0)
          const doneNodes = nodes.filter(tn => tn.submitStatus === 1)
          const hasPending = pendingNodes.length > 0
          // 退回重做判定：节点存在晚于“最近一次已处理记录”的待办 → 被退回重做，状态为处理中
          const latestHandledId = doneNodes.reduce((m, tn) => Math.max(m, tn.taskNodeId || 0), 0)
          const newerPending = pendingNodes.some(tn => (tn.taskNodeId || 0) > latestHandledId)
          // 表单数据：优先“有真实提交”的节点（有 formRecordId），
          // 自动完成的分支（任一处理人完成即可时其余分支被标记完成、无表单）不作为完成人展示
          // 退回重做中不展示旧表单
          const submittedDone = doneNodes.filter(tn => tn.formRecordId != null)
          const latestDone = newerPending ? null
            : (submittedDone.length > 0
              ? submittedDone.reduce((a, b) => (a.taskNodeId > b.taskNodeId ? a : b))
              : (doneNodes.length > 0 ? doneNodes.reduce((a, b) => (a.taskNodeId > b.taskNodeId ? a : b)) : null))
          let status = 'pending'
          if (newerPending) status = 'current'
          else if (doneNodes.length > 0) status = (latestDone && latestDone.action === 1) ? 'rejected' : 'done'
          else if (pendingNodes.length > 0) status = 'current'
          // 操作历史：该节点真实提交记录（排除“任一完成即可”自动完成的分支，按时间正序）
          const actionHistory = doneNodes
            .filter(tn => tn.formRecordId != null)
            .slice().sort((a, b) => (a.taskNodeId || 0) - (b.taskNodeId || 0))
            .map(tn => ({
              action: tn.action,
              handlerName: tn.handlerName,
              handleTime: tn.handleTime,
              rejectReason: tn.rejectReason,
              passComment: tn.passComment
            }))
          // 待处理人名（同一节点多处理人全部展示）
          const pendingHandlerNames = pendingNodes
            .map(tn => tn.handlerName)
            .filter(Boolean)
            .filter((v, i, arr) => arr.indexOf(v) === i)
            .join('、')
          // 退回建议：优先取重做待办携带的（退回时写入），其次取该节点历史已退回记录
          const rejectReason = (pendingNodes.find(n => n.rejectReason) || doneNodes.find(n => n.action === 1 && n.rejectReason) || {}).rejectReason
          return {
            nodeId: tpl.id,
            nodeName: tpl.nodeName,
            nodeType: tpl.nodeType,
            sortNum: tpl.sortNum,
            isMine: handlerId ? nodes.some(tn => tn.handlerUserId === handlerId) : true,
            status,
            hasPending,
            latestDone,
            pendingHandlerNames,
            rejectReason,
            branchCount: nodes.length,
            actionHistory,
            guideText: tpl.guideText,
            guideFiles: tpl.guideFiles
          }
        })
    },
    /** 任务是否已全部完成（操作历史时间线末尾补完成节点） */
    taskFinished() {
      return this.taskDetail && this.taskDetail.task && this.taskDetail.task.status === 2
    },
    /** 横向模式：当前展开详情的节点（同时只展开一个） */
    hExpandedItem() {
      if (!this.hExpandedNodeId) return null
      return this.flowChain.find(i => i.nodeId === this.hExpandedNodeId) || null
    },
    /** 横向模式：当前展开节点在流程链中的序号 */
    hExpandedIndex() {
      const idx = this.flowChain.findIndex(i => i.nodeId === this.hExpandedNodeId)
      return idx >= 0 ? idx : 0
    },
    /** 任务截止时间（超期标记判断依据） */
    taskEndTime() {
      return (this.taskDetail && this.taskDetail.task && this.taskDetail.task.endTime) || ''
    },
    /** 完整操作历史：任务的全部操作节点（不区分提交人员，每条显示提交人员），按时间正序 */
    allHistory() {
      if (!this.taskDetail) return []
      const tns = this.taskDetail.taskNodes || []
      return tns
        // 仅真实提交记录（排除“任一完成即可”自动完成的无表单分支）
        .filter(tn => tn.submitStatus === 1 && tn.formRecordId != null)
        .sort((a, b) => (a.taskNodeId || 0) - (b.taskNodeId || 0))
    }
  },
  watch: {
    taskDetail() {
      this.expandedNodeIds = []
      this.foldedGuideNodeIds = []
      this.hExpandedNodeId = null
    }
  },
  methods: {
    statusLabel(s) { return { done: '已通过', current: '处理中', rejected: '已退回', pending: '未到' }[s] || '未到' },
    /** 切换节点详情展开：横向同时只展开一个；竖向可同时展开多个 */
    toggleNodeForm(item) {
      if (item.status !== 'done') return
      if (this.chainOrientation === 'horizontal') {
        this.hExpandedNodeId = this.hExpandedNodeId === item.nodeId ? null : item.nodeId
      } else {
        const idx = this.expandedNodeIds.indexOf(item.nodeId)
        if (idx >= 0) this.expandedNodeIds.splice(idx, 1)
        else this.expandedNodeIds.push(item.nodeId)
      }
    },
    /** 节点处理时间是否超过任务截止时间（超期处理软性标记：仍提交，仅标注） */
    nodeOverdue(timeStr) {
      if (!timeStr || !this.taskEndTime) return false
      const t = new Date(String(timeStr).replace(/-/g, '/'))
      const end = new Date(String(this.taskEndTime).replace(/-/g, '/'))
      return !isNaN(t.getTime()) && !isNaN(end.getTime()) && t > end
    },
    /** 节点是否有填写说明（文字或文件） */
    hasGuide(node) {
      return !!(node && (node.guideText || this.guideFileNames(node).length > 0))
    },
    /** 填写说明是否被收起 */
    isGuideFolded(nodeId) {
      return this.foldedGuideNodeIds.includes(nodeId)
    },
    /** 切换填写说明的展开/收回 */
    toggleGuideFold(nodeId) {
      const idx = this.foldedGuideNodeIds.indexOf(nodeId)
      if (idx >= 0) this.foldedGuideNodeIds.splice(idx, 1)
      else this.foldedGuideNodeIds.push(nodeId)
    },
    /** 解析节点说明文件列表（guideFiles 兼容 JSON 字符串 / 数组 / 纯文件名） */
    guideFileNames(node) {
      const g = node && node.guideFiles
      if (!g) return []
      const arr = Array.isArray(g) ? g : (() => {
        if (typeof g !== 'string') return []
        const t = g.trim()
        if (!t || !t.startsWith('[')) return t ? [t] : []
        try { const p = JSON.parse(t); return Array.isArray(p) ? p : [] } catch (e) { return [] }
      })()
      return arr.map(x => (typeof x === 'string' ? x : (x && (x.fileName || x.name)) || '')).filter(Boolean)
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.flow-detail-wrap { display: flex; flex-direction: column; gap: 16px; }
.hfd-desc { display: flex; gap: 12px; align-items: flex-start; padding: 10px 14px; background: var(--color-primary-light); border: 1px dashed rgba(var(--color-primary-rgb),0.4); border-radius: 3px; font-size: 13px;
  .hfd-desc-label { width: 90px; color: #757575; flex-shrink: 0; line-height: 1.6; }
  .hfd-desc-text { flex: 1; color: var(--color-primary); line-height: 1.6; white-space: pre-wrap; word-break: break-all; }
}
.hfd-tpl { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; }
.tpl-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 32px; }
.tpl-item { display: flex; flex-direction: column; gap: 5px; min-width: 0; }
.tpl-label { font-size: 12px; color: #999; display: inline-flex; align-items: center; gap: 4px; }
.tpl-value { font-size: 14px; color: var(--color-primary); font-weight: 500; word-break: break-all; line-height: 1.5; white-space: pre-wrap; }
.role-hint-icon { width: 18px; height: 18px; border-radius: 4px; display: inline-flex; align-items: center; justify-content: center; cursor: help; font-size: 12px;
  &.role-creator { background: rgba(21, 128, 61,0.1); color: #15803D; }
  &.role-handler { background: rgba(180, 83, 9,0.12); color: #B45309; }
}
.hfd-cols { display: flex; gap: 16px; align-items: flex-start; }
.hfd-left { flex: 3; min-width: 0; }
.hfd-right { flex: 1; min-width: 0; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; }
.chain-section { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 16px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 8px; }
// 横/竖切换按钮
.orient-toggle { display: inline-flex; align-items: center; gap: 4px; margin-left: auto; background: #f5f5f5; border: 1px solid #e8e8e8; border-radius: 2px; padding: 2px; }
.orient-btn { display: inline-flex; align-items: center; gap: 4px; padding: 4px 12px; border-radius: 2px; font-size: 12px; color: #757575; cursor: pointer; transition: all .2s; user-select: none;
  &:hover { color: $primary; }
  &.active { background: $primary; color: #fff; font-weight: 600; box-shadow: 0 2px 6px rgba(var(--color-primary-rgb),0.3); }
}
// 横向模式：节点 chip 轨道（全部节点，可换行）
.chain-track-h { display: flex; flex-wrap: wrap; align-items: center; gap: 6px 0; padding: 6px 0 14px; }
.chain-chip { display: inline-flex; align-items: center; gap: 7px; padding: 8px 14px 8px 8px; border-radius: 2px; border: 1px solid #CBD5E1; background: #fff; font-size: 13px; cursor: default; transition: all .2s; white-space: nowrap;
  &.chip-done { border-color: rgba(21, 128, 61,0.4); background: rgba(21, 128, 61,0.05);
    &.clickable { cursor: pointer; &:hover { border-color: #15803D; box-shadow: 0 2px 8px rgba(21, 128, 61,0.15); transform: translateY(-1px); } }
  }
  &.chip-current { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 0 0 1px rgba(var(--color-primary-rgb),0.2); }
  &.chip-pending { opacity: 0.55; background: #f7f7f7; border-style: dashed; }
  &.chip-rejected { border-color: #B45309; background: rgba(180, 83, 9,0.08); }
  &.expanded { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.15); }
  &.mine::after { content: ''; display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: $primary; }
}
.cc-no { width: 20px; height: 20px; border-radius: 50%; background: #CBD5E1; color: var(--color-primary); display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; flex-shrink: 0; }
.chip-done .cc-no { background: #15803D; color: #fff; }
.chip-current .cc-no { background: $primary; color: #fff; }
.cc-name { font-weight: 600; color: #1b1c1c; }
.chip-pending .cc-name { color: #999; }
.cc-now { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 700; background: $primary; color: #fff; }
.cc-done-tag { color: #15803D; font-size: 15px; display: inline-flex; }
.chain-arrow { margin: 0 2px; color: #94A3B8; font-size: 14px; flex-shrink: 0; }
// 横向模式：展开的节点详情面板
.step-detail { margin-top: 10px; border: 1px solid $border; border-radius: 3px; background: #fff; overflow: hidden;
  .sd-head { display: flex; align-items: center; gap: 10px; padding: 10px 14px; background: var(--color-primary-light); border-bottom: 1px dashed rgba(var(--color-primary-rgb),0.3); flex-wrap: wrap; }
  .sd-no { width: 20px; height: 20px; border-radius: 50%; background: $primary; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 11px; font-weight: 700; flex-shrink: 0; }
  .sd-name { font-size: 14px; font-weight: 700; color: #1b1c1c; }
  .sd-meta { font-size: 12px; color: #757575; display: inline-flex; align-items: center; gap: 3px; i { margin-right: 1px; } }
  .meta-reason { font-size: 12px; color: #B45309; font-weight: 600; max-width: 340px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: inline-flex; align-items: center; gap: 3px; }
  .sd-overdue { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 700; color: #fff; background: #D97706; display: inline-flex; align-items: center; gap: 3px; }
  .step-expanded { border: none; margin: 0; padding: 14px; }
}
.chain-track { display: flex; flex-direction: column; gap: 10px; }
.chain-step { border: 1px solid #ebeef5; border-radius: 3px; padding: 12px 14px; background: #fff; transition: all .2s;
  &.st-done { border-color: rgba(21, 128, 61,0.3); background: rgba(21, 128, 61,0.03); }
  &.st-current { background: var(--color-primary-light); }
  &.st-pending { opacity: 0.55; background: #f7f7f7; }
  &:hover { border-color: $primary; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  &.clickable { cursor: pointer; }
  &.expanded { border-color: $primary; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
}
.step-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.step-no { width: 22px; height: 22px; border-radius: 50%; background: #CBD5E1; color: var(--color-primary); display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.st-current .step-no { background: $primary; color: #fff; }
.st-done .step-no { background: #15803D; color: #fff; }
.step-name { font-size: 14px; font-weight: 600; color: #1b1c1c; flex: 1; }
.step-badge { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.badge-done { background: rgba(21, 128, 61,0.12); color: #15803D; }
.badge-current { background: rgba(var(--color-primary-rgb),0.12); color: $primary; }
.badge-pending { background: #e8e8e8; color: #999; }
.mine-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; background: $primary; color: #fff; }
.branch-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
.cur-stage-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 700; background: $primary; color: #fff; letter-spacing: .5px; }
.step-meta { display: flex; gap: 16px; margin-top: 6px; padding-left: 32px; font-size: 12px; color: #757575;
  i { margin-right: 3px; }
}
.step-reject-reason { display: block; flex-basis: 100%; color: #B45309; font-weight: 600; line-height: 1.5; white-space: pre-wrap; word-break: break-all; }
.step-expanded { display: flex; gap: 16px; margin-top: 12px; padding: 12px; background: #fff; border-radius: 2px; border: 1px dashed rgba(var(--color-primary-rgb),0.35); }
.expanded-left { flex: 3; min-width: 0; }
.expanded-right { flex: 1; min-width: 0; border-left: 1px solid #f0f0f0; padding-left: 16px; }
.expanded-sub-title { font-size: 12px; font-weight: 700; color: #414755; margin-bottom: 8px; }
.bd-block { margin-top: 10px; padding-top: 8px; border-top: 1px dashed #CBD5E1; }
.bd-sub-title { color: #B45309; }
.form-rows { display: flex; flex-direction: column; }
.form-row { display: flex; gap: 8px; padding: 7px 10px; font-size: 13px; background: #f7f7f9; border-radius: 4px; margin-bottom: 6px;
  &:last-child { margin-bottom: 0; }
}
.fr-label { width: 120px; color: #757575; flex-shrink: 0; font-weight: 600; }
.fr-value { color: #1b1c1c; flex: 1; word-break: break-all; white-space: pre-wrap; line-height: 1.5; }
.form-empty { font-size: 12px; color: #bbb; text-align: center; padding: 8px 0; }
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
.action-list { display: flex; flex-direction: column; gap: 6px; }
.action-item { display: flex; flex-direction: column; gap: 4px; padding: 6px 8px; border-radius: 4px; font-size: 12px;
  &.act-pass { background: rgba(21, 128, 61,0.06); }
  &.act-reject { background: rgba(180, 83, 9,0.08); }
  i { margin-right: 2px; }
}
.action-head { display: flex; align-items: center; gap: 8px; }
.action-badge { padding: 1px 6px; border-radius: 3px; font-weight: 700; font-size: 11px; flex-shrink: 0; }
.act-pass .action-badge { background: #15803D; color: #fff; }
.act-reject .action-badge { background: #B45309; color: #fff; }
.action-user { color: #414755; }
.action-time { color: #999; margin-left: auto; }
.action-reason { color: #B45309; font-size: 12px; padding-left: 4px; line-height: 1.4; word-break: break-all; white-space: pre-wrap; }
.action-comment { color: #15803D; font-size: 12px; padding-left: 4px; line-height: 1.4; word-break: break-all; white-space: pre-wrap; }

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
.empty-state { text-align: center; padding: 60px 20px; color: #bbb;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
</style>
