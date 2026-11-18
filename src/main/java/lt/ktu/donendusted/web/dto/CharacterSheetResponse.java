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
@Schema(name = "CharacterSheetResponse", description = "Veikejo kortele: lygis, XP ir charakteristikos is patvirtintu irasu")
public record CharacterSheetResponse(

        @Schema(example = "4") Long userId,
        @Schema(example = "jonas") String username,

        @Schema(description = "Grupe; draugo kortelei (visos grupes kartu) nepateikiama", example = "2")
        Long guildId,

        @Schema(description = "Draugo kortelei nepateikiama", example = "Iron Vow")
        String guildName,

        @Schema(description = "Role grupeje; draugo kortelei nepateikiama", example = "PLAYER")
        GuildRole role,

        @Schema(description = "FULL - pats naudotojas, grupes DM arba ADMIN; PUBLIC - kiti grupes nariai ir draugai",
                example = "FULL")
        View view,

        @Schema(description = "totalXp / 100 + 1", example = "2")
        int level,

        @Schema(description = "Visu patvirtintu irasu XP suma", example = "100")
        long totalXp,

        @Schema(description = "totalXp % 100 - kiek XP jau surinkta dabartiniame lygyje", example = "0")
        int xpIntoLevel,

        @Schema(description = "Kiek XP reikia vienam lygiui (visada 100)", example = "100")
        int xpForNextLevel,

        @Schema(description = "Patvirtintu irasu inspiration suma", example = "1")
        long inspiration,

        @Schema(description = "Kiek UTC dienu is eiles, baigiant siandien arba vakar, turi bent viena patvirtinta "
                + "irasa (pagal occurredAt); kitaip 0", example = "3")
        int streakDays,

        @Schema(description = "Visada 6 charakteristikos tvarka STR, DEX, CON, INT, WIS, CHA")
        List<StatScore> stats,

        @Schema(description = "Tik FULL: 10 naujausiu patvirtintu irasu, naujausias pirmas")
        List<RecentLog> recentLogs,

        @Schema(description = "Tik FULL: kiek siu irasu dar laukia DM perziuros (PENDING)", example = "1")
        Long pendingCount) {

    public enum View {
        FULL, PUBLIC
    }

    @Schema(name = "StatScore", description = "Charakteristikos reiksme ir jai skirtas XP")
    public record StatScore(
            @Schema(example = "CON") Stat stat,
            @Schema(description = "min(20, 10 + xp / 50)", example = "12") int score,
            @Schema(example = "100") long xp) {
    }

    @Schema(name = "SheetRecentLog", description = "Patvirtintas irasas veikejo korteleje")
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
