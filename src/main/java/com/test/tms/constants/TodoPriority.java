package com.test.tms.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * TODO 优先级，与表字段 {@code tms_todo.priority} 存储值一致。
 */
public enum TodoPriority {
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH");

    @EnumValue
    private final String code;

    TodoPriority(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
