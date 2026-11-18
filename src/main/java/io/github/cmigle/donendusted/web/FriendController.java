package io.github.cmigle.donendusted.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import io.github.cmigle.donendusted.domain.Friendship;
import io.github.cmigle.donendusted.domain.FriendshipStatus;
import io.github.cmigle.donendusted.error.ApiError;
import io.github.cmigle.donendusted.security.CurrentUser;
import io.github.cmigle.donendusted.service.FriendshipService;
import io.github.cmigle.donendusted.web.dto.CharacterSheetResponse;
import io.github.cmigle.donendusted.web.dto.FriendRequest;
import io.github.cmigle.donendusted.web.dto.FriendshipResponse;
import io.github.cmigle.donendusted.web.dto.FriendshipUpdateRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** Prisijungusio naudotojo draugystes. Svetima draugyste - 404, ne 403: jos buvimas neatskleidziamas. */
@RestController
@RequestMapping(path = "/api/friends", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Friends", description = "Friend requests and friends' character sheets")
public class FriendController {

    private final FriendshipService friends;

    public FriendController(FriendshipService friends) {
        this.friends = friends;
    }

    @GetMapping
    @Operation(summary = "My friendships and friend requests",
            description = "Both directions (OUTGOING and INCOMING). Sorted from the newest friend request.")
    @ApiResponse(responseCode = "200", description = "List returned")
    public PagedResponse<FriendshipResponse> list(
            @Parameter(description = "Filter by status") @RequestParam(required = false) FriendshipStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long me = CurrentUser.get().id();
        return PagedResponse.from(friends.list(status, Sorting.allow(pageable, "createdAt", "status")),
                friendship -> FriendshipResponse.from(friendship, me));
    }

    @GetMapping("/{friendshipId}")
    @Operation(summary = "Get a friendship")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Friendship found"),
            @ApiResponse(responseCode = "404", description = "Friendship not found or not yours",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FriendshipResponse get(@PathVariable Long friendshipId) {
        return FriendshipResponse.from(friends.get(friendshipId), CurrentUser.get().id());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Send a friend request", description = "The friend request is created with status PENDING.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Friend request sent"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "A friendship or friend request with this user already exists",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "No such user, or the user invited themselves",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<FriendshipResponse> request(@Valid @RequestBody FriendRequest request) {
        Friendship created = friends.request(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{friendshipId}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(FriendshipResponse.from(created, CurrentUser.get().id()));
    }

    @PatchMapping(path = "/{friendshipId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Accept a friend request or block",
            description = """
                    ACCEPTED: only the recipient can accept a pending friend request. BLOCKED: either side can block;
                    the blocked user no longer sees this friendship. To decline, cancel or unblock, use DELETE.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "The sender cannot accept their own friend request",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Friendship not found or not yours",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Status transition not allowed",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FriendshipResponse update(@PathVariable Long friendshipId,
                                     @Valid @RequestBody FriendshipUpdateRequest request) {
        return FriendshipResponse.from(friends.update(friendshipId, request), CurrentUser.get().id());
    }

    @DeleteMapping("/{friendshipId}")
    @Operation(summary = "Cancel, decline, unfriend or unblock",
            description = "Either side; a blocked friendship can only be deleted (unblocked) by the user who blocked it.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted; no response body"),
            @ApiResponse(responseCode = "404", description = "Friendship not found or not yours",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long friendshipId) {
        friends.delete(friendshipId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{friendshipId}/sheet")
    @Operation(summary = "Friend's character sheet",
            description = "Accepted friendships only: the PUBLIC view, combined across all of the friend's guilds.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Character sheet returned"),
            @ApiResponse(responseCode = "403", description = "Friendship not accepted yet, or blocked",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Friendship not found or not yours",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public CharacterSheetResponse sheet(@PathVariable Long friendshipId) {
        return friends.friendSheet(friendshipId);
    }
}
