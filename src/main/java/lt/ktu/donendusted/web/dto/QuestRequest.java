package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lt.ktu.donendusted.domain.Difficulty;
import lt.ktu.donendusted.domain.QuestType;
import lt.ktu.donendusted.domain.Stat;

@Schema(name = "QuestRequest", description = "Data for creating a quest")
public record QuestRequest(

        @Schema(example = "Run 50 kilometers")
        @NotBlank @Size(min = 3, max = 120)
        String title,

        @Schema(description = "Saved as \"\" when omitted",
                example = "A GRIND quest: every approved log entry brings you closer to the goal.")
        @Size(max = 1000)
        String description,

        @Schema(description = "Stat that the quest improves", example = "CON")
        @NotNull
        Stat stat,

        @Schema(example = "GRIND")
        @NotNull
        QuestType type,

        @Schema(description = "Multiplier of the base XP", example = "HARD")
        @NotNull
        Difficulty difficulty,

        @Schema(description = "Required for GRIND quests only; must be omitted for other types",
                example = "50")
        @Positive
        Integer targetCount,

        @Schema(description = "Whether the quest is still active; true when omitted", example = "true")
        Boolean active) {

    public boolean activeOrDefault() {
        return active == null || active;
    }
}
