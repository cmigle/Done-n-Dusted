package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lt.ktu.donendusted.domain.UserRole;

/** PATCH - keiciami tik atsiusti laukai. */
// Swagger pavyzdys nekenksmingas: paspaudus "Execute" naudotojas neuzblokuojamas ir netampa ADMIN.
@Schema(name = "UserUpdateRequest", description = "Role or status change made by an admin",
        example = "{\"enabled\": true}")
public record UserUpdateRequest(
        @Schema(description = "New global role", example = "ADMIN") UserRole role,
        @Schema(description = "false blocks the account (all sessions are revoked)", example = "false") Boolean enabled) {
}
