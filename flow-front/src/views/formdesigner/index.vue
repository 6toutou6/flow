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
        <button class="btn-save" :disabled="saving" @click="handleSave">
          <i v-if="saving" class="el-icon-loading" />
          <i v-else class="el-icon-check" />
          保存流程
        </button>
      </div>
    </header>

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
              </div>
              <div class="node-item-actions">
                <button class="op-btn" :disabled="idx === 0" title="上移" @click.stop="selectNode(idx); moveNode(idx, -1)"><i class="el-icon-top" /></button>
                <button class="op-btn" :disabled="idx === nodes.length - 1" title="下移" @click.stop="selectNode(idx); moveNode(idx, 1)"><i class="el-icon-bottom" /></button>
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
          <div class="panel-title">{{ viewMode === 'template' ? '任务基础字段说明' : '节点属性' }}</div>
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
              <div class="node-type-readonly">{{ nodeTypeText(currentNode.node.nodeType) }}（按位置自动判定）</div>
            </div>
            <div class="prop-group">
              <label class="prop-label">节点提示</label>
              <input v-model="currentNode.node.nodeTips" class="prop-input" placeholder="处理人看到的提示文案">
            </div>
            <p v-if="currentNode.node.nodeType === 3" class="prop-hint">结束节点提交后任务即完成，无需指定下一处理人</p>
          </div>
        </div>

        <!-- 字段属性 -->
        <div class="prop-section">
          <div class="panel-title">字段属性</div>
          <div v-if="!selectedField" class="panel-empty">请选择一个字段进行配置</div>
          <div v-else class="prop-form">
            <div class="prop-group">
              <label class="prop-label">字段标签 <span class="req">*</span></label>
              <input v-model="selectedField.fieldLabel" class="prop-input" placeholder="字段中文名称">
            </div>
            <div class="prop-group">
              <label class="prop-label">字段标识</label>
              <input v-model="selectedField.fieldKey" class="prop-input" placeholder="英文标识，如 name">
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
              <el-switch v-model="selectedField.required" :active-value="1" :inactive-value="0" active-color="#C53030" />
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
                    <input v-model="opt.label" class="enum-input" placeholder="显示名称">
                    <input v-model="opt.value" class="enum-input enum-value" placeholder="值">
                    <button class="enum-del" @click="enumOptions.splice(oi, 1)"><i class="el-icon-close" /></button>
                  </div>
                </div>
                <button class="enum-add" @click="enumOptions.push({ label: '', value: '' })"><i class="el-icon-plus" /> 添加选项</button>
              </div>
            </template>

          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script>
import { getTemplateDetail, saveTemplateFlow } from '@/api/template'

export default {
  name: 'FormDesigner',
  data() {
    return {
      templateId: null,
      templateName: '',
      template: {},
      nodes: [],
      /** 模板级字段（不依附节点，如规章制度/采购说明等任务基础信息） */
      templateFields: [],
      /** 画布模式：node=节点字段，template=任务基础字段 */
      viewMode: 'node',
      selectedNodeIndex: 0,
      selectedFieldIndex: -1,
      saving: false,
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
        { type: 'file', label: '文件上传', icon: 'el-icon-paperclip' },
        { type: 'image', label: '图片上传', icon: 'el-icon-picture' }
      ]
    }
  },
  computed: {
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
    },
    enumOptions: {
      deep: true,
      handler() { this.syncEnumToField() }
    }
  },
  mounted() {
    this.templateId = this.$route.query.templateId
    this.templateName = this.$route.query.name || '流程设计器'
    if (this.templateId) this.fetchDetail()
    else this.initDefaultNodes()
  },
  methods: {
    nodeTypeText(t) {
      return { 1: '开始', 2: '中间', 3: '结束' }[t] || '中间'
    },
    nodeTypeClass(t) {
      return { 1: 'badge-start', 2: 'badge-mid', 3: 'badge-end' }[t] || 'badge-mid'
    },
    typeLabel(type) {
      return this.allFieldTypes.find(f => f.type === type)?.label || type
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
          if (item.node.nodeType !== 2) item.node.nodeType = 2
        }
      })
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
        if (this.$route.query.name == null && this.template.templateName) {
          this.templateName = this.template.templateName
        }
        this.selectedNodeIndex = 0
        this.selectedFieldIndex = -1
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
          nodeTips: ''
        },
        fields: []
      })
      this.fixNodeTypes()
      this.selectedNodeIndex = insertAt
      this.selectedFieldIndex = -1
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
        confirmButtonClass: 'el-button--danger'
      }).then(() => {
        this.removeNode(idx)
      }).catch(() => {})
    },
    moveNode(idx, dir) {
      const target = idx + dir
      // 不允许把节点移到首位之前或末位之后；首位/末位固定为开始/结束
      if (target <= 0 || target >= this.nodes.length - 1) return
      if (idx === 0 || idx === this.nodes.length - 1) return
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
        enumOptions: (type === 'radio' || type === 'checkbox') ? JSON.stringify([{ label: '选项1', value: '1' }, { label: '选项2', value: '2' }]) : null
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
        f.enumOptions = JSON.stringify(this.enumOptions.filter(o => o.label || o.value))
      }
    },
    async handleSave() {
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
      this.fixNodeTypes()
      this.saving = true
      try {
        const payload = {
          templateId: Number(this.templateId),
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
              nodeType: item.node.nodeType
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
        await saveTemplateFlow(payload)
        this.$message.success('流程保存成功')
        this.fetchDetail()
      } catch (e) {
        console.error(e)
      } finally {
        this.saving = false
      }
    },
    goBack() {
      this.$router.push('/flow-template/index')
    }
  }
}
</script>

<style lang="scss" scoped>
$primary: #C53030;
$border: #e4beba;
.designer-container { display: flex; flex-direction: column; height: calc(100vh - 64px); background: #F5F7FA; font-family: 'Inter', sans-serif; overflow: hidden; }

// 顶部
.designer-header { display: flex; justify-content: space-between; align-items: center; padding: 0 24px; height: 60px; background: #fff; border-bottom: 1px solid $border; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 16px; }
.back-btn { display: flex; align-items: center; gap: 4px; padding: 6px 12px; background: transparent; border: 1px solid $border; border-radius: 6px; color: #5b403d; cursor: pointer; font-size: 13px;
  &:hover { background: #f6f3f2; }
}
.tpl-info { display: flex; align-items: center; gap: 8px; }
.tpl-name { font-size: 16px; font-weight: 600; color: #1b1c1c; }
.tpl-cat { padding: 2px 8px; background: rgba(197,48,48,0.1); color: $primary; border-radius: 4px; font-size: 12px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.field-count { font-size: 13px; color: #757575; }
.tfe-count { color: $primary; margin-left: 4px; }
.btn-save { display: flex; align-items: center; gap: 4px; padding: 8px 20px; background: $primary; color: #fff; border: none; border-radius: 6px; font-weight: 600; font-size: 13px; cursor: pointer;
  &:hover { opacity: 0.9; } &:disabled { opacity: 0.6; cursor: not-allowed; }
}

// 任务基础字段入口
.tfe-entry { display: flex; align-items: center; gap: 10px; margin: 12px 0 16px; padding: 12px; border: 1px dashed #cbd5e0; border-radius: 8px; cursor: pointer; transition: all 0.2s; flex-shrink: 0;
  i { font-size: 20px; color: $primary; }
  // hover 仅轻微提示，避免与选中态混淆
  &:hover { background: #f7f8fa; border-color: #cbd5e0; }
  // 选中态才显示红色系
  &.active { border-color: $primary; background: #FFF5F5; box-shadow: 0 0 0 1px $primary;
    i { color: $primary; }
  }
}
.tfe-entry-info { display: flex; flex-direction: column; gap: 2px; }
.tfe-entry-title { font-size: 13px; font-weight: 700; color: #1b1c1c; }
.tfe-entry-desc { font-size: 11px; color: #999; }
.tfe-tip { font-size: 12px; color: #999; line-height: 1.7; padding: 8px 0; }
.badge-tpl { background: rgba(197,48,48,0.1); color: $primary; }
.field-role-select { display: flex; gap: 8px; }
.role-btn { flex: 1; padding: 6px 0; border: 1px solid #ddd; border-radius: 5px; background: #fff; color: #555; font-size: 13px; cursor: pointer;
  &.active { border-color: $primary; color: $primary; background: rgba(197,48,48,0.06); font-weight: 600; }
}
.role-tip { font-size: 11px; color: #999; margin-top: 5px; }

// 三栏
.designer-body { flex: 1; display: grid; grid-template-columns: 280px 1fr 320px; gap: 0; overflow: hidden; }

// 左栏：节点链
.node-chain { background: #fff; border-right: 1px solid $border; padding: 16px; overflow-y: auto; display: flex; flex-direction: column; }
.panel-title { font-size: 15px; font-weight: 700; color: #1b1c1c; margin-bottom: 4px; }
.panel-desc { font-size: 12px; color: #999; margin-bottom: 16px; }
.chain-list { flex: 1; overflow-y: auto; }
.node-item { padding: 12px; border: 1px solid #e4e7ed; border-radius: 8px; margin-bottom: 0; cursor: pointer; transition: all 0.2s; position: relative;
  // hover 仅轻微提示，避免与选中态混淆
  &:hover { background: #f7f8fa; }
  &.active { border-color: $primary; background: #FFF5F5; box-shadow: 0 0 0 1px $primary; }
}
.node-item-head { display: flex; align-items: center; gap: 6px; margin-bottom: 6px; }
.node-idx { width: 20px; height: 20px; border-radius: 50%; background: $primary; color: #fff; font-size: 11px; font-weight: 700; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.node-name { font-size: 14px; font-weight: 600; color: #1b1c1c; flex: 1; }
.node-type-badge { padding: 1px 6px; border-radius: 3px; font-size: 11px; font-weight: 600; flex-shrink: 0; }
.badge-start { background: rgba(38,109,0,0.1); color: #266d00; }
.badge-mid { background: #f0f3ff; color: #545f72; }
.badge-end { background: rgba(197,48,48,0.1); color: $primary; }
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
.btn-add-node { margin-top: 12px; padding: 10px; border: 1px dashed #cbd5e0; background: transparent; border-radius: 6px; cursor: pointer; color: $primary; font-size: 13px; font-weight: 600; flex-shrink: 0;
  &:hover { border-color: $primary; background: #FFF5F5; }
}

// 中栏
.canvas-area { display: flex; flex-direction: column; padding: 16px 24px; overflow: hidden; }
.canvas-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.canvas-title { font-size: 15px; font-weight: 700; color: #1b1c1c; display: flex; align-items: center; gap: 8px; }
.global-tips { font-size: 12px; color: #757575; display: flex; align-items: center; gap: 4px; max-width: 50%; }

// 字段库条
.field-library-bar { background: #fff; border: 1px solid $border; border-radius: 8px; padding: 10px 12px; margin-bottom: 12px; }
.lib-label { font-size: 12px; color: #999; display: block; margin-bottom: 8px; }
.field-cards { display: flex; flex-wrap: wrap; gap: 8px; }
.field-card { display: flex; align-items: center; gap: 4px; padding: 6px 12px; border: 1px dashed #cbd5e0; border-radius: 6px; cursor: pointer; font-size: 12px; color: #414755; transition: all 0.2s;
  i { color: $primary; font-size: 14px; }
  &:hover { border-color: $primary; background: #FFF5F5; color: $primary; }
}

.canvas-list { flex: 1; overflow-y: auto; background: #fff; border: 1px solid $border; border-radius: 8px; padding: 16px; }
.field-item { display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; border: 1px solid #e4e7ed; border-radius: 6px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s;
  &:hover { border-color: $primary; }
  &.active { border-color: $primary; background: #FFF5F5; box-shadow: 0 0 0 1px $primary; }
}
.field-item-main { flex: 1; }
.field-item-label { font-size: 14px; font-weight: 600; color: #1b1c1c; display: flex; align-items: center; gap: 6px; }
.req { color: $primary; font-weight: 700; }
.field-type-tag { padding: 1px 6px; background: #f0f3ff; color: #545f72; border-radius: 3px; font-size: 11px; font-weight: 500; }
.field-item-ph { font-size: 12px; color: #999; margin-top: 4px; }
.field-item-actions { display: flex; gap: 4px; align-items: center; }
.role-hint-icon { width: 22px; height: 22px; border-radius: 4px; display: flex; align-items: center; justify-content: center; cursor: help; font-size: 13px;
  &.role-creator { background: rgba(38,109,0,0.1); color: #266d00; }
  &.role-handler { background: rgba(183,121,31,0.12); color: #b7791f; }
}
.op-btn { width: 28px; height: 28px; border: 1px solid #e4e7ed; background: #fff; border-radius: 4px; cursor: pointer; color: #606266; display: flex; align-items: center; justify-content: center;
  &:hover:not(:disabled) { border-color: $primary; color: $primary; }
  &:disabled { opacity: 0.4; cursor: not-allowed; }
}
.op-del:hover:not(:disabled) { border-color: #ba1a1a; color: #ba1a1a; }
.canvas-empty { text-align: center; padding: 50px 0; color: #ccc;
  i { font-size: 48px; display: block; margin-bottom: 12px; }
  p { margin: 4px 0; font-size: 14px; color: #999; }
  .empty-tip { font-size: 12px; color: #bbb; }
}

// 右栏
.property-panel { background: #fff; border-left: 1px solid $border; overflow-y: auto; }
.prop-section { padding: 16px; border-bottom: 1px solid #f0f0f0;
  &:last-child { border-bottom: none; }
}
.panel-empty { text-align: center; padding: 30px 0; color: #ccc;
  p { font-size: 13px; color: #999; margin-top: 8px; }
}
.prop-form { display: flex; flex-direction: column; gap: 12px; margin-top: 12px; }
.prop-group { display: flex; flex-direction: column; gap: 4px; }
.prop-label { font-size: 12px; color: #757575; font-weight: 600; }
.prop-input { height: 32px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 8px; font-size: 13px; outline: none; transition: all 0.2s;
  &:focus { border-color: $primary; box-shadow: 0 0 0 1px rgba(197,48,48,0.2); }
}
.prop-row { flex-direction: row; align-items: center; justify-content: space-between; }
.node-type-readonly { font-size: 13px; color: #606266; padding: 6px 0; }
.prop-hint { font-size: 12px; color: $primary; margin: 0; }
.enum-list { display: flex; flex-direction: column; gap: 6px; margin-bottom: 6px; }
.enum-row { display: flex; gap: 4px; }
.enum-input { flex: 1; height: 28px; border: 1px solid #dcdfe6; border-radius: 4px; padding: 0 6px; font-size: 12px; outline: none;
  &:focus { border-color: $primary; }
}
.enum-value { max-width: 80px; }
.enum-del { width: 28px; height: 28px; border: 1px solid #dcdfe6; background: #fff; border-radius: 4px; cursor: pointer; color: #ba1a1a; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.enum-add { width: 100%; padding: 6px; border: 1px dashed #cbd5e0; background: transparent; border-radius: 4px; cursor: pointer; color: $primary; font-size: 12px;
  &:hover { border-color: $primary; background: #FFF5F5; }
}
</style>
