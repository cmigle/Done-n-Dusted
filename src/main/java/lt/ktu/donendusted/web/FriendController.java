package lt.ktu.donendusted.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lt.ktu.donendusted.domain.Friendship;
import lt.ktu.donendusted.domain.FriendshipStatus;
import lt.ktu.donendusted.error.ApiError;
import lt.ktu.donendusted.security.CurrentUser;
import lt.ktu.donendusted.service.FriendshipService;
import lt.ktu.donendusted.web.dto.CharacterSheetResponse;
import lt.ktu.donendusted.web.dto.FriendRequest;
import lt.ktu.donendusted.web.dto.FriendshipResponse;
import lt.ktu.donendusted.web.dto.FriendshipUpdateRequest;
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
@Tag(name = "Friends", description = "Draugystes kvietimai ir draugu veikejo korteles")
public class FriendController {

    private final FriendshipService friends;

    public FriendController(FriendshipService friends) {
        this.friends = friends;
    }

    @GetMapping
    @Operation(summary = "Mano draugystes ir kvietimai",
            description = "Abiem kryptimis (OUTGOING ir INCOMING). Rikiuojama nuo naujausio kvietimo.")
    @ApiResponse(responseCode = "200", description = "Sarasas grazintas")
    public PagedResponse<FriendshipResponse> list(
            @Parameter(description = "Filtras pagal busena") @RequestParam(required = false) FriendshipStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long me = CurrentUser.get().id();
        return PagedResponse.from(friends.list(status, Sorting.allow(pageable, "createdAt", "status")),
                friendship -> FriendshipResponse.from(friendship, me));
    }

    @GetMapping("/{friendshipId}")
    @Operation(summary = "Viena draugyste")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Draugyste rasta"),
            @ApiResponse(responseCode = "404", description = "Draugystes nera arba ji ne jusu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FriendshipResponse get(@PathVariable Long friendshipId) {
        return FriendshipResponse.from(friends.get(friendshipId), CurrentUser.get().id());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Pakviesti i draugus", description = "Kvietimas sukuriamas PENDING busenoje.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Kvietimas issiustas"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Su siuo naudotoju draugyste ar kvietimas jau yra",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Tokio naudotojo nera arba kvieciamas pats save",
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
    @Operation(summary = "Priimti kvietima arba uzblokuoti",
            description = """
                    ACCEPTED - laukianti kvietima priima tik jo gavejas. BLOCKED - uzblokuoti gali bet kuri puse;
                    uzblokuotasis sios draugystes nebemato. Atmesti, atsaukti ar atblokuoti - DELETE.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Busena pakeista"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Savo issiusta kvietima priima ne siuntejas",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Draugystes nera arba ji ne jusu",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Negalimas busenos perejimas",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public FriendshipResponse update(@PathVariable Long friendshipId,
                                     @Valid @RequestBody FriendshipUpdateRequest request) {
        return FriendshipResponse.from(friends.update(friendshipId, request), CurrentUser.get().id());
    }

    @DeleteMapping("/{friendshipId}")
    @Operation(summary = "Atsaukti, atmesti, nutraukti arba atblokuoti",
            description = "Bet kuri puse; uzblokuota draugyste istrina (atblokuoja) tik uzblokaves naudotojas.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Istrinta; atsakymas be turinio"),
            @ApiResponse(responseCode = "404", description = "Draugystes nera arba ji ne jusu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long friendshipId) {
        friends.delete(friendshipId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{friendshipId}/sheet")
    @Operation(summary = "Draugo veikejo kortele",
            description = "Tik patvirtintai draugystei: PUBLIC pjuvis, sujungtas per visas draugo grupes.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Kortele grazinta"),
            @ApiResponse(responseCode = "403", description = "Draugyste dar nepatvirtinta arba uzblokuota",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Draugystes nera arba ji ne jusu",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public CharacterSheetResponse sheet(@PathVariable Long friendshipId) {
        return friends.friendSheet(friendshipId);
    }
}
