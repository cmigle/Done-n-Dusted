package lt.ktu.donendusted.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

@Schema(name = "RegisterRequest", description = "Naujo naudotojo registracija")
public record RegisterRequest(

        @Schema(description = "Unikalus vardas (raidziu dydis nesvarbus)", example = "naujokas")
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_.-]{3,30}", message = "3-30 simboliu: raides, skaiciai, _ . -")
        String username,

        @Schema(description = "8-72 simboliai (daugiausia 72 baitai UTF-8)", example = "Demo123!")
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {

    /** BCrypt riba - 72 baitai, ne simboliai: lietuviska raide UTF-8 uzima 2 baitus. Ilgesnio BCrypt neuzsifruotu. */
    @JsonIgnore
    @AssertTrue(message = "slaptazodis per ilgas - daugiausia 72 baitai (lietuviskos raides uzima po 2)")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
