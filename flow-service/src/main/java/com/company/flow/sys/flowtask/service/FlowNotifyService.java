package com.company.flow.sys.flowtask.service;

import com.company.flow.sys.base.robot.service.RobotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 流程消息通知统一出口（预留）。
 * 转发到 RobotService.sendRobot 占位实现（互联网阶段仅落日志，真推送由内网框架接管）；
 * 各业务点只需在此声明语义化方法、传接收人，通知失败不影响主流程。
 */
@Service
public class FlowNotifyService {

    private static final Logger log = LoggerFactory.getLogger(FlowNotifyService.class);

    @Autowired
    private RobotService robotService;

    /** 安全发送：过滤空接收人并捕获异常，通知失败绝不影响业务流程 */
    private void safeSend(List<String> receiverIds, String title, String messageType, String content) {
        List<String> ids = receiverIds == null ? new ArrayList<>() : receiverIds.stream()
                .filter(s -> s != null && !s.trim().isEmpty())
                .distinct()
                .collect(Collectors.toList());
        if (ids.isEmpty()) return;
        try {
            robotService.sendRobot(ids, title, messageType, content);
        } catch (Exception e) {
            log.warn("[flow-notify] 通知发送失败（已忽略）：type={} err={}", messageType, e.getMessage());
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    /** 期次/任务下发通知各处理人 */
    public void notifyDispatch(String taskName, String periodName, List<String> receiverIds) {
        safeSend(receiverIds, "新任务下发 · 请处理", "任务下发",
                "您收到任务「" + safe(taskName) + "」" + (periodName == null || periodName.trim().isEmpty()
                        ? "" : "（期次：" + periodName + "）") + "，请登录查看并尽快处理。");
    }

    /** 流转至下一节点，通知该节点各处理人 */
    public void notifyFlowNext(String taskName, String nextNodeName, List<String> receiverIds) {
        safeSend(receiverIds, "任务流转 · 待您处理", "流转通知",
                "任务「" + safe(taskName) + "」已流转至「" + safe(nextNodeName) + "」，请您处理。");
    }

    /** 退回到目标节点，通知需重新处理的人员（接收人通常不含发起退回者本人） */
    public void notifyRejectRedo(String taskName, String targetNodeName, String reason, List<String> receiverIds) {
        safeSend(receiverIds, "任务退回 · 请重新处理", "退回重做",
                "任务「" + safe(taskName) + "」被退回至「" + safe(targetNodeName) + "」，请您重新处理"
                        + (reason == null || reason.trim().isEmpty() ? "。" : "。原因：" + reason));
    }

    /** 催办通知 */
    public void notifyUrge(String taskName, String nodeName, List<String> receiverIds) {
        safeSend(receiverIds, "任务催办 · 请尽快处理", "催办",
                "任务「" + safe(taskName) + "」当前节点「" + safe(nodeName) + "」等待处理，请尽快处理。");
    }
}
