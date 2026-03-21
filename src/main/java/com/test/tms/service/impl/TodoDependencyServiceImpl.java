package com.test.tms.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.test.tms.entity.TodoDependency;
import com.test.tms.mapper.TodoDependencyMapper;
import com.test.tms.service.TodoDependencyService;
import org.springframework.stereotype.Service;

@Service
public class TodoDependencyServiceImpl extends ServiceImpl<TodoDependencyMapper, TodoDependency>
        implements TodoDependencyService {
}

