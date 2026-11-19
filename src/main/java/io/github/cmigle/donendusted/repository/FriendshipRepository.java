package io.github.cmigle.donendusted.repository;

import java.util.Optional;
import io.github.cmigle.donendusted.domain.Friendship;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface FriendshipRepository extends JpaRepository<Friendship, Long>, JpaSpecificationExecutor<Friendship> {

    // Abu naudotojai uzkraunami ta pacia uzklausa: atsakyme reikia kitos puses vardo.
    @Override
    @EntityGraph(attributePaths = {"requester", "addressee"})
    Page<Friendship> findAll(Specification<Friendship> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "addressee"})
    Optional<Friendship> findWithUsersById(Long id);

    /** Ar pora jau turi eilute bet kuria kryptimi ir bet kokia busena. */
    @Query("""
            select count(f) > 0 from Friendship f
            where (f.requester.id = :userId and f.addressee.id = :otherId)
               or (f.requester.id = :otherId and f.addressee.id = :userId)
            """)
    boolean existsBetween(Long userId, Long otherId);
}
