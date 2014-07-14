# patch 4
# --- !Ups

ALTER TABLE `alert_history`
	ADD COLUMN `alert_value` DECIMAL NOT NULL DEFAULT 0;
	

# --- !Downs
ALTER TABLE `alert_history`
	DROP COLUMN `alert_value`;

