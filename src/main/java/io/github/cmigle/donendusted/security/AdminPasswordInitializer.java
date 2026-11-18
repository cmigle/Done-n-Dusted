package io.github.cmigle.donendusted.security;

import java.time.Instant;
import io.github.cmigle.donendusted.repository.RefreshTokenRepository;
import io.github.cmigle.donendusted.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Viesame diegime seed administratoriaus slaptazodis (Demo123!) pakeiciamas slaptu is aplinkos kintamojo
 * ADMIN_PASSWORD. Tikrinama kiekviena paleidima, todel ir is naujo sukurus DB (Flyway seed) admin
 * slaptazodis lieka slaptas. Kintamojo nera (lokaliai) - nieko nedaroma.
 */
@Component
public class AdminPasswordInitializer implements ApplicationRunner {

    static final int MIN_LENGTH = 12;
    private static final Logger log = LoggerFactory.getLogger(AdminPasswordInitializer.class);

    private final String adminPassword;
    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final PasswordEncoder encoder;

    public AdminPasswordInitializer(@Value("${app.admin-password:}") String adminPassword, UserRepository users,
                                    RefreshTokenRepository refreshTokens, PasswordEncoder encoder) {
        this.adminPassword = adminPassword;
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminPassword.isBlank()) {
            return;
        }
        if (adminPassword.length() < MIN_LENGTH) {
            log.warn("ADMIN_PASSWORD per trumpas (maziau nei {} simboliu) - admin slaptazodis nekeiciamas", MIN_LENGTH);
            return;
        }
        users.findByUsernameIgnoreCase("admin")
                .filter(admin -> !encoder.matches(adminPassword, admin.getPasswordHash()))
                .ifPresent(admin -> {
                    admin.changePassword(encoder.encode(adminPassword));
                    // Senu sesiju su ankstesniu slaptazodziu neliko.
                    refreshTokens.revokeAllForUser(admin.getId(), Instant.now());
                    log.info("Administratoriaus slaptazodis pakeistas is ADMIN_PASSWORD");
                });
    }
}
