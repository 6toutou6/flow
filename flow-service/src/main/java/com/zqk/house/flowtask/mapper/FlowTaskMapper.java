package com.zqk.house.flowtask.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.vo.TaskMemberVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowTaskMapper extends BaseMapper<FlowTask> {

    /** 某任务组的全部成员（起始节点处理人 = 被下发者） */
    List<TaskMemberVO> selectMembersByDispatch(@Param("dispatchId") Long dispatchId);
}
