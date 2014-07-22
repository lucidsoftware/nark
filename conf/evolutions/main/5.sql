# patch 5
# --- !Ups

ALTER TABLE `users`
	ADD COLUMN `admin` TINYINT(1) NOT NULL DEFAULT 0;
	

# --- !Downs
ALTER TABLE `users`
	DROP COLUMN `admin`;

