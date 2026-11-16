package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lt.ktu.donendusted.domain.UserRole;

/** PATCH - keiciami tik atsiusti laukai. */
// Swagger pavyzdys nekenksmingas: paspaudus "Execute" naudotojas neuzblokuojamas ir netampa ADMIN.
@Schema(name = "UserUpdateRequest", description = "Administratoriaus atliekamas naudotojo roles ar busenos keitimas",
        example = "{\"enabled\": true}")
public record UserUpdateRequest(
        @Schema(description = "Nauja globali role", example = "ADMIN") UserRole role,
        @Schema(description = "false - uzblokuoti paskyra (atsaukiamos visos sesijos)", example = "false") Boolean enabled) {
}
