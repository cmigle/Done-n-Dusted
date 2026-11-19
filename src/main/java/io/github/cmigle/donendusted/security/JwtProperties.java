package io.github.cmigle.donendusted.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * app.jwt.* nustatymai is application.yml.
 *
 * @param issuer          kas isdave zetona (iss); tikrinama kiekvienoje uzklausoje
 * @param secret          HS256 parasymo raktas, bent 32 baitai; prod aplinkoje - is JWT_SECRET
 * @param accessTokenTtl  access zetono galiojimas (trumpas - nutekejes zetonas greit nebetinka)
 * @param refreshTokenTtl refresh zetono galiojimas
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String issuer, String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
