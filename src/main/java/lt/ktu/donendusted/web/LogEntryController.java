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
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;
import lt.ktu.donendusted.error.ApiError;
import lt.ktu.donendusted.service.LogEntryService;
import lt.ktu.donendusted.web.dto.LogEntryCreateRequest;
import lt.ktu.donendusted.web.dto.LogEntryResponse;
import lt.ktu.donendusted.web.dto.LogEntryUpdateRequest;
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

@RestController
@RequestMapping(path = "/api/guilds/{guildId}/quests/{questId}/logs", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Log entries", description = "Progreso irasai - pasiekiami tik per grupe ir uzduoti")
public class LogEntryController {

    private final LogEntryService logs;

    public LogEntryController(LogEntryService logs) {
        this.logs = logs;
    }

    @GetMapping
    @Operation(summary = "Uzduoties irasu sarasas",
            description = "Puslapiuotas sarasas; filtruojamas pagal busena. Rikiuojama nuo naujausio ivykio.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sarasas grazintas"),
            @ApiResponse(responseCode = "404", description = "Neteisinga grupes/uzduoties grandine",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PagedResponse<LogEntryResponse> list(
            @PathVariable Long guildId,
            @PathVariable Long questId,
            @Parameter(description = "Filtras pagal iraso busena") @RequestParam(required = false) LogStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return PagedResponse.from(logs.list(guildId, questId, status, pageable), LogEntryResponse::from);
    }

    @GetMapping("/{logId}")
    @Operation(summary = "Vienas irasas")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Irasas rastas"),
            @ApiResponse(responseCode = "404", description = "Neteisinga grandine arba iraso nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LogEntryResponse get(@PathVariable Long guildId, @PathVariable Long questId,
                                @PathVariable Long logId) {
        return LogEntryResponse.from(logs.get(guildId, questId, logId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Registruoti progresa",
            description = "Irasas visada sukuriamas PENDING busenoje su 0 XP - taskus skiria tik DM tvirtindamas.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Irasas sukurtas"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Neteisinga grandine",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Neaktyvi uzduotis arba data ateityje",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<LogEntryResponse> create(@PathVariable Long guildId, @PathVariable Long questId,
                                                   @Valid @RequestBody LogEntryCreateRequest request) {
        LogEntry created = logs.create(guildId, questId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{logId}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(LogEntryResponse.from(created));
    }

    @PatchMapping(path = "/{logId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atnaujinti irasa ir jo perziuros rezultata",
            description = """
                    Dalinis atnaujinimas: keiciami tik atsiusti laukai. xpAwarded, inspirationAwarded ir
                    reviewerNote priimami tik kartu su status. Tvirtinant (APPROVED) praleistas xpAwarded
                    reiskia, kad XP imamas is uzduoties sudetingumo. PENDING ir REJECTED irasai privalo
                    likti be XP ir inspiration.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Irasas atnaujintas"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Neteisinga grandine arba iraso nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Pazeista dalykine taisykle",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LogEntryResponse update(@PathVariable Long guildId, @PathVariable Long questId,
                                   @PathVariable Long logId,
                                   @Valid @RequestBody LogEntryUpdateRequest request) {
        return LogEntryResponse.from(logs.update(guildId, questId, logId, request));
    }

    @DeleteMapping("/{logId}")
    @Operation(summary = "Istrinti irasa")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Istrinta; atsakymas be turinio"),
            @ApiResponse(responseCode = "404", description = "Neteisinga grandine arba iraso nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long guildId, @PathVariable Long questId,
                                       @PathVariable Long logId) {
        logs.delete(guildId, questId, logId);
        return ResponseEntity.noContent().build();
    }
}
