package lt.ktu.donendusted.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.repository.RefreshTokenRepository;
import lt.ktu.donendusted.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminPasswordInitializerTest {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final UserRepository users = mock(UserRepository.class);
    private final RefreshTokenRepository tokens = mock(RefreshTokenRepository.class);
    private final User admin = new User("admin", encoder.encode("Demo123!"));

    @Test
    void replacesSeedPasswordWithConfiguredOne() {
        when(users.findByUsernameIgnoreCase("admin")).thenReturn(Optional.of(admin));
        new AdminPasswordInitializer("Slaptas-Admin-2026", users, tokens, encoder).run(null);
        assertThat(encoder.matches("Slaptas-Admin-2026", admin.getPasswordHash())).isTrue();
        assertThat(encoder.matches("Demo123!", admin.getPasswordHash())).isFalse();
        verify(tokens).revokeAllForUser(any(), any());
    }

    @Test
    void doesNothingWithoutConfiguredPassword() {
        String before = admin.getPasswordHash();
        new AdminPasswordInitializer("", users, tokens, encoder).run(null);
        assertThat(admin.getPasswordHash()).isEqualTo(before);
        verify(users, never()).findByUsernameIgnoreCase(any());
    }

    @Test
    void ignoresTooShortPassword() {
        String before = admin.getPasswordHash();
        new AdminPasswordInitializer("trumpas", users, tokens, encoder).run(null);
        assertThat(admin.getPasswordHash()).isEqualTo(before);
        verify(tokens, never()).revokeAllForUser(anyLong(), any());
    }
}
