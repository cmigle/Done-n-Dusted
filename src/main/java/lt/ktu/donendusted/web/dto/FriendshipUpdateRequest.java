package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lt.ktu.donendusted.domain.FriendshipStatus;

/** Atmesti, atsaukti, nutraukti ar atblokuoti - ne PATCH, o DELETE /api/friends/{id}. */
@Schema(name = "FriendshipUpdateRequest", description = "Atsakymas i kvietima arba uzblokavimas")
public record FriendshipUpdateRequest(
        @Schema(description = "ACCEPTED - priimti kvietima (tik gavejas); BLOCKED - uzblokuoti (bet kuri puse)",
                example = "ACCEPTED")
        @NotNull
        FriendshipStatus status) {
}
