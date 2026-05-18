CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `username` VARCHAR(64) NOT NULL UNIQUE,
    `phone` VARCHAR(20),
    `nickname` VARCHAR(64),
    `avatar_url` VARCHAR(512),
    `real_name` VARCHAR(32),
    `student_id` VARCHAR(32),
    `campus` VARCHAR(64),
    `credit_score` INT DEFAULT 100,
    `completed_orders` INT DEFAULT 0,
    `cancelled_orders` INT DEFAULT 0,
    `dispute_orders` INT DEFAULT 0,
    `role` TINYINT DEFAULT 0,
    `status` TINYINT DEFAULT 1,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_username` (`username`),
    INDEX `idx_campus` (`campus`),
    INDEX `idx_credit_score` (`credit_score`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `publisher_id` BIGINT NOT NULL,
    `acceptor_id` BIGINT,
    `type` TINYINT NOT NULL,
    `title` VARCHAR(128) NOT NULL,
    `description` VARCHAR(512),
    `reward` DECIMAL(8,2) NOT NULL,
    `pickup_location` VARCHAR(256),
    `delivery_location` VARCHAR(256),
    `status` TINYINT DEFAULT 1,
    `deadline` DATETIME,
    `accepted_at` DATETIME,
    `completed_at` DATETIME,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX `idx_publisher` (`publisher_id`),
    INDEX `idx_acceptor` (`acceptor_id`),
    INDEX `idx_status_type` (`status`, `type`),
    INDEX `idx_delivery_deadline` (`delivery_location`, `deadline`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `review` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `task_id` BIGINT NOT NULL,
    `reviewer_id` BIGINT NOT NULL,
    `target_id` BIGINT NOT NULL,
    `rating` TINYINT NOT NULL,
    `tags` VARCHAR(256),
    `comment` VARCHAR(512),
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY `uk_task_reviewer_target` (`task_id`, `reviewer_id`, `target_id`),
    INDEX `idx_target` (`target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `credit_log` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `change_amount` INT NOT NULL,
    `reason` VARCHAR(128) NOT NULL,
    `related_task_id` BIGINT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX `idx_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
