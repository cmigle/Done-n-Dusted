package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import org.hibernate.validator.constraints.URL;

/**
 * Zaidejo registruojamas progreso irasas. Cia specialiai nera nei XP, nei busenos -
 * naujas irasas visada gimsta PENDING su nuliu tasku.
 */
@Schema(name = "LogEntryCreateRequest", description = "Naujo progreso iraso duomenys")
public record LogEntryCreateRequest(

        @Schema(example = "Begau 8 km palei upe.")
        @Size(max = 1000)
        String note,

        @Schema(description = "Nuoroda i irodyma, pvz. nuotrauka",
                example = "https://example.org/evidence/run-8km.png")
        @URL @Size(max = 500)
        String evidenceUrl,

        @Schema(description = "Kada veiksmas atliktas; negali buti ateityje",
                example = "2026-09-19T15:20:00Z")
        @NotNull
        Instant occurredAt) {
}
