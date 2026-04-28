-- =====================================================
-- CRIAÇÃO DO BANCO
-- =====================================================

CREATE
DATABASE IF NOT EXISTS financeiro_app;
USE
financeiro_app;
-- =====================================================
-- TABELA DE USUÁRIOS
-- =====================================================

CREATE TABLE users
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(150) NOT NULL,
    email      VARCHAR(150) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    password_reset_version INT NOT NULL DEFAULT 0
);

-- =====================================================
-- TABELA DE CONTAS (onde o dinheiro está guardado)
-- =====================================================

CREATE TABLE accounts
(
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

-- =====================================================
-- TABELA DE CATEGORIAS
-- =====================================================

CREATE TABLE categories
(
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    name       VARCHAR(100) NOT NULL,
    type       ENUM('INCOME', 'EXPENSE') NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_categories_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

-- =====================================================
-- TABELA DE TRANSAÇÕES
-- =====================================================

CREATE TABLE transactions
(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    category_id BIGINT NULL,  -- 👈 agora permite NULL
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

-- =====================================================
-- TABELA DE IMPORTAÇÃO DE EXTRATOS (CSV)
-- =====================================================

CREATE TABLE imports
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL,
    file_name   VARCHAR(255) NOT NULL,
    imported_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status      ENUM('PROCESSING', 'COMPLETED', 'FAILED') DEFAULT 'PROCESSING',

    CONSTRAINT fk_imports_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);

-- =====================================================
-- TABELA DE TRANSAÇÕES IMPORTADAS (RAW DATA)
-- =====================================================

CREATE TABLE imported_transactions
(
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

-- =====================================================
-- TABELA DE INSIGHTS DE IA
-- =====================================================

CREATE TABLE ai_insights
(
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    insight_type ENUM('SPENDING_PATTERN', 'SAVING_TIP', 'ANOMALY_DETECTION') NOT NULL,
    content      TEXT   NOT NULL,
    generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_ai_insights_user
        FOREIGN KEY (user_id) REFERENCES users (id)
            ON DELETE CASCADE
);