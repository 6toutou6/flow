package com.zqk.house.flowtask.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 我的任务-期次分组：任务下某期次的待办节点列表
 */
@Data
public class MyTodoPeriodVO {
    private Long dispatchId;
    private String periodName;
    private Integer periodNo;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date startTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date endTime;

    /** 该用户在本期次的待办节点（最新一条/人） */
    private List<MyTodoVO> todos = new ArrayList<>();

    /** 完整流程节点链（模板节点 + 当前用户各节点完成状态），前端展示完成进度 */
    private List<TodoNodeVO> nodes = new ArrayList<>();
}
