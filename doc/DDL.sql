-- tms SleekFlow TODO 项目 DDL
-- 数据模型说明：
-- 1) tms_user：多用户支持（可选认证字段预留）
-- 2) tms_org：组织（用户与 todo 都归属于组织）
-- 2) tms_todo：TODO 主表（包含状态/优先级/周期任务参数/软删除时间）
-- 3) tms_todo_dependency：TODO 依赖关系（自引用的多对多，one TODO can depend on many)

-- 建议：若你需要重建库表，可手动先执行下方 DROP 语句（取消注释）。
-- DROP TABLE IF EXISTS `tms_todo_dependency`;
-- DROP TABLE IF EXISTS `tms_todo`;
-- DROP TABLE IF EXISTS `tms_user`;

CREATE TABLE IF NOT EXISTS `tms_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(64) NOT NULL,
  `password_hash` VARCHAR(255) NULL,
  `org_id` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tms_user_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `tms_org` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(200) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tms_org_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE `tms_user`
  ADD KEY `idx_tms_user_org` (`org_id`);

CREATE TABLE IF NOT EXISTS `tms_todo` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `org_id` BIGINT NOT NULL,
  `name` VARCHAR(200) NOT NULL,
  `description` TEXT NULL,
  `due_date` DATETIME NOT NULL,
  `status` VARCHAR(20) NOT NULL,
  `priority` VARCHAR(10) NOT NULL,

  -- 周期任务：当 is_recurring=1 时，以下字段表示生成下一次 occurrence 的规则
  `is_recurring` TINYINT(1) NOT NULL DEFAULT 0,
  `recurrence_type` VARCHAR(20) NULL,      -- DAILY / WEEKLY / MONTHLY / CUSTOM
  `recurrence_interval` INT NOT NULL DEFAULT 1,
  `recurrence_cron` VARCHAR(100) NULL,    -- CUSTOM 场景可填 cron 表达式

  -- 软删除：当执行 delete 时，不真正删除数据，而标记为归档/并记录时间
  `deleted_at` DATETIME NULL,

  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tms_todo_user_due` (`user_id`, `due_date`),
  KEY `idx_tms_todo_org_due` (`org_id`, `due_date`),
  KEY `idx_tms_todo_status` (`status`),
  KEY `idx_tms_todo_priority` (`priority`),
  KEY `idx_tms_todo_deleted_at` (`deleted_at`)
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
  -- MySQL 8+ 支持 CHECK；如果你的 MySQL 版本较老可忽略该约束
  CHECK (`todo_id` <> `depends_on_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

