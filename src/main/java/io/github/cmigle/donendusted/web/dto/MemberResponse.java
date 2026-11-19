package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import io.github.cmigle.donendusted.domain.GuildMembership;
import io.github.cmigle.donendusted.domain.GuildRole;

@Schema(name = "MemberResponse", description = "Guild member")
public record MemberResponse(
        @Schema(example = "4") Long userId,
        @Schema(example = "jonas") String username,
        @Schema(example = "PLAYER") GuildRole role,
        Instant joinedAt) {

    public static MemberResponse from(GuildMembership membership) {
        return new MemberResponse(membership.getUser().getId(), membership.getUser().getUsername(),
                membership.getRole(), membership.getJoinedAt());
    }
}
