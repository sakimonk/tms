package com.test.tms.model.dto;

import com.test.tms.constants.TodoPriority;
import com.test.tms.constants.TodoStatus;
import com.test.tms.entity.Todo;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

/**
 * TODO 读模型，供接口返回；与持久化实体解耦。
 */
@Data
public class TodoResponse {

    private Long id;
    private Long userId;
    private String name;
    private String description;
    private LocalDateTime dueDate;
    private TodoStatus status;
    private TodoPriority priority;
    private String seriesId;
    private Long parentId;
    private Long recurrenceId;
    private int blockingDepCount;
    private Integer version;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;
    /** 本任务依赖的前置 todo id 列表 */
    private List<Long> dependsOnTodoIds;

    public static TodoResponse fromEntity(Todo t) {
        if (t == null) {
            return null;
        }
        TodoResponse r = new TodoResponse();
        r.setId(t.getId());
        r.setUserId(t.getUserId());
        r.setName(t.getName());
        r.setDescription(t.getDescription());
        r.setDueDate(t.getDueDate());
        r.setStatus(t.getStatus());
        r.setPriority(t.getPriority());
        r.setSeriesId(t.getSeriesId());
        r.setParentId(t.getParentId());
        r.setRecurrenceId(t.getRecurrenceId());
        r.setBlockingDepCount(t.getBlockingDepCount());
        r.setVersion(t.getVersion());
        r.setCreatedAt(t.getCreatedAt());
        r.setCreatedBy(t.getCreatedBy());
        r.setUpdatedAt(t.getUpdatedAt());
        r.setUpdatedBy(t.getUpdatedBy());
        List<Long> deps = t.getDependsOnTodoIds();
        r.setDependsOnTodoIds(deps == null ? Collections.emptyList() : List.copyOf(deps));
        return r;
    }
}
