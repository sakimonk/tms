package com.test.tms.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TodoUpdateRequest {

    private String name;

    private String description;

    private LocalDateTime dueDate;

    private String status;

    private String priority;

    private Boolean isRecurring;

    private String recurrenceType;

    private Integer recurrenceInterval;

    private String recurrenceCron;

    private Long updatedBy;
}

