package com.test.tms.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("tms_todo_dependency")
public class TodoDependency {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("todo_id")
    private Long todoId;

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

