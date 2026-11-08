package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lt.ktu.donendusted.domain.GuildRole;

@Schema(name = "MemberRoleRequest", description = "Nario roles keitimas")
public record MemberRoleRequest(
        @Schema(example = "DUNGEON_MASTER") @NotNull GuildRole role) {
}
