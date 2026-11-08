package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TokenResponse", description = "Zetonu pora")
public record TokenResponse(

        @Schema(description = "JWT; siunciamas antrasteje Authorization: Bearer <accessToken>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJpc3MiOi...")
        String accessToken,

        @Schema(example = "Bearer") String tokenType,

        @Schema(description = "Po kiek sekundziu access zetonas nustos galioti", example = "900")
        long expiresIn,

        @Schema(description = "Vienkartinis zetonas naujai porai gauti per /api/auth/refresh",
                example = "q3Vd0n9mJk0E8dX1w6Zl8rTzW2a5bH7cYf4uN1sP0eI")
        String refreshToken) {
}
