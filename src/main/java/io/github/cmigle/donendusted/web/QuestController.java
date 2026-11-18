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
import io.github.cmigle.donendusted.domain.Difficulty;
import io.github.cmigle.donendusted.domain.Quest;
import io.github.cmigle.donendusted.domain.QuestType;
import io.github.cmigle.donendusted.domain.Stat;
import io.github.cmigle.donendusted.error.ApiError;
import io.github.cmigle.donendusted.service.QuestService;
import io.github.cmigle.donendusted.web.dto.QuestRequest;
import io.github.cmigle.donendusted.web.dto.QuestResponse;
import io.github.cmigle.donendusted.web.dto.QuestUpdateRequest;
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

@RestController
@RequestMapping(path = "/api/guilds/{guildId}/quests", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Quests", description = "Quests, reachable only through their guild")
public class QuestController {

    private final QuestService quests;

    public QuestController(QuestService quests) {
        this.quests = quests;
    }

    @GetMapping
    @Operation(summary = "List a guild's quests",
            description = "Paged list with filters by stat, type, difficulty and active state.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned"),
            @ApiResponse(responseCode = "404", description = "Guild does not exist",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PagedResponse<QuestResponse> list(
            @PathVariable Long guildId,
            @Parameter(description = "Filter by the stat the quest improves") @RequestParam(required = false) Stat stat,
            @Parameter(description = "Filter by quest type") @RequestParam(required = false) QuestType type,
            @Parameter(description = "Filter by difficulty") @RequestParam(required = false) Difficulty difficulty,
            @Parameter(description = "Filter by active state") @RequestParam(required = false) Boolean active,
            @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable) {

        return PagedResponse.from(quests.list(guildId, stat, type, difficulty, active,
                Sorting.allow(pageable, "id", "title", "stat", "type", "difficulty", "targetCount", "active", "createdAt")),
                QuestResponse::from);
    }

    @GetMapping("/{questId}")
    @Operation(summary = "Get a quest",
            description = "Returns 404 if the quest does not belong to the given guild.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quest found"),
            @ApiResponse(responseCode = "404", description = "Wrong chain or quest not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public QuestResponse get(@PathVariable Long guildId, @PathVariable Long questId) {
        return QuestResponse.from(quests.get(guildId, questId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Create a quest")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Quest created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Guild does not exist",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "This title already exists in the guild",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violated (for example, GRIND without targetCount)",
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
    @Operation(summary = "Update a quest",
            description = """
                    Partial update: only the fields sent are changed. A difficulty change affects only future
                    log entries; the XP of already approved log entries stays the same.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Quest updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Wrong chain or quest not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "This title already exists in the guild",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violated",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public QuestResponse update(@PathVariable Long guildId, @PathVariable Long questId,
                                @Valid @RequestBody QuestUpdateRequest request) {
        return QuestResponse.from(quests.update(guildId, questId, request));
    }

    @DeleteMapping("/{questId}")
    @Operation(summary = "Delete a quest", description = "Also deletes all of its log entries.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted; no response body"),
            @ApiResponse(responseCode = "404", description = "Wrong chain or quest not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long guildId, @PathVariable Long questId) {
        quests.delete(guildId, questId);
        return ResponseEntity.noContent().build();
    }
}
