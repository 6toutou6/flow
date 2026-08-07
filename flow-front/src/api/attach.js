import request from '@/utils/request-flow'

// 上传附件（multipart：file + bizId）
export function uploadAttach(file, bizId) {
  const formData = new FormData()
  formData.append('file', file)
  if (bizId) formData.append('bizId', bizId)
  return request({
    url: '/flow-attach/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 附件列表（按业务id）
export function getAttachList(bizId) {
  return request({
    url: '/flow-attach/list',
    method: 'get',
    params: { bizId }
  })
}

// 预览附件（后端日志输出）
export function previewAttach(attachId) {
  return request({
    url: `/flow-attach/preview/${attachId}`,
    method: 'get'
  })
}

// 下载附件（后端日志输出）
export function downloadAttach(attachId) {
  return request({
    url: `/flow-attach/download/${attachId}`,
    method: 'get'
  })
}

// 删除附件（后端日志输出）
export function deleteAttach(attachId) {
  return request({
    url: `/flow-attach/${attachId}`,
    method: 'delete'
  })
}
