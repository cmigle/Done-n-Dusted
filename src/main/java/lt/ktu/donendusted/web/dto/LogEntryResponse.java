package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;

@Schema(name = "LogEntryResponse", description = "Progreso irasas")
public record LogEntryResponse(

        @Schema(example = "7") Long id,
        @Schema(description = "Tevine uzduotis", example = "4") Long questId,
        String note,
        String evidenceUrl,
        @Schema(example = "APPROVED") LogStatus status,

        @Schema(description = "Patvirtinimo metu uzsaldytas XP; PENDING ir REJECTED visada 0", example = "50")
        int xpAwarded,

        @Schema(example = "1") int inspirationAwarded,
        String reviewerNote,
        Instant occurredAt,
        @Schema(description = "Nera, kol irasas neperziuretas") Instant reviewedAt,
        Instant createdAt) {

    public static LogEntryResponse from(LogEntry entry) {
        return new LogEntryResponse(
                entry.getId(),
                entry.getQuest().getId(),
                entry.getNote(),
                entry.getEvidenceUrl(),
                entry.getStatus(),
                entry.getXpAwarded(),
                entry.getInspirationAwarded(),
                entry.getReviewerNote(),
                entry.getOccurredAt(),
                entry.getReviewedAt(),
                entry.getCreatedAt());
    }
}
