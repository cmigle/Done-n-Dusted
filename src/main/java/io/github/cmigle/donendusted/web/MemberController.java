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
import io.github.cmigle.donendusted.domain.GuildMembership;
import io.github.cmigle.donendusted.domain.GuildRole;
import io.github.cmigle.donendusted.error.ApiError;
import io.github.cmigle.donendusted.service.CharacterSheetService;
import io.github.cmigle.donendusted.service.MembershipService;
import io.github.cmigle.donendusted.web.dto.CharacterSheetResponse;
import io.github.cmigle.donendusted.web.dto.MemberRequest;
import io.github.cmigle.donendusted.web.dto.MemberResponse;
import io.github.cmigle.donendusted.web.dto.MemberRoleRequest;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
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

/** Narys identifikuojamas naudotojo id grupes viduje: /api/guilds/{guildId}/members/{userId}. */
@RestController
@RequestMapping(path = "/api/guilds/{guildId}/members", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Members", description = "Guild members and their roles (PLAYER / DUNGEON_MASTER)")
public class MemberController {

    private final MembershipService members;
    private final CharacterSheetService sheets;

    public MemberController(MembershipService members, CharacterSheetService sheets) {
        this.members = members;
        this.sheets = sheets;
    }

    @GetMapping
    @Operation(summary = "List guild members", description = "Visible to all guild members and ADMIN.")
    @ApiResponse(responseCode = "200", description = "List returned")
    public PagedResponse<MemberResponse> list(
            @PathVariable Long guildId,
            @Parameter(description = "Filter by role") @RequestParam(required = false) GuildRole role,
            @ParameterObject @PageableDefault(size = 20, sort = "joinedAt") Pageable pageable) {

        return PagedResponse.from(members.list(guildId, role, Sorting.allow(pageable, "joinedAt", "role")),
                MemberResponse::from);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get a member")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Member found"),
            @ApiResponse(responseCode = "404", description = "Guild not found, or the user is not a member",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public MemberResponse get(@PathVariable Long guildId, @PathVariable Long userId) {
        return MemberResponse.from(members.get(guildId, userId));
    }

    @GetMapping("/{userId}/sheet")
    @Operation(summary = "Member's character sheet",
            description = """
                    Level, XP, stats, inspiration and streak, only from the member's approved (APPROVED) log entries
                    in this guild. The FULL view (with the 10 latest log entries and the pending count) is for the member,
                    the guild's game master and ADMIN; other guild members get PUBLIC.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Character sheet returned"),
            @ApiResponse(responseCode = "404", description = "Guild not found, or the user is not a member",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public CharacterSheetResponse sheet(@PathVariable Long guildId, @PathVariable Long userId) {
        return sheets.guildSheet(guildId, userId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Add a member", description = "Guild owner or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Member added"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Already a member, or the guild already has a game master",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "No such user",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<MemberResponse> add(@PathVariable Long guildId, @Valid @RequestBody MemberRequest request) {
        GuildMembership created = members.add(guildId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{userId}")
                .buildAndExpand(created.getUser().getId())
                .toUri();
        return ResponseEntity.created(location).body(MemberResponse.from(created));
    }

    @PatchMapping(path = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Change a member's role", description = "Guild owner or ADMIN only.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role changed"),
            @ApiResponse(responseCode = "404", description = "User is not a member of the guild",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "The guild already has a game master",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public MemberResponse changeRole(@PathVariable Long guildId, @PathVariable Long userId,
                                     @Valid @RequestBody MemberRoleRequest request) {
        return MemberResponse.from(members.changeRole(guildId, userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Remove a member or leave the guild",
            description = "The owner or ADMIN can remove any member, and a member can remove themselves. The owner cannot be removed.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "User is not a member of the guild",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Attempt to remove the owner",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> remove(@PathVariable Long guildId, @PathVariable Long userId) {
        members.remove(guildId, userId);
        return ResponseEntity.noContent().build();
    }
}
