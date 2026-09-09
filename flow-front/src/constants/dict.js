// 业务字典：固定语义编码集中定义，避免各处散落魔法数字（1/2/3/4）
// 与后端 com.company.flow.sys.base.enums 保持一致

// 流程节点类型
export const NODE_TYPE = {
  START: 1, // 开始
  MIDDLE: 2, // 中间
  END: 3 // 结束
}

export const NODE_TYPE_TEXT = {
  [NODE_TYPE.START]: '开始',
  [NODE_TYPE.MIDDLE]: '中间',
  [NODE_TYPE.END]: '结束'
}

// 下发周期类型
export const CYCLE_TYPE = {
  WEEK: 1, // 每周
  MONTH: 2, // 每月
  QUARTER: 3, // 每季度
  ONCE: 4 // 单次下发
}

export const CYCLE_TYPE_TEXT = {
  [CYCLE_TYPE.WEEK]: '每周',
  [CYCLE_TYPE.MONTH]: '每月',
  [CYCLE_TYPE.QUARTER]: '每季度',
  [CYCLE_TYPE.ONCE]: '单次下发'
}

// 条件操作符文本（条件编辑器下拉展示）
export const COND_OP_TEXT = {
  eq: '等于',
  neq: '不等于',
  gt: '大于',
  gte: '大于等于',
  lt: '小于',
  lte: '小于等于',
  in: '包含于',
  empty: '为空',
  notempty: '非空'
}

// 可参与条件判断的字段类型（文本/人员/部门/文件/图片等不做条件依据）
export const COND_FIELD_TYPES = ['number', 'date', 'radio', 'checkbox']

// 按字段类型返回可用操作符（避免文本字段出现无意义的「大于/小于」）
export function opsForFieldType(fieldType) {
  switch (fieldType) {
    case 'number':
      return ['eq', 'neq', 'gt', 'gte', 'lt', 'lte', 'in', 'empty', 'notempty']
    case 'date':
      return ['eq', 'neq', 'gt', 'gte', 'lt', 'lte', 'empty', 'notempty']
    case 'radio':
      return ['eq', 'neq', 'empty', 'notempty']
    case 'checkbox':
      return ['eq', 'neq', 'in', 'empty', 'notempty']
    default:
      return ['eq', 'neq', 'in', 'empty', 'notempty']
  }
}

// 结构化条件求值（与后端 ConditionEvaluator 口径一致）：
// 叶子条件 {"fieldKey":"","op":"","value":""}；分组 {"logic":"and|or","children":[叶子|分组,...]}
// values 为 fieldKey → value 的 map

// 叶子条件求值
function evalLeaf(o, values) {
  const fieldKey = o.fieldKey
  const op = o.op || 'eq'
  const expect = o.value == null ? '' : String(o.value)
  const actual = fieldKey && values ? values[fieldKey] : undefined
  switch (op) {
    case 'empty':
      return actual == null || String(actual).trim() === ''
    case 'notempty':
      return actual != null && String(actual).trim() !== ''
    case 'eq':
      return actual != null && String(actual) === expect
    case 'neq':
      return actual == null || String(actual) !== expect
    case 'in':
      return actual != null && expect.split(',').map(s => s.trim()).includes(String(actual))
    case 'gt':
      return cmp(actual, expect) > 0
    case 'gte':
      return cmp(actual, expect) >= 0
    case 'lt':
      return cmp(actual, expect) < 0
    case 'lte':
      return cmp(actual, expect) <= 0
    default:
      return true
  }
}

function cmp(a, b) {
  if (a == null) return -1
  const da = Number(a)
  const db = Number(b)
  if (!isNaN(da) && !isNaN(db)) return da - db
  return String(a).localeCompare(String(b))
}

// 递归求值节点：分组（children/conds）或叶子（fieldKey）
function evalNode(o, values) {
  let children = o.children
  if (!Array.isArray(children)) children = o.conds
  if (Array.isArray(children)) {
    if (children.length === 0) return true
    const isOr = o.logic === 'or'
    return isOr
      ? children.some(c => evalNode(c, values))
      : children.every(c => evalNode(c, values))
  }
  return evalLeaf(o, values)
}

// 单条件求值（叶子）
export function evalCond(cond, values) {
  if (!cond) return true
  try {
    return evalLeaf(JSON.parse(cond), values)
  } catch (e) {
    return true
  }
}

// 条件表达式求值（递归树，支持任意嵌套；兼容旧 conds 与单条件结构）
export function evalCondAll(cond, values) {
  if (!cond) return true
  try {
    return evalNode(JSON.parse(cond), values)
  } catch (e) {
    return true
  }
}
