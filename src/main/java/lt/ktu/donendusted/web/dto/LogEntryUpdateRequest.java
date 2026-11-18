package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lt.ktu.donendusted.domain.LogStatus;
import org.hibernate.validator.constraints.URL;

/**
 * PATCH - dalinis iraso atnaujinimas: iraso turinys ir/arba DM sprendimas. Visi laukai neprivalomi,
 * neatsiustas (null) laukas nekeiciamas. Tai vienintele vieta, kurioje XP apskritai gali buti
 * paduotas is kliento - ir tik kartu su status.
 */
// Swagger pavyzdys - vien DM sprendimas: turinio ir perziuros laukai kartu niekam nepraeitu
// (turini keicia autorius, perziuri DM, kuris nera autorius).
@Schema(name = "LogEntryUpdateRequest", description = "Partial update of a log entry's content and review result",
        example = "{\"status\": \"APPROVED\", \"xpAwarded\": 50, \"reviewerNote\": \"Approved, even though the photo is blurry.\"}")
public record LogEntryUpdateRequest(

        @Schema(example = "Ran 8 km along the river.")
        @Size(max = 1000)
        String note,

        @Schema(example = "https://example.org/evidence/run-8km.png")
        @URL @Size(max = 500)
        String evidenceUrl,

        @Schema(description = "Cannot be in the future", example = "2026-09-19T15:20:00Z")
        Instant occurredAt,

        @Schema(description = "New log entry status (the game master's decision)", example = "APPROVED")
        LogStatus status,

        @Schema(description = "XP set by the game master; only together with status. When omitted on approval, "
                + "the quest's XP value is used.", example = "50")
        @PositiveOrZero
        Integer xpAwarded,

        @Schema(description = "Heroic inspiration awarded; only together with status, 0 when omitted",
                example = "1")
        @PositiveOrZero
        Integer inspirationAwarded,

        @Schema(description = "Game master's comment; only together with status", example = "Approved, even though the photo is blurry.")
        @Size(max = 500)
        String reviewerNote) {
}
