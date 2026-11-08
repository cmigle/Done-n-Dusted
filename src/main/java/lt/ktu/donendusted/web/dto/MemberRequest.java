package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lt.ktu.donendusted.domain.GuildRole;

@Schema(name = "MemberRequest", description = "Naujas grupes narys")
public record MemberRequest(
        @Schema(description = "Esamo naudotojo vardas", example = "jonas") @NotBlank String username,
        @Schema(example = "PLAYER") @NotNull GuildRole role) {
}
