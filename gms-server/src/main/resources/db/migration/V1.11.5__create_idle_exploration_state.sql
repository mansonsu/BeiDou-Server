CREATE TABLE IF NOT EXISTS `idle_exploration_state`
(
    `characterid`    INT(11)   NOT NULL,
    `explore_map_id` INT(11)   NOT NULL,
    `started_at`     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`characterid`),
    KEY `explore_map_id` (`explore_map_id`),
    CONSTRAINT `fk_idle_exploration_state_characterid`
        FOREIGN KEY (`characterid`) REFERENCES `characters` (`id`) ON DELETE CASCADE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4;
