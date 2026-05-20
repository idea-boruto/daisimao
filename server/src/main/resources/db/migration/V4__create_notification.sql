CREATE TABLE `notification` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `type` VARCHAR(32) NOT NULL COMMENT 'event type: accepted, started, completed, confirmed, cancelled, auto_released, auto_confirmed, review_new',
    `title` VARCHAR(128) NOT NULL,
    `content` VARCHAR(512) NOT NULL,
    `is_read` TINYINT DEFAULT 0 NOT NULL,
    `related_task_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_user_type_task` (`user_id`, `type`, `related_task_id`),
    INDEX `idx_user_read` (`user_id`, `is_read`),
    INDEX `idx_user_created` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
