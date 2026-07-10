INSERT INTO `game_config`(`config_type`, `config_sub_type`, `config_clazz`, `config_code`, `config_value`, `config_desc`, `update_time`)
SELECT 'server', 'Safe', 'java.lang.Integer', 'registration_ip_account_limit', '3', 'registration_ip_account_limit', NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM `game_config` WHERE `config_code` = 'registration_ip_account_limit'
);

INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'zh-CN', 'game_config', 'registration_ip_account_limit', '同一 IP 可注册账号数量上限，设置为 0 表示不限制', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'zh-CN' AND `lang_base` = 'game_config' AND `lang_code` = 'registration_ip_account_limit'
);

INSERT INTO `lang_resources`(`lang_type`, `lang_base`, `lang_code`, `lang_value`, `lang_extend`)
SELECT 'en-US', 'game_config', 'registration_ip_account_limit', 'Maximum accounts that can be registered from the same IP. Set 0 to disable the limit.', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM `lang_resources` WHERE `lang_type` = 'en-US' AND `lang_base` = 'game_config' AND `lang_code` = 'registration_ip_account_limit'
);
