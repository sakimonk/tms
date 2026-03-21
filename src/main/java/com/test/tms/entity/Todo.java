package com.test.tms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tms_todo")
public class Todo {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("org_id")
    private Long orgId;

    private String name;

    private String description;

    @TableField("due_date")
    private LocalDateTime dueDate;

    /**
     * NOT_STARTED / IN_PROGRESS / COMPLETED / ARCHIVED
     */
    private String status;

    /**
     * LOW / MEDIUM / HIGH
     */
    private String priority;

    @TableField("is_recurring")
    private Boolean isRecurring;

    @TableField("recurrence_type")
    private String recurrenceType;

    @TableField("recurrence_interval")
    private Integer recurrenceInterval;

    @TableField("recurrence_cron")
    private String recurrenceCron;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("updated_by")
    private Long updatedBy;
}

