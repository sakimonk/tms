package com.test.tms.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.test.tms.entity.Todo;
import com.test.tms.model.dto.TodoCreateRequest;
import com.test.tms.model.dto.TodoUpdateRequest;
import com.test.tms.service.TodoService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/orgs/{orgId}/todos")
public class TodoController {

    private final TodoService todoService;

    public TodoController(TodoService todoService) {
        this.todoService = todoService;
    }

    @PostMapping
    public Todo create(@PathVariable("orgId") Long orgId, @RequestBody TodoCreateRequest request) {
        return todoService.createTodo(orgId, request);
    }

    @GetMapping("/{id}")
    public Todo getById(@PathVariable("orgId") Long orgId, @PathVariable("id") Long id) {
        return todoService.getTodo(orgId, id);
    }

    @GetMapping
    public IPage<Todo> list(
            @PathVariable("orgId") Long orgId,
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueTo,
            @RequestParam(required = false, defaultValue = "due_date") String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String sortDir
    ) {
        return todoService.listTodos(orgId, pageNum, pageSize, status, priority, dueFrom, dueTo, sortBy, sortDir);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> update(
            @PathVariable("orgId") Long orgId,
            @PathVariable("id") Long id,
            @RequestBody TodoUpdateRequest request
    ) {
        todoService.updateTodo(orgId, id, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("orgId") Long orgId,
            @PathVariable("id") Long id,
            @RequestParam(required = false) Long updatedBy
    ) {
        todoService.softDeleteTodo(orgId, id, updatedBy);
        return ResponseEntity.noContent().build();
    }
}

