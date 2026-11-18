package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "FriendRequest", description = "Friend request")
public record FriendRequest(
        @Schema(description = "Username of an existing user", example = "jonas") @NotBlank String username) {
}
