package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import io.github.cmigle.donendusted.domain.Guild;

@Schema(name = "GuildResponse", description = "Guild")
public record GuildResponse(

        @Schema(example = "1") Long id,
        @Schema(example = "Thursday Night Crew") String name,
        @Schema(example = "Campaign night every Thursday.") String description,
        @Schema(description = "User ID of the guild owner (creator)", example = "2") Long ownerId,
        @Schema(example = "2026-09-21T09:15:30.123Z") Instant createdAt) {

    // getOwner().getId() - LAZY proxy id zino be papildomos uzklausos i DB.
    public static GuildResponse from(Guild guild) {
        return new GuildResponse(guild.getId(), guild.getName(), guild.getDescription(),
                guild.getOwner().getId(), guild.getCreatedAt());
    }
}
