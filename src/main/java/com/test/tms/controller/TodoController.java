package com.test.tms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import com.test.tms.entity.Todo;
import com.test.tms.model.dto.TodoCreateRequest;
import com.test.tms.model.dto.TodoUpdateRequest;
import com.test.tms.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    public Todo create(@Valid @NotNull @RequestBody TodoCreateRequest request) {
        return todoService.createTodo(request);
    }

    @GetMapping("/{id}")
    public Todo getById(@PathVariable("id") @NotNull @Min(1) Long id) {
        return todoService.getTodo(id);
    }

    @GetMapping
    public IPage<Todo> list(
            @RequestParam(defaultValue = "1") @Min(1) long pageNum,
            @RequestParam(defaultValue = "20") @Min(1) long pageSize,
            @RequestParam(required = false) TodoStatus status,
            @RequestParam(required = false) TodoPriority priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueTo,
            @RequestParam(required = false, defaultValue = "due_date") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {
        return todoService.listTodos(pageNum, pageSize, status, priority, dueFrom, dueTo, sortBy, sortDir);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable("id") @NotNull @Min(1) Long id,
            @Valid @NotNull @RequestBody TodoUpdateRequest request
    ) {
        todoService.updateTodo(id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("id") @NotNull @Min(1) Long id,
            @RequestParam(required = false) Long updatedBy
    ) {
        todoService.softDeleteTodo(id, updatedBy);
        return ResponseEntity.noContent().build();
    }
}
