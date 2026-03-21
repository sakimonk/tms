package com.test.tms.model.dto;

import com.test.tms.constants.RecurrenceType;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 创建 TODO 的请求体。
 * <p>循环相关交叉规则由 {@code TodoService#createTodo} 内校验。</p>
 */
@Data
public class TodoCreateRequest {

    /** 待办归属用户 id */
    @NotNull(message = "userId is required")
    private Long userId;

    /** 标题 */
    @NotBlank(message = "name is required")
    @Size(max = 200, message = "name length must be <= 200")
    private String name;

    /** 描述（可空） */
    private String description;

    /** 截止时间 */
    @NotNull(message = "dueDate is required")
    private LocalDateTime dueDate;

    /** 初始状态 */
    @NotNull(message = "status is required")
    private TodoStatus status;

    /** 优先级 */
    @NotNull(message = "priority is required")
    private TodoPriority priority;

    /** 是否循环任务；为 {@code true} 时需配合 {@link #recurrenceType} 等字段 */
    @NotNull(message = "isRecurring is required")
    private Boolean isRecurring;

    /** 循环类型；{@link #isRecurring} 为 true 时必填 */
    private RecurrenceType recurrenceType;

    /** 循环间隔，≥1 */
    @Min(value = 1, message = "recurrenceInterval must be >= 1")
    private Integer recurrenceInterval;

    /** {@link RecurrenceType#CUSTOM} 时必填，用于计算下一次截止时间 */
    @Size(max = 100, message = "recurrenceCron length must be <= 100")
    private String recurrenceCron;

    /** 创建人 id（审计，可空） */
    private Long createdBy;

    /** 更新人 id（审计，可空） */
    private Long updatedBy;
}
