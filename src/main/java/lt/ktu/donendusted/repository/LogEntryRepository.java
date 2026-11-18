package lt.ktu.donendusted.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {

    /**
     * Tikrinama visa grandine guild -> quest -> log, kad neteisingas kelias grazintu 404.
     * SQL rasyti nereikia - Spring Data sugeneruoja uzklausa is metodo pavadinimo:
     * "QuestGuildId" reiskia logEntry.quest.guild.id, t.y. WHERE id=? AND quest_id=? AND quest.guild_id=?
     */
    Optional<LogEntry> findByIdAndQuestIdAndQuestGuildId(Long id, Long questId, Long guildId);

    // Veikejo kortele. Visur guildId == null reiskia "visose grupese" (draugo kortele).

    /** Patvirtintas XP ir inspiration pagal charakteristika - viena uzklausa, irasai i atminti nekraunami. */
    @Query("""
            select new lt.ktu.donendusted.repository.StatTotal(q.stat, sum(l.xpAwarded), sum(l.inspirationAwarded))
            from LogEntry l join l.quest q
            where l.author.id = :authorId
              and (:guildId is null or q.guild.id = :guildId)
              and l.status = lt.ktu.donendusted.domain.LogStatus.APPROVED
            group by q.stat
            """)
    List<StatTotal> sumApprovedByStat(Long authorId, Long guildId);

    /**
     * Skirtingos UTC dienos, kuriomis buvo bent vienas patvirtintas irasas, nuo naujausios. Native SQL, nes
     * timestamptz -> date priklauso nuo DB sesijos laiko juostos, o serija skaiciuojama UTC dienomis.
     */
    @Query(nativeQuery = true, value = """
            select distinct cast(l.occurred_at at time zone 'UTC' as date) as day
            from log_entry l
                     join quest q on q.id = l.quest_id
            where l.author_id = :authorId
              and (cast(:guildId as bigint) is null or q.guild_id = :guildId)
              and l.status = 'APPROVED'
            order by day desc
            """)
    List<LocalDate> findApprovedDays(Long authorId, Long guildId);

    /** Naujausi patvirtinti irasai; uzduotis uzkraunama ta pacia uzklausa (pavadinimas ir charakteristika). */
    @Query("""
            select l from LogEntry l join fetch l.quest q
            where l.author.id = :authorId
              and q.guild.id = :guildId
              and l.status = lt.ktu.donendusted.domain.LogStatus.APPROVED
            order by l.occurredAt desc, l.id desc
            """)
    List<LogEntry> findRecentApproved(Long authorId, Long guildId, Limit limit);

    long countByAuthorIdAndQuestGuildIdAndStatus(Long authorId, Long guildId, LogStatus status);
}
