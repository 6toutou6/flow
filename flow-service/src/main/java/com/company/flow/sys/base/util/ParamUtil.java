package com.company.flow.sys.base.util;

/**
 * 请求参数解析工具：从 Map 值等 Object 安全提取字符串/整数/布尔
 * （post 请求参数统一走 body，Controller 内用本工具取值，避免各类手写类型转换）
 */
public class ParamUtil {

    private ParamUtil() {
    }

    /** 对象转字符串（null 安全，去除首尾空白；null 返回 null） */
    public static String str(Object o) {
        return o == null ? null : String.valueOf(o).trim();
    }

    /** 对象转整数（null 安全，解析失败返回 null） */
    public static Integer intv(Object o) {
        if (o == null) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 对象转整数，null / 解析失败时取默认值 */
    public static int intv(Object o, int defaultValue) {
        Integer v = intv(o);
        return v == null ? defaultValue : v;
    }

    /** 对象转布尔（null 安全，解析失败返回 null） */
    public static Boolean boolv(Object o) {
        if (o == null) {
            return null;
        }
        return Boolean.valueOf(String.valueOf(o).trim());
    }

    /** 对象转布尔，null / 解析失败时取默认值 */
    public static boolean boolv(Object o, boolean defaultValue) {
        Boolean v = boolv(o);
        return v == null ? defaultValue : v;
    }
}
