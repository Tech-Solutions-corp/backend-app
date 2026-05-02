CREATE TABLE IF NOT EXISTS monthly_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    reference_month DATE NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_monthly_limits_user_month UNIQUE (user_id, reference_month),
    CONSTRAINT fk_monthly_limits_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);