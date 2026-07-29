package com.zqk.house.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 日期工具类
 */
public class DateUtil {
    
    private static final String DATE_PATTERN = "yyyy-MM-dd";
    
    /**
     * 将Date对象转换为纯日期格式（去除时分秒）
     * 
     * @param date 需要转换的日期
     * @return 转换后的纯日期，如果输入为null则返回null
     */
    public static Date toDateOnly(Date date) {
        if (date == null) {
            return null;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_PATTERN);
            return sdf.parse(sdf.format(date));
        } catch (ParseException e) {
            return null;
        }
    }
    
    /**
     * 将字符串转换为日期对象
     * 
     * @param dateStr 日期字符串
     * @param pattern 日期格式
     * @return 转换后的日期对象，转换失败返回null
     */
    public static Date parse(String dateStr, String pattern) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            return sdf.parse(dateStr);
        } catch (ParseException e) {
            return null;
        }
    }
    
    /**
     * 将日期对象格式化为字符串
     * 
     * @param date 日期对象
     * @param pattern 日期格式
     * @return 格式化后的字符串，如果输入为null则返回null
     */
    public static String format(Date date, String pattern) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        return sdf.format(date);
    }
} 