package com.company.flow.sys.flowtemplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.flow.sys.flowtask.entity.FlowDispatch;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.flowtask.mapper.FlowDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchMapper;
import com.company.flow.sys.flowtemplate.entity.FlowTemplate;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateField;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateNode;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateQueryForm;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateVersion;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateMapper;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateNodeMapper;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateVersionMapper;
import com.company.flow.sys.flowtemplate.vo.TemplateDetailVO;
import com.company.flow.sys.flowtemplate.vo.TemplateFlowSaveDTO;
import com.company.flow.sys.flowtemplate.vo.TemplateNodeWithFieldsVO;
import com.company.flow.sys.flowtemplate.vo.TemplateStatsVO;
import com.company.flow.sys.base.enums.NodeType;
import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.mapper.UserMapper;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FlowTemplateService {

    @Autowired
    private FlowTemplateMapper flowTemplateMapper;
    @Autowired
    private FlowTemplateFieldMapper flowTemplateFieldMapper;
    @Autowired
    private FlowTemplateNodeMapper flowTemplateNodeMapper;
    @Autowired
    private FlowTemplateVersionMapper flowTemplateVersionMapper;
    @Autowired
    private FlowDispatchMapper flowDispatchMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private com.company.flow.sys.base.deptAdmin.service.DeptAdminService deptAdminService;

    // ==================== 可见性与权限 ====================

    private boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
    }

    /** 追加可见性过滤：超管全量；普通用户 is_sample=1（样例公共可见） OR dept_id=其在 dept_admin 登记的部门 */
    private void applyVisibleFilter(LambdaQueryWrapper<FlowTemplate> wrapper, LoginUser loginUser) {
        if (isSuperAdmin(loginUser)) return;
        Long deptId = loginUser == null ? null : deptAdminService.deptIdOf(loginUser.getYyytId());
        if (deptId != null) {
            // 样例公共可见 或 同部门创建
            wrapper.and(w -> w.eq(FlowTemplate::getIsSample, 1).or().eq(FlowTemplate::getDeptId, deptId));
        } else {
            // 未在 dept_admin 登记部门：仅样例公共可见
            wrapper.eq(FlowTemplate::getIsSample, 1);
        }
    }

    /** 当前登录人在 dept_admin 登记的部门（未登记返回 null） */
    private Long currentDept(LoginUser loginUser) {
        if (loginUser == null || !StringUtils.hasText(loginUser.getYyytId())) return null;
        return deptAdminService.deptIdOf(loginUser.getYyytId());
    }

    /** 操作权限校验：超管全量；样例仅超管可改；普通模板需为 dept_admin 登记的同一部门（未登记部门者不可操作） */
    private void checkPermission(FlowTemplate t) {
        if (t == null) throw new RuntimeException("模板不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (t.getIsSample() != null && t.getIsSample() == 1) {
            throw new RuntimeException("样例模板仅超管可修改");
        }
        Long dept = currentDept(loginUser);
        if (dept == null) {
            throw new RuntimeException("无权操作其他部门的模板");
        }
        if (t.getDeptId() == null || !Objects.equals(t.getDeptId(), dept)) {
            throw new RuntimeException("无权操作其他部门的模板");
        }
    }

    /** 详情可见性校验（只读）：超管全量；样例公共可见；dept_admin 登记的同一部门可见；否则拒绝 */
    private void checkVisible(FlowTemplate t) {
        if (t == null) throw new RuntimeException("模板不存在");
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (isSuperAdmin(loginUser)) return;
        if (t.getIsSample() != null && t.getIsSample() == 1) return;
        Long dept = currentDept(loginUser);
        if (dept == null) {
            throw new RuntimeException("无权查看其他部门的模板");
        }
        if (t.getDeptId() == null || !Objects.equals(t.getDeptId(), dept)) {
            throw new RuntimeException("无权查看其他部门的模板");
        }
    }

    public PageResult<FlowTemplate> getPage(FlowTemplateQueryForm form) {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        // 更新范围先判空取局部值（wrapper 条件值为调用时求值，空值不可直接 .trim()）
        String updStart = StringUtils.hasText(form.getUpdateStart()) ? form.getUpdateStart().trim() : null;
        String updEnd = StringUtils.hasText(form.getUpdateEnd()) ? form.getUpdateEnd().trim() + " 23:59:59" : null;
        wrapper.like(StringUtils.hasText(form.getTemplateName()), FlowTemplate::getTemplateName, form.getTemplateName())
               .like(StringUtils.hasText(form.getCategory()), FlowTemplate::getCategory, form.getCategory())
               .eq(StringUtils.hasText(form.getStatus()), FlowTemplate::getStatus, form.getStatus())
               .eq(form.getIsSample() != null, FlowTemplate::getIsSample, form.getIsSample())
               .ge(updStart != null, FlowTemplate::getUpdateTime, updStart)
               .le(updEnd != null, FlowTemplate::getUpdateTime, updEnd);
        applyVisibleFilter(wrapper, SecurityUtils.getLoginUser());
        wrapper.orderByDesc(FlowTemplate::getUpdateTime);
        Page<FlowTemplate> p = new Page<>(form.getPage() == null ? 1 : form.getPage(), form.getLimit() == null ? 10 : form.getLimit());
        Page<FlowTemplate> result = flowTemplateMapper.selectPage(p, wrapper);
        fillCreatorInfo(result.getRecords());
        return new PageResult<>(result.getRecords(), result.getTotal());
    }

    /** 批量补充创建人姓名/部门（按 creatorId 查 sys_user） */
    private void fillCreatorInfo(List<FlowTemplate> records) {
        if (records == null || records.isEmpty()) return;
        List<String> ids = records.stream().map(FlowTemplate::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<String, User> userMap = userMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(User::getYyytId, u -> u, (a, b) -> a));
        for (FlowTemplate t : records) {
            User u = t.getCreatorId() == null ? null : userMap.get(t.getCreatorId());
            if (u != null) {
                t.setCreatorName(u.getUserName());
                t.setDeptName(u.getDeptName());
            }
        }
    }

    /** 模板详情：模板元数据 + 节点链（每个节点含其字段） */
    public TemplateDetailVO getDetail(String id) {
        FlowTemplate template = flowTemplateMapper.selectById(id);
        if (template == null) return null;
        // 详情可见性校验（超管全量 / 样例公共 / 同部门，否则拒绝）
        checkVisible(template);
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, id).orderByAsc(FlowTemplateNode::getSortNum);
        List<FlowTemplateNode> nodeList = flowTemplateNodeMapper.selectList(nw);
        LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowTemplateField::getTemplateId, id).orderByAsc(FlowTemplateField::getNodeId, FlowTemplateField::getSortNum);
        List<FlowTemplateField> allFields = flowTemplateFieldMapper.selectList(fw);
        // 仅按节点分组（模板级字段 node_id 为 null，不参与节点分组，避免 groupingBy 对 null key 抛异常）
        Map<String, List<FlowTemplateField>> fieldMap = allFields.stream()
                .filter(f -> f.getNodeId() != null)
                .collect(Collectors.groupingBy(FlowTemplateField::getNodeId));
        List<TemplateNodeWithFieldsVO> nodes = new ArrayList<>();
        for (FlowTemplateNode node : nodeList) {
            TemplateNodeWithFieldsVO nv = new TemplateNodeWithFieldsVO();
            nv.setNode(node);
            nv.setFields(fieldMap.getOrDefault(node.getId(), new ArrayList<>()));
            nodes.add(nv);
        }
        TemplateDetailVO vo = new TemplateDetailVO();
        vo.setTemplate(template);
        vo.setNodes(nodes);
        // 模板级字段（node_id 为空，不依附节点，如规章制度/采购说明）
        LambdaQueryWrapper<FlowTemplateField> tfw = new LambdaQueryWrapper<>();
        tfw.eq(FlowTemplateField::getTemplateId, id).isNull(FlowTemplateField::getNodeId)
           .orderByAsc(FlowTemplateField::getSortNum);
        vo.setTemplateFields(flowTemplateFieldMapper.selectList(tfw));
        return vo;
    }

    /** 创建/复制权限校验：须为 dept_admin 登记的部门管理员（aut_user 部门不作依据） */
    private void checkDeptAdmin(LoginUser loginUser) {
        if (loginUser == null) {
            throw new RuntimeException("未登录");
        }
        if (deptAdminService.deptIdOf(loginUser.getYyytId()) == null) {
            throw new RuntimeException("非部门管理员，无创建权限");
        }
    }

    /** 状态归一化：null / "1" / "0"（历史前端传数字）→ 中文语义值 */
    private void normalizeStatus(FlowTemplate template) {
        if (template.getStatus() == null || "1".equals(template.getStatus())) {
            template.setStatus("启用");
        } else if ("0".equals(template.getStatus())) {
            template.setStatus("停用");
        }
    }

    public String save(FlowTemplate template) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        checkDeptAdmin(loginUser);
        template.setCreatorId(loginUser == null ? null : loginUser.getYyytId());
        template.setCreatorName(loginUser == null ? null : loginUser.getUserName());
        // 部门归属以 dept_admin 登记为准（aut_user 为全量用户表、部门可能滞后）
        template.setDeptId(loginUser == null ? null : deptAdminService.deptIdOf(loginUser.getYyytId()));
        // 样例仅超管通过单独接口设置，普通创建一律为普通模板
        template.setIsSample(0);
        template.setVersion(1);
        normalizeStatus(template);
        flowTemplateMapper.insert(template);
        return template.getId();
    }

    public boolean update(FlowTemplate template) {
        if (template.getId() == null) return false;
        FlowTemplate old = flowTemplateMapper.selectById(template.getId());
        checkPermission(old);
        // 非超管不允许通过 update 改动样例标记/部门归属
        if (!isSuperAdmin(SecurityUtils.getLoginUser())) {
            template.setIsSample(null);
            template.setDeptId(null);
        }
        normalizeStatus(template);
        return flowTemplateMapper.updateById(template) > 0;
    }

    public boolean toggleStatus(String id) {
        FlowTemplate t = flowTemplateMapper.selectById(id);
        checkPermission(t);
        t.setStatus("启用".equals(t.getStatus()) ? "停用" : "启用");
        return flowTemplateMapper.updateById(t) > 0;
    }

    /** 设置/取消样例：仅超管可操作（样例公共可见、普通用户不可改，但模板可复制） */
    public boolean toggleSample(String id) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!isSuperAdmin(loginUser)) throw new RuntimeException("仅超管可设置样例");
        FlowTemplate t = flowTemplateMapper.selectById(id);
        if (t == null) return false;
        t.setIsSample(t.getIsSample() == null || t.getIsSample() == 0 ? 1 : 0);
        return flowTemplateMapper.updateById(t) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public String copy(String id) {
        FlowTemplate src = flowTemplateMapper.selectById(id);
        if (src == null) return null;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // 复制产物归属本部门，须为 dept_admin 登记的部门管理员
        checkDeptAdmin(loginUser);
        src.setId(null);
        src.setTemplateName(src.getTemplateName() + "_副本");
        src.setVersion(1);
        src.setStatus("停用");
        // 复制得到的模板归属当前部门管理员，样例复制后变为普通模板
        src.setCreatorId(loginUser == null ? null : loginUser.getYyytId());
        src.setCreatorName(loginUser == null ? null : loginUser.getUserName());
        src.setDeptId(loginUser == null ? null : deptAdminService.deptIdOf(loginUser.getYyytId()));
        src.setIsSample(0);
        src.setModifierId(null);
        flowTemplateMapper.insert(src);
        String newId = src.getId();
        // 复制模板级字段（node_id 为空，不依附节点）
        LambdaQueryWrapper<FlowTemplateField> tfw = new LambdaQueryWrapper<>();
        tfw.eq(FlowTemplateField::getTemplateId, id).isNull(FlowTemplateField::getNodeId)
           .orderByAsc(FlowTemplateField::getSortNum);
        for (FlowTemplateField f : flowTemplateFieldMapper.selectList(tfw)) {
            f.setId(null);
            f.setTemplateId(newId);
            f.setNodeId(null);
            flowTemplateFieldMapper.insert(f);
        }
        // 复制节点链 + 字段
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, id).orderByAsc(FlowTemplateNode::getSortNum);
        List<FlowTemplateNode> nodeList = flowTemplateNodeMapper.selectList(nw);
        for (FlowTemplateNode node : nodeList) {
            String oldNodeId = node.getId();
            node.setId(null);
            node.setTemplateId(newId);
            flowTemplateNodeMapper.insert(node);
            String newNodeId = node.getId();
            LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
            fw.eq(FlowTemplateField::getNodeId, oldNodeId).orderByAsc(FlowTemplateField::getSortNum);
            List<FlowTemplateField> fields = flowTemplateFieldMapper.selectList(fw);
            for (FlowTemplateField f : fields) {
                f.setId(null);
                f.setTemplateId(newId);
                f.setNodeId(newNodeId);
                flowTemplateFieldMapper.insert(f);
            }
        }
        return newId;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean delete(String id) {
        checkPermission(flowTemplateMapper.selectById(id));
        LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowTemplateField::getTemplateId, id);
        flowTemplateFieldMapper.delete(fw);
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, id);
        flowTemplateNodeMapper.delete(nw);
        return flowTemplateMapper.deleteById(id) > 0;
    }

    /**
     * 模板被使用情况：任务数（flow_dispatch）+ 期次数（flow_task_dispatch），附任务/期次名称列表。
     * 保存流程设计前调用，用于提示用户：修改仅对新下发的期次生效，已下发期次持有独立配置快照不受影响。
     */
    public Map<String, Object> usageCount(String templateId) {
        Map<String, Object> usage = new HashMap<>();
        List<FlowDispatch> tasks = flowDispatchMapper.selectList(
                new LambdaQueryWrapper<FlowDispatch>().eq(FlowDispatch::getTemplateId, templateId));
        List<FlowTaskDispatch> dispatches = flowTaskDispatchMapper.selectList(
                new LambdaQueryWrapper<FlowTaskDispatch>().eq(FlowTaskDispatch::getTemplateId, templateId));
        usage.put("taskCount", tasks.size());
        usage.put("dispatchCount", dispatches.size());
        usage.put("taskNames", tasks.stream().map(FlowDispatch::getTaskName).filter(StringUtils::hasText).collect(Collectors.toList()));
        usage.put("dispatchNames", dispatches.stream()
                .map(d -> StringUtils.hasText(d.getPeriodName()) ? d.getPeriodName() : d.getTaskName())
                .filter(StringUtils::hasText).collect(Collectors.toList()));
        return usage;
    }

    /**
     * 流程设计器保存：节点链 + 每个节点的字段
     * 校验：首节点=开始(1)、末节点=结束(3)、结束节点 assignNext=0
     * 保存方式：current=保存到当前版本（覆盖，版本号不变）；new=保存为新版本（版本号+1，旧配置归档到版本记录）
     * 逻辑：删旧 nodes+fields → 插新 nodes+fields → 更新版本号/改动说明/修改人
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFlow(TemplateFlowSaveDTO dto) {
        String templateId = dto.getTemplateId();
        List<TemplateFlowSaveDTO.NodeItem> nodes = dto.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            throw new RuntimeException("流程节点不能为空");
        }
        FlowTemplateNode first = nodes.get(0).getNode();
        FlowTemplateNode last = nodes.get(nodes.size() - 1).getNode();
        if (first == null || first.getNodeType() == null || first.getNodeType() != NodeType.START.getCode()) {
            throw new RuntimeException("首个节点必须为开始节点");
        }
        if (last == null || last.getNodeType() == null || last.getNodeType() != NodeType.END.getCode()) {
            throw new RuntimeException("末个节点必须为结束节点");
        }
        FlowTemplate t = flowTemplateMapper.selectById(templateId);
        if (t == null) throw new RuntimeException("模板不存在");
        checkPermission(t);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        boolean newVersion = "new".equalsIgnoreCase(dto.getSaveMode());
        // 保存为新版本：先把旧配置归档为「上一版本」的版本记录
        if (newVersion) {
            FlowTemplateVersion ver = new FlowTemplateVersion();
            ver.setTemplateId(templateId);
            ver.setVersion(t.getVersion());
            ver.setVersionDesc(t.getVersionDesc());
            if (loginUser != null) {
                ver.setModifierId(loginUser.getYyytId());
                ver.setModifierName(loginUser.getUserName());
            }
            ver.setConfigSnapshot(buildSnapshot(templateId));
            flowTemplateVersionMapper.insert(ver);
        }
        // B 方案：增量保存——节点按模板内顺序对齐保留 ID，字段按 field_key 匹配保留 ID，
        // 只增删改真正有变化的部分；模板级（node_id 空）创建人字段 ID 保持稳定，
        // 避免绑定任务的 template_data（以字段 ID 为钥匙）因模板微调而整体失配
        incrementalSaveFlow(templateId, nodes, dto.getTemplateFields());
        // 更新版本信息：新版本+1；当前版本覆盖不动；记录改动说明与修改人
        if (newVersion) {
            t.setVersion(t.getVersion() + 1);
        }
        t.setVersionDesc(dto.getVersionDesc());
        if (loginUser != null) t.setModifierId(loginUser.getYyytId());
        flowTemplateMapper.updateById(t);
        return true;
    }

    /**
     * 模板流程/字段增量保存（B 方案核心）：
     * 1) 节点：与旧节点按 sort_num 顺序一一对齐 → 保留原 ID 更新；新节点插入、多余旧节点删除；
     * 2) 字段：与旧字段按 field_key 全局匹配（模板级字段 node_id 为空优先全局；缺失 key 时按归属+label 兜底）→ 保留原 ID 更新；
     *    key 未命中的新增字段插入、旧字段未再提交的删除。保证未变化的字段 ID 不动。
     */
    private void incrementalSaveFlow(String templateId, List<TemplateFlowSaveDTO.NodeItem> nodes,
                                     List<FlowTemplateField> tplFields) {
        // 1) 旧节点（按 sort_num 升序）
        List<FlowTemplateNode> oldNodes = flowTemplateNodeMapper.selectList(
                new LambdaQueryWrapper<FlowTemplateNode>().eq(FlowTemplateNode::getTemplateId, templateId)
                        .orderByAsc(FlowTemplateNode::getSortNum));
        List<String> newNodeIds = new ArrayList<>();
        // 2) 提交节点与旧节点顺序对齐：保留原 ID 增量更新；数量多则补插
        for (int i = 0; i < nodes.size(); i++) {
            FlowTemplateNode nb = nodes.get(i).getNode();
            nb.setTemplateId(templateId);
            nb.setSortNum(i);
            if (i < oldNodes.size()) {
                FlowTemplateNode old = oldNodes.get(i);
                nb.setId(old.getId());
                nb.setCreateTime(old.getCreateTime());
                flowTemplateNodeMapper.updateById(nb);
            } else {
                nb.setId(null);
                flowTemplateNodeMapper.insert(nb);
            }
            newNodeIds.add(nb.getId());
        }
        // 3) 提交中不再存在的旧节点删除（其字段在第 6 步统一清理）
        for (int i = nodes.size(); i < oldNodes.size(); i++) {
            flowTemplateNodeMapper.deleteById(oldNodes.get(i).getId());
        }
        // 4) 旧字段全集（节点内 + 模板级），按 field_key 建索引
        List<FlowTemplateField> oldFields = flowTemplateFieldMapper.selectList(
                new LambdaQueryWrapper<FlowTemplateField>().eq(FlowTemplateField::getTemplateId, templateId));
        Map<String, FlowTemplateField> byKey = new HashMap<>();
        for (FlowTemplateField f : oldFields) {
            if (f.getFieldKey() != null && StringUtils.hasText(f.getFieldKey()) && !byKey.containsKey(f.getFieldKey())) {
                byKey.put(f.getFieldKey(), f);
            } else if (!StringUtils.hasText(f.getFieldKey()) && f.getNodeId() == null) {
                // 模板级字段且无 key：以 label 作兜底键
                String lk = "__tpl__label__" + f.getFieldLabel();
                if (!byKey.containsKey(lk)) byKey.put(lk, f);
            }
        }
        Set<String> occupied = new HashSet<>();
        // 5) 逐条 upsert：先节点内字段（owner 为新节点 id，保证节点已存在），后模板级字段（owner 为 null）
        for (int i = 0; i < nodes.size(); i++) {
            String nodeId = newNodeIds.get(i);
            List<FlowTemplateField> fs = nodes.get(i).getFields();
            if (fs == null) continue;
            for (int j = 0; j < fs.size(); j++) {
                FlowTemplateField f = fs.get(j);
                f.setTemplateId(templateId);
                f.setNodeId(nodeId);
                f.setBindNodeId(null);
                f.setSortNum(j);
                if (f.getRequired() == null) f.setRequired(0);
                if (f.getFieldRole() == null) f.setFieldRole(1);
                upsertField(f, nodeId, byKey, occupied);
            }
        }
        // 模板级字段（node_id 为空；fieldRole=2 时 bindNodeIndex 映射新节点 id）
        if (tplFields != null) {
            for (int k = 0; k < tplFields.size(); k++) {
                FlowTemplateField f = tplFields.get(k);
                f.setTemplateId(templateId);
                f.setNodeId(null);
                f.setSortNum(k);
                if (f.getRequired() == null) f.setRequired(0);
                if (f.getFieldRole() == null) f.setFieldRole(1);
                if (f.getFieldRole() == 2 && f.getBindNodeIndex() != null
                        && f.getBindNodeIndex() >= 0 && f.getBindNodeIndex() < newNodeIds.size()) {
                    f.setBindNodeId(newNodeIds.get(f.getBindNodeIndex()));
                } else {
                    f.setBindNodeId(null);
                }
                upsertField(f, null, byKey, occupied);
            }
        }
        // 6) 删除旧字段中未被占用的（模板中已移除的字段）
        for (FlowTemplateField f : oldFields) {
            if (f.getId() != null && !occupied.contains(f.getId())) {
                flowTemplateFieldMapper.deleteById(f.getId());
            }
        }
    }

    /**
     * 字段增量 upsert：按 field_key（归属为空时优先全局 key 匹配）命中旧字段 → 保留原 ID 更新；否则插入。
     */
    private void upsertField(FlowTemplateField f, String ownerNodeId, Map<String, FlowTemplateField> byKey,
                             Set<String> occupied) {
        String key = f.getFieldKey();
        FlowTemplateField old = (key != null) ? byKey.remove(key) : null;
        if (old == null) {
            // key 缺失或未命中：模板级按 label 兜底匹配旧模板级字段（label 与归属均一致才视为同一字段）
            if (ownerNodeId == null && key == null) {
                old = byKey.get("__tpl__label__" + f.getFieldLabel());
                if (old != null) byKey.remove("__tpl__label__" + f.getFieldLabel());
            }
        }
        if (old != null) {
            f.setId(old.getId());
            f.setCreateTime(old.getCreateTime());
            boolean clearNode = f.getNodeId() == null && old.getNodeId() != null;
            flowTemplateFieldMapper.updateById(f);
            // MP updateById 忽略 null 字段：node_id、visible_when/editable_when 可能在本次被清空，需用 wrapper 强制同步
            LambdaUpdateWrapper<FlowTemplateField> force = new LambdaUpdateWrapper<FlowTemplateField>()
                    .eq(FlowTemplateField::getId, f.getId())
                    .set(FlowTemplateField::getVisibleWhen, f.getVisibleWhen())
                    .set(FlowTemplateField::getEditableWhen, f.getEditableWhen());
            if (clearNode) {
                force.set(FlowTemplateField::getNodeId, null);
            }
            flowTemplateFieldMapper.update(null, force);
            occupied.add(f.getId());
        } else {
            f.setId(null);
            flowTemplateFieldMapper.insert(f);
            occupied.add(f.getId());
        }
    }

    /**
     * 单独保存某节点的说明文件（上传/删除后即时持久化，避免刷新丢失）
     * 只更新 guide_files 字段，不触发节点链重建（避免任务节点悬空）
     */
    public void saveNodeGuideFiles(String nodeId, String guideFiles) {
        if (nodeId == null) throw new RuntimeException("节点ID不能为空");
        FlowTemplateNode node = flowTemplateNodeMapper.selectById(nodeId);
        if (node == null) throw new RuntimeException("节点不存在");
        FlowTemplate t = flowTemplateMapper.selectById(node.getTemplateId());
        checkPermission(t);
        LambdaUpdateWrapper<FlowTemplateNode> uw = new LambdaUpdateWrapper<>();
        uw.eq(FlowTemplateNode::getId, nodeId)
          .set(FlowTemplateNode::getGuideFiles, StringUtils.hasText(guideFiles) ? guideFiles : null);
        flowTemplateNodeMapper.update(null, uw);
    }

    /** 构建当前模板配置快照 JSON（nodes + templateFields），供版本记录归档 */
    private String buildSnapshot(String templateId) {
        try {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
            nw.eq(FlowTemplateNode::getTemplateId, templateId).orderByAsc(FlowTemplateNode::getSortNum);
            List<Map<String, Object>> nodes = new ArrayList<>();
            for (FlowTemplateNode node : flowTemplateNodeMapper.selectList(nw)) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("node", node);
                LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
                fw.eq(FlowTemplateField::getNodeId, node.getId()).orderByAsc(FlowTemplateField::getSortNum);
                item.put("fields", flowTemplateFieldMapper.selectList(fw));
                nodes.add(item);
            }
            snapshot.put("nodes", nodes);
            LambdaQueryWrapper<FlowTemplateField> tfw = new LambdaQueryWrapper<>();
            tfw.eq(FlowTemplateField::getTemplateId, templateId).isNull(FlowTemplateField::getNodeId)
               .orderByAsc(FlowTemplateField::getSortNum);
            snapshot.put("templateFields", flowTemplateFieldMapper.selectList(tfw));
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            return null;
        }
    }

    /** 模板版本记录列表（按版本号倒序） */
    public List<FlowTemplateVersion> getVersions(String templateId) {
        LambdaQueryWrapper<FlowTemplateVersion> vw = new LambdaQueryWrapper<>();
        vw.eq(FlowTemplateVersion::getTemplateId, templateId).orderByDesc(FlowTemplateVersion::getVersion);
        return flowTemplateVersionMapper.selectList(vw);
    }

    public TemplateStatsVO getStats() {
        TemplateStatsVO vo = new TemplateStatsVO();
        LoginUser loginUser = SecurityUtils.getLoginUser();
        LambdaQueryWrapper<FlowTemplate> totalWrapper = new LambdaQueryWrapper<>();
        applyVisibleFilter(totalWrapper, loginUser);
        long total = flowTemplateMapper.selectCount(totalWrapper);
        vo.setTotal(total);
        LambdaQueryWrapper<FlowTemplate> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(FlowTemplate::getStatus, "启用");
        applyVisibleFilter(activeWrapper, loginUser);
        vo.setActive(flowTemplateMapper.selectCount(activeWrapper));
        // 本月更新率
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        Date monthStart = cal.getTime();
        LambdaQueryWrapper<FlowTemplate> monthWrapper = new LambdaQueryWrapper<>();
        monthWrapper.ge(FlowTemplate::getUpdateTime, monthStart);
        applyVisibleFilter(monthWrapper, loginUser);
        long monthCount = flowTemplateMapper.selectCount(monthWrapper);
        vo.setMonthlyUpdateRate(total == 0 ? 0 : Math.round(monthCount * 1000.0 / total) / 10.0);
        return vo;
    }

    public List<FlowTemplate> getEnabledList() {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplate::getStatus, "启用");
        applyVisibleFilter(wrapper, SecurityUtils.getLoginUser());
        wrapper.orderByDesc(FlowTemplate::getUpdateTime);
        return flowTemplateMapper.selectList(wrapper);
    }
}
