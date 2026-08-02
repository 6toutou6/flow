package com.zqk.house.flowdata.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zqk.house.flowdata.entity.FlowAttachment;
import com.zqk.house.flowdata.entity.FlowFormData;
import com.zqk.house.flowdata.mapper.FlowAttachmentMapper;
import com.zqk.house.flowdata.mapper.FlowFormDataMapper;
import com.zqk.house.flowdata.mapper.FlowFormRecordMapper;
import com.zqk.house.flowdata.vo.DataStatsVO;
import com.zqk.house.flowdata.vo.FormRecordDetailVO;
import com.zqk.house.flowdata.vo.FormRecordListVO;
import com.zqk.house.flowdata.vo.FormRecordQueryForm;
import com.zqk.house.flowdata.vo.TrendPointVO;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.SecurityUtils;
import com.zqk.house.sysuser.vo.LoginUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FlowDataService {

    @Autowired
    private FlowFormRecordMapper flowFormRecordMapper;
    @Autowired
    private FlowFormDataMapper flowFormDataMapper;
    @Autowired
    private FlowAttachmentMapper flowAttachmentMapper;

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
        vo.setFormDataList(flowFormDataMapper.selectList(fw));
        LambdaQueryWrapper<FlowAttachment> aw = new LambdaQueryWrapper<>();
        aw.eq(FlowAttachment::getRecordId, id);
        vo.setAttachments(flowAttachmentMapper.selectList(aw));
        return vo;
    }

    public DataStatsVO getStats() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser == null ? null : loginUser.getId();
        return flowFormRecordMapper.selectStats(userId);
    }

    public List<TrendPointVO> getTrend(int days) {
        return flowFormRecordMapper.selectTrend(days);
    }
}
