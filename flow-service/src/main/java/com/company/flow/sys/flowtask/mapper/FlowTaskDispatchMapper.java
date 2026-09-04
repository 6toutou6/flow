package com.company.flow.sys.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.flowtask.vo.TaskGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskDispatchMapper extends BaseMapper<FlowTaskDispatch> {

    /** 期次分页：每期一行，含成员聚合计数与聚合状态（status=0 表示空期次） */
    List<TaskGroupVO> selectDispatchPage(@Param("taskId") String taskId,
                                         @Param("taskName") String taskName,
                                         @Param("status") String status,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 期次总数（与 selectDispatchPage 同过滤口径） */
    Long selectDispatchCount(@Param("taskId") String taskId,
                             @Param("taskName") String taskName,
                             @Param("status") String status);
}
