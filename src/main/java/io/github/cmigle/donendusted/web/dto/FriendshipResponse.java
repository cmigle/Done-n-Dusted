package io.github.cmigle.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import io.github.cmigle.donendusted.domain.Friendship;
import io.github.cmigle.donendusted.domain.FriendshipStatus;
import io.github.cmigle.donendusted.domain.User;

/** Draugyste prisijungusio naudotojo akimis: userId ir username - visada kita puse. */
@Schema(name = "FriendshipResponse", description = "Friendship or friend request")
public record FriendshipResponse(
        @Schema(example = "1") Long id,
        @Schema(description = "ID of the other user", example = "4") Long userId,
        @Schema(description = "Username of the other user", example = "jonas") String username,
        @Schema(example = "ACCEPTED") FriendshipStatus status,

        @Schema(description = "OUTGOING if the current user sent the friend request (for BLOCKED, did the blocking); "
                + "INCOMING if the other user did", example = "OUTGOING")
        Direction direction,

        Instant createdAt,
        @Schema(description = "When the friend request was accepted or blocked; absent while PENDING") Instant respondedAt) {

    public enum Direction {
        OUTGOING, INCOMING
    }

    public static FriendshipResponse from(Friendship friendship, Long currentUserId) {
        User other = friendship.other(currentUserId);
        return new FriendshipResponse(friendship.getId(), other.getId(), other.getUsername(), friendship.getStatus(),
                friendship.isRequester(currentUserId) ? Direction.OUTGOING : Direction.INCOMING,
                friendship.getCreatedAt(), friendship.getRespondedAt());
    }
}
