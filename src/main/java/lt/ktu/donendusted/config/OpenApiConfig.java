package lt.ktu.donendusted.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI doneNDustedOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Done n Dusted API")
                        .version("0.1.0")
                        .description("""
                                DnD principu veikianti produktyvumo sistema.

                                Resursu hierarchija: **Guild (1:N) -> Quest (1:N) -> LogEntry**.
                                Vaikinis objektas pasiekiamas tik per tevini kelia; neteisinga
                                grandine grazina 404. Visos klaidos grazinamos vienoda `ApiError`
                                struktura, visi sarasai - puslapiuoti `PagedResponse` su
                                self/first/prev/next/last nuorodomis.
                                """)
                        .contact(new Contact().name("Migle Cirtautaite"))
                        .license(new License().name("KTU T120B165")))
                .servers(List.of(new Server().url("/").description("Dabartinis serveris")));
    }
}
