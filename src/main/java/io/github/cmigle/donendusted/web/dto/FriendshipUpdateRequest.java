package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import io.github.cmigle.donendusted.domain.FriendshipStatus;

/** Atmesti, atsaukti, nutraukti ar atblokuoti - ne PATCH, o DELETE /api/friends/{id}. */
@Schema(name = "FriendshipUpdateRequest", description = "Reply to a friend request, or a block")
public record FriendshipUpdateRequest(
        @Schema(description = "ACCEPTED accepts the friend request (recipient only); BLOCKED blocks the other user (either side)",
                example = "ACCEPTED")
        @NotNull
        FriendshipStatus status) {
}
