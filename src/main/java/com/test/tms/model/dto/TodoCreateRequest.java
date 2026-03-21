package com.test.tms.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TodoCreateRequest {

    private Long userId;

    private String name;

    private String description;

    private LocalDateTime dueDate;

    /**
     * 支持：Not Started / In Progress / Completed / Archived 或其大写下划线形式
     */
    private String status;

    /**
     * 支持：Low / Medium / High 或其大写下划线形式
     */
    private String priority;

    private Boolean isRecurring;

    /**
     * 支持：DAILY / WEEKLY / MONTHLY / CUSTOM
     */
    private String recurrenceType;

    private Integer recurrenceInterval;

    /**
     * CUSTOM 使用，用于计算下一次 dueDate
     */
    private String recurrenceCron;

    private Long createdBy;

    private Long updatedBy;
}

