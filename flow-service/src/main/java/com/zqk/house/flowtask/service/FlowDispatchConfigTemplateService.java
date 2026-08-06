package com.zqk.house.flowtask.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zqk.house.flowtask.entity.FlowDispatchConfigTemplate;
import com.zqk.house.flowtask.mapper.FlowDispatchConfigTemplateMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 下发配置模板：增删改查，供新建/编辑任务时一键拉取复用。
 */
@Service
public class FlowDispatchConfigTemplateService {

    @Autowired
    private FlowDispatchConfigTemplateMapper mapper;

    /** 全量列表（按创建时间倒序，配置数量有限无需分页） */
    public List<FlowDispatchConfigTemplate> list(String keyword) {
        LambdaQueryWrapper<FlowDispatchConfigTemplate> qw = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like(FlowDispatchConfigTemplate::getConfigName, keyword.trim())
              .or().like(FlowDispatchConfigTemplate::getRemark, keyword.trim());
        }
        qw.orderByDesc(FlowDispatchConfigTemplate::getId);
        return mapper.selectList(qw);
    }

    public FlowDispatchConfigTemplate getById(Long id) {
        return id == null ? null : mapper.selectById(id);
    }

    /** 新增或更新（存在 id 则更新） */
    public FlowDispatchConfigTemplate save(FlowDispatchConfigTemplate tpl) {
        Date now = new Date();
        if (tpl.getId() == null) {
            tpl.setCreateTime(now);
            tpl.setUpdateTime(now);
            mapper.insert(tpl);
        } else {
            tpl.setUpdateTime(now);
            mapper.updateById(tpl);
        }
        return tpl;
    }

    /** 删除模板：不影响已拉取到任务上的配置 */
    public boolean delete(Long id) {
        return id != null && mapper.deleteById(id) > 0;
    }
}
