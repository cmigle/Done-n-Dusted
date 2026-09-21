package lt.ktu.donendusted.repository;

import java.util.Optional;
import lt.ktu.donendusted.domain.LogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long>, JpaSpecificationExecutor<LogEntry> {

    /** Tikrinama visa grandine guild -> quest -> log, kad neteisingas kelias grazintu 404. */
    Optional<LogEntry> findByIdAndQuestIdAndQuestGuildId(Long id, Long questId, Long guildId);
}
