package lt.ktu.donendusted.repository;

import java.util.Optional;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LogEntryRepository extends JpaRepository<LogEntry, Long> {

    /** Tikrinama visa grandine guild -> quest -> log, kad neteisingas kelias grazintu 404. */
    Optional<LogEntry> findByIdAndQuestIdAndQuestGuildId(Long id, Long questId, Long guildId);

    @Query("""
            select l from LogEntry l
            where l.quest.id = :questId
              and (:status is null or l.status = :status)
            """)
    Page<LogEntry> search(@Param("questId") Long questId,
                          @Param("status") LogStatus status,
                          Pageable pageable);
}
