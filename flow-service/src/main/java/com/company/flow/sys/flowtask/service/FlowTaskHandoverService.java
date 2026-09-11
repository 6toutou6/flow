package com.company.flow.sys.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.flow.sys.base.autuser.entity.User;
import com.company.flow.sys.base.autuser.mapper.UserMapper;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.deptAdmin.service.DeptAdminService;
import com.company.flow.sys.base.util.SecurityUtils;
import com.company.flow.sys.flowtask.entity.FlowDispatch;
import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.flowtask.entity.FlowTaskHandover;
import com.company.flow.sys.flowtask.entity.FlowTaskMember;
import com.company.flow.sys.flowtask.entity.FlowTaskNode;
import com.company.flow.sys.flowtask.mapper.FlowDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskHandoverMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMemberMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskNodeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 任务交接服务：处理人（A）把某任务配置下「自己名下的全部期次 + 自己在该任务下的全部节点席位」
 * 交接给 B，由创建人同部门的部门管理员任一审批通过后生效。
 * 判定锚点是「我在这条任务里的席位」（不论节点是否已提交、所属任务是否已结束），
 * 而不是「我手上还有没有活」——调岗时需把经办过的痕迹一并转给接手人查看。
 * 生效动作：
 * 1) 期次归属：flow_task.owner_id（A→B），并连带该期次下 A 名下的节点处理人（A→B）；
 * 2) 节点席位：A 在他人负责的任务下的节点（含已提交历史节点），仅换节点处理人（不动期次归属）；
 * 3) 可选同步任务配置名单 flow_task_member（A→B，未来期次下发给 B），仅当 A 在名单中时适用。
 */
@Service
public class FlowTaskHandoverService {

    /** 待审批 */
    private static final int PENDING = 0;
    /** 已通过 */
    private static final int APPROVED = 1;
    /** 已拒绝 */
    private static final int REJECTED = 2;
    /** 任务进行中（flow_task.status 中文语义值） */
    private static final String RUNNING = "进行中";

    @Autowired
    private FlowTaskHandoverMapper handoverMapper;
    @Autowired
    private FlowDispatchMapper flowDispatchMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTaskMemberMapper flowTaskMemberMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DeptAdminService deptAdminService;

    // ==================== 发起方 ====================

    /** 发起交接申请：把该任务配置下我名下的全部期次与全部节点席位交接给 toUserId */
    @Transactional(rollbackFor = Exception.class)
    public FlowTaskHandover apply(FlowTaskHandover req) {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) throw new RuntimeException("未登录");
        if (req.getDispatchId() == null || req.getDispatchId().isEmpty()) {
            throw new RuntimeException("请选择要交接的任务");
        }
        if (req.getToUserId() == null || req.getToUserId().trim().isEmpty()) {
            throw new RuntimeException("请选择接手人");
        }
        String toId = req.getToUserId().trim();
        if (toId.equals(me.getYyytId())) throw new RuntimeException("不能交接给自己");

        FlowDispatch cfg = flowDispatchMapper.selectById(req.getDispatchId());
        if (cfg == null) throw new RuntimeException("任务不存在");
        String toName = userNameOf(toId);
        if (toName == null) throw new RuntimeException("接手人不存在");

        // 范围一：我名下期次（flow_task.owner_id = 我，且属于该任务配置）
        List<String> periodIds = periodIdsOf(cfg.getId());
        List<FlowTask> minePeriods = periodIds.isEmpty() ? new ArrayList<>()
                : flowTaskMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                        .eq(FlowTask::getOwnerId, me.getYyytId())
                        .in(FlowTask::getDispatchId, periodIds));
        // 范围二：我在该任务下的全部节点席位（含已提交的历史节点，中间节点处理人调岗时同样需要交出）
        List<FlowTaskNode> myNodes = nodesOf(allTaskIdsOf(periodIds), me.getYyytId(), null);
        if (minePeriods.isEmpty() && myNodes.isEmpty()) {
            throw new RuntimeException("该任务下没有您名下的期次或参与记录，无需交接");
        }

        // 并发锁：同一任务 + 同一申请人已有待审批申请时禁止再发
        Long pending = handoverMapper.selectCount(new LambdaQueryWrapper<FlowTaskHandover>()
                .eq(FlowTaskHandover::getDispatchId, cfg.getId())
                .eq(FlowTaskHandover::getFromUserId, me.getYyytId())
                .eq(FlowTaskHandover::getStatus, PENDING));
        if (pending != null && pending > 0) {
            throw new RuntimeException("该任务已有您待审批的交接申请，请等待审批结果");
        }

        FlowTaskHandover h = new FlowTaskHandover();
        h.setDispatchId(cfg.getId());
        h.setDispatchName(cfg.getTaskName());
        h.setFromUserId(me.getYyytId());
        h.setFromUserName(me.getUserName());
        h.setToUserId(toId);
        h.setToUserName(toName);
        // 是否同步任务配置名单由审批人（部门管理员）在审批时决定，申请阶段先置 0
        h.setSyncMember(0);
        h.setStatus(PENDING);
        h.setPeriodCount(minePeriods.size());
        h.setNodeCount(myNodes.size());
        h.setRemark(req.getRemark());
        h.setApplyTime(new Date());
        handoverMapper.insert(h);
        return h;
    }

    /** 我发起的交接申请 */
    public List<FlowTaskHandover> listMine() {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) return new ArrayList<>();
        return decorate(handoverMapper.selectList(new LambdaQueryWrapper<FlowTaskHandover>()
                .eq(FlowTaskHandover::getFromUserId, me.getYyytId())
                .orderByDesc(FlowTaskHandover::getApplyTime)), me);
    }

    // ==================== 审批方 ====================

    /** 待我审批：创建人同部门下部门管理员可见的待审批申请 */
    public List<FlowTaskHandover> listPendingForApprover() {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) return new ArrayList<>();
        List<FlowTaskHandover> pending = handoverMapper.selectList(new LambdaQueryWrapper<FlowTaskHandover>()
                .eq(FlowTaskHandover::getStatus, PENDING)
                .orderByDesc(FlowTaskHandover::getApplyTime));
        if (Boolean.TRUE.equals(me.getSuperAdmin())) return decorate(pending, me);
        List<FlowTaskHandover> result = new ArrayList<>();
        for (FlowTaskHandover h : pending) {
            if (isApproverOf(me, h)) result.add(h);
        }
        return decorate(result, me);
    }

    /** 待我审批数量（角标用） */
    public long pendingCountForApprover() {
        return listPendingForApprover().size();
    }

    /** 审批通过：执行交接 */
    @Transactional(rollbackFor = Exception.class)
    public void approve(String id, Integer syncMember) {
        FlowTaskHandover h = handoverMapper.selectById(id);
        if (h == null) throw new RuntimeException("交接申请不存在");
        if (h.getStatus() == null || h.getStatus() != PENDING) throw new RuntimeException("该申请已处理");
        LoginUser me = SecurityUtils.getLoginUser();
        if (!isApproverOf(me, h)) throw new RuntimeException("仅创建人同部门的部门管理员可审批");
        // 是否同步任务配置名单由审批人在此最终确定；交接人不在配置名单中时该项不适用
        boolean applicable = isMember(h.getDispatchId(), h.getFromUserId());
        h.setSyncMember(applicable && syncMember != null && syncMember == 1 ? 1 : 0);

        doHandover(h);

        h.setStatus(APPROVED);
        h.setApproverId(me.getYyytId());
        h.setApproverName(me.getUserName());
        h.setApproveTime(new Date());
        handoverMapper.updateById(h);
    }

    /** 审批拒绝 */
    @Transactional(rollbackFor = Exception.class)
    public void reject(String id, String reason) {
        FlowTaskHandover h = handoverMapper.selectById(id);
        if (h == null) throw new RuntimeException("交接申请不存在");
        if (h.getStatus() == null || h.getStatus() != PENDING) throw new RuntimeException("该申请已处理");
        LoginUser me = SecurityUtils.getLoginUser();
        if (!isApproverOf(me, h)) throw new RuntimeException("仅创建人同部门的部门管理员可审批");
        h.setStatus(REJECTED);
        h.setApproverId(me.getYyytId());
        h.setApproverName(me.getUserName());
        h.setApproveTime(new Date());
        h.setRejectReason(reason);
        handoverMapper.updateById(h);
    }

    // ==================== 记录 ====================

    /**
     * 我相关的交接记录：仅返回登录用户作为「交接人」或「接手人」的记录（只读审计，按申请时间倒序）。
     * 可见性收口——他人之间的交接与本人无关，不下发。
     */
    public List<FlowTaskHandover> listMineRelated() {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) return new ArrayList<>();
        return decorate(handoverMapper.selectList(new LambdaQueryWrapper<FlowTaskHandover>()
                .and(w -> w.eq(FlowTaskHandover::getFromUserId, me.getYyytId())
                        .or().eq(FlowTaskHandover::getToUserId, me.getYyytId()))
                .orderByDesc(FlowTaskHandover::getApplyTime)), me);
    }

    /**
     * 本部门审批范围内的交接记录（部门管理员 / 超管可见，含全部状态）。
     * 范围与「待我审批」一致：该任务配置创建人所在部门的部门管理员，即本部门审批权限覆盖到的记录。
     */
    public List<FlowTaskHandover> listApproverRecords() {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) return new ArrayList<>();
        List<FlowTaskHandover> all = handoverMapper.selectList(new LambdaQueryWrapper<FlowTaskHandover>()
                .orderByDesc(FlowTaskHandover::getApplyTime));
        if (Boolean.TRUE.equals(me.getSuperAdmin())) return decorate(all, me);
        List<FlowTaskHandover> result = new ArrayList<>();
        // 同一任务配置的审批权限相同，按 dispatchId 缓存判定结果，避免逐条回查
        Map<String, Boolean> approvedCache = new HashMap<>();
        for (FlowTaskHandover h : all) {
            Boolean ok = approvedCache.get(h.getDispatchId());
            if (ok == null) {
                ok = isApproverOf(me, h);
                approvedCache.put(h.getDispatchId(), ok);
            }
            if (Boolean.TRUE.equals(ok)) result.add(h);
        }
        return decorate(result, me);
    }

    /** 某任务配置下我相关的交接记录（仅我是交接人或接手人，全部状态，按申请时间倒序） */
    public List<FlowTaskHandover> listByDispatch(String dispatchId) {
        LoginUser me = SecurityUtils.getLoginUser();
        if (me == null) return new ArrayList<>();
        if (dispatchId == null || dispatchId.isEmpty()) return new ArrayList<>();
        return decorate(handoverMapper.selectList(new LambdaQueryWrapper<FlowTaskHandover>()
                .eq(FlowTaskHandover::getDispatchId, dispatchId)
                .and(w -> w.eq(FlowTaskHandover::getFromUserId, me.getYyytId())
                        .or().eq(FlowTaskHandover::getToUserId, me.getYyytId()))
                .orderByDesc(FlowTaskHandover::getApplyTime)), me);
    }

    // ==================== 内部 ====================

    /** 该任务配置下全部任务实例ID（不论进行中 / 已结束） */
    private List<String> allTaskIdsOf(List<String> periodIds) {
        if (periodIds == null || periodIds.isEmpty()) return new ArrayList<>();
        return flowTaskMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                .in(FlowTask::getDispatchId, periodIds))
                .stream().map(FlowTask::getId).collect(Collectors.toList());
    }

    /** 该任务配置下全部期次ID */
    private List<String> periodIdsOf(String cfgId) {
        return flowTaskDispatchMapper.selectList(new LambdaQueryWrapper<FlowTaskDispatch>()
                .eq(FlowTaskDispatch::getTaskId, cfgId)
                .select(FlowTaskDispatch::getId))
                .stream().map(FlowTaskDispatch::getId).collect(Collectors.toList());
    }

    /** 该任务配置下某人名下的节点（submitStatus 为 null 表示不限处理状态，含已提交历史节点） */
    private List<FlowTaskNode> nodesOf(List<String> taskIds, String userId, Integer submitStatus) {
        if (taskIds == null || taskIds.isEmpty()) return new ArrayList<>();
        return flowTaskNodeMapper.selectList(new LambdaQueryWrapper<FlowTaskNode>()
                .in(FlowTaskNode::getTaskId, taskIds)
                .eq(FlowTaskNode::getHandlerUserId, userId)
                .eq(submitStatus != null, FlowTaskNode::getSubmitStatus, submitStatus));
    }

    /** 用户是否在任务配置名单 flow_task_member 中（决定「同步名单」是否适用） */
    private boolean isMember(String cfgId, String userId) {
        if (cfgId == null || userId == null) return false;
        Long count = flowTaskMemberMapper.selectCount(new LambdaQueryWrapper<FlowTaskMember>()
                .eq(FlowTaskMember::getTaskId, cfgId)
                .eq(FlowTaskMember::getUserId, userId));
        return count != null && count > 0;
    }

    /** 列表展示补充：创建部门 + 同步名单适用性 + 当前登录用户在该记录中的角色（均不落库） */
    private List<FlowTaskHandover> decorate(List<FlowTaskHandover> list, LoginUser me) {
        if (list == null || list.isEmpty()) return list;
        Map<String, FlowDispatch> cfgMap = dispatchesOf(list);
        Map<String, String> deptNames = deptNamesOf();
        Map<String, User> creators = creatorsOf(cfgMap);
        for (FlowTaskHandover h : list) {
            FlowDispatch cfg = cfgMap.get(h.getDispatchId());
            if (cfg != null) {
                h.setDeptId(cfg.getDeptId());
                h.setDeptName(deptNameOf(cfg, deptNames, creators));
            }
            h.setSyncMemberApplicable(isMember(h.getDispatchId(), h.getFromUserId()));
            h.setMyRole(roleOf(me, h));
        }
        return list;
    }

    /** 批量取记录涉及的任务配置（按 dispatchId 去重，一次查询） */
    private Map<String, FlowDispatch> dispatchesOf(List<FlowTaskHandover> list) {
        List<String> ids = new ArrayList<>();
        for (FlowTaskHandover h : list) {
            if (h.getDispatchId() != null && !ids.contains(h.getDispatchId())) ids.add(h.getDispatchId());
        }
        Map<String, FlowDispatch> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        for (FlowDispatch d : flowDispatchMapper.selectBatchIds(ids)) {
            map.put(d.getId(), d);
        }
        return map;
    }

    /** 部门ID → 部门名称（dept_admin 已有部门，部门命名以该表为准） */
    private Map<String, String> deptNamesOf() {
        Map<String, String> map = new HashMap<>();
        for (Map<String, String> o : deptAdminService.deptOptions()) {
            map.put(o.get("deptId"), o.get("deptName"));
        }
        return map;
    }

    /** 任务配置创建人（dept_admin 未覆盖该部门时用于兜底取部门名） */
    private Map<String, User> creatorsOf(Map<String, FlowDispatch> cfgMap) {
        List<String> ids = new ArrayList<>();
        for (FlowDispatch d : cfgMap.values()) {
            if (d.getCreatorId() != null && !ids.contains(d.getCreatorId())) ids.add(d.getCreatorId());
        }
        Map<String, User> map = new HashMap<>();
        if (ids.isEmpty()) return map;
        for (User u : userMapper.selectBatchIds(ids)) {
            map.put(u.getYyytId(), u);
        }
        return map;
    }

    /** 创建部门名称：优先 dept_admin 的部门名，其次创建人 aut_user.dept_name */
    private String deptNameOf(FlowDispatch cfg, Map<String, String> deptNames, Map<String, User> creators) {
        if (cfg.getDeptId() != null) {
            String name = deptNames.get(String.valueOf(cfg.getDeptId()));
            if (name != null && !name.isEmpty()) return name;
        }
        User u = cfg.getCreatorId() == null ? null : creators.get(cfg.getCreatorId());
        return u == null ? null : u.getDeptName();
    }

    /** 当前登录用户在该记录中的角色：交接人 / 接手人；审批人（非当事人）返回 null */
    private String roleOf(LoginUser me, FlowTaskHandover h) {
        if (me == null || me.getYyytId() == null) return null;
        if (me.getYyytId().equals(h.getFromUserId())) return "交接人";
        if (me.getYyytId().equals(h.getToUserId())) return "接手人";
        return null;
    }

    /** 是否可审批：超管，或该任务配置创建人所在部门的部门管理员 */
    private boolean isApproverOf(LoginUser me, FlowTaskHandover h) {
        if (me == null) return false;
        if (Boolean.TRUE.equals(me.getSuperAdmin())) return true;
        FlowDispatch cfg = flowDispatchMapper.selectById(h.getDispatchId());
        if (cfg == null) return false;
        Long deptId = cfg.getDeptId();
        return deptId != null && deptAdminService.isDeptAdmin(me.getYyytId(), deptId);
    }

    /** 执行交接：期次归属（整期，含该期下我名下全部节点）+ 我在他人任务下的全部节点席位；可选同步配置名单 */
    private void doHandover(FlowTaskHandover h) {
        String from = h.getFromUserId();
        String to = h.getToUserId();
        String toName = userNameOf(to);
        String fromName = h.getFromUserName();
        List<String> periodIds = periodIdsOf(h.getDispatchId());
        if (periodIds.isEmpty()) return;

        // 1) 期次归属：我名下期次整期交出，owner → B；该期下我名下全部节点（含历史已提交）处理人 → B
        List<FlowTask> minePeriods = flowTaskMapper.selectList(new LambdaQueryWrapper<FlowTask>()
                .eq(FlowTask::getOwnerId, from)
                .in(FlowTask::getDispatchId, periodIds));
        List<String> mineTaskIds = new ArrayList<>();
        for (FlowTask t : minePeriods) {
            t.setOwnerId(to);
            // 当前处理人指针仅在进行中的期次上跟随（已结束任务的指针无意义，不改动）
            if (from.equals(t.getCurrentHandlerId()) && RUNNING.equals(t.getStatus())) {
                t.setCurrentHandlerId(to);
            }
            flowTaskMapper.updateById(t);
            mineTaskIds.add(t.getId());
        }
        for (FlowTaskNode n : nodesOf(mineTaskIds, from, null)) {
            transferNode(n, from, to, toName, fromName);
        }

        // 2) 节点席位：我在他人负责的任务下的全部节点（含已提交历史节点），仅换处理人（不动期次归属）
        List<String> otherTaskIds = allTaskIdsOf(periodIds).stream()
                .filter(id -> !mineTaskIds.contains(id))
                .collect(Collectors.toList());
        Set<String> touchedTaskIds = new LinkedHashSet<>();
        for (FlowTaskNode n : nodesOf(otherTaskIds, from, null)) {
            transferNode(n, from, to, toName, fromName);
            touchedTaskIds.add(n.getTaskId());
        }
        // 节点换人后同步任务当前处理人指针（仅进行中的任务实例，已结束任务的指针无意义）
        for (String taskId : touchedTaskIds) {
            FlowTask t = flowTaskMapper.selectById(taskId);
            if (t != null && from.equals(t.getCurrentHandlerId()) && RUNNING.equals(t.getStatus())) {
                t.setCurrentHandlerId(to);
                flowTaskMapper.updateById(t);
            }
        }

        if (h.getSyncMember() != null && h.getSyncMember() == 1) {
            syncMember(h.getDispatchId(), from, to, toName);
        }
    }

    /** 节点处理人换人（保留原处理人审计信息） */
    private void transferNode(FlowTaskNode n, String from, String to, String toName, String fromName) {
        // 留痕只记「最早那一位」：节点可能被连续交接（A→B→C），
        // 已提交节点的 transferFrom 就是真正经办的人，不能被后来的接手人覆盖
        if (n.getTransferFromUserId() == null || n.getTransferFromUserId().isEmpty()) {
            n.setTransferFromUserId(from);
            n.setTransferFromUserName(fromName);
        }
        n.setHandlerUserId(to);
        n.setHandlerUserName(toName);
        flowTaskNodeMapper.updateById(n);
    }

    /** 同步任务配置名单：A→B（B 已在名单则仅移除 A），未来期次即下发给 B */
    private void syncMember(String cfgId, String from, String to, String toName) {
        List<FlowTaskMember> members = flowTaskMemberMapper.selectList(
                new LambdaQueryWrapper<FlowTaskMember>().eq(FlowTaskMember::getTaskId, cfgId));
        FlowTaskMember fromRow = null;
        boolean toExists = false;
        for (FlowTaskMember m : members) {
            if (from.equals(m.getUserId())) fromRow = m;
            if (to.equals(m.getUserId())) toExists = true;
        }
        if (toExists) {
            if (fromRow != null) flowTaskMemberMapper.deleteById(fromRow.getId());
        } else if (fromRow != null) {
            fromRow.setUserId(to);
            fromRow.setUserName(toName);
            fromRow.setTaskName("下发给" + toName + "的任务");
            flowTaskMemberMapper.updateById(fromRow);
        }
    }

    /** 按用户号查姓名 */
    private String userNameOf(String yyytId) {
        if (yyytId == null) return null;
        User u = userMapper.selectById(yyytId);
        return u == null ? null : u.getUserName();
    }
}
