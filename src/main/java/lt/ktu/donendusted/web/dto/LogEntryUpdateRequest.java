package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lt.ktu.donendusted.domain.LogStatus;
import org.hibernate.validator.constraints.URL;

/**
 * Pilnas iraso atnaujinimas: iraso turinys ir DM sprendimas. Tai vienintele vieta,
 * kurioje XP apskritai gali buti paduotas is kliento.
 */
@Schema(name = "LogEntryUpdateRequest", description = "Iraso turinio ir perziuros rezultato atnaujinimas")
public record LogEntryUpdateRequest(

        @Schema(example = "Begau 8 km palei upe.")
        @Size(max = 1000)
        String note,

        @Schema(example = "https://example.org/evidence/run-8km.png")
        @URL @Size(max = 500)
        String evidenceUrl,

        @Schema(example = "2026-09-19T15:20:00Z")
        @NotNull
        Instant occurredAt,

        @Schema(description = "Nauja iraso busena", example = "APPROVED")
        @NotNull
        LogStatus status,

        @Schema(description = "DM perrasytas XP ivertis. Praleidus, tvirtinant naudojama uzduoties verte.",
                example = "50")
        @PositiveOrZero
        Integer xpAwarded,

        @Schema(description = "Skiriamas heroic inspiration; praleidus laikoma 0", example = "1")
        @PositiveOrZero
        Integer inspirationAwarded,

        @Schema(example = "Uzskaitau, nors nuotrauka neryski.")
        @Size(max = 500)
        String reviewerNote) {
}
