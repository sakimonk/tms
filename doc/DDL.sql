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
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tms_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tms_todo_recurrence` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `recurrence_type` VARCHAR(20) NOT NULL,
  `recurrence_interval` INT NOT NULL DEFAULT 1,
  `recurrence_cron` VARCHAR(100) NULL,
  `root_todo_id` BIGINT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tms_todo` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `description` TEXT NULL,
  `due_date` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `priority` VARCHAR(10) NOT NULL,

  `series_id` VARCHAR(36) NULL,
  `parent_id` BIGINT NULL,
  `recurrence_id` BIGINT NULL,

  `deleted` TINYINT(1) NOT NULL DEFAULT 0,

  `blocking_dep_count` INT NOT NULL DEFAULT 0 COMMENT '未完成且未软删的依赖目标个数，0=非阻塞',

  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本',

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tms_todo_user_due` (`user_id`, `due_date`),
  KEY `idx_tms_todo_status` (`status`),
  KEY `idx_tms_todo_priority` (`priority`),
  KEY `idx_tms_todo_deleted` (`deleted`),
  KEY `idx_tms_todo_blocking_dep_count` (`blocking_dep_count`),
  KEY `idx_tms_todo_series` (`series_id`),
  KEY `idx_tms_todo_recurrence` (`recurrence_id`),
  KEY `idx_tms_todo_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tms_todo_dependency` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `todo_id` BIGINT NOT NULL,
  `depends_on_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tms_todo_dependency_pair` (`todo_id`, `depends_on_id`),
  KEY `idx_tms_todo_dependency_todo_id` (`todo_id`),
  KEY `idx_tms_todo_dependency_depends_on_id` (`depends_on_id`),
  CHECK (`todo_id` <> `depends_on_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
