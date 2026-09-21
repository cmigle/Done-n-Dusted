package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.Guild;

@Schema(name = "GuildResponse", description = "Grupe")
public record GuildResponse(

        @Schema(example = "1") Long id,
        @Schema(example = "Thursday Night Crew") String name,
        @Schema(example = "Ketvirtadieniais renkasi kampanija.") String description,
        @Schema(example = "2026-09-21T09:15:30.123Z") Instant createdAt) {

    public static GuildResponse from(Guild guild) {
        return new GuildResponse(guild.getId(), guild.getName(), guild.getDescription(), guild.getCreatedAt());
    }
}
