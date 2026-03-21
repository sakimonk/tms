package com.test.tms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.test.tms.constants.RecurrenceType;
import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import com.test.tms.entity.Todo;
import com.test.tms.entity.TodoDependency;
import com.test.tms.entity.TodoRecurrence;
import com.test.tms.mapper.TodoDependencyMapper;
import com.test.tms.mapper.TodoMapper;
import com.test.tms.mapper.TodoRecurrenceMapper;
import com.test.tms.model.dto.TodoCreateRequest;
import com.test.tms.model.dto.TodoUpdateRequest;
import com.test.tms.service.TodoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@Validated
public class TodoServiceImpl extends ServiceImpl<TodoMapper, Todo> implements TodoService {

    private TodoDependencyMapper todoDependencyMapper;
    private TodoRecurrenceMapper todoRecurrenceMapper;

    @Autowired
    public void setTodoDependencyMapper(TodoDependencyMapper todoDependencyMapper) {
        this.todoDependencyMapper = todoDependencyMapper;
    }

    @Autowired
    public void setTodoRecurrenceMapper(TodoRecurrenceMapper todoRecurrenceMapper) {
        this.todoRecurrenceMapper = todoRecurrenceMapper;
    }

    @Override
    @Transactional
    public Todo createTodo(
            @NotNull @Valid TodoCreateRequest request
    ) {
        validateRecurrenceForCreate(request);

        LocalDateTime now = LocalDateTime.now();
        Long by = request.getCreatedBy() != null ? request.getCreatedBy() : request.getUpdatedBy();

        TodoRecurrence recurrenceRow = null;
        String seriesId = null;
        if (Boolean.TRUE.equals(request.getIsRecurring())) {
            recurrenceRow = new TodoRecurrence();
            recurrenceRow.setRecurrenceType(request.getRecurrenceType());
            recurrenceRow.setRecurrenceInterval(request.getRecurrenceInterval() != null ? request.getRecurrenceInterval() : 1);
            recurrenceRow.setRecurrenceCron(request.getRecurrenceCron());
            recurrenceRow.setRootTodoId(null);
            recurrenceRow.setCreatedAt(now);
            recurrenceRow.setUpdatedAt(now);
            recurrenceRow.setCreatedBy(by);
            recurrenceRow.setUpdatedBy(by);
            int ins = todoRecurrenceMapper.insert(recurrenceRow);
            if (ins <= 0 || recurrenceRow.getId() == null) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create recurrence rule");
            }
            seriesId = UUID.randomUUID().toString();
        }

        Todo todo = new Todo();
        todo.setUserId(request.getUserId());
        todo.setName(request.getName());
        todo.setDescription(request.getDescription());
        todo.setDueDate(request.getDueDate());
        todo.setStatus(request.getStatus());
        todo.setPriority(request.getPriority());

        todo.setSeriesId(seriesId);
        todo.setParentId(null);
        todo.setRecurrenceId(recurrenceRow != null ? recurrenceRow.getId() : null);

        todo.setDeleted(false);
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        todo.setCreatedBy(by);
        todo.setUpdatedBy(by);

        boolean saved = this.save(todo);
        if (!saved) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create todo");
        }

        if (recurrenceRow != null && recurrenceRow.getRootTodoId() == null) {
            recurrenceRow.setRootTodoId(todo.getId());
            recurrenceRow.setUpdatedAt(LocalDateTime.now());
            recurrenceRow.setUpdatedBy(by);
            todoRecurrenceMapper.updateById(recurrenceRow);
        }

        if (TodoStatus.IN_PROGRESS.equals(todo.getStatus())) {
            validateDependenciesCompleted(todo.getId());
        }
        if (TodoStatus.COMPLETED.equals(todo.getStatus()) && todo.getRecurrenceId() != null) {
            createNextOccurrence(todo, by);
        }
        return todo;
    }

    @Override
    public Todo getTodo(
            @NotNull @Min(1) Long id
    ) {
        LambdaQueryWrapper<Todo> qw = new LambdaQueryWrapper<>();
        qw.eq(Todo::getId, id).eq(Todo::isDeleted, false);
        Todo todo = this.getBaseMapper().selectOne(qw);
        if (todo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found");
        }
        return todo;
    }

    @Override
    public IPage<Todo> listTodos(
            @Min(1) long pageNum,
            @Min(1) long pageSize,
            TodoStatus status,
            TodoPriority priority,
            LocalDateTime dueFrom,
            LocalDateTime dueTo,
            String sortBy,
            String sortDir
    ) {
        LambdaQueryWrapper<Todo> qw = new LambdaQueryWrapper<>();
        qw.eq(Todo::isDeleted, false);

        if (status != null) {
            qw.eq(Todo::getStatus, status);
        }
        if (priority != null) {
            qw.eq(Todo::getPriority, priority);
        }
        if (dueFrom != null) {
            qw.ge(Todo::getDueDate, dueFrom);
        }
        if (dueTo != null) {
            qw.le(Todo::getDueDate, dueTo);
        }

        applyTodoListOrder(qw, sortBy, sortDir);

        Page<Todo> page = new Page<>(pageNum, pageSize);
        return this.getBaseMapper().selectPage(page, qw);
    }

    @Override
    @Transactional
    public void updateTodo(
            @NotNull @Min(1) Long id,
            @NotNull @Valid TodoUpdateRequest request
    ) {
        LambdaQueryWrapper<Todo> qw = new LambdaQueryWrapper<>();
        qw.eq(Todo::getId, id).eq(Todo::isDeleted, false);
        Todo existing = this.getBaseMapper().selectOne(qw);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found");
        }

        TodoStatus oldStatus = existing.getStatus();
        boolean statusWillChange = request.getStatus() != null;

        if (request.getName() != null) {
            existing.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existing.setDescription(request.getDescription());
        }
        if (request.getDueDate() != null) {
            existing.setDueDate(request.getDueDate());
        }
        if (request.getPriority() != null) {
            existing.setPriority(request.getPriority());
        }

        applyRecurrenceUpdates(existing, request);

        Long updatedBy = request.getUpdatedBy();
        LocalDateTime now = LocalDateTime.now();
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(updatedBy);

        if (statusWillChange) {
            TodoStatus newStatus = request.getStatus();
            boolean changed = !Objects.equals(oldStatus, newStatus);
            existing.setStatus(newStatus);

            if (changed && newStatus == TodoStatus.IN_PROGRESS) {
                validateDependenciesCompleted(existing.getId());
            }
            if (changed && newStatus == TodoStatus.COMPLETED && existing.getRecurrenceId() != null) {
                this.updateById(existing);
                createNextOccurrence(existing, updatedBy);
                return;
            }
        }

        this.updateById(existing);
    }

    /**
     * 创建请求中的循环相关字段校验（替代原类级 Bean 校验）。
     */
    private void validateRecurrenceForCreate(TodoCreateRequest request) {
        Integer interval = request.getRecurrenceInterval();
        if (interval != null && interval < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceInterval must be >= 1");
        }
        if (Boolean.TRUE.equals(request.getIsRecurring())) {
            if (request.getRecurrenceType() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceType is required when isRecurring is true");
            }
        }
        RecurrenceType type = request.getRecurrenceType();
        if (type != null && type == RecurrenceType.CUSTOM) {
            String cron = request.getRecurrenceCron();
            if (cron == null || cron.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceCron is required when recurrenceType is CUSTOM");
            }
        }
    }

    /**
     * 更新请求中循环相关字段的校验（与创建规则一致，并合并库中已有规则判断 CUSTOM+cron）。
     */
    private void validateRecurrencePatchForUpdate(Todo existing, TodoUpdateRequest request) {
        boolean touched = request.getRecurrenceType() != null
                || request.getRecurrenceInterval() != null
                || request.getRecurrenceCron() != null
                || request.getIsRecurring() != null;
        if (!touched) {
            return;
        }
        if (request.getRecurrenceInterval() != null && request.getRecurrenceInterval() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceInterval must be >= 1");
        }
        if (Boolean.TRUE.equals(request.getIsRecurring()) && existing.getRecurrenceId() == null) {
            if (request.getRecurrenceType() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceType is required when isRecurring is true");
            }
        }
        if (existing.getRecurrenceId() != null) {
            TodoRecurrence rule = loadRecurrence(existing.getRecurrenceId());
            RecurrenceType effectiveType = request.getRecurrenceType() != null
                    ? request.getRecurrenceType()
                    : rule.getRecurrenceType();
            String effectiveCron = request.getRecurrenceCron() != null
                    ? request.getRecurrenceCron()
                    : rule.getRecurrenceCron();
            if (effectiveType == RecurrenceType.CUSTOM
                    && (effectiveCron == null || effectiveCron.isBlank())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceCron is required when recurrenceType is CUSTOM");
            }
        } else if (request.getRecurrenceType() == RecurrenceType.CUSTOM) {
            String cron = request.getRecurrenceCron();
            if (cron == null || cron.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceCron is required when recurrenceType is CUSTOM");
            }
        }
    }

    /**
     * 更新与实例关联的循环规则；不支持通过 PATCH 将非循环改为循环（避免歧义）。
     */
    private void applyRecurrenceUpdates(Todo existing, TodoUpdateRequest request) {
        validateRecurrencePatchForUpdate(existing, request);

        boolean touchedRule = request.getRecurrenceType() != null
                || request.getRecurrenceInterval() != null
                || request.getRecurrenceCron() != null
                || request.getIsRecurring() != null;

        if (!touchedRule) {
            return;
        }

        if (existing.getRecurrenceId() == null) {
            if (Boolean.TRUE.equals(request.getIsRecurring())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Cannot enable recurrence on an existing non-recurring todo via update; create a new recurring todo instead"
                );
            }
            if (request.getRecurrenceType() != null
                    || request.getRecurrenceInterval() != null
                    || request.getRecurrenceCron() != null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "This todo has no recurrence rule; recurrence fields cannot be updated"
                );
            }
            return;
        }

        if (Boolean.FALSE.equals(request.getIsRecurring())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cannot disable recurrence on an existing recurring todo via update"
            );
        }

        TodoRecurrence rule = loadRecurrence(existing.getRecurrenceId());
        if (request.getRecurrenceType() != null) {
            rule.setRecurrenceType(request.getRecurrenceType());
        }
        if (request.getRecurrenceInterval() != null) {
            rule.setRecurrenceInterval(request.getRecurrenceInterval());
        }
        if (request.getRecurrenceCron() != null) {
            rule.setRecurrenceCron(request.getRecurrenceCron());
        }
        rule.setUpdatedAt(LocalDateTime.now());
        rule.setUpdatedBy(request.getUpdatedBy());
        todoRecurrenceMapper.updateById(rule);
    }

    private TodoRecurrence loadRecurrence(Long recurrenceId) {
        TodoRecurrence rule = todoRecurrenceMapper.selectById(recurrenceId);
        if (rule == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recurrence rule not found");
        }
        return rule;
    }

    @Override
    @Transactional
    public void softDeleteTodo(
            @NotNull @Min(1) Long id,
            Long updatedBy
    ) {
        Todo existing = getTodo(id);
        LocalDateTime now = LocalDateTime.now();

        existing.setDeleted(true);
        existing.setStatus(TodoStatus.ARCHIVED);
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(updatedBy);

        boolean updated = this.updateById(existing);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete todo");
        }
    }

    private void validateDependenciesCompleted(Long todoId) {
        LambdaQueryWrapper<TodoDependency> depQw = new LambdaQueryWrapper<>();
        depQw.eq(TodoDependency::getTodoId, todoId);
        List<TodoDependency> dependencies = todoDependencyMapper.selectList(depQw);
        if (dependencies == null || dependencies.isEmpty()) {
            return;
        }

        List<Long> dependsOnIds = dependencies.stream()
                .map(TodoDependency::getDependsOnId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (dependsOnIds.isEmpty()) {
            return;
        }

        LambdaQueryWrapper<Todo> blockedQw = new LambdaQueryWrapper<>();
        blockedQw.in(Todo::getId, dependsOnIds)
                .eq(Todo::isDeleted, false)
                .ne(Todo::getStatus, TodoStatus.COMPLETED);

        long blockedCount = this.getBaseMapper().selectCount(blockedQw);
        if (blockedCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move to IN_PROGRESS: dependencies not completed");
        }
    }

    private void createNextOccurrence(Todo completedTodo, Long by) {
        if (completedTodo == null || completedTodo.getDueDate() == null) {
            return;
        }
        if (completedTodo.getRecurrenceId() == null) {
            return;
        }

        TodoRecurrence rule = loadRecurrence(completedTodo.getRecurrenceId());
        LocalDateTime nextDueDate = computeNextDueDate(rule, completedTodo.getDueDate());

        LocalDateTime now = LocalDateTime.now();
        Todo next = new Todo();
        next.setUserId(completedTodo.getUserId());
        next.setName(completedTodo.getName());
        next.setDescription(completedTodo.getDescription());
        next.setDueDate(nextDueDate);

        next.setStatus(TodoStatus.NOT_STARTED);
        next.setPriority(completedTodo.getPriority());

        next.setSeriesId(completedTodo.getSeriesId());
        next.setParentId(completedTodo.getId());
        next.setRecurrenceId(completedTodo.getRecurrenceId());

        next.setDeleted(false);
        next.setCreatedAt(now);
        next.setUpdatedAt(now);
        next.setCreatedBy(by);
        next.setUpdatedBy(by);

        this.save(next);
        copyDependenciesToNewTodo(completedTodo.getId(), next.getId(), by, now);
    }

    private void copyDependenciesToNewTodo(Long fromTodoId, Long toTodoId, Long by, LocalDateTime now) {
        LambdaQueryWrapper<TodoDependency> depQw = new LambdaQueryWrapper<>();
        depQw.eq(TodoDependency::getTodoId, fromTodoId);
        List<TodoDependency> list = todoDependencyMapper.selectList(depQw);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (TodoDependency d : list) {
            TodoDependency nd = new TodoDependency();
            nd.setTodoId(toTodoId);
            nd.setDependsOnId(d.getDependsOnId());
            nd.setCreatedAt(now);
            nd.setUpdatedAt(now);
            nd.setCreatedBy(by);
            nd.setUpdatedBy(by);
            todoDependencyMapper.insert(nd);
        }
    }

    private LocalDateTime computeNextDueDate(TodoRecurrence rule, LocalDateTime dueDate) {
        RecurrenceType recurrenceType = rule.getRecurrenceType();
        if (recurrenceType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceType is missing on rule");
        }
        int interval = rule.getRecurrenceInterval() == null ? 1 : rule.getRecurrenceInterval();

        return switch (recurrenceType) {
            case DAILY -> dueDate.plusDays(interval);
            case WEEKLY -> dueDate.plusWeeks(interval);
            case MONTHLY -> dueDate.plusMonths(interval);
            case CUSTOM -> {
                if (rule.getRecurrenceCron() == null || rule.getRecurrenceCron().isBlank()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceCron is required for CUSTOM");
                }
                CronExpression cronExpression = CronExpression.parse(rule.getRecurrenceCron());
                ZonedDateTime base = ZonedDateTime.of(dueDate, ZoneId.systemDefault());
                ZonedDateTime next = cronExpression.next(base);
                if (next == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot compute next dueDate from cron");
                }
                yield next.toLocalDateTime();
            }
        };
    }

    private void applyTodoListOrder(LambdaQueryWrapper<Todo> qw, String sortBy, String sortDir) {
        String col = normalizeSortColumn(sortBy);
        boolean desc = "desc".equalsIgnoreCase(sortDir);
        switch (col) {
            case "due_date" -> {
                if (desc) {
                    qw.orderByDesc(Todo::getDueDate);
                } else {
                    qw.orderByAsc(Todo::getDueDate);
                }
            }
            case "priority" -> {
                if (desc) {
                    qw.orderByDesc(Todo::getPriority);
                } else {
                    qw.orderByAsc(Todo::getPriority);
                }
            }
            case "status" -> {
                if (desc) {
                    qw.orderByDesc(Todo::getStatus);
                } else {
                    qw.orderByAsc(Todo::getStatus);
                }
            }
            case "name" -> {
                if (desc) {
                    qw.orderByDesc(Todo::getName);
                } else {
                    qw.orderByAsc(Todo::getName);
                }
            }
            default -> {
                if (desc) {
                    qw.orderByDesc(Todo::getDueDate);
                } else {
                    qw.orderByAsc(Todo::getDueDate);
                }
            }
        }
    }

    private String normalizeSortColumn(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "due_date";
        }
        String s = sortBy.trim().toLowerCase(Locale.ROOT);
        if ("due_date".equals(s) || "duedate".equals(s)) {
            return "due_date";
        }
        if ("priority".equals(s)) {
            return "priority";
        }
        if ("status".equals(s)) {
            return "status";
        }
        if ("name".equals(s) || "todo_name".equals(s)) {
            return "name";
        }
        return sortBy;
    }
}
