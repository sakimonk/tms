package com.test.tms.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.test.tms.constants.RecurrenceType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 循环任务规则，对应表 {@code tms_todo_recurrence}。
 * <p>多条 {@link Todo} 实例可共享同一条规则（相同 {@code recurrence_id}）。</p>
 */
@Data
@TableName("tms_todo_recurrence")
public class TodoRecurrence {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 循环类型：按天/周/月/自定义 Cron */
    @TableField("recurrence_type")
    private RecurrenceType recurrenceType;

    /** 间隔（例如每 2 天则为 2） */
    @TableField("recurrence_interval")
    private Integer recurrenceInterval;

    /** {@link RecurrenceType#CUSTOM} 时使用的 Cron 表达式 */
    @TableField("recurrence_cron")
    private String recurrenceCron;

    /** 该系列首个实例的 todo id，便于溯源 */
    @TableField("root_todo_id")
    private Long rootTodoId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("updated_by")
    private Long updatedBy;

    /**
     * 逻辑删除：{@code 0} 未删，{@code 1} 已删。
     */
    @TableLogic(value = "0", delval = "1")
    @TableField("deleted")
    private Integer deleted;
}
