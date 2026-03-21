package com.test.tms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TODO 实例，对应表 {@code tms_todo}。
 * <p>周期任务的实际规则见 {@link TodoRecurrence}，本表通过 {@link #recurrenceId} 关联。</p>
 */
@Data
@TableName("tms_todo")
public class Todo {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 所属用户 id */
    @TableField("user_id")
    private Long userId;

    /** 标题 */
    private String name;

    /** 描述 */
    private String description;

    /** 截止时间 */
    @TableField("due_date")
    private LocalDateTime dueDate;

    /** 业务状态 */
    private TodoStatus status;

    /** 优先级 */
    private TodoPriority priority;

    /**
     * 同一循环系列 UUID；非循环任务为 {@code null}。
     */
    @TableField("series_id")
    private String seriesId;

    /**
     * 上一实例 todo id；系列首条为 {@code null}。
     */
    @TableField("parent_id")
    private Long parentId;

    /**
     * 关联 {@code tms_todo_recurrence.id}；非循环为 {@code null}。
     */
    @TableField("recurrence_id")
    private Long recurrenceId;

    /** 软删除标记：{@code true} 表示已删除（归档） */
    @TableField("deleted")
    private boolean deleted;

    /** 创建时间 */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /** 创建人用户 id（可空） */
    @TableField("created_by")
    private Long createdBy;

    /** 最后更新时间 */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /** 最后更新人用户 id（可空） */
    @TableField("updated_by")
    private Long updatedBy;
}
