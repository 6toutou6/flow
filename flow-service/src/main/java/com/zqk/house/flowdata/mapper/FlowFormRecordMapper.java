package com.zqk.house.flowdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zqk.house.flowdata.entity.FlowFormRecord;
import com.zqk.house.flowdata.vo.DataStatsVO;
import com.zqk.house.flowdata.vo.FormRecordListVO;
import com.zqk.house.flowdata.vo.FormRecordQueryForm;
import com.zqk.house.flowdata.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowFormRecordMapper extends BaseMapper<FlowFormRecord> {

    List<FormRecordListVO> selectRecordList(@Param("form") FormRecordQueryForm form,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    Long selectRecordCount(@Param("form") FormRecordQueryForm form);

    FormRecordListVO selectRecordById(@Param("id") Long id);

    DataStatsVO selectStats(@Param("userId") Long userId);

    List<TrendPointVO> selectTrend(@Param("days") int days);
}
