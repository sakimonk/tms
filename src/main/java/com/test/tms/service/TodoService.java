package com.test.tms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.test.tms.constants.TodoBlockedFilter;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import com.test.tms.entity.Todo;
import com.test.tms.model.dto.TodoBatchStatusRequest;
import com.test.tms.model.dto.TodoCreateRequest;
import com.test.tms.model.dto.TodoResponse;
import com.test.tms.model.dto.TodoUpdateRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public interface TodoService extends IService<Todo> {

    TodoResponse createTodo(@NotNull @Valid TodoCreateRequest request);

    TodoResponse getTodo(@NotNull @Min(1) Long id);

    IPage<TodoResponse> listTodos(
            @Min(1) long pageNum,
            @Min(1) long pageSize,
            TodoStatus status,
            TodoPriority priority,
            LocalDateTime dueFrom,
            LocalDateTime dueTo,
            TodoBlockedFilter blockedFilter,
            String sortBy,
            String sortDir
    );

    void updateTodo(@NotNull @Min(1) Long id, @NotNull @Valid TodoUpdateRequest request);

    /**
     * 软删除 TODO。
     *
     * @param deleteAssociated 为 {@code true} 时沿用完整逻辑：无剩余未删实例时同步软删 {@code TodoRecurrence}；
     *                         为 {@code false} 时仅软删当前实例，不处理关联循环规则。
     */
    void softDeleteTodo(@NotNull @Min(1) Long id, Long updatedBy, boolean deleteAssociated);

    void batchUpdateTodoStatus(@NotNull @Valid TodoBatchStatusRequest request);
}
