package lt.ktu.donendusted.repository;

import java.util.List;
import java.util.Optional;
import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GuildMembershipRepository extends JpaRepository<GuildMembership, Long> {

    boolean existsByGuildIdAndUserId(Long guildId, Long userId);

    /**
     * Tik role, ne visas objektas: narystes entity neatsiduria Hibernate sesijoje, todel tos pacios
     * transakcijos metu istrinant grupe nelieka i ja rodancio "managed" objekto.
     */
    @Query("select m.role from GuildMembership m where m.guild.id = :guildId and m.user.id = :userId")
    Optional<GuildRole> findRole(Long guildId, Long userId);

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
