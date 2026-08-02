package com.zqk.house.flowtemplate.controller;

import com.zqk.house.flowtemplate.entity.FlowTemplate;
import com.zqk.house.flowtemplate.entity.FlowTemplateQueryForm;
import com.zqk.house.flowtemplate.service.FlowTemplateService;
import com.zqk.house.flowtemplate.vo.TemplateDetailVO;
import com.zqk.house.flowtemplate.vo.TemplateFlowSaveDTO;
import com.zqk.house.flowtemplate.vo.TemplateStatsVO;
import com.zqk.house.util.PageResult;
import com.zqk.house.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/flow-template")
@CrossOrigin
public class FlowTemplateController {

    @Autowired
    private FlowTemplateService flowTemplateService;

    @PostMapping("/list")
    public Result<PageResult<FlowTemplate>> list(@RequestBody FlowTemplateQueryForm form) {
        return Result.success("获取成功", flowTemplateService.getPage(form));
    }

    @GetMapping("/{id}")
    public Result<TemplateDetailVO> detail(@PathVariable Long id) {
        TemplateDetailVO vo = flowTemplateService.getDetail(id);
        return vo != null ? Result.success("获取成功", vo) : Result.notFound("模板不存在");
    }

    @PostMapping("/save")
    public Result<Long> save(@RequestBody FlowTemplate template) {
        return Result.success("创建成功", flowTemplateService.save(template));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody FlowTemplate template) {
        return flowTemplateService.update(template) ? Result.success("更新成功") : Result.fail("更新失败");
    }

    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable Long id) {
        return flowTemplateService.toggleStatus(id) ? Result.success("操作成功") : Result.fail("操作失败");
    }

    @PostMapping("/copy/{id}")
    public Result<Long> copy(@PathVariable Long id) {
        Long newId = flowTemplateService.copy(id);
        return newId != null ? Result.success("复制成功", newId) : Result.fail("复制失败");
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        return flowTemplateService.delete(id) ? Result.success("删除成功") : Result.fail("删除失败");
    }

    @GetMapping("/stats")
    public Result<TemplateStatsVO> stats() {
        return Result.success("获取成功", flowTemplateService.getStats());
    }

    @GetMapping("/enabled-list")
    public Result<List<FlowTemplate>> enabledList() {
        return Result.success("获取成功", flowTemplateService.getEnabledList());
    }

    @PutMapping("/save-flow")
    public Result<Void> saveFlow(@RequestBody TemplateFlowSaveDTO dto) {
        try {
            return flowTemplateService.saveFlow(dto) ? Result.success("保存成功") : Result.fail("保存失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }
}
