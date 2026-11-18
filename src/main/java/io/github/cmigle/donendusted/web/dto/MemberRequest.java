package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.github.cmigle.donendusted.domain.GuildRole;

@Schema(name = "MemberRequest", description = "New guild member")
public record MemberRequest(
        @Schema(description = "Username of an existing user", example = "jonas") @NotBlank String username,
        @Schema(example = "PLAYER") @NotNull GuildRole role) {
}
