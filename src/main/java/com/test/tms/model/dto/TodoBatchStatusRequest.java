package com.test.tms.model.dto;

import com.test.tms.constants.TodoStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class TodoBatchStatusRequest {

    @NotEmpty(message = "ids must not be empty")
    @Size(max = 500, message = "ids size must be <= 500")
    private List<Long> ids;

    @NotNull(message = "status is required")
    private TodoStatus status;

    private Long updatedBy;
}
