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
                <span class="link" @click="goBack">任务管理</span>
                <span>/</span>
                <span class="active">{{ pageTitle }}</span>
              </nav>
            </div>
            <h3 class="page-heading">{{ pageTitle }}</h3>
            <!-- 样例任务：对所有用户开放编辑（参考学习），但保存被拒 -->
            <div v-if="sampleViewOnly" class="sample-tip">
              <i class="el-icon-info" /> 该任务为<b>样例</b>，对所有用户开放编辑仅供<b>学习参考</b>：可自由查看并试改各项配置，但<b>修改不可保存</b>（仅超管可维护样例）。如需练习完整流程，请复制为普通任务。
            </div>
          </div>
        </div>

        <!-- 提示条 -->
        <section class="tip-bar">
          <i class="el-icon-info" />
          一个页面完成全部配置：基本信息、模板配置、下发周期、任务人员；保存后任务即可按周期生成期次。
        </section>

        <!-- 基本信息 -->
        <div class="form-card">
          <div class="card-head" @click="toggleCollapse('basic')">
            <div class="card-title"><i class="el-icon-document" /> 基本信息 <span class="card-sub">任务的基础说明信息</span></div>
            <i class="el-icon-arrow-up card-fold" :class="{ folded: collapsed.basic }" />
          </div>
          <div v-show="!collapsed.basic">
            <div class="form-row">
              <label class="form-label"><span class="req">*</span> 任务名称</label>
              <input v-model="form.taskName" class="form-input" placeholder="如: xxx整改" maxlength="100">
            </div>
            <div class="form-row">
              <label class="form-label">任务说明</label>
              <textarea v-model="form.taskDesc" class="form-textarea" rows="2" placeholder="任务说明（选填）" maxlength="500" />
            </div>
          </div>
        </div>

        <!-- 模板配置 -->
        <div class="form-card">
          <div class="card-head" @click="toggleCollapse('template')">
            <div class="card-title"><i class="el-icon-collection" /> 模板配置 <span class="card-sub">选择流程模板并填写模板级字段</span></div>
            <i class="el-icon-arrow-up card-fold" :class="{ folded: collapsed.template }" />
          </div>
          <div v-show="!collapsed.template">
            <div class="form-row">
              <label class="form-label"><span class="req">*</span> 流程模板</label>
              <el-select v-model="form.templateId" placeholder="选择启用的流程模板" style="width:100%" @change="onTemplateChange">
                <el-option v-for="t in templates" :key="t.id" :label="t.templateName" :value="t.id" />
              </el-select>
            </div>
            <!-- 模板配置信息（模板级字段，任务配置好后期次抄用；下方为模板流程预览） -->
            <div v-if="creatorFields.length || templateNodes.length" class="tpl-fields-box">
              <div class="tpl-fields-title">
                <i class="el-icon-collection" /> 模板级字段与流程预览
                <span class="tpl-fields-tip">（期次生成时自动抄用，处理人与后台可见）</span>
              </div>
              <div v-for="f in creatorFields" :key="f.id" class="form-row">
                <label class="form-label"><span v-if="f.required === 1" class="req">*</span> {{ f.fieldLabel }}</label>
                <el-input v-if="f.fieldType === 'text'" v-model="templateForm[f.id]" :placeholder="f.placeholder || '请输入' + f.fieldLabel" :maxlength="f.maxLength || undefined" />
                <el-input v-else-if="f.fieldType === 'textarea'" v-model="templateForm[f.id]" type="textarea" :rows="2" :placeholder="f.placeholder || '请输入' + f.fieldLabel" :maxlength="f.maxLength || undefined" />
                <el-input v-else-if="f.fieldType === 'number'" v-model="templateForm[f.id]" type="number" :placeholder="f.placeholder || '请输入' + f.fieldLabel" />
                <el-date-picker v-else-if="f.fieldType === 'date'" v-model="templateForm[f.id]" type="date" value-format="yyyy-MM-dd" :placeholder="f.placeholder || '选择日期'" style="width:100%" />
                <el-select v-else-if="f.fieldType === 'radio'" v-model="templateForm[f.id]" :placeholder="f.placeholder || '请选择'" style="width:100%">
                  <el-option v-for="opt in parseOptions(f.enumOptions)" :key="opt.value" :label="opt.label" :value="opt.value" />
                </el-select>
                <el-checkbox-group v-else-if="f.fieldType === 'checkbox'" v-model="templateForm[f.id]">
                  <el-checkbox v-for="opt in parseOptions(f.enumOptions)" :key="opt.value" :label="opt.value">{{ opt.label }}</el-checkbox>
                </el-checkbox-group>
                <el-input v-else v-model="templateForm[f.id]" :placeholder="f.placeholder || '请输入' + f.fieldLabel" />
              </div>
              <!-- 处理人填写字段：任务配置阶段只读展示（由处理人处理节点时填写） -->
              <div v-if="handlerFields.length" class="handler-fields-box">
                <div class="tpl-fields-title">
                  <i class="el-icon-user" /> 处理人填写字段
                  <span class="tpl-fields-tip">（由处理人在对应节点处理时填写，此处仅展示）</span>
                </div>
                <div v-for="f in handlerFields" :key="f.id" class="form-row handler-row">
                  <label class="form-label"><span v-if="f.required === 1" class="req">*</span> {{ f.fieldLabel }}</label>
                  <div class="handler-field-tip">
                    <i class="el-icon-info" />
                    {{ handlerNodeName(f) ? '由处理人在「' + handlerNodeName(f) + '」节点处理时填写' : '由处理人处理对应节点时填写' }}
                  </div>
                </div>
              </div>
              <!-- 模板流程预览（横向流程链，点击节点展开查看表单字段） -->
              <div v-if="templateNodes.length > 0" class="tpl-flow-preview">
                <div class="tpl-flow-title">
                  <i class="el-icon-set-up" /> 模板流程预览
                  <span class="tpl-fields-tip">（节点横向展示，点击节点可查看该节点填写的表单字段）</span>
                </div>
                <div class="chain-track-h">
                  <div v-for="(nv, ni) in templateNodes" :key="ni" class="chain-seg">
                    <div
                      class="chain-chip"
                      :class="['chip-pending', { clickable: true, expanded: expandedNodeIdx === ni }]"
                      :title="`${ni + 1}. ${nodeName(nv)}`"
                      @click="toggleNodePreview(ni)"
                    >
                      <span class="cc-no">{{ ni + 1 }}</span>
                      <span class="cc-name">{{ nodeName(nv) }}</span>
                      <span v-if="isStartNode(nv)" class="cc-tag">开始</span>
                      <span v-else-if="isEndNode(nv)" class="cc-tag">结束</span>
                      <span v-if="nodeFields(nv).length > 0" class="cc-count">{{ nodeFields(nv).length }} 字段</span>
                    </div>
                    <span v-if="ni < templateNodes.length - 1" class="chain-arrow"><i class="el-icon-right" /></span>
                  </div>
                </div>
                <!-- 点击节点展开的字段详情 -->
                <div v-if="expandedNode" class="node-detail" @click.stop>
                  <div class="nd-head">
                    <span class="nd-no">{{ expandedNodeIdx + 1 }}</span>
                    <span class="nd-name">{{ nodeName(expandedNode) }}</span>
                    <span v-if="expandedNode.node && expandedNode.node.nodeTips" class="nd-tips">{{ expandedNode.node.nodeTips }}</span>
                  </div>
                  <div v-if="expandedNode.node && expandedNode.node.nextHandlerTip" class="nd-tip-next"><i class="el-icon-user" /> 下一步处理人提示：{{ expandedNode.node.nextHandlerTip }}</div>
                  <div v-if="hasNodeGuide" class="guide-fold" :class="{ open: !guideFoldedEdit }" @click.stop>
                    <div class="guide-fold-head" @click="guideFoldedEdit = !guideFoldedEdit">
                      <i class="el-icon-info guide-fold-flag" />
                      <span class="guide-fold-title">填写说明</span>
                      <span v-if="guideFoldedEdit" class="guide-fold-preview">点击展开查看本节点填写要求与参考文件</span>
                      <span v-else class="guide-fold-preview">点击收回</span>
                      <i :class="guideFoldedEdit ? 'el-icon-arrow-down' : 'el-icon-arrow-up'" class="guide-fold-arrow" />
                    </div>
                    <div v-show="!guideFoldedEdit" class="guide-fold-body">
                      <div v-if="expandedNode.node && expandedNode.node.guideText" class="nd-guide-text">{{ expandedNode.node.guideText }}</div>
                      <div v-if="expandedNode.node && expandedNode.node.guideFiles" class="nd-guide-files">
                        <div class="nd-sub-title"><i class="el-icon-paperclip" /> 说明文件<span class="chain-hint">可预览 / 下载</span></div>
                        <AttachField readonly :value="expandedNode.node.guideFiles" />
                      </div>
                    </div>
                  </div>
                  <div v-if="nodeFields(expandedNode).length > 0" class="nd-fields">
                    <div v-for="(f, fi) in nodeFields(expandedNode)" :key="fi" class="nd-field-row">
                      <span class="ndf-label">
                        {{ f.fieldLabel }}
                        <span v-if="f.required === 1" class="req">*</span>
                      </span>
                      <span class="ndf-type">{{ fieldTypeText(f.fieldType) }}</span>
                      <span v-if="f.enumOptions" class="ndf-options">选项：{{ optionsText(f) }}</span>
                    </div>
                  </div>
                  <div v-else class="nd-empty">该节点无需填写表单字段</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 下发配置 -->
        <div class="form-card">
          <div class="card-head" @click="toggleCollapse('dispatch')">
            <div class="card-title"><i class="el-icon-alarm-clock" /> 下发配置 <span class="card-sub">决定每期什么时候下发、最晚什么时候完成</span></div>
            <i class="el-icon-arrow-up card-fold" :class="{ folded: collapsed.dispatch }" />
          </div>
          <div v-show="!collapsed.dispatch">

            <!-- 从配置模板一键拉取 -->
            <div class="tpl-pick-card">
              <div class="tpl-pick-head">
                <span><i class="el-icon-setting" /> 从下发配置模板拉取</span>
                <button class="tpl-manage" @click="$router.push('/flow-dispatch/config-template')">管理模板</button>
              </div>
              <div class="tpl-pick-body">
                <el-select
                  v-model="pickedTplId"
                  placeholder="选择一套已保存的配置模板，一键填充下方配置"
                  style="width:100%"
                  clearable
                  filterable
                  :loading="tplLoading"
                  @change="onPickTemplate"
                >
                  <el-option v-for="t in configTemplates" :key="t.id" :label="tplLabel(t)" :value="t.id" />
                </el-select>
                <div v-if="pickedTplId" class="tpl-picked-tip"><i class="el-icon-check" /> 已拉取「{{ pickedTplName }}」，可继续微调下方配置</div>
              </div>
            </div>

            <div class="form-row">
              <label class="form-label"><span class="req">*</span> 周期类型</label>
              <el-radio-group v-model="form.cycleType" @change="onCycleChange">
                <el-radio-button :label="1">每周</el-radio-button>
                <el-radio-button :label="2">每月</el-radio-button>
                <el-radio-button :label="3">每季度</el-radio-button>
                <el-radio-button :label="4">单次下发</el-radio-button>
              </el-radio-group>
            </div>
            <div v-if="form.cycleType === 1" class="form-row">
              <label class="form-label"><span class="req">*</span> 每周几</label>
              <el-select v-model="form.cycleDay" placeholder="选择触发日（周几）" style="width:100%">
                <el-option v-for="d in weekDays" :key="d.value" :label="d.label" :value="d.value" />
              </el-select>
            </div>
            <div v-else-if="form.cycleType === 2 || form.cycleType === 3" class="form-row">
              <label class="form-label"><span class="req">*</span> 每月几号</label>
              <el-select v-model="form.cycleDay" placeholder="选择触发日（几号）" style="width:100%">
                <el-option v-for="n in 31" :key="n" :label="n + ' 号'" :value="n" />
              </el-select>
            </div>
            <div v-if="form.cycleType === 4" class="cycle-tip">单次下发：不按周期，每次生成期次为一个独立期次，期次名称在生成时填写</div>
            <div class="form-row">
              <label class="form-label" :class="{ req: true }">{{ form.cycleType === 4 ? '截止天数' : '截止时间' }}</label>
              <div class="inline-control">
                <el-input-number v-model="form.deadlineDays" :min="1" :max="365" />
                <span class="field-tip">{{ form.cycleType === 4 ? '下发后 N 天截止' : '触发日后 N 天截止' }}</span>
              </div>
            </div>
            <div class="form-row">
              <label class="form-label">提前催办</label>
              <div class="inline-control">
                <el-input-number v-model="form.urgeDays" :min="0" :max="180" />
                <span class="field-tip">截止前 N 天自动催办（仅写后端日志，不真实通知）</span>
              </div>
            </div>

            <!-- 下发效果预览 -->
            <div v-if="form.cycleType !== 4" class="preview-bar">
              <div class="pv-item"><span class="pv-label">下期触发</span><b>{{ triggerPreview || '—' }}</b></div>
              <i class="el-icon-right pv-arrow" />
              <div class="pv-item"><span class="pv-label">下期截止</span><b>{{ deadlinePreview || '—' }}</b></div>
              <template v-if="form.urgeDays > 0">
                <i class="el-icon-right pv-arrow" />
                <div class="pv-item"><span class="pv-label">提前{{ form.urgeDays }}天提醒</span><b>{{ urgePreview || '—' }}</b></div>
              </template>
            </div>
          </div>
        </div>

        <!-- 配置人员 -->
        <div class="form-card">
          <div class="card-head" @click="toggleCollapse('member')">
            <div class="card-title"><i class="el-icon-user" /> 配置人员 <span class="card-sub">每次生成期次时自动为这些人员创建提交任务</span></div>
            <i class="el-icon-arrow-up card-fold" :class="{ folded: collapsed.member }" />
          </div>
          <div v-show="!collapsed.member">
            <div class="members-head">
              <div class="selected-summary">
                <i class="el-icon-user" />
                <span>已选 <b>{{ firstHandlers.length }}</b> 个人员</span>
                <span v-if="firstHandlers.length === 0" class="sum-tip">· 点击「添加人员」选择</span>
              </div>
              <button class="btn-add-user" @click="pickerVisible = true"><i class="el-icon-plus" /> 添加人员</button>
            </div>
            <div v-if="firstHandlers.length > 0" class="handler-list">
              <table class="member-table">
                <thead>
                  <tr>
                    <th class="col-user">人员</th>
                    <th class="col-task">任务名称<span class="col-task-tip">下发后该成员每期任务以此名称区分</span></th>
                    <th class="col-op">操作</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="h in firstHandlers" :key="h.id" class="mt-row">
                    <td>
                      <div class="mt-user">
                        <span class="mt-avatar">{{ h.userName ? h.userName.charAt(0) : 'U' }}</span>
                        <span class="mt-name">{{ h.userName }}<em class="mt-emp">{{ h.yyytId }}</em></span>
                        <span class="mt-dept">{{ h.deptName || '—' }}</span>
                      </div>
                    </td>
                    <td>
                      <input
                        v-model="h.taskName"
                        class="ht-input"
                        maxlength="100"
                        :placeholder="defaultTaskName(h)"
                        @blur="ensureHandlerTaskName(h)"
                      >
                    </td>
                    <td class="col-op">
                      <button class="action-link text-error" @click="removeHandler(h.id)"><i class="el-icon-close" /> 移除</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="empty-handler">
              <i class="el-icon-user" />
              <p>尚未配置人员</p>
            </div>
          </div>
        </div>

        <!-- 操作 -->
        <div class="form-actions">
          <button class="btn-cancel" @click="goBack"><i class="el-icon-arrow-left" /> 取消</button>
          <button class="btn-submit" :disabled="saving" @click="handleSubmit">
            <i v-if="saving" class="el-icon-loading" />
            <i v-else class="el-icon-check" /> {{ pageSubmit }}
          </button>
        </div>
      </section>
    </main>

    <!-- 选人弹窗（多选） -->
    <UserPicker
      :visible="pickerVisible"
      title="选择任务人员（可多选）"
      :exclude-ids="excludeUserIds"
      @confirm="confirmPick"
      @close="pickerVisible = false"
    />
  </div>
</template>

<script>
import { getTemplateDetail, getTemplateList } from '@/service/sys/TemplateService'
import { saveDispatchPlan, updateDispatchPlan, getTaskMembers, getDispatchTask, getConfigTemplates } from '@/service/sys/FlowDispatchService'
import UserPicker from '@/components/UserPicker'
import AttachField from '@/components/AttachField'

export default {
  name: 'TaskEditPage',
  components: { UserPicker, AttachField },
  data() {
    return {
      loading: false,
      saving: false,
      // 四大板块折叠状态（默认全部展开）
      collapsed: { basic: false, template: false, dispatch: false, member: false },
      templates: [],
      weekDays: [
        { value: 1, label: '周一' },
        { value: 2, label: '周二' },
        { value: 3, label: '周三' },
        { value: 4, label: '周四' },
        { value: 5, label: '周五' },
        { value: 6, label: '周六' },
        { value: 7, label: '周日' }
      ],
      form: {
        taskName: '', taskDesc: '', templateId: null,
        cycleType: 2, cycleDay: 1, deadlineDays: 7, urgeDays: 0, status: 1
      },
      templateFields: [],
      templateForm: {},
      // 模板流程预览（节点链 + 当前展开节点）
      templateNodes: [],
      expandedNodeIdx: -1,
      /** 预览节点填写说明是否收起（默认展开） */
      guideFoldedEdit: false,
      firstHandlers: [],
      pickerVisible: false,
      // 下发配置模板
      configTemplates: [],
      tplLoading: false,
      pickedTplId: null,
      /** 当前编辑的是否为样例任务（样例对所有用户开放编辑学习，但保存仅超管可落库） */
      isSampleTask: false
    }
  },
  computed: {
    isEdit() { return !!this.$route.query.id },
    taskId() { return this.$route.query.id || null },
    /** 复制新增：源任务 id（复制基本信息/人员配置，保存为全新任务；不复制期次） */
    copyFrom() { return this.$route.query.copyFrom || null },
    /** 页面标题：编辑 / 复制新增 / 新建 */
    pageTitle() { return this.isEdit ? '编辑任务' : (this.copyFrom ? '复制新增任务' : '新建任务') },
    pageSubmit() { return this.isEdit ? '保存修改' : (this.copyFrom ? '保存副本' : '创建任务') },
    isSuperAdmin() {
      const u = this.$store.state.user.userInfo
      return !!(u && u.superAdmin)
    },
    /** 样例任务 + 非超管：只开放编辑学习、保存被拒绝（后端兜底同口径） */
    sampleViewOnly() {
      return this.isEdit && this.isSampleTask && !this.isSuperAdmin
    },
    creatorFields() {
      return (this.templateFields || []).filter(f => f.fieldRole !== 2)
    },
    /** 处理人填写字段（fieldRole=2）：任务配置阶段只读展示 */
    handlerFields() {
      return (this.templateFields || []).filter(f => f.fieldRole === 2)
    },
    excludeUserIds() {
      return this.firstHandlers.map(h => h.id)
    },
    pickedTplName() {
      const t = this.configTemplates.find(x => x.id === this.pickedTplId)
      return t ? t.configName : ''
    },
    // 下发效果预览（与后端周期窗口计算口径一致）
    triggerDate() {
      return calcTrigger(this.form.cycleType, this.form.cycleDay)
    },
    triggerPreview() {
      if (!this.triggerDate) return null
      return fmtDate(this.triggerDate) + ' ' + weekName(this.triggerDate)
    },
    deadlinePreview() {
      if (!this.triggerDate || !this.form.deadlineDays) return null
      return fmtDate(addDays(this.triggerDate, this.form.deadlineDays)) + ' ' + weekName(addDays(this.triggerDate, this.form.deadlineDays))
    },
    urgePreview() {
      if (!this.triggerDate || !this.form.urgeDays) return null
      const end = addDays(this.triggerDate, this.form.deadlineDays)
      return fmtDate(addDays(end, -this.form.urgeDays)) + ' ' + weekName(addDays(end, -this.form.urgeDays))
    },
    // 当前展开预览的模板节点
    expandedNode() {
      return this.expandedNodeIdx >= 0 ? this.templateNodes[this.expandedNodeIdx] : null
    },
    /** 展开预览的节点是否配置了填写说明（文字或文件） */
    hasNodeGuide() {
      const n = this.expandedNode && this.expandedNode.node
      return !!(n && (n.guideText || n.guideFiles))
    }
  },
  watch: {
    // 配置模板列表加载完成后再做一次反向匹配（initEdit 可能早于列表返回）
    configTemplates() {
      this.matchPickedTemplate()
    }
  },
  created() {
    this.loadTemplates()
    this.loadConfigTemplates()
    // 编辑 或 复制新增（copyFrom）都需预填源任务数据；复制走新建保存（不复制期次）
    if (this.isEdit || this.copyFrom) this.initEdit()
  },
  methods: {
    /** 折叠/展开板块 */
    toggleCollapse(key) {
      this.$set(this.collapsed, key, !this.collapsed[key])
    },
    async loadConfigTemplates() {
      this.tplLoading = true
      try {
        const res = await getConfigTemplates()
        this.configTemplates = res.data || []
      } catch (e) {
        console.error(e)
        this.configTemplates = []
      } finally {
        this.tplLoading = false
      }
    },
    /** 模板下拉展示文案：名称 · 周期 · 触发日 · 截止 */
    tplLabel(t) {
      const cycle = { 1: '每周', 2: '每月', 3: '每季度', 4: '单次下发' }[t.cycleType] || '—'
      let day = '—'
      if (t.cycleType === 1) day = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'][t.cycleDay] || '—'
      else if (t.cycleType === 2 || t.cycleType === 3) day = `每月 ${t.cycleDay} 号`
      return `${t.configName}（${cycle}${day !== '—' ? ' · ' + day : ''} · 截止${t.deadlineDays}天）`
    },
    /** 选中模板：一键填充下发配置（可再微调）；不触发 onCycleChange，避免触发日被重置 */
    onPickTemplate(id) {
      if (!id) { this.pickedTplId = null; return }
      const t = this.configTemplates.find(x => x.id === id)
      if (!t) return
      this.form.cycleType = t.cycleType
      this.form.cycleDay = t.cycleDay
      this.form.deadlineDays = t.deadlineDays
      this.form.urgeDays = t.urgeDays || 0
    },
    async loadTemplates() {
      try {
        const res = await getTemplateList({ page: 1, limit: 500 })
        this.templates = (res.data && res.data.records) || []
      } catch (e) {
        console.error(e)
        this.templates = []
      }
    },
    async initEdit() {
      this.loading = true
      try {
        // 编辑源 = 本任务 id；复制新增源 = copyFrom（保存时仍走新建 saveDispatchPlan，不产生副本期次）
        const sourceId = this.isEdit ? this.taskId : this.copyFrom
        if (!sourceId) {
          this.loading = false
          return
        }
        const [detailRes, memberRes] = await Promise.all([
          getDispatchTask(sourceId),
          getTaskMembers(sourceId)
        ])
        const row = detailRes.data
        this.isSampleTask = row.isSample === 1 && this.isEdit
        // 复制新增：任务名追加副本后缀；状态回新建初始（不沿用源任务启停态）
        if (!this.isEdit) {
          row.taskName = (row.taskName || '') + '_副本'
          row.status = 1
        }
        this.form = {
          id: this.isEdit ? row.id : null,
          taskName: row.taskName || '',
          taskDesc: row.taskDesc || '',
          templateId: row.templateId,
          cycleType: row.cycleType || 2,
          cycleDay: row.cycleDay || 1,
          deadlineDays: row.deadlineDays || 7,
          urgeDays: row.urgeDays || 0,
          status: row.status == null ? 1 : row.status
        }
        await this.loadTemplateFields(row.templateId)
        if (row.templateData) {
          try {
            this.templateForm = JSON.parse(row.templateData) || {}
          } catch (e) {
            this.templateForm = {}
          }
        }
        this.firstHandlers = (memberRes.data || []).map(m => ({
          id: m.yyytId,
          yyytId: m.yyytId,
          userName: m.userName,
          deptName: m.deptName,
          taskName: m.taskName || this.defaultTaskName(m)
        }))
        this.matchPickedTemplate()
      } catch (e) {
        console.error(e)
        this.$message.error('任务加载失败')
      } finally {
        this.loading = false
      }
    },
    /** 处理人填写字段绑定的节点名（bindNodeId → 模板节点链中的 nodeName） */
    handlerNodeName(field) {
      if (!field || !field.bindNodeId) return ''
      const nv = (this.templateNodes || []).find(n => n.node && n.node.id === field.bindNodeId)
      return nv && nv.node ? nv.node.nodeName : ''
    },

    /** 编辑回显：按任务当前周期配置反向匹配下发配置模板（无匹配则保持不选） */
    matchPickedTemplate() {
      if (!this.isEdit || !this.configTemplates.length || !this.form.cycleType) return
      const match = this.configTemplates.find(t =>
        t.cycleType === this.form.cycleType &&
        (t.cycleDay === this.form.cycleDay) &&
        (t.deadlineDays === this.form.deadlineDays) &&
        ((t.urgeDays || 0) === (this.form.urgeDays || 0))
      )
      this.pickedTplId = match ? match.id : null
    },

    async loadTemplateFields(templateId) {
      if (!templateId) return
      try {
        const res = await getTemplateDetail(templateId)
        this.templateFields = (res.data && res.data.templateFields) || []
        this.templateNodes = (res.data && res.data.nodes) || []
        this.expandedNodeIdx = -1
        this.templateForm = {}
      } catch (e) {
        console.error(e)
        this.templateFields = []
        this.templateNodes = []
      }
    },
    onTemplateChange(id) {
      this.loadTemplateFields(id)
    },
    // ===== 模板流程预览方法 =====
    nodeName(nv) {
      return (nv && nv.node && nv.node.nodeName) || '未命名节点'
    },
    isStartNode(nv) {
      return nv && nv.node && nv.node.nodeType === 1
    },
    isEndNode(nv) {
      return nv && nv.node && nv.node.nodeType === 3
    },
    nodeFields(nv) {
      return (nv && nv.fields) || []
    },
    toggleNodePreview(idx) {
      this.expandedNodeIdx = this.expandedNodeIdx === idx ? -1 : idx
      this.guideFoldedEdit = false
    },
    fieldTypeText(t) {
      return { text: '单行文本', textarea: '多行文本', number: '数字', date: '日期', radio: '单选', checkbox: '多选', file: '文件', image: '图片' }[t] || t || '—'
    },
    optionsText(f) {
      try {
        const arr = JSON.parse(f.enumOptions)
        if (!Array.isArray(arr)) return '—'
        return arr.map(o => o.label || o.value).join('、')
      } catch (e) {
        return '—'
      }
    },
    onCycleChange() {
      this.form.cycleDay = this.form.cycleType === 1 ? 1 : (this.form.cycleType === 4 ? null : 1)
    },
    parseOptions(json) {
      if (!json) return []
      try {
        return JSON.parse(json) || []
      } catch (e) {
        return []
      }
    },
    confirmPick(users) {
      const existing = new Set(this.firstHandlers.map(h => h.id))
      users.forEach(u => {
        const uid = u.yyytId || u.id
        if (uid && !existing.has(uid)) {
          this.firstHandlers.push({ id: uid, yyytId: uid, userName: u.userName, deptName: u.deptName, taskName: this.defaultTaskName({ userName: u.userName }) })
        }
      })
      this.pickerVisible = false
    },
    defaultTaskName(h) {
      const n = (h && h.userName) || ''
      return n ? '下发给' + n + '的任务' : '下发给的任务'
    },
    ensureHandlerTaskName(h) {
      if (!h.taskName || !h.taskName.trim()) {
        this.$nextTick(() => { h.taskName = this.defaultTaskName(h) })
      }
    },
    removeHandler(id) {
      this.firstHandlers = this.firstHandlers.filter(h => h.id !== id)
    },
    async handleSubmit() {
      // 样例任务：对非超管开放编辑学习，但保存被拒绝（后端兜底）
      if (this.sampleViewOnly) {
        this.$message.warning('样例任务仅供学习参考，修改不可保存（仅超管可维护样例）；如需完整流程请复制为普通任务')
        return
      }
      if (!this.form.taskName || !this.form.taskName.trim()) {
        this.$message.warning('请填写任务名称')
        return
      }
      for (const f of this.creatorFields) {
        if (f.required === 1) {
          const v = this.templateForm[f.id]
          const empty = v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0)
          if (empty) {
            this.$message.warning(`请填写「${f.fieldLabel}」`)
            return
          }
        }
      }
      if (!this.form.templateId) {
        this.$message.warning('请选择流程模板')
        return
      }
      if (this.form.cycleType !== 4 && !this.form.cycleDay) {
        this.$message.warning('请选择触发日')
        return
      }
      if (this.firstHandlers.length === 0) {
        this.$message.warning('请至少配置一个人员')
        return
      }
      this.saving = true
      try {
        const templateData = {}
        // 创建人填写字段全部写入键（空值写空串），使任务固化字段集与模板创建人字段集一致（供一致性检测/同步判定）
        this.creatorFields.forEach(f => {
          const v = this.templateForm[f.id]
          templateData[f.id] = Array.isArray(v) ? v.join(',') : (v === undefined || v === null ? '' : String(v))
        })
        const payload = {
          id: this.isEdit ? this.form.id : null,
          templateId: this.form.templateId,
          taskName: this.form.taskName,
          taskDesc: this.form.taskDesc,
          templateData,
          cycleType: this.form.cycleType,
          cycleDay: this.form.cycleType === 4 ? null : this.form.cycleDay,
          deadlineDays: this.form.deadlineDays,
          urgeDays: this.form.urgeDays,
          status: this.form.status,
          memberIds: this.firstHandlers.map(h => h.id),
          memberTaskNames: this.firstHandlers.reduce((acc, h) => {
            acc[h.id] = (h.taskName && h.taskName.trim()) ? h.taskName.trim() : this.defaultTaskName(h)
            return acc
          }, {})
        }
        const res = this.isEdit ? await updateDispatchPlan(payload) : await saveDispatchPlan(payload)
        this.$message.success(res.message || '保存成功')
        if (!this.isEdit) {
          this.goBack()
        } else {
          // 编辑模式：保存后留在本页并刷新回显（成员/模板配置）
          this.initEdit()
        }
      } catch (e) {
        console.error(e)
        this.$message.error((e && e.message) || '保存失败')
      } finally {
        this.saving = false
      }
    },
    goBack() {
      this.$router.push('/flow-dispatch/index')
    }
  }
}

// ===== 周期触发日期计算（与后端 FlowDispatchService.nextWindow 口径一致） =====
function calcTrigger(cycleType, cycleDay) {
  if (cycleType === 4 || !cycleDay) return null
  const now = new Date()
  const today = new Date(now.getFullYear(), now.getMonth(), now.getDate())
  const day = Math.max(1, cycleDay)
  if (cycleType === 1) {
    const dow = (today.getDay() + 6) % 7 // 周一=0..周日=6
    const monday = new Date(today)
    monday.setDate(today.getDate() - dow)
    let d = new Date(monday)
    d.setDate(monday.getDate() + Math.min(day, 7) - 1)
    if (d < today) {
      d = new Date(monday)
      d.setDate(monday.getDate() + 7 + Math.min(day, 7) - 1)
    }
    return d
  }
  if (cycleType === 2) {
    const y = today.getFullYear()
    const m = today.getMonth()
    const last = new Date(y, m + 1, 0).getDate()
    let d = new Date(y, m, Math.min(day, last))
    if (d < today) {
      const nextLast = new Date(y, m + 2, 0).getDate()
      d = new Date(y, m + 1, Math.min(day, nextLast))
    }
    return d
  }
  const y = today.getFullYear()
  const q = Math.floor(today.getMonth() / 3)
  const fm = q * 3
  const last = new Date(y, fm + 1, 0).getDate()
  let d = new Date(y, fm, Math.min(day, last))
  if (d < today) {
    const nq = q === 3 ? 0 : q + 1
    const ny = q === 3 ? y + 1 : y
    const nm = nq * 3
    const nLast = new Date(ny, nm + 1, 0).getDate()
    d = new Date(ny, nm, Math.min(day, nLast))
  }
  return d
}

function addDays(date, n) {
  const d = new Date(date)
  d.setDate(d.getDate() + n)
  return d
}

function fmtDate(d) {
  const pad = n => (n < 10 ? '0' + n : '' + n)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

function weekName(d) {
  return ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.dashboard-container { display: flex; min-height: 100vh; background-color: var(--color-primary-surface);  color: #1b1c1c; }
.main-content { width: 100%; display: flex; flex-direction: column; min-height: 100vh; }
.page-content { padding: 24px; display: flex; flex-direction: column; gap: 16px; width: 100%; box-sizing: border-box; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; }
.header-left { display: flex; flex-direction: column; align-items: flex-start; gap: 8px; }
.hl-row1 { display: flex; align-items: center; gap: 14px; }
.hl-row1 .breadcrumb { margin-bottom: 0; }
.breadcrumb { display: flex; gap: 8px; font-size: 12px; line-height: 20px; color: #414755; margin-bottom: 8px;
  .active { color: $primary; font-weight: 600; }
  .link { color: $primary; cursor: pointer;
    &:hover { text-decoration: underline; }
  }
}
.page-heading { font-size: 24px; line-height: 32px; font-weight: 600; color: #1b1c1c; }
.btn-back { display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; background: transparent; border: none; color: var(--color-primary); cursor: pointer; font-size: 13px; transition: background .2s;
  &:hover { background: rgba(var(--color-primary-rgb), 0.08); }
  &:hover { background: var(--color-primary-light); }
}
.tip-bar { display: flex; align-items: center; gap: 8px; background: var(--color-primary-light); border: 1px solid $border; color: var(--color-primary-hover); font-size: 13px; border-radius: 3px; padding: 10px 14px;
  i { color: $primary; }
}
// 样例任务学习提示（橙）
.sample-tip { display: flex; align-items: center; gap: 8px; background: rgba(180, 83, 9, 0.08); border: 1px solid rgba(180, 83, 9, 0.4); color: #B45309; font-size: 13px; border-radius: 3px; padding: 10px 14px; margin-bottom: 4px;
  i { color: #B45309; }
  b { font-weight: 700; }
}
.form-card { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 18px 20px; }
.card-head { display: flex; align-items: center; justify-content: space-between; cursor: pointer; user-select: none; margin-bottom: 14px;
  &:hover .card-fold { color: $primary; }
}
.card-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: #1b1c1c;
  i { color: $primary; }
}
.card-fold { font-size: 16px; color: #94A3B8; transition: all .25s; cursor: pointer;
  &.folded { transform: rotate(180deg); color: $primary; }
  &:hover { color: $primary; }
}
.card-sub { font-size: 12px; color: #999; font-weight: 400; }
.form-row { display: flex; flex-direction: column; gap: 6px; margin-bottom: 14px;
  &:last-child { margin-bottom: 0; }
}
.form-label { font-size: 13px; color: #414755; font-weight: 600; }
.req { color: $primary; }
.form-input { height: 36px; border: 1px solid #dcdfe6; border-radius: 2px; padding: 0 10px; font-size: 13px; outline: none; transition: all .2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.15); }
}
.form-textarea { border: 1px solid #dcdfe6; border-radius: 2px; padding: 8px 10px; font-size: 13px; outline: none; resize: vertical; font-family: inherit;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.15); }
}
.inline-control { display: flex; align-items: center; gap: 10px; }
.field-tip { font-size: 12px; color: #909399; }
.cycle-tip { font-size: 12px; color: var(--color-primary-hover); background: var(--color-primary-light); border-radius: 2px; padding: 7px 10px; margin-bottom: 14px; line-height: 1.6; }
.tpl-fields-box { margin-top: 8px; padding: 16px; border: 1px dashed $border; border-radius: 3px; background: var(--color-primary-light); }
.handler-fields-box { margin-top: 12px; padding: 12px 16px; border: 1px dashed $border; border-radius: 3px; background: rgba(114, 119, 134, 0.06); }
.handler-row { margin-bottom: 10px; }
.handler-field-tip { display: inline-flex; align-items: center; gap: 6px; font-size: 12px; color: #727786; line-height: 1.6; padding: 6px 10px; background: #fff; border: 1px solid #CBD5E1; border-radius: 3px; }
.tpl-flow-preview { margin-top: 10px; padding: 14px 16px; background: #fff; border: 1px solid rgba(var(--color-primary-rgb),0.35); border-radius: 3px; box-shadow: 0 1px 4px rgba(var(--color-primary-rgb),0.08); }
.tpl-flow-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: #1b1c1c; margin-bottom: 10px;
  i { color: $primary; }
}
// 横向流程链（参考任务处理弹窗样式）
.chain-track-h { display: flex; align-items: center; gap: 6px; overflow-x: auto; white-space: nowrap; padding: 4px 0 6px;
  &::-webkit-scrollbar { height: 4px; }
  &::-webkit-scrollbar-thumb { background: #ddd; border-radius: 4px; }
}
.chain-seg { display: inline-flex; align-items: center; gap: 6px; flex-shrink: 0; }
.chain-chip { display: inline-flex; align-items: center; gap: 6px; padding: 6px 12px; border-radius: 2px; font-size: 13px; font-weight: 600; flex-shrink: 0; line-height: 1.5; border: 1px solid transparent;
  &.chip-pending { background: #f0f0f0; color: #757575; border: 1px solid $border; }
  &.clickable { cursor: pointer;
    &:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.12); border-color: $primary; color: $primary; }
  }
  &.expanded { border-color: $primary; background: var(--color-primary-light); color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.2); }
}
.cc-no { width: 20px; height: 20px; border-radius: 50%; background: rgba(0,0,0,0.12); display: inline-flex; align-items: center; justify-content: center; font-size: 12px; flex-shrink: 0; }
.chain-arrow { color: #ccc; font-size: 14px; flex-shrink: 0; }
.cc-tag { font-size: 11px; background: $primary; color: #fff; padding: 0 6px; border-radius: 3px; line-height: 1.6; flex-shrink: 0; }
.cc-count { font-size: 11px; color: #999; font-weight: 400; }
// 节点详情（点击节点展开）
.node-detail { margin-top: 10px; padding: 12px 14px; background: #fff; border: 1px dashed $border; border-radius: 3px; }
.nd-head { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; margin-bottom: 8px; }
.nd-no { width: 22px; height: 22px; border-radius: 50%; background: $primary; color: #fff; display: inline-flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 700; flex-shrink: 0; }
.nd-name { font-size: 14px; font-weight: 700; color: #1b1c1c; }
.nd-tips { font-size: 12px; color: var(--color-primary-hover); background: var(--color-primary-light); border-radius: 4px; padding: 3px 8px; line-height: 1.5; }
.nd-tip-next { font-size: 12px; color: #B45309; background: rgba(180, 83, 9,0.08); border-radius: 4px; padding: 5px 9px; margin-bottom: 8px; line-height: 1.5;
  i { margin-right: 3px; }
}
.nd-sub-title { display: flex; align-items: center; gap: 5px; font-size: 12px; font-weight: 700; color: #4a4f58; margin-bottom: 4px;
  i { color: $primary; }
}
.nd-guide { margin-bottom: 8px; }
.nd-guide-text { margin-bottom: 10px; font-size: 13px; line-height: 1.7; color: #606266; background: #f7f7f9; border-radius: 2px; padding: 8px 12px; white-space: pre-wrap; word-break: break-all; }
.nd-guide-files { margin-bottom: 8px; }
// 填写说明折叠面板（节点预览内）
.guide-fold { margin-bottom: 10px; border: 1px dashed rgba(180, 83, 9,0.4); border-radius: 3px; background: #EFF6FF; overflow: hidden;
  .guide-fold-head { display: flex; align-items: center; gap: 6px; padding: 10px 14px; cursor: pointer; user-select: none;
    &:hover { background: rgba(180, 83, 9,0.06); }
  }
  .guide-fold-flag { color: #B45309; font-size: 15px; }
  .guide-fold-title { font-size: 13px; font-weight: 700; color: var(--color-primary-hover); }
  .guide-fold-preview { flex: 1; min-width: 0; font-size: 12px; color: #64748B; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-left: 4px; }
  .guide-fold-arrow { margin-left: auto; color: #B45309; font-size: 13px; flex-shrink: 0; transition: transform 0.2s; }
  .guide-fold-body { padding: 0 14px 12px; }
  .nd-guide-text { background: #fff; border: 1px dashed rgba(180, 83, 9,0.3); }
}
.nd-fields { display: flex; flex-direction: column; gap: 4px; }
.nd-field-row { display: flex; align-items: baseline; gap: 10px; padding: 6px 10px; background: #f7f7f9; border-radius: 4px; font-size: 13px; }
.ndf-label { font-weight: 600; color: #1b1c1c; min-width: 120px; }
.ndf-type { color: $primary; font-size: 12px; background: rgba(var(--color-primary-rgb),0.08); border-radius: 3px; padding: 1px 7px; flex-shrink: 0; }
.ndf-options { color: #757575; font-size: 12px; flex: 1; word-break: break-all; }
.nd-empty { font-size: 12px; color: #bbb; padding: 6px 0; }
.tpl-fields-title { display: flex; align-items: center; gap: 6px; font-size: 14px; font-weight: 700; color: #1b1c1c; margin-bottom: 12px;
  i { color: $primary; }
}
.tpl-fields-tip { font-size: 12px; color: #999; font-weight: 400; }
.tpl-pick-card { margin-bottom: 16px; padding: 12px 14px; border: 1px dashed rgba(var(--color-primary-rgb),0.35); border-radius: 3px; background: var(--color-primary-light); }
.tpl-pick-head { display: flex; justify-content: space-between; align-items: center; font-size: 13px; font-weight: 700; color: #1b1c1c; margin-bottom: 10px;
  i { color: $primary; }
}
.tpl-manage { color: $primary; background: none; border: 1px solid $primary; border-radius: 2px; padding: 3px 12px; cursor: pointer; font-size: 12px;
  &:hover { background: $primary; color: #fff; }
}
.tpl-pick-body { display: flex; flex-direction: column; gap: 8px; }
.tpl-picked-tip { display: flex; align-items: center; gap: 5px; font-size: 12px; color: #15803D; background: rgba(21, 128, 61,0.08); border-radius: 2px; padding: 6px 10px;
  i { color: #15803D; }
}
.preview-bar { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; margin-top: 4px; padding: 10px 14px; background: var(--color-primary-surface); border: 1px dashed rgba(var(--color-primary-rgb),0.35); border-radius: 3px; }
.pv-item { display: flex; flex-direction: column; gap: 2px;
  .pv-label { font-size: 11px; color: #909399; }
  b { font-size: 13px; color: #1b1c1c; }
}
.pv-arrow { color: $primary; font-size: 14px; }
.members-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.selected-summary { display: flex; align-items: center; gap: 6px; font-size: 14px; color: #414755;
  b { color: $primary; }
  .sum-tip { color: #999; font-size: 12px; }
}
.btn-add-user { display: flex; align-items: center; gap: 4px; padding: 6px 14px; background: $primary; color: #fff; border: none; border-radius: 2px; cursor: pointer; font-size: 13px; font-weight: 600;
  &:hover { opacity: 0.9; }
}
.handler-list { border: 1px solid $border; border-radius: 3px; overflow: hidden; }
.member-table { width: 100%; border-collapse: collapse; table-layout: fixed; }
.member-table th { text-align: left; font-size: 12px; font-weight: 500; color: #8a93a5; background: #F7F9FC; padding: 8px 14px; border-bottom: 1px solid $border; }
.member-table td { padding: 6px 10px; border-bottom: 1px solid #F0F2F6; vertical-align: middle; }
.member-table tbody tr:last-child td { border-bottom: none; }
.member-table tbody tr:hover td { background: #F8FAFC; }
.col-user { width: 34%; }
.col-task-tip { color: #b0b4bd; font-weight: 400; margin-left: 8px; }
.col-op { width: 100px; text-align: right; }
.member-table .col-op .action-link { white-space: nowrap; flex-shrink: 0; }
.mt-user { display: flex; align-items: center; gap: 10px; min-width: 0; }
.mt-avatar { width: 26px; height: 26px; border-radius: 50%; background: $primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 12px; font-weight: 600; flex-shrink: 0; }
.mt-name { font-size: 13px; font-weight: 600; color: #1b1c1c; white-space: nowrap; }
.mt-emp { font-style: normal; font-size: 12px; color: #999; margin-left: 6px; font-family: monospace; }
.mt-dept { font-size: 12px; color: #8a93a5; margin-left: 4px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ht-input { width: 100%; min-width: 0; height: 28px; padding: 0 10px; border: 1px solid #DCDFE6; border-radius: 3px; font-size: 13px; color: #1b1c1c; background: #fff; outline: none; transition: border-color .2s;
  &:focus { border-color: $primary; }
}
.empty-handler { text-align: center; padding: 40px 20px; color: #bbb; border: 1px dashed $border; border-radius: 3px;
  i { font-size: 40px; display: block; margin-bottom: 8px; }
  p { margin: 4px 0; font-size: 14px; color: #999; }
}
.form-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-cancel { display: flex; align-items: center; gap: 4px; padding: 9px 20px; background: #fff; border: 1px solid $border; border-radius: 3px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); }
}
.btn-submit { display: flex; align-items: center; gap: 6px; padding: 9px 26px; background: $primary; color: #fff; border: none; border-radius: 3px; cursor: pointer; font-size: 14px; font-weight: 700; box-shadow: 0 2px 6px rgba(var(--color-primary-rgb),0.2);
  &:hover { opacity: 0.9; }
  &:disabled { opacity: 0.5; cursor: not-allowed; box-shadow: none; }
}
</style>
