# 💰 Financial Control API

API REST desenvolvida com Spring Boot para atender uma aplicação mobile de **controle de gastos financeiros**.

O sistema permite que usuários gerenciem **despesas, receitas, metas financeiras e acompanhem seu histórico financeiro** com segurança via autenticação JWT.

## 🚀 Tecnologias Utilizadas

☕ Java 21

🌱 Spring Boot 3

🔐 Spring Security + JWT

🗄️ Spring Data JPA

🐬 MySQL

🛠️ Flyway (versionamento de banco)

📘 Springdoc OpenAPI (Swagger UI)

📱 Contexto da Aplicação

## Essa API serve como backend para um aplicativo mobile de controle financeiro pessoal.

### O objetivo é permitir que o usuário:

Cadastre despesas e receitas

Organize por categorias

Acompanhe saldo e metas financeiras

Consulte histórico de movimentações

Tenha autenticação segura com JWT

## 🔐 Segurança

A autenticação é baseada em JWT (JSON Web Token).

### Fluxo:

Usuário realiza login

API gera um Access Token

Mobile envia o token no header:

Authorization: Bearer {token}


Requisições protegidas são autorizadas via filtro de segurança

## 📦 Estrutura do Projeto
src/main/java/org/tech_solutions/application
├── controller
├── exception
├── model
├── repository
├── security
└── service


### Separação por responsabilidade:

Controller → Camada de entrada (HTTP)

Service → Regras de negócio

Repository → Acesso ao banco

Security → JWT, filtros e configurações

DTO → Objetos de transferência

Entity → Modelos persistidos

## 🗄️ Banco de Dados

Banco: MySQL

Migrações gerenciadas pelo Flyway

Scripts localizados em:

src/main/resources/db/migration


Exemplo de migration:

V1__create_user_table.sql
V2__create_transaction_table.sql

## 📊 Funcionalidades Principais
### 👤 Usuário

Cadastro

Login

Autenticação via JWT

## 📘 Documentação da API

Após iniciar a aplicação, acesse:

`http://localhost:8080/swagger-ui.html`


A documentação é gerada automaticamente pelo Springdoc OpenAPI.

## ⚙️ Configuração 
### 1️⃣ Clone o projeto
git clone <url-do-repositorio>

### 2️⃣ Configure o application.yml
```
spring:
    jpa:
        hibernate:
            ddl-auto: validate
    datasource:
        url: jdbc:mysql://localhost:3306/db
        username: root
        password: sua_senha
```
flyway:
enabled: true

### 3️⃣ Execute a aplicação
`mvn spring-boot:run`

## 🧠 Conceitos Importantes Aplicados
### 🔹 JWT

Token assinado digitalmente contendo informações do usuário.

### 🔹 Stateless Authentication

A API não mantém sessão no servidor.
Cada requisição deve conter o token.

### 🔹 DTO Pattern

Evita expor entidades diretamente e protege regras internas.

### 🔹 Migration com Flyway

Versionamento do banco, evitando inconsistência entre ambientes.

## 🏗️ Arquitetura

Arquitetura em camadas (Layered Architecture):

Controller → Service → Repository → Database

Separação clara de responsabilidades e alta manutenibilidade.