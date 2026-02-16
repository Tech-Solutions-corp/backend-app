CREATE TABLE usuarios (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome     VARCHAR(70)  NOT NULL,
    senha    VARCHAR(255) NOT NULL,
    email    VARCHAR(150) NOT NULL UNIQUE,
    telefone VARCHAR(11)
);