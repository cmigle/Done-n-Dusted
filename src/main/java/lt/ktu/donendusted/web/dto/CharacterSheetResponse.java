package lt.ktu.donendusted.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lt.ktu.donendusted.domain.GuildRole;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.Stat;

/**
 * Veikejo kortele. Viskas skaiciuojama tik is patvirtintu (APPROVED) irasu. PUBLIC pjuvyje recentLogs ir
 * pendingCount yra null, todel i JSON neirasomi; draugo kortelei (per visas grupes) nera ir grupes lauku.
 */
@Schema(name = "CharacterSheetResponse", description = "Character sheet: level, XP and stats from approved log entries")
public record CharacterSheetResponse(

        @Schema(example = "4") Long userId,
        @Schema(example = "jonas") String username,

        @Schema(description = "Guild; omitted on a friend's character sheet (all guilds combined)", example = "2")
        Long guildId,

        @Schema(description = "Omitted on a friend's character sheet", example = "Iron Vow")
        String guildName,

        @Schema(description = "Role in the guild; omitted on a friend's character sheet", example = "PLAYER")
        GuildRole role,

        @Schema(description = "FULL for the user, the guild's game master or ADMIN; PUBLIC for other guild members and friends",
                example = "FULL")
        View view,

        @Schema(description = "totalXp / 100 + 1", example = "2")
        int level,

        @Schema(description = "Total XP of all approved log entries", example = "100")
        long totalXp,

        @Schema(description = "totalXp % 100: XP earned in the current level", example = "0")
        int xpIntoLevel,

        @Schema(description = "XP needed per level (always 100)", example = "100")
        int xpForNextLevel,

        @Schema(description = "Total inspiration of approved log entries", example = "1")
        long inspiration,

        @Schema(description = "Consecutive UTC days, ending today or yesterday, with at least one approved log entry "
                + "(by occurredAt); otherwise 0", example = "3")
        int streakDays,

        @Schema(description = "Always the 6 stats in the order STR, DEX, CON, INT, WIS, CHA")
        List<StatScore> stats,

        @Schema(description = "FULL only: the 10 latest approved log entries, newest first")
        List<RecentLog> recentLogs,

        @Schema(description = "FULL only: how many log entries still wait for the game master's review (PENDING)", example = "1")
        Long pendingCount) {

    public enum View {
        FULL, PUBLIC
    }

    @Schema(name = "StatScore", description = "Stat score and the XP earned for it")
    public record StatScore(
            @Schema(example = "CON") Stat stat,
            @Schema(description = "min(20, 10 + xp / 50)", example = "12") int score,
            @Schema(example = "100") long xp) {
    }

    @Schema(name = "SheetRecentLog", description = "Approved log entry on the character sheet")
    public record RecentLog(
            @Schema(example = "5") Long logId,
            @Schema(example = "4") Long questId,
            @Schema(example = "Run 50 kilometers") String questTitle,
            @Schema(example = "CON") Stat stat,
            @Schema(example = "50") int xpAwarded,
            @Schema(example = "0") int inspirationAwarded,
            @Schema(example = "2026-09-13T07:00:00Z") Instant occurredAt,
            @Schema(example = "2026-09-14T08:30:00Z") Instant reviewedAt) {

        public static RecentLog from(LogEntry entry) {
            return new RecentLog(entry.getId(), entry.getQuest().getId(), entry.getQuest().getTitle(),
                    entry.getQuest().getStat(), entry.getXpAwarded(), entry.getInspirationAwarded(),
                    entry.getOccurredAt(), entry.getReviewedAt());
        }
    }
}
