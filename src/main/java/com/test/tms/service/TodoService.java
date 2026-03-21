package com.test.tms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import com.test.tms.entity.Todo;
import com.test.tms.model.dto.TodoCreateRequest;
import com.test.tms.model.dto.TodoUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public interface TodoService extends IService<Todo> {

    Todo createTodo(@NotNull @Valid TodoCreateRequest request);

    Todo getTodo(@NotNull @Min(1) Long id);

    IPage<Todo> listTodos(
            @Min(1) long pageNum,
            @Min(1) long pageSize,
            TodoStatus status,
            TodoPriority priority,
            LocalDateTime dueFrom,
            LocalDateTime dueTo,
            String sortBy,
            String sortDir
    );

    void updateTodo(@NotNull @Min(1) Long id, @NotNull @Valid TodoUpdateRequest request);

    void softDeleteTodo(@NotNull @Min(1) Long id, Long updatedBy);
}
