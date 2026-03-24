package org.tech_solutions.application.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

@Service
public class TokenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);
    private static final String PASSWORD_RESET_PURPOSE = "PASSWORD_RESET";
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;
    private final long tempoExpiracaoToken;
    private final long tempoExpiracaoTokenRecuperacao;

    public TokenManager(
            @Value("${application.var.token.secret}") String secret,
            @Value("${application.var.token.expiration-time}") long tempoExpiracaoToken,
            @Value("${application.var.token.password-reset-expiration-time:900000}") long tempoExpiracaoTokenRecuperacao
    ) {
        this.secretKey = criarSecretKey(secret);
        this.tempoExpiracaoToken = tempoExpiracaoToken;
        this.tempoExpiracaoTokenRecuperacao = tempoExpiracaoTokenRecuperacao;
    }

    public String gerarToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(this.gerarTempoExpiracao())
                .signWith(this.obterSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Optional<String> obterUsernameDeToken(String token) {
        try {
            return Optional.of(
                Jwts.parser()
                        .verifyWith(this.obterSecretKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getSubject()
            );
        } catch (ExpiredJwtException e) {
            LOGGER.debug("Token expirado");
        } catch (MalformedJwtException e) {
            LOGGER.debug("Token malformado");
        } catch (SignatureException e) {
            LOGGER.debug("Assinatura invalida");
        } catch (Exception e) {
            LOGGER.debug("Erro ao parsear token: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public String gerarTokenRecuperacaoSenha(String email) {
        return Jwts.builder()
                .subject(email)
                .claim("purpose", PASSWORD_RESET_PURPOSE)
                .issuedAt(new Date())
                .expiration(this.gerarTempoExpiracao(tempoExpiracaoTokenRecuperacao))
                .signWith(this.obterSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Optional<String> obterEmailDeTokenRecuperacao(String token) {
        try {
            var claims = Jwts.parser()
                    .verifyWith(this.obterSecretKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String purpose = claims.get("purpose", String.class);
            if (!PASSWORD_RESET_PURPOSE.equals(purpose)) {
                return Optional.empty();
            }

            return Optional.ofNullable(claims.getSubject());
        } catch (ExpiredJwtException e) {
            LOGGER.debug("Token de recuperacao expirado");
        } catch (MalformedJwtException e) {
            LOGGER.debug("Token de recuperacao malformado");
        } catch (SignatureException e) {
            LOGGER.debug("Assinatura do token de recuperacao invalida");
        } catch (Exception e) {
            LOGGER.debug("Erro ao parsear token de recuperacao: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Date gerarTempoExpiracao() {
        return new Date(System.currentTimeMillis() + tempoExpiracaoToken);
    }

    private Date gerarTempoExpiracao(long tempo) {
        return new Date(System.currentTimeMillis() + tempo);
    }

    private SecretKey obterSecretKey() {
        return secretKey;
    }

    private SecretKey criarSecretKey(String rawSecret) {
        if (rawSecret == null || rawSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET nao configurado. Informe uma chave com pelo menos 32 bytes para HS256");
        }

        byte[] secretBytes;
        if (rawSecret.startsWith("base64:")) {
            try {
                secretBytes = Base64.getDecoder().decode(rawSecret.substring(7).trim());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("JWT_SECRET em base64 invalido. Use formato: base64:<valor>");
            }
        } else {
            secretBytes = rawSecret.getBytes(StandardCharsets.UTF_8);
        }

        if (secretBytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET fraco: minimo de 32 bytes para HS256. Configure um valor mais longo ou use base64:<valor>"
            );
        }

        return Keys.hmacShaKeyFor(secretBytes);
    }
}
