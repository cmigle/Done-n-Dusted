package lt.ktu.donendusted.repository;

import java.util.Optional;
import lt.ktu.donendusted.domain.GuildMembership;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuildMembershipRepository extends JpaRepository<GuildMembership, Long> {

    Optional<GuildMembership> findByGuildIdAndUserId(Long guildId, Long userId);
}
