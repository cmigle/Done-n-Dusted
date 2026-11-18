package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;

@Schema(name = "LogEntryResponse", description = "Log entry")
public record LogEntryResponse(

        @Schema(example = "7") Long id,
        @Schema(description = "Parent quest", example = "4") Long questId,
        @Schema(description = "Author of the log entry (user ID)", example = "4") Long authorId,
        String note,
        String evidenceUrl,
        @Schema(example = "APPROVED") LogStatus status,

        @Schema(description = "XP frozen at approval; always 0 for PENDING and REJECTED", example = "50")
        int xpAwarded,

        @Schema(example = "1") int inspirationAwarded,
        String reviewerNote,
        @Schema(description = "Game master who reviewed the log entry (user ID); absent until reviewed", example = "2")
        Long reviewerId,
        Instant occurredAt,
        @Schema(description = "Absent until the log entry is reviewed") Instant reviewedAt,
        Instant createdAt) {

    // Entity niekada negrazinamas tiesiai - DTO leidzia valdyti, ka mato klientas.
    // getQuest().getId() veikia ir uz transakcijos ribu: LAZY proxy id zino be papildomos uzklausos.
    public static LogEntryResponse from(LogEntry entry) {
        return new LogEntryResponse(
                entry.getId(),
                entry.getQuest().getId(),
                entry.getAuthor().getId(),
                entry.getNote(),
                entry.getEvidenceUrl(),
                entry.getStatus(),
                entry.getXpAwarded(),
                entry.getInspirationAwarded(),
                entry.getReviewerNote(),
                entry.getReviewer() == null ? null : entry.getReviewer().getId(),
                entry.getOccurredAt(),
                entry.getReviewedAt(),
                entry.getCreatedAt());
    }
}
