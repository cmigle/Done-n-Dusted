package lt.ktu.donendusted.repository;

import java.util.Optional;
import lt.ktu.donendusted.domain.Difficulty;
import lt.ktu.donendusted.domain.Quest;
import lt.ktu.donendusted.domain.QuestType;
import lt.ktu.donendusted.domain.Stat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestRepository extends JpaRepository<Quest, Long> {

    /** Vaikinis objektas pasiekiamas tik per teva - todel visada ieskome ir pagal guildId. */
    Optional<Quest> findByIdAndGuildId(Long id, Long guildId);

    boolean existsByGuildIdAndTitleIgnoreCase(Long guildId, String title);

    boolean existsByGuildIdAndTitleIgnoreCaseAndIdNot(Long guildId, String title, Long id);

    @Query("""
            select q from Quest q
            where q.guild.id = :guildId
              and (:stat is null or q.stat = :stat)
              and (:type is null or q.type = :type)
              and (:difficulty is null or q.difficulty = :difficulty)
              and (:active is null or q.active = :active)
            """)
    Page<Quest> search(@Param("guildId") Long guildId,
                       @Param("stat") Stat stat,
                       @Param("type") QuestType type,
                       @Param("difficulty") Difficulty difficulty,
                       @Param("active") Boolean active,
                       Pageable pageable);
}
