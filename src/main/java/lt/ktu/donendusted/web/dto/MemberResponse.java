package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;

@Schema(name = "MemberResponse", description = "Grupes narys")
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
