package com.zqk.house.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowtask.entity.FlowTaskNode;
import com.zqk.house.flowtask.vo.MyTodoStatsVO;
import com.zqk.house.flowtask.vo.MyTodoTaskVO;
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

    /** 我的任务（任务级分组分页）：按任务分组，含涉及期次数、待处理数；taskName 模糊过滤，status=1 仅待处理、0 仅已处理 */
    List<MyTodoTaskVO> selectMyTaskGroups(@Param("handlerUserId") Long handlerUserId,
                                          @Param("taskName") String taskName,
                                          @Param("status") Integer status,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    Long selectMyTaskGroupCount(@Param("handlerUserId") Long handlerUserId,
                                @Param("taskName") String taskName,
                                @Param("status") Integer status);

    /** 我的任务统计（统计卡） */
    MyTodoStatsVO selectMyTodoStats(@Param("handlerUserId") Long handlerUserId);

    /** 我的任务明细：指定任务集内该用户各任务最新一条节点（含期次信息） */
    List<MyTodoVO> selectMyTodoByTasks(@Param("handlerUserId") Long handlerUserId,
                                       @Param("taskIds") List<Long> taskIds);

    /** 任务流转进度：该任务全部节点（按 sort_num）+ 处理人姓名 */
    List<TaskProgressVO> selectTaskProgress(@Param("taskId") Long taskId);
}
