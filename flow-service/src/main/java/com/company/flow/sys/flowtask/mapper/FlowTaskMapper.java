package com.company.flow.sys.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.vo.TaskMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskMapper extends BaseMapper<FlowTask> {

    /** 某任务组全部成员（起始节点处理人 = 被下发者；供组详情统计用） */
    List<TaskMemberVO> selectMembersByDispatch(@Param("dispatchId") String dispatchId);

    /** 某任务组的成员分页查询（起始节点处理人 = 被下发者；支持姓名/部门/状态过滤） */
    List<TaskMemberVO> selectMembersPage(@Param("dispatchId") String dispatchId,
                                         @Param("name") String name,
                                         @Param("dept") String dept,
                                         @Param("status") String status,
                                         @Param("offset") int offset,
                                         @Param("limit") int limit);

    /** 某任务组成员总数（与 selectMembersPage 同过滤口径） */
    Long selectMembersCount(@Param("dispatchId") String dispatchId,
                            @Param("name") String name,
                            @Param("dept") String dept,
                            @Param("status") String status);
}
