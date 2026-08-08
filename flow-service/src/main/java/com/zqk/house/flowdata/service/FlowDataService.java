package com.zqk.house.flowdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zqk.house.flowdata.entity.FlowFormData;
import com.zqk.house.flowdata.mapper.FlowFormDataMapper;
import com.zqk.house.flowdata.mapper.FlowFormRecordMapper;
import com.zqk.house.flowdata.vo.DataStatsVO;
import com.zqk.house.flowdata.vo.DashboardVO;
import com.zqk.house.flowdata.vo.FormRecordDetailVO;
import com.zqk.house.flowdata.vo.FormRecordListVO;
import com.zqk.house.flowdata.vo.FormRecordQueryForm;
import com.zqk.house.flowdata.vo.PersonRecordVO;
import com.zqk.house.flowdata.vo.PersonSubmitVO;
import com.zqk.house.flowdata.vo.TrendPointVO;
import com.zqk.house.flowtask.entity.FlowTask;
import com.zqk.house.flowtask.entity.FlowTaskDispatch;
import com.zqk.house.flowtask.entity.FlowTaskDispatchNode;
import com.zqk.house.flowtask.entity.FlowTaskNode;
import com.zqk.house.flowtask.mapper.FlowTaskDispatchMapper;
import com.zqk.house.flowtask.mapper.FlowTaskDispatchNodeMapper;
import com.zqk.house.flowtask.mapper.FlowTaskMapper;
import com.zqk.house.flowtask.mapper.FlowTaskNodeMapper;
import com.zqk.house.flowtask.vo.FormDataItemVO;
import com.zqk.house.flowtemplate.entity.FlowTemplateField;
import com.zqk.house.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.SecurityUtils;
import com.zqk.house.sysuser.vo.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FlowDataService {

    @Autowired
    private FlowFormRecordMapper flowFormRecordMapper;
    @Autowired
    private FlowFormDataMapper flowFormDataMapper;
    @Autowired
    private FlowTaskMapper flowTaskMapper;
    @Autowired
    private FlowTaskDispatchMapper flowTaskDispatchMapper;
    @Autowired
    private FlowTaskNodeMapper flowTaskNodeMapper;
    @Autowired
    private FlowTaskDispatchNodeMapper flowTaskDispatchNodeMapper;
    @Autowired
    private FlowTemplateFieldMapper flowTemplateFieldMapper;
    @Autowired
    private ObjectMapper objectMapper;

    public PageResult<FormRecordListVO> getPage(FormRecordQueryForm form) {
        int page = form.getPage() == null ? 1 : form.getPage();
        int limit = form.getLimit() == null ? 10 : form.getLimit();
        int offset = (page - 1) * limit;
        List<FormRecordListVO> records = flowFormRecordMapper.selectRecordList(form, offset, limit);
        Long total = flowFormRecordMapper.selectRecordCount(form);
        return new PageResult<>(records, total);
    }

    public FormRecordDetailVO getDetail(Long id) {
        FormRecordListVO record = flowFormRecordMapper.selectRecordById(id);
        if (record == null) return null;
        FormRecordDetailVO vo = new FormRecordDetailVO();
        vo.setRecord(record);
        LambdaQueryWrapper<FlowFormData> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowFormData::getRecordId, id);
        List<FlowFormData> dataList = flowFormDataMapper.selectList(fw);
        vo.setFormDataList(dataList);
        // 字段 label/type 回填（期次快照优先，降级当前模板）
        Map<Long, FlowTemplateField> fmap = buildFieldMap(record.getTaskId(), record.getTemplateId());
        if (dataList != null && !dataList.isEmpty()) {
            List<FormDataItemVO> items = dataList.stream().map(fd -> {
                FormDataItemVO item = new FormDataItemVO();
                FlowTemplateField f = fmap.get(fd.getFieldId());
                item.setFieldLabel(f == null ? fd.getFieldKey() : f.getFieldLabel());
                item.setFieldValue(fd.getFieldValue());
                item.setFieldType(f == null ? null : f.getFieldType());
                return item;
            }).collect(Collectors.toList());
            vo.setFormDataItems(items);
        }
        return vo;
    }

    /** 期次快照字段（节点字段 + 模板级字段）优先；无快照降级当前模板字段 */
    private Map<Long, FlowTemplateField> buildFieldMap(Long taskId, Long templateId) {
        Map<Long, FlowTemplateField> map = new HashMap<>();
        if (taskId != null) {
            FlowTask task = flowTaskMapper.selectById(taskId);
            if (task != null) {
                // 节点快照字段（经 task_node.dispatch_node_id 定位快照节点）
                if (task.getDispatchId() != null) {
                    List<FlowTaskNode> tns = flowTaskNodeMapper.selectList(
                            new LambdaQueryWrapper<FlowTaskNode>().eq(FlowTaskNode::getTaskId, taskId));
                    List<Long> dispatchNodeIds = tns.stream()
                            .map(FlowTaskNode::getDispatchNodeId)
                            .filter(java.util.Objects::nonNull).distinct().collect(Collectors.toList());
                    if (!dispatchNodeIds.isEmpty()) {
                        List<FlowTaskDispatchNode> snaps = flowTaskDispatchNodeMapper.selectBatchIds(dispatchNodeIds);
                        for (FlowTaskDispatchNode s : snaps) {
                            List<FlowTemplateField> fs = parseSnapshotFields(s.getFieldsJson());
                            if (fs != null) for (FlowTemplateField f : fs) map.putIfAbsent(f.getId(), f);
                        }
                    }
                }
                // 期次模板级字段快照
                if (task.getDispatchId() != null) {
                    FlowTaskDispatch d = flowTaskDispatchMapper.selectById(task.getDispatchId());
                    if (d != null) {
                        List<FlowTemplateField> tfs = parseSnapshotFields(d.getTemplateFieldsJson());
                        if (tfs != null) for (FlowTemplateField f : tfs) map.putIfAbsent(f.getId(), f);
                    }
                }
            }
        }
        // 降级：当前模板字段
        if (templateId != null && map.isEmpty()) {
            List<FlowTemplateField> fields = flowTemplateFieldMapper.selectList(
                    new LambdaQueryWrapper<FlowTemplateField>().eq(FlowTemplateField::getTemplateId, templateId));
            for (FlowTemplateField f : fields) map.putIfAbsent(f.getId(), f);
        }
        return map;
    }

    /** 快照字段 JSON → 字段定义列表 */
    private List<FlowTemplateField> parseSnapshotFields(String fieldsJson) {
        if (!StringUtils.hasText(fieldsJson)) return null;
        try {
            return objectMapper.readValue(fieldsJson, new TypeReference<List<FlowTemplateField>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    public DataStatsVO getStats() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getId();
        return flowFormRecordMapper.selectStats(userId);
    }

    public List<TrendPointVO> getTrend(int days) {
        return flowFormRecordMapper.selectTrend(days);
    }

    // ==================== 按人员展示 ====================

    /** 人员提交汇总（分页；taskId 非空时仅统计该任务下人员） */
    public PageResult<PersonSubmitVO> getPersonPage(String name, Long taskId, Integer page, Integer limit) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        List<PersonSubmitVO> list = flowFormRecordMapper.selectPersonPage(name, taskId, offset, l);
        Long total = flowFormRecordMapper.selectPersonCount(name, taskId);
        return new PageResult<>(list, total);
    }

    /** 某人历史提交记录（分页；taskId 非空时仅统计该任务下提交） */
    public PageResult<PersonRecordVO> getPersonRecords(Long userId, Long taskId, Integer page, Integer limit) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        List<PersonRecordVO> list = flowFormRecordMapper.selectPersonRecordsPage(userId, taskId, offset, l);
        Long total = flowFormRecordMapper.selectPersonRecordsCount(userId, taskId);
        return new PageResult<>(list, total);
    }

    // ==================== 数据展示页 ====================

    /** 数据展示页聚合数据（统计卡 + 各图表；trendType 控制提交趋势粒度，startDate/endDate 为可选时间范围） */
    public DashboardVO getDashboard(String trendType, String startDate, String endDate) {
        DashboardVO vo = new DashboardVO();
        vo.setStats(getStats());
        DashboardVO totals = flowFormRecordMapper.selectDashboardTotals();
        vo.setSubmitTotal(totals == null ? 0L : totals.getSubmitTotal());
        vo.setPersonCount(totals == null ? 0L : totals.getPersonCount());
        vo.setTaskStatus(flowFormRecordMapper.selectTaskStatusDist());
        vo.setPeriodStatus(flowFormRecordMapper.selectPeriodStatusDist());
        vo.setNodeStatus(flowFormRecordMapper.selectNodeStatusDist());
        vo.setPeriodCycle(flowFormRecordMapper.selectPeriodCycleDist());
        vo.setDispatchDeptRank(flowFormRecordMapper.selectDispatchDeptRank());
        vo.setHandlerDeptRank(flowFormRecordMapper.selectHandlerDeptRank());
        vo.setTemplateNodeDist(flowFormRecordMapper.selectTemplateNodeDist());
        vo.setFieldTypeRank(flowFormRecordMapper.selectFieldTypeRank());
        String type = StringUtils.hasText(trendType) ? trendType.trim() : "day";
        vo.setTrend(flowFormRecordMapper.selectTrendByType(type, startDate, endDate));
        return vo;
    }
}
