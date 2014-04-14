# patch 3
# --- !Ups

ALTER TABLE `alerts`
	ADD COLUMN `data_seconds` INT NOT NULL DEFAULT 300;

ALTER TABLE `dynamic_alerts`
	ADD COLUMN `data_seconds` INT NOT NULL DEFAULT 300;

# --- !Downs

ALTER TABLE `dynamic_alerts`
	DROP COLUMN `data_seconds`;

ALTER TABLE `alerts`
	DROP COLUMN `data_seconds`;
