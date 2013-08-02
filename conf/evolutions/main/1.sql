# patch 1
# initial setup - dashboards

# --- !Ups

CREATE TABLE `users` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`id` BINARY(16) NOT NULL,
	`email` VARCHAR(100) NOT NULL,
	`created` DATETIME NOT NULL,
	`name` VARCHAR(100) NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`id`),
	KEY `email_key` (`email`),
	KEY `created_key` (`created`)
) ENGINE=InnoDB CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `openid_nonces` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`provider` VARCHAR(255) NOT NULL,
	`nonce` VARCHAR(100) NOT NULL,
	`created` DATETIME NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`provider`, `nonce`),
	KEY `created_key` (`created`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `openid_associations` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`provider` VARCHAR(255) NOT NULL,
	`handle` VARCHAR(255) NOT NULL,
	`created` DATETIME NOT NULL,
	`expire` DATETIME NOT NULL,
	`type` TINYINT UNSIGNED NOT NULL,
	`secret` BLOB NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`handle`, `provider`),
	KEY `last_by_provider_key` (`provider`, `expire`),
	KEY `expire_key` (`expire`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `dashboards` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`id` BINARY(16) NOT NULL,
	`name` VARCHAR(100) NOT NULL,
	`url` VARCHAR(15) CHARACTER SET UTF8 COLLATE UTF8_BIN NOT NULL,
	`created` DATETIME NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`id`),
	UNIQUE KEY `url_key` (`url`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `graphs` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`id` BINARY(16) NOT NULL,
	`name` VARCHAR(100) NOT NULL,
	`dashboard_id` BINARY(16) NOT NULL,
	`sort` INT NOT NULL,
	`type` TINYINT UNSIGNED NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`id`),
	KEY `dashboard_key` (`dashboard_id`, `sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `graph_targets` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`id` BINARY(16) NOT NULL,
	`graph_id` BINARY(16) NOT NULL,
	`target` VARCHAR(500) NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`id`),
	KEY `graph_key` (`graph_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;

CREATE TABLE `hosts` (
	`hiddenid` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
	`name` VARCHAR(100) CHARACTER SET UTF8 COLLATE UTF8_BIN NOT NULL,
	`state` TINYINT UNSIGNED NOT NULL,
	`last_confirmed` DATETIME NOT NULL,
	PRIMARY KEY (`hiddenid`),
	UNIQUE KEY `app_pk` (`name`),
	KEY `state_key` (`state`, `name`),
	KEY `last_confirmed_key` (`last_confirmed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_general_ci;


# --- !Downs

DROP TABLE `hosts`;
DROP TABLE `graph_targets`;
DROP TABLE `graphs`;
DROP TABLE `dashboards`;
DROP TABLE `openid_associations`;
DROP TABLE `openid_nonces`;
DROP TABLE `users`;
