package io.github.cmigle.donendusted.service;

import java.time.Instant;
import io.github.cmigle.donendusted.domain.RefreshToken;
import io.github.cmigle.donendusted.domain.User;
import io.github.cmigle.donendusted.error.ConflictException;
import io.github.cmigle.donendusted.error.ForbiddenException;
import io.github.cmigle.donendusted.error.UnauthorizedException;
import io.github.cmigle.donendusted.repository.GuildMembershipRepository;
import io.github.cmigle.donendusted.repository.RefreshTokenRepository;
import io.github.cmigle.donendusted.repository.UserRepository;
import io.github.cmigle.donendusted.security.CurrentUser;
import io.github.cmigle.donendusted.security.TokenService;
import io.github.cmigle.donendusted.web.dto.LoginRequest;
import io.github.cmigle.donendusted.web.dto.MeResponse;
import io.github.cmigle.donendusted.web.dto.RegisterRequest;
import io.github.cmigle.donendusted.web.dto.TokenResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;
    private final GuildMembershipRepository memberships;
    private final TokenService tokens;
    private final PasswordEncoder passwordEncoder;

    /** Lyginama, kai naudotojo nera - atsakymo laikas neisduoda, ar toks vardas egzistuoja. */
    private final String dummyHash;

    public AuthService(UserRepository users, RefreshTokenRepository refreshTokens,
                       GuildMembershipRepository memberships, TokenService tokens, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.refreshTokens = refreshTokens;
        this.memberships = memberships;
        this.tokens = tokens;
        this.passwordEncoder = passwordEncoder;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    /** Registracija visada sukuria USER; ADMIN role gali suteikti tik kitas administratorius. */
    public User register(RegisterRequest request) {
        if (users.existsByUsernameIgnoreCase(request.username())) {
            throw new ConflictException("Username \"" + request.username() + "\" is already taken.");
        }
        return users.save(new User(request.username(), passwordEncoder.encode(request.password())));
    }

    public TokenResponse login(LoginRequest request) {
        User user = users.findByUsernameIgnoreCase(request.username()).orElse(null);
        String hash = user != null ? user.getPasswordHash() : dummyHash;
        // Ta pati klaida ir neteisingam vardui, ir slaptazodziui - neatskleidziame, kas buvo blogai.
        if (!passwordEncoder.matches(request.password(), hash) || user == null) {
            throw new UnauthorizedException("Invalid username or password");
        }
        requireEnabled(user);
        return issueTokens(user);
    }

    /**
     * Rotacija: senas refresh zetonas atsaukiamas ir isduodama nauja pora. Jei atsiunciamas jau
     * atsauktas zetonas, jis greiciausiai pavogtas - atsaukiamos visos naudotojo sesijos.
     * noRollbackFor: atsaukimai DB turi islikti, nors metodas baigiasi klaida.
     */
    @Transactional(noRollbackFor = {UnauthorizedException.class, ForbiddenException.class})
    public TokenResponse refresh(String refreshToken) {
        Instant now = Instant.now();
        RefreshToken stored = refreshTokens.findByTokenHash(TokenService.hash(refreshToken))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        User user = stored.getUser();
        // Pirma: uzblokuojant atsaukiami visi zetonai, todel kitaip visada laimetu "jau panaudotas" (401),
        // o dokumentuotas 403 "Paskyra uzblokuota" niekada nebutu pasiektas.
        requireEnabled(user);

        if (stored.isRevoked()) {
            refreshTokens.revokeAllForUser(user.getId(), now);
            throw new UnauthorizedException(
                    "Refresh token was already used or revoked. All sessions have been revoked for security.");
        }
        if (stored.isExpired(now)) {
            throw new UnauthorizedException("Refresh token has expired. Please log in again.");
        }
        stored.revoke(now);
        return issueTokens(user);
    }

    /** Idempotentiskas: nezinomas ar jau atsauktas zetonas - ne klaida, rezultatas tas pats. */
    public void logout(String refreshToken) {
        refreshTokens.findByTokenHash(TokenService.hash(refreshToken))
                .ifPresent(token -> token.revoke(Instant.now()));
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        User user = users.findById(CurrentUser.get().id())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists."));
        return MeResponse.from(user, memberships.findByUserIdOrderByGuildName(user.getId()));
    }

    private void requireEnabled(User user) {
        if (!user.isEnabled()) {
            throw new ForbiddenException("Account is blocked.");
        }
    }

    private TokenResponse issueTokens(User user) {
        String refreshToken = tokens.newRefreshToken();
        refreshTokens.save(new RefreshToken(user, TokenService.hash(refreshToken), tokens.refreshTokenExpiry()));
        return new TokenResponse(tokens.createAccessToken(user), "Bearer", tokens.accessTokenSeconds(), refreshToken);
    }
}
