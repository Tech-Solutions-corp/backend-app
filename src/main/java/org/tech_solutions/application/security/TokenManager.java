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
import java.util.Date;
import java.util.Optional;

@Service
public class TokenManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(TokenManager.class);

    private final String secret;
    private final long tempoExpiracaoToken;

    public TokenManager(
            @Value("${application.var.token.secret}") String secret,
            @Value("${application.var.token.expiration-time}") long tempoExpiracaoToken
    ) {
        this.secret = secret;
        this.tempoExpiracaoToken = tempoExpiracaoToken;
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
            LOGGER.debug("Assinatura inválida");
        } catch (Exception e) {
            LOGGER.debug("Erro ao parsear token: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Date gerarTempoExpiracao() {
        return new Date(System.currentTimeMillis() + tempoExpiracaoToken);
    }

    private SecretKey obterSecretKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
