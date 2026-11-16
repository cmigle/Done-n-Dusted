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
@Schema(name = "LogEntryUpdateRequest", description = "Dalinis iraso turinio ir perziuros rezultato atnaujinimas",
        example = "{\"status\": \"APPROVED\", \"xpAwarded\": 50, \"reviewerNote\": \"Uzskaitau, nors nuotrauka neryski.\"}")
public record LogEntryUpdateRequest(

        @Schema(example = "Begau 8 km palei upe.")
        @Size(max = 1000)
        String note,

        @Schema(example = "https://example.org/evidence/run-8km.png")
        @URL @Size(max = 500)
        String evidenceUrl,

        @Schema(description = "Negali buti ateityje", example = "2026-09-19T15:20:00Z")
        Instant occurredAt,

        @Schema(description = "Nauja iraso busena (DM sprendimas)", example = "APPROVED")
        LogStatus status,

        @Schema(description = "DM perrasytas XP ivertis; tik kartu su status. Praleidus, tvirtinant "
                + "naudojama uzduoties verte.", example = "50")
        @PositiveOrZero
        Integer xpAwarded,

        @Schema(description = "Skiriamas heroic inspiration; tik kartu su status, praleidus laikoma 0",
                example = "1")
        @PositiveOrZero
        Integer inspirationAwarded,

        @Schema(description = "DM komentaras; tik kartu su status", example = "Uzskaitau, nors nuotrauka neryski.")
        @Size(max = 500)
        String reviewerNote) {
}
