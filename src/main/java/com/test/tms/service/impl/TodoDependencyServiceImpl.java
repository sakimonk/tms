package com.test.tms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.test.tms.entity.TodoDependency;
import com.test.tms.mapper.TodoDependencyMapper;
import com.test.tms.service.TodoBlockingDepCountHelper;
import com.test.tms.service.TodoDependencyService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;

@Service
public class TodoDependencyServiceImpl extends ServiceImpl<TodoDependencyMapper, TodoDependency>
        implements TodoDependencyService {

    private final TodoBlockingDepCountHelper blockingDepCountHelper;

    public TodoDependencyServiceImpl(TodoBlockingDepCountHelper blockingDepCountHelper) {
        this.blockingDepCountHelper = blockingDepCountHelper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean save(TodoDependency entity) {
        boolean ok = super.save(entity);
        if (ok) {
            blockingDepCountHelper.onDependencyEdgeAdded(entity.getTodoId(), entity.getDependsOnId());
        }
        return ok;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeById(Serializable id) {
        TodoDependency dep = getBaseMapper().selectById(id);
        if (dep == null) {
            return false;
        }
        boolean ok = super.removeById(id);
        if (ok) {
            blockingDepCountHelper.onDependencyEdgeRemoved(dep.getTodoId(), dep.getDependsOnId());
        }
        return ok;
    }
}
