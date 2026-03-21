package com.test.tms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * TODO 依赖关系，对应表 {@code tms_todo_dependency}。
 * <p>表示 {@code todo_id} 所代表的待办依赖于 {@code depends_on_id} 必须先完成等业务约束。</p>
 */
@Data
@TableName("tms_todo_dependency")
public class TodoDependency {

    /** 主键 */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 当前待办 id */
    @TableField("todo_id")
    private Long todoId;

    /** 所依赖的待办 id */
    @TableField("depends_on_id")
    private Long dependsOnId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_by")
    private Long updatedBy;
}
