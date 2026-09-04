package com.company.flow.sys.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.flow.sys.flowdata.entity.FlowFormData;
import com.company.flow.sys.flowdata.entity.FlowFormRecord;
import com.company.flow.sys.flowdata.mapper.FlowFormDataMapper;
import com.company.flow.sys.flowdata.mapper.FlowFormRecordMapper;
import com.company.flow.sys.flowtask.entity.FlowDispatch;
import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatchNode;
import com.company.flow.sys.flowtask.entity.FlowTaskLog;
import com.company.flow.sys.flowtask.entity.FlowTaskNode;
import com.company.flow.sys.flowtask.entity.FlowTaskQueryForm;
import com.company.flow.sys.flowtask.mapper.FlowDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchNodeMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskLogMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskNodeMapper;
import com.company.flow.sys.flowtask.vo.FormDataItemVO;
import com.company.flow.sys.flowtask.vo.MyTodoPeriodVO;
import com.company.flow.sys.flowtask.vo.MyTodoStatsVO;
import com.company.flow.sys.flowtask.vo.MyTodoTaskVO;
import com.company.flow.sys.flowtask.vo.MyTodoVO;
import com.company.flow.sys.flowtask.vo.MemberNodeStepVO;
import com.company.flow.sys.flowtask.vo.NodeSubmitDTO;
import com.company.flow.sys.flowtask.vo.TaskDetailVO;
import com.company.flow.sys.flowtask.vo.TaskGroupVO;
import com.company.flow.sys.flowtask.vo.TaskMemberVO;
import com.company.flow.sys.flowtask.vo.TaskProgressVO;
import com.company.flow.sys.flowtask.vo.TodoNodeVO;
import com.company.flow.sys.flowtemplate.entity.FlowTemplate;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateField;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateNode;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateMapper;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateNodeMapper;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private FlowTaskLogMapper flowTaskLogMapper;
    @Autowired
    private FlowDispatchMapper flowDispatchMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowTaskDispatchNodeMapper flowTaskDispatchNodeMapper;
    @Autowired
    private FlowTemplateMapper flowTemplateMapper;
    @Autowired
    private FlowTemplateNodeMapper flowTemplateNodeMapper;
    @Autowired
    private FlowTemplateFieldMapper flowTemplateFieldMapper;
    @Autowired
    private FlowFormRecordMapper flowFormRecordMapper;
    @Autowired
    private com.company.flow.sys.base.autuser.mapper.UserMapper userMapper;
    @Autowired
    private FlowFormDataMapper flowFormDataMapper;
    @Autowired
    private ObjectMapper objectMapper;

    /** 模板级字段值 Map → JSON（null/空返回 null） */
    private String serializeTemplateData(Map<String, String> data) {
        if (data == null || data.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }

    /** JSON → 模板级字段值 Map（null/空返回空 Map） */
    private Map<String, String> deserializeTemplateData(String json) {
        if (!StringUtils.hasText(json)) return new HashMap<>();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    /** 桥接：任意键 Map → String 键 Map（NodeSubmitDTO.baseData 键仍为 Long，fieldId 迁移后为 String） */
    private Map<String, String> toStringKeyMap(Map<?, String> src) {
        if (src == null) return null;
        Map<String, String> m = new LinkedHashMap<>();
        for (Map.Entry<?, String> e : src.entrySet()) {
            if (e.getKey() != null) m.put(String.valueOf(e.getKey()), e.getValue());
        }
        return m;
    }

    // ==================== 期次节点快照（flow_task_dispatch_node）解析 ====================

    /** 任务→期次快照节点列表（按 sort_num 升序；无快照返回 null，调用方降级模板解析） */
    private List<FlowTaskDispatchNode> snapshotNodesOf(FlowTask task) {
        if (task == null || task.getDispatchId() == null) return null;
        List<FlowTaskDispatchNode> list = flowTaskDispatchNodeMapper.selectList(
                new LambdaQueryWrapper<FlowTaskDispatchNode>()
                        .eq(FlowTaskDispatchNode::getDispatchId, task.getDispatchId())
                        .orderByAsc(FlowTaskDispatchNode::getSortNum));
        return list.isEmpty() ? null : list;
    }

    /** 期次模板级字段定义快照（flow_task_dispatch.template_fields_json；无快照返回 null） */
    private List<FlowTemplateField> snapshotTemplateFieldsOf(FlowTask task) {
        if (task == null || task.getDispatchId() == null) return null;
        FlowTaskDispatch d = flowTaskDispatchMapper.selectById(task.getDispatchId());
        if (d == null) return null;
        return parseSnapshotFields(d.getTemplateFieldsJson());
    }

    /** 快照字段 JSON → 字段定义列表 */
    private List<FlowTemplateField> parseSnapshotFields(String fieldsJson) {
        if (!StringUtils.hasText(fieldsJson)) return null;
        try {
            return objectMapper.readValue(fieldsJson, new TypeReference<List<FlowTemplateField>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    /** 快照节点 → 视图节点（id=快照 id；nodeTips/guideText/guideFiles/nextHandlerTip 一并带出，供处理人弹窗使用） */
    private FlowTemplateNode toTemplateNode(FlowTaskDispatchNode s) {
        FlowTemplateNode n = new FlowTemplateNode();
        n.setId(s.getId());
        n.setNodeName(s.getNodeName());
        n.setSortNum(s.getSortNum());
        n.setNodeType(s.getNodeType());
        n.setNodeTips(s.getNodeTips());
        n.setGuideText(s.getGuideText());
        n.setGuideFiles(s.getGuideFiles());
        n.setNextHandlerTip(s.getNextHandlerTip());
        return n;
    }

    /** 快照 id → 快照节点 */
    private FlowTaskDispatchNode snapshotById(List<FlowTaskDispatchNode> snaps, String id) {
        if (snaps == null || id == null) return null;
        for (FlowTaskDispatchNode s : snaps) {
            if (id.equals(s.getId())) return s;
        }
        return null;
    }

    /** 任务节点配置解析（快照优先，降级模板）：返回视图节点或 null */
    private FlowTemplateNode resolveNodeFor(FlowTask task, String nodeId) {
        if (nodeId == null) return null;
        List<FlowTaskDispatchNode> snaps = snapshotNodesOf(task);
        if (snaps != null) {
            FlowTaskDispatchNode hit = snapshotById(snaps, nodeId);
            if (hit != null) return toTemplateNode(hit);
            return null;
        }
        FlowTemplateNode direct = flowTemplateNodeMapper.selectById(nodeId);
        return direct != null && task.getTemplateId() != null
                && task.getTemplateId().equals(direct.getTemplateId()) ? direct : null;
    }

    /** 下一快照节点（sort_num > currentSort，取最小）；无快照或没有下一节点返回 null */
    private FlowTemplateNode nextSnapshotNode(FlowTask task, Integer currentSort) {
        List<FlowTaskDispatchNode> snaps = snapshotNodesOf(task);
        if (snaps != null) {
            FlowTaskDispatchNode next = null;
            for (FlowTaskDispatchNode s : snaps) {
                if (currentSort == null || (s.getSortNum() != null && s.getSortNum() > currentSort)) {
                    if (next == null || s.getSortNum() < next.getSortNum()) next = s;
                }
            }
            return next == null ? null : toTemplateNode(next);
        }
        if (currentSort == null) return null;
        LambdaQueryWrapper<FlowTemplateNode> nextW = new LambdaQueryWrapper<>();
        nextW.eq(FlowTemplateNode::getTemplateId, task.getTemplateId())
             .gt(FlowTemplateNode::getSortNum, currentSort)
             .orderByAsc(FlowTemplateNode::getSortNum).last("LIMIT 1");
        return flowTemplateNodeMapper.selectOne(nextW);
    }

    /** 聚合任务全部字段定义（快照节点字段 + 期次模板级字段），用于 label/type 回填展示；无快照返回 null */
    private Map<String, FlowTemplateField> allSnapshotFieldMap(FlowTask task) {
        List<FlowTaskDispatchNode> snaps = snapshotNodesOf(task);
        Map<String, FlowTemplateField> map = new HashMap<>();
        if (snaps != null) {
            for (FlowTaskDispatchNode s : snaps) {
                List<FlowTemplateField> fs = parseSnapshotFields(s.getFieldsJson());
                if (fs != null) for (FlowTemplateField f : fs) map.putIfAbsent(f.getId(), f);
            }
        }
        List<FlowTemplateField> tfs = snapshotTemplateFieldsOf(task);
        if (tfs != null) for (FlowTemplateField f : tfs) map.putIfAbsent(f.getId(), f);
        return map.isEmpty() ? null : map;
    }

    /** 期次分页：某任务下的期次列表（数据后台第二层），含成员聚合状态 */
    public PageResult<TaskGroupVO> getPage(FlowTaskQueryForm form) {
        int page = form.getPage() == null ? 1 : form.getPage();
        int limit = form.getLimit() == null ? 10 : form.getLimit();
        int offset = (page - 1) * limit;
        String taskName = StringUtils.hasText(form.getTaskName()) ? form.getTaskName() : null;
        String status = form.getStatus();
        List<TaskGroupVO> groups = flowTaskDispatchMapper.selectDispatchPage(form.getTaskId(), taskName, status, offset, limit);
        Long total = flowTaskDispatchMapper.selectDispatchCount(form.getTaskId(), taskName, status);
        return new PageResult<>(groups, total);
    }

    /** 成员分页查询（按姓名/部门/状态过滤，防止成员过多一次拉取） */
    public PageResult<TaskMemberVO> getMembersPage(String dispatchId, Integer page, Integer limit,
                                                   String name, String dept, String status) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        String n = (name == null || name.trim().isEmpty()) ? null : name.trim();
        String d = (dept == null || dept.trim().isEmpty()) ? null : dept.trim();
        List<TaskMemberVO> list = flowTaskMapper.selectMembersPage(dispatchId, n, d, status, offset, l);
        for (TaskMemberVO m : list) fillMemberNodeSteps(m);
        Long total = flowTaskMapper.selectMembersCount(dispatchId, n, d, status);
        return new PageResult<>(list, total);
    }

    /** 为成员填充流程节点步骤（done/current/rejected/pending），供成员卡片横向展示（快照优先） */
    private void fillMemberNodeSteps(TaskMemberVO m) {
        if (m == null || m.getTaskId() == null) return;
        FlowTask task = flowTaskMapper.selectById(m.getTaskId());
        if (task == null) return;
        List<FlowTaskDispatchNode> snaps = snapshotNodesOf(task);
        List<FlowTemplateNode> tplNodes;
        if (snaps != null) {
            tplNodes = new ArrayList<>();
            for (FlowTaskDispatchNode s : snaps) tplNodes.add(toTemplateNode(s));
        } else {
            if (m.getTemplateId() == null) return;
            tplNodes = flowTemplateNodeMapper.selectList(
                    new LambdaQueryWrapper<FlowTemplateNode>()
                            .eq(FlowTemplateNode::getTemplateId, m.getTemplateId())
                            .orderByAsc(FlowTemplateNode::getSortNum));
        }
        if (tplNodes.isEmpty()) return;
        List<FlowTaskNode> taskNodes = flowTaskNodeMapper.selectList(
                new LambdaQueryWrapper<FlowTaskNode>()
                        .eq(FlowTaskNode::getTaskId, m.getTaskId()));
        // legacy nodeId → 视图节点（快照场景 id 直接命中；否则按 direct id / sortNum / nodeName 逐级匹配）
        Map<String, FlowTemplateNode> remap = new HashMap<>();
        if (snaps != null) {
            for (FlowTemplateNode t : tplNodes) remap.put(t.getId(), t);
        } else {
            Map<Integer, FlowTemplateNode> bySort = new HashMap<>();
            Map<String, FlowTemplateNode> byName = new HashMap<>();
            for (FlowTemplateNode n : tplNodes) {
                if (n.getSortNum() != null) bySort.putIfAbsent(n.getSortNum(), n);
                if (n.getNodeName() != null) byName.putIfAbsent(n.getNodeName(), n);
            }
            for (FlowTaskNode tn : taskNodes) {
                if (tn.getNodeId() == null || remap.containsKey(tn.getNodeId())) continue;
                FlowTemplateNode direct = flowTemplateNodeMapper.selectById(tn.getNodeId());
                if (direct != null && m.getTemplateId() != null && m.getTemplateId().equals(direct.getTemplateId())) {
                    remap.put(tn.getNodeId(), direct);
                    continue;
                }
                FlowTemplateNode hit = (tn.getSortNum() != null) ? bySort.get(tn.getSortNum()) : null;
                if (hit == null && tn.getNodeName() != null) hit = byName.get(tn.getNodeName());
                if (hit == null) hit = tplNodes.get(tplNodes.size() - 1);
                remap.put(tn.getNodeId(), hit);
            }
        }
        // 按当前模板节点聚合 task_node
        Map<String, List<FlowTaskNode>> byNode = new HashMap<>();
        for (FlowTaskNode tn : taskNodes) {
            FlowTemplateNode t = remap.get(tn.getNodeId());
            if (t == null) continue;
            byNode.computeIfAbsent(t.getId(), k -> new ArrayList<>()).add(tn);
        }
        List<MemberNodeStepVO> steps = new ArrayList<>();
        int stepNo = 1;
        for (FlowTemplateNode tpl : tplNodes) {
            List<FlowTaskNode> ns = byNode.getOrDefault(tpl.getId(), Collections.emptyList());
            MemberNodeStepVO s = new MemberNodeStepVO();
            s.setStepNo(stepNo++);
            s.setNodeId(tpl.getId());
            s.setNodeName(tpl.getNodeName());
            boolean done = ns.stream().anyMatch(t -> t.getSubmitStatus() != null && t.getSubmitStatus() == 1);
            boolean hasPending = ns.stream().anyMatch(t -> t.getSubmitStatus() != null && t.getSubmitStatus() == 0);
            if (done) {
                // 最新一条已处理记录若为退回 → rejected，否则 done
                boolean rejected = ns.stream()
                        .filter(t -> t.getSubmitStatus() != null && t.getSubmitStatus() == 1)
                        .max(Comparator.comparing(FlowTaskNode::getId))
                        .map(t -> t.getAction() != null && t.getAction() == 1).orElse(false);
                s.setStatus(rejected ? "rejected" : "done");
            } else if (hasPending) s.setStatus("current");
            else s.setStatus("pending");
            steps.add(s);
        }
        m.setNodeSteps(steps);
    }

    /** 期次详情：期次头 + 全部成员（数据后台第三层）。空期次（无成员）仍返回，便于补员/删除 */
    public TaskGroupVO getDispatchDetail(String dispatchId) {
        FlowTaskDispatch dispatch = flowTaskDispatchMapper.selectById(dispatchId);
        if (dispatch == null) return null;
        List<TaskMemberVO> members = flowTaskMapper.selectMembersByDispatch(dispatchId);
        TaskGroupVO vo = new TaskGroupVO();
        vo.setDispatchId(dispatchId);
        vo.setPrimaryTaskId(dispatchId);
        vo.setTaskName(dispatch.getTaskName());
        vo.setTaskDesc(dispatch.getTaskDesc());
        vo.setTemplateId(dispatch.getTemplateId());
        vo.setDispatchPlanId(dispatch.getTaskId());
        vo.setPlanName(taskNameOf(dispatch.getTaskId()));
        vo.setPeriodNo(dispatch.getPeriodNo());
        vo.setPeriodName(dispatch.getPeriodName());
        vo.setManualFlag(dispatch.getManualFlag());
        vo.setStartTime(dispatch.getStartTime());
        vo.setEndTime(dispatch.getEndTime());
        vo.setDispatchTime(dispatch.getCreateTime());
        vo.setMembers(members);
        int running = 0, finished = 0, cancelled = 0;
        for (TaskMemberVO m : members) {
            if (m.getStatus() == null) continue;
            if ("进行中".equals(m.getStatus())) running++;
            else if ("已结束".equals(m.getStatus())) finished++;
            else if ("已作废".equals(m.getStatus())) cancelled++;
        }
        vo.setMemberCount(members.size());
        vo.setRunningCount(running);
        vo.setFinishedCount(finished);
        vo.setCancelledCount(cancelled);
        // 聚合状态：空组→0；任一进行中→1；全作废→3；否则→2（与列表/统计口径一致）
        vo.setStatus(members.isEmpty() ? 0 : (running > 0 ? 1 : (cancelled == members.size() ? 3 : 2)));
        return vo;
    }

    /** 所属任务名称（flow_dispatch.task_name） */
    private String taskNameOf(String taskId) {
        if (taskId == null) return null;
        FlowDispatch task = flowDispatchMapper.selectById(taskId);
        return task == null ? null : task.getTaskName();
    }

    /**
     * 解析当前模板节点：处理模板节点ID在重建后变成悬空引用的场景
     * 优先按 nodeId 直接查；查不到时按 taskId 的 task_node 找到该 legacyId 对应的 sort_num/node_name，
     * 再用 templateId + sort_num 反查当前模板同位置节点；仍找不到回退到模板 sort_num 最大节点
     * @return 当前模板中的有效 FlowTemplateNode
     */
    private FlowTemplateNode resolveTemplateNode(String templateId, String legacyNodeId, String taskId) {
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
    private java.util.Map<String, FlowTemplateNode> buildNodeIdRemap(String templateId, List<TaskProgressVO> progress) {
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
        java.util.Map<String, FlowTemplateNode> remap = new java.util.HashMap<>();
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
    public TaskDetailVO getDetail(String id) {
        FlowTask task = flowTaskMapper.selectById(id);
        if (task == null) return null;
        List<TaskProgressVO> progress = flowTaskNodeMapper.selectTaskProgress(id);
        TaskDetailVO vo = new TaskDetailVO();
        vo.setTask(task);
        vo.setTaskNodes(progress);
        // 节点链（期次快照优先，模板升级后历史期次仍展示下发时的节点配置）
        List<FlowTaskDispatchNode> snaps = snapshotNodesOf(task);
        List<FlowTemplateNode> tplNodes;
        if (snaps != null) {
            tplNodes = new ArrayList<>();
            for (FlowTaskDispatchNode s : snaps) tplNodes.add(toTemplateNode(s));
        } else {
            LambdaQueryWrapper<FlowTemplateNode> tnw = new LambdaQueryWrapper<>();
            tnw.eq(FlowTemplateNode::getTemplateId, task.getTemplateId()).orderByAsc(FlowTemplateNode::getSortNum);
            tplNodes = flowTemplateNodeMapper.selectList(tnw);
        }
        vo.setTemplateNodes(tplNodes);
        // 归一化 progress 的 nodeId（快照 id 直接命中；无快照降级模板 remap）
        java.util.Map<String, FlowTemplateNode> remap = new java.util.HashMap<>();
        if (snaps != null) {
            for (FlowTaskDispatchNode s : snaps) remap.put(s.getId(), toTemplateNode(s));
        } else {
            remap = buildNodeIdRemap(task.getTemplateId(), progress);
        }
        for (TaskProgressVO p : progress) {
            FlowTemplateNode target = remap.get(p.getNodeId());
            if (target != null) {
                p.setNodeId(target.getId());
                p.setNodeName(target.getNodeName());
                p.setNodeType(target.getNodeType());
                p.setSortNum(target.getSortNum());
            }
        }
        // 当前处理人查看时，返回当前节点字段配置（快照优先，降级 resolve 模板节点）
        LoginUser loginUser = SecurityUtils.getLoginUser();
        FlowTemplateNode currentNode = null;
        if (loginUser != null && "进行中".equals(task.getStatus())) {
            if (snaps != null) {
                FlowTaskDispatchNode cur = snapshotById(snaps, task.getCurrentNodeId());
                currentNode = cur == null ? null : toTemplateNode(cur);
            } else {
                currentNode = resolveTemplateNode(task.getTemplateId(), task.getCurrentNodeId(), id);
            }
            if (currentNode != null) {
                List<FlowTemplateField> cf = null;
                if (snaps != null) {
                    FlowTaskDispatchNode cur = snapshotById(snaps, currentNode.getId());
                    if (cur != null) cf = parseSnapshotFields(cur.getFieldsJson());
                }
                if (cf != null) {
                    vo.setCurrentNodeFields(cf);
                } else {
                    LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
                    fw.eq(FlowTemplateField::getNodeId, currentNode.getId()).orderByAsc(FlowTemplateField::getSortNum);
                    vo.setCurrentNodeFields(flowTemplateFieldMapper.selectList(fw));
                }
            }
            // 退回重填：取当前节点最近一条已 done task_node 的表单数据，预填回表单
            vo.setCurrentFormData(loadCurrentNodeFormData(task));
        }
        // 回填各已处理节点的历史表单数据（点击查看用）
        fillHistoryFormData(progress, task);
        // 回填各已处理节点处理人填写的任务基础字段（fieldRole=2）
        fillBaseData(progress, task);
        // 模板级字段配置（node_id 为空，不依附节点）+ 创建人下发的值
        List<FlowTemplateField> tplFieldDefs = snapshotTemplateFieldsOf(task);
        if (tplFieldDefs == null) {
            LambdaQueryWrapper<FlowTemplateField> tfw = new LambdaQueryWrapper<>();
            tfw.eq(FlowTemplateField::getTemplateId, task.getTemplateId()).isNull(FlowTemplateField::getNodeId)
               .orderByAsc(FlowTemplateField::getSortNum);
            tplFieldDefs = flowTemplateFieldMapper.selectList(tfw);
        }
        vo.setTemplateFields(tplFieldDefs);
        vo.setTemplateData(deserializeTemplateData(task.getTemplateData()));
        // 处理人填写的任务基础字段配置（fieldRole=2，绑定到当前节点的才展示：仅处理该节点时填写）
        if (currentNode != null) {
            List<FlowTemplateField> hb = null;
            if (snaps != null) {
                FlowTaskDispatchNode cur = snapshotById(snaps, currentNode.getId());
                List<FlowTemplateField> cf = cur == null ? null : parseSnapshotFields(cur.getFieldsJson());
                if (cf != null) {
                    hb = cf.stream().filter(f -> f.getFieldRole() != null && f.getFieldRole() == 2)
                            .collect(Collectors.toList());
                }
            }
            if (hb == null) {
                LambdaQueryWrapper<FlowTemplateField> hbw = new LambdaQueryWrapper<>();
                hbw.eq(FlowTemplateField::getTemplateId, task.getTemplateId())
                   .isNull(FlowTemplateField::getNodeId)
                   .eq(FlowTemplateField::getFieldRole, 2)
                   .eq(FlowTemplateField::getBindNodeId, currentNode.getId())
                   .orderByAsc(FlowTemplateField::getSortNum);
                hb = flowTemplateFieldMapper.selectList(hbw);
            }
            vo.setHandlerBaseFields(hb == null || hb.isEmpty() ? null : hb);
            // 当前处理人最近一次提交值（退回重做回填，仅回填绑定节点的）
            vo.setCurrentBaseData(loadCurrentBaseData(task));
        }
        // 处理人填写的任务基础字段汇总值：各绑定节点已提交的最新值，同步展示在任务基础信息区
        vo.setHandlerBaseData(loadAllBaseData(progress));
        return vo;
    }

    /** 聚合任务全部已提交节点中处理人填写的任务基础字段值（fieldId → 最新提交值） */
    private Map<String, String> loadAllBaseData(List<TaskProgressVO> progress) {
        if (progress == null || progress.isEmpty()) return null;
        List<String> tnIds = progress.stream().map(TaskProgressVO::getTaskNodeId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toList());
        if (tnIds.isEmpty()) return null;
        List<FlowTaskNode> tns = flowTaskNodeMapper.selectBatchIds(tnIds);
        // 按节点记录ID升序合并，后提交的值覆盖先提交的值（同一字段可能被退回重做多次填写）
        tns.sort(java.util.Comparator.comparing(FlowTaskNode::getId, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())));
        Map<String, String> result = new LinkedHashMap<>();
        for (FlowTaskNode tn : tns) {
            if (tn.getSubmitStatus() == null || tn.getSubmitStatus() != 1) continue;
            if (tn.getBaseData() == null || tn.getBaseData().trim().isEmpty()) continue;
            Map<String, String> map = deserializeTemplateData(tn.getBaseData());
            if (map.isEmpty()) continue;
            result.putAll(map);
        }
        return result.isEmpty() ? null : result;
    }

    /** 当前节点当前登录用户的待处理 task_node（用于草稿回填定位） */
    private FlowTaskNode findCurrentPendingNode(FlowTask task) {
        if (task.getCurrentNodeId() == null) return null;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return null;
        LambdaQueryWrapper<FlowTaskNode> w = new LambdaQueryWrapper<>();
        w.eq(FlowTaskNode::getTaskId, task.getId())
         .eq(FlowTaskNode::getNodeId, task.getCurrentNodeId())
         .eq(FlowTaskNode::getHandlerUserId, loginUser.getYyytId())
         .eq(FlowTaskNode::getSubmitStatus, 0)
         .orderByDesc(FlowTaskNode::getId).last("LIMIT 1");
        return flowTaskNodeMapper.selectOne(w);
    }

    /** 某 task_node 最近一条暂存草稿记录（无草稿返回 null） */
    private FlowFormRecord findDraftRecord(String taskNodeId) {
        if (taskNodeId == null) return null;
        return flowFormRecordMapper.selectOne(new LambdaQueryWrapper<FlowFormRecord>()
                .eq(FlowFormRecord::getTaskNodeId, taskNodeId)
                .eq(FlowFormRecord::getIsDraft, 1)
                .orderByDesc(FlowFormRecord::getId).last("LIMIT 1"));
    }

    /** 加载当前节点当前处理人的任务基础字段值（草稿优先；无草稿则取最近一次已提交值，仅回填本人的） */
    private Map<String, String> loadCurrentBaseData(FlowTask task) {
        if (task.getCurrentNodeId() == null) return null;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return null;
        // 草稿优先：当前用户当前节点存在暂存记录则直接回填
        FlowTaskNode pendingNode = findCurrentPendingNode(task);
        if (pendingNode != null) {
            FlowFormRecord draft = findDraftRecord(pendingNode.getId());
            if (draft != null) {
                FlowFormData bd = flowFormDataMapper.selectOne(new LambdaQueryWrapper<FlowFormData>()
                        .eq(FlowFormData::getRecordId, draft.getId())
                        .eq(FlowFormData::getFieldId, DRAFT_BASE_FIELD_ID)
                        .last("LIMIT 1"));
                if (bd != null && StringUtils.hasText(bd.getFieldValue())) {
                    Map<String, String> map = deserializeTemplateData(bd.getFieldValue());
                    if (!map.isEmpty()) return map;
                }
            }
        }
        // 已提交逻辑（退回重做回填）
        LambdaQueryWrapper<FlowTaskNode> tnw = new LambdaQueryWrapper<>();
        tnw.eq(FlowTaskNode::getTaskId, task.getId())
           .eq(FlowTaskNode::getNodeId, task.getCurrentNodeId())
           .eq(FlowTaskNode::getHandlerUserId, loginUser.getYyytId())
           .eq(FlowTaskNode::getSubmitStatus, 1)
           .orderByDesc(FlowTaskNode::getId).last("LIMIT 1");
        FlowTaskNode lastDone = flowTaskNodeMapper.selectOne(tnw);
        // 退回重做场景：本人未填过（任一完成即可时被自动关单）→ 取该节点最近一次任何人的提交
        if (lastDone == null || lastDone.getBaseData() == null || lastDone.getBaseData().trim().isEmpty()) {
            LambdaQueryWrapper<FlowTaskNode> anyw = new LambdaQueryWrapper<>();
            anyw.eq(FlowTaskNode::getTaskId, task.getId())
                .eq(FlowTaskNode::getNodeId, task.getCurrentNodeId())
                .eq(FlowTaskNode::getSubmitStatus, 1)
                .orderByDesc(FlowTaskNode::getId).last("LIMIT 1");
            FlowTaskNode anyDone = flowTaskNodeMapper.selectOne(anyw);
            if (anyDone != null && anyDone.getBaseData() != null && !anyDone.getBaseData().trim().isEmpty()) {
                lastDone = anyDone;
            }
        }
        if (lastDone == null || lastDone.getBaseData() == null || lastDone.getBaseData().trim().isEmpty()) return null;
        Map<String, String> map = deserializeTemplateData(lastDone.getBaseData());
        return map.isEmpty() ? null : map;
    }

    /** 聚合任务全部字段定义（快照优先，降级模板），返回非空 map（label/type 回填展示用） */
    /** 当前模板字段按 field_key 索引（历史无快照数据 field_id 失效时兜底取中文 label） */
    private Map<String, FlowTemplateField> templateFieldKeyMap(String templateId) {
        Map<String, FlowTemplateField> keyMap = new HashMap<>();
        if (templateId != null) {
            flowTemplateFieldMapper.selectList(
                    new LambdaQueryWrapper<FlowTemplateField>().eq(FlowTemplateField::getTemplateId, templateId))
                    .forEach(f -> keyMap.putIfAbsent(f.getFieldKey(), f));
        }
        return keyMap;
    }

    private Map<String, FlowTemplateField> resolveAllFieldMap(FlowTask task) {
        Map<String, FlowTemplateField> fmap = allSnapshotFieldMap(task);
        if (fmap != null) return fmap;
        LambdaQueryWrapper<FlowTemplateField> tfw = new LambdaQueryWrapper<>();
        tfw.eq(FlowTemplateField::getTemplateId, task.getTemplateId());
        return flowTemplateFieldMapper.selectList(tfw).stream()
                .collect(Collectors.toMap(FlowTemplateField::getId, f -> f, (a, b) -> a));
    }

    /** 回填各已处理 task_node 的处理人填写任务基础字段（base_data，fieldRole=2；快照 label/type 优先） */
    private void fillBaseData(List<TaskProgressVO> progress, FlowTask task) {
        if (progress == null || progress.isEmpty()) return;
        List<String> tnIds = progress.stream().map(TaskProgressVO::getTaskNodeId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toList());
        if (tnIds.isEmpty()) return;
        List<FlowTaskNode> tns = flowTaskNodeMapper.selectBatchIds(tnIds);
        // 字段 label/type 映射（快照优先，降级模板）
        Map<String, FlowTemplateField> fmap = resolveAllFieldMap(task);
        Map<String, List<FormDataItemVO>> byTn = new HashMap<>();
        for (FlowTaskNode tn : tns) {
            if (tn.getBaseData() == null || tn.getBaseData().trim().isEmpty()) continue;
            Map<String, String> map = deserializeTemplateData(tn.getBaseData());
            if (map.isEmpty()) continue;
            List<FormDataItemVO> items = map.entrySet().stream().map(e -> {
                FormDataItemVO item = new FormDataItemVO();
                item.setFieldId(String.valueOf(e.getKey()));
                FlowTemplateField f = fmap.get(e.getKey());
                item.setFieldLabel(f == null ? String.valueOf(e.getKey()) : f.getFieldLabel());
                item.setFieldValue(e.getValue());
                item.setFieldType(f == null ? null : f.getFieldType());
                return item;
            }).collect(Collectors.toList());
            byTn.put(tn.getId(), items);
        }
        for (TaskProgressVO p : progress) {
            List<FormDataItemVO> items = byTn.get(p.getTaskNodeId());
            if (items != null) p.setBaseDataList(items);
        }
    }

    /** 加载当前节点当前处理人的表单数据（草稿优先；无草稿则取最近一次已提交表单，用于退回后回填，仅回填本人的） */
    private List<FlowFormData> loadCurrentNodeFormData(FlowTask task) {
        if (task.getCurrentNodeId() == null) return null;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return null;
        // 草稿优先：当前用户当前节点存在暂存记录则直接回填（排除承载任务基础字段的特殊行）
        FlowTaskNode pendingNode = findCurrentPendingNode(task);
        if (pendingNode != null) {
            FlowFormRecord draft = findDraftRecord(pendingNode.getId());
            if (draft != null) {
                LambdaQueryWrapper<FlowFormData> fdw = new LambdaQueryWrapper<>();
                fdw.eq(FlowFormData::getRecordId, draft.getId())
                   .ne(FlowFormData::getFieldId, DRAFT_BASE_FIELD_ID);
                List<FlowFormData> draftData = flowFormDataMapper.selectList(fdw);
                if (draftData != null && !draftData.isEmpty()) return draftData;
            }
        }
        // 当前节点最近一条「当前登录用户」已 done 的 task_node（避免回填他人的数据）
        LambdaQueryWrapper<FlowTaskNode> tnw = new LambdaQueryWrapper<>();
        tnw.eq(FlowTaskNode::getTaskId, task.getId())
           .eq(FlowTaskNode::getNodeId, task.getCurrentNodeId())
           .eq(FlowTaskNode::getHandlerUserId, loginUser.getYyytId())
           .eq(FlowTaskNode::getSubmitStatus, 1)
           .orderByDesc(FlowTaskNode::getId).last("LIMIT 1");
        FlowTaskNode lastDone = flowTaskNodeMapper.selectOne(tnw);
        // 退回重做场景：本人未填过（任一完成即可时被自动关单，无表单记录）→ 取该节点最近一次任何人的提交，
        // 保证重做人能看到节点上已有的表单数据（一份表单，多人入口）
        if (lastDone == null || lastDone.getFormRecordId() == null) {
            LambdaQueryWrapper<FlowTaskNode> anyw = new LambdaQueryWrapper<>();
            anyw.eq(FlowTaskNode::getTaskId, task.getId())
                .eq(FlowTaskNode::getNodeId, task.getCurrentNodeId())
                .eq(FlowTaskNode::getSubmitStatus, 1)
                .isNotNull(FlowTaskNode::getFormRecordId)
                .orderByDesc(FlowTaskNode::getId).last("LIMIT 1");
            lastDone = flowTaskNodeMapper.selectOne(anyw);
        }
        if (lastDone == null || lastDone.getFormRecordId() == null) return null;
        LambdaQueryWrapper<FlowFormData> fdw = new LambdaQueryWrapper<>();
        fdw.eq(FlowFormData::getRecordId, lastDone.getFormRecordId());
        return flowFormDataMapper.selectList(fdw);
    }

    /** 回填各已处理 task_node 的表单数据（关联字段取 fieldLabel；快照 label/type 优先） */
    private void fillHistoryFormData(List<TaskProgressVO> progress, FlowTask task) {
        List<String> recordIds = progress.stream()
                .filter(n -> n.getFormRecordId() != null)
                .map(TaskProgressVO::getFormRecordId)
                .collect(Collectors.toList());
        if (recordIds.isEmpty()) return;
        // 一次查询所有 form_data
        LambdaQueryWrapper<FlowFormData> fdw = new LambdaQueryWrapper<>();
        fdw.in(FlowFormData::getRecordId, recordIds);
        List<FlowFormData> allData = flowFormDataMapper.selectList(fdw);
        if (allData.isEmpty()) return;
        // 字段 label/type 映射（快照优先，降级模板）
        Map<String, FlowTemplateField> fmap = resolveAllFieldMap(task);
        // field_key 兜底：历史无快照数据 field_id 失效时，按 field_key 从当前模板取中文 label
        Map<String, FlowTemplateField> keyMap = templateFieldKeyMap(task.getTemplateId());
        // 按 recordId 分组
        Map<String, List<FlowFormData>> dataByRecord = allData.stream()
                .collect(Collectors.groupingBy(FlowFormData::getRecordId));
        // 回填到各 TaskProgressVO
        for (TaskProgressVO n : progress) {
            if (n.getFormRecordId() == null) continue;
            List<FlowFormData> dataList = dataByRecord.get(n.getFormRecordId());
            if (dataList == null || dataList.isEmpty()) continue;
            List<FormDataItemVO> items = dataList.stream().map(fd -> {
                FormDataItemVO item = new FormDataItemVO();
                item.setFieldId(fd.getFieldId());
                FlowTemplateField f = fmap.get(fd.getFieldId());
                if (f == null) f = keyMap.get(fd.getFieldKey());
                item.setFieldLabel(f == null ? fd.getFieldKey() : f.getFieldLabel());
                item.setFieldValue(fd.getFieldValue());
                item.setFieldType(f == null ? null : f.getFieldType());
                return item;
            }).collect(Collectors.toList());
            n.setFormDataList(items);
        }
    }

    /**
     * 期次内新增人员：以期次为对象，为每个新增人员创建一条独立成员任务（从开始节点重新走流程），归入本期次。
     * 每个成员任务都是独立的、互不影响——即便某人在其他任务里做过某节点处理人，也不影响为其建独立任务。
     * 已是本期次成员（起始节点处理人）的人员自动跳过，避免重复。
     * @return 实际创建的任务数量
     */
    @Transactional(rollbackFor = Exception.class)
    /** 按用户号查姓名（双冗余入库用） */
    private String userNameOf(String yyytId) {
        if (yyytId == null) return null;
        com.company.flow.sys.base.autuser.entity.User u = userMapper.selectById(yyytId);
        return u == null ? null : u.getUserName();
    }

    /** 为任务节点补齐处理人/下一处理人姓名（冗余双写） */
    private void fillNodeHandlerName(FlowTaskNode node) {
        if (node == null) return;
        if (node.getHandlerUserId() != null && node.getHandlerUserName() == null) {
            node.setHandlerUserName(userNameOf(node.getHandlerUserId()));
        }
        if (node.getNextHandlerUserId() != null && node.getNextHandlerUserName() == null) {
            node.setNextHandlerUserName(userNameOf(node.getNextHandlerUserId()));
        }
    }

    public int addHandlers(String dispatchId, List<String> handlerIds) {
        if (handlerIds == null || handlerIds.isEmpty()) {
            throw new RuntimeException("请至少选择一个处理人");
        }
        FlowTaskDispatch dispatch = flowTaskDispatchMapper.selectById(dispatchId);
        if (dispatch == null) throw new RuntimeException("任务不存在");
        // 期次快照节点（优先，保证与期次锁定版本一致）；存量期次无快照降级查当前模板
        List<FlowTaskDispatchNode> snapNodes = snapshotNodesOf(new FlowTask() {{
            setDispatchId(dispatchId);
        }});
        String firstNodeId;
        String firstNodeName;
        Integer firstNodeSort;
        Integer firstNodeType;
        int total;
        if (snapNodes != null && !snapNodes.isEmpty()) {
            FlowTaskDispatchNode firstSnap = snapNodes.get(0);
            firstNodeId = firstSnap.getId();
            firstNodeName = firstSnap.getNodeName();
            firstNodeSort = firstSnap.getSortNum();
            firstNodeType = firstSnap.getNodeType();
            total = snapNodes.size();
        } else {
            FlowTemplate tpl = flowTemplateMapper.selectById(dispatch.getTemplateId());
            if (tpl == null) throw new RuntimeException("模板不存在");
            // 开始节点
            LambdaQueryWrapper<FlowTemplateNode> sw = new LambdaQueryWrapper<>();
            sw.eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId())
              .eq(FlowTemplateNode::getNodeType, 1).last("LIMIT 1");
            FlowTemplateNode firstNode = flowTemplateNodeMapper.selectOne(sw);
            if (firstNode == null) throw new RuntimeException("模板缺少开始节点");
            firstNodeId = firstNode.getId();
            firstNodeName = firstNode.getNodeName();
            firstNodeSort = firstNode.getSortNum();
            firstNodeType = firstNode.getNodeType();
            LambdaQueryWrapper<FlowTemplateNode> countW = new LambdaQueryWrapper<>();
            countW.eq(FlowTemplateNode::getTemplateId, dispatch.getTemplateId());
            total = flowTemplateNodeMapper.selectCount(countW).intValue();
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String creatorId = loginUser == null ? null : loginUser.getYyytId();
        // 组内已有成员任务的起始节点处理人（去重，避免重复建独立任务）
        List<String> groupTaskIds = flowTaskMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getDispatchId, dispatchId))
                .stream().map(FlowTask::getId).collect(Collectors.toList());
        Set<String> memberOwners = groupTaskIds.isEmpty() ? new java.util.HashSet<>() :
                flowTaskNodeMapper.selectList(new LambdaQueryWrapper<FlowTaskNode>()
                        .in(FlowTaskNode::getTaskId, groupTaskIds)
                        .eq(FlowTaskNode::getNodeType, 1))
                        .stream().map(FlowTaskNode::getHandlerUserId)
                        .filter(java.util.Objects::nonNull)
                        .collect(Collectors.toSet());
        int count = 0;
        for (String handlerId : handlerIds) {
            if (handlerId == null) continue;
            if (memberOwners.contains(handlerId)) continue;
            FlowTask task = new FlowTask();
            task.setTemplateId(dispatch.getTemplateId());
            task.setTaskName(dispatch.getTaskName());
            task.setTaskDesc(dispatch.getTaskDesc());
            task.setTemplateData(dispatch.getTemplateData());
            task.setStartTime(dispatch.getStartTime());
            task.setEndTime(dispatch.getEndTime());
            task.setStatus("进行中");
            task.setTemplateVersion(dispatch.getTemplateVersion());
            task.setCurrentNodeId(firstNodeId);
            task.setCurrentHandlerId(handlerId);
            task.setFinishedNodeCount(0);
            task.setTotalNodeCount(total);
            task.setCreatorId(creatorId);
            task.setCreatorName(creatorId == null ? null : userNameOf(creatorId));
            task.setDispatchId(dispatchId);
            flowTaskMapper.insert(task);
            // 插首个 task_node（待处理）
            FlowTaskNode firstTaskNode = new FlowTaskNode();
            firstTaskNode.setTaskId(task.getId());
            firstTaskNode.setNodeId(firstNodeId);
            firstTaskNode.setDispatchNodeId(snapNodes == null || snapNodes.isEmpty() ? null : snapNodes.get(0).getId());
            firstTaskNode.setNodeName(firstNodeName);
            firstTaskNode.setSortNum(firstNodeSort);
            firstTaskNode.setNodeType(firstNodeType);
            firstTaskNode.setHandlerUserId(handlerId);
            firstTaskNode.setHandlerUserName(handlerId == null ? null : userNameOf(handlerId));
            firstTaskNode.setSubmitStatus(0);
            firstTaskNode.setAction(0);
            fillNodeHandlerName(firstTaskNode);
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
    public int removeHandler(String taskId, String handlerUserId) {
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
        if ("进行中".equals(task.getStatus()) && handlerUserId.equals(task.getCurrentHandlerId())) {
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
    public String submit(NodeSubmitDTO dto) {
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
        if (!"进行中".equals(task.getStatus())) throw new RuntimeException("任务已结束或作废，无法提交");
        if (task.getCurrentNodeId() == null || !task.getCurrentNodeId().equals(currentTaskNode.getNodeId())) {
            throw new RuntimeException("任务当前节点已变化，请刷新后重试");
        }
        // 归一化下一处理人ID列表（兼容旧字段 nextHandlerUserId）
        List<String> nextHandlerIds = new ArrayList<>();
        if (dto.getNextHandlerIds() != null) {
            for (Object h : dto.getNextHandlerIds()) {
                if (h != null) nextHandlerIds.add(String.valueOf(h));
            }
        }
        if (nextHandlerIds.isEmpty() && dto.getNextHandlerUserId() != null) {
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
        String recordId = saveFormRecordAndData(task, currentTaskNode, loginUser, dto.getFormData(), isReject);
        // 4.1 处理人填写的任务基础字段（fieldRole=2），随本节点提交持久化
        currentTaskNode.setBaseData(serializeTemplateData(dto.getBaseData()));

        if (isReject) {
            // ===== 退回到指定节点（当前处理人重做该节点） =====
            currentTaskNode.setSubmitStatus(1);
            currentTaskNode.setAction(1);
            currentTaskNode.setHandleTime(new Date());
            currentTaskNode.setNextHandlerUserId(null);
            currentTaskNode.setFormRecordId(recordId);
            currentTaskNode.setRejectReason(dto.getRejectReason());
            flowTaskNodeMapper.updateById(currentTaskNode);
            // 同一节点其他 pending 分支：因退回而一并结束，避免残留他人待办（统计虚高/阻塞完成判定）
            LambdaQueryWrapper<FlowTaskNode> rejectSiblingW = new LambdaQueryWrapper<>();
            rejectSiblingW.eq(FlowTaskNode::getTaskId, task.getId())
                    .eq(FlowTaskNode::getNodeId, currentTaskNode.getNodeId())
                    .eq(FlowTaskNode::getSubmitStatus, 0)
                    .ne(FlowTaskNode::getId, currentTaskNode.getId());
            for (FlowTaskNode sib : flowTaskNodeMapper.selectList(rejectSiblingW)) {
                sib.setSubmitStatus(1);
                sib.setAction(1);
                sib.setHandleTime(new Date());
                sib.setRejectReason(dto.getRejectReason());
                flowTaskNodeMapper.updateById(sib);
            }
            // 查退回目标节点（快照优先，降级模板；须 sortNum < 当前）
            FlowTemplateNode targetNode = resolveNodeFor(task,
                    dto.getRejectToNodeId());
            if (targetNode == null) {
                throw new RuntimeException("退回目标节点不存在");
            }
            if (currentTaskNode.getSortNum() == null || targetNode.getSortNum() == null
                    || targetNode.getSortNum() >= currentTaskNode.getSortNum()) {
                throw new RuntimeException("退回目标须为已到达过的前置节点");
            }
            // 新建目标节点 task_node（待处理，由当前处理人重做）
            boolean targetFromSnap = snapshotNodesOf(task) != null;
            FlowTaskNode targetTaskNode = new FlowTaskNode();
            targetTaskNode.setTaskId(task.getId());
            targetTaskNode.setNodeId(targetNode.getId());
            targetTaskNode.setDispatchNodeId(targetFromSnap ? targetNode.getId() : null);
            targetTaskNode.setNodeName(targetNode.getNodeName());
            targetTaskNode.setSortNum(targetNode.getSortNum());
            targetTaskNode.setNodeType(targetNode.getNodeType());
            targetTaskNode.setHandlerUserId(loginUser.getYyytId());
            targetTaskNode.setSubmitStatus(0);
            targetTaskNode.setAction(0);
            // 携带退回建议：重做该节点的人能看到“为什么被退回”
            targetTaskNode.setRejectReason(dto.getRejectReason());
            fillNodeHandlerName(targetTaskNode);
            flowTaskNodeMapper.insert(targetTaskNode);
            // 退回重做：同步该节点原有处理人（如需求设计并行派给张三、李四时，退回后李四也要能重新处理）。
            // 从该节点历史已处理记录取原有处理人，为其补建待办（当前处理人已建 redo 跳过）
            LambdaQueryWrapper<FlowTaskNode> origDoneW = new LambdaQueryWrapper<>();
            origDoneW.eq(FlowTaskNode::getTaskId, task.getId())
                     .eq(FlowTaskNode::getNodeId, targetNode.getId())
                     .eq(FlowTaskNode::getSubmitStatus, 1);
            Set<String> origHandlers = flowTaskNodeMapper.selectList(origDoneW).stream()
                    .map(FlowTaskNode::getHandlerUserId)
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toSet());
            for (String h : origHandlers) {
                if (loginUser.getYyytId().equals(h)) continue; // 当前处理人已建 redo
                LambdaQueryWrapper<FlowTaskNode> dupW = new LambdaQueryWrapper<>();
                dupW.eq(FlowTaskNode::getTaskId, task.getId())
                     .eq(FlowTaskNode::getNodeId, targetNode.getId())
                     .eq(FlowTaskNode::getHandlerUserId, h)
                     .eq(FlowTaskNode::getSubmitStatus, 0);
                if (flowTaskNodeMapper.selectCount(dupW) > 0) continue;
                FlowTaskNode redo = new FlowTaskNode();
                redo.setTaskId(task.getId());
                redo.setNodeId(targetNode.getId());
                redo.setDispatchNodeId(targetFromSnap ? targetNode.getId() : null);
                redo.setNodeName(targetNode.getNodeName());
                redo.setSortNum(targetNode.getSortNum());
                redo.setNodeType(targetNode.getNodeType());
                redo.setHandlerUserId(h);
                redo.setSubmitStatus(0);
                redo.setAction(0);
                redo.setRejectReason(dto.getRejectReason());
                fillNodeHandlerName(redo);
                flowTaskNodeMapper.insert(redo);
            }
            // task 指针回到目标节点
            task.setCurrentNodeId(targetNode.getId());
            task.setCurrentHandlerId(loginUser.getYyytId());
            flowTaskMapper.updateById(task);
            // 流转通知日志：退回
            recordFlowLog(task, currentTaskNode, loginUser, 1,
                    "退回至「" + targetNode.getNodeName() + "」节点" +
                            (dto.getRejectReason() != null && !dto.getRejectReason().isEmpty()
                                    ? "，原因：" + dto.getRejectReason() : ""));
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
            // 查下一节点（快照优先，sort_num 大于当前）
            boolean nextFromSnap = snapshotNodesOf(task) != null;
            FlowTemplateNode nextNode = nextSnapshotNode(task, currentTaskNode.getSortNum());
            if (nextNode == null) {
                // 无下一节点，直接判定完成
                checkAndFinishTask(task);
            } else {
                // 为每个选中处理人创建一条 pending task_node（独立分支）
                String firstHandlerId = nextHandlerIds.get(0);
                for (String handlerId : nextHandlerIds) {
                    if (handlerId == null) continue;
                    FlowTaskNode nextTaskNode = new FlowTaskNode();
                    nextTaskNode.setTaskId(task.getId());
                    nextTaskNode.setNodeId(nextNode.getId());
                    nextTaskNode.setDispatchNodeId(nextFromSnap ? nextNode.getId() : null);
                    nextTaskNode.setNodeName(nextNode.getNodeName());
                    nextTaskNode.setSortNum(nextNode.getSortNum());
                    nextTaskNode.setNodeType(nextNode.getNodeType());
                    nextTaskNode.setHandlerUserId(handlerId);
                    nextTaskNode.setSubmitStatus(0);
                    nextTaskNode.setAction(0);
                    fillNodeHandlerName(nextTaskNode);
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
        // 流转通知日志：通过
        if (isEnd) {
            recordFlowLog(task, currentTaskNode, loginUser, 1,
                    "通过「" + currentTaskNode.getNodeName() + "」，流程完成");
        } else {
            FlowTemplateNode nextInfo = resolveNodeFor(task, task.getCurrentNodeId());
            recordFlowLog(task, currentTaskNode, loginUser, 1,
                    "通过「" + currentTaskNode.getNodeName() + "」，流转至「"
                            + (nextInfo != null ? nextInfo.getNodeName() : "下一节点") + "」");
        }
        return recordId;
    }

    /** 任务完成判定：无 pending task_node 残留则标记完成 */
    private void checkAndFinishTask(FlowTask task) {
        LambdaQueryWrapper<FlowTaskNode> pw = new LambdaQueryWrapper<>();
        pw.eq(FlowTaskNode::getTaskId, task.getId()).eq(FlowTaskNode::getSubmitStatus, 0);
        long pending = flowTaskNodeMapper.selectCount(pw);
        if (pending == 0) {
            task.setStatus("已结束");
            task.setFinishedNodeCount(recomputeFinishedCount(task.getId()));
            flowTaskMapper.updateById(task);
        }
    }

    // ==================== 暂存（草稿） ====================

    /** 草稿中承载「处理人任务基础字段值」的特殊 form_data 字段 ID/KEY */
    private static final String DRAFT_BASE_FIELD_ID = "-999";
    private static final String DRAFT_BASE_FIELD_KEY = "__DRAFT_BASE__";

    /**
     * 暂存（草稿）：把当前节点已填的表单数据写入 flow_form_record(is_draft=1)。
     * 不校验必填、不流转、不改任务状态；同一 task_node 的草稿幂等覆盖。
     * 下次打开处理弹窗时自动回填草稿数据。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveDraft(NodeSubmitDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) throw new RuntimeException("未登录");
        if (dto.getTaskNodeId() == null) throw new RuntimeException("参数缺失");
        FlowTaskNode currentTaskNode = flowTaskNodeMapper.selectById(dto.getTaskNodeId());
        if (currentTaskNode == null) throw new RuntimeException("节点不存在");
        if (currentTaskNode.getSubmitStatus() != null && currentTaskNode.getSubmitStatus() == 1) {
            throw new RuntimeException("该节点已处理，暂存无效");
        }
        FlowTask task = flowTaskMapper.selectById(dto.getTaskId());
        if (task == null) throw new RuntimeException("任务不存在");
        if (task.getStatus() != null && !"进行中".equals(task.getStatus())) {
            throw new RuntimeException("任务已结束或作废，无法暂存");
        }
        // 字段 key 补齐映射（快照优先，降级模板）
        Map<String, FlowTemplateField> fieldMap = resolveAllFieldMap(task);
        // 该 task_node 已有草稿则覆盖，否则新建
        FlowFormRecord draft = flowFormRecordMapper.selectOne(new LambdaQueryWrapper<FlowFormRecord>()
                .eq(FlowFormRecord::getTaskNodeId, dto.getTaskNodeId())
                .eq(FlowFormRecord::getIsDraft, 1)
                .orderByDesc(FlowFormRecord::getId).last("LIMIT 1"));
        if (draft == null) {
            draft = new FlowFormRecord();
            draft.setTaskId(task.getId());
            draft.setTemplateId(task.getTemplateId());
            draft.setNodeId(currentTaskNode.getNodeId());
            draft.setTaskNodeId(currentTaskNode.getId());
            draft.setUserId(loginUser.getYyytId());
            draft.setUserName(loginUser.getUserName());
            draft.setRecordStatus(1);
            draft.setIsDraft(1);
            flowFormRecordMapper.insert(draft);
        } else {
            flowFormDataMapper.delete(new LambdaQueryWrapper<FlowFormData>()
                    .eq(FlowFormData::getRecordId, draft.getId()));
        }
        String recordId = draft.getId();
        // 节点动态表单数据
        if (dto.getFormData() != null) {
            for (FlowFormData fd : dto.getFormData()) {
                fd.setId(null);
                fd.setRecordId(recordId);
                if (fd.getFieldKey() == null && fieldMap.containsKey(fd.getFieldId())) {
                    fd.setFieldKey(fieldMap.get(fd.getFieldId()).getFieldKey());
                }
                flowFormDataMapper.insert(fd);
            }
        }
        // 处理人填写的任务基础字段（fieldRole=2）：序列化存入一条特殊 form_data
        if (dto.getBaseData() != null && !dto.getBaseData().isEmpty()) {
            FlowFormData bd = new FlowFormData();
            bd.setRecordId(recordId);
            bd.setFieldId(DRAFT_BASE_FIELD_ID);
            bd.setFieldKey(DRAFT_BASE_FIELD_KEY);
            bd.setFieldValue(serializeTemplateData(dto.getBaseData()));
            flowFormDataMapper.insert(bd);
        }
    }

    /** 重算已完成节点数：有 ≥1 条已 done task_node 的 nodeId 数量 */
    private int recomputeFinishedCount(String taskId) {
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
    private String saveFormRecordAndData(FlowTask task, FlowTaskNode currentTaskNode, LoginUser loginUser, List<FlowFormData> formDataList, boolean skipRequired) {
        // 字段定义（快照优先：dispatch_node_id → fields_json；存量任务降级模板字段表）
        List<FlowTemplateField> nodeFields = null;
        if (currentTaskNode.getDispatchNodeId() != null) {
            FlowTaskDispatchNode snap = flowTaskDispatchNodeMapper.selectById(currentTaskNode.getDispatchNodeId());
            if (snap != null) nodeFields = parseSnapshotFields(snap.getFieldsJson());
        }
        if (nodeFields == null) {
            LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
            fw.eq(FlowTemplateField::getNodeId, currentTaskNode.getNodeId());
            nodeFields = flowTemplateFieldMapper.selectList(fw);
        }
        Map<String, FlowTemplateField> fieldMap = nodeFields.stream()
                .collect(Collectors.toMap(FlowTemplateField::getId, f -> f, (a, b) -> a));
        if (!skipRequired) {
            Map<String, String> submittedValues = formDataList == null ? new java.util.HashMap<>() :
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
        record.setUserId(loginUser.getYyytId());
        record.setUserName(loginUser.getUserName());
        record.setRecordStatus(1);
        record.setIsDraft(0);
        record.setSubmitTime(new Date());
        flowFormRecordMapper.insert(record);
        String recordId = record.getId();
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
        // 提交成功后清除该 task_node 的暂存草稿（避免残留）
        flowFormRecordMapper.delete(new LambdaQueryWrapper<FlowFormRecord>()
                .eq(FlowFormRecord::getTaskNodeId, currentTaskNode.getId())
                .eq(FlowFormRecord::getIsDraft, 1));
        return recordId;
    }

    /** 我的任务：当前登录用户参与的任务（待处理 + 已处理仅查看），按任务去重取最新节点 */
    public PageResult<MyTodoVO> myTodo(Integer page, Integer limit) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return new PageResult<>(new ArrayList<>(), 0L);
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        List<MyTodoVO> list = flowTaskNodeMapper.selectMyTodo(loginUser.getYyytId(), offset, l);
        Long total = flowTaskNodeMapper.selectMyTodoCount(loginUser.getYyytId());
        return new PageResult<>(list, total);
    }

    /** 我的任务（任务→期次 两级展示）：任务级分页，任务下按期次分组待办节点 */
    public PageResult<MyTodoTaskVO> myTodoGrouped(Integer page, Integer limit, String taskName, Integer status) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return new PageResult<>(new ArrayList<>(), 0L);
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        String name = StringUtils.hasText(taskName) ? taskName.trim() : null;
        List<MyTodoTaskVO> groups = flowTaskNodeMapper.selectMyTaskGroups(loginUser.getYyytId(), name, status, offset, l);
        Long total = flowTaskNodeMapper.selectMyTaskGroupCount(loginUser.getYyytId(), name, status);
        if (!groups.isEmpty()) {
            List<String> taskIds = groups.stream().map(MyTodoTaskVO::getTaskId).collect(Collectors.toList());
            List<MyTodoVO> nodes = flowTaskNodeMapper.selectMyTodoByTasks(loginUser.getYyytId(), taskIds);
            // 按计划任务（planId）分组：同一计划任务下的多期次归入同一组
            Map<String, List<MyTodoVO>> byTask = nodes.stream()
                    .collect(Collectors.groupingBy(n -> n.getPlanId() == null ? null : n.getPlanId(),
                            LinkedHashMap::new, Collectors.toList()));
            for (MyTodoTaskVO g : groups) {
                List<MyTodoVO> taskNodes = byTask.getOrDefault(g.getTaskId(), new ArrayList<>());
                // 按期次分组（dispatchId 为 null 的存量任务归入「无期次」组）
                Map<String, MyTodoPeriodVO> periodMap = new LinkedHashMap<>();
                for (MyTodoVO n : taskNodes) {
                    String key = n.getDispatchId() == null ? "" : String.valueOf(n.getDispatchId());
                    MyTodoPeriodVO per = periodMap.computeIfAbsent(key, k -> {
                        MyTodoPeriodVO v = new MyTodoPeriodVO();
                        v.setDispatchId(n.getDispatchId());
                        v.setPeriodName(n.getPeriodName());
                        v.setPeriodNo(n.getPeriodNo());
                        v.setStartTime(n.getPeriodStartTime());
                        v.setEndTime(n.getPeriodEndTime());
                        return v;
                    });
                    per.getTodos().add(n);
                }
                // 每个期次补充完整流程节点链（当前用户的完成进度）
                for (MyTodoPeriodVO per : periodMap.values()) {
                    // 同一期次可能有多个不同子任务的待办，各自构建独立流程链（不共用一条）
                    for (MyTodoVO t : per.getTodos()) {
                        t.setChains(buildTodoNodes(t));
                    }
                    if (!per.getTodos().isEmpty()) {
                        per.setNodes(buildTodoNodes(per.getTodos().get(0)));
                    }
                }
                g.setPeriods(new ArrayList<>(periodMap.values()));
            }
        }
        return new PageResult<>(groups, total);
    }

    /** 我的任务统计（统计卡）：任务数 / 待处理节点数 / 已完成节点数 */
    public MyTodoStatsVO myTodoStats() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null) return new MyTodoStatsVO();
        return flowTaskNodeMapper.selectMyTodoStats(loginUser.getYyytId());
    }

    /**
     * 构建某期次下当前用户的完整流程节点链（含完成状态）。
     * 以模板节点链为准（order by sort_num），用该用户实例（taskId）的提交记录标记：
     * 有记录且 submit_status=1 → 已完成；有记录且 submit_status=0 → 进行中（当前节点）；无记录 → 未开始。
     * 模板节点链可能被重建（node_id 悬空），故优先按 node_id 匹配，失败时按 sort_num 位置兜底匹配。
     */
    private List<TodoNodeVO> buildTodoNodes(MyTodoVO todo) {
        List<TodoNodeVO> result = new ArrayList<>();
        if (todo == null || todo.getTaskId() == null) return result;
        String taskId = todo.getTaskId();
        // 我的节点提交记录（按 id 排序，同节点多次记录时取最后一条，即最近状态）
        List<FlowTaskNode> myNodes = flowTaskNodeMapper.selectList(new LambdaQueryWrapper<FlowTaskNode>()
                .eq(FlowTaskNode::getTaskId, taskId)
                .orderByAsc(FlowTaskNode::getId));
        Map<String, Integer> statusByNode = new HashMap<>();
        Map<Integer, Integer> statusBySort = new HashMap<>();
        for (FlowTaskNode tn : myNodes) {
            if (tn.getNodeId() != null) {
                statusByNode.put(tn.getNodeId(), tn.getSubmitStatus() == null ? 0 : tn.getSubmitStatus());
            }
            if (tn.getSortNum() != null) {
                statusBySort.put(tn.getSortNum(), tn.getSubmitStatus() == null ? 0 : tn.getSubmitStatus());
            }
        }
        // 模板节点链
        List<FlowTemplateNode> chain = todo.getTemplateId() == null ? new ArrayList<>()
                : flowTemplateNodeMapper.selectList(new LambdaQueryWrapper<FlowTemplateNode>()
                        .eq(FlowTemplateNode::getTemplateId, todo.getTemplateId())
                        .orderByAsc(FlowTemplateNode::getSortNum));
        // 模板链缺失（模板节点被重建等）时，用我的提交记录按创建顺序兜底
        if (chain.isEmpty()) {
            myNodes.sort(Comparator.comparing(FlowTaskNode::getId));
            for (FlowTaskNode tn : myNodes) {
                TodoNodeVO v = new TodoNodeVO();
                v.setNodeId(tn.getNodeId());
                v.setNodeName(tn.getNodeName());
                v.setNodeType(tn.getNodeType());
                v.setStatus(tn.getSubmitStatus() == null || tn.getSubmitStatus() == 0 ? 2 : 1);
                result.add(v);
            }
            return result;
        }
        for (FlowTemplateNode tpl : chain) {
            TodoNodeVO v = new TodoNodeVO();
            v.setNodeId(tpl.getId());
            v.setNodeName(tpl.getNodeName());
            v.setNodeType(tpl.getNodeType());
            Integer st = statusByNode.get(tpl.getId());
            if (st == null) {
                // node_id 悬空（模板节点链重建），按 sort_num 位置兜底匹配
                st = statusBySort.get(tpl.getSortNum());
            }
            v.setStatus(st == null ? 0 : (st == 1 ? 1 : 2));
            result.add(v);
        }
        return result;
    }

    /** 任务流转进度（全部节点） */
    public List<TaskProgressVO> taskProgress(String taskId) {
        return flowTaskNodeMapper.selectTaskProgress(taskId);
    }

    public boolean update(FlowTask task) {
        return flowTaskMapper.updateById(task) > 0;
    }

    public boolean endTask(String id) {
        FlowTask t = new FlowTask();
        t.setId(id);
        t.setStatus("已结束");
        return flowTaskMapper.updateById(t) > 0;
    }

    public boolean cancelTask(String id) {
        FlowTask t = new FlowTask();
        t.setId(id);
        t.setStatus("已作废");
        return flowTaskMapper.updateById(t) > 0;
    }

    /** 删除任务（成员）级联清理全部提交：任务 + 节点 + 表单记录 + 表单数据 + 附件。主任务保留 */
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        // 1) 表单记录 → 表单数据 / 附件
        LambdaQueryWrapper<FlowFormRecord> rw = new LambdaQueryWrapper<>();
        rw.eq(FlowFormRecord::getTaskId, id);
        List<FlowFormRecord> records = flowFormRecordMapper.selectList(rw);
        if (!records.isEmpty()) {
            List<String> recordIds = records.stream().map(FlowFormRecord::getId).collect(Collectors.toList());
            LambdaQueryWrapper<FlowFormData> dw = new LambdaQueryWrapper<>();
            dw.in(FlowFormData::getRecordId, recordIds);
            flowFormDataMapper.delete(dw);
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
    public boolean deleteDispatch(String dispatchId) {
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

    /**
     * 记录流转通知/催办日志
     * @param logType 1流转通知 2催办
     */
    private void recordFlowLog(FlowTask task, FlowTaskNode taskNode, LoginUser operator, int logType, String content) {
        try {
            FlowTaskLog log = new FlowTaskLog();
            log.setTaskId(task.getId());
            log.setDispatchId(task.getDispatchId());
            log.setTaskNodeId(taskNode != null ? taskNode.getId() : null);
            log.setNodeId(taskNode != null ? taskNode.getNodeId() : null);
            log.setLogType(logType);
            log.setContent(content);
            log.setHandlerUserId(taskNode != null ? taskNode.getHandlerUserId() : null);
            log.setHandlerUserName(taskNode != null ? taskNode.getHandlerUserName() : null);
            if (operator != null) {
                log.setOperatorId(operator.getYyytId());
                log.setOperatorName(operator.getUserName());
            }
            flowTaskLogMapper.insert(log);
        } catch (Exception ignored) {
        }
    }

    /** 催办：向指定成员任务（进行中）发送催办通知并记录日志 */
    @Transactional(rollbackFor = Exception.class)
    public boolean urgeTask(String taskId) {
        FlowTask task = flowTaskMapper.selectById(taskId);
        if (task == null) throw new RuntimeException("任务不存在");
        if (!"进行中".equals(task.getStatus())) throw new RuntimeException("仅进行中的任务可催办");
        LambdaQueryWrapper<FlowTaskNode> pw = new LambdaQueryWrapper<>();
        pw.eq(FlowTaskNode::getTaskId, taskId)
           .eq(FlowTaskNode::getSubmitStatus, 0)
           .orderByAsc(FlowTaskNode::getSortNum).last("LIMIT 1");
        FlowTaskNode pendingNode = flowTaskNodeMapper.selectOne(pw);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        recordFlowLog(task, pendingNode, loginUser, 2,
                "已发送催办通知" + (pendingNode != null ? "（当前节点「" + pendingNode.getNodeName() + "」）" : ""));
        return true;
    }

    /** 批量催办：跳过已完成/已作废等不可催办项，返回实际催办成功数 */
    @Transactional(rollbackFor = Exception.class)
    public int urgeTaskBatch(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) throw new RuntimeException("请选择要催办的人员");
        int count = 0;
        for (String taskId : taskIds) {
            FlowTask task = flowTaskMapper.selectById(taskId);
            if (task == null || !"进行中".equals(task.getStatus())) continue;
            if (urgeTask(taskId)) count++;
        }
        return count;
    }

    /** 批量删除成员任务（级联清理节点/表单/附件），返回实际删除数 */
    @Transactional(rollbackFor = Exception.class)
    public int deleteBatch(List<String> taskIds) {
        if (taskIds == null || taskIds.isEmpty()) throw new RuntimeException("请选择要删除的人员");
        int count = 0;
        for (String taskId : taskIds) {
            if (flowTaskMapper.selectById(taskId) == null) continue;
            if (delete(taskId)) count++;
        }
        return count;
    }

    /** 查询任务的流转/催办日志（按时间正序） */
    public List<FlowTaskLog> getTaskLogs(String taskId) {
        LambdaQueryWrapper<FlowTaskLog> lw = new LambdaQueryWrapper<>();
        lw.eq(FlowTaskLog::getTaskId, taskId).orderByAsc(FlowTaskLog::getId);
        return flowTaskLogMapper.selectList(lw);
    }
}
