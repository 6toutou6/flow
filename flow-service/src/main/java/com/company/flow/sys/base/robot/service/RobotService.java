package com.company.flow.sys.base.robot.service;

import java.util.List;

/**
 * 机器人通知服务（预留接口，参考 gzfb base/zh/robot）。
 * 用于企业微信/钉钉/飞书等机器人消息推送；互联网阶段不实现具体推送逻辑（内网框架接管），仅记录日志占位。
 */
public interface RobotService {

    /**
     * 发送机器人通知。
     *
     * @param receiverYyytIds 接收人用户号（aut_user.yyyt_id）列表
     * @param title           通知时显示的标题
     * @param messageType     消息类型（如 催办/提交/退回/模板字段同步）
     * @param content         通知内容
     */
    void sendRobot(List<String> receiverYyytIds, String title, String messageType, String content);
}
