package com.test.tms.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.test.tms.constants.TodoStatus;
import com.test.tms.entity.Todo;
import com.test.tms.entity.TodoDependency;
import com.test.tms.mapper.TodoDependencyMapper;
import com.test.tms.mapper.TodoMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * 维护 {@code tms_todo.blocking_dep_count}：未完成且未软删的依赖目标个数；为 0 表示非阻塞。
 */
@Component
public class TodoBlockingDepCountHelper {

    private final TodoMapper todoMapper;
    private final TodoDependencyMapper todoDependencyMapper;

    public TodoBlockingDepCountHelper(TodoMapper todoMapper, TodoDependencyMapper todoDependencyMapper) {
        this.todoMapper = todoMapper;
        this.todoDependencyMapper = todoDependencyMapper;
    }

    /**
     * 依赖目标是否仍算作「会阻塞下游」（未软删且非已完成）。
     */
    public boolean countsAsBlocking(Todo prerequisite) {
        if (prerequisite == null) {
            return false;
        }
        Integer del = prerequisite.getDeleted();
        boolean notDeleted = del == null || del == 0;
        return notDeleted &&
                (prerequisite.getStatus() != TodoStatus.COMPLETED && prerequisite.getStatus() != TodoStatus.ARCHIVED);
    }

    /**
     * 当前置任务（他人依赖的 todo）的 deleted/status 变化时，对其所有下游的计数做增量修正。
     */
    public void onPrerequisiteSnapshotAndNow(Todo before, Todo after) {
        Objects.requireNonNull(before.getId(), "id");
        Objects.requireNonNull(after.getId(), "id");
        if (!before.getId().equals(after.getId())) {
            throw new IllegalArgumentException("before/after id mismatch");
        }
        boolean wasBlocking = countsAsBlocking(before);
        boolean nowBlocking = countsAsBlocking(after);
        if (wasBlocking == nowBlocking) {
            return;
        }
        int delta = nowBlocking ? 1 : -1;
        applyDeltaToAllDependents(after.getId(), delta);
    }

    /**
     * 新增依赖边 {@code todoId -> dependsOnId} 后调用。
     */
    public void onDependencyEdgeAdded(Long todoId, Long dependsOnId) {
        Todo dep = todoMapper.selectById(dependsOnId);
        if (countsAsBlocking(dep)) {
            incrementBlockingCount(todoId, 1);
        }
    }

    /**
     * 删除依赖边后调用（根据依赖目标当前是否仍算阻塞决定是否减计数）。
     */
    public void onDependencyEdgeRemoved(Long todoId, Long dependsOnId) {
        Todo dep = todoMapper.selectById(dependsOnId);
        if (countsAsBlocking(dep)) {
            decrementBlockingCount(todoId, 1);
        }
    }

    /**
     * 按当前依赖边全量重算某条 todo 的阻塞依赖数（用于批量复制依赖等）。
     */
    public void recalculateBlockingDepCount(Long todoId) {
        LambdaQueryWrapper<TodoDependency> depQw = new LambdaQueryWrapper<>();
        depQw.eq(TodoDependency::getTodoId, todoId);
        List<TodoDependency> deps = todoDependencyMapper.selectList(depQw);
        int count = 0;
        if (deps != null) {
            for (TodoDependency d : deps) {
                Todo t = todoMapper.selectById(d.getDependsOnId());
                if (countsAsBlocking(t)) {
                    count++;
                }
            }
        }
        todoMapper.update(
                null,
                new LambdaUpdateWrapper<Todo>()
                        .set(Todo::getBlockingDepCount, count)
                        .eq(Todo::getId, todoId)
        );
    }

    private void applyDeltaToAllDependents(Long prerequisiteId, int delta) {
        LambdaQueryWrapper<TodoDependency> qw = new LambdaQueryWrapper<>();
        qw.eq(TodoDependency::getDependsOnId, prerequisiteId);
        List<TodoDependency> list = todoDependencyMapper.selectList(qw);
        if (list == null || list.isEmpty()) {
            return;
        }
        for (TodoDependency d : list) {
            if (delta > 0) {
                incrementBlockingCount(d.getTodoId(), delta);
            } else {
                decrementBlockingCount(d.getTodoId(), -delta);
            }
        }
    }

    private void incrementBlockingCount(Long todoId, int delta) {
        if (delta <= 0) {
            return;
        }
        todoMapper.update(
                null,
                new LambdaUpdateWrapper<Todo>()
                        .setSql("blocking_dep_count = blocking_dep_count + " + delta)
                        .eq(Todo::getId, todoId)
        );
    }

    private void decrementBlockingCount(Long todoId, int delta) {
        if (delta <= 0) {
            return;
        }
        todoMapper.update(
                null,
                new LambdaUpdateWrapper<Todo>()
                        .setSql("blocking_dep_count = GREATEST(0, blocking_dep_count - " + delta + ")")
                        .eq(Todo::getId, todoId)
        );
    }
}
