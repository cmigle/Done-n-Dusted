package io.github.cmigle.donendusted.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS: narsykle leidzia React klientui is kito adreso (pvz. http://localhost:5173) kviesti API tik tada, kai
 * serveris atsako leidziancia Access-Control-* antraste. Leidziami adresai - app.cors.allowed-origins
 * (keli - per kableli; prod aplinkoje is CORS_ORIGINS). Taisykles taiko Spring Security (SecurityConfig
 * http.cors), todel preflight OPTIONS atsakomas dar pries autentifikacija - be zetono.
 */
@Configuration
public class CorsConfig {

    // Bean pavadinimas "corsConfigurationSource" - ji pagal varda randa Spring Security http.cors().
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String[] allowedOrigins) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(Arrays.stream(allowedOrigins).map(String::trim).filter(o -> !o.isEmpty()).toList());
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE));
        // Be sito JavaScript neperskaitytu Location antrastes (naujo resurso adreso po 201).
        cors.setExposedHeaders(List.of(HttpHeaders.LOCATION));
        // Preflight atsakymas narsykleje laikomas valanda - ne pries kiekviena uzklausa.
        cors.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cors);
        return source;
    }
}
