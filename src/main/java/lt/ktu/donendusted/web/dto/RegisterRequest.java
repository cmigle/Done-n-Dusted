package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(name = "RegisterRequest", description = "Naujo naudotojo registracija")
public record RegisterRequest(

        @Schema(description = "Unikalus vardas (raidziu dydis nesvarbus)", example = "naujokas")
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_.-]{3,30}", message = "3-30 simboliu: raides, skaiciai, _ . -")
        String username,

        // 72 - BCrypt riba: ilgesnio slaptazodzio galas butu tiesiog ignoruojamas.
        @Schema(description = "8-72 simboliai", example = "Demo123!")
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {
}
