/**
 * Created by PanJiaChen on 16/11/18.
 */

/**
 * Parse the time to string
 * @param {(Object|string|number)} time
 * @param {string} cFormat
 * @returns {string | null}
 */
export function parseTime(time, cFormat) {
  if (arguments.length === 0 || !time) {
    return null
  }
  const format = cFormat || '{y}-{m}-{d} {h}:{i}:{s}'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if ((typeof time === 'string')) {
      if ((/^[0-9]+$/.test(time))) {
        // support "1548221490638"
        time = parseInt(time)
      } else {
        // support safari
        // https://stackoverflow.com/questions/4310953/invalid-date-in-safari
        time = time.replace(new RegExp(/-/gm), '/')
      }
    }

    if ((typeof time === 'number') && (time.toString().length === 10)) {
      time = time * 1000
    }
    date = new Date(time)
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  }
  const time_str = format.replace(/{([ymdhisa])+}/g, (result, key) => {
    const value = formatObj[key]
    // Note: getDay() returns 0 on Sunday
    if (key === 'a') { return ['日', '一', '二', '三', '四', '五', '六'][value ] }
    return value.toString().padStart(2, '0')
  })
  return time_str
}

/**
 * @param {number} time
 * @param {string} option
 * @returns {string}
 */
export function formatTime(time, option) {
  if (('' + time).length === 10) {
    time = parseInt(time) * 1000
  } else {
    time = +time
  }
  const d = new Date(time)
  const now = Date.now()

  const diff = (now - d) / 1000

  if (diff < 30) {
    return '刚刚'
  } else if (diff < 3600) {
    // less 1 hour
    return Math.ceil(diff / 60) + '分钟前'
  } else if (diff < 3600 * 24) {
    return Math.ceil(diff / 3600) + '小时前'
  } else if (diff < 3600 * 24 * 2) {
    return '1天前'
  }
  if (option) {
    return parseTime(time, option)
  } else {
    return (
      d.getMonth() +
      1 +
      '月' +
      d.getDate() +
      '日' +
      d.getHours() +
      '时' +
      d.getMinutes() +
      '分'
    )
  }
}

/**
 * @param {string} url
 * @returns {Object}
 */
export function param2Obj(url) {
  const search = decodeURIComponent(url.split('?')[1]).replace(/\+/g, ' ')
  if (!search) {
    return {}
  }
  const obj = {}
  const searchArr = search.split('&')
  searchArr.forEach(v => {
    const index = v.indexOf('=')
    if (index !== -1) {
      const name = v.substring(0, index)
      const val = v.substring(index + 1, v.length)
      obj[name] = val
    }
  })
  return obj
}

// 附件 biz_id 生成：stableBizId —— 输入任意业务串，输出 ≤32 位稳定字母数字（适配 attach.biz_id varchar(32)）
export function stableBizId(key) {
  if (!key) return ''
  const s = String(key)
  if (s.length <= 32 && /^[A-Za-z0-9]+$/.test(s)) return s
  // 64 位哈希（cyrb53），转 16 位十六进制（16 字符），保证 ≤32 且碰撞概率极低
  let h1 = 0xdeadbeef ^ 0
  let h2 = 0x41c6ce57 ^ 0
  for (let i = 0; i < s.length; i++) {
    const ch = s.charCodeAt(i)
    h1 = Math.imul(h1 ^ ch, 2654435761)
    h2 = Math.imul(h2 ^ ch, 1597334677)
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909)
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909)
  const n1 = (h2 >>> 0).toString(16).padStart(8, '0')
  const n2 = (h1 >>> 0).toString(16).padStart(8, '0')
  return ('a' + n1 + n2).slice(0, 32)
}

// 处理人展示（实际处理人优先）：节点发生任务交接后，库里 handler 已改成接手人，
// 但真正经办的是原处理人 —— 因此展示为「原处理人 用户号（现 接手人 用户号）」，
// 例如「钱七 emp0005（现 孙八 emp0006）」；未发生交接时原样返回姓名。
export function formatHandlerWithTransfer(node, fallback) {
  if (!node) return fallback || '—'
  if (node.transferFromUserName) {
    const from = node.transferFromUserName + (node.transferFromUserId ? ' ' + node.transferFromUserId : '')
    const now = node.handlerName
      ? '（现 ' + node.handlerName + (node.handlerUserId ? ' ' + node.handlerUserId : '') + '）'
      : ''
    return from + now
  }
  if (!node.handlerName) return fallback || '—'
  return node.handlerName + (node.handlerUserId ? ' ' + node.handlerUserId : '')
}

// 节点处理人格式化：输入 task_node 数组（含 handlerName/handlerUserId/submitStatus），
// 输出「姓名 用户号」并列串（按 userId 去重）。pendingOnly=true 只取待办（可处理人），否则取全部（含已处理）
// 这里输出的是节点「当前归属人」名单；实际经办人的展示见 formatHandlerWithTransfer
export function formatNodeHandlers(nodes, pendingOnly) {
  const arr = (nodes || []).filter(n => n && n.handlerName)
    .filter(n => (pendingOnly ? n.submitStatus === 0 : true))
  const seen = new Set()
  const out = []
  arr.forEach(n => {
    const key = n.handlerUserId || n.handlerName
    if (seen.has(key)) return
    seen.add(key)
    out.push(n.handlerName + (n.handlerUserId ? ' ' + n.handlerUserId : ''))
  })
  return out.join('、')
}
