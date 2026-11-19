package io.github.cmigle.donendusted.repository;

import io.github.cmigle.donendusted.domain.Guild;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GuildRepository extends JpaRepository<Guild, Long>, JpaSpecificationExecutor<Guild> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
