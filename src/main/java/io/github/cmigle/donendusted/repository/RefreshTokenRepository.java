package io.github.cmigle.donendusted.repository;

import java.time.Instant;
import java.util.Optional;
import io.github.cmigle.donendusted.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Atsaukia visus dar galiojancius naudotojo zetonus - visos jo sesijos baigiasi. */
    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.user.id = :userId and t.revokedAt is null")
    int revokeAllForUser(Long userId, Instant now);
}
