package com.test.tms.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.test.tms.entity.Todo;
import com.test.tms.model.dto.TodoCreateRequest;
import com.test.tms.model.dto.TodoUpdateRequest;

import java.time.LocalDateTime;

public interface TodoService extends IService<Todo> {

    Todo createTodo(Long orgId, TodoCreateRequest request);

    Todo getTodo(Long orgId, Long id);

    IPage<Todo> listTodos(
            Long orgId,
            long pageNum,
            long pageSize,
            String status,
            String priority,
            LocalDateTime dueFrom,
            LocalDateTime dueTo,
            String sortBy,
            String sortDir
    );

    void updateTodo(Long orgId, Long id, TodoUpdateRequest request);

    void softDeleteTodo(Long orgId, Long id, Long updatedBy);
}

