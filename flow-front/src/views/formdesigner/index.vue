<template>
  <div class="designer-container">
    <!-- 顶部栏 -->
    <header class="designer-header">
      <div class="header-left">
        <button class="back-btn" @click="goBack"><i class="el-icon-arrow-left" /> 返回</button>
        <div class="tpl-info">
          <span class="tpl-name">{{ template.templateName || templateName }}</span>
          <span v-if="template.category" class="tpl-cat">{{ template.category }}</span>
        </div>
      </div>
      <div class="header-right">
        <span class="field-count">共 {{ nodes.length }} 个节点 / {{ totalFieldCount }} 个字段<span v-if="templateFields.length" class="tfe-count">（含 {{ templateFields.length }} 个任务基础字段）</span></span>
        <button class="btn-versions" @click="versionVisible = true">
          <i class="el-icon-tickets" /> 版本记录<span v-if="template.version"> v{{ template.version }}</span>
        </button>
        <template v-if="!isReadonly">
          <button class="btn-save" :disabled="saving" @click="onSaveClick">
            <i v-if="saving" class="el-icon-loading" />
            <i v-else class="el-icon-check" />
            保存流程
          </button>
        </template>
        <span v-else class="readonly-tag"><i class="el-icon-view" /> 查看模式</span>
      </div>
    </header>
    <!-- 样例模板只读提示 -->
    <div v-if="isReadonly" class="designer-readonly-tip">
      <i class="el-icon-info" /> 样例模板 · 只读查看，流程/字段结构完整可见；如需修改请先在模板管理中「复制」后编辑。
    </div>

    <div class="designer-body">
      <!-- 左栏：节点链 -->
      <aside class="node-chain">
        <div class="panel-title">流程节点链</div>
        <p class="panel-desc">按业务顺序排列，首位自动标记为「开始」、末位自动标记为「结束」，所有节点均可重命名/删除</p>

        <!-- 任务基础字段入口（模板级字段，不依附节点，置顶展示） -->
        <div class="tfe-entry" :class="{ active: viewMode === 'template' }" @click="selectTemplateFields">
          <i class="el-icon-collection" />
          <div class="tfe-entry-info">
            <div class="tfe-entry-title">任务基础字段</div>
            <div class="tfe-entry-desc">{{ templateFields.length }} 个字段 · 不依附节点</div>
          </div>
        </div>

        <div class="chain-list">
          <div v-if="nodes.length === 0" class="chain-empty">
            <i class="el-icon-set-up" />
            <p>尚未添加节点</p>
            <p class="empty-tip">点击下方「添加节点」开始设计流程</p>
            <p class="empty-tip">首位自动标记为「开始」，末位自动标记为「结束」</p>
          </div>
          <div v-for="(item, idx) in nodes" :key="idx">
            <div
              class="node-item"
              :class="{ active: viewMode === 'node' && selectedNodeIndex === idx }"
              @click="selectNode(idx)"
            >
              <div class="node-item-head">
                <span class="node-idx">{{ idx + 1 }}</span>
                <span class="node-name">{{ item.node.nodeName || '未命名节点' }}</span>
                <span class="node-type-badge" :class="nodeTypeClass(item.node.nodeType)">{{ nodeTypeText(item.node.nodeType) }}</span>
              </div>
              <div class="node-item-meta">
                <span><i class="el-icon-edit" /> {{ (item.fields || []).length }} 个字段</span>
                <span v-if="hasBranchConfig(item.node.branchConfig)" class="cfg-tag tag-branch" title="已配置条件分支"><i class="el-icon-share" /> 分支</span>
              </div>
              <div class="node-item-actions">
                <button class="op-btn" :disabled="idx === 0" title="上移" @click.stop="selectNode(idx); moveNode(idx, -1)"><i class="el-icon-top" /></button>
                <button class="op-btn" :disabled="idx === nodes.length - 1" title="下移" @click.stop="selectNode(idx); moveNode(idx, 1)"><i class="el-icon-bottom" /></button>
                <button class="op-btn" title="复制节点（含字段）" @click.stop="selectNode(idx); copyNode(idx)"><i class="el-icon-copy-document" /></button>
                <button class="op-btn op-del" :disabled="nodes.length <= 2" title="删除" @click.stop="selectNode(idx); confirmRemoveNode(idx)"><i class="el-icon-delete" /></button>
              </div>
            </div>
            <div v-if="idx < nodes.length - 1" class="chain-arrow"><i class="el-icon-bottom" /></div>
          </div>
        </div>
        <button class="btn-add-node" @click="addNode"><i class="el-icon-plus" /> 添加节点</button>
      </aside>

      <!-- 中栏：当前节点/任务基础字段 画布 -->
      <section class="canvas-area">
        <div class="canvas-header">
          <span class="canvas-title">
            <template v-if="viewMode === 'template'">任务基础字段<span class="node-type-badge badge-tpl">任务级</span></template>
            <template v-else>
              当前节点：{{ currentNode ? currentNode.node.nodeName : '—' }}
              <span v-if="currentNode" class="node-type-badge" :class="nodeTypeClass(currentNode.node.nodeType)">{{ nodeTypeText(currentNode.node.nodeType) }}</span>
            </template>
          </span>
        </div>

        <!-- 字段库 -->
        <div class="field-library-bar">
          <span class="lib-label">点击添加字段{{ viewMode === 'template' ? '到任务基础信息' : '到当前节点' }}：</span>
          <div class="field-cards">
            <div v-for="ft in allFieldTypes" :key="ft.type" class="field-card" @click="addField(ft.type)">
              <i :class="ft.icon" />
              <span>{{ ft.label }}</span>
            </div>
          </div>
        </div>

        <!-- 字段画布 -->
        <div class="canvas-list">
          <div
            v-for="(field, idx) in currentFields"
            :key="viewMode + '-' + idx"
            class="field-item"
            :class="{ active: selectedFieldIndex === idx }"
            @click="selectedFieldIndex = idx"
          >
            <div class="field-item-main">
              <div class="field-item-label">
                <span v-if="field.required === 1" class="req">*</span>
                {{ field.fieldLabel }}
                <span class="field-type-tag">{{ typeLabel(field.fieldType) }}</span>
                <span v-if="field.visibleWhen" class="cfg-tag tag-visible" title="已配置显隐条件（满足条件才显示）"><i class="el-icon-view" /> 显隐</span>
                <span v-if="field.editableWhen" class="cfg-tag tag-editable" title="已配置只读条件（满足条件则锁定）"><i class="el-icon-lock" /> 只读</span>
              </div>
              <div class="field-item-ph">{{ field.placeholder || `请输入${field.fieldLabel}` }}</div>
            </div>
            <div class="field-item-actions" @click.stop>
              <!-- 任务基础字段：填写方式悬停提示（处理人填写显示绑定节点，创建人填写不显示节点） -->
              <el-tooltip v-if="viewMode === 'template'" :content="roleTip(field)" placement="top">
                <span class="role-hint-icon" :class="field.fieldRole === 2 ? 'role-handler' : 'role-creator'">
                  <i :class="field.fieldRole === 2 ? 'el-icon-user' : 'el-icon-s-custom'" />
                </span>
              </el-tooltip>
              <button class="op-btn" :disabled="idx === 0" title="上移" @click="moveField(idx, -1)"><i class="el-icon-top" /></button>
              <button class="op-btn" :disabled="idx === currentFields.length - 1" title="下移" @click="moveField(idx, 1)"><i class="el-icon-bottom" /></button>
              <button class="op-btn op-del" title="删除" @click="removeField(idx)"><i class="el-icon-delete" /></button>
            </div>
          </div>
          <div v-if="currentFields.length === 0" class="canvas-empty">
            <i class="el-icon-set-up" />
            <p>{{ viewMode === 'template' ? '尚未配置任务基础字段' : '当前节点暂无字段' }}</p>
            <p class="empty-tip">{{ viewMode === 'template' ? '任务基础字段由下发任务时填写，处理人与后台可见' : '从上方字段库点击添加字段' }}</p>
          </div>
        </div>
      </section>

      <!-- 右栏：属性面板 -->
      <aside class="property-panel">
        <!-- 节点属性 -->
        <div class="prop-section">
          <div class="panel-title panel-title-click" @click="nodePropCollapsed = !nodePropCollapsed">
            <i class="el-icon-set-up" /> {{ viewMode === 'template' ? '任务基础字段说明' : '节点属性' }}
            <i class="el-icon-arrow-down collapse-arrow" :class="{ open: !nodePropCollapsed }" />
          </div>
          <div v-show="!nodePropCollapsed">
          <div v-if="viewMode === 'template'" class="tfe-tip">
            任务基础字段不依附于任何流程节点，是任务本身携带的信息（如规章制度、采购说明等）。
            创建人填写：下发任务时由创建人赋值，处理人与后台均可见；处理人填写：由处理人处理时填写，随节点提交保存。
          </div>
          <div v-else-if="!currentNode" class="panel-empty">请选择一个节点</div>
          <div v-else class="prop-form">
            <div class="prop-group">
              <label class="prop-label">节点名称 <span class="req">*</span></label>
              <input v-model="currentNode.node.nodeName" class="prop-input" placeholder="如：需求设计">
            </div>
            <div class="prop-group">
              <label class="prop-label">节点类型</label>
              <div class="node-type-line">
                <span class="node-type-badge" :class="nodeTypeClass(currentNode.node.nodeType)">{{ nodeTypeText(currentNode.node.nodeType) }}</span>
                <span class="node-type-note">按位置自动判定</span>
              </div>
            </div>
            <div class="prop-group">
              <label class="prop-label">节点提示</label>
              <input v-model="currentNode.node.nodeTips" class="prop-input" placeholder="处理人看到的提示文案">
            </div>
            <div class="prop-group">
              <label class="prop-label">节点填写说明</label>
              <textarea v-model="currentNode.node.guideText" class="prop-textarea" rows="4" placeholder="告诉处理人如何填写本节点（如：请填写需求编号与日期，并在附件上传需求文档）"></textarea>
            </div>
            <div class="prop-group">
              <label class="prop-label">说明文件</label>
              <AttachField v-model="guideFilesValue" :biz-id="guideBizId" />
              <div class="role-tip">上传节点说明文件（如需求模板.docx），处理人在该节点处理时可见</div>
            </div>
            <div class="prop-group">
              <label class="prop-label">下一步处理人提示</label>
              <textarea v-model="currentNode.node.nextHandlerTip" class="prop-textarea" rows="2" placeholder="告诉处理人提交后应交给谁（如：请选择需求负责人作为下一步处理人）"></textarea>
              <div class="role-tip">处理人提交本节点、选择下一处理人时展示</div>
            </div>
            <div v-if="currentNode.node.nodeType !== NODE_TYPE.END" class="prop-group">
              <label class="prop-label">条件分支</label>
              <div v-for="(br, bi) in branchForm.branches" :key="bi" class="branch-item">
                <div class="branch-head">
                  <span class="branch-title">分支 {{ bi + 1 }}</span>
                  <button class="enum-del" title="删除分支" @click="removeBranch(bi)"><i class="el-icon-close" /></button>
                </div>
                <div class="branch-conds">
                  <CondGroupEditor :group="br" :fields="currentFields" @change="onBranchChange" />
                </div>
                <div class="branch-target">
                  <span class="branch-target-label">流转到</span>
                  <select v-model="br.targetNodeId" class="cond-select" @change="onBranchChange">
                    <option value="">目标节点</option>
                    <option v-for="n in nodes" :key="n.node.id || n.node.nodeName" :value="n.node.id">{{ n.node.nodeName }}</option>
                  </select>
                </div>
              </div>
              <button class="btn-add-cond" @click="addBranch"><i class="el-icon-plus" /> 添加分支</button>
              <div v-if="branchForm.branches.length > 0" class="branch-default">
                <label class="cond-label">都不命中时流转到</label>
                <select v-model="branchForm.defaultNodeId" class="cond-select" @change="onBranchChange">
                  <option value="">（按顺序流转）</option>
                  <option v-for="n in nodes" :key="n.node.id || n.node.nodeName" :value="n.node.id">{{ n.node.nodeName }}</option>
                </select>
              </div>
              <div class="role-tip">仅数字/日期/单选/多选字段可作判断依据；按分支顺序匹配，同一分支内条件可按「且/或」任意嵌套组合（如 A且(B或C)），命中第一个分支流转到其目标节点；都不命中走默认目标；未配置则按顺序流转</div>
            </div>
            <p v-if="currentNode.node.nodeType === NODE_TYPE.END" class="prop-hint">结束节点提交后任务即完成，无需指定下一处理人</p>
          </div>
          </div>
        </div>

        <!-- 字段属性 -->
        <div class="prop-section">
          <div class="panel-title"><i class="el-icon-tickets" /> 字段属性</div>
          <div v-if="!selectedField" class="panel-empty">请选择一个字段进行配置</div>
          <div v-else class="prop-form">
            <div class="prop-group">
              <label class="prop-label">字段标签 <span class="req">*</span></label>
              <input v-model="selectedField.fieldLabel" class="prop-input" placeholder="字段中文名称">
            </div>
            <div class="prop-group">
              <label class="prop-label">占位提示</label>
              <input v-model="selectedField.placeholder" class="prop-input" placeholder="输入框占位文字">
            </div>
            <div class="prop-group">
              <label class="prop-label">帮助说明</label>
              <input v-model="selectedField.fieldTips" class="prop-input" placeholder="字段下方的提示文案">
            </div>
            <div class="prop-group prop-row">
              <label class="prop-label">是否必填</label>
              <el-switch v-model="selectedField.required" :active-value="1" :inactive-value="0" active-color="var(--color-primary)" />
            </div>

            <!-- 字段显隐条件 -->
            <div class="prop-group">
              <label class="prop-label">显隐条件</label>
              <CondGroupEditor :group="visibleCondForm" :fields="currentFields" @change="persistFieldVisibleCond" />
              <div class="role-tip">仅数字/日期/单选/多选字段可作判断依据；条件满足时才显示该字段，可「且/或」任意嵌套；无条件恒显示；隐藏的必填字段不参与必填校验</div>
            </div>

            <!-- 字段只读条件 -->
            <div class="prop-group">
              <label class="prop-label">只读条件</label>
              <CondGroupEditor :group="editableCondForm" :fields="currentFields" @change="persistFieldEditableCond" />
              <div class="role-tip">仅数字/日期/单选/多选字段可作判断依据；条件满足时该字段只读（不可编辑），可「且/或」任意嵌套；无条件恒可编辑</div>
            </div>

            <!-- 任务基础字段：填写方式（创建人填写 / 处理人填写） -->
            <div v-if="viewMode === 'template'" class="prop-group">
              <label class="prop-label">填写方式</label>
              <div class="field-role-select">
                <button
                  class="role-btn"
                  :class="{ active: selectedField.fieldRole !== 2 }"
                  @click="selectedField.fieldRole = 1"
                >创建人填写</button>
                <button
                  class="role-btn"
                  :class="{ active: selectedField.fieldRole === 2 }"
                  @click="selectedField.fieldRole = 2"
                >处理人填写</button>
              </div>
              <div class="role-tip">{{ selectedField.fieldRole === 2 ? '处理人处理指定节点时填写，随提交保存' : '下发任务时由创建人赋值' }}</div>
            </div>

            <!-- 处理人填写字段：绑定流程节点（仅该节点处理时显示填写） -->
            <div v-if="viewMode === 'template' && selectedField.fieldRole === 2" class="prop-group">
              <label class="prop-label">绑定流程节点 <span class="req">*</span></label>
              <el-select v-model="selectedField.bindNodeIndex" placeholder="选择填写该字段的流程节点" style="width: 100%">
                <el-option v-for="(n, ni) in nodes" :key="ni" :label="(ni + 1) + '. ' + n.node.nodeName" :value="ni" />
              </el-select>
              <div class="role-tip">仅当流程到达该节点时，处理人需要填写此任务基础字段</div>
            </div>

            <template v-if="['text', 'textarea'].includes(selectedField.fieldType)">
              <div class="prop-group">
                <label class="prop-label">最大长度</label>
                <input v-model.number="selectedField.maxLength" type="number" class="prop-input" placeholder="如 100">
              </div>
            </template>

            <template v-if="['radio', 'checkbox'].includes(selectedField.fieldType)">
              <div class="prop-group">
                <label class="prop-label">选项配置</label>
                <div class="enum-list">
                  <div v-for="(opt, oi) in enumOptions" :key="oi" class="enum-row">
                    <input v-model="opt.label" class="enum-input" placeholder="选项内容（存储值与显示一致）">
                    <button class="enum-del" @click="removeEnumOption(oi)"><i class="el-icon-close" /></button>
                  </div>
                </div>
                <button class="enum-add" @click="addEnumOption"><i class="el-icon-plus" /> 添加选项</button>
              </div>
            </template>

          </div>
        </div>
      </aside>
    </div>

    <!-- 保存流程弹窗（当前版本/新版本 + 改动说明） -->
    <SaveFlowModal
      :visible="saveVisible"
      :loading="saving"
      :template-version="template.version || 1"
      :usage="templateUsage"
      @confirm="handleSaveWithMode"
      @close="saveVisible = false"
    />

    <!-- 版本记录弹窗 -->
    <VersionListModal
      :visible="versionVisible"
      :template-id="templateId"
      :current-version="template"
      @close="versionVisible = false"
    />
  </div>
</template>

<script>
import { getTemplateDetail, getTemplateUsage, saveTemplateFlow, saveNodeGuideFiles } from '@/service/sys/TemplateService'
import { stableBizId } from '@/utils'
import { NODE_TYPE, NODE_TYPE_TEXT } from '@/constants/dict'
import SaveFlowModal from './components/SaveFlowModal.vue'
import VersionListModal from './components/VersionListModal.vue'
import AttachField from '@/components/AttachField'
import CondGroupEditor from './components/CondGroupEditor.vue'

export default {
  name: 'FormDesigner',
  components: { SaveFlowModal, VersionListModal, AttachField, CondGroupEditor },
  data() {
    return {
      // 模板中直接引用节点类型枚举（v-if 判断用 NODE_TYPE.END），需暴露到实例
      NODE_TYPE,
      templateId: null,
      templateName: '',
      template: {},
      nodes: [],
      /** 当前节点的条件分支编辑态（响应式，供属性面板 v-model/v-for 绑定；由 currentNode 切换时同步） */
      branchForm: { branches: [], defaultNodeId: '' },
      /** 选中字段的显隐/只读条件编辑态（递归表达式树，logic=and/or + children；由 selectedField 切换时同步） */
      visibleCondForm: { logic: 'and', children: [] },
      editableCondForm: { logic: 'and', children: [] },
      /** 模板级字段（不依附节点，如规章制度/采购说明等任务基础信息） */
      templateFields: [],
      templateFieldsSnapshot: [],
      // 打开模板时的创建人填写字段（fieldRole!=2）key 快照，用于保存后提示字段增删
      /** 画布模式：node=节点字段，template=任务基础字段 */
      viewMode: 'node',
      /** 右栏「节点属性」区块是否收起 */
      nodePropCollapsed: false,
      selectedNodeIndex: 0,
      selectedFieldIndex: -1,
      saving: false,
      // 保存/版本弹窗
      saveVisible: false,
      versionVisible: false,
      /** 模板被使用情况 { taskCount, dispatchCount }（保存流程前提示用户） */
      templateUsage: null,
      enumOptions: [],
      basicFields: [
        { type: 'text', label: '单行文本', icon: 'el-icon-edit' },
        { type: 'textarea', label: '多行文本', icon: 'el-icon-document' },
        { type: 'number', label: '数字', icon: 'el-icon-s-marketing' },
        { type: 'date', label: '日期', icon: 'el-icon-date' }
      ],
      advancedFields: [
        { type: 'radio', label: '单选枚举', icon: 'el-icon-circle-check' },
        { type: 'checkbox', label: '多选枚举', icon: 'el-icon-finished' },
        { type: 'user', label: '人员', icon: 'el-icon-user' },
        { type: 'dept', label: '部门', icon: 'el-icon-office-building' },
        { type: 'file', label: '文件上传', icon: 'el-icon-paperclip' },
        { type: 'image', label: '图片上传', icon: 'el-icon-picture' }
      ]
    }
  },
  computed: {
    /** 只读查看模式（样例模板且非超管：route.query.readonly=1） */
    isReadonly() {
      return Number(this.$route.query.readonly) === 1
    },
    allFieldTypes() {
      return [...this.basicFields, ...this.advancedFields]
    },
    currentNode() {
      return this.selectedNodeIndex >= 0 && this.selectedNodeIndex < this.nodes.length ? this.nodes[this.selectedNodeIndex] : null
    },
    currentFields() {
      if (this.viewMode === 'template') return this.templateFields
      return this.currentNode && this.currentNode.fields ? this.currentNode.fields : []
    },
    selectedField() {
      return this.selectedFieldIndex >= 0 && this.selectedFieldIndex < this.currentFields.length ? this.currentFields[this.selectedFieldIndex] : null
    },
    totalFieldCount() {
      return this.nodes.reduce((sum, n) => sum + ((n.fields || []).length), 0) + this.templateFields.length
    },
    /** 当前节点「说明文件」值（JSON 字符串，格式同 attach：[{attachId,fileName,ecsUrl,...}]） */
    guideFilesValue: {
      get() {
        const node = this.currentNode && this.currentNode.node
        return (node && node.guideFiles) || ''
      },
      set(val) {
        const node = this.currentNode && this.currentNode.node
        if (!node) return
        node.guideFiles = val || null
        // 已保存节点：即时持久化，避免刷新后说明文件丢失（新节点无 id，待保存流程后生效）
        this.persistNodeGuideFiles(node)
      }
    },
    /** 节点说明文件的业务 id（attach.biz_id 仅 varchar(32)，用稳定短码；模板级说明文件统一挂靠模板） */
    guideBizId() {
      return stableBizId('node-guide:' + (this.templateId || 'new'))
    }
  },
  watch: {
    'selectedField.fieldType'() { this.syncEnumFromField() },
    selectedField(val) {
      if (val && ['radio', 'checkbox'].includes(val.fieldType)) {
        this.syncEnumFromField()
      } else {
        this.enumOptions = []
      }
      this.syncFieldCondForm()
    },
    enumOptions: {
      deep: true,
      handler() { this.syncEnumToField() }
    },
    // 切换节点时，同步条件分支编辑态
    currentNode() { this.syncBranchForm() }
  },
  mounted() {
    this.templateId = this.$route.query.templateId
    this.templateName = this.$route.query.name || '流程设计器'
    if (this.templateId) this.fetchDetail()
    else this.initDefaultNodes()
  },
  methods: {
    nodeTypeText(t) {
      return NODE_TYPE_TEXT[t] || '中间'
    },
    nodeTypeClass(t) {
      return { [NODE_TYPE.START]: 'badge-start', [NODE_TYPE.MIDDLE]: 'badge-mid', [NODE_TYPE.END]: 'badge-end' }[t] || 'badge-mid'
    },
    typeLabel(type) {
      return this.allFieldTypes.find(f => f.type === type)?.label || type
    },
    /** 节点是否配置了条件分支（branchConfig 有非空 JSON 内容即视为已配置） */
    hasBranchConfig(cfg) {
      return !!cfg && typeof cfg === 'string' && cfg.trim().length > 0
    },
    /** 处理人填写字段绑定的节点名（bindNodeIndex 为节点链下标） */
    bindNodeName(field) {
      if (field.fieldRole !== 2) return ''
      if (field.bindNodeIndex === null || field.bindNodeIndex === undefined) return ''
      const n = this.nodes[field.bindNodeIndex]
      return n ? n.node.nodeName : ''
    },
    /** 任务基础字段填写方式悬停提示：处理人填写显示绑定节点，创建人填写不显示节点 */
    roleTip(field) {
      if (field.fieldRole === 2) {
        const nodeName = this.bindNodeName(field)
        return nodeName ? `在「${nodeName}」节点由处理人填写` : '由处理人填写'
      }
      return '创建人填写'
    },
    /** 根据位置自动修正节点类型：首位=开始(1)，末位=结束(3)，中间=2 */
    fixNodeTypes() {
      this.nodes.forEach((item, idx) => {
        if (idx === 0) {
          item.node.nodeType = 1
        } else if (idx === this.nodes.length - 1) {
          item.node.nodeType = 3
        } else {
          if (item.node.nodeType !== NODE_TYPE.MIDDLE) item.node.nodeType = NODE_TYPE.MIDDLE
        }
      })
    },
    /** 条件分支：从当前节点 branchConfig 同步到 branchForm（响应式编辑态） */
    syncBranchForm() {
      const node = this.currentNode && this.currentNode.node
      if (!node || !node.branchConfig) {
        this.branchForm = { branches: [], defaultNodeId: '' }
        return
      }
      try {
        const obj = JSON.parse(node.branchConfig)
        const branches = (obj.branches || []).map(br => {
          const root = asGroup(parseCondGroup(br))
          return { logic: root.logic, children: root.children, targetNodeId: (br && br.targetNodeId) || '' }
        })
        this.branchForm = { branches, defaultNodeId: obj.defaultNodeId || '' }
      } catch (e) {
        this.branchForm = { branches: [], defaultNodeId: '' }
      }
    },
    /** 条件分支：添加一条分支 */
    addBranch() {
      this.branchForm.branches.push({ logic: 'and', children: [{ fieldKey: '', op: 'eq', value: '' }], targetNodeId: '' })
      this.persistBranchConfig()
    },
    /** 条件分支：删除一条分支 */
    removeBranch(i) {
      this.branchForm.branches.splice(i, 1)
      this.persistBranchConfig()
    },
    /** 条件分支：把编辑结果序列化写回节点 branchConfig */
    onBranchChange() {
      this.persistBranchConfig()
    },
    persistBranchConfig() {
      const node = this.currentNode && this.currentNode.node
      if (!node) return
      const branches = this.branchForm.branches
        .map(br => {
          const cleaned = cleanGroup(br)
          return cleaned ? { logic: cleaned.logic, children: cleaned.children, targetNodeId: br.targetNodeId || '' } : null
        })
        .filter(Boolean)
      if (branches.length === 0 && !this.branchForm.defaultNodeId) {
        node.branchConfig = null
      } else {
        node.branchConfig = JSON.stringify({ branches, defaultNodeId: this.branchForm.defaultNodeId || '' })
      }
    },
    /** 字段显隐/只读条件：从选中字段同步到编辑态（递归表达式树） */
    syncFieldCondForm() {
      this.visibleCondForm = parseCondList(this.selectedField ? this.selectedField.visibleWhen : null)
      this.editableCondForm = parseCondList(this.selectedField ? this.selectedField.editableWhen : null)
    },
    /** 字段显隐条件：把编辑结果写回 visibleWhen */
    persistFieldVisibleCond() {
      if (this.selectedField) this.selectedField.visibleWhen = serializeCondList(this.visibleCondForm)
    },
    /** 字段只读条件：把编辑结果写回 editableWhen */
    persistFieldEditableCond() {
      if (this.selectedField) this.selectedField.editableWhen = serializeCondList(this.editableCondForm)
    },
    initDefaultNodes() {
      // 不自动创建节点：用户手动添加，首位自动标"开始"、末位自动标"结束"
      this.nodes = []
      this.selectedNodeIndex = -1
      this.selectedFieldIndex = -1
    },
    async fetchDetail() {
      try {
        const res = await getTemplateDetail(this.templateId)
        if (!res || res.code !== 200) return
        this.template = res.data.template || {}
        const nodes = res.data.nodes || []
        if (nodes.length === 0) {
          this.initDefaultNodes()
        } else {
          this.nodes = nodes.map(n => ({
            node: { ...n.node },
            fields: (n.fields || []).map(f => ({ ...f }))
          }))
        }
        // 模板级字段：bindNodeId（数据库节点ID）→ bindNodeIndex（节点链下标）
        this.templateFields = (res.data.templateFields || []).map(f => {
          const tf = { ...f }
          if (tf.fieldRole === 2 && tf.bindNodeId != null) {
            const ni = this.nodes.findIndex(n => n.node.id === tf.bindNodeId)
            if (ni >= 0) tf.bindNodeIndex = ni
          }
          return tf
        })
        this.templateFieldsSnapshot = this.templateFields.filter(f => f.fieldRole !== 2).map(f => ({ key: f.fieldKey || f.fieldLabel, label: f.fieldLabel }))
        if (this.$route.query.name == null && this.template.templateName) {
          this.templateName = this.template.templateName
        }
        this.selectedNodeIndex = 0
        this.selectedFieldIndex = -1
        this.syncBranchForm()
      } catch (e) {
        console.error('加载模板失败:', e)
        this.initDefaultNodes()
      }
    },
    selectNode(idx) {
      this.viewMode = 'node'
      this.selectedNodeIndex = idx
      this.selectedFieldIndex = -1
    },
    /** 切换到任务基础字段画布（模板级字段，不依附节点） */
    selectTemplateFields() {
      this.viewMode = 'template'
      this.selectedFieldIndex = -1
    },
    addNode() {
      // 空链或仅1节点时追加到末尾；2+节点时插到结束节点前
      const insertAt = this.nodes.length <= 1 ? this.nodes.length : this.nodes.length - 1
      this.nodes.splice(insertAt, 0, {
        node: {
          id: null,
          nodeName: '新节点',
          sortNum: insertAt,
          nodeType: 2,
          nodeTips: '',
          guideText: '',
          guideFiles: null,
          nextHandlerTip: '',
          branchConfig: null
        },
        fields: []
      })
      this.fixNodeTypes()
      this.selectedNodeIndex = insertAt
      this.selectedFieldIndex = -1
    },
    /** 复制节点（连同其字段配置），插到原节点之后（不越过结束节点），副本自动转为中间节点 */
    copyNode(idx) {
      const src = this.nodes[idx]
      const copyItem = {
        node: {
          id: null,
          nodeName: (src.node.nodeName || '未命名节点') + '（副本）',
          sortNum: 0,
          nodeType: 2,
          nodeTips: src.node.nodeTips || '',
          guideText: src.node.guideText || '',
          guideFiles: src.node.guideFiles || null,
          nextHandlerTip: src.node.nextHandlerTip || '',
          branchConfig: null
        },
        fields: (src.fields || []).map(f => ({
          id: null,
          nodeId: null,
          fieldRole: f.fieldRole || 1,
          bindNodeId: null,
          bindNodeIndex: null,
          fieldKey: 'field_' + Date.now().toString(36) + '_' + Math.floor(Math.random() * 1000),
          fieldLabel: f.fieldLabel,
          fieldType: f.fieldType,
          sortNum: f.sortNum,
          required: f.required || 0,
          placeholder: f.placeholder || '',
          fieldTips: f.fieldTips || '',
          maxLength: f.maxLength,
          enumOptions: f.enumOptions ? JSON.parse(JSON.stringify(f.enumOptions)) : null
        }))
      }
      // 原节点是结束节点时插到其之前，否则插到原节点之后
      const insertAt = Math.min(idx + 1, this.nodes.length - 1)
      this.nodes.splice(insertAt, 0, copyItem)
      this.fixNodeTypes()
      this.selectedNodeIndex = insertAt
      this.selectedFieldIndex = -1
      this.$message.success(`已复制节点「${copyItem.node.nodeName}」，可在右侧画布继续编辑字段`)
    },
    removeNode(idx) {
      if (this.nodes.length <= 2) {
        this.$message.warning('至少保留 2 个节点（首位为开始、末位为结束）')
        return
      }
      this.nodes.splice(idx, 1)
      this.fixNodeTypes()
      if (this.selectedNodeIndex >= this.nodes.length) this.selectedNodeIndex = this.nodes.length - 1
      this.selectedFieldIndex = -1
    },
    /** 删除节点前二次确认，防止误删 */
    confirmRemoveNode(idx) {
      this.$confirm(`确定删除节点「${this.nodes[idx].node.nodeName}」吗？该节点及其字段配置将一并移除。`, '删除节点确认', {
        confirmButtonText: '删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--primary'
      }).then(() => {
        this.removeNode(idx)
      }).catch(() => {})
    },
    moveNode(idx, dir) {
      // 首位（开始）/末位（结束）节点锁定不可移动，其余节点可自由调整位置
      if (idx === 0 || idx === this.nodes.length - 1) return
      const target = idx + dir
      if (target < 0 || target >= this.nodes.length) return
      const tmp = this.nodes[idx]
      this.$set(this.nodes, idx, this.nodes[target])
      this.$set(this.nodes, target, tmp)
      this.fixNodeTypes()
      this.selectedNodeIndex = target
    },
    addField(type) {
      const conf = this.allFieldTypes.find(f => f.type === type)
      const idx = this.currentFields.length + 1
      const newField = {
        id: null,
        nodeId: null,
        fieldRole: 1,
        bindNodeId: null,
        bindNodeIndex: null,
        fieldKey: 'field_' + Date.now().toString(36) + '_' + idx,
        fieldLabel: conf.label,
        fieldType: type,
        sortNum: this.currentFields.length,
        required: 0,
        placeholder: '',
        fieldTips: '',
        maxLength: type === 'text' ? 100 : (type === 'textarea' ? 500 : null),
        enumOptions: (type === 'radio' || type === 'checkbox') ? JSON.stringify([{ label: '选项1', value: '选项1' }, { label: '选项2', value: '选项2' }]) : null
      }
      this.currentFields.push(newField)
      this.selectedFieldIndex = this.currentFields.length - 1
    },
    moveField(idx, dir) {
      const target = idx + dir
      if (target < 0 || target >= this.currentFields.length) return
      const arr = this.currentFields
      const tmp = arr[idx]
      this.$set(arr, idx, arr[target])
      this.$set(arr, target, tmp)
      this.selectedFieldIndex = target
    },
    removeField(idx) {
      this.currentFields.splice(idx, 1)
      if (this.selectedFieldIndex >= this.currentFields.length) this.selectedFieldIndex = this.currentFields.length - 1
    },
    syncEnumFromField() {
      const f = this.selectedField
      if (f && f.enumOptions) {
        try { this.enumOptions = JSON.parse(f.enumOptions) || [] } catch (e) { this.enumOptions = [] }
      } else {
        this.enumOptions = []
      }
    },
    syncEnumToField() {
      const f = this.selectedField
      if (f && ['radio', 'checkbox'].includes(f.fieldType)) {
        // key（存储值）与 value（显示内容）一致，直接存选项文本，便于处理人回显
        f.enumOptions = JSON.stringify(this.enumOptions.map(o => ({ label: o.label, value: o.label })).filter(o => o.label))
      }
    },
    /** 新增枚举选项：存储值与显示内容一致，只填选项文本即可 */
    addEnumOption() {
      this.enumOptions.push({ label: '', value: '' })
    },
    /** 删除枚举选项 */
    removeEnumOption(oi) {
      this.enumOptions.splice(oi, 1)
    },
    /** 保存按钮：先校验，再查询模板被使用情况并弹出保存方式（当前版本/新版本 + 改动说明） */
    async onSaveClick() {
      if (this.isReadonly) {
        this.$message.warning('样例模板仅可查看，修改请先复制模板')
        return
      }
      if (this.nodes.length < 2) {
        this.$message.warning('至少需要 2 个节点（首位自动标记为开始、末位为结束）')
        return
      }
      // 校验节点名称
      for (let i = 0; i < this.nodes.length; i++) {
        if (!this.nodes[i].node.nodeName || !this.nodes[i].node.nodeName.trim()) {
          this.$message.warning(`第 ${i + 1} 个节点名称不能为空`)
          return
        }
      }
      // 校验任务基础字段标签
      for (const f of this.templateFields) {
        if (!f.fieldLabel || !f.fieldLabel.trim()) {
          this.$message.warning('任务基础字段的字段标签不能为空')
          return
        }
        // 处理人填写字段必须绑定流程节点
        if (f.fieldRole === 2 && (f.bindNodeIndex === null || f.bindNodeIndex === undefined || f.bindNodeIndex < 0)) {
          this.$message.warning(`任务基础字段「${f.fieldLabel}」需绑定流程节点（仅该节点处理时填写）`)
          return
        }
      }
      // 检索该模板已被哪些任务/期次使用，弹窗中明确告知修改不影响已下发期次
      try {
        const res = await getTemplateUsage(this.templateId)
        this.templateUsage = (res && res.data) || null
      } catch (e) {
        this.templateUsage = null
      }
      this.saveVisible = true
    },
    /** 保存弹窗确认：携带 saveMode/versionDesc 与节点填写说明执行保存 */
    async handleSaveWithMode({ saveMode, versionDesc }) {
      this.saveVisible = false
      this.fixNodeTypes()
      this.saving = true
      try {
        const payload = {
          templateId: this.templateId,
          saveMode: saveMode || 'current',
          versionDesc: versionDesc || '',
          templateFields: this.templateFields.map((f, k) => ({
            ...f,
            id: null,
            nodeId: null,
            sortNum: k,
            required: f.required || 0,
            // 处理人填写字段：以 bindNodeIndex（节点下标）传给后端，由后端映射为新节点ID
            bindNodeId: null,
            bindNodeIndex: f.fieldRole === 2 ? f.bindNodeIndex : null
          })),
          nodes: this.nodes.map((item, i) => ({
            node: {
              ...item.node,
              id: null,
              sortNum: i,
              nodeType: item.node.nodeType,
              guideText: item.node.guideText || '',
              guideFiles: item.node.guideFiles || null
            },
            fields: (item.fields || []).map((f, j) => ({
              ...f,
              id: null,
              nodeId: null,
              sortNum: j,
              required: f.required || 0
            }))
          }))
        }
        const res = await saveTemplateFlow(payload)
        if (!res || res.code !== 200) return
        this.$message.success(saveMode === 'new' ? '已保存为新版本 v' + (this.template.version + 1) : '流程保存成功')
        // 创建人填写字段增删提示（用字段名展示，弹窗加宽，关键提示加粗）：影响已绑定未同步任务的下发
        const beforeList = this.templateFieldsSnapshot || []
        const afterList = (this.templateFields || []).filter(f => f.fieldRole !== 2)
        const nameOf = f => f.label || f.fieldLabel || f.key || '未知字段'
        const added = afterList.filter(a => !beforeList.some(b => b.key === (a.fieldKey || a.fieldLabel)))
        const removed = beforeList.filter(b => !afterList.some(a => (a.fieldKey || a.fieldLabel) === b.key))
        if (added.length || removed.length) {
          const parts = []
          if (added.length) parts.push('新增：' + added.map(nameOf).join('、'))
          if (removed.length) parts.push('删除：' + removed.map(nameOf).join('、'))
          const html = '<div style="font-size:14px;line-height:1.9;color:#414755">本模板<b>「创建人填写」字段</b>发生变动：<br>' + parts.join('<br>') +
            '<br><br><b style="color:#B45309">已绑定该模板且尚未同步的任务：手动下发将被阻止、自动下发将跳过并通知创建人，直至在任务管理中打开任务核对并重新保存。</b></div>'
          this.$alert(html, '模板字段变更提醒', { type: 'warning', confirmButtonText: '知道了', dangerouslyUseHTMLString: true, customClass: 'tpl-change-alert' })
        }
        this.fetchDetail()
      } catch (e) {
        console.error(e)
      } finally {
        this.saving = false
      }
    },
    goBack() {
      this.$router.push('/flow-dispatch/flow-template')
    },
    /** 说明文件变更后即时持久化到节点（已保存节点直接更新，新节点等保存流程） */
    async persistNodeGuideFiles(node) {
      if (!node || !node.id) return
      try {
        await saveNodeGuideFiles(node.id, node.guideFiles || null)
      } catch (e) {
        console.error('保存说明文件失败:', e)
        this.$notifyError(null, '说明文件保存失败，请稍后重试')
      }
    }
  }
}

// 字段联动条件解析/序列化（递归表达式树，与后端 ConditionEvaluator.matchesAll 口径一致）
// 结构：叶子 {fieldKey,op,value}；分组 {logic:'and'|'or', children:[叶子|分组,...]}

// 递归解析节点：分组（children/conds）或叶子（fieldKey）
function parseCondGroup(o) {
  if (!o) return { logic: 'and', children: [] }
  let children = o.children
  if (!Array.isArray(children)) children = o.conds
  if (Array.isArray(children)) {
    return {
      logic: o.logic === 'or' ? 'or' : 'and',
      children: children.map(c => parseCondGroup(c))
    }
  }
  return { fieldKey: (o && o.fieldKey) || '', op: (o && o.op) || 'eq', value: o && o.value != null ? String(o.value) : '' }
}

// 把节点转成分组根（叶子包一层 and 组）
function asGroup(node) {
  if (node.children) return { logic: node.logic === 'or' ? 'or' : 'and', children: node.children }
  return { logic: 'and', children: [node] }
}

// 解析 JSON 字符串为分组根（永远返回 {logic, children}）
function parseCondList(json) {
  try {
    return asGroup(parseCondGroup(json ? JSON.parse(json) : null))
  } catch (e) {
    return { logic: 'and', children: [] }
  }
}

// 递归清理：剔除未配置字段的叶子与空子组，返回干净表达式树（无有效条件返回 null）
function cleanGroup(g) {
  if (!g || !Array.isArray(g.children)) return null
  const children = []
  for (const c of g.children) {
    if (c && c.children) {
      const sub = cleanGroup(c)
      if (sub) children.push(sub)
    } else if (c && c.fieldKey) {
      children.push({ fieldKey: c.fieldKey, op: c.op || 'eq', value: c.value != null ? String(c.value) : '' })
    }
  }
  if (children.length === 0) return null
  return { logic: g.logic === 'or' ? 'or' : 'and', children }
}

function serializeCondList(form) {
  const cleaned = cleanGroup(form)
  return cleaned ? JSON.stringify(cleaned) : null
}
</script>

<style lang="scss" scoped>
$primary: var(--color-primary);
$border: #CBD5E1;
.designer-container { display: flex; flex-direction: column; height: calc(100vh - 64px); background: var(--color-primary-surface);  overflow: hidden; }

// 顶部
.designer-header { display: flex; justify-content: space-between; align-items: center; padding: 0 24px; height: 60px; background: #fff; border-bottom: 1px solid $border; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 16px; }
.back-btn { display: flex; align-items: center; gap: 4px; padding: 6px 12px; background: transparent; border: 1px solid $border; border-radius: 2px; color: var(--color-primary); cursor: pointer; font-size: 13px;
  &:hover { background: var(--color-primary-light); }
}
.tpl-info { display: flex; align-items: center; gap: 8px; }
.tpl-name { font-size: 16px; font-weight: 600; color: #1b1c1c; }
.tpl-cat { padding: 2px 8px; background: rgba(var(--color-primary-rgb),0.1); color: $primary; border-radius: 4px; font-size: 12px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.field-count { font-size: 13px; color: #757575; }
.tfe-count { color: $primary; margin-left: 4px; }
.btn-versions { display: flex; align-items: center; gap: 4px; padding: 8px 14px; background: transparent; border: 1px solid $border; border-radius: 2px; color: var(--color-primary); font-size: 13px; cursor: pointer;
  &:hover { border-color: $primary; color: $primary; background: var(--color-primary-light); }
}
.btn-save { display: flex; align-items: center; gap: 4px; padding: 8px 20px; background: $primary; color: #fff; border: none; border-radius: 2px; font-weight: 600; font-size: 13px; cursor: pointer;
  &:hover { opacity: 0.9; } &:disabled { opacity: 0.6; cursor: not-allowed; }
}

// 任务基础字段入口
.tfe-entry { display: flex; align-items: center; gap: 10px; margin: 12px 0 16px; padding: 12px; border: 1px dashed #cbd5e0; border-radius: 3px; cursor: pointer; transition: all 0.2s; flex-shrink: 0;
  i { font-size: 20px; color: $primary; }
  // hover 仅轻微提示，避免与选中态混淆
  &:hover { background: #f7f8fa; border-color: #cbd5e0; }
  // 选中态才显示红色系
  &.active { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 0 0 1px $primary;
    i { color: $primary; }
  }
}
.tfe-entry-info { display: flex; flex-direction: column; gap: 2px; }
.tfe-entry-title { font-size: 13px; font-weight: 700; color: #1b1c1c; }
.tfe-entry-desc { font-size: 11px; color: #999; }
.tfe-tip { font-size: 12px; color: #999; line-height: 1.7; padding: 8px 0; }
.badge-tpl { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
.field-role-select { display: flex; gap: 8px; }
.role-btn { flex: 1; padding: 6px 0; border: 1px solid #ddd; border-radius: 2px; background: #fff; color: #555; font-size: 13px; cursor: pointer;
  &.active { border-color: $primary; color: $primary; background: rgba(var(--color-primary-rgb),0.06); font-weight: 600; }
}
.role-tip { font-size: 11px; color: #999; margin-top: 5px; }

// 三栏
.designer-body { flex: 1; display: grid; grid-template-columns: 380px 1fr 640px; gap: 0; overflow: hidden; }

// 左栏：节点链
.node-chain { background: #fff; border-right: 1px solid $border; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; }
.panel-title { font-size: 15px; font-weight: 700; color: #1b1c1c; margin-bottom: 4px; }
.panel-desc { font-size: 12px; color: #999; margin-bottom: 16px; }
.chain-list { flex: 1; overflow-y: auto; }
.node-item { padding: 12px; border: 1px solid #e4e7ed; border-radius: 3px; margin-bottom: 0; cursor: pointer; transition: all 0.2s; position: relative;
  // hover 仅轻微提示，避免与选中态混淆
  &:hover { background: #f7f8fa; }
  &.active { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 0 0 1px $primary; }
}
.node-item-head { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.node-idx { width: 20px; height: 20px; border-radius: 50%; background: $primary; color: #fff; font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.node-name { font-size: 14px; font-weight: 600; color: #1b1c1c; flex: 1; }
.node-type-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; flex-shrink: 0; }
.badge-start { background: rgba(21, 128, 61,0.1); color: #15803D; }
.badge-mid { background: #f0f3ff; color: #545f72; }
.badge-end { background: rgba(var(--color-primary-rgb),0.1); color: $primary; }
.node-item-meta { display: flex; gap: 10px; font-size: 12px; color: #999; flex-wrap: wrap;
  i { margin-right: 2px; }
}
.assign-tag { color: $primary; }
.node-item-actions { display: flex; gap: 4px; margin-top: 8px; padding-top: 8px; border-top: 1px dashed #eef0f2; }
.chain-arrow { text-align: center; color: #c0c4cc; padding: 4px 0; font-size: 14px; }
.chain-empty { text-align: center; padding: 40px 12px; color: #ccc;
  i { font-size: 40px; display: block; margin-bottom: 10px; }
  p { margin: 4px 0; font-size: 13px; color: #999; }
  .empty-tip { font-size: 12px; color: #bbb; }
}
.btn-add-node { margin-top: 12px; padding: 10px; border: 1px dashed #cbd5e0; background: transparent; border-radius: 2px; cursor: pointer; color: $primary; font-size: 13px; font-weight: 600; flex-shrink: 0;
  &:hover { border-color: $primary; background: var(--color-primary-light); }
}

// 中栏
.canvas-area { display: flex; flex-direction: column; padding: 16px 24px; overflow: hidden; }
.canvas-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.canvas-title { font-size: 15px; font-weight: 700; color: #1b1c1c; display: flex; align-items: center; gap: 8px; }
.global-tips { font-size: 12px; color: #757575; display: flex; align-items: center; gap: 4px; max-width: 50%; }

// 字段库条
.field-library-bar { background: #fff; border: 1px solid $border; border-radius: 3px; padding: 10px 12px; margin-bottom: 12px; }
.lib-label { font-size: 12px; color: #999; display: block; margin-bottom: 8px; }
.field-cards { display: flex; flex-wrap: wrap; gap: 8px; }
.field-card { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border: 1px dashed #cbd5e0; border-radius: 2px; cursor: pointer; font-size: 12px; color: #414755; transition: all 0.2s;
  i { color: $primary; font-size: 14px; }
  &:hover { border-color: $primary; background: var(--color-primary-light); color: $primary; }
}

.canvas-list { flex: 1; overflow-y: auto; background: #fff; border: 1px solid $border; border-radius: 3px; padding: 16px; }
.field-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border: 1px solid #e4e7ed; border-radius: 2px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s;
  &:hover { border-color: $primary; }
  &.active { border-color: $primary; background: var(--color-primary-light); box-shadow: 0 0 0 1px $primary; }
}
.field-item-main { flex: 1; }
.field-item-label { font-size: 14px; font-weight: 600; color: #1b1c1c; display: flex; align-items: center; gap: 6px; }
.req { color: $primary; font-weight: 700; }
.field-type-tag { padding: 1px 6px; background: #f0f3ff; color: #545f72; border-radius: 3px; font-size: 11px; font-weight: 500; }
// 节点/字段高级配置标记（条件分支/显隐/只读）
.cfg-tag { display: inline-flex; align-items: center; gap: 2px; padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 500; line-height: 16px; flex-shrink: 0;
  i { font-size: 11px; }
}
.tag-branch { background: rgba(21, 128, 61, 0.12); color: #15803D; } // 条件分支（绿）
.tag-visible { background: rgba(51, 65, 85, 0.1); color: $primary; } // 显隐条件（主题蓝）
.tag-editable { background: rgba(71, 85, 105, 0.14); color: #475569; } // 只读条件（深灰）
.field-item-ph { font-size: 12px; color: #999; margin-top: 4px; }
.field-item-actions { display: flex; gap: 4px; align-items: center; }
.role-hint-icon { width: 22px; height: 22px; border-radius: 4px; display: flex; align-items: center; justify-content: center; cursor: help; font-size: 13px;
  &.role-creator { background: rgba(21, 128, 61,0.1); color: #15803D; }
  &.role-handler { background: rgba(180, 83, 9,0.12); color: #B45309; }
}
.op-btn { width: 28px; height: 28px; border: 1px solid #e4e7ed; background: #fff; border-radius: 4px; cursor: pointer; color: #606266; display: flex; align-items: center; justify-content: center;
  &:hover:not(:disabled) { border-color: $primary; color: $primary; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.op-del:hover:not(:disabled) { border-color: #DC2626; color: #DC2626; }
.canvas-empty { text-align: center; padding: 50px 0; color: #ccc;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { margin: 4px 0; font-size: 14px; color: #999; }
  .empty-tip { font-size: 12px; color: #bbb; }
}

// 右栏
.property-panel { background: #f5f6f8; border-left: 1px solid $border; overflow-y: auto; padding: 14px; box-sizing: border-box; }
.prop-section { background: #fff; border: 1px solid #f0e8e7; border-radius: 3px; padding: 16px 18px; margin-bottom: 14px; box-shadow: 0 1px 3px rgba(0,0,0,0.03);
  &:last-child { margin-bottom: 0; }
}
.panel-title { font-size: 15px; font-weight: 700; color: #1b1c1c; display: flex; align-items: center; gap: 7px; border-left: 3px solid $primary; padding-left: 9px;
  i { color: $primary; font-size: 15px; }
}
.panel-title-click { cursor: pointer; user-select: none;
  &:hover { color: $primary; }
}
.collapse-arrow { margin-left: auto; color: #94A3B8; font-size: 16px; transition: transform 0.25s ease; cursor: pointer;
  &.open { transform: rotate(180deg); color: var(--color-primary); }
  &:hover { color: var(--color-primary); }
}
.panel-empty { text-align: center; padding: 30px 0; color: #ccc;
  p { font-size: 13px; color: #999; margin-top: 8px; }
}
.prop-form { display: flex; flex-direction: column; gap: 14px; margin-top: 14px; }
.prop-group { display: flex; flex-direction: column; gap: 5px; }
.prop-label { font-size: 13px; color: #4a4f58; font-weight: 600; }
.prop-input { height: 36px; border: 1px solid #dcdfe6; border-radius: 2px; padding: 0 10px; font-size: 13px; outline: none; transition: all 0.2s; background: #fff;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.12); }
  &:hover { border-color: #c0c4cc; }
}
.prop-textarea { width: 100%; border: 1px solid #dcdfe6; border-radius: 2px; padding: 8px 10px; font-size: 13px; font-family: inherit; line-height: 1.6; resize: vertical; outline: none; box-sizing: border-box; transition: all 0.2s; background: #fff;
  &:focus { border-color: $primary; box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb),0.12); }
  &:hover { border-color: #c0c4cc; }
}
.node-type-line { display: flex; align-items: center; gap: 10px; padding: 4px 0; }
.node-type-note { font-size: 12px; color: #999; }
.guide-files-editor { display: flex; flex-direction: column; gap: 6px; }
.guide-file-row { display: flex; align-items: center; gap: 6px;
  i { color: #909399; font-size: 13px; flex-shrink: 0; }
}
.gf-input { flex: 1; }
.gf-del { width: 26px; height: 26px; border: 1px solid #dcdfe6; background: #fff; border-radius: 4px; cursor: pointer; color: #DC2626; display: flex; align-items: center; justify-content: center; flex-shrink: 0;
  &:hover { border-color: #DC2626; }
}
.gf-add { padding: 6px; border: 1px dashed #cbd5e0; background: transparent; border-radius: 4px; cursor: pointer; color: $primary; font-size: 12px; text-align: center;
  &:hover { border-color: $primary; background: var(--color-primary-light); }
}
.prop-row { flex-direction: row; align-items: center; justify-content: space-between; }
.prop-hint { font-size: 12px; color: $primary; margin: 0; }
.enum-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 6px; }
.enum-row { display: flex; gap: 4px; }
.enum-input { flex: 1; height: 28px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 6px; font-size: 12px; outline: none;
  &:focus { border-color: $primary; }
}
.enum-del { width: 28px; height: 28px; border: 1px solid #dcdfe6; background: #fff; border-radius: 4px; cursor: pointer; color: #DC2626; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.enum-add { width: 100%; padding: 6px; border: 1px dashed #cbd5e0; background: transparent; border-radius: 4px; cursor: pointer; color: $primary; font-size: 12px;
  &:hover { border-color: $primary; background: var(--color-primary-light); }
}

// 条件分支
.branch-item {
  border: 1px solid #e8edf3;
  border-radius: 8px;
  padding: 10px 12px;
  margin-bottom: 10px;
  background: #fbfcfe;
  transition: all 0.15s;
  &:hover { border-color: #d8dee8; }
}
.branch-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 6px;
}
.branch-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-primary);
}
.branch-conds { display: flex; flex-direction: column; gap: 4px; }
.branch-target {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #e2e8f0;
}
.branch-target-label {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
}
.cond-select {
  height: 30px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
  background: #fff;
  min-width: 0;
  transition: all 0.15s;
  &:hover { border-color: #c0c4cc; }
  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb), 0.12);
  }
}
.branch-target .cond-select { flex: 1; }
.btn-add-cond {
  width: 100%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 7px;
  border: 1px dashed #cbd5e0;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: var(--color-primary);
  font-size: 12px;
  margin-bottom: 8px;
  transition: all 0.15s;
  &:hover { border-color: var(--color-primary); background: var(--color-primary-light); }
}
.branch-default {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  background: #f6f8fa;
  border-radius: 6px;
}
.cond-label {
  font-size: 12px;
  color: #757575;
  white-space: nowrap;
}

.designer-readonly-tip { display: flex; align-items: center; gap: 6px; padding: 8px 16px; background: rgba(180, 83, 9, 0.1); color: #B45309; font-size: 13px; border-bottom: 1px solid rgba(180, 83, 9, 0.2); }
.readonly-tag { display: inline-flex; align-items: center; gap: 5px; padding: 6px 14px; border-radius: 3px; background: #F1F3F6; color: #8a93a5; font-size: 13px; font-weight: 600; }
</style>
