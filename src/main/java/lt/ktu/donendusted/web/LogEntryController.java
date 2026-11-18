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
@Tag(name = "Log entries", description = "Log entries, reachable only through their guild and quest")
public class LogEntryController {

    private final LogEntryService logs;

    public LogEntryController(LogEntryService logs) {
        this.logs = logs;
    }

    @GetMapping
    @Operation(summary = "List a quest's log entries",
            description = "Paged list, filterable by status. Sorted from the most recent activity.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned"),
            @ApiResponse(responseCode = "404", description = "Wrong guild/quest chain",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public PagedResponse<LogEntryResponse> list(
            @PathVariable Long guildId,
            @PathVariable Long questId,
            @Parameter(description = "Filter by log entry status") @RequestParam(required = false) LogStatus status,
            @ParameterObject @PageableDefault(size = 20, sort = "occurredAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return PagedResponse.from(logs.list(guildId, questId, status,
                Sorting.allow(pageable, "id", "occurredAt", "createdAt", "status", "xpAwarded", "reviewedAt")),
                LogEntryResponse::from);
    }

    @GetMapping("/{logId}")
    @Operation(summary = "Get a log entry")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log entry found"),
            @ApiResponse(responseCode = "404", description = "Wrong chain or log entry not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LogEntryResponse get(@PathVariable Long guildId, @PathVariable Long questId,
                                @PathVariable Long logId) {
        return LogEntryResponse.from(logs.get(guildId, questId, logId));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Log progress",
            description = "A log entry is always created as PENDING with 0 XP; only the game master awards XP when approving it.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Log entry created"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Wrong chain",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Inactive quest or a date in the future",
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
    @Operation(summary = "Update a log entry and its review result",
            description = """
                    Partial update: only the fields sent are changed. xpAwarded, inspirationAwarded and
                    reviewerNote are accepted only together with status. On approval (APPROVED) an omitted xpAwarded
                    means the XP comes from the quest difficulty. PENDING and REJECTED log entries must
                    have no XP or inspiration.
                    """)
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Log entry updated"),
            @ApiResponse(responseCode = "400", description = "Invalid request body",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Wrong chain or log entry not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "422", description = "Business rule violated",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public LogEntryResponse update(@PathVariable Long guildId, @PathVariable Long questId,
                                   @PathVariable Long logId,
                                   @Valid @RequestBody LogEntryUpdateRequest request) {
        return LogEntryResponse.from(logs.update(guildId, questId, logId, request));
    }

    @DeleteMapping("/{logId}")
    @Operation(summary = "Delete a log entry")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted; no response body"),
            @ApiResponse(responseCode = "404", description = "Wrong chain or log entry not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long guildId, @PathVariable Long questId,
                                       @PathVariable Long logId) {
        logs.delete(guildId, questId, logId);
        return ResponseEntity.noContent().build();
    }
}
