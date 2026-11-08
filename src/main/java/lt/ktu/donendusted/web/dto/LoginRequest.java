package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "LoginRequest", description = "Prisijungimo duomenys")
public record LoginRequest(
        @Schema(example = "jonas") @NotBlank String username,
        @Schema(example = "Demo123!") @NotBlank String password) {
}
