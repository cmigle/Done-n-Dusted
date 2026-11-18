package lt.ktu.donendusted.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lt.ktu.donendusted.security.ApiAuthErrorHandler;
import lt.ktu.donendusted.security.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Visas API apsaugotas JWT access zetonu (Authorization: Bearer ...), isskyrus registracija,
 * prisijungima, zetono atnaujinima ir Swagger dokumentacija. Serveris busenos nesaugo (STATELESS) -
 * kiekviena uzklausa autentifikuojama is naujo pagal zetona.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] PUBLIC_AUTH = {
            "/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout"
    };

    @Bean
    public SecurityFilterChain apiSecurity(HttpSecurity http, ApiAuthErrorHandler errors) throws Exception {
        http
                // CSRF reikalingas cookie sesijoms. Cia zetonas siunciamas antrasteje, kurios narsykle
                // pati neprideda, todel svetima svetaine negali atlikti uzklausos naudotojo vardu.
                .csrf(csrf -> csrf.disable())
                // CORS taisykles is CorsConfig bean: preflight OPTIONS atsakomas pries autentifikacija (be zetono).
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, PUBLIC_AUTH).permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/error").permitAll()
                        .requestMatchers("/api/users/**").hasRole("ADMIN")
                        // Visa kita - tik prisijungus. Grupiu teises (narys, DM, savininkas) tikrina servisai.
                        .anyRequest().authenticated())
                .oauth2ResourceServer(rs -> rs
                        .bearerTokenResolver(publicAuthIgnoresToken())
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                        .authenticationEntryPoint(errors)
                        .accessDeniedHandler(errors))
                // 401/403 is filtru - ta pati ApiError struktura kaip ir kitos klaidos
                .exceptionHandling(e -> e.authenticationEntryPoint(errors).accessDeniedHandler(errors));
        return http.build();
    }

    /**
     * Viesi /api/auth keliai Authorization antrastes neskaito. Klientas (pvz. LD3) pasibaigusi access
     * zetona dazniausiai prideda prie visu uzklausu - jis neturi trukdyti prisijungti ar atnaujinti zetonu.
     */
    private static BearerTokenResolver publicAuthIgnoresToken() {
        DefaultBearerTokenResolver resolver = new DefaultBearerTokenResolver();
        List<String> publicPaths = List.of(PUBLIC_AUTH);
        return request -> publicPaths.contains(request.getRequestURI()) ? null : resolver.resolve(request);
    }

    /** "role" claim (USER/ADMIN) virsta Spring authority ROLE_USER / ROLE_ADMIN. */
    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    @Bean
    public SecretKey jwtSigningKey(JwtProperties props) {
        byte[] secret = props.secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("app.jwt.secret turi buti bent 32 baitu ilgio (HS256)");
        }
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtSigningKey) {
        return NimbusJwtEncoder.withSecretKey(jwtSigningKey).algorithm(MacAlgorithm.HS256).build();
    }

    /**
     * Tikrina parasa, galiojimo laika (exp), isdavija (iss) ir claims forma: sub - naudotojo ID (skaicius),
     * role - USER arba ADMIN. Kitoks zetonas atmetamas filtre (401), o ne luzta giliau kode.
     */
    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtSigningKey, JwtProperties props) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSigningKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(props.issuer()),
                new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && sub.matches("\\d{1,18}")),
                new JwtClaimValidator<Object>("role", role -> "USER".equals(role) || "ADMIN".equals(role))));
        return decoder;
    }

    /** BCrypt: leta maisos funkcija su atsitiktine druska - vienodi slaptazodziai duoda skirtingas maisas. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
