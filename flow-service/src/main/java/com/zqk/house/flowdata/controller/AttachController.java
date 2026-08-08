package com.zqk.house.flowdata.controller;

import com.zqk.house.flowdata.entity.Attach;
import com.zqk.house.flowdata.service.AttachService;
import com.zqk.house.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件管理：文件/图片上传（支持单文件 file 与多文件 files 数组）、列表、预览、下载、删除
 * 仅记录文件关键信息（attachId/文件名/随机 ecsUrl）入库，不做真实文件存储
 */
@RestController
@RequestMapping("/flow-attach")
@CrossOrigin
public class AttachController {

    @Autowired
    private AttachService attachService;

    /** 单文件上传（兼容旧字段 file），返回单个 Attach */
    @PostMapping("/upload")
    public Result<Attach> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "bizId", required = false) String bizId,
                                 @RequestParam(value = "creator", required = false) String creator) {
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }
        return Result.success("上传成功", attachService.saveFile(file, bizId, creator));
    }

    /** 批量上传：files 数组逐条入库，返回 List&lt;Attach&gt; */
    @PostMapping("/uploads")
    public Result<List<Attach>> uploads(@RequestParam("files") MultipartFile[] files,
                                        @RequestParam(value = "bizId", required = false) String bizId,
                                        @RequestParam(value = "creator", required = false) String creator) {
        if (files == null || files.length == 0) {
            return Result.fail("上传文件不能为空");
        }
        return Result.success("上传成功", attachService.saveFiles(files, bizId, creator));
    }

    /** 附件列表（按业务id查询，按创建时间升序） */
    @GetMapping("/list")
    public Result<List<Attach>> list(@RequestParam(value = "bizId", required = false) String bizId) {
        return Result.success("获取成功", attachService.list(bizId));
    }

    /** 预览附件：日志输出体现 */
    @GetMapping("/preview/{attachId}")
    public Result<Attach> preview(@PathVariable String attachId) {
        Attach a = attachService.preview(attachId);
        if (a == null) return Result.notFound("附件不存在");
        return Result.success("预览成功", a);
    }

    /** 下载附件：日志输出体现 */
    @GetMapping("/download/{attachId}")
    public Result<Attach> download(@PathVariable String attachId) {
        Attach a = attachService.download(attachId);
        if (a == null) return Result.notFound("附件不存在");
        return Result.success("下载成功", a);
    }

    /** 删除附件：日志输出体现并删除记录 */
    @DeleteMapping("/{attachId}")
    public Result<Void> delete(@PathVariable String attachId) {
        boolean ok = attachService.delete(attachId);
        return ok ? Result.success("删除成功") : Result.notFound("附件不存在");
    }
}
