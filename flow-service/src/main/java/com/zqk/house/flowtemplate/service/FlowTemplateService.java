package com.zqk.house.flowtemplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zqk.house.flowtemplate.entity.FlowTemplate;
import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import com.zqk.house.flowtemplate.entity.FlowTemplateNode;
import com.zqk.house.flowtemplate.entity.FlowTemplateQueryForm;
import com.zqk.house.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateMapper;
import com.zqk.house.flowtemplate.mapper.FlowTemplateNodeMapper;
import com.zqk.house.flowtemplate.vo.TemplateDetailVO;
import com.zqk.house.flowtemplate.vo.TemplateFlowSaveDTO;
import com.zqk.house.flowtemplate.vo.TemplateNodeWithFieldsVO;
import com.zqk.house.flowtemplate.vo.TemplateStatsVO;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlowTemplateService {

    @Autowired
    private FlowTemplateMapper flowTemplateMapper;
    @Autowired
    private FlowTemplateFieldMapper flowTemplateFieldMapper;
    @Autowired
    private FlowTemplateNodeMapper flowTemplateNodeMapper;

    public PageResult<FlowTemplate> getPage(FlowTemplateQueryForm form) {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.hasText(form.getTemplateName()), FlowTemplate::getTemplateName, form.getTemplateName())
               .like(StringUtils.hasText(form.getCategory()), FlowTemplate::getCategory, form.getCategory())
               .eq(form.getStatus() != null, FlowTemplate::getStatus, form.getStatus())
               .orderByDesc(FlowTemplate::getUpdateTime);
        Page<FlowTemplate> p = new Page<>(form.getPage() == null ? 1 : form.getPage(), form.getLimit() == null ? 10 : form.getLimit());
        Page<FlowTemplate> result = flowTemplateMapper.selectPage(p, wrapper);
        return new PageResult<>(result.getRecords(), result.getTotal());
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
        Map<Long, List<FlowTemplateField>> fieldMap = allFields.stream()
                .collect(Collectors.groupingBy(f -> f.getNodeId()));
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
        return vo;
    }

    public Long save(FlowTemplate template) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        template.setCreatorId(loginUser == null ? null : loginUser.getId());
        template.setVersion(1);
        if (template.getStatus() == null) template.setStatus(1);
        flowTemplateMapper.insert(template);
        return template.getId();
    }

    public boolean update(FlowTemplate template) {
        return flowTemplateMapper.updateById(template) > 0;
    }

    public boolean toggleStatus(Long id) {
        FlowTemplate t = flowTemplateMapper.selectById(id);
        if (t == null) return false;
        t.setStatus(t.getStatus() == 1 ? 0 : 1);
        return flowTemplateMapper.updateById(t) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        FlowTemplate src = flowTemplateMapper.selectById(id);
        if (src == null) return null;
        src.setId(null);
        src.setTemplateName(src.getTemplateName() + "_副本");
        src.setVersion(1);
        src.setStatus(0);
        flowTemplateMapper.insert(src);
        Long newId = src.getId();
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
     * 逻辑：删旧 nodes+fields → 插新 nodes+fields → version+1
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
        // 删旧 nodes + fields
        LambdaQueryWrapper<FlowTemplateField> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowTemplateField::getTemplateId, templateId);
        flowTemplateFieldMapper.delete(fw);
        LambdaQueryWrapper<FlowTemplateNode> nw = new LambdaQueryWrapper<>();
        nw.eq(FlowTemplateNode::getTemplateId, templateId);
        flowTemplateNodeMapper.delete(nw);
        // 插新 nodes + fields
        for (int i = 0; i < nodes.size(); i++) {
            TemplateFlowSaveDTO.NodeItem item = nodes.get(i);
            FlowTemplateNode node = item.getNode();
            node.setId(null);
            node.setTemplateId(templateId);
            node.setSortNum(i);
            flowTemplateNodeMapper.insert(node);
            Long nodeId = node.getId();
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
        // version+1
        FlowTemplate t = flowTemplateMapper.selectById(templateId);
        if (t != null) {
            t.setVersion(t.getVersion() + 1);
            flowTemplateMapper.updateById(t);
        }
        return true;
    }

    public TemplateStatsVO getStats() {
        TemplateStatsVO vo = new TemplateStatsVO();
        long total = flowTemplateMapper.selectCount(null);
        vo.setTotal(total);
        LambdaQueryWrapper<FlowTemplate> activeWrapper = new LambdaQueryWrapper<>();
        activeWrapper.eq(FlowTemplate::getStatus, 1);
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
        long monthCount = flowTemplateMapper.selectCount(monthWrapper);
        vo.setMonthlyUpdateRate(total == 0 ? 0 : Math.round(monthCount * 1000.0 / total) / 10.0);
        return vo;
    }

    public List<FlowTemplate> getEnabledList() {
        LambdaQueryWrapper<FlowTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FlowTemplate::getStatus, 1).orderByDesc(FlowTemplate::getUpdateTime);
        return flowTemplateMapper.selectList(wrapper);
    }
}
