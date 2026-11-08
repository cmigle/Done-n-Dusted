package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.domain.UserRole;

/** Prisijunges naudotojas ir jo grupes - klientas is karto zino, kur jis DM, o kur zaidejas. */
@Schema(name = "MeResponse", description = "Prisijunges naudotojas su narystemis")
public record MeResponse(
        @Schema(example = "2") Long id,
        @Schema(example = "rokas") String username,
        @Schema(example = "USER") UserRole role,
        @Schema(example = "true") boolean enabled,
        Instant createdAt,
        List<Membership> memberships) {

    @Schema(name = "MeMembership")
    public record Membership(
            @Schema(example = "2") Long guildId,
            @Schema(example = "Iron Vow") String guildName,
            @Schema(example = "DUNGEON_MASTER") GuildRole role) {
    }

    public static MeResponse from(User user, List<GuildMembership> memberships) {
        return new MeResponse(user.getId(), user.getUsername(), user.getRole(), user.isEnabled(), user.getCreatedAt(),
                memberships.stream()
                        .map(m -> new Membership(m.getGuild().getId(), m.getGuild().getName(), m.getRole()))
                        .toList());
    }
}
