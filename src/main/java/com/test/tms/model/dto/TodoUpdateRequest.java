package com.test.tms.model.dto;

import com.test.tms.constants.RecurrenceType;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 更新 TODO 的请求体；未出现的字段表示不修改。
 * <p>循环相关交叉规则由 {@code TodoService#updateTodo} / 内部规则更新逻辑校验。</p>
 */
@Data
public class TodoUpdateRequest {

    /** 标题 */
    @Size(max = 200, message = "name length must be <= 200")
    private String name;

    /** 描述 */
    @Size(max = 2000, message = "description length must be <= 2000")
    private String description;

    /** 截止时间 */
    private LocalDateTime dueDate;

    /** 状态 */
    private TodoStatus status;

    /** 优先级 */
    private TodoPriority priority;

    /** 是否循环（仅部分场景允许修改，见服务层说明） */
    private Boolean isRecurring;

    /** 循环类型 */
    private RecurrenceType recurrenceType;

    /** 循环间隔 */
    @Min(value = 1, message = "recurrenceInterval must be >= 1")
    private Integer recurrenceInterval;

    /** CUSTOM 类型下的 Cron */
    @Size(max = 100, message = "recurrenceCron length must be <= 100")
    private String recurrenceCron;

    /** 更新人 id（审计，可空） */
    private Long updatedBy;
}
