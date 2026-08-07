package com.zqk.house.flowdata.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zqk.house.flowdata.entity.Attach;
import com.zqk.house.flowdata.mapper.AttachMapper;
import com.zqk.house.sysuser.vo.LoginUser;
import com.zqk.house.util.Result;
import com.zqk.house.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 附件管理：文件/图片上传、列表、预览、下载、删除
 * ecsUrl 仅记录随机字符（无真实文件存储），预览/下载/删除均以日志输出体现
 */
@RestController
@RequestMapping("/flow-attach")
@CrossOrigin
public class AttachController {

    private static final Logger log = LoggerFactory.getLogger(AttachController.class);

    private static final SimpleDateFormat TS = new SimpleDateFormat("yyyyMMddHHmmssSSS");

    @Autowired
    private AttachMapper attachMapper;

    /** 生成附件id：AT + 时间戳字符串 + 随机后缀 */
    private String genAttachId() {
        return "AT" + TS.format(new Date()) + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /** 生成随机 ecs_url */
    private String genEcsUrl() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().substring(0, 8);
    }

    /** 当前登录用户姓名（未登录返回空） */
    private String currentUserName() {
        LoginUser u = SecurityUtils.getLoginUser();
        return u != null ? u.getRealName() : null;
    }

    /**
     * 上传附件：记录文件名、业务id等，ecs_url 随机字符。
     * multipart/form-data：file + bizId（可选）
     */
    @PostMapping("/upload")
    public Result<Attach> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "bizId", required = false) String bizId) {
        if (file == null || file.isEmpty()) {
            return Result.fail("上传文件不能为空");
        }
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) fileName = file.getName();
        Attach a = new Attach();
        a.setAttachId(genAttachId());
        a.setBizId(StringUtils.hasText(bizId) ? bizId : null);
        a.setFileName(fileName.length() > 256 ? fileName.substring(0, 256) : fileName);
        a.setEcsUrl(genEcsUrl());
        a.setCreator(currentUserName());
        attachMapper.insert(a);
        log.info("附件上传：attachId={}, 文件名={}, bizId={}, 创建人={}", a.getAttachId(), a.getFileName(), a.getBizId(), a.getCreator());
        return Result.success("上传成功", a);
    }

    /** 附件列表（按业务id查询，按创建时间升序） */
    @GetMapping("/list")
    public Result<List<Attach>> list(@RequestParam(value = "bizId", required = false) String bizId) {
        LambdaQueryWrapper<Attach> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(bizId)) w.eq(Attach::getBizId, bizId);
        w.orderByAsc(Attach::getCreateTime);
        return Result.success("获取成功", attachMapper.selectList(w));
    }

    /** 预览附件：日志输出体现 */
    @GetMapping("/preview/{attachId}")
    public Result<Attach> preview(@PathVariable String attachId) {
        Attach a = attachMapper.selectById(attachId);
        if (a == null) return Result.notFound("附件不存在");
        log.info("附件预览：attachId={}, 文件名={}, ecsUrl={}, 操作人={}", a.getAttachId(), a.getFileName(), a.getEcsUrl(), currentUserName());
        return Result.success("预览成功", a);
    }

    /** 下载附件：日志输出体现 */
    @GetMapping("/download/{attachId}")
    public Result<Attach> download(@PathVariable String attachId) {
        Attach a = attachMapper.selectById(attachId);
        if (a == null) return Result.notFound("附件不存在");
        log.info("附件下载：attachId={}, 文件名={}, ecsUrl={}, 操作人={}", a.getAttachId(), a.getFileName(), a.getEcsUrl(), currentUserName());
        return Result.success("下载成功", a);
    }

    /** 删除附件：日志输出体现并删除记录 */
    @DeleteMapping("/{attachId}")
    public Result<Void> delete(@PathVariable String attachId) {
        Attach a = attachMapper.selectById(attachId);
        if (a == null) return Result.notFound("附件不存在");
        log.info("附件删除：attachId={}, 文件名={}, 操作人={}", a.getAttachId(), a.getFileName(), currentUserName());
        attachMapper.deleteById(attachId);
        return Result.success("删除成功");
    }
}
