# patch 6
# --- !Ups

ALTER TABLE `alert_history` 
	MODIFY `alert_value` DECIMAL(10,0) DEFAULT 0;

	
# --- !Downs
ALTER TABLE `alert_history`
	MODIFY `alert_value` DECIMAL(10,0) NOT NULL DEFAULT 0;

