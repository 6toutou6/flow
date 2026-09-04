package com.company.flow.sys.base.robot.service.impl;

import com.company.flow.sys.base.robot.service.RobotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 机器人通知实现（预留）：互联网阶段仅落日志占位，真推送由内网框架接管。
 */
@Service
public class RobotServiceImpl implements RobotService {

    private static final Logger log = LoggerFactory.getLogger(RobotServiceImpl.class);

    @Override
    public void sendRobot(List<String> receiverYyytIds, String title, String messageType, String content) {
        log.info("[机器人通知·预留] type={} title={} receivers={} content={}",
                messageType, title, receiverYyytIds == null ? "" : String.join(",", receiverYyytIds),
                content == null ? "" : content);
    }
}
