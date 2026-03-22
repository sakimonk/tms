package com.test.tms.constants;

/**
 * 列表按「是否被依赖阻塞」过滤。
 * <p>BLOCKED：存在至少一条依赖，其目标待办未软删且未完成。</p>
 * <p>UNBLOCKED：无此类依赖（含无依赖、或依赖目标已软删因而视为不存在）。</p>
 */
public enum TodoBlockedFilter {
    BLOCKED,
    UNBLOCKED
}
