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
import com.company.flow.sys.flowdata.vo.ScopedStatsVO;
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

    // ==================== 数据展示：个人维度（普通用户，只看与自己相关的） ====================

    /** 个人统计卡：我的任务数/期次数、我的待办与已办、我的提交数 */
    ScopedStatsVO selectUserStats(@Param("userId") String userId);

    /** 我的任务状态分布（flow_task.owner_id = 我） */
    List<NameCountVO> selectUserTaskStatusDist(@Param("userId") String userId);

    /** 我的节点进度（flow_task_node.handler_user_id = 我，按 submit_status 统计） */
    List<NameCountVO> selectUserNodeStatusDist(@Param("userId") String userId);

    /** 我的提交趋势（flow_form_record.user_id = 我） */
    List<TrendPointVO> selectUserTrendByType(@Param("userId") String userId,
                                             @Param("type") String type,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate);

    // ==================== 数据展示：部门维度（部门管理员，只看本部门） ====================

    /** 部门统计卡：部门任务数/期次数、进行中与已完成、部门人数、部门待办与已办、部门提交数 */
    ScopedStatsVO selectDeptStats(@Param("deptId") Long deptId);

    /** 部门任务状态分布（本部门成员的任务） */
    List<NameCountVO> selectDeptTaskStatusDist(@Param("deptId") Long deptId);

    /** 部门期次状态分布（本部门创建的任务下的期次） */
    List<NameCountVO> selectDeptPeriodStatusDist(@Param("deptId") Long deptId);

    /** 部门下发排行：本部门各管理员（以 dept_admin 登记为准）下发的期次数 */
    List<NameCountVO> selectDeptDispatchRank(@Param("deptId") Long deptId);

    /** 下发期次中未处理的任务：本部门下发的期次里仍处于进行中的成员任务数，按期次列出 */
    List<NameCountVO> selectDeptPeriodPendingRank(@Param("deptId") Long deptId);

    /** 部门字段类型使用排行（按本部门任务所用的模板统计） */
    List<NameCountVO> selectDeptFieldTypeRank(@Param("deptId") Long deptId);

    /** 部门模板节点数分布（按本部门任务所用的模板统计） */
    List<NameCountVO> selectDeptTemplateNodeDist(@Param("deptId") Long deptId);

    /** 部门提交趋势（本部门成员的提交记录） */
    List<TrendPointVO> selectDeptTrendByType(@Param("deptId") Long deptId,
                                             @Param("type") String type,
                                             @Param("startDate") String startDate,
                                             @Param("endDate") String endDate);
}
