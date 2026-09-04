package com.company.flow.sys.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.flow.sys.flowtask.entity.FlowDispatch;
import com.company.flow.sys.flowtask.vo.DispatchStatsVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowDispatchMapper extends BaseMapper<FlowDispatch> {

    /** 任务分页：任务 + 期次数 + 人员数（deptId 传 null 时不加部门过滤，超管全量） */
    List<FlowDispatch> selectTaskPage(@Param("taskName") String taskName,
                                      @Param("status") String status,
                                      @Param("deptId") String deptId,
                                      @Param("offset") int offset,
                                      @Param("limit") int limit);

    /** 任务总数（与 selectTaskPage 同过滤口径） */
    Long selectTaskCount(@Param("taskName") String taskName,
                         @Param("status") String status,
                         @Param("deptId") String deptId);

    /** 任务管理页统计卡（与 selectTaskPage 同过滤口径） */
    DispatchStatsVO selectStats(@Param("deptId") String deptId);

    /** 任务人员列表（JOIN sys_user） */
    List<com.company.flow.sys.flowtask.vo.TaskMemberInfoVO> selectMembersByTask(@Param("taskId") String taskId);
}
