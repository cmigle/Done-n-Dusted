package lt.ktu.donendusted.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
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
                        .version("0.2.0")
                        .description("""
                                DnD principu veikianti produktyvumo sistema.

                                Resursu hierarchija: **Guild (1:N) -> Quest (1:N) -> LogEntry**.
                                Vaikinis objektas pasiekiamas tik per tevini kelia; neteisinga
                                grandine grazina 404. Visos klaidos grazinamos vienoda `ApiError`
                                struktura, visi sarasai - puslapiuoti `PagedResponse` su
                                self/first/prev/next/last nuorodomis.

                                **Autentifikacija (JWT).** `POST /api/auth/login` grazina access zetona
                                (galioja 15 min) ir refresh zetona (7 d.). Access zetonas siunciamas
                                antrasteje `Authorization: Bearer <accessToken>`; pasibaigus - nauja pora
                                gaunama per `POST /api/auth/refresh`. Swagger UI: mygtukas **Authorize**.

                                **Roles.** Svecias - tik registracija ir prisijungimas. Grupeje naudotojas
                                yra `PLAYER` arba `DUNGEON_MASTER` (vienas grupeje). `ADMIN` - globali role:
                                mato visas grupes, salina turini ir valdo naudotojus.
                                401 - nera zetono arba jis netinka, 403 - neuztenka teisiu.
                                """)
                        .contact(new Contact().name("Migle Cirtautaite"))
                        .license(new License().name("KTU T120B165")))
                .servers(List.of(new Server().url("/").description("Dabartinis serveris")))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Access zetonas is POST /api/auth/login")))
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
                        error("Neteisingi parametrai ar turinys (pvz. ne skaicius vietoje ID, netinkamas sort ar JSON)"));
            }
            if (path.contains("{")) {
                operation.getResponses().putIfAbsent("404",
                        error("Resursas nerastas arba neteisinga tevine grandine"));
            }
            if (operation.getSecurity() != null && operation.getSecurity().isEmpty()) {
                return;
            }
            operation.getResponses().putIfAbsent("401",
                    error("Nera access zetono arba jis neteisingas / pasibaiges"));
            if (path.startsWith("/api/guilds/{guildId}") || path.startsWith("/api/users")) {
                operation.getResponses().putIfAbsent("403",
                        error("Neuztenka teisiu: ne grupes narys, netinkama role grupeje arba ne ADMIN"));
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
