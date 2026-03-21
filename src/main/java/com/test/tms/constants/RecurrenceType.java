package com.test.tms.constants;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * 循环规则类型，与表字段 {@code tms_todo_recurrence.recurrence_type} 存储值一致。
 */
public enum RecurrenceType {
    DAILY("DAILY"),
    WEEKLY("WEEKLY"),
    MONTHLY("MONTHLY"),
    CUSTOM("CUSTOM");

    @EnumValue
    private final String code;

    RecurrenceType(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
