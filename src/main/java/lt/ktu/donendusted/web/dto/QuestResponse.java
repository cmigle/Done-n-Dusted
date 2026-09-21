package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.Difficulty;
import lt.ktu.donendusted.domain.Quest;
import lt.ktu.donendusted.domain.QuestType;
import lt.ktu.donendusted.domain.Stat;

@Schema(name = "QuestResponse", description = "Uzduotis")
public record QuestResponse(

        @Schema(example = "4") Long id,
        @Schema(description = "Tevine grupe", example = "2") Long guildId,
        @Schema(example = "Run 50 kilometers") String title,
        String description,
        @Schema(example = "CON") Stat stat,
        @Schema(example = "GRIND") QuestType type,
        @Schema(example = "HARD") Difficulty difficulty,
        @Schema(example = "50") Integer targetCount,
        @Schema(example = "true") boolean active,

        @Schema(description = "Serverio isvestas XP uz viena patvirtinta irasa (BASE_XP * daugiklis)",
                example = "50")
        int xpPerCompletion,

        Instant createdAt) {

    public static QuestResponse from(Quest quest) {
        return new QuestResponse(
                quest.getId(),
                quest.getGuild().getId(),
                quest.getTitle(),
                quest.getDescription(),
                quest.getStat(),
                quest.getType(),
                quest.getDifficulty(),
                quest.getTargetCount(),
                quest.isActive(),
                quest.getDifficulty().xpValue(),
                quest.getCreatedAt());
    }
}
