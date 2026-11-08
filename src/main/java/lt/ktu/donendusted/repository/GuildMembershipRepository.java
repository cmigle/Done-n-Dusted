package lt.ktu.donendusted.repository;

import java.util.List;
import java.util.Optional;
import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildMembershipRepository extends JpaRepository<GuildMembership, Long> {

    Optional<GuildMembership> findByGuildIdAndUserId(Long guildId, Long userId);

    boolean existsByGuildIdAndRole(Long guildId, GuildRole role);

    // @EntityGraph - naudotojas uzkraunamas ta pacia uzklausa (JOIN), nes atsakyme reikia jo vardo,
    // o LAZY rysio uz transakcijos ribu perskaityti nebeimanoma.
    @EntityGraph(attributePaths = "user")
    Optional<GuildMembership> findWithUserByGuildIdAndUserId(Long guildId, Long userId);

    @EntityGraph(attributePaths = "user")
    Page<GuildMembership> findByGuildId(Long guildId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<GuildMembership> findByGuildIdAndRole(Long guildId, GuildRole role, Pageable pageable);

    @EntityGraph(attributePaths = "guild")
    List<GuildMembership> findByUserIdOrderByGuildName(Long userId);
}
