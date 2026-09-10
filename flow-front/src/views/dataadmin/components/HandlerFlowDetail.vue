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

    <div class="hfd-cols">
      <!-- 左侧：完整流程链（统一组件：横/竖切换，点击节点查看表单与操作记录） -->
      <div v-if="taskDetail" class="hfd-left">
        <FlowChain :task-detail="taskDetail" :selected-handler="selectedHandler" title="流程链" hint="点击节点可展开查看表单与操作记录" />
      </div>
      <div v-else class="hfd-left empty-state">
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
                <span class="tl-user"><i class="el-icon-user" /> {{ handlerText(h) }}</span>
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
import FlowChain from '@/components/FlowChain.vue'
import { formatHandlerWithTransfer } from '@/utils'

export default {
  name: 'HandlerFlowDetail',
  components: { FlowChain },
  props: {
    /** TaskDetailVO（含 templateNodes + taskNodes） */
    taskDetail: { type: Object, default: null },
    /** 当前选中处理人（用于高亮该人员处理的节点） */
    selectedHandler: { type: Object, default: null }
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
      // 处理人填写字段（fieldRole=2）：回溯实际提交该字段的任务节点，标注「哪个节点由谁填写」
      const filledNodes = (this.taskDetail.taskNodes || []).filter(tn => tn.submitStatus === 1 && Array.isArray(tn.baseDataList))
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
          // 多行文本值过长（>60 字或含换行）时该项独占整行
          longText: f.fieldType === 'textarea' && (() => {
            const v = map[f.id]
            if (v === undefined || v === null) return false
            const s = String(v)
            return s.length > 60 || s.indexOf('\n') >= 0
          })()
        }
      })
    },
    /** 任务是否已全部完成（操作历史时间线末尾补完成节点） */
    taskFinished() {
      return this.taskDetail && this.taskDetail.task && this.taskDetail.task.status === 2
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
        .sort((a, b) => String(a.taskNodeId || '').localeCompare(String(b.taskNodeId || '')))
    }
  },
  methods: {
    /** 处理人展示：交接过的节点显示实际经办人「原处理人 工号（现 接手人 工号）」 */
    handlerText(node, fallback) {
      return formatHandlerWithTransfer(node, fallback)
    },
    /** 节点处理时间是否超过任务截止时间（超期处理软性标记） */
    nodeOverdue(timeStr) {
      if (!timeStr || !this.taskEndTime) return false
      const t = new Date(String(timeStr).replace(/-/g, '/'))
      const end = new Date(String(this.taskEndTime).replace(/-/g, '/'))
      return !isNaN(t.getTime()) && !isNaN(end.getTime()) && t > end
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
.tpl-item.tpl-wide { grid-column: 1 / -1; }
.tpl-label { font-size: 12px; color: #999; display: inline-flex; align-items: center; gap: 4px; }
.tpl-value { font-size: 14px; color: var(--color-primary); font-weight: 500; word-break: break-all; line-height: 1.5; white-space: pre-wrap; }
.hfd-cols { display: flex; gap: 16px; align-items: flex-start; }
.hfd-left { flex: 3; min-width: 0; }
.hfd-right { flex: 1; min-width: 0; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 20px; }
.section-title { font-size: 15px; font-weight: 700; color: $primary; margin-bottom: 16px; display: flex; align-items: center; flex-wrap: wrap; gap: 6px; }
.chain-hint { font-size: 12px; color: #999; font-weight: 400; margin-left: 8px; }
// 任务基础字段来源注明（顶部汇总区）
.tpl-handler-note { display: inline-flex; align-items: center; gap: 3px; font-size: 11px; font-weight: 400; color: var(--color-primary); background: rgba(var(--color-primary-rgb), 0.08); padding: 0 6px; border-radius: 3px; vertical-align: 1px; }
.tpl-creator-note { display: inline-flex; align-items: center; gap: 3px; font-size: 11px; font-weight: 400; color: #8a93a5; background: #F1F3F6; padding: 0 6px; border-radius: 3px; vertical-align: 1px; }
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
.empty-state { text-align: center; padding: 60px 20px; color: #bbb; background: #fff; border: 1px solid $border; border-radius: 3px;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { font-size: 14px; margin: 0; }
}
// 超期标记（右栏时间线共用）
.sd-overdue { padding: 1px 8px; border-radius: 3px; font-size: 11px; font-weight: 700; color: #fff; background: #D97706; display: inline-flex; align-items: center; gap: 3px; }
</style>
