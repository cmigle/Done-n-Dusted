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
@Schema(name = "LogEntryCreateRequest", description = "Data for a new log entry")
public record LogEntryCreateRequest(

        @Schema(example = "Ran 8 km along the river.")
        @Size(max = 1000)
        String note,

        @Schema(description = "Link to evidence, for example a photo",
                example = "https://example.org/evidence/run-8km.png")
        @URL @Size(max = 500)
        String evidenceUrl,

        @Schema(description = "When the activity happened; cannot be in the future",
                example = "2026-09-19T15:20:00Z")
        @NotNull
        Instant occurredAt) {
}
