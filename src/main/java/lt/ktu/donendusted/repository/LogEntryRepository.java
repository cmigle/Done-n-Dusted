package lt.ktu.donendusted.repository;

import java.util.Optional;
import lt.ktu.donendusted.domain.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {

    /**
     * Tikrinama visa grandine guild -> quest -> log, kad neteisingas kelias grazintu 404.
     * SQL rasyti nereikia - Spring Data sugeneruoja uzklausa is metodo pavadinimo:
     * "QuestGuildId" reiskia logEntry.quest.guild.id, t.y. WHERE id=? AND quest_id=? AND quest.guild_id=?
     */
    Optional<LogEntry> findByIdAndQuestIdAndQuestGuildId(Long id, Long questId, Long guildId);
}
