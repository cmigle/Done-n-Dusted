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
import lt.ktu.donendusted.domain.Guild;
import lt.ktu.donendusted.error.ApiError;
import lt.ktu.donendusted.service.GuildService;
import lt.ktu.donendusted.web.dto.GuildRequest;
import lt.ktu.donendusted.web.dto.GuildResponse;
import lt.ktu.donendusted.web.dto.GuildUpdateRequest;
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

/**
 * Kontroleris tik priima HTTP uzklausa ir grazina atsakyma - jokios logikos cia nera.
 * Taisykles yra servise, o klaidos virsta ApiError per ApiExceptionHandler.
 * produces = JSON: jei klientas prasys Accept: application/xml, gaus 406.
 */
@RestController
@RequestMapping(path = "/api/guilds", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Guilds", description = "Grupes - virsutinis hierarchijos lygis")
public class GuildController {

    private final GuildService guilds;

    public GuildController(GuildService guilds) {
        this.guilds = guilds;
    }

    @GetMapping
    @Operation(summary = "Grupiu sarasas",
            description = "Puslapiuotas ir filtruojamas sarasas. Nuorodose islieka visi filtrai.")
    @ApiResponse(responseCode = "200", description = "Sarasas grazintas")
    public PagedResponse<GuildResponse> list(
            @Parameter(description = "Filtras pagal pavadinimo dali, neatsizvelgiant i raidziu dydi")
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20, sort = "name") Pageable pageable) {

        return PagedResponse.from(guilds.list(name, pageable), GuildResponse::from);
    }

    @GetMapping("/{guildId}")
    @Operation(summary = "Viena grupe")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grupe rasta"),
            @ApiResponse(responseCode = "404", description = "Grupe neegzistuoja",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public GuildResponse get(@PathVariable Long guildId) {
        return GuildResponse.from(guilds.get(guildId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sukurti grupe")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Grupe sukurta; Location antraste nurodo nauja resursa"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Toks pavadinimas jau uzimtas",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    // consumes = JSON: kitoks Content-Type -> 415. @Valid paleidzia DTO anotacijas (@NotBlank ir pan.),
    // nepraejus - MethodArgumentNotValidException -> 400 su lauku sarasu.
    public ResponseEntity<GuildResponse> create(@Valid @RequestBody GuildRequest request) {
        Guild created = guilds.create(request);
        // REST: 201 Created + Location antraste su naujo resurso adresu, pvz. /api/guilds/4.
        // fromCurrentRequest() paima dabartini URL (host, port, kelia), prie jo pridedamas id.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{guildId}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(GuildResponse.from(created));
    }

    // PATCH, ne PUT: keiciami tik atsiusti laukai. Su PUT neatsiustas aprasymas butu perrasytas null
    // ir dingtu is atsakymo.
    @PatchMapping(path = "/{guildId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atnaujinti grupe",
            description = "Dalinis atnaujinimas: keiciami tik atsiusti laukai, neatsiusti lieka nepakeisti.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Grupe atnaujinta"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Grupe neegzistuoja",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Toks pavadinimas jau uzimtas",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public GuildResponse update(@PathVariable Long guildId, @Valid @RequestBody GuildUpdateRequest request) {
        return GuildResponse.from(guilds.update(guildId, request));
    }

    @DeleteMapping("/{guildId}")
    @Operation(summary = "Istrinti grupe",
            description = "Kartu istrinamos visos grupes uzduotys ir ju progreso irasai.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Istrinta; atsakymas be turinio"),
            @ApiResponse(responseCode = "404", description = "Grupe neegzistuoja",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long guildId) {
        guilds.delete(guildId);
        return ResponseEntity.noContent().build();
    }
}
