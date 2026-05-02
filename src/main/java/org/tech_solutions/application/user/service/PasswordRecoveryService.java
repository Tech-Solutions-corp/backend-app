package org.tech_solutions.application.user.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.security.CurrentUserService;
import org.tech_solutions.application.security.PasswordResetTokenData;
import org.tech_solutions.application.security.TokenManager;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class PasswordRecoveryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordRecoveryService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private final JavaMailSender mailSender;
    private final CurrentUserService currentUserService;

    @Value("${application.var.frontend.reset-password-url}")
    private String resetPasswordUrl;

    @Value("${application.var.mail.from}")
    private String fromEmail;

    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenManager tokenManager,
            JavaMailSender mailSender,
            CurrentUserService currentUserService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenManager = tokenManager;
        this.mailSender = mailSender;
        this.currentUserService = currentUserService;
    }

    public void requestReset(String email) {
        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    try {
                        sendRecoveryEmail(user);
                    } catch (Exception ex) {
                        LOGGER.error("Falha ao enviar email de recuperacao para {}", user.getEmail(), ex);
                    }
                });
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetTokenData tokenData = tokenManager.obterDadosTokenRecuperacao(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperacao invalido ou expirado"));

        User user = userRepository.findByEmail(tokenData.email())
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperacao invalido ou expirado"));

        int currentVersion = user.getPasswordResetVersion() == null ? 0 : user.getPasswordResetVersion();
        if (currentVersion != tokenData.version()) {
            throw new IllegalArgumentException("Token de recuperacao invalido ou expirado");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordResetVersion(currentVersion + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    public void changePassword(String currentPassword, String newPassword, String confirmNewPassword) {
        User user = currentUserService.requireCurrentUser();

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Senha atual invalida");
        }

        if (!newPassword.equals(confirmNewPassword)) {
            throw new IllegalArgumentException("Nova senha e confirmacao devem ser iguais");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        int currentVersion = user.getPasswordResetVersion() == null ? 0 : user.getPasswordResetVersion();
        user.setPasswordResetVersion(currentVersion + 1);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void sendRecoveryEmail(User user) {
        int version = user.getPasswordResetVersion() == null ? 0 : user.getPasswordResetVersion();
        String token = tokenManager.gerarTokenRecuperacaoSenha(user.getEmail(), version);
        String resetLink = String.format("%s?token=%s", resetPasswordUrl, token);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(user.getEmail());
        message.setSubject("Recuperacao de senha - Financeiro App");
        message.setText(buildBody(user.getName(), resetLink));

        mailSender.send(message);
    }

    private String buildBody(String userName, String resetLink) {
        return "Ola " + userName + ",\n\n"
                + "Recebemos uma solicitacao para redefinir sua senha.\n"
                + "Use o link abaixo (expira em 15 minutos):\n"
                + resetLink + "\n\n"
                + "Se voce nao solicitou, ignore este email.\n\n"
                + "Equipe Financeiro App";
    }
}
