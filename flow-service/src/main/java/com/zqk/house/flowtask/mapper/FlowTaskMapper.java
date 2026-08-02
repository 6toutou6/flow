package com.zqk.house.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.vo.TaskGroupVO;
import com.zqk.house.flowtask.vo.TaskMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskMapper extends BaseMapper<FlowTask> {

    /** 任务组分页：每组一行，含聚合计数与聚合状态 */
    List<TaskGroupVO> selectDispatchPage(@Param("taskName") String taskName,
                                         @Param("status") Integer status,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 任务组总数（与 selectDispatchPage 同过滤口径） */
    Long selectDispatchCount(@Param("taskName") String taskName,
                             @Param("status") Integer status);

    /** 本页各组的代表行（组内 min-id 任务），用于填充组头字段 */
    List<FlowTask> selectPrimaryByDispatchIds(@Param("dispatchIds") List<Long> dispatchIds);

    /** 某组的全部成员（起始节点处理人 = 被下发者） */
    List<TaskMemberVO> selectMembersByDispatch(@Param("dispatchId") Long dispatchId);
}
