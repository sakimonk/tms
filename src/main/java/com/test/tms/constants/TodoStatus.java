package com.test.tms.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * TODO 业务状态，与表字段 {@code tms_todo.status} 存储值一致。
 */
public enum TodoStatus {
    NOT_STARTED("NOT_STARTED"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    ARCHIVED("ARCHIVED");

    @EnumValue
    private final String code;

    TodoStatus(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
