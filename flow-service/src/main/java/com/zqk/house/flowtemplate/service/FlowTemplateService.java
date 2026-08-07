package com.zqk.house.flowtemplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqk.house.flowtemplate.entity.FlowTemplate;
import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import com.zqk.house.flowtemplate.entity.FlowTemplateNode;
import com.zqk.house.flowtemplate.entity.FlowTemplateQueryForm;
import com.zqk.house.flowtemplate.entity.FlowTemplateVersion;
import com.zqk.house.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateNodeMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateVersionMapper;
import com.zqk.house.flowtemplate.vo.TemplateDetailVO;
import com.zqk.house.flowtemplate.vo.TemplateFlowSaveDTO;
import com.zqk.house.flowtemplate.vo.TemplateNodeWithFieldsVO;
import com.zqk.house.flowtemplate.vo.TemplateStatsVO;
import com.zqk.house.sysuser.entity.SysUser;
import com.zqk.house.sysuser.mapper.SysUserMapper;
import com.zqk.house.sysuser.vo.LoginUser;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
    private ObjectMapper objectMapper;
    @Autowired
    private SysUserMapper sysUserMapper;

    // ==================== 可见性与权限 ====================

    private boolean isSuperAdmin(LoginUser loginUser) {
        return loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
    }

    /** 追加可见性过滤：超管全量；普通用户 is_sample=1（样例公共可见） OR dept_id=当前用户部门 */
    private void applyVisibleFilter(LambdaQueryWrapper<FlowTemplate> wrapper, LoginUser loginUser) {
        if (isSuperAdmin(loginUser)) return;
        Long deptId = loginUser == null ? null : loginUser.getDeptId();
        if (deptId != null) {
            wrapper.and(w -> w.eq(FlowTemplate::getIsSample, 1).or().eq(FlowTemplate::getDeptId, deptId));
        } else {
            wrapper.eq(FlowTemplate::getIsSample, 1);
        }
    }

    /** 操作权限校验：超管全量；样例仅超管可改；普通模板需同部门 */
    private void checkPermission(FlowTemplate t) {
        if (t == null) throw new RuntimeException("模板不存在");
        if (isSuperAdmin(SecurityUtils.getLoginUser())) return;
        if (t.getIsSample() != null && t.getIsSample() == 1) {
            throw new RuntimeException("样例模板仅超管可修改");
        }
        if (t.getDeptId() != null) {
            LoginUser loginUser = SecurityUtils.getLoginUser();
            if (loginUser == null || !Objects.equals(t.getDeptId(), loginUser.getDeptId())) {
                throw new RuntimeException("无权操作其他部门的模板");
            }
        }
    }

    public PageResult<FlowTemplate> getPage(FlowTemplateQueryForm form) {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(form.getTemplateName()), FlowTemplate::getTemplateName, form.getTemplateName())
               .like(StringUtils.hasText(form.getCategory()), FlowTemplate::getCategory, form.getCategory())
               .eq(form.getStatus() != null, FlowTemplate::getStatus, form.getStatus());
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
        List<Long> ids = records.stream().map(FlowTemplate::getCreatorId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (ids.isEmpty()) return;
        Map<Long, SysUser> userMap = sysUserMapper.selectBatchIds(ids).stream()
                .collect(Collectors.toMap(SysUser::getId, u -> u, (a, b) -> a));
        for (FlowTemplate t : records) {
            SysUser u = t.getCreatorId() == null ? null : userMap.get(t.getCreatorId());
            if (u != null) {
                t.setCreatorName(u.getRealName());
                t.setDeptName(u.getDeptName());
            }
        }
    }

    /** 模板详情：模板元数据 + 节点链（每个节点含其字段） */
    public TemplateDetailVO getDetail(Long id) {
        FlowTemplate template = flowTemplateMapper.selectById(id);
        if (template == null) return null;
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, id).orderByAsc(FlowTemplateNode::getSortNum);
        List<FlowTemplateNode> nodeList = flowTemplateNodeMapper.selectList(nw);
        LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowTemplateField::getTemplateId, id).orderByAsc(FlowTemplateField::getNodeId, FlowTemplateField::getSortNum);
        List<FlowTemplateField> allFields = flowTemplateFieldMapper.selectList(fw);
        // 仅按节点分组（模板级字段 node_id 为 null，不参与节点分组，避免 groupingBy 对 null key 抛异常）
        Map<Long, List<FlowTemplateField>> fieldMap = allFields.stream()
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

    public Long save(FlowTemplate template) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        template.setCreatorId(loginUser == null ? null : loginUser.getId());
        template.setDeptId(loginUser == null ? null : loginUser.getDeptId());
        // 样例仅超管通过单独接口设置，普通创建一律为普通模板
        template.setIsSample(0);
        template.setVersion(1);
        if (template.getStatus() == null) template.setStatus(1);
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
        return flowTemplateMapper.updateById(template) > 0;
    }

    public boolean toggleStatus(Long id) {
        FlowTemplate t = flowTemplateMapper.selectById(id);
        checkPermission(t);
        t.setStatus(t.getStatus() == 1 ? 0 : 1);
        return flowTemplateMapper.updateById(t) > 0;
    }

    /** 设置/取消样例：仅超管可操作（样例公共可见、普通用户不可改，但模板可复制） */
    public boolean toggleSample(Long id) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!isSuperAdmin(loginUser)) throw new RuntimeException("仅超管可设置样例");
        FlowTemplate t = flowTemplateMapper.selectById(id);
        if (t == null) return false;
        t.setIsSample(t.getIsSample() == null || t.getIsSample() == 0 ? 1 : 0);
        return flowTemplateMapper.updateById(t) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        FlowTemplate src = flowTemplateMapper.selectById(id);
        if (src == null) return null;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        src.setId(null);
        src.setTemplateName(src.getTemplateName() + "_副本");
        src.setVersion(1);
        src.setStatus(0);
        // 复制得到的模板归属当前用户/部门，样例复制后变为普通模板
        src.setCreatorId(loginUser == null ? null : loginUser.getId());
        src.setDeptId(loginUser == null ? null : loginUser.getDeptId());
        src.setIsSample(0);
        src.setModifierId(null);
        flowTemplateMapper.insert(src);
        Long newId = src.getId();
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
            Long oldNodeId = node.getId();
            node.setId(null);
            node.setTemplateId(newId);
            flowTemplateNodeMapper.insert(node);
            Long newNodeId = node.getId();
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
    public boolean delete(Long id) {
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
     * 流程设计器保存：节点链 + 每个节点的字段
     * 校验：首节点=开始(1)、末节点=结束(3)、结束节点 assignNext=0
     * 保存方式：current=保存到当前版本（覆盖，版本号不变）；new=保存为新版本（版本号+1，旧配置归档到版本记录）
     * 逻辑：删旧 nodes+fields → 插新 nodes+fields → 更新版本号/改动说明/修改人
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFlow(TemplateFlowSaveDTO dto) {
        Long templateId = dto.getTemplateId();
        List<TemplateFlowSaveDTO.NodeItem> nodes = dto.getNodes();
        if (nodes == null || nodes.isEmpty()) {
            throw new RuntimeException("流程节点不能为空");
        }
        FlowTemplateNode first = nodes.get(0).getNode();
        FlowTemplateNode last = nodes.get(nodes.size() - 1).getNode();
        if (first == null || first.getNodeType() == null || first.getNodeType() != 1) {
            throw new RuntimeException("首个节点必须为开始节点");
        }
        if (last == null || last.getNodeType() == null || last.getNodeType() != 3) {
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
                ver.setModifierId(loginUser.getId());
                ver.setModifierName(loginUser.getRealName());
            }
            ver.setConfigSnapshot(buildSnapshot(templateId));
            flowTemplateVersionMapper.insert(ver);
        }
        // 删旧 nodes + fields
        LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowTemplateField::getTemplateId, templateId);
        flowTemplateFieldMapper.delete(fw);
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, templateId);
        flowTemplateNodeMapper.delete(nw);
        // 先插新 nodes + fields（模板级字段的 bindNodeIndex 需要映射到新节点 ID）
        List<Long> newNodeIds = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            TemplateFlowSaveDTO.NodeItem item = nodes.get(i);
            FlowTemplateNode node = item.getNode();
            node.setId(null);
            node.setTemplateId(templateId);
            node.setSortNum(i);
            flowTemplateNodeMapper.insert(node);
            Long nodeId = node.getId();
            newNodeIds.add(nodeId);
            if (item.getFields() != null) {
                for (int j = 0; j < item.getFields().size(); j++) {
                    FlowTemplateField f = item.getFields().get(j);
                    f.setId(null);
                    f.setTemplateId(templateId);
                    f.setNodeId(nodeId);
                    f.setSortNum(j);
                    if (f.getRequired() == null) f.setRequired(0);
                    flowTemplateFieldMapper.insert(f);
                }
            }
        }
        // 插模板级字段（node_id 为空，不依附节点）
        if (dto.getTemplateFields() != null) {
            for (int k = 0; k < dto.getTemplateFields().size(); k++) {
                FlowTemplateField f = dto.getTemplateFields().get(k);
                f.setId(null);
                f.setTemplateId(templateId);
                f.setNodeId(null);
                f.setSortNum(k);
                if (f.getRequired() == null) f.setRequired(0);
                // 填写方式：1=创建人填写（默认） 2=处理人填写
                if (f.getFieldRole() == null) f.setFieldRole(1);
                // 处理人填写字段：bindNodeIndex（nodes 下标）→ 新节点 ID
                if (f.getFieldRole() == 2 && f.getBindNodeIndex() != null
                        && f.getBindNodeIndex() >= 0 && f.getBindNodeIndex() < newNodeIds.size()) {
                    f.setBindNodeId(newNodeIds.get(f.getBindNodeIndex()));
                } else {
                    f.setBindNodeId(null);
                }
                flowTemplateFieldMapper.insert(f);
            }
        }
        // 更新版本信息：新版本+1；当前版本覆盖不动；记录改动说明与修改人
        if (newVersion) {
            t.setVersion(t.getVersion() + 1);
        }
        t.setVersionDesc(dto.getVersionDesc());
        if (loginUser != null) t.setModifierId(loginUser.getId());
        flowTemplateMapper.updateById(t);
        return true;
    }

    /** 构建当前模板配置快照 JSON（nodes + templateFields），供版本记录归档 */
    private String buildSnapshot(Long templateId) {
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
    public List<FlowTemplateVersion> getVersions(Long templateId) {
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
        activeWrapper.eq(FlowTemplate::getStatus, 1);
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
        wrapper.eq(FlowTemplate::getStatus, 1);
        applyVisibleFilter(wrapper, SecurityUtils.getLoginUser());
        wrapper.orderByDesc(FlowTemplate::getUpdateTime);
        return flowTemplateMapper.selectList(wrapper);
    }
}
