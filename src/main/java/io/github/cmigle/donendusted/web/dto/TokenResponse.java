package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse", description = "Token pair")
public record TokenResponse(

        @Schema(description = "JWT; send it in the header Authorization: Bearer <accessToken>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOi...")
        String accessToken,

        @Schema(example = "Bearer") String tokenType,

        @Schema(description = "Seconds until the access token expires", example = "900")
        long expiresIn,

        @Schema(description = "Single-use token for getting a new pair from /api/auth/refresh",
                example = "q3Vd0n9mJk0E8dX1w6Zl8rTzW2a5bH7cYf4uN1sP0eI")
        String refreshToken) {
}
