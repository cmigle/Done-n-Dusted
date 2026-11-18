package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.Friendship;
import lt.ktu.donendusted.domain.FriendshipStatus;
import lt.ktu.donendusted.domain.User;

/** Draugyste prisijungusio naudotojo akimis: userId ir username - visada kita puse. */
@Schema(name = "FriendshipResponse", description = "Draugyste arba kvietimas")
public record FriendshipResponse(
        @Schema(example = "1") Long id,
        @Schema(description = "Kito naudotojo id", example = "4") Long userId,
        @Schema(description = "Kito naudotojo vardas", example = "jonas") String username,
        @Schema(example = "ACCEPTED") FriendshipStatus status,

        @Schema(description = "OUTGOING - kvietima siunte (BLOCKED - uzblokavo) prisijunges naudotojas; "
                + "INCOMING - kitas naudotojas", example = "OUTGOING")
        Direction direction,

        Instant createdAt,
        @Schema(description = "Kada kvietimas priimtas ar uzblokuotas; nera, kol PENDING") Instant respondedAt) {

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
