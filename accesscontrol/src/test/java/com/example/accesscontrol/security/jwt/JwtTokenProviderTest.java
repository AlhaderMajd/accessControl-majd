package com.example.accesscontrol.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;
    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    @BeforeEach
    void setUp() throws Exception {
        provider = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(provider, SECRET);
        provider.init();
    }

    @Test
    void generateToken_and_parse_email_success() {
        String email = "user@example.com";
        String token = provider.generateToken(email);
        assertNotNull(token);
        assertTrue(provider.validateToken(token));
        String parsedEmail = provider.getEmailFromToken(token);
        assertEquals(email, parsedEmail);
    }

    @Test
    void validateToken_withInvalidSignature_returnsFalse_and_getEmail_throws() throws Exception {
        JwtTokenProvider other = new JwtTokenProvider();
        Field secretField = JwtTokenProvider.class.getDeclaredField("secret");
        secretField.setAccessible(true);
        secretField.set(other, "ffffffffffffffffffffffffffffffff");
        other.init();

        String tokenFromOther = other.generateToken("user@example.com");
        assertFalse(provider.validateToken(tokenFromOther));
        assertThrows(Exception.class, () -> provider.getEmailFromToken(tokenFromOther));
    }

    @Test
    void validateToken_withExpiredToken_returnsFalse() {
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();
        Date expiredAt = new Date(now.getTime() - 1000L);
        String expiredToken = Jwts.builder()
                .setSubject("user@example.com")
                .setIssuedAt(new Date(now.getTime() - 2000L))
                .setExpiration(expiredAt)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertFalse(provider.validateToken(expiredToken));
        assertThrows(Exception.class, () -> provider.getEmailFromToken(expiredToken));
    }

    @Test
    void validateToken_withMalformedToken_returnsFalse() {
        String malformed = "not.a.jwt";
        assertFalse(provider.validateToken(malformed));
        assertThrows(Exception.class, () -> provider.getEmailFromToken(malformed));
    }
}
