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
import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;
import lt.ktu.donendusted.error.ApiError;
import lt.ktu.donendusted.service.MembershipService;
import lt.ktu.donendusted.web.dto.MemberRequest;
import lt.ktu.donendusted.web.dto.MemberResponse;
import lt.ktu.donendusted.web.dto.MemberRoleRequest;
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
@Tag(name = "Members", description = "Grupes nariai ir ju roles (PLAYER / DUNGEON_MASTER)")
public class MemberController {

    private final MembershipService members;

    public MemberController(MembershipService members) {
        this.members = members;
    }

    @GetMapping
    @Operation(summary = "Grupes nariu sarasas", description = "Mato visi grupes nariai ir ADMIN.")
    @ApiResponse(responseCode = "200", description = "Sarasas grazintas")
    public PagedResponse<MemberResponse> list(
            @PathVariable Long guildId,
            @Parameter(description = "Filtras pagal role") @RequestParam(required = false) GuildRole role,
            @ParameterObject @PageableDefault(size = 20, sort = "joinedAt") Pageable pageable) {

        return PagedResponse.from(members.list(guildId, role, pageable), MemberResponse::from);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Vienas narys")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Narys rastas"),
            @ApiResponse(responseCode = "404", description = "Grupes nera arba naudotojas nera jos narys",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public MemberResponse get(@PathVariable Long guildId, @PathVariable Long userId) {
        return MemberResponse.from(members.get(guildId, userId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Prideti nari", description = "Tik grupes savininkas arba ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Narys pridetas"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Jau narys arba grupe jau turi DM",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Tokio naudotojo nera",
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
    @Operation(summary = "Pakeisti nario role", description = "Tik grupes savininkas arba ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role pakeista"),
            @ApiResponse(responseCode = "404", description = "Naudotojas nera grupes narys",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Grupe jau turi DM",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public MemberResponse changeRole(@PathVariable Long guildId, @PathVariable Long userId,
                                     @Valid @RequestBody MemberRoleRequest request) {
        return MemberResponse.from(members.changeRole(guildId, userId, request));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Pasalinti nari arba iseiti is grupes",
            description = "Savininkas ar ADMIN salina bet kuri nari, narys - pats save. Savininko pasalinti negalima.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pasalinta"),
            @ApiResponse(responseCode = "404", description = "Naudotojas nera grupes narys",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Bandoma pasalinti savininka",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> remove(@PathVariable Long guildId, @PathVariable Long userId) {
        members.remove(guildId, userId);
        return ResponseEntity.noContent().build();
    }
}
