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
import lt.ktu.donendusted.domain.Difficulty;
import lt.ktu.donendusted.domain.Quest;
import lt.ktu.donendusted.domain.QuestType;
import lt.ktu.donendusted.domain.Stat;
import lt.ktu.donendusted.error.ApiError;
import lt.ktu.donendusted.service.QuestService;
import lt.ktu.donendusted.web.dto.QuestRequest;
import lt.ktu.donendusted.web.dto.QuestResponse;
import lt.ktu.donendusted.web.dto.QuestUpdateRequest;
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

@RestController
@RequestMapping(path = "/api/guilds/{guildId}/quests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Quests", description = "Uzduotys - pasiekiamos tik per savo grupe")
public class QuestController {

    private final QuestService quests;

    public QuestController(QuestService quests) {
        this.quests = quests;
    }

    @GetMapping
    @Operation(summary = "Grupes uzduociu sarasas",
            description = "Puslapiuotas sarasas su filtrais pagal charakteristika, tipa, sudetinguma ir aktyvuma.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Sarasas grazintas"),
            @ApiResponse(responseCode = "404", description = "Grupe neegzistuoja",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PagedResponse<QuestResponse> list(
            @PathVariable Long guildId,
            @Parameter(description = "Filtras pagal auginama charakteristika") @RequestParam(required = false) Stat stat,
            @Parameter(description = "Filtras pagal uzduoties tipa") @RequestParam(required = false) QuestType type,
            @Parameter(description = "Filtras pagal sudetinguma") @RequestParam(required = false) Difficulty difficulty,
            @Parameter(description = "Filtras pagal aktyvuma") @RequestParam(required = false) Boolean active,
            @PageableDefault(size = 20, sort = "title") Pageable pageable) {

        return PagedResponse.from(quests.list(guildId, stat, type, difficulty, active, pageable),
                QuestResponse::from);
    }

    @GetMapping("/{questId}")
    @Operation(summary = "Viena uzduotis",
            description = "Grazina 404, jei uzduotis nepriklauso nurodytai grupei.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uzduotis rasta"),
            @ApiResponse(responseCode = "404", description = "Neteisinga grandine arba uzduoties nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public QuestResponse get(@PathVariable Long guildId, @PathVariable Long questId) {
        return QuestResponse.from(quests.get(guildId, questId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Sukurti uzduoti")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Uzduotis sukurta"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Grupe neegzistuoja",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Toks pavadinimas grupeje jau yra",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Pazeista dalykine taisykle (pvz., GRIND be targetCount)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<QuestResponse> create(@PathVariable Long guildId,
                                                @Valid @RequestBody QuestRequest request) {
        Quest created = quests.create(guildId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{questId}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(QuestResponse.from(created));
    }

    @PatchMapping(path = "/{questId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Atnaujinti uzduoti",
            description = """
                    Dalinis atnaujinimas: keiciami tik atsiusti laukai. Sudetingumo keitimas veikia tik busimus
                    irasus - jau patvirtintu irasu XP nekeiciamas.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Uzduotis atnaujinta"),
            @ApiResponse(responseCode = "400", description = "Neteisingas payload",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Neteisinga grandine arba uzduoties nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Toks pavadinimas grupeje jau yra",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Pazeista dalykine taisykle",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public QuestResponse update(@PathVariable Long guildId, @PathVariable Long questId,
                                @Valid @RequestBody QuestUpdateRequest request) {
        return QuestResponse.from(quests.update(guildId, questId, request));
    }

    @DeleteMapping("/{questId}")
    @Operation(summary = "Istrinti uzduoti", description = "Kartu istrinami visi jos progreso irasai.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Istrinta; atsakymas be turinio"),
            @ApiResponse(responseCode = "404", description = "Neteisinga grandine arba uzduoties nera",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long guildId, @PathVariable Long questId) {
        quests.delete(guildId, questId);
        return ResponseEntity.noContent().build();
    }
}
