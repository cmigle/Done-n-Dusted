package lt.ktu.donendusted.repository;

import lt.ktu.donendusted.domain.Guild;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GuildRepository extends JpaRepository<Guild, Long> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /** Neprivalomas filtras: kai :name yra null, grazinamos visos grupes. */
    @Query("""
            select g from Guild g
            where (:name is null or lower(g.name) like lower(concat('%', :name, '%')))
            """)
    Page<Guild> search(@Param("name") String name, Pageable pageable);
}
