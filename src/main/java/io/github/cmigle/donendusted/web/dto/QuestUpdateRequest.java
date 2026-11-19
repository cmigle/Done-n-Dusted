package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import io.github.cmigle.donendusted.domain.Difficulty;
import io.github.cmigle.donendusted.domain.QuestType;
import io.github.cmigle.donendusted.domain.Stat;

/** PATCH - dalinis atnaujinimas: visi laukai neprivalomi, neatsiustas (null) laukas nekeiciamas. */
@Schema(name = "QuestUpdateRequest", description = "Partial quest update: only the fields sent are changed")
public record QuestUpdateRequest(

        @Schema(example = "Run 50 kilometers")
        @Pattern(regexp = ".*\\S.*", message = "Must not be blank")
        @Size(min = 3, max = 120)
        String title,

        @Schema(description = "New description; unchanged when omitted, \"\" clears it",
                example = "A GRIND quest: every approved log entry brings you closer to the goal.")
        @Size(max = 1000)
        String description,

        @Schema(example = "CON")
        Stat stat,

        @Schema(description = "Changing to a non-GRIND type clears targetCount", example = "GRIND")
        QuestType type,

        @Schema(example = "HARD")
        Difficulty difficulty,

        @Schema(description = "Allowed for GRIND quests only", example = "50")
        @Positive
        Integer targetCount,

        @Schema(example = "false")
        Boolean active) {
}
