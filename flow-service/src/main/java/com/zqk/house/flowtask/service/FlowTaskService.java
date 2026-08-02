package com.zqk.house.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zqk.house.flowdata.entity.FlowAttachment;
import com.zqk.house.flowdata.entity.FlowFormData;
import com.zqk.house.flowdata.entity.FlowFormRecord;
import com.zqk.house.flowdata.mapper.FlowAttachmentMapper;
import com.zqk.house.flowdata.mapper.FlowFormDataMapper;
import com.zqk.house.flowdata.mapper.FlowFormRecordMapper;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.entity.FlowTaskDispatch;
import com.zqk.house.flowtask.entity.FlowTaskNode;
import com.zqk.house.flowtask.entity.FlowTaskQueryForm;
import com.zqk.house.flowtask.mapper.FlowTaskDispatchMapper;
import com.zqk.house.flowtask.mapper.FlowTaskMapper;
import com.zqk.house.flowtask.mapper.FlowTaskNodeMapper;
import com.zqk.house.flowtask.vo.FormDataItemVO;
import com.zqk.house.flowtask.vo.MyTodoVO;
import com.zqk.house.flowtask.vo.NodeSubmitDTO;
import com.zqk.house.flowtask.vo.TaskCreateDTO;
import com.zqk.house.flowtask.vo.TaskDetailVO;
import com.zqk.house.flowtask.vo.TaskGroupVO;
import com.zqk.house.flowtask.vo.TaskMemberVO;
import com.zqk.house.flowtask.vo.TaskProgressVO;
import com.zqk.house.flowtemplate.entity.FlowTemplate;
import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import com.zqk.house.flowtemplate.entity.FlowTemplateNode;
import com.zqk.house.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateNodeMapper;
import com.zqk.house.sysuser.vo.LoginUser;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class FlowTaskService {

    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowTemplateMapper flowTemplateMapper;
    @Autowired
    private FlowTemplateNodeMapper flowTemplateNodeMapper;
    @Autowired
    private FlowTemplateFieldMapper flowTemplateFieldMapper;
    @Autowired
    private FlowFormRecordMapper flowFormRecordMapper;
    @Autowired
    private FlowFormDataMapper flowFormDataMapper;
    @Autowired
    private FlowAttachmentMapper flowAttachmentMapper;

    /** 主任务分页：一次下发 = 一条主任务，组级不含“当前处理人”；成员各自独立（当前处理人/进度在成员上） */
    public PageResult<TaskGroupVO> getPage(FlowTaskQueryForm form) {
        int page = form.getPage() == null ? 1 : form.getPage();
        int limit = form.getLimit() == null ? 10 : form.getLimit();
        int offset = (page - 1) * limit;
        String taskName = StringUtils.hasText(form.getTaskName()) ? form.getTaskName() : null;
        Integer status = form.getStatus();
        List<TaskGroupVO> groups = flowTaskDispatchMapper.selectDispatchPage(taskName, status, offset, limit);
        Long total = flowTaskDispatchMapper.selectDispatchCount(taskName, status);
        return new PageResult<>(groups, total);
    }

    /** 主任务详情：组头 + 全部成员（level 2 展示）。空组（无成员）仍返回，便于补员/删除 */
    public TaskGroupVO getDispatchDetail(Long dispatchId) {
        FlowTaskDispatch dispatch = flowTaskDispatchMapper.selectById(dispatchId);
        if (dispatch == null) return null;
        List<TaskMemberVO> members = flowTaskMapper.selectMembersByDispatch(dispatchId);
        TaskGroupVO vo = new TaskGroupVO();
        vo.setDispatchId(dispatchId);
        vo.setPrimaryTaskId(dispatchId);
        vo.setTaskName(dispatch.getTaskName());
        vo.setTaskDesc(dispatch.getTaskDesc());
        vo.setTemplateId(dispatch.getTemplateId());
        vo.setStartTime(dispatch.getStartTime());
        vo.setEndTime(dispatch.getEndTime());
        vo.setDispatchTime(dispatch.getCreateTime());
        vo.setMembers(members);
        int running = 0, finished = 0, cancelled = 0;
        for (TaskMemberVO m : members) {
            if (m.getStatus() == null) continue;
            if (m.getStatus() == 1) running++;
            else if (m.getStatus() == 2) finished++;
            else if (m.getStatus() == 3) cancelled++;
        }
        vo.setMemberCount(members.size());
        vo.setRunningCount(running);
        vo.setFinishedCount(finished);
        vo.setCancelledCount(cancelled);
        // 聚合状态：空组→0；任一进行中→1；全作废→3；否则→2（与列表/统计口径一致）
        vo.setStatus(members.isEmpty() ? 0 : (running > 0 ? 1 : (cancelled == members.size() ? 3 : 2)));
        return vo;
    }

    /**
     * 解析当前模板节点：处理模板节点ID在重建后变成悬空引用的场景
     * 优先按 nodeId 直接查；查不到时按 taskId 的 task_node 找到该 legacyId 对应的 sort_num/node_name，
     * 再用 templateId + sort_num 反查当前模板同位置节点；仍找不到回退到模板 sort_num 最大节点
     * @return 当前模板中的有效 FlowTemplateNode
     */
    private FlowTemplateNode resolveTemplateNode(Long templateId, Long legacyNodeId, Long taskId) {
        if (legacyNodeId == null) return null;
        // 1) 如果 legacyId 在当前模板存在，直接返回（绝大多数正常情况）
        FlowTemplateNode direct = flowTemplateNodeMapper.selectById(legacyNodeId);
        if (direct != null && templateId != null && templateId.equals(direct.getTemplateId())) return direct;
        // 2) 反查 legacyNodeId 对应 task_node 的 sort_num 与 node_name
        Integer sortNum = null;
        String nodeName = null;
        if (taskId != null) {
            LambdaQueryWrapper<FlowTaskNode> tnw = new LambdaQueryWrapper<>();
            tnw.eq(FlowTaskNode::getTaskId, taskId)
               .eq(FlowTaskNode::getNodeId, legacyNodeId)
               .last("LIMIT 1");
            FlowTaskNode sample = flowTaskNodeMapper.selectOne(tnw);
            if (sample != null) {
                sortNum = sample.getSortNum();
                nodeName = sample.getNodeName();
            }
        }
        // 3) 按 sort_num 同位置匹配
        if (templateId != null && sortNum != null) {
            LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
            nw.eq(FlowTemplateNode::getTemplateId, templateId)
              .eq(FlowTemplateNode::getSortNum, sortNum)
              .last("LIMIT 1");
            FlowTemplateNode bySort = flowTemplateNodeMapper.selectOne(nw);
            if (bySort != null) return bySort;
        }
        // 4) 按 node_name 模糊匹配（重排序一致优先）
        if (templateId != null && nodeName != null) {
            LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
            nw.eq(FlowTemplateNode::getTemplateId, templateId)
              .eq(FlowTemplateNode::getNodeName, nodeName)
              .orderByAsc(FlowTemplateNode::getSortNum)
              .last("LIMIT 1");
            FlowTemplateNode byName = flowTemplateNodeMapper.selectOne(nw);
            if (byName != null) return byName;
        }
        // 5) 回退：模板最大 sort_num 的节点
        if (templateId != null) {
            LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
            nw.eq(FlowTemplateNode::getTemplateId, templateId)
              .orderByDesc(FlowTemplateNode::getSortNum)
              .last("LIMIT 1");
            return flowTemplateNodeMapper.selectOne(nw);
        }
        return null;
    }

    /** 构建 legacy nodeId → 当前模板节点 的映射（按 sort_num 同位置），用于 progress 归一化 */
    private java.util.Map<Long, FlowTemplateNode> buildNodeIdRemap(Long templateId, List<TaskProgressVO> progress) {
        if (progress == null || progress.isEmpty() || templateId == null) return new java.util.HashMap<>();
        // 该模板所有节点（按 sort_num 索引）
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, templateId).orderByAsc(FlowTemplateNode::getSortNum);
        List<FlowTemplateNode> tplNodes = flowTemplateNodeMapper.selectList(nw);
        java.util.Map<Integer, FlowTemplateNode> bySort = new java.util.HashMap<>();
        java.util.Map<String, FlowTemplateNode> byName = new java.util.HashMap<>();
        for (FlowTemplateNode n : tplNodes) {
            if (n.getSortNum() != null) bySort.putIfAbsent(n.getSortNum(), n);
            if (n.getNodeName() != null) byName.putIfAbsent(n.getNodeName(), n);
        }
        FlowTemplateNode maxSort = tplNodes.isEmpty() ? null : tplNodes.get(tplNodes.size() - 1);
        java.util.Map<Long, FlowTemplateNode> remap = new java.util.HashMap<>();
        for (TaskProgressVO p : progress) {
            if (p.getNodeId() == null || remap.containsKey(p.getNodeId())) continue;
            // 1) 同 template 的直接 id 命中
            FlowTemplateNode direct = flowTemplateNodeMapper.selectById(p.getNodeId());
            if (direct != null && templateId.equals(direct.getTemplateId())) {
                remap.put(p.getNodeId(), direct);
                continue;
            }
            // 2) sort_num 同位置
            FlowTemplateNode hit = (p.getSortNum() != null) ? bySort.get(p.getSortNum()) : null;
            // 3) node_name 匹配
            if (hit == null && p.getNodeName() != null) hit = byName.get(p.getNodeName());
            // 4) 回退最大 sort
            if (hit == null) hit = maxSort;
            remap.put(p.getNodeId(), hit);
        }
        return remap;
    }

    /** 任务详情：任务 + 流转进度 + 当前节点字段配置 + 模板完整节点链 + 各节点历史表单 */
    public TaskDetailVO getDetail(Long id) {
        FlowTask task = flowTaskMapper.selectById(id);
        if (task == null) return null;
        List<TaskProgressVO> progress = flowTaskNodeMapper.selectTaskProgress(id);
        TaskDetailVO vo = new TaskDetailVO();
        vo.setTask(task);
        vo.setTaskNodes(progress);
        // 模板完整节点链（用于展示未到节点的灰色骨架）
        LambdaQueryWrapper<FlowTemplateNode> tnw = new LambdaQueryWrapper<>();
        tnw.eq(FlowTemplateNode::getTemplateId, task.getTemplateId()).orderByAsc(FlowTemplateNode::getSortNum);
        List<FlowTemplateNode> tplNodes = flowTemplateNodeMapper.selectList(tnw);
        vo.setTemplateNodes(tplNodes);
        // 归一化 progress 的 nodeId → 当前模板节点 id（保证前端 byNode[tpl.id] 匹配正确）
        java.util.Map<Long, FlowTemplateNode> remap = buildNodeIdRemap(task.getTemplateId(), progress);
        for (TaskProgressVO p : progress) {
            FlowTemplateNode target = remap.get(p.getNodeId());
            if (target != null) {
                p.setNodeId(target.getId());
                p.setNodeName(target.getNodeName());
                p.setNodeType(target.getNodeType());
                p.setSortNum(target.getSortNum());
            }
        }
        // 当前处理人查看时，返回当前节点字段配置（resolve 到当前模板节点以防悬空引用）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser != null && task.getStatus() == 1) {
            FlowTemplateNode currentNode = resolveTemplateNode(task.getTemplateId(), task.getCurrentNodeId(), id);
            if (currentNode != null) {
                LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
                fw.eq(FlowTemplateField::getNodeId, currentNode.getId()).orderByAsc(FlowTemplateField::getSortNum);
                vo.setCurrentNodeFields(flowTemplateFieldMapper.selectList(fw));
            }
            // 退回重填：取当前节点最近一条已 done task_node 的表单数据，预填回表单
            vo.setCurrentFormData(loadCurrentNodeFormData(task));
        }
        // 回填各已处理节点的历史表单数据（点击查看用）
        fillHistoryFormData(progress, task.getTemplateId());
        return vo;
    }

    /** 加载当前节点当前处理人最近一次已提交表单数据（用于退回后表单回填，仅回填本人的） */
    private List<FlowFormData> loadCurrentNodeFormData(FlowTask task) {
        if (task.getCurrentNodeId() == null) return null;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return null;
        // 当前节点最近一条「当前登录用户」已 done 的 task_node（避免回填他人的数据）
        LambdaQueryWrapper<FlowTaskNode> tnw = new LambdaQueryWrapper<>();
        tnw.eq(FlowTaskNode::getTaskId, task.getId())
           .eq(FlowTaskNode::getNodeId, task.getCurrentNodeId())
           .eq(FlowTaskNode::getHandlerUserId, loginUser.getId())
           .eq(FlowTaskNode::getSubmitStatus, 1)
           .orderByDesc(FlowTaskNode::getId).last("LIMIT 1");
        FlowTaskNode lastDone = flowTaskNodeMapper.selectOne(tnw);
        if (lastDone == null || lastDone.getFormRecordId() == null) return null;
        LambdaQueryWrapper<FlowFormData> fdw = new LambdaQueryWrapper<>();
        fdw.eq(FlowFormData::getRecordId, lastDone.getFormRecordId());
        return flowFormDataMapper.selectList(fdw);
    }

    /** 回填各已处理 task_node 的表单数据（关联模板字段取 fieldLabel） */
    private void fillHistoryFormData(List<TaskProgressVO> progress, Long templateId) {
        List<Long> recordIds = progress.stream()
                .filter(n -> n.getFormRecordId() != null)
                .map(TaskProgressVO::getFormRecordId)
                .collect(Collectors.toList());
        if (recordIds.isEmpty()) return;
        // 一次查询所有 form_data
        LambdaQueryWrapper<FlowFormData> fdw = new LambdaQueryWrapper<>();
        fdw.in(FlowFormData::getRecordId, recordIds);
        List<FlowFormData> allData = flowFormDataMapper.selectList(fdw);
        if (allData.isEmpty()) return;
        // 查模板字段取 fieldLabel（按 fieldId 映射）
        LambdaQueryWrapper<FlowTemplateField> tfw = new LambdaQueryWrapper<>();
        tfw.eq(FlowTemplateField::getTemplateId, templateId);
        List<FlowTemplateField> tplFields = flowTemplateFieldMapper.selectList(tfw);
        Map<Long, String> labelMap = tplFields.stream()
                .collect(Collectors.toMap(FlowTemplateField::getId, FlowTemplateField::getFieldLabel, (a, b) -> a));
        // 按 recordId 分组
        Map<Long, List<FlowFormData>> dataByRecord = allData.stream()
                .collect(Collectors.groupingBy(FlowFormData::getRecordId));
        // 回填到各 TaskProgressVO
        for (TaskProgressVO n : progress) {
            if (n.getFormRecordId() == null) continue;
            List<FlowFormData> dataList = dataByRecord.get(n.getFormRecordId());
            if (dataList == null || dataList.isEmpty()) continue;
            List<FormDataItemVO> items = dataList.stream().map(fd -> {
                FormDataItemVO item = new FormDataItemVO();
                item.setFieldLabel(labelMap.getOrDefault(fd.getFieldId(), fd.getFieldKey()));
                item.setFieldValue(fd.getFieldValue());
                return item;
            }).collect(Collectors.toList());
            n.setFormDataList(items);
        }
    }

    /**
     * 批量创建任务：锁定模板版本 → 查首节点 → 为每个首节点处理人创建一个独立任务
     * 每个任务独立按流程节点链流转，互不影响
     * @return 实际创建的任务数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int create(TaskCreateDTO dto) {
        FlowTemplate tpl = flowTemplateMapper.selectById(dto.getTemplateId());
        if (tpl == null) throw new RuntimeException("模板不存在");
        if (dto.getFirstHandlerIds() == null || dto.getFirstHandlerIds().isEmpty()) {
            throw new RuntimeException("请至少指定一个首节点处理人");
        }
        // 查首节点（开始节点）
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, dto.getTemplateId())
          .eq(FlowTemplateNode::getNodeType, 1).last("LIMIT 1");
        FlowTemplateNode firstNode = flowTemplateNodeMapper.selectOne(nw);
        if (firstNode == null) throw new RuntimeException("模板未设计流程节点（缺少开始节点）");
        // 总节点数
        LambdaQueryWrapper<FlowTemplateNode> countW = new LambdaQueryWrapper<>();
        countW.eq(FlowTemplateNode::getTemplateId, dto.getTemplateId());
        long total = flowTemplateNodeMapper.selectCount(countW);

        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long creatorId = loginUser == null ? null : loginUser.getId();
        // 先建主任务（一次下发 = 一条主任务，人员删空后仍保留）
        FlowTaskDispatch dispatch = new FlowTaskDispatch();
        dispatch.setTemplateId(dto.getTemplateId());
        dispatch.setTemplateVersion(tpl.getVersion());
        dispatch.setTaskName(dto.getTaskName());
        dispatch.setTaskDesc(dto.getTaskDesc());
        dispatch.setStartTime(dto.getStartTime());
        dispatch.setEndTime(dto.getEndTime());
        dispatch.setCreatorId(creatorId);
        flowTaskDispatchMapper.insert(dispatch);
        Long dispatchId = dispatch.getId();
        int count = 0;
        for (Long handlerId : dto.getFirstHandlerIds()) {
            if (handlerId == null) continue;
            FlowTask task = new FlowTask();
            task.setTemplateId(dto.getTemplateId());
            task.setTaskName(dto.getTaskName());
            task.setTaskDesc(dto.getTaskDesc());
            task.setStartTime(dto.getStartTime());
            task.setEndTime(dto.getEndTime());
            task.setStatus(1);
            task.setTemplateVersion(tpl.getVersion());
            task.setCurrentNodeId(firstNode.getId());
            task.setCurrentHandlerId(handlerId);
            task.setFinishedNodeCount(0);
            task.setTotalNodeCount((int) total);
            task.setCreatorId(creatorId);
            task.setDispatchId(dispatchId);
            flowTaskMapper.insert(task);
            // 插首个 task_node（待处理）
            FlowTaskNode firstTaskNode = new FlowTaskNode();
            firstTaskNode.setTaskId(task.getId());
            firstTaskNode.setNodeId(firstNode.getId());
            firstTaskNode.setNodeName(firstNode.getNodeName());
            firstTaskNode.setSortNum(firstNode.getSortNum());
            firstTaskNode.setNodeType(firstNode.getNodeType());
            firstTaskNode.setHandlerUserId(handlerId);
            firstTaskNode.setSubmitStatus(0);
            flowTaskNodeMapper.insert(firstTaskNode);
            count++;
        }
        return count;
    }

    /**
     * 新增人员：以主任务为对象，为每个新增处理人创建一条独立成员任务（从开始节点重新走流程），归入本主任务。
     * 每个下发任务都是独立的、互不影响——即便某人在其他任务里做过某节点处理人，也不影响为其建独立任务。
     * 已是该组成员（起始节点处理人）的人员自动跳过，避免重复。
     * @return 实际创建的任务数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int addHandlers(Long dispatchId, List<Long> handlerIds) {
        if (handlerIds == null || handlerIds.isEmpty()) {
            throw new RuntimeException("请至少选择一个处理人");
        }
        FlowTaskDispatch dispatch = flowTaskDispatchMapper.selectById(dispatchId);
        if (dispatch == null) throw new RuntimeException("任务不存在");
        FlowTemplate tpl = flowTemplateMapper.selectById(dispatch.getTemplateId());
        if (tpl == null) throw new RuntimeException("模板不存在");
        // 开始节点
        LambdaQueryWrapper<FlowTemplateNode> sw = new LambdaQueryWrapper<>();
        sw.eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId())
          .eq(FlowTemplateNode::getNodeType, 1).last("LIMIT 1");
        FlowTemplateNode firstNode = flowTemplateNodeMapper.selectOne(sw);
        if (firstNode == null) throw new RuntimeException("模板缺少开始节点");
        // 总节点数
        LambdaQueryWrapper<FlowTemplateNode> countW = new LambdaQueryWrapper<>();
        countW.eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId());
        long total = flowTemplateNodeMapper.selectCount(countW);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long creatorId = loginUser == null ? null : loginUser.getId();
        // 组内已有成员任务的起始节点处理人（去重，避免重复建独立任务）
        List<Long> groupTaskIds = flowTaskMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getDispatchId, dispatchId))
                .stream().map(FlowTask::getId).collect(Collectors.toList());
        Set<Long> memberOwners = groupTaskIds.isEmpty() ? new java.util.HashSet<>() :
                flowTaskNodeMapper.selectList(new LambdaQueryWrapper<FlowTaskNode>()
                        .in(FlowTaskNode::getTaskId, groupTaskIds)
                        .eq(FlowTaskNode::getNodeType, 1))
                        .stream().map(FlowTaskNode::getHandlerUserId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
        int count = 0;
        for (Long handlerId : handlerIds) {
            if (handlerId == null) continue;
            if (memberOwners.contains(handlerId)) continue;
            FlowTask task = new FlowTask();
            task.setTemplateId(dispatch.getTemplateId());
            task.setTaskName(dispatch.getTaskName());
            task.setTaskDesc(dispatch.getTaskDesc());
            task.setStartTime(dispatch.getStartTime());
            task.setEndTime(dispatch.getEndTime());
            task.setStatus(1);
            task.setTemplateVersion(tpl.getVersion());
            task.setCurrentNodeId(firstNode.getId());
            task.setCurrentHandlerId(handlerId);
            task.setFinishedNodeCount(0);
            task.setTotalNodeCount((int) total);
            task.setCreatorId(creatorId);
            task.setDispatchId(dispatchId);
            flowTaskMapper.insert(task);
            // 插首个 task_node（待处理）
            FlowTaskNode firstTaskNode = new FlowTaskNode();
            firstTaskNode.setTaskId(task.getId());
            firstTaskNode.setNodeId(firstNode.getId());
            firstTaskNode.setNodeName(firstNode.getNodeName());
            firstTaskNode.setSortNum(firstNode.getSortNum());
            firstTaskNode.setNodeType(firstNode.getNodeType());
            firstTaskNode.setHandlerUserId(handlerId);
            firstTaskNode.setSubmitStatus(0);
            firstTaskNode.setAction(0);
            flowTaskNodeMapper.insert(firstTaskNode);
            count++;
        }
        if (count == 0) throw new RuntimeException("所选人员均已参与过该任务组，无需重复添加");
        return count;
    }

    /**
     * 删除任务中的某个处理人：只删除该人员的待处理（pending）task_node，保留已处理的历史记录
     * （避免删除历史导致流程链断裂、人员记录混乱）
     * 删除后重算 finished_node_count；若被删的是当前处理人且仍有其他 pending，则更新 current_handler_id
     * @return 实际删除的 task_node 数量
     */
    @Transactional(rollbackFor = Exception.class)
    public int removeHandler(Long taskId, Long handlerUserId) {
        if (taskId == null || handlerUserId == null) {
            throw new RuntimeException("参数缺失");
        }
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        // 只查该人员的 pending task_node（待处理），保留已处理的历史记录
        LambdaQueryWrapper<FlowTaskNode> tnw = new LambdaQueryWrapper<>();
        tnw.eq(FlowTaskNode::getTaskId, taskId)
           .eq(FlowTaskNode::getHandlerUserId, handlerUserId)
           .eq(FlowTaskNode::getSubmitStatus, 0);
        List<FlowTaskNode> pendingNodes = flowTaskNodeMapper.selectList(tnw);
        if (pendingNodes.isEmpty()) {
            throw new RuntimeException("该人员当前无待处理任务");
        }
        // pending 的 task_node 没有关联表单记录，直接删除即可
        flowTaskNodeMapper.delete(tnw);
        // 重算已完成节点数
        task.setFinishedNodeCount(recomputeFinishedCount(taskId));
        // 若被删的是当前处理人，且任务仍进行中，则把指针指向任一其他 pending 处理人
        if (task.getStatus() == 1 && handlerUserId.equals(task.getCurrentHandlerId())) {
            LambdaQueryWrapper<FlowTaskNode> pw = new LambdaQueryWrapper<>();
            pw.eq(FlowTaskNode::getTaskId, taskId).eq(FlowTaskNode::getSubmitStatus, 0)
              .last("LIMIT 1");
            FlowTaskNode anyPending = flowTaskNodeMapper.selectOne(pw);
            task.setCurrentHandlerId(anyPending != null ? anyPending.getHandlerUserId() : null);
        }
        flowTaskMapper.updateById(task);
        return pendingNodes.size();
    }

    /**
     * 节点提交并流转（核心事务，支持多选分支与退回到指定节点）
     * action=pass：非结束节点为每个 nextHandlerIds 创建一条 pending task_node（独立并行分支）
     * action=reject：退回到 rejectToNodeId（已到达过的节点），由当前处理人重做，表单可回填上次数据
     * 任务完成判定：无 pending task_node 残留时 status=2
     */
    @Transactional(rollbackFor = Exception.class)
    public Long submit(NodeSubmitDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) throw new RuntimeException("未登录");
        boolean isReject = "reject".equalsIgnoreCase(dto.getAction());
        // 1. 校验当前 task_node
        FlowTaskNode currentTaskNode = flowTaskNodeMapper.selectById(dto.getTaskNodeId());
        if (currentTaskNode == null) throw new RuntimeException("节点不存在");
        if (currentTaskNode.getSubmitStatus() != null && currentTaskNode.getSubmitStatus() == 1) {
            throw new RuntimeException("该节点已处理，请勿重复提交");
        }
        // 2. 校验 task
        FlowTask task = flowTaskMapper.selectById(dto.getTaskId());
        if (task == null) throw new RuntimeException("任务不存在");
        if (task.getStatus() != 1) throw new RuntimeException("任务已结束或作废，无法提交");
        if (task.getCurrentNodeId() == null || !task.getCurrentNodeId().equals(currentTaskNode.getNodeId())) {
            throw new RuntimeException("任务当前节点已变化，请刷新后重试");
        }
        // 归一化下一处理人ID列表（兼容旧字段 nextHandlerUserId）
        List<Long> nextHandlerIds = dto.getNextHandlerIds();
        if ((nextHandlerIds == null || nextHandlerIds.isEmpty()) && dto.getNextHandlerUserId() != null) {
            nextHandlerIds = new ArrayList<>();
            nextHandlerIds.add(dto.getNextHandlerUserId());
        }
        boolean isEnd = currentTaskNode.getNodeType() != null && currentTaskNode.getNodeType() == 3;
        // 3. 退回校验：开始节点不可退回；须指定退回目标节点
        if (isReject) {
            if (currentTaskNode.getNodeType() != null && currentTaskNode.getNodeType() == 1) {
                throw new RuntimeException("开始节点不可退回");
            }
            if (dto.getRejectToNodeId() == null) {
                throw new RuntimeException("请选择退回目标节点");
            }
            if (dto.getRejectReason() == null || dto.getRejectReason().trim().isEmpty()) {
                throw new RuntimeException("请填写退回原因");
            }
        } else {
            // 通过：非结束节点需至少一个下一处理人
            if (!isEnd && (nextHandlerIds == null || nextHandlerIds.isEmpty())) {
                throw new RuntimeException("请指定下一节点处理人");
            }
        }
        // 4. 校验必填字段 + 插 form_record/form_data（退回时跳过必填校验，允许不填表单）
        Long recordId = saveFormRecordAndData(task, currentTaskNode, loginUser, dto.getFormData(), isReject);

        if (isReject) {
            // ===== 退回到指定节点（当前处理人重做该节点） =====
            currentTaskNode.setSubmitStatus(1);
            currentTaskNode.setAction(1);
            currentTaskNode.setHandleTime(new Date());
            currentTaskNode.setNextHandlerUserId(null);
            currentTaskNode.setFormRecordId(recordId);
            currentTaskNode.setRejectReason(dto.getRejectReason());
            flowTaskNodeMapper.updateById(currentTaskNode);
            // 查退回目标模板节点（须 sortNum < 当前，且属于同模板）
            FlowTemplateNode targetNode = flowTemplateNodeMapper.selectById(dto.getRejectToNodeId());
            if (targetNode == null || !targetNode.getTemplateId().equals(task.getTemplateId())) {
                throw new RuntimeException("退回目标节点不存在");
            }
            if (currentTaskNode.getSortNum() == null || targetNode.getSortNum() == null
                    || targetNode.getSortNum() >= currentTaskNode.getSortNum()) {
                throw new RuntimeException("退回目标须为已到达过的前置节点");
            }
            // 新建目标节点 task_node（待处理，由当前处理人重做）
            FlowTaskNode targetTaskNode = new FlowTaskNode();
            targetTaskNode.setTaskId(task.getId());
            targetTaskNode.setNodeId(targetNode.getId());
            targetTaskNode.setNodeName(targetNode.getNodeName());
            targetTaskNode.setSortNum(targetNode.getSortNum());
            targetTaskNode.setNodeType(targetNode.getNodeType());
            targetTaskNode.setHandlerUserId(loginUser.getId());
            targetTaskNode.setSubmitStatus(0);
            targetTaskNode.setAction(0);
            // 携带退回建议：重做该节点的人能看到“为什么被退回”
            targetTaskNode.setRejectReason(dto.getRejectReason());
            flowTaskNodeMapper.insert(targetTaskNode);
            // 退回重做：同步该节点原有处理人（如需求设计并行派给张三、李四时，退回后李四也要能重新处理）。
            // 从该节点历史已处理记录取原有处理人，为其补建待办（当前处理人已建 redo 跳过）
            LambdaQueryWrapper<FlowTaskNode> origDoneW = new LambdaQueryWrapper<>();
            origDoneW.eq(FlowTaskNode::getTaskId, task.getId())
                     .eq(FlowTaskNode::getNodeId, targetNode.getId())
                     .eq(FlowTaskNode::getSubmitStatus, 1);
            Set<Long> origHandlers = flowTaskNodeMapper.selectList(origDoneW).stream()
                    .map(FlowTaskNode::getHandlerUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            for (Long h : origHandlers) {
                if (loginUser.getId().equals(h)) continue; // 当前处理人已建 redo
                LambdaQueryWrapper<FlowTaskNode> dupW = new LambdaQueryWrapper<>();
                dupW.eq(FlowTaskNode::getTaskId, task.getId())
                     .eq(FlowTaskNode::getNodeId, targetNode.getId())
                     .eq(FlowTaskNode::getHandlerUserId, h)
                     .eq(FlowTaskNode::getSubmitStatus, 0);
                if (flowTaskNodeMapper.selectCount(dupW) > 0) continue;
                FlowTaskNode redo = new FlowTaskNode();
                redo.setTaskId(task.getId());
                redo.setNodeId(targetNode.getId());
                redo.setNodeName(targetNode.getNodeName());
                redo.setSortNum(targetNode.getSortNum());
                redo.setNodeType(targetNode.getNodeType());
                redo.setHandlerUserId(h);
                redo.setSubmitStatus(0);
                redo.setAction(0);
                redo.setRejectReason(dto.getRejectReason());
                flowTaskNodeMapper.insert(redo);
            }
            // task 指针回到目标节点
            task.setCurrentNodeId(targetNode.getId());
            task.setCurrentHandlerId(loginUser.getId());
            flowTaskMapper.updateById(task);
            return recordId;
        }

        // ===== 通过：正向流转（多选分支） =====
        currentTaskNode.setSubmitStatus(1);
        currentTaskNode.setAction(0);
        currentTaskNode.setPassComment(dto.getPassComment());
        currentTaskNode.setHandleTime(new Date());
        currentTaskNode.setNextHandlerUserId(isEnd ? null : nextHandlerIds.get(0));
        currentTaskNode.setFormRecordId(recordId);
        flowTaskNodeMapper.updateById(currentTaskNode);
        // 同一节点其他 pending 分支：任一处理人完成即可，一并标记完成，避免遗留他人待办
        LambdaQueryWrapper<FlowTaskNode> siblingW = new LambdaQueryWrapper<>();
        siblingW.eq(FlowTaskNode::getTaskId, task.getId())
                .eq(FlowTaskNode::getNodeId, currentTaskNode.getNodeId())
                .eq(FlowTaskNode::getSubmitStatus, 0)
                .ne(FlowTaskNode::getId, currentTaskNode.getId());
        List<FlowTaskNode> siblings = flowTaskNodeMapper.selectList(siblingW);
        for (FlowTaskNode sib : siblings) {
            sib.setSubmitStatus(1);
            sib.setAction(0);
            sib.setHandleTime(new Date());
            flowTaskNodeMapper.updateById(sib);
        }

        if (isEnd) {
            // 结束节点 → 不创建下游，检查是否所有分支均完成
            checkAndFinishTask(task);
        } else {
            // 查下一模板节点（sort_num 大于当前）
            LambdaQueryWrapper<FlowTemplateNode> nextW = new LambdaQueryWrapper<>();
            nextW.eq(FlowTemplateNode::getTemplateId, task.getTemplateId())
                 .gt(FlowTemplateNode::getSortNum, currentTaskNode.getSortNum())
                 .orderByAsc(FlowTemplateNode::getSortNum).last("LIMIT 1");
            FlowTemplateNode nextNode = flowTemplateNodeMapper.selectOne(nextW);
            if (nextNode == null) {
                // 无下一节点，直接判定完成
                checkAndFinishTask(task);
            } else {
                // 为每个选中处理人创建一条 pending task_node（独立分支）
                Long firstHandlerId = nextHandlerIds.get(0);
                for (Long handlerId : nextHandlerIds) {
                    if (handlerId == null) continue;
                    FlowTaskNode nextTaskNode = new FlowTaskNode();
                    nextTaskNode.setTaskId(task.getId());
                    nextTaskNode.setNodeId(nextNode.getId());
                    nextTaskNode.setNodeName(nextNode.getNodeName());
                    nextTaskNode.setSortNum(nextNode.getSortNum());
                    nextTaskNode.setNodeType(nextNode.getNodeType());
                    nextTaskNode.setHandlerUserId(handlerId);
                    nextTaskNode.setSubmitStatus(0);
                    nextTaskNode.setAction(0);
                    flowTaskNodeMapper.insert(nextTaskNode);
                }
                // 更新 task 指针（代表性值：下一节点 + 首选处理人）
                task.setCurrentNodeId(nextNode.getId());
                task.setCurrentHandlerId(firstHandlerId);
                task.setFinishedNodeCount(recomputeFinishedCount(task.getId()));
                flowTaskMapper.updateById(task);
                checkAndFinishTask(task);
            }
        }
        return recordId;
    }

    /** 任务完成判定：无 pending task_node 残留则标记完成 */
    private void checkAndFinishTask(FlowTask task) {
        LambdaQueryWrapper<FlowTaskNode> pw = new LambdaQueryWrapper<>();
        pw.eq(FlowTaskNode::getTaskId, task.getId()).eq(FlowTaskNode::getSubmitStatus, 0);
        long pending = flowTaskNodeMapper.selectCount(pw);
        if (pending == 0) {
            task.setStatus(2);
            task.setFinishedNodeCount(recomputeFinishedCount(task.getId()));
            flowTaskMapper.updateById(task);
        }
    }

    /** 重算已完成节点数：有 ≥1 条已 done task_node 的 nodeId 数量 */
    private int recomputeFinishedCount(Long taskId) {
        LambdaQueryWrapper<FlowTaskNode> dw = new LambdaQueryWrapper<>();
        dw.eq(FlowTaskNode::getTaskId, taskId).eq(FlowTaskNode::getSubmitStatus, 1);
        List<FlowTaskNode> doneNodes = flowTaskNodeMapper.selectList(dw);
        long count = doneNodes.stream()
                .map(FlowTaskNode::getNodeId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();
        return (int) count;
    }

    /** 校验必填字段 + 插 form_record + 批量插 form_data，返回 recordId
     * @param skipRequired true 时跳过必填校验（退回场景允许不填表单） */
    private Long saveFormRecordAndData(FlowTask task, FlowTaskNode currentTaskNode, LoginUser loginUser, List<FlowFormData> formDataList, boolean skipRequired) {
        // 校验必填字段
        LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowTemplateField::getNodeId, currentTaskNode.getNodeId());
        List<FlowTemplateField> nodeFields = flowTemplateFieldMapper.selectList(fw);
        Map<Long, FlowTemplateField> fieldMap = nodeFields.stream()
                .collect(Collectors.toMap(FlowTemplateField::getId, f -> f, (a, b) -> a));
        if (!skipRequired) {
            Map<Long, String> submittedValues = formDataList == null ? new java.util.HashMap<>() :
                    formDataList.stream().collect(Collectors.toMap(FlowFormData::getFieldId, FlowFormData::getFieldValue, (a, b) -> a));
            for (FlowTemplateField tf : nodeFields) {
                if (tf.getRequired() != null && tf.getRequired() == 1) {
                    String val = submittedValues.get(tf.getId());
                    if (val == null || val.trim().isEmpty()) {
                        throw new RuntimeException("字段「" + tf.getFieldLabel() + "」为必填项");
                    }
                }
            }
        }
        // 插 form_record
        FlowFormRecord record = new FlowFormRecord();
        record.setTaskId(task.getId());
        record.setTemplateId(task.getTemplateId());
        record.setNodeId(currentTaskNode.getNodeId());
        record.setTaskNodeId(currentTaskNode.getId());
        record.setUserId(loginUser.getId());
        record.setRecordStatus(1);
        record.setIsDraft(0);
        record.setSubmitTime(new Date());
        flowFormRecordMapper.insert(record);
        Long recordId = record.getId();
        // 批量插 form_data
        if (formDataList != null) {
            for (FlowFormData fd : formDataList) {
                fd.setId(null);
                fd.setRecordId(recordId);
                if (fd.getFieldKey() == null && fieldMap.containsKey(fd.getFieldId())) {
                    fd.setFieldKey(fieldMap.get(fd.getFieldId()).getFieldKey());
                }
                flowFormDataMapper.insert(fd);
            }
        }
        return recordId;
    }

    /** 我的待办：当前登录用户作为处理人且未处理的任务节点 */
    public PageResult<MyTodoVO> myTodo(Integer page, Integer limit) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return new PageResult<>(new ArrayList<>(), 0L);
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        List<MyTodoVO> list = flowTaskNodeMapper.selectMyTodo(loginUser.getId(), offset, l);
        Long total = flowTaskNodeMapper.selectMyTodoCount(loginUser.getId());
        return new PageResult<>(list, total);
    }

    /** 任务流转进度（全部节点） */
    public List<TaskProgressVO> taskProgress(Long taskId) {
        return flowTaskNodeMapper.selectTaskProgress(taskId);
    }

    public boolean update(FlowTask task) {
        return flowTaskMapper.updateById(task) > 0;
    }

    public boolean endTask(Long id) {
        FlowTask t = new FlowTask();
        t.setId(id);
        t.setStatus(2);
        return flowTaskMapper.updateById(t) > 0;
    }

    public boolean cancelTask(Long id) {
        FlowTask t = new FlowTask();
        t.setId(id);
        t.setStatus(3);
        return flowTaskMapper.updateById(t) > 0;
    }

    /** 删除任务（成员）级联清理全部提交：任务 + 节点 + 表单记录 + 表单数据 + 附件。主任务保留 */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long id) {
        // 1) 表单记录 → 表单数据 / 附件
        LambdaQueryWrapper<FlowFormRecord> rw = new LambdaQueryWrapper<>();
        rw.eq(FlowFormRecord::getTaskId, id);
        List<FlowFormRecord> records = flowFormRecordMapper.selectList(rw);
        if (!records.isEmpty()) {
            List<Long> recordIds = records.stream().map(FlowFormRecord::getId).collect(Collectors.toList());
            LambdaQueryWrapper<FlowFormData> dw = new LambdaQueryWrapper<>();
            dw.in(FlowFormData::getRecordId, recordIds);
            flowFormDataMapper.delete(dw);
            LambdaQueryWrapper<FlowAttachment> aw = new LambdaQueryWrapper<>();
            aw.in(FlowAttachment::getRecordId, recordIds);
            flowAttachmentMapper.delete(aw);
            flowFormRecordMapper.delete(rw);
        }
        // 2) 任务节点
        LambdaQueryWrapper<FlowTaskNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTaskNode::getTaskId, id);
        flowTaskNodeMapper.delete(nw);
        // 3) 任务本身
        return flowTaskMapper.deleteById(id) > 0;
    }

    /** 删除主任务（任务组）：仅当组内无人员时才允许 */
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDispatch(Long dispatchId) {
        FlowTaskDispatch dispatch = flowTaskDispatchMapper.selectById(dispatchId);
        if (dispatch == null) throw new RuntimeException("任务不存在");
        LambdaQueryWrapper<FlowTask> mw = new LambdaQueryWrapper<>();
        mw.eq(FlowTask::getDispatchId, dispatchId);
        long memberCount = flowTaskMapper.selectCount(mw);
        if (memberCount > 0) {
            throw new RuntimeException("请先删除该任务的所有人员");
        }
        return flowTaskDispatchMapper.deleteById(dispatchId) > 0;
    }
}
