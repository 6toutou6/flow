<template>
  <div class="fc" :class="{ 'fc-card': card }">
    <!-- 头部：标题 + 提示 + 横/竖切换 -->
    <div v-if="showHeader" class="fc-head">
      <span class="fc-title">{{ title }}</span>
      <span v-if="hint" class="fc-hint">{{ hint }}</span>
      <span class="orient-toggle">
        <span class="orient-btn" :class="{ active: chainOrientation === 'horizontal' }" :title="'横向：每次展开一个节点详情'" @click="chainOrientation = 'horizontal'"><i class="el-icon-s-fold" /> 横向</span>
        <span class="orient-btn" :class="{ active: chainOrientation === 'vertical' }" :title="'竖向：可同时展开全部节点详情'" @click="chainOrientation = 'vertical'"><i class="el-icon-s-unfold" /> 竖向</span>
      </span>
    </div>

    <!-- 空态 -->
    <div v-if="flowChain.length === 0" class="fc-empty">
      <i class="el-icon-set-up" />
      <p>暂无流程数据</p>
    </div>

    <template v-else>
      <!-- 横向模式：全部节点 chip 轨道，点击已处理节点展开单个节点详情（同时只展开一个） -->
      <template v-if="chainOrientation === 'horizontal'">
        <div class="chain-track-h">
          <template v-for="(item, idx) in flowChain">
            <div
              :key="idx"
              class="chain-chip"
              :class="['chip-' + item.status, { clickable: item.status === 'done', expanded: hExpandedNodeId === item.nodeId, mine: item.isMine }]"
              :title="`${idx + 1}. ${item.nodeName}（${statusLabel(item.status)}）${item.assignedText ? '\n处理人：' + item.assignedText : ''}${item.handledText && item.assignedText !== item.handledText ? '\n实际处理：' + item.handledText : ''}`"
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
            <span v-if="hExpandedItem.latestDone && hExpandedItem.latestDone.handleTime" class="step-time"><i class="el-icon-time" /> {{ hExpandedItem.latestDone.handleTime }}</span>
            <span class="step-badge" :class="'badge-' + hExpandedItem.status">{{ statusLabel(hExpandedItem.status) }}</span>
            <span v-if="hExpandedItem.status === 'current'" class="cur-stage-tag">当前阶段</span>
            <div class="sd-handlers">
              <span v-if="hExpandedItem.hasPending" class="sd-meta"><i class="el-icon-user" /> 处理人：{{ hExpandedItem.pendingHandlersText || '—' }}</span>
              <span v-if="hExpandedItem.latestDone" class="actual-handler"><i class="el-icon-user" /> 实际处理：{{ hExpandedItem.handledText || hExpandedItem.latestDone.handlerName || '—' }}</span>
              <span v-if="hExpandedItem.latestDone && hExpandedItem.assignedText !== hExpandedItem.handledText" class="sd-meta"><i class="el-icon-s-custom" /> 当时分配：{{ hExpandedItem.assignedText }}</span>
              <span v-if="hExpandedItem.latestDone && nodeOverdue(hExpandedItem.latestDone.handleTime)" class="sd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span>
            </div>
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
              <!-- 该节点处理人填写的任务基础字段（fieldRole=2）置顶展示 -->
              <div v-if="hExpandedItem.latestDone && hExpandedItem.latestDone.baseDataList && hExpandedItem.latestDone.baseDataList.length > 0" class="bd-block">
                <div class="expanded-sub-title bd-sub-title">
                  任务基础信息
                  <span class="bd-handler-note"><i class="el-icon-user" /> 「{{ hExpandedItem.nodeName }}」节点由 {{ hExpandedItem.latestDone.handlerName }}{{ hExpandedItem.latestDone.handlerUserId ? ' ' + hExpandedItem.latestDone.handlerUserId : '' }} 填写</span>
                </div>
                <div v-for="(bd, bi) in hExpandedItem.latestDone.baseDataList" :key="bi" class="form-row">
                  <span class="fr-label">{{ bd.fieldLabel }}</span>
                  <AttachField v-if="bd.fieldType === 'file' || bd.fieldType === 'image'" :value="bd.fieldValue" :field-type="bd.fieldType" readonly class="fr-value" />
                  <span v-else class="fr-value">{{ bd.fieldValue || '—' }}</span>
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

      <!-- 竖向模式：卡片式流程链，可同时展开多个节点 -->
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
            <span v-if="item.latestDone && item.latestDone.handleTime" class="step-time"><i class="el-icon-time" /> {{ item.latestDone.handleTime }}</span>
            <span class="step-badge" :class="'badge-' + item.status">{{ statusLabel(item.status) }}</span>
            <span v-if="item.status === 'current'" class="cur-stage-tag">当前阶段</span>
            <span v-if="item.latestDone" class="mine-tag">{{ item.latestDone.handlerName || '该人员' }}已处理</span>
          </div>
          <div class="step-meta">
            <template v-if="item.latestDone">
              <span class="actual-handler"><i class="el-icon-user" /> 实际处理：{{ item.handledText || item.latestDone.handlerName || '—' }}</span>
              <span v-if="item.assignedText !== item.handledText"><i class="el-icon-s-custom" /> 当时分配：{{ item.assignedText }}</span>
              <span v-if="nodeOverdue(item.latestDone.handleTime)" class="sd-overdue"><i class="el-icon-alarm-clock" /> 超期处理</span>
            </template>
            <template v-else-if="item.hasPending">
              <span><i class="el-icon-user" /> 处理人：{{ item.pendingHandlersText || '待处理' }}</span>
            </template>
          </div>
          <!-- 展开内容：左表单 + 右操作记录 -->
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
              <!-- 该节点处理人填写的任务基础字段（fieldRole=2）置顶展示 -->
              <div v-if="item.latestDone && item.latestDone.baseDataList && item.latestDone.baseDataList.length > 0" class="bd-block">
                <div class="expanded-sub-title bd-sub-title">
                  任务基础信息
                  <span class="bd-handler-note"><i class="el-icon-user" /> 「{{ item.nodeName }}」节点由 {{ item.latestDone.handlerName }}{{ item.latestDone.handlerUserId ? ' ' + item.latestDone.handlerUserId : '' }} 填写</span>
                </div>
                <div v-for="(bd, bi) in item.latestDone.baseDataList" :key="bi" class="form-row">
                  <span class="fr-label">{{ bd.fieldLabel }}</span>
                  <AttachField v-if="bd.fieldType === 'file' || bd.fieldType === 'image'" :value="bd.fieldValue" :field-type="bd.fieldType" readonly class="fr-value" />
                  <span v-else class="fr-value">{{ bd.fieldValue || '—' }}</span>
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
    </template>
  </div>
</template>

<script>
import AttachField from '@/components/AttachField.vue'
import { formatNodeHandlers } from '@/utils'

/**
 * 任务流程链（统一组件）：
 * 以「期次人员-流程详情」的展示为准——横向 chip / 竖向卡片可切换，
 * 展示该任务从开始到当前节点的全部阶段；多处理人阶段合并展示；
 * 点击已处理节点展开表单/基础字段/填写说明/操作记录，并支持超期/本人高亮。
 */
export default {
  name: 'FlowChain',
  components: { AttachField },
  props: {
    /** TaskDetailVO（需含 templateNodes + taskNodes + task） */
    taskDetail: { type: Object, default: null },
    /** 当前选中人员（用于高亮该人员处理的节点；为空则不标注本人节点） */
    selectedHandler: { type: Object, default: null },
    /** 是否展示头部标题与横/竖切换（按人员查看等已有外层标题时可关闭） */
    showHeader: { type: Boolean, default: true },
    /** 是否自带白卡容器（嵌入已有面板时传 false，仅渲染链本体） */
    card: { type: Boolean, default: true },
    /** 头部标题 */
    title: { type: String, default: '流程链' },
    /** 头部提示文案 */
    hint: { type: String, default: '点击节点可展开查看表单与操作记录' }
  },
  data() {
    return {
      /** 流程链展示方向：默认横向 */
      chainOrientation: 'horizontal',
      /** 竖向模式：已展开的节点 id 集合（竖向可同时展开多个） */
      expandedNodeIds: [],
      /** 已处理节点中「填写说明」被收起的节点 id 集合（默认全部展开） */
      foldedGuideNodeIds: [],
      /** 横向模式：当前展开详情的节点 id（横向同时只展开一个） */
      hExpandedNodeId: null
    }
  },
  computed: {
    /** 流程链：展示任务模板的全部阶段节点（含未走到的阶段灰显 pending，便于查看完整流程与当前位置）；节点有多个处理人时全部展示（高亮选中人员） */
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
      if (tplNodes.length === 0) return []
      return tplNodes
        .map(tpl => {
          const nodes = byNode[tpl.id] || []
          const pendingNodes = nodes.filter(tn => tn.submitStatus === 0)
          const doneNodes = nodes.filter(tn => tn.submitStatus === 1)
          const hasPending = pendingNodes.length > 0
          // 退回重做判定：节点存在晚于“最近一次已处理记录”的待办 → 被退回重做，状态为处理中
          // 注意：taskNodeId 为 32 位字符串主键（FLOWTASKN+时间戳+随机数），必须按字典序比较（同前缀定长=时间序），数值化会失真
          const latestHandledId = doneNodes.reduce((m, tn) => {
            const cur = String(tn.taskNodeId || '')
            return cur > String(m || '') ? tn.taskNodeId : m
          }, '')
          const newerPending = pendingNodes.some(tn => String(tn.taskNodeId || '') > String(latestHandledId || ''))
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
          // 操作记录：该节点真实提交记录（排除“任一完成即可”自动完成的分支，按时间正序）
          const actionHistory = doneNodes
            .filter(tn => tn.formRecordId != null)
            .slice().sort((a, b) => String(a.taskNodeId || '').localeCompare(String(b.taskNodeId || '')))
            .map(tn => ({
              action: tn.action,
              handlerName: tn.handlerName,
              handleTime: tn.handleTime,
              rejectReason: tn.rejectReason,
              passComment: tn.passComment
            }))
          // 待处理人名（同一节点多处理人全部展示，带用户号）
          const pendingHandlersText = formatNodeHandlers(nodes, true)
          // 节点分配的全部处理人（当时选了谁就能看到谁；已完成节点也保留完整名单）
          const assignedText = formatNodeHandlers(nodes, false)
          // 已处理人（实际处理者，带用户号）
          const handledText = latestDone ? formatNodeHandlers([latestDone], false) : ''
          return {
            nodeId: tpl.id,
            nodeName: tpl.nodeName,
            nodeType: tpl.nodeType,
            sortNum: tpl.sortNum,
            isMine: handlerId ? nodes.some(tn => tn.handlerUserId === handlerId) : false,
            status,
            hasPending,
            latestDone,
            pendingHandlersText,
            assignedText,
            handledText,
            actionHistory,
            guideText: tpl.guideText,
            guideFiles: tpl.guideFiles
          }
        })
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
// 卡片容器与头部（组件自带白卡，嵌入各页面即成一节）
.fc-card { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; }
.fc-head { display: flex; align-items: center; flex-wrap: wrap; gap: 6px; margin-bottom: 16px; }
.fc-title { font-size: 15px; font-weight: 700; color: $primary; }
.fc-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 8px; }
.fc-empty { text-align: center; padding: 40px 20px; color: #bbb;
  i { font-size: 40px; display: block; margin-bottom: 10px; }
  p { font-size: 13px; margin: 0; }
}
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
  .sd-overdue { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 700; color: #fff; background: #D97706; display: inline-flex; align-items: center; gap: 3px; }
  .step-expanded { border: none; margin: 0; padding: 14px; }
}
// 竖向模式：卡片式链
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
.cur-stage-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 700; background: $primary; color: #fff; letter-spacing: .5px; }
.step-meta { display: flex; gap: 16px; margin-top: 6px; padding-left: 32px; font-size: 12px; color: #757575;
  i { margin-right: 3px; }
}
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
// 实际处理人（置前高亮，绿色底）
.actual-handler { display: inline-flex; align-items: center; gap: 4px; padding: 1px 10px; border-radius: 4px; font-weight: 600; color: #fff; background: #15803D; font-size: 12px; line-height: 1.7;
  i { color: #fff; font-size: 12px; }
}
.bd-handler-note { display: inline-flex; align-items: center; gap: 3px; font-size: 12px; font-weight: 400; color: var(--color-primary); margin-left: 6px;
  i { color: var(--color-primary); font-size: 12px; }
}
.step-time { display: inline-flex; align-items: center; gap: 3px; font-size: 12px; font-weight: 400; color: #8a93a5; margin-left: 2px;
  i { font-size: 12px; }
}
.sd-handlers { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; flex-basis: 100%; margin-top: 2px; }
</style>
