# patch 2

# alerting

# --- !Ups

ALTER TABLE `users`
	ADD COLUMN `warn_address` VARCHAR(100) NOT NULL,
	ADD COLUMN `warn_enable` BOOLEAN NOT NULL,
	ADD COLUMN `error_address` VARCHAR(100) NOT NULL,
	ADD COLUMN `error_enable` BOOLEAN NOT NULL;

ALTER TABLE `graph_targets`
	ADD COLUMN `summarizer` TINYINT UNSIGNED NOT NULL DEFAULT 0;

CREATE TABLE `dynamic_alert_tags` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`alert_id` BINARY(16) NOT NULL,
	`tag` VARCHAR(25),
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`alert_id`, `tag`),
	KEY `tag` (`tag`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `dynamic_alerts` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`id` BINARY(16) NOT NULL,
	`name` VARCHAR(100) NOT NULL,
	`user_id`  BINARY(16) NOT NULL,
	`created` DATETIME NOT NULL,
	`search_target` VARCHAR(500) NOT NULL COMMENT 'list of graphite targets to get',
	`match` VARCHAR(500) NOT NULL COMMENT 'the regex to break the targets up with',
	`build_target` VARCHAR(500) NOT NULL COMMENT 'use the pieces from the regex to build targets',
	`error_threshold` DECIMAL(20, 3),
	`warn_threshold` DECIMAL(20, 3),
	`comparison` TINYINT UNSIGNED NOT NULL,
	`active` BOOLEAN NOT NULL,
	`deleted` BOOLEAN NOT NULL,
	`frequency` INT NOT NULL,
	PRIMARY KEY (`hiddenid`),
	KEY `name` (`name`),
	KEY `active` (`active`),
	KEY `deleted` (`deleted`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `dynamic_alert_tag_subscriptions` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`user_id` BINARY(16) NOT NULL,
	`tag` VARCHAR(25) NOT NULL,
	`active` BOOLEAN NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `alert_tag_lookup` (`user_id`, `tag`),
	KEY `tag_lookup` (`tag`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `alert_tags` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`alert_id` BINARY(16) NOT NULL,
	`tag` VARCHAR(25),
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`alert_id`, `tag`),
	KEY `tag` (`tag`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `alerts` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`id` BINARY(16) NOT NULL,
	`name` VARCHAR(100) NOT NULL,
	`user_id`  BINARY(16) NOT NULL,
	`target` VARCHAR(500) NOT NULL,
	`comparison` TINYINT UNSIGNED NOT NULL,
	`dynamic_alert_id` BINARY(16),
	`active` BOOLEAN NOT NULL,
	`deleted` BOOLEAN NOT NULL,
	`created` DATETIME NOT NULL,
	`thread_id` BINARY(16),
	`thread_start` DATETIME,
	`last_checked` DATETIME NOT NULL,
	`next_check` DATETIME NOT NULL,
	`frequency` INT NOT NULL,
	`warn_threshold` DECIMAL(20,3) NOT NULL,
	`error_threshold` DECIMAL(20,3) NOT NULL,
	`worst_state` TINYINT UNSIGNED NOT NULL,
	`consecutive_failures` INT NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`id`),
	KEY `name` (`name`),
	KEY `worker` (`active`, `deleted`, `thread_id`, `next_check`, `id`),
	KEY `worst_state` (`worst_state`, `last_checked`),
	KEY `deleted` (`deleted`),
	KEY `active` (`active`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `alert_subscriptions` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`user_id` BINARY(16) NOT NULL,
	`alert_id` BINARY(16) NOT NULL COMMENT "Can be alert id, alert tag id, or dynamic alert id",
	`alert_type` TINYINT UNSIGNED NOT NULL,
	`active` BOOLEAN NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `users_alerts_lookup` (`user_id`, `alert_type`, `alert_id`),
	KEY `alert_lookup` (`alert_id`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `alert_tag_subscriptions` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`user_id` BINARY(16) NOT NULL,
	`tag` VARCHAR(25) NOT NULL,
	`active` BOOLEAN NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `alert_tag_lookup` (`user_id`, `tag`),
	KEY `tag_lookup` (`tag`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `alert_history` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`alert_id` BINARY(16) NOT NULL,
	`target` VARCHAR(200) NOT NULL,
	`date` DATETIME NOT NULL,
	`state` TINYINT UNSIGNED NOT NULL,
	`messages_sent` INT NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`alert_id`, `target`, `date`),
	KEY `alert_date_lookup` (`alert_id`, `date`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `alert_target_state` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`alert_id` BINARY(16) NOT NULL,
	`target` VARCHAR(200) NOT NULL,
	`state` TINYINT UNSIGNED NOT NULL,
	`last_updated` DATETIME NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`alert_id`, `target`),
	KEY `alert_date_lookup` (`alert_id`, `last_updated`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `dashboard_tags` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`dashboard_id` BINARY(16) NOT NULL,
	`tag` VARCHAR(25) NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `dashboard_tag_key` (`dashboard_id`,`tag`),
	KEY `tag_key` (`tag`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;



# --- !Downs
DROP TABLE `dashboard_tags`;
DROP TABLE `alert_target_state`;
DROP TABLE `alert_history`;
DROP TABLE `alert_subscriptions`;
DROP TABLE `alert_tag_subscriptions`;
DROP TABLE `alerts`;
DROP TABLE `alert_tags`;
DROP TABLE `dynamic_alert_tag_subscriptions`;
DROP TABLE `dynamic_alerts`;
DROP TABLE `dynamic_alert_tags`;
ALTER TABLE `graph_targets`
	DROP COLUMN `summarizer`;
ALTER TABLE `users`
	DROP COLUMN `warn_address`,
	DROP COLUMN `warn_enable`,
	DROP COLUMN `error_address`,
	DROP COLUMN `error_enable`;
