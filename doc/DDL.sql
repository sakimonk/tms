-- tms SleekFlow TODO 项目 DDL
-- 数据模型说明：
-- 1) tms_user：多用户支持（可选认证字段预留）
-- 2) tms_todo_recurrence：循环任务规则表
-- 3) tms_todo：TODO 实例表（deleted=1 表示软删除/归档）
-- 4) tms_todo_dependency：TODO 依赖关系

-- 建议：若你需要重建库表，可手动先执行下方 DROP 语句（取消注释）。
-- DROP TABLE IF EXISTS `tms_todo_dependency`;
-- DROP TABLE IF EXISTS `tms_todo`;
-- DROP TABLE IF EXISTS `tms_todo_recurrence`;
-- DROP TABLE IF EXISTS `tms_user`;

CREATE TABLE IF NOT EXISTS `tms_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  `username` VARCHAR(64) NOT NULL COMMENT '登录名（全局唯一）',
  `password_hash` VARCHAR(255) NULL COMMENT '密码哈希（不存明文）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL COMMENT '创建人用户 id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `updated_by` BIGINT NULL COMMENT '最后更新人用户 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tms_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS `tms_todo_recurrence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '循环规则主键',
  `recurrence_type` ENUM('DAILY','WEEKLY','MONTHLY','CUSTOM') NOT NULL COMMENT '与 RecurrenceType 枚举一致',
  `recurrence_interval` INT NOT NULL DEFAULT 1 COMMENT '循环间隔（例如每 2 天则为 2）',
  `recurrence_cron` VARCHAR(100) NULL COMMENT 'CUSTOM 类型使用的 Cron 表达式',
  `root_todo_id` BIGINT NULL COMMENT '该系列首个 todo id（便于溯源）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL COMMENT '创建人用户 id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `updated_by` BIGINT NULL COMMENT '最后更新人用户 id',
  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0=未删 1=软删',
  PRIMARY KEY (`id`),
  KEY `idx_tms_todo_recurrence_deleted` (`deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='循环任务规则表';

-- 若 `tms_todo_recurrence` 已存在且无 deleted 列，可执行：
-- ALTER TABLE `tms_todo_recurrence` ADD COLUMN `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0=未删 1=软删' AFTER `updated_by`;
-- ALTER TABLE `tms_todo_recurrence` ADD KEY `idx_tms_todo_recurrence_deleted` (`deleted`);

CREATE TABLE IF NOT EXISTS `tms_todo` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'todo 主键',
  `user_id` BIGINT NOT NULL COMMENT '所属用户 id',
  `name` VARCHAR(200) NOT NULL COMMENT '标题',
  `description` TEXT NULL COMMENT '描述',
  `due_date` DATETIME NOT NULL COMMENT '截止时间',
  `status` ENUM('NOT_STARTED','IN_PROGRESS','COMPLETED','ARCHIVED') NOT NULL COMMENT '与 TodoStatus 枚举一致',
  `priority` ENUM('LOW','MEDIUM','HIGH') NOT NULL COMMENT '与 TodoPriority 枚举一致',

  `series_id` VARCHAR(36) NULL COMMENT '同一循环系列 UUID，非循环任务为 NULL',
  `parent_id` BIGINT NULL COMMENT '上一实例 todo id，系列首条为 NULL',
  `recurrence_id` BIGINT NULL COMMENT '关联 tms_todo_recurrence.id，非循环任务为 NULL',

  `deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0=未删 1=软删',

  `blocking_dep_count` INT NOT NULL DEFAULT 0 COMMENT '未完成且未软删的依赖目标个数，0=非阻塞',

  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL COMMENT '创建人用户 id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `updated_by` BIGINT NULL COMMENT '最后更新人用户 id',
  PRIMARY KEY (`id`),
  KEY `idx_tms_todo_user_due` (`user_id`, `due_date`),
  KEY `idx_tms_todo_status` (`status`),
  KEY `idx_tms_todo_priority` (`priority`),
  KEY `idx_tms_todo_deleted` (`deleted`),
  KEY `idx_tms_todo_blocking_dep_count` (`blocking_dep_count`),
  KEY `idx_tms_todo_series` (`series_id`),
  KEY `idx_tms_todo_recurrence` (`recurrence_id`),
  KEY `idx_tms_todo_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='todo 实例表';

CREATE TABLE IF NOT EXISTS `tms_todo_dependency` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '依赖关系主键',
  `todo_id` BIGINT NOT NULL COMMENT '当前 todo id',
  `depends_on_id` BIGINT NOT NULL COMMENT '被依赖的前置 todo id',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `created_by` BIGINT NULL COMMENT '创建人用户 id',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `updated_by` BIGINT NULL COMMENT '最后更新人用户 id',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tms_todo_dependency_pair` (`todo_id`, `depends_on_id`),
  KEY `idx_tms_todo_dependency_todo_id` (`todo_id`),
  KEY `idx_tms_todo_dependency_depends_on_id` (`depends_on_id`),
  CHECK (`todo_id` <> `depends_on_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='todo 依赖关系表';

