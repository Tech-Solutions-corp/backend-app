package org.tech_solutions.application.user.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.tech_solutions.application.security.TokenManager;
import org.tech_solutions.application.user.model.User;
import org.tech_solutions.application.user.repository.UserRepository;

import java.time.LocalDateTime;

@Service
public class PasswordRecoveryService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;
    private final JavaMailSender mailSender;

    @Value("${application.var.frontend.reset-password-url}")
    private String resetPasswordUrl;

    @Value("${application.var.mail.from}")
    private String fromEmail;

    public PasswordRecoveryService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenManager tokenManager,
            JavaMailSender mailSender
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenManager = tokenManager;
        this.mailSender = mailSender;
    }

    public void requestReset(String email) {
        userRepository.findByEmail(email)
                .ifPresent(this::sendRecoveryEmail);
    }

    public void resetPassword(String token, String newPassword) {
        String email = tokenManager.obterEmailDeTokenRecuperacao(token)
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperacao invalido ou expirado"));

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Token de recuperacao invalido ou expirado"));

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private void sendRecoveryEmail(User user) {
        String token = tokenManager.gerarTokenRecuperacaoSenha(user.getEmail());
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
