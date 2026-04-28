package org.tech_solutions.application.security;

public record PasswordResetTokenData(String email, int version) {
}