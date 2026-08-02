package com.zqk.house.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowtask.entity.FlowTaskNode;
import com.zqk.house.flowtask.vo.MyTodoVO;
import com.zqk.house.flowtask.vo.TaskProgressVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskNodeMapper extends BaseMapper<FlowTaskNode> {

    /** 我的待办：当前登录用户作为处理人且未处理的任务节点 */
    List<MyTodoVO> selectMyTodo(@Param("handlerUserId") Long handlerUserId,
                                 @Param("offset") int offset,
                                 @Param("limit") int limit);

    Long selectMyTodoCount(@Param("handlerUserId") Long handlerUserId);

    /** 任务流转进度：该任务全部节点（按 sort_num）+ 处理人姓名 */
    List<TaskProgressVO> selectTaskProgress(@Param("taskId") Long taskId);
}
