package com.zqk.house.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowtask.entity.FlowTaskDispatch;
import com.zqk.house.flowtask.vo.TaskGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskDispatchMapper extends BaseMapper<FlowTaskDispatch> {

    /** 主任务分页：每组一行，含成员聚合计数与聚合状态（status=0 表示空组） */
    List<TaskGroupVO> selectDispatchPage(@Param("taskName") String taskName,
                                         @Param("status") Integer status,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 主任务总数（与 selectDispatchPage 同过滤口径） */
    Long selectDispatchCount(@Param("taskName") String taskName,
                             @Param("status") Integer status);
}
