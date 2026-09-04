package com.company.flow.sys.base.attach.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.flow.sys.base.attach.entity.Attach;
import com.company.flow.sys.base.attach.mapper.AttachMapper;
import com.company.flow.sys.base.autuser.vo.LoginUser;
import com.company.flow.sys.base.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 附件服务：批量上传（仅记录关键信息，不落盘）、列表、预览、下载、删除
 * ecsUrl 仅记录随机字符（无真实文件存储），预览/下载/删除均以日志输出体现
 */
@Service
public class AttachService {

    private static final Logger log = LoggerFactory.getLogger(AttachService.class);

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
        return u != null ? u.getUserName() : null;
    }

    /** 单文件关键信息入库，返回入库后的 Attach（creator 优先取调用方传入，空则取登录用户） */
    public Attach saveFile(MultipartFile file, String bizId, String creator) {
        String fileName = file.getOriginalFilename();
        if (!StringUtils.hasText(fileName)) fileName = file.getName();
        Attach a = new Attach();
        a.setAttachId(genAttachId());
        a.setBizId(StringUtils.hasText(bizId) ? bizId : null);
        a.setFileName(fileName.length() > 256 ? fileName.substring(0, 256) : fileName);
        a.setEcsUrl(genEcsUrl());
        a.setCreator(StringUtils.hasText(creator) ? creator : currentUserName());
        // 手动回填时间（数据库 DEFAULT CURRENT_TIMESTAMP 不会回填到返回对象，前端列表需展示）
        a.setCreateTime(new Date());
        a.setModifiedTime(new Date());
        attachMapper.insert(a);
        log.info("附件上传：attachId={}, 文件名={}, bizId={}, 创建人={}", a.getAttachId(), a.getFileName(), a.getBizId(), a.getCreator());
        return a;
    }

    /** 批量上传：MultipartFile[] 数组逐条入库，返回入库后的 Attach 列表（顺序对应入参） */
    public List<Attach> saveFiles(MultipartFile[] files, String bizId, String creator) {
        List<Attach> list = new ArrayList<>();
        if (files == null) return list;
        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;
            list.add(saveFile(f, bizId, creator));
        }
        return list;
    }

    /**
     * 期次快照复制：为模板说明文件复制一条附件记录（新 attachId，bizId 改为期次维度）。
     * 使模板中删除/替换说明文件不影响历史期次快照的预览/下载（防悬空引用）。
     */
    public Attach copyForSnapshot(Attach src, String bizId) {
        Attach copy = new Attach();
        copy.setAttachId(genAttachId());
        copy.setBizId(StringUtils.hasText(bizId) ? bizId : src.getBizId());
        copy.setFileName(src.getFileName());
        copy.setEcsUrl(src.getEcsUrl());
        copy.setCreator(src.getCreator());
        copy.setCreateTime(src.getCreateTime());
        copy.setModifiedTime(new Date());
        attachMapper.insert(copy);
        log.info("附件快照复制：attachId={}, 文件名={}, 原attachId={}, bizId={}", copy.getAttachId(), copy.getFileName(), src.getAttachId(), copy.getBizId());
        return copy;
    }

    /** 附件列表（按业务id查询，按创建时间升序） */
    public List<Attach> list(String bizId) {
        LambdaQueryWrapper<Attach> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(bizId)) w.eq(Attach::getBizId, bizId);
        w.orderByAsc(Attach::getCreateTime);
        return attachMapper.selectList(w);
    }

    /** 按附件id查询（不存在返回 null） */
    public Attach getById(String attachId) {
        return attachMapper.selectById(attachId);
    }

    /** 预览附件：日志输出体现 */
    public Attach preview(String attachId) {
        Attach a = attachMapper.selectById(attachId);
        if (a == null) return null;
        log.info("附件预览：attachId={}, 文件名={}, ecsUrl={}, 操作人={}", a.getAttachId(), a.getFileName(), a.getEcsUrl(), currentUserName());
        return a;
    }

    /** 下载附件：日志输出体现 */
    public Attach download(String attachId) {
        Attach a = attachMapper.selectById(attachId);
        if (a == null) return null;
        log.info("附件下载：attachId={}, 文件名={}, ecsUrl={}, 操作人={}", a.getAttachId(), a.getFileName(), a.getEcsUrl(), currentUserName());
        return a;
    }

    /** 删除附件：日志输出体现并删除记录 */
    public boolean delete(String attachId) {
        Attach a = attachMapper.selectById(attachId);
        if (a == null) return false;
        log.info("附件删除：attachId={}, 文件名={}, 操作人={}", a.getAttachId(), a.getFileName(), currentUserName());
        return attachMapper.deleteById(attachId) > 0;
    }
}
