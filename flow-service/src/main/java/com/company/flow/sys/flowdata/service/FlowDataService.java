package com.company.flow.sys.flowdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.flow.sys.flowdata.entity.FlowFormData;
import com.company.flow.sys.flowdata.mapper.FlowFormDataMapper;
import com.company.flow.sys.flowdata.mapper.FlowFormRecordMapper;
import com.company.flow.sys.flowdata.vo.DataStatsVO;
import com.company.flow.sys.flowdata.vo.DashboardVO;
import com.company.flow.sys.flowdata.vo.FormRecordDetailVO;
import com.company.flow.sys.flowdata.vo.FormRecordListVO;
import com.company.flow.sys.flowdata.vo.FormRecordQueryForm;
import com.company.flow.sys.flowdata.vo.PersonNodeVO;
import com.company.flow.sys.flowdata.vo.PersonSubmitVO;
import com.company.flow.sys.flowdata.vo.TrendPointVO;
import com.company.flow.sys.flowtask.entity.FlowTask;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatch;
import com.company.flow.sys.flowtask.entity.FlowTaskDispatchNode;
import com.company.flow.sys.flowtask.entity.FlowTaskNode;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskDispatchNodeMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskMapper;
import com.company.flow.sys.flowtask.mapper.FlowTaskNodeMapper;
import com.company.flow.sys.flowtask.vo.FormDataItemVO;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateField;
import com.company.flow.sys.flowtemplate.mapper.FlowTemplateFieldMapper;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.util.SecurityUtils;
import com.company.flow.sys.base.autuser.vo.LoginUser;
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

    public FormRecordDetailVO getDetail(String id) {
        FormRecordListVO record = flowFormRecordMapper.selectRecordById(id);
        if (record == null) return null;
        FormRecordDetailVO vo = new FormRecordDetailVO();
        vo.setRecord(record);
        LambdaQueryWrapper<FlowFormData> fw = new LambdaQueryWrapper<>();
        fw.eq(FlowFormData::getRecordId, id);
        List<FlowFormData> dataList = flowFormDataMapper.selectList(fw);
        vo.setFormDataList(dataList);
        // 字段 label/type 回填（期次快照优先，降级当前模板）
        Map<String, FlowTemplateField> fmap = buildFieldMap(record.getTaskId(), record.getTemplateId());
        // 兜底：field_key 匹配当前模板字段（历史无快照数据，原 field_id 已随模板改版失效）
        Map<String, FlowTemplateField> keyMap = new HashMap<>();
        if (record.getTemplateId() != null) {
            flowTemplateFieldMapper.selectList(
                    new LambdaQueryWrapper<FlowTemplateField>().eq(FlowTemplateField::getTemplateId, record.getTemplateId()))
                    .forEach(f -> keyMap.putIfAbsent(f.getFieldKey(), f));
        }
        if (dataList != null && !dataList.isEmpty()) {
            List<FormDataItemVO> items = dataList.stream().map(fd -> {
                FormDataItemVO item = new FormDataItemVO();
                FlowTemplateField f = fmap.get(fd.getFieldId());
                if (f == null) f = keyMap.get(fd.getFieldKey());
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
    private Map<String, FlowTemplateField> buildFieldMap(String taskId, String templateId) {
        Map<String, FlowTemplateField> map = new HashMap<>();
        if (taskId != null) {
            FlowTask task = flowTaskMapper.selectById(taskId);
            if (task != null) {
                // 节点快照字段：按期次直接查全部节点快照（不依赖 task_node.dispatch_node_id，历史数据该字段可能为空）
                if (task.getDispatchId() != null) {
                    List<FlowTaskDispatchNode> snaps = flowTaskDispatchNodeMapper.selectList(
                            new LambdaQueryWrapper<FlowTaskDispatchNode>().eq(FlowTaskDispatchNode::getDispatchId, task.getDispatchId()));
                    for (FlowTaskDispatchNode s : snaps) {
                        List<FlowTemplateField> fs = parseSnapshotFields(s.getFieldsJson());
                        if (fs != null) for (FlowTemplateField f : fs) map.putIfAbsent(f.getId(), f);
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
        // 降级/补充：当前模板字段（快照未命中的字段从当前模板补充；不覆盖快照字段）
        if (templateId != null) {
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
            // 历史快照容错：早期 enumOptions 被双重转义（\\"），降为单重转义（\"）后重试
            try {
                String fixed = fieldsJson.replace("\\\\\"", "\\\"");
                return objectMapper.readValue(fixed, new TypeReference<List<FlowTemplateField>>() {});
            } catch (Exception e2) {
                return null;
            }
        }
    }

    public DataStatsVO getStats() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String userId = loginUser == null ? null : loginUser.getYyytId();
        return flowFormRecordMapper.selectStats(userId);
    }

    public List<TrendPointVO> getTrend(int days) {
        return flowFormRecordMapper.selectTrend(days);
    }

    // ==================== 按人员展示 ====================

    /** 人员提交汇总（分页；taskId 非空时仅统计该任务下人员） */
    public PageResult<PersonSubmitVO> getPersonPage(String name, String taskId, Integer page, Integer limit) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        List<PersonSubmitVO> list = flowFormRecordMapper.selectPersonPage(name, taskId, offset, l);
        Long total = flowFormRecordMapper.selectPersonCount(name, taskId);
        return new PageResult<>(list, total);
    }

    /** 某人参与的任务链全部节点行（分页；taskId 非空时仅返回该任务下的链） */
    public PageResult<PersonNodeVO> getPersonRecords(String userId, String taskId, Integer page, Integer limit) {
        int p = page == null ? 1 : page;
        int l = limit == null ? 10 : limit;
        int offset = (p - 1) * l;
        List<PersonNodeVO> list = flowFormRecordMapper.selectPersonRecordsPage(userId, taskId, offset, l);
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
