package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.domain.UserRole;

/** Slaptazodzio maisa niekada negrazinama. */
@Schema(name = "UserResponse", description = "User")
public record UserResponse(
        @Schema(example = "4") Long id,
        @Schema(example = "jonas") String username,
        @Schema(example = "USER") UserRole role,
        @Schema(example = "true") boolean enabled,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.isEnabled(),
                user.getCreatedAt());
    }
}
