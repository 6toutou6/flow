package com.company.flow.sys.flowtask.expr;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

/**
 * 结构化条件求值：用于条件流转分支与字段显隐/只读联动。
 * 支持任意嵌套的布尔表达式树：
 * <pre>
 * 叶子条件：{"fieldKey":"","op":"","value":""}
 * 分组节点：{"logic":"and|or","children":[叶子|分组, ...]}
 * </pre>
 * op 支持 eq/neq/gt/gte/lt/lte/in/empty/notempty；条件为空恒真（无约束）。
 * 兼容旧结构：{"logic","conds":[...]} 与单条件（fieldKey/op/value 直接放顶层）。
 */
public class ConditionEvaluator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ConditionEvaluator() {
    }

    /** 条件表达式求值（递归树，支持任意嵌套；兼容旧 conds 与单条件结构；条件为空恒真） */
    public static boolean matchesAll(String condJson, Map<String, String> values) {
        if (condJson == null || condJson.trim().isEmpty()) return true;
        try {
            return evalNode(MAPPER.readValue(condJson, Map.class), values);
        } catch (Exception e) {
            return false;
        }
    }

    /** 递归求值节点：分组节点（有 children/conds）或叶子节点（有 fieldKey） */
    private static boolean evalNode(Map<?, ?> node, Map<String, String> values) {
        Object childrenObj = node.get("children");
        if (!(childrenObj instanceof List)) {
            childrenObj = node.get("conds");
        }
        if (childrenObj instanceof List) {
            List<?> children = (List<?>) childrenObj;
            if (children.isEmpty()) return true;
            boolean isOr = "or".equalsIgnoreCase(String.valueOf(node.get("logic")));
            for (Object c : children) {
                if (!(c instanceof Map)) continue;
                boolean hit = evalNode((Map<?, ?>) c, values);
                if (isOr && hit) return true;
                if (!isOr && !hit) return false;
            }
            return !isOr;
        }
        // 叶子节点
        String fieldKey = node.get("fieldKey") == null ? null : String.valueOf(node.get("fieldKey"));
        String op = node.get("op") == null ? null : String.valueOf(node.get("op"));
        String expect = node.get("value") == null ? null : String.valueOf(node.get("value"));
        String actual = values == null ? null : values.get(fieldKey);
        return eval(op, actual, expect);
    }

    private static boolean eval(String op, String actual, String expect) {
        if (op == null) return true;
        switch (op) {
            case "empty":
                return actual == null || actual.trim().isEmpty();
            case "notempty":
                return actual != null && !actual.trim().isEmpty();
            case "eq":
                return actual != null && actual.equals(expect);
            case "neq":
                return actual == null || !actual.equals(expect);
            case "in":
                return actual != null && expect != null && inList(expect, actual);
            case "gt":
                return compare(actual, expect) > 0;
            case "gte":
                return compare(actual, expect) >= 0;
            case "lt":
                return compare(actual, expect) < 0;
            case "lte":
                return compare(actual, expect) <= 0;
            default:
                return true;
        }
    }

    private static boolean inList(String list, String v) {
        for (String s : list.split(",")) {
            if (s.trim().equals(v)) return true;
        }
        return false;
    }

    /** 数值优先比较，非数值按字典序 */
    private static int compare(String a, String b) {
        if (a == null) return -1;
        try {
            return Double.compare(Double.parseDouble(a), Double.parseDouble(b));
        } catch (NumberFormatException e) {
            return a.compareTo(b);
        }
    }
}
