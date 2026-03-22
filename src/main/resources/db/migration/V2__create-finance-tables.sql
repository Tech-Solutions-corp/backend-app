CREATE TABLE IF NOT EXISTS accounts (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       ENUM('WALLET', 'BANK', 'SAVINGS', 'CREDIT_CARD', 'INVESTMENT') NOT NULL,
    balance    DECIMAL(15, 2) DEFAULT 0.00,
    created_at TIMESTAMP      DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT fk_accounts_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS categories (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       ENUM('INCOME', 'EXPENSE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    category_id BIGINT NULL,
    transaction_description VARCHAR(255),
    amount DECIMAL(15,2) NOT NULL,
    transaction_date DATE NOT NULL,
    transaction_type ENUM('INCOME', 'EXPENSE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_transactions_account
        FOREIGN KEY (account_id) REFERENCES accounts(id)
            ON DELETE CASCADE,

    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id) REFERENCES categories(id)
            ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS imports (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status      ENUM('PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PROCESSING',

    CONSTRAINT fk_imports_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS imported_transactions (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    import_id       BIGINT NOT NULL,
    raw_description VARCHAR(255),
    raw_amount      DECIMAL(15, 2),
    raw_date        DATE,
    processed       BOOLEAN DEFAULT FALSE,

    CONSTRAINT fk_imported_transactions_import
        FOREIGN KEY (import_id) REFERENCES imports (id)
            ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_insights (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    insight_type ENUM('SPENDING_PATTERN', 'SAVING_TIP', 'ANOMALY_DETECTION') NOT NULL,
    content      TEXT   NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_insights_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

