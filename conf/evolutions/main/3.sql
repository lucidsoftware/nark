# patch 3
# --- !Ups

ALTER TABLE `alerts`
	ADD COLUMN `data_seconds` INT NOT NULL DEFAULT 300,
	ADD COLUMN `drop_null_points` TINYINT UNSIGNED NOT NULL DEFAULT 0,
	ADD COLUMN `drop_null_targets` BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE `dynamic_alerts`
	ADD COLUMN `data_seconds` INT NOT NULL DEFAULT 300,
	ADD COLUMN `drop_null_points` TINYINT UNSIGNED NOT NULL DEFAULT 0,
	ADD COLUMN `drop_null_targets` BOOLEAN NOT NULL DEFAULT TRUE;

# --- !Downs

ALTER TABLE `dynamic_alerts`
	DROP COLUMN `data_seconds`,
	DROP COLUMN `drop_null_points`,
	DROP COLUMN `drop_null_targets`;

ALTER TABLE `alerts`
	DROP COLUMN `data_seconds`,
	DROP COLUMN `drop_null_points`,
	DROP COLUMN `drop_null_targets`;
