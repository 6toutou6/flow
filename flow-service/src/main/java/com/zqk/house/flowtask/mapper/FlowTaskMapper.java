package com.zqk.house.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.vo.TaskMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskMapper extends BaseMapper<FlowTask> {

    /** 某任务组全部成员（起始节点处理人 = 被下发者；供组详情统计用） */
    List<TaskMemberVO> selectMembersByDispatch(@Param("dispatchId") Long dispatchId);

    /** 某任务组的成员分页查询（起始节点处理人 = 被下发者；支持姓名/部门/状态过滤） */
    List<TaskMemberVO> selectMembersPage(@Param("dispatchId") Long dispatchId,
                                         @Param("name") String name,
                                         @Param("dept") String dept,
                                         @Param("status") Integer status,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 某任务组成员总数（与 selectMembersPage 同过滤口径） */
    Long selectMembersCount(@Param("dispatchId") Long dispatchId,
                            @Param("name") String name,
                            @Param("dept") String dept,
                            @Param("status") Integer status);
}
