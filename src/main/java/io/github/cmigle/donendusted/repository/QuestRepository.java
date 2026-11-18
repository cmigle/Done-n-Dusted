package io.github.cmigle.donendusted.repository;

import java.util.Optional;
import io.github.cmigle.donendusted.domain.Quest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuestRepository extends JpaRepository<Quest, Long>, JpaSpecificationExecutor<Quest> {

    /** Vaikinis objektas pasiekiamas tik per teva - todel visada ieskome ir pagal guildId. */
    Optional<Quest> findByIdAndGuildId(Long id, Long guildId);

    boolean existsByGuildIdAndTitleIgnoreCase(Long guildId, String title);

    boolean existsByGuildIdAndTitleIgnoreCaseAndIdNot(Long guildId, String title, Long id);
}
