package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "FriendRequest", description = "Draugystes kvietimas")
public record FriendRequest(
        @Schema(description = "Esamo naudotojo vardas", example = "jonas") @NotBlank String username) {
}
