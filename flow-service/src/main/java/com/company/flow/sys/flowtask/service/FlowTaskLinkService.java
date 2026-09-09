package com.company.flow.sys.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.flowtask.entity.FlowDispatch;
import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.flowtask.entity.FlowTaskLink;
import com.company.flow.sys.flowtask.entity.FlowTaskNode;
import com.company.flow.sys.flowtask.mapper.FlowDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskLinkMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskNodeMapper;
import com.company.flow.sys.flowtask.vo.TaskLinkVO;
import com.company.flow.sys.flowtask.vo.TodoNodeVO;
import com.company.flow.sys.base.util.SecurityUtils;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateNode;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 任务关联服务（汇总 ← 收集）：
 * 支持配置级（source_dispatch_id）与成员级（source_task_id）两种粒度建关联；
 * 查询时带出目标收集任务信息与收集进度聚合（收集人数 / 已收集份数）。
 */
@Service
public class FlowTaskLinkService {

    /** 成员任务完成状态（完成即视为"已交一份"） */
    private static final String TASK_DONE = "已结束";

    @Autowired
    private FlowTaskLinkMapper flowTaskLinkMapper;
    @Autowired
    private FlowDispatchMapper flowDispatchMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTemplateNodeMapper flowTemplateNodeMapper;

    /** 建立关联（期次级模型）：source = 我创建的某任务的某一期次（source_period_id 必填），target = 我收到的某条成员任务（target_task_id 必填） */
    public TaskLinkVO create(FlowTaskLink link) {
        if (link.getTargetTaskId() == null || link.getTargetTaskId().isEmpty()) {
            throw new RuntimeException("请选择要关联的收到的任务");
        }
        if (link.getSourcePeriodId() == null || link.getSourcePeriodId().isEmpty()) {
            throw new RuntimeException("请选择要关联的期次");
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        // source 校验：期次必须存在且属于「我创建的任务」
        FlowTaskDispatch period = flowTaskDispatchMapper.selectById(link.getSourcePeriodId());
        if (period == null) throw new RuntimeException("要关联的期次不存在");
        FlowDispatch cfg = period.getTaskId() == null ? null
                : flowDispatchMapper.selectById(period.getTaskId());
        if (cfg == null) throw new RuntimeException("期次所属任务不存在");
        boolean owner = loginUser != null
                && (Boolean.TRUE.equals(loginUser.getSuperAdmin()) || loginUser.getYyytId().equals(cfg.getCreatorId()));
        if (!owner) throw new RuntimeException("只能关联您自己创建的任务的期次");
        link.setSourceDispatchId(cfg.getId());
        link.setSourcePeriodId(period.getId());
        link.setSourceTaskId(null); // 收敛：来源一律为期次级，不再用成员任务作来源
        // target 校验：成员任务存在，并冗余补全所属任务配置与期次
        FlowTask tt = flowTaskMapper.selectById(link.getTargetTaskId());
        if (tt == null) throw new RuntimeException("要关联的任务不存在");
        if (link.getTargetDispatchId() == null || link.getTargetDispatchId().isEmpty()) {
            FlowTaskDispatch tp = tt.getDispatchId() == null ? null
                    : flowTaskDispatchMapper.selectById(tt.getDispatchId());
            link.setTargetDispatchId(tp == null ? null : tp.getTaskId());
        }
        if (link.getTargetPeriodId() == null || link.getTargetPeriodId().isEmpty()) {
            link.setTargetPeriodId(tt.getDispatchId());
        }
        if (link.getTargetDispatchId() == null || link.getTargetDispatchId().isEmpty()) {
            throw new RuntimeException("该任务未归属任何任务配置，无法作为关联对象");
        }
        // 幂等：同一来源期次 + 同一目标任务
        LambdaQueryWrapper<FlowTaskLink> dup = new LambdaQueryWrapper<>();
        dup.eq(FlowTaskLink::getSourcePeriodId, link.getSourcePeriodId())
           .eq(FlowTaskLink::getTargetTaskId, link.getTargetTaskId());
        if (flowTaskLinkMapper.selectCount(dup) > 0) {
            throw new RuntimeException("该期次已关联该任务，请勿重复关联");
        }
        link.setId(null);
        link.setLinkType(link.getLinkType() == null || link.getLinkType().isEmpty() ? "collect" : link.getLinkType());
        link.setCreatorId(loginUser == null ? null : loginUser.getYyytId());
        flowTaskLinkMapper.insert(link);
        return toVO(link);
    }

    /** 按来源查询关联。管理端按期次 periodId、或按某任务配置 dispatchId（该配置全部期次的关联）；taskId 为兼容旧数据保留 */
    public List<TaskLinkVO> listBySource(String taskId, String dispatchId, String periodId) {
        LambdaQueryWrapper<FlowTaskLink> w = new LambdaQueryWrapper<>();
        if (periodId != null && !periodId.isEmpty()) {
            w.eq(FlowTaskLink::getSourcePeriodId, periodId);
        } else if (dispatchId != null && !dispatchId.isEmpty()) {
            w.eq(FlowTaskLink::getSourceDispatchId, dispatchId);
        } else if (taskId != null && !taskId.isEmpty()) {
            w.eq(FlowTaskLink::getSourceTaskId, taskId);
        } else {
            return new ArrayList<>();
        }
        w.orderByDesc(FlowTaskLink::getCreateTime);
        return flowTaskLinkMapper.selectList(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 按目标反查：收到任务侧查看「被哪些期次关联」用 targetTaskId；dispatchId/periodId 为兼容旧目标维度保留 */
    public List<TaskLinkVO> listByTarget(String targetTaskId, String targetDispatchId, String targetPeriodId) {
        LambdaQueryWrapper<FlowTaskLink> w = new LambdaQueryWrapper<>();
        if (targetTaskId != null && !targetTaskId.isEmpty()) {
            w.eq(FlowTaskLink::getTargetTaskId, targetTaskId);
        } else if (targetPeriodId != null && !targetPeriodId.isEmpty()) {
            w.eq(FlowTaskLink::getTargetPeriodId, targetPeriodId);
        } else if (targetDispatchId != null && !targetDispatchId.isEmpty()) {
            w.eq(FlowTaskLink::getTargetDispatchId, targetDispatchId);
        } else {
            return new ArrayList<>();
        }
        w.orderByDesc(FlowTaskLink::getCreateTime);
        return flowTaskLinkMapper.selectList(w).stream().map(this::toVO).collect(Collectors.toList());
    }

    /** 解除关联：仅创建人或超管可操作 */
    public void remove(String id) {
        FlowTaskLink link = flowTaskLinkMapper.selectById(id);
        if (link == null) return;
        LoginUser loginUser = SecurityUtils.getLoginUser();
        boolean superAdmin = loginUser != null && Boolean.TRUE.equals(loginUser.getSuperAdmin());
        if (loginUser == null || (!superAdmin && !loginUser.getYyytId().equals(link.getCreatorId()))) {
            throw new RuntimeException("仅关联创建人或超管可解除关联");
        }
        flowTaskLinkMapper.deleteById(id);
    }

    /** 组装展示 VO：目标配置名 + 期次信息（未落期次则取该配置最新一期）+ 收集进度聚合 */
    private TaskLinkVO toVO(FlowTaskLink link) {
        TaskLinkVO vo = new TaskLinkVO();
        vo.setId(link.getId());
        vo.setLinkType(link.getLinkType());
        vo.setRemark(link.getRemark());
        vo.setSourceDispatchId(link.getSourceDispatchId());
        vo.setSourceTaskId(link.getSourceTaskId());
        vo.setSourcePeriodId(link.getSourcePeriodId());
        vo.setTargetDispatchId(link.getTargetDispatchId());
        vo.setCreateTime(link.getCreateTime());

        // 来源侧名称（反链展示用）
        if (link.getSourceDispatchId() != null && !link.getSourceDispatchId().isEmpty()) {
            FlowDispatch sd = flowDispatchMapper.selectById(link.getSourceDispatchId());
            if (sd != null) vo.setSourceDispatchName(sd.getTaskName());
        }
        if (link.getSourceTaskId() != null && !link.getSourceTaskId().isEmpty()) {
            FlowTask st = flowTaskMapper.selectById(link.getSourceTaskId());
            if (st != null) vo.setSourceTaskName(st.getTaskName());
        }
        if (link.getSourcePeriodId() != null && !link.getSourcePeriodId().isEmpty()) {
            FlowTaskDispatch sp = flowTaskDispatchMapper.selectById(link.getSourcePeriodId());
            if (sp != null) vo.setSourcePeriodName(sp.getPeriodName());
        }

        FlowDispatch target = link.getTargetDispatchId() == null || link.getTargetDispatchId().isEmpty()
                ? null : flowDispatchMapper.selectById(link.getTargetDispatchId());
        if (target != null) {
            vo.setTargetDispatchName(target.getTaskName());
        }
        // 目标为收到的成员任务：展示该任务自身状态（单条进度，已完成=1/未完成=0）+ 起止时间 + 流程节点链
        if (link.getTargetTaskId() != null && !link.getTargetTaskId().isEmpty()) {
            FlowTask tt = flowTaskMapper.selectById(link.getTargetTaskId());
            if (tt != null) {
                vo.setTargetTaskId(tt.getId());
                vo.setTargetTaskName(tt.getTaskName());
                FlowTaskDispatch tpd = tt.getDispatchId() == null ? null
                        : flowTaskDispatchMapper.selectById(tt.getDispatchId());
                if (tpd != null) {
                    vo.setTargetPeriodId(tpd.getId());
                    vo.setTargetPeriodName(tpd.getPeriodName());
                }
                vo.setTargetStartTime(tt.getStartTime());
                vo.setTargetEndTime(tt.getEndTime());
                vo.setTargetChain(buildTargetChain(tt));
                vo.setMemberCount(1);
                vo.setDoneCount(TASK_DONE.equals(tt.getStatus()) ? 1 : 0);
            } else {
                vo.setMemberCount(0);
                vo.setDoneCount(0);
            }
            return vo;
        }
        FlowTaskDispatch period = null;
        if (link.getTargetPeriodId() != null && !link.getTargetPeriodId().isEmpty()) {
            period = flowTaskDispatchMapper.selectById(link.getTargetPeriodId());
        }
        if (period == null && target != null) {
            // 配置级关联未落具体期次：取该配置最新一期作为聚合对象
            period = flowTaskDispatchMapper.selectOne(new LambdaQueryWrapper<FlowTaskDispatch>()
                    .eq(FlowTaskDispatch::getTaskId, target.getId())
                    .orderByDesc(FlowTaskDispatch::getId).last("LIMIT 1"));
        }
        if (period != null) {
            vo.setTargetPeriodId(period.getId());
            vo.setTargetPeriodName(period.getPeriodName());
            vo.setTargetEndTime(period.getEndTime());
            vo.setMemberCount(flowTaskMapper.selectCount(new LambdaQueryWrapper<FlowTask>()
                    .eq(FlowTask::getDispatchId, period.getId())).intValue());
            vo.setDoneCount(flowTaskMapper.selectCount(new LambdaQueryWrapper<FlowTask>()
                    .eq(FlowTask::getDispatchId, period.getId())
                    .eq(FlowTask::getStatus, TASK_DONE)).intValue());
        } else {
            vo.setMemberCount(0);
            vo.setDoneCount(0);
        }
        return vo;
    }

    /** 目标任务流程节点链（对齐任务处理列表的展示）：模板链逐节点算状态，模板缺失时按流转记录兜底 */
    private List<TodoNodeVO> buildTargetChain(FlowTask task) {
        List<TodoNodeVO> result = new ArrayList<>();
        if (task == null || task.getId() == null) return result;
        List<FlowTaskNode> taskNodes = flowTaskNodeMapper.selectList(new LambdaQueryWrapper<FlowTaskNode>()
                .eq(FlowTaskNode::getTaskId, task.getId())
                .orderByAsc(FlowTaskNode::getId));
        if (taskNodes.isEmpty()) return result;
        // 当前活动节点位置：最新一条待处理记录所在节点 sortNum（退回重做时进度由此重新开始）
        Integer activeSort = null;
        for (int i = taskNodes.size() - 1; i >= 0; i--) {
            FlowTaskNode tn = taskNodes.get(i);
            if (tn.getSubmitStatus() != null && tn.getSubmitStatus() == 0 && tn.getSortNum() != null) {
                activeSort = tn.getSortNum();
                break;
            }
        }
        List<FlowTemplateNode> chain = task.getTemplateId() == null ? new ArrayList<>()
                : flowTemplateNodeMapper.selectList(new LambdaQueryWrapper<FlowTemplateNode>()
                        .eq(FlowTemplateNode::getTemplateId, task.getTemplateId())
                        .orderByAsc(FlowTemplateNode::getSortNum));
        // 模板链缺失时，用流转记录按创建顺序兜底
        if (chain.isEmpty()) {
            taskNodes.sort(java.util.Comparator.comparing(FlowTaskNode::getId));
            for (FlowTaskNode tn : taskNodes) {
                TodoNodeVO v = new TodoNodeVO();
                v.setNodeId(tn.getNodeId());
                v.setNodeName(tn.getNodeName());
                v.setNodeType(tn.getNodeType());
                v.setStatus(resolveChainStatus(tn.getSubmitStatus(), tn.getSortNum(), activeSort));
                result.add(v);
            }
            return result;
        }
        for (FlowTemplateNode tpl : chain) {
            FlowTaskNode last = null;
            for (FlowTaskNode tn : taskNodes) {
                boolean match = tn.getNodeId() != null && tn.getNodeId().equals(tpl.getId());
                if (!match && tn.getSortNum() != null && tpl.getSortNum() != null
                        && tn.getSortNum().equals(tpl.getSortNum())) {
                    match = true;
                }
                if (match) last = tn;
            }
            TodoNodeVO v = new TodoNodeVO();
            v.setNodeId(tpl.getId());
            v.setNodeName(tpl.getNodeName());
            v.setNodeType(tpl.getNodeType());
            v.setStatus(resolveChainStatus(
                    last == null ? null : last.getSubmitStatus(),
                    last == null ? null : last.getSortNum(),
                    activeSort));
            result.add(v);
        }
        return result;
    }

    /** 节点链状态：0未开始 / 1已完成 / 2进行中（当前活动节点） */
    private int resolveChainStatus(Integer submitStatus, Integer nodeSort, Integer activeSort) {
        if (submitStatus == null) return 0;
        if (submitStatus == 0) return 2;
        if (activeSort == null) return 1;
        if (nodeSort == null || nodeSort < activeSort) return 1;
        return 0;
    }
}
