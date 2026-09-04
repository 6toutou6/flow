package com.company.flow.sys.flowdata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.flow.sys.flowdata.entity.FlowFormRecord;
import com.company.flow.sys.flowdata.vo.DashboardVO;
import com.company.flow.sys.flowdata.vo.DataStatsVO;
import com.company.flow.sys.flowdata.vo.FormRecordListVO;
import com.company.flow.sys.flowdata.vo.FormRecordQueryForm;
import com.company.flow.sys.flowdata.vo.NameCountVO;
import com.company.flow.sys.flowdata.vo.PersonNodeVO;
import com.company.flow.sys.flowdata.vo.PersonSubmitVO;
import com.company.flow.sys.flowdata.vo.TrendPointVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FlowFormRecordMapper extends BaseMapper<FlowFormRecord> {

    List<FormRecordListVO> selectRecordList(@Param("form") FormRecordQueryForm form,
                                            @Param("offset") int offset,
                                            @Param("limit") int limit);

    Long selectRecordCount(@Param("form") FormRecordQueryForm form);

    FormRecordListVO selectRecordById(@Param("id") String id);

    DataStatsVO selectStats(@Param("userId") String userId);

    List<TrendPointVO> selectTrend(@Param("days") int days);

    List<PersonSubmitVO> selectPersonPage(@Param("name") String name,
                                          @Param("taskId") String taskId,
                                          @Param("offset") int offset,
                                          @Param("limit") int limit);

    Long selectPersonCount(@Param("name") String name,
                           @Param("taskId") String taskId);

    /** 某人参与的任务链全部节点行（taskId 非空时仅返回该任务下的链；人员参与判定：任一节点处理人 = userId） */
    List<PersonNodeVO> selectPersonRecordsPage(@Param("userId") String userId,
                                               @Param("taskId") String taskId,
                                               @Param("offset") int offset,
                                               @Param("limit") int limit);

    Long selectPersonRecordsCount(@Param("userId") String userId,
                                  @Param("taskId") String taskId);

    DashboardVO selectDashboardTotals();

    List<NameCountVO> selectTaskStatusDist();

    List<NameCountVO> selectPeriodStatusDist();

    /** 节点状态分布（flow_task_node.submit_status：待处理/已完成） */
    List<NameCountVO> selectNodeStatusDist();

    /** 下发周期类型分布（flow_dispatch_config.cycle_type：每周/每月/每季度/单次） */
    List<NameCountVO> selectPeriodCycleDist();

    /** 下发部门排行：各任务创建部门的下发期次数 */
    List<NameCountVO> selectDispatchDeptRank();

    /** 处理人部门排行：各提交人部门的历史提交次数 */
    List<NameCountVO> selectHandlerDeptRank();

    /** 模板节点数分布：按节点数分档统计模板数（如 1个节点有 N 个模板） */
    List<NameCountVO> selectTemplateNodeDist();

    /** 字段类型使用排行：各字段类型被使用的数量 */
    List<NameCountVO> selectFieldTypeRank();

    /** 提交趋势（按粒度：day=日 / month=月 / quarter=季度 / year=年；可选 startDate/endDate 时间范围） */
    List<TrendPointVO> selectTrendByType(@Param("type") String type,
                                         @Param("startDate") String startDate,
                                         @Param("endDate") String endDate);
}
