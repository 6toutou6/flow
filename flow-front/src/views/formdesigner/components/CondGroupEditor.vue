<template>
  <div class="cg-root">
    <!-- 逻辑切换（≥2 子项时显示） -->
    <div v-if="group.children.length >= 2" class="cg-logic">
      <span class="cg-logic-label">满足</span>
      <div class="cg-seg">
        <button
          class="cg-seg-btn"
          :class="{ active: group.logic !== 'or' }"
          @click="setLogic('and')"
        >且 · 全部</button>
        <button
          class="cg-seg-btn"
          :class="{ active: group.logic === 'or' }"
          @click="setLogic('or')"
        >或 · 任一</button>
      </div>
    </div>

    <div v-for="(child, i) in group.children" :key="i" class="cg-item">
      <!-- 连接词 -->
      <div v-if="i > 0" class="cg-joiner">
        <span class="cg-joiner-line" />
        <span class="cg-joiner-tag" :class="group.logic === 'or' ? 'is-or' : 'is-and'">
          {{ group.logic === 'or' ? '或' : '且' }}
        </span>
        <span class="cg-joiner-line" />
      </div>

      <!-- 嵌套子组 -->
      <div v-if="child.children" class="cg-sub">
        <div class="cg-sub-head">
          <i class="el-icon-files cg-sub-icon" />
          <span class="cg-sub-title">条件组</span>
          <button class="cg-del" title="删除条件组" @click="removeChild(i)"><i class="el-icon-close" /></button>
        </div>
        <CondGroupEditor :group="child" :fields="fields" @change="$emit('change')" />
      </div>

      <!-- 叶子条件 -->
      <div v-else class="cg-leaf">
        <select v-model="child.fieldKey" class="cg-select cg-field" @change="onFieldChange(child)">
          <option value="">选择字段</option>
          <option v-for="f in condFields" :key="f.fieldKey" :value="f.fieldKey">{{ f.fieldLabel }}</option>
        </select>
        <select v-model="child.op" class="cg-select cg-op" @change="$emit('change')">
          <option v-for="op in opOptions(child)" :key="op" :value="op">{{ COND_OP_TEXT[op] }}</option>
        </select>
        <input
          v-if="!['empty', 'notempty'].includes(child.op)"
          v-model="child.value"
          class="cg-input"
          placeholder="值"
          @change="$emit('change')"
        >
        <button class="cg-del" title="删除条件" @click="removeChild(i)"><i class="el-icon-close" /></button>
      </div>
    </div>

    <div class="cg-actions">
      <button class="cg-add" @click="addCond"><i class="el-icon-plus" /> 添加条件</button>
      <button class="cg-add" @click="addGroup"><i class="el-icon-plus" /> 添加条件组</button>
    </div>
  </div>
</template>

<script>
import { COND_OP_TEXT, COND_FIELD_TYPES, opsForFieldType } from '@/constants/dict'

export default {
  name: 'CondGroupEditor',
  props: {
    // 条件组（递归树）：{ logic: 'and'|'or', children: [叶子{fieldKey,op,value} | 子组{logic,children}] }
    group: { type: Object, required: true },
    // 可选字段列表（下拉项）
    fields: { type: Array, default: () => [] }
  },
  data() {
    return {
      // 模板中展示操作符文本（import 常量需暴露到实例）
      COND_OP_TEXT
    }
  },
  computed: {
    // 仅数字/日期/单选/多选字段可作为条件判断依据
    condFields() {
      return this.fields.filter(f => COND_FIELD_TYPES.includes(f.fieldType))
    }
  },
  created() {
    // 清理历史数据中引用「非条件字段类型」的叶子（如旧配置里用文本字段做条件依据）
    this.prune(this.group)
  },
  methods: {
    // 递归剔除引用了非条件字段的叶子，及剔除后为空的子组
    prune(node) {
      if (!node || !Array.isArray(node.children)) return
      const validKeys = new Set(this.condFields.map(f => f.fieldKey))
      node.children = node.children.filter(child => {
        if (child.children) {
          this.prune(child)
          return child.children.length > 0
        }
        return validKeys.has(child.fieldKey)
      })
    },
    // 根据选中字段类型返回可用操作符列表
    opOptions(child) {
      const f = this.condFields.find(x => x.fieldKey === child.fieldKey)
      return opsForFieldType(f ? f.fieldType : null)
    },
    // 切换字段后，若当前操作符不在新类型可用列表内，重置为第一个可用操作符
    onFieldChange(child) {
      const ops = this.opOptions(child)
      if (!ops.includes(child.op)) child.op = ops[0] || 'eq'
      this.$emit('change')
    },
    setLogic(v) {
      this.group.logic = v
      this.$emit('change')
    },
    addCond() {
      this.group.children.push({ fieldKey: '', op: 'eq', value: '' })
      this.$emit('change')
    },
    addGroup() {
      this.group.children.push({ logic: 'and', children: [{ fieldKey: '', op: 'eq', value: '' }] })
      this.$emit('change')
    },
    removeChild(i) {
      this.group.children.splice(i, 1)
      this.$emit('change')
    }
  }
}
</script>

<style lang="scss" scoped>
.cg-root {
  width: 100%;
}

// 逻辑切换
.cg-logic {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.cg-logic-label {
  font-size: 12px;
  color: #909399;
}
.cg-seg {
  display: inline-flex;
  background: #eef1f5;
  border-radius: 6px;
  padding: 2px;
  gap: 2px;
}
.cg-seg-btn {
  border: none;
  background: transparent;
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 5px;
  cursor: pointer;
  color: #606266;
  transition: all 0.15s;
  &:hover { color: var(--color-primary); }
  &.active {
    background: #fff;
    color: var(--color-primary);
    font-weight: 600;
    box-shadow: 0 1px 3px rgba(15, 23, 42, 0.12);
  }
}

// 子项
.cg-item {
  margin-bottom: 4px;
}

// 连接词
.cg-joiner {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 6px 0 6px 2px;
}
.cg-joiner-line {
  width: 12px;
  height: 1px;
  background: #e2e8f0;
}
.cg-joiner-tag {
  font-size: 11px;
  padding: 1px 10px;
  border-radius: 999px;
  font-weight: 600;
  line-height: 16px;
  &.is-and {
    color: var(--color-primary);
    background: var(--color-primary-light);
  }
  &.is-or {
    color: #b45309;
    background: #fdf3e7;
  }
}

// 叶子条件卡片
.cg-leaf {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px;
  background: #f8fafc;
  border: 1px solid #eef1f5;
  border-radius: 8px;
  transition: all 0.15s;
  &:hover {
    background: #fff;
    border-color: #dcdfe6;
    box-shadow: 0 1px 4px rgba(15, 23, 42, 0.05);
  }
}

// 控件
.cg-select {
  height: 30px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
  background: #fff;
  transition: all 0.15s;
  &:hover { border-color: #c0c4cc; }
  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb), 0.12);
  }
}
.cg-field {
  flex: 1;
  min-width: 0;
}
.cg-op {
  width: 108px;
  flex-shrink: 0;
}
.cg-input {
  flex: 1;
  min-width: 56px;
  height: 30px;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 0 8px;
  font-size: 12px;
  outline: none;
  transition: all 0.15s;
  &:hover { border-color: #c0c4cc; }
  &:focus {
    border-color: var(--color-primary);
    box-shadow: 0 0 0 2px rgba(var(--color-primary-rgb), 0.12);
  }
}

// 删除按钮
.cg-del {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: #c0c4cc;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.15s;
  &:hover {
    background: #fee2e2;
    color: #dc2626;
  }
}

// 子组
.cg-sub {
  border: 1px solid #e8edf3;
  border-left: 3px solid var(--color-primary);
  border-radius: 8px;
  padding: 10px 12px 8px;
  background: #fafbfd;
  margin-left: 2px;
}
.cg-sub-head {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.cg-sub-icon {
  color: var(--color-primary);
  font-size: 14px;
}
.cg-sub-title {
  font-size: 12px;
  font-weight: 600;
  color: #4a4f58;
}
.cg-sub-head .cg-del {
  margin-left: auto;
}

// 添加按钮
.cg-actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.cg-add {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border: 1px dashed #cbd5e0;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  color: var(--color-primary);
  font-size: 12px;
  transition: all 0.15s;
  &:hover {
    border-color: var(--color-primary);
    background: var(--color-primary-light);
  }
}
</style>
