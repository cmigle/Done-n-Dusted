package lt.ktu.donendusted.service;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lt.ktu.donendusted.domain.Guild;
import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;
import lt.ktu.donendusted.domain.LogStatus;
import lt.ktu.donendusted.domain.Stat;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.GuildMembershipRepository;
import lt.ktu.donendusted.repository.LogEntryRepository;
import lt.ktu.donendusted.repository.StatTotal;
import lt.ktu.donendusted.web.dto.CharacterSheetResponse;
import lt.ktu.donendusted.web.dto.CharacterSheetResponse.RecentLog;
import lt.ktu.donendusted.web.dto.CharacterSheetResponse.StatScore;
import lt.ktu.donendusted.web.dto.CharacterSheetResponse.View;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Veikejo kortele (character sheet). Lygis, XP, charakteristikos, inspiration ir serija skaiciuojami tik is
 * patvirtintu (APPROVED) naudotojo irasu - DB uzklausomis (GROUP BY), irasai i atminti nekraunami.
 * FULL pjuvi mato pats naudotojas, grupes DM ir ADMIN; kiti grupes nariai - PUBLIC (be irasu ir laukianciu).
 */
@Service
@Transactional(readOnly = true)
public class CharacterSheetService {

    static final int XP_PER_LEVEL = 100;
    static final int BASE_SCORE = 10;
    static final int MAX_SCORE = 20;
    static final int XP_PER_SCORE_POINT = 50;
    static final int RECENT_LOGS = 10;

    private final GuildService guilds;
    private final GuildMembershipRepository memberships;
    private final LogEntryRepository logs;

    public CharacterSheetService(GuildService guilds, GuildMembershipRepository memberships, LogEntryRepository logs) {
        this.guilds = guilds;
        this.memberships = memberships;
        this.logs = logs;
    }

    /** Nario kortele grupeje: grupes nera - 404, ne narys ir ne ADMIN - 403, naudotojas ne grupes narys - 404. */
    public CharacterSheetResponse guildSheet(Long guildId, Long userId) {
        GuildAccess access = guilds.access(guildId);
        GuildMembership member = memberships.findWithUserByGuildIdAndUserId(guildId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Naudotojas " + userId + " nera grupes " + guildId + " narys"));
        boolean full = access.is(member.getUser()) || access.isDm() || access.isAdmin();
        return build(member.getUser(), access.guild(), member.getRole(), full);
    }

    /** Draugo kortele: tik PUBLIC pjuvis, sujungtas per visas naudotojo grupes. Teises tikrina kvieciantis servisas. */
    public CharacterSheetResponse publicSheet(User user) {
        return build(user, null, null, false);
    }

    /** @param guild null - visos grupes kartu */
    private CharacterSheetResponse build(User user, Guild guild, GuildRole role, boolean full) {
        Long userId = user.getId();
        Long guildId = guild == null ? null : guild.getId();

        Map<Stat, StatTotal> totals = logs.sumApprovedByStat(userId, guildId).stream()
                .collect(Collectors.toMap(StatTotal::stat, Function.identity()));
        // Visada visos 6 charakteristikos enum tvarka - ir tos, kurioms dar nera nei vieno iraso.
        List<StatScore> stats = Arrays.stream(Stat.values())
                .map(stat -> {
                    long xp = totals.containsKey(stat) ? totals.get(stat).xp() : 0;
                    return new StatScore(stat, score(xp), xp);
                })
                .toList();
        long totalXp = stats.stream().mapToLong(StatScore::xp).sum();
        long inspiration = totals.values().stream().mapToLong(StatTotal::inspiration).sum();
        int streak = streakDays(logs.findApprovedDays(userId, guildId), LocalDate.now(ZoneOffset.UTC));

        List<RecentLog> recent = null;
        Long pending = null;
        if (full) {
            recent = logs.findRecentApproved(userId, guildId, Limit.of(RECENT_LOGS)).stream()
                    .map(RecentLog::from)
                    .toList();
            pending = logs.countByAuthorIdAndQuestGuildIdAndStatus(userId, guildId, LogStatus.PENDING);
        }

        return new CharacterSheetResponse(
                userId,
                user.getUsername(),
                guildId,
                guild == null ? null : guild.getName(),
                role,
                full ? View.FULL : View.PUBLIC,
                (int) (totalXp / XP_PER_LEVEL) + 1,
                totalXp,
                (int) (totalXp % XP_PER_LEVEL),
                XP_PER_LEVEL,
                inspiration,
                streak,
                stats,
                recent,
                pending);
    }

    /** DnD charakteristika: pradine 10, +1 uz kas 50 XP, daugiausia 20. */
    static int score(long statXp) {
        return (int) Math.min(MAX_SCORE, BASE_SCORE + statXp / XP_PER_SCORE_POINT);
    }

    /**
     * Serija - kiek dienu is eiles (be tarpu) turi bent viena patvirtinta irasa. Ji gyva, jei paskutine tokia
     * diena yra siandien arba vakar: siandien dar galima speti uzregistruoti progresa.
     *
     * @param daysDesc skirtingos dienos nuo naujausios
     */
    static int streakDays(List<LocalDate> daysDesc, LocalDate today) {
        if (daysDesc.isEmpty() || daysDesc.getFirst().isBefore(today.minusDays(1))) {
            return 0;
        }
        int streak = 0;
        LocalDate expected = daysDesc.getFirst();
        for (LocalDate day : daysDesc) {
            if (!day.equals(expected)) {
                break;
            }
            streak++;
            expected = expected.minusDays(1);
        }
        return streak;
    }
}
