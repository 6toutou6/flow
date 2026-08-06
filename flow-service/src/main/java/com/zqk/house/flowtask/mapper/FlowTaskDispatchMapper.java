package com.zqk.house.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowtask.entity.FlowTaskDispatch;
import com.zqk.house.flowtask.vo.TaskGroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskDispatchMapper extends BaseMapper<FlowTaskDispatch> {

    /** 期次分页：每期一行，含成员聚合计数与聚合状态（status=0 表示空期次） */
    List<TaskGroupVO> selectDispatchPage(@Param("taskId") Long taskId,
                                         @Param("taskName") String taskName,
                                         @Param("status") Integer status,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 期次总数（与 selectDispatchPage 同过滤口径） */
    Long selectDispatchCount(@Param("taskId") Long taskId,
                             @Param("taskName") String taskName,
                             @Param("status") Integer status);
}
