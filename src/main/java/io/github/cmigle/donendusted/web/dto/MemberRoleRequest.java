package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import io.github.cmigle.donendusted.domain.GuildRole;

@Schema(name = "MemberRoleRequest", description = "Member role change")
public record MemberRoleRequest(
        @Schema(example = "DUNGEON_MASTER") @NotNull GuildRole role) {
}
