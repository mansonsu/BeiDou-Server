CREATE TABLE IF NOT EXISTS account_identities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_id INT(11) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    provider_subject VARCHAR(191) NOT NULL,
    email VARCHAR(255) NULL,
    email_verified TINYINT(1) NOT NULL DEFAULT 0,
    display_name VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP NULL DEFAULT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_identity_provider_subject (provider, provider_subject),
    UNIQUE KEY uq_identity_account_provider (account_id, provider),
    KEY idx_identity_account (account_id),
    CONSTRAINT fk_identity_account FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
