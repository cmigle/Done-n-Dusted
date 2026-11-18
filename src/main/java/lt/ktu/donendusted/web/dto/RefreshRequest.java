package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "RefreshRequest", description = "Refresh token from login or from the previous refresh")
public record RefreshRequest(
        @Schema(example = "q3Vd0n9mJk0E8dX1w6Zl8rTzW2a5bH7cYf4uN1sP0eI") @NotBlank String refreshToken) {
}
