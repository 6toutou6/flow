/** 模板字段类型 → 中文名（数据展示各页共用，避免各页各写一套映射） */
export const FIELD_TYPE_NAMES = {
  text: '单行文本',
  textarea: '多行文本',
  number: '数字',
  date: '日期',
  datetime: '日期时间',
  select: '下拉选择',
  radio: '单选',
  checkbox: '多选',
  upload: '附件上传',
  file: '文件',
  image: '图片',
  person: '人员选择',
  rich: '富文本'
}

/** 把 [{ name, value }] 里的字段类型名翻成中文；未知类型原样返回 */
export function withFieldTypeNames(list) {
  return (list || []).map(i => Object.assign({}, i, { name: FIELD_TYPE_NAMES[i.name] || i.name }))
}
