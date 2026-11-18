package io.github.cmigle.donendusted.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

@Schema(name = "RegisterRequest", description = "New user registration")
public record RegisterRequest(

        @Schema(description = "Unique username (case-insensitive)", example = "naujokas")
        @NotBlank
        @Pattern(regexp = "[A-Za-z0-9_.-]{3,30}", message = "Must be 3-30 characters: letters, digits, _ . -")
        String username,

        @Schema(description = "8-72 characters (at most 72 bytes in UTF-8)", example = "Demo123!")
        @NotBlank
        @Size(min = 8, max = 72)
        String password) {

    /** BCrypt riba - 72 baitai, ne simboliai: lietuviska raide UTF-8 uzima 2 baitus. Ilgesnio BCrypt neuzsifruotu. */
    @JsonIgnore
    @AssertTrue(message = "Password is too long: at most 72 bytes (non-ASCII letters take 2 bytes each)")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
