<template>
  <div class="flow-detail-wrap">
    <div class="hfd-cols">
    <!-- 左侧：完整流程链（去重：开始到当前节点，不显示退回重复步骤） -->
    <div v-if="flowChain.length > 0" class="chain-section hfd-left">
      <div class="section-title">流程链 <span class="chain-hint">仅显示开始到当前节点；点击节点可展开查看表单与操作记录</span></div>
      <div class="chain-track">
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
            <span v-if="item.isMine" class="mine-tag">该人员处理</span>
            <span v-if="item.branchCount > 1" class="branch-tag">{{ item.branchCount }} 分支</span>
          </div>
          <div class="step-meta">
            <template v-if="item.latestDone">
              <span><i class="el-icon-user" /> {{ item.latestDone.handlerName || '—' }}</span>
              <span><i class="el-icon-time" /> {{ item.latestDone.handleTime || '—' }}</span>
            </template>
            <template v-else-if="item.hasPending">
              <span><i class="el-icon-user" /> {{ item.pendingHandlerNames || '待处理' }}</span>
            </template>
          </div>
          <!-- 展开内容：左表单 + 右操作历史 -->
          <div v-if="item.status === 'done' && expandedNodeIds.includes(item.nodeId)" class="step-expanded" @click.stop>
            <div class="expanded-left">
              <div class="expanded-sub-title">表单数据（最近一次提交）</div>
              <div v-if="item.latestDone && item.latestDone.formDataList && item.latestDone.formDataList.length > 0" class="form-rows">
                <div v-for="(fd, fi) in item.latestDone.formDataList" :key="fi" class="form-row">
                  <span class="fr-label">{{ fd.fieldLabel }}</span>
                  <span class="fr-value">{{ fd.fieldValue || '—' }}</span>
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
            <div class="tl-time"><i class="el-icon-time" /> {{ h.handleTime || '—' }}</div>
            <div v-if="h.action === 0 && h.passComment" class="tl-comment">通过意见：{{ h.passComment }}</div>
            <div v-if="h.action === 1 && h.rejectReason" class="tl-reason">退回原因：{{ h.rejectReason }}</div>
          </div>
        </div>
      </div>
      <div v-else class="history-empty">暂无操作记录</div>
    </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HandlerFlowDetail',
  props: {
    /** TaskDetailVO（含 templateNodes + taskNodes） */
    taskDetail: { type: Object, default: null },
    /** 当前选中处理人（用于高亮该人员处理的节点） */
    selectedHandler: { type: Object, default: null }
  },
  data() {
    return {
      expandedNodeIds: []
    }
  },
  computed: {
    /** 去重流程链：按选中处理人过滤，显示该人员开始到当前节点的流程 */
    flowChain() {
      if (!this.taskDetail) return []
      const tplNodes = this.taskDetail.templateNodes || []
      const taskNodes = this.taskDetail.taskNodes || []
      const handlerId = this.selectedHandler ? this.selectedHandler.id : null
      if (!handlerId) return []
      // 只取该处理人的 task_node，按 nodeId 分组
      const myNodes = taskNodes.filter(tn => tn.handlerUserId === handlerId)
      const byNode = {}
      myNodes.forEach(tn => {
        if (!byNode[tn.nodeId]) byNode[tn.nodeId] = []
        byNode[tn.nodeId].push(tn)
      })
      // 该处理人走到的最大 sortNum（决定流程链显示到哪）
      let maxSort = -Infinity
      myNodes.forEach(tn => { if (tn.sortNum != null && tn.sortNum > maxSort) maxSort = tn.sortNum })
      if (maxSort === -Infinity) return []
      return tplNodes
        .filter(tpl => tpl.sortNum != null && tpl.sortNum <= maxSort)
        .map(tpl => {
          const nodes = byNode[tpl.id] || []
          const pendingNodes = nodes.filter(tn => tn.submitStatus === 0)
          const doneNodes = nodes.filter(tn => tn.submitStatus === 1)
          const hasPending = pendingNodes.length > 0
          const latestDone = doneNodes.length > 0
            ? doneNodes.reduce((a, b) => (a.taskNodeId > b.taskNodeId ? a : b))
            : null
          let status = 'pending'
          if (hasPending) status = 'current'
          else if (doneNodes.length > 0) status = 'done'
          // 操作历史：该处理人已提交 task_node（按时间正序）
          const actionHistory = doneNodes
            .slice().sort((a, b) => (a.taskNodeId || 0) - (b.taskNodeId || 0))
            .map(tn => ({
              action: tn.action,
              handlerName: tn.handlerName,
              handleTime: tn.handleTime,
              rejectReason: tn.rejectReason,
              passComment: tn.passComment
            }))
          // 待处理人名
          const pendingHandlerNames = pendingNodes
            .map(tn => tn.handlerName)
            .filter(Boolean)
            .filter((v, i, arr) => arr.indexOf(v) === i)
            .join('、')
          return {
            nodeId: tpl.id,
            nodeName: tpl.nodeName,
            nodeType: tpl.nodeType,
            sortNum: tpl.sortNum,
            isMine: true,
            status,
            hasPending,
            latestDone,
            pendingHandlerNames,
            branchCount: nodes.length,
            actionHistory
          }
        })
    },
    /** 完整操作历史：该处理人已提交的通过/退回记录（按时间正序），展示在右侧 */
    allHistory() {
      if (!this.taskDetail) return []
      const handlerId = this.selectedHandler ? this.selectedHandler.id : null
      if (!handlerId) return []
      const tns = this.taskDetail.taskNodes || []
      return tns
        .filter(tn => tn.submitStatus === 1 && tn.handlerUserId === handlerId)
        .sort((a, b) => (a.taskNodeId || 0) - (b.taskNodeId || 0))
    }
  },
  watch: {
    taskDetail() {
      this.expandedNodeIds = []
    }
  },
  methods: {
    statusLabel(s) { return { done: '已通过', current: '处理中', pending: '未到' }[s] || '未到' },
    toggleNodeForm(item) {
      if (item.status !== 'done') return
      const idx = this.expandedNodeIds.indexOf(item.nodeId)
      if (idx >= 0) {
        this.expandedNodeIds.splice(idx, 1)
      } else {
        this.expandedNodeIds.push(item.nodeId)
      }
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
$border: #e4beba;
.flow-detail-wrap { display: flex; flex-direction: column; gap: 16px; }
.hfd-cols { display: flex; gap: 16px; align-items: flex-start; }
.hfd-left { flex: 3; min-width: 0; }
.hfd-right { flex: 1; min-width: 0; background: #fff; border: 1px solid $border; border-radius: 8px; padding: 20px; }
.chain-section { background: #fff; border: 1px solid $border; border-radius: 8px; padding: 20px; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 16px; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 8px; }
.chain-track { display: flex; flex-direction: column; gap: 10px; }
.chain-step { border: 1px solid #ebeef5; border-radius: 8px; padding: 12px 14px; background: #fff; transition: all .2s;
  &.st-done { border-color: rgba(38,109,0,0.3); background: rgba(38,109,0,0.03); }
  &.st-current { background: #FFF5F5; }
  &.st-pending { opacity: 0.55; background: #f7f7f7; }
  &:hover { border-color: $primary; box-shadow: 0 2px 8px rgba(0,0,0,0.08); }
  &.clickable { cursor: pointer; }
  &.expanded { border-color: $primary; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }
}
.step-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }
.step-no { width: 22px; height: 22px; border-radius: 50%; background: #e4beba; color: #5b403d; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.st-current .step-no { background: $primary; color: #fff; }
.st-done .step-no { background: #266d00; color: #fff; }
.step-name { font-size: 14px; font-weight: 600; color: #1b1c1c; flex: 1; }
.step-badge { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 600; }
.badge-done { background: rgba(38,109,0,0.12); color: #266d00; }
.badge-current { background: rgba(197,48,48,0.12); color: $primary; }
.badge-pending { background: #e8e8e8; color: #999; }
.mine-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; background: $primary; color: #fff; }
.branch-tag { padding: 1px 7px; border-radius: 3px; font-size: 11px; font-weight: 600; background: rgba(0,89,111,0.1); color: #00596f; }
.step-meta { display: flex; gap: 16px; margin-top: 6px; padding-left: 32px; font-size: 12px; color: #757575;
  i { margin-right: 3px; }
}
.step-expanded { display: flex; gap: 16px; margin-top: 12px; padding: 12px; background: #fff; border-radius: 6px; border: 1px dashed #e4beba; }
.expanded-left { flex: 3; min-width: 0; }
.expanded-right { flex: 1; min-width: 0; border-left: 1px solid #f0f0f0; padding-left: 16px; }
.expanded-sub-title { font-size: 12px; font-weight: 700; color: #414755; margin-bottom: 8px; }
.form-rows { display: flex; flex-direction: column; }
.form-row { display: flex; padding: 5px 0; font-size: 13px; border-bottom: 1px solid #f5f5f5;
  &:last-child { border-bottom: none; }
}
.fr-label { width: 120px; color: #757575; flex-shrink: 0; }
.fr-value { color: #1b1c1c; flex: 1; word-break: break-all; }
.form-empty { font-size: 12px; color: #bbb; text-align: center; padding: 8px 0; }
.action-list { display: flex; flex-direction: column; gap: 6px; }
.action-item { display: flex; flex-direction: column; gap: 4px; padding: 6px 8px; border-radius: 4px; font-size: 12px;
  &.act-pass { background: rgba(38,109,0,0.06); }
  &.act-reject { background: rgba(183,121,31,0.08); }
  i { margin-right: 2px; }
}
.action-head { display: flex; align-items: center; gap: 8px; }
.action-badge { padding: 1px 6px; border-radius: 3px; font-weight: 700; font-size: 11px; flex-shrink: 0; }
.act-pass .action-badge { background: #266d00; color: #fff; }
.act-reject .action-badge { background: #b7791f; color: #fff; }
.action-user { color: #414755; }
.action-time { color: #999; margin-left: auto; }
.action-reason { color: #b7791f; font-size: 12px; padding-left: 4px; line-height: 1.4; word-break: break-all; }
.action-comment { color: #266d00; font-size: 12px; padding-left: 4px; line-height: 1.4; word-break: break-all; }

// 右侧操作历史时间线
.history-timeline { position: relative; padding-left: 18px;
  &::before { content: ''; position: absolute; left: 5px; top: 4px; bottom: 4px; width: 2px; background: #e4beba; }
}
.tl-item { position: relative; padding-bottom: 16px;
  &:last-child { padding-bottom: 0; }
}
.tl-dot { position: absolute; left: -18px; top: 4px; width: 12px; height: 12px; border-radius: 50%; border: 2px solid #fff; box-shadow: 0 0 0 1px rgba(0,0,0,0.1); }
.tl-pass .tl-dot { background: #266d00; }
.tl-reject .tl-dot { background: #b7791f; }
.tl-body { padding: 8px 10px; border-radius: 6px; background: #fafafa; font-size: 12px; }
.tl-node { font-size: 13px; font-weight: 700; color: #1b1c1c; margin-bottom: 4px; }
.tl-head { display: flex; align-items: center; gap: 8px; margin-bottom: 3px; }
.tl-badge { padding: 1px 7px; border-radius: 3px; font-weight: 700; font-size: 11px; color: #fff; flex-shrink: 0; }
.tl-pass .tl-badge { background: #266d00; }
.tl-reject .tl-badge { background: #b7791f; }
.tl-user { color: #414755; i { margin-right: 2px; } }
.tl-time { color: #999; i { margin-right: 2px; } }
.tl-comment { margin-top: 5px; color: #266d00; line-height: 1.5; word-break: break-all; }
.tl-reason { margin-top: 5px; color: #b7791f; line-height: 1.5; word-break: break-all; }
.history-empty { font-size: 13px; color: #bbb; text-align: center; padding: 32px 0; }
.empty-state { text-align: center; padding: 60px 20px; color: #bbb;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
</style>
