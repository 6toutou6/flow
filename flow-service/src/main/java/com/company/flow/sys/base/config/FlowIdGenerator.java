package com.company.flow.sys.base.config;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.incrementer.IdentifierGenerator;
import com.company.flow.sys.base.util.UUIDUtil;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 全局主键生成器：按实体对应表名生成 32 位主键（表名缩写 + 时间戳 + 随机数）。
 * entity 主键使用 @TableId(type = IdType.ASSIGN_UUID)，插入时主键为空自动调用本生成器。
 */
@Component
public class FlowIdGenerator implements IdentifierGenerator {

    /** 表名 → 主键前缀（与数据库迁移约定一致） */
    private static final Map<String, String> PREFIX = new ConcurrentHashMap<>();

    static {
        PREFIX.put("aut_user", "AU");
        PREFIX.put("flow_template", "FLOWT");
        PREFIX.put("flow_template_node", "FLOWTN");
        PREFIX.put("flow_template_field", "FLOWTF");
        PREFIX.put("flow_template_version", "FLOWTV");
        PREFIX.put("flow_task", "FLOWTASK");
        PREFIX.put("flow_task_dispatch", "FLOWTD");
        PREFIX.put("flow_task_dispatch_node", "FLOWTDN");
        PREFIX.put("flow_task_node", "FLOWTASKN");
        PREFIX.put("flow_task_member", "FLOWTM");
        PREFIX.put("flow_task_log", "FLOWTL");
        PREFIX.put("flow_task_user", "FLOWTU");
        PREFIX.put("flow_form_record", "FLOWFR");
        PREFIX.put("flow_form_data", "FLOWFD");
        PREFIX.put("flow_dispatch", "FLOWD");
        PREFIX.put("flow_dispatch_config", "FLOWDC");
        PREFIX.put("flow_dispatch_config_template", "FLOWDCT");
        PREFIX.put("flow_attachment", "FLOWAT");
    }

    @Override
    public Number nextId(Object entity) {
        return null;
    }

    @Override
    public String nextUUID(Object entity) {
        String table = resolveTable(entity);
        return UUIDUtil.generateId(PREFIX.getOrDefault(table, "FLOW"));
    }

    private String resolveTable(Object entity) {
        if (entity == null) {
            return "FLOW";
        }
        TableName tn = entity.getClass().getAnnotation(TableName.class);
        return tn == null ? "FLOW" : tn.value();
    }
}
