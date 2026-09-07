package com.company.flow.sys.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.flow.sys.flowtask.entity.FlowTaskNode;
import com.company.flow.sys.flowtask.vo.MyTodoStatsVO;
import com.company.flow.sys.flowtask.vo.MyTodoTaskVO;
import com.company.flow.sys.flowtask.vo.MyTodoVO;
import com.company.flow.sys.flowtask.vo.TaskProgressVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskNodeMapper extends BaseMapper<FlowTaskNode> {

    /** 我的待办：当前登录用户作为处理人且未处理的任务节点 */
    List<MyTodoVO> selectMyTodo(@Param("handlerUserId") String handlerUserId,
                                @Param("offset") int offset,
                                @Param("limit") int limit);

    Long selectMyTodoCount(@Param("handlerUserId") String handlerUserId);

    /** 我的任务（任务级分组分页）：按任务分组，含涉及期次数、待处理数；taskName 模糊过滤，status=1 仅待处理、0 仅已处理；taskType 1样例/0普通；templateName 模板名模糊；createStart/End 创建时间范围 */
    List<MyTodoTaskVO> selectMyTaskGroups(@Param("handlerUserId") String handlerUserId,
                                          @Param("taskName") String taskName,
                                          @Param("status") Integer status,
                                          @Param("taskType") Integer taskType,
                                          @Param("templateName") String templateName,
                                          @Param("createStart") String createStart,
                                          @Param("createEnd") String createEnd,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    Long selectMyTaskGroupCount(@Param("handlerUserId") String handlerUserId,
                                @Param("taskName") String taskName,
                                @Param("status") Integer status,
                                @Param("taskType") Integer taskType,
                                @Param("templateName") String templateName,
                                @Param("createStart") String createStart,
                                @Param("createEnd") String createEnd);

    /** 我的任务统计（统计卡） */
    MyTodoStatsVO selectMyTodoStats(@Param("handlerUserId") String handlerUserId);

    /** 我的任务明细：指定任务集内该用户各任务最新一条节点（含期次信息） */
    List<MyTodoVO> selectMyTodoByTasks(@Param("handlerUserId") String handlerUserId,
                                       @Param("taskIds") List<String> taskIds);

    /** 任务流转进度：该任务全部节点（按 sort_num）+ 处理人姓名 */
    List<TaskProgressVO> selectTaskProgress(@Param("taskId") String taskId);
}
