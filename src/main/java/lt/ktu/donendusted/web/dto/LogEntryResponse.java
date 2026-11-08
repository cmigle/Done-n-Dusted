package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;

@Schema(name = "LogEntryResponse", description = "Progreso irasas")
public record LogEntryResponse(

        @Schema(example = "7") Long id,
        @Schema(description = "Tevine uzduotis", example = "4") Long questId,
        @Schema(description = "Iraso autorius (naudotojo id)", example = "4") Long authorId,
        String note,
        String evidenceUrl,
        @Schema(example = "APPROVED") LogStatus status,

        @Schema(description = "Patvirtinimo metu uzsaldytas XP; PENDING ir REJECTED visada 0", example = "50")
        int xpAwarded,

        @Schema(example = "1") int inspirationAwarded,
        String reviewerNote,
        @Schema(description = "Perziurejes DM (naudotojo id); nera, kol irasas neperziuretas", example = "2")
        Long reviewerId,
        Instant occurredAt,
        @Schema(description = "Nera, kol irasas neperziuretas") Instant reviewedAt,
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
