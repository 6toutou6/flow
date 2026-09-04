package com.company.flow.sys.base.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 主键 ID 生成工具：表名缩写（全大写）+ 时间戳(yyyyMMddHHmmssSSS) + 随机数字，总长度固定 32 字符。
 * 表名缩写规则：单英文单词 → 全表名大写；多单词 → 各单词首字母大写拼接。
 * 示例：UUIDUtil.generateId("ATTACH") → ATTACH20260810172600123456789012345
 */
public class UUIDUtil {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
    private static final int LEN = 32;

    /**
     * 生成 32 位主键
     *
     * @param prefix 表名缩写（全大写，建议不超过 8 位）
     */
    public static String generateId(String prefix) {
        String ts = LocalDateTime.now().format(TS);
        int randLen = LEN - prefix.length() - ts.length();
        StringBuilder sb = new StringBuilder(LEN);
        sb.append(prefix).append(ts);
        for (int i = 0; i < randLen; i++) {
            sb.append(ThreadLocalRandom.current().nextInt(10));
        }
        return sb.toString();
    }
}
