package lt.ktu.donendusted.security;

import lt.ktu.donendusted.error.UnauthorizedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/** Prisijunges naudotojas, nuskaitytas is patikrinto access zetono. */
public record CurrentUser(Long id, String username, boolean admin) {

    public static CurrentUser get() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            Jwt jwt = token.getToken();
            return new CurrentUser(Long.valueOf(jwt.getSubject()), jwt.getClaimAsString("username"),
                    "ADMIN".equals(jwt.getClaimAsString("role")));
        }
        throw new UnauthorizedException("Reikalingas prisijungimas");
    }
}
