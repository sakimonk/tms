package com.test.tms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.test.tms.entity.Todo;
import com.test.tms.entity.TodoDependency;
import com.test.tms.mapper.TodoDependencyMapper;
import com.test.tms.mapper.TodoMapper;
import com.test.tms.model.dto.TodoCreateRequest;
import com.test.tms.model.dto.TodoUpdateRequest;
import com.test.tms.service.TodoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class TodoServiceImpl extends ServiceImpl<TodoMapper, Todo> implements TodoService {

    private static final String STATUS_NOT_STARTED = "NOT_STARTED";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";
    private static final String STATUS_ARCHIVED = "ARCHIVED";

    private static final String PRIORITY_LOW = "LOW";
    private static final String PRIORITY_MEDIUM = "MEDIUM";
    private static final String PRIORITY_HIGH = "HIGH";

    private static final String RECURRENCE_DAILY = "DAILY";
    private static final String RECURRENCE_WEEKLY = "WEEKLY";
    private static final String RECURRENCE_MONTHLY = "MONTHLY";
    private static final String RECURRENCE_CUSTOM = "CUSTOM";

    private TodoDependencyMapper todoDependencyMapper;
    @Autowired
    public void setTodoDependencyMapper(TodoDependencyMapper todoDependencyMapper) {
        this.todoDependencyMapper = todoDependencyMapper;
    }

    @Override
    @Transactional
    public Todo createTodo(Long orgId, TodoCreateRequest request) {
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId is required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }
        if (request.getUserId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "userId is required");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name is required");
        }
        if (request.getDueDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate is required");
        }
        if (request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        if (request.getPriority() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priority is required");
        }

        LocalDateTime now = LocalDateTime.now();
        Long by = request.getCreatedBy() != null ? request.getCreatedBy() : request.getUpdatedBy();

        Todo todo = new Todo();
        todo.setOrgId(orgId);
        todo.setUserId(request.getUserId());
        todo.setName(request.getName());
        todo.setDescription(request.getDescription());
        todo.setDueDate(request.getDueDate());
        todo.setStatus(normalizeStatus(request.getStatus()));
        todo.setPriority(normalizePriority(request.getPriority()));

        todo.setIsRecurring(Boolean.TRUE.equals(request.getIsRecurring()));
        todo.setRecurrenceType(normalizeRecurrenceType(request.getRecurrenceType()));
        todo.setRecurrenceInterval(request.getRecurrenceInterval() != null ? request.getRecurrenceInterval() : 1);
        todo.setRecurrenceCron(request.getRecurrenceCron());

        todo.setDeletedAt(null);
        todo.setCreatedAt(now);
        todo.setUpdatedAt(now);
        todo.setCreatedBy(by);
        todo.setUpdatedBy(by);

        boolean saved = this.save(todo);
        if (!saved) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create todo");
        }

        if (STATUS_IN_PROGRESS.equals(todo.getStatus())) {
            validateDependenciesCompleted(todo.getId());
        }
        if (STATUS_COMPLETED.equals(todo.getStatus()) && Boolean.TRUE.equals(todo.getIsRecurring())) {
            createNextOccurrence(todo, by);
        }
        return todo;
    }

    @Override
    public Todo getTodo(Long orgId, Long id) {
        if (orgId == null || id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId and id are required");
        }

        QueryWrapper<Todo> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("org_id", orgId).isNull("deleted_at");
        Todo todo = this.getBaseMapper().selectOne(qw);
        if (todo == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found");
        }
        return todo;
    }

    @Override
    public IPage<Todo> listTodos(
            Long orgId,
            long pageNum,
            long pageSize,
            String status,
            String priority,
            LocalDateTime dueFrom,
            LocalDateTime dueTo,
            String sortBy,
            String sortDir
    ) {
        if (orgId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId is required");
        }

        QueryWrapper<Todo> qw = new QueryWrapper<>();
        qw.eq("org_id", orgId).isNull("deleted_at");

        if (status != null && !status.isBlank()) {
            qw.eq("status", normalizeStatus(status));
        }
        if (priority != null && !priority.isBlank()) {
            qw.eq("priority", normalizePriority(priority));
        }
        if (dueFrom != null) {
            qw.ge("due_date", dueFrom);
        }
        if (dueTo != null) {
            qw.le("due_date", dueTo);
        }

        String sortColumn = normalizeSortColumn(sortBy);
        boolean desc = "desc".equalsIgnoreCase(sortDir);
        if (desc) {
            qw.orderByDesc(sortColumn);
        } else {
            qw.orderByAsc(sortColumn);
        }

        Page<Todo> page = new Page<>(pageNum, pageSize);
        return this.getBaseMapper().selectPage(page, qw);
    }

    @Override
    @Transactional
    public void updateTodo(Long orgId, Long id, TodoUpdateRequest request) {
        if (orgId == null || id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "orgId and id are required");
        }
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "request body is required");
        }

        QueryWrapper<Todo> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("org_id", orgId).isNull("deleted_at");
        Todo existing = this.getBaseMapper().selectOne(qw);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo not found");
        }

        String oldStatus = existing.getStatus();
        String oldNormalizedStatus = oldStatus == null ? null : oldStatus;
        boolean statusWillChange = request.getStatus() != null;

        // update basic fields
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
            existing.setPriority(normalizePriority(request.getPriority()));
        }
        if (request.getIsRecurring() != null) {
            existing.setIsRecurring(request.getIsRecurring());
        }
        if (request.getRecurrenceType() != null) {
            existing.setRecurrenceType(normalizeRecurrenceType(request.getRecurrenceType()));
        }
        if (request.getRecurrenceInterval() != null) {
            existing.setRecurrenceInterval(request.getRecurrenceInterval());
        }
        if (request.getRecurrenceCron() != null) {
            existing.setRecurrenceCron(request.getRecurrenceCron());
        }

        Long updatedBy = request.getUpdatedBy();
        LocalDateTime now = LocalDateTime.now();
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(updatedBy);

        if (statusWillChange) {
            String newStatus = normalizeStatus(request.getStatus());
            boolean changed = !Objects.equals(oldNormalizedStatus, newStatus);
            existing.setStatus(newStatus);

            if (changed && STATUS_IN_PROGRESS.equals(newStatus)) {
                validateDependenciesCompleted(existing.getId());
            }
            if (changed && STATUS_COMPLETED.equals(newStatus) && Boolean.TRUE.equals(existing.getIsRecurring())) {
                // 先更新当前任务，再生成下一次（nextDueDate 基于更新后的 dueDate/周期配置）
                this.updateById(existing);
                createNextOccurrence(existing, updatedBy);
                return;
            }
        }

        this.updateById(existing);
    }

    @Override
    @Transactional
    public void softDeleteTodo(Long orgId, Long id, Long updatedBy) {
        Todo existing = getTodo(orgId, id);
        LocalDateTime now = LocalDateTime.now();

        existing.setDeletedAt(now);
        existing.setStatus(STATUS_ARCHIVED);
        existing.setUpdatedAt(now);
        existing.setUpdatedBy(updatedBy);

        boolean updated = this.updateById(existing);
        if (!updated) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete todo");
        }
    }

    private void validateDependenciesCompleted(Long todoId) {
        QueryWrapper<TodoDependency> depQw = new QueryWrapper<>();
        depQw.eq("todo_id", todoId);
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

        QueryWrapper<Todo> blockedQw = new QueryWrapper<>();
        blockedQw.in("id", dependsOnIds)
                .isNull("deleted_at")
                .ne("status", STATUS_COMPLETED);

        long blockedCount = this.getBaseMapper().selectCount(blockedQw);
        if (blockedCount > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move to IN_PROGRESS: dependencies not completed");
        }
    }

    private void createNextOccurrence(Todo completedTodo, Long by) {
        if (completedTodo == null || completedTodo.getDueDate() == null) {
            return;
        }
        if (!Boolean.TRUE.equals(completedTodo.getIsRecurring())) {
            return;
        }

        LocalDateTime nextDueDate = computeNextDueDate(completedTodo);

        LocalDateTime now = LocalDateTime.now();
        Todo next = new Todo();
        next.setOrgId(completedTodo.getOrgId());
        next.setUserId(completedTodo.getUserId());
        next.setName(completedTodo.getName());
        next.setDescription(completedTodo.getDescription());
        next.setDueDate(nextDueDate);

        next.setStatus(STATUS_NOT_STARTED);
        next.setPriority(completedTodo.getPriority());

        next.setIsRecurring(completedTodo.getIsRecurring());
        next.setRecurrenceType(completedTodo.getRecurrenceType());
        next.setRecurrenceInterval(completedTodo.getRecurrenceInterval());
        next.setRecurrenceCron(completedTodo.getRecurrenceCron());

        next.setDeletedAt(null);
        next.setCreatedAt(now);
        next.setUpdatedAt(now);
        next.setCreatedBy(by);
        next.setUpdatedBy(by);

        this.save(next);
    }

    private LocalDateTime computeNextDueDate(Todo todo) {
        String recurrenceType = normalizeRecurrenceType(todo.getRecurrenceType());
        int interval = todo.getRecurrenceInterval() == null ? 1 : todo.getRecurrenceInterval();
        LocalDateTime dueDate = todo.getDueDate();

        if (RECURRENCE_DAILY.equals(recurrenceType)) {
            return dueDate.plusDays(interval);
        }
        if (RECURRENCE_WEEKLY.equals(recurrenceType)) {
            return dueDate.plusWeeks(interval);
        }
        if (RECURRENCE_MONTHLY.equals(recurrenceType)) {
            return dueDate.plusMonths(interval);
        }
        if (RECURRENCE_CUSTOM.equals(recurrenceType)) {
            if (todo.getRecurrenceCron() == null || todo.getRecurrenceCron().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceCron is required for CUSTOM");
            }
            CronExpression cronExpression = CronExpression.parse(todo.getRecurrenceCron());
            ZonedDateTime base = ZonedDateTime.of(dueDate, ZoneId.systemDefault());
            ZonedDateTime next = cronExpression.next(base);
            if (next == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot compute next dueDate from cron");
            }
            return next.toLocalDateTime();
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported recurrenceType: " + todo.getRecurrenceType());
    }

    private String normalizeStatus(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status is required");
        }
        String s = raw.trim();
        String upper = s.toUpperCase(Locale.ROOT);
        if ("NOT_STARTED".equals(upper) || "NOT STARTED".equals(upper) || "NOT STARTED".equals(s.toUpperCase(Locale.ROOT)) || "NOT STARTED".equals(upper)) {
            return STATUS_NOT_STARTED;
        }
        if ("IN_PROGRESS".equals(upper) || "IN PROGRESS".equals(upper)) {
            return STATUS_IN_PROGRESS;
        }
        if ("COMPLETED".equals(upper) || "DONE".equals(upper)) {
            return STATUS_COMPLETED;
        }
        if ("ARCHIVED".equals(upper) || "ARCHIVE".equals(upper)) {
            return STATUS_ARCHIVED;
        }
        // "Not Started" / "In Progress" / "Completed" / "Archived"
        if ("Not Started".equalsIgnoreCase(s)) {
            return STATUS_NOT_STARTED;
        }
        if ("In Progress".equalsIgnoreCase(s)) {
            return STATUS_IN_PROGRESS;
        }
        if ("Completed".equalsIgnoreCase(s)) {
            return STATUS_COMPLETED;
        }
        if ("Archived".equalsIgnoreCase(s)) {
            return STATUS_ARCHIVED;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported status: " + raw);
    }

    private String normalizePriority(String raw) {
        if (raw == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priority is required");
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if ("LOW".equals(upper)) {
            return PRIORITY_LOW;
        }
        if ("MEDIUM".equals(upper)) {
            return PRIORITY_MEDIUM;
        }
        if ("HIGH".equals(upper)) {
            return PRIORITY_HIGH;
        }
        // 支持 Low/Medium/High
        if ("Low".equalsIgnoreCase(raw)) {
            return PRIORITY_LOW;
        }
        if ("Medium".equalsIgnoreCase(raw)) {
            return PRIORITY_MEDIUM;
        }
        if ("High".equalsIgnoreCase(raw)) {
            return PRIORITY_HIGH;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported priority: " + raw);
    }

    private String normalizeRecurrenceType(String raw) {
        if (raw == null) {
            return null;
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (RECURRENCE_DAILY.equals(upper)) {
            return RECURRENCE_DAILY;
        }
        if (RECURRENCE_WEEKLY.equals(upper)) {
            return RECURRENCE_WEEKLY;
        }
        if (RECURRENCE_MONTHLY.equals(upper)) {
            return RECURRENCE_MONTHLY;
        }
        if (RECURRENCE_CUSTOM.equals(upper)) {
            return RECURRENCE_CUSTOM;
        }
        return upper;
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
        // 允许直接传 columnName
        return sortBy;
    }
}

