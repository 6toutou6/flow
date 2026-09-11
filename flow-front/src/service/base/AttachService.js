import { getDg, postDg } from '@/service/BaseAxios'

// 后端控制器
const controller = 'flow-attach'

/**
 * @desc: 上传附件（POST /flow-attach/upload；multipart：file + bizId）
 */
export function uploadAttach(file, bizId) {
  const formData = new FormData()
  formData.append('file', file)
  if (bizId) formData.append('bizId', bizId)
  return postDg(controller, 'upload', formData)
}

/**
 * @desc: 批量上传附件（POST /flow-attach/uploads；multipart：files + bizId + creator，返回 Attach 数组）
 */
export function uploadAttaches(files, bizId, creator) {
  const formData = new FormData()
  files.forEach(f => formData.append('files', f))
  if (bizId) formData.append('bizId', bizId)
  if (creator) formData.append('creator', creator)
  return postDg(controller, 'uploads', formData)
}

/**
 * @desc: 附件列表（POST /flow-attach/list；param: { bizId }）
 */
export function getAttachList(bizId) {
  return postDg(controller, 'list', { bizId })
}

/**
 * @desc: 预览附件（GET /flow-attach/preview/{attachId}）
 */
export function previewAttach(attachId) {
  return getDg(controller, 'preview', attachId)
}

/**
 * @desc: 下载附件（GET /flow-attach/download/{attachId}）
 */
export function downloadAttach(attachId) {
  return getDg(controller, 'download', attachId)
}

/**
 * @desc: 删除附件（POST /flow-attach/delete/{attachId}）
 */
export function deleteAttach(attachId) {
  return postDg(controller, `delete/${attachId}`)
}
