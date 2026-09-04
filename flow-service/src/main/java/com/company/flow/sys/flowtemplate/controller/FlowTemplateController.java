package com.company.flow.sys.flowtemplate.controller;

import com.company.flow.sys.flowtemplate.entity.FlowTemplate;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateQueryForm;
import com.company.flow.sys.flowtemplate.entity.FlowTemplateVersion;
import com.company.flow.sys.flowtemplate.service.FlowTemplateService;
import com.company.flow.sys.flowtemplate.vo.TemplateDetailVO;
import com.company.flow.sys.flowtemplate.vo.TemplateFlowSaveDTO;
import com.company.flow.sys.flowtemplate.vo.TemplateStatsVO;
import com.company.flow.sys.base.result.PageResult;
import com.company.flow.sys.base.result.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
    public Result<TemplateDetailVO> detail(@PathVariable String id) {
        try {
            TemplateDetailVO vo = flowTemplateService.getDetail(id);
            return vo != null ? Result.success("获取成功", vo) : Result.notFound("模板不存在");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/save")
    public Result<String> save(@RequestBody FlowTemplate template) {
        return Result.success("创建成功", flowTemplateService.save(template));
    }

    @PutMapping("/update")
    public Result<Void> update(@RequestBody FlowTemplate template) {
        try {
            return flowTemplateService.update(template) ? Result.success("更新成功") : Result.fail("更新失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PutMapping("/toggle-status/{id}")
    public Result<Void> toggleStatus(@PathVariable String id) {
        try {
            return flowTemplateService.toggleStatus(id) ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 设置/取消样例：仅超管可操作（样例公共可见、不可改，模板可复制） */
    @PutMapping("/sample/{id}")
    public Result<Void> toggleSample(@PathVariable String id) {
        try {
            return flowTemplateService.toggleSample(id) ? Result.success("操作成功") : Result.fail("操作失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/copy/{id}")
    public Result<String> copy(@PathVariable String id) {
        String newId = flowTemplateService.copy(id);
        return newId != null ? Result.success("复制成功", newId) : Result.fail("复制失败");
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable String id) {
        try {
            return flowTemplateService.delete(id) ? Result.success("删除成功") : Result.fail("删除失败");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @GetMapping("/stats")
    public Result<TemplateStatsVO> stats() {
        return Result.success("获取成功", flowTemplateService.getStats());
    }

    /** 模板被使用情况（任务数/期次数/名称列表）：保存流程设计前提示用户修改不影响已下发期次 */
    @GetMapping("/usage/{id}")
    public Result<Map<String, Object>> usage(@PathVariable String id) {
        return Result.success("获取成功", flowTemplateService.usageCount(id));
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

    /** 单独保存某节点的说明文件（上传/删除后即时持久化，避免刷新丢失） */
    @PutMapping("/node-guide-files")
    public Result<Void> saveNodeGuideFiles(@RequestBody Map<String, Object> body) {
        try {
            Object nid = body.get("nodeId");
            String nodeId = nid == null ? null : String.valueOf(nid);
            Object gfs = body.get("guideFiles");
            String guideFiles = gfs == null ? null : String.valueOf(gfs);
            flowTemplateService.saveNodeGuideFiles(nodeId, guideFiles);
            return Result.success("保存成功");
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    /** 模板版本记录列表 */
    @GetMapping("/versions/{id}")
    public Result<List<FlowTemplateVersion>> versions(@PathVariable String id) {
        return Result.success("获取成功", flowTemplateService.getVersions(id));
    }
}
