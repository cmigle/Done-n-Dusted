package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lt.ktu.donendusted.domain.Difficulty;
import lt.ktu.donendusted.domain.QuestType;
import lt.ktu.donendusted.domain.Stat;

@Schema(name = "QuestRequest", description = "Uzduoties kurimo duomenys")
public record QuestRequest(

        @Schema(example = "Run 50 kilometers")
        @NotBlank @Size(min = 3, max = 120)
        String title,

        @Schema(description = "Nenurodzius issaugomas tuscias \"\"",
                example = "Kaupiamoji uzduotis - kiekvienas patvirtintas irasas priartina prie tikslo.")
        @Size(max = 1000)
        String description,

        @Schema(description = "Charakteristika, kuria uzduotis augina", example = "CON")
        @NotNull
        Stat stat,

        @Schema(example = "GRIND")
        @NotNull
        QuestType type,

        @Schema(description = "Bazinio XP daugiklis", example = "HARD")
        @NotNull
        Difficulty difficulty,

        @Schema(description = "Privalomas tik GRIND tipui; kitiems tipams turi buti nenurodytas",
                example = "50")
        @Positive
        Integer targetCount,

        @Schema(description = "Ar uzduotis vis dar aktyvi; nenurodzius laikoma true", example = "true")
        Boolean active) {

    public boolean activeOrDefault() {
        return active == null || active;
    }
}
