package io.github.cmigle.donendusted.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    public OpenAPI doneNDustedOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Done n Dusted API")
                        .version("0.3.0")
                        .description("""
                                A productivity system built on DnD ideas.

                                Resource hierarchy: **Guild (1:N) -> Quest (1:N) -> LogEntry**.
                                A child resource is reachable only through its parent path; a wrong
                                chain returns 404. All errors use the same `ApiError`
                                structure, and all lists are paged `PagedResponse` objects with
                                self/first/prev/next/last links.

                                **Authentication (JWT).** `POST /api/auth/login` returns an access token
                                (valid for 15 min) and a refresh token (7 days). Send the access token
                                in the `Authorization: Bearer <accessToken>` header; when it expires, get a new pair
                                from `POST /api/auth/refresh`. In Swagger UI, use the **Authorize** button.

                                **Roles.** A guest can only register and log in. In a guild, a user
                                is a `PLAYER` or the `DUNGEON_MASTER` (the game master, one per guild). `ADMIN` is a global role:
                                sees all guilds, removes content and manages users.
                                401: no token or an invalid one; 403: not enough permissions.
                                """)
                        .contact(new Contact().name("Migle Cirtautaite")))
                .servers(List.of(new Server().url("/").description("Current server")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access token from POST /api/auth/login")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }

    /**
     * Kad nereiketu kartoti anotaciju prie kiekvieno metodo: kiekvienam metodui su parametrais ar turiniu
     * pridedamas 400, keliams su ID - 404, apsaugotiems metodams - 401, o grupes ir naudotoju metodams - 403.
     * Viesiems metodams (security: []) 401/403 nepridedami.
     */
    @Bean
    public OpenApiCustomizer authErrorResponses() {
        return openApi -> openApi.getPaths().forEach((path, item) -> item.readOperations().forEach(operation -> {
            boolean hasInput = operation.getRequestBody() != null
                    || (operation.getParameters() != null && !operation.getParameters().isEmpty());
            if (hasInput) {
                operation.getResponses().putIfAbsent("400",
                        error("Invalid parameters or body (for example, a non-numeric ID, an invalid sort or malformed JSON)"));
            }
            if (path.contains("{")) {
                operation.getResponses().putIfAbsent("404",
                        error("Resource not found or wrong parent chain"));
            }
            if (operation.getSecurity() != null && operation.getSecurity().isEmpty()) {
                return;
            }
            operation.getResponses().putIfAbsent("401",
                    error("Access token missing, invalid or expired"));
            if (path.startsWith("/api/guilds/{guildId}") || path.startsWith("/api/users")) {
                operation.getResponses().putIfAbsent("403",
                        error("Not enough permissions: not a guild member, wrong guild role or not ADMIN"));
            }
        }));
    }

    private static ApiResponse error(String description) {
        return new ApiResponse()
                .description(description)
                .content(new Content().addMediaType("application/json",
                        new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiError"))));
    }
}
