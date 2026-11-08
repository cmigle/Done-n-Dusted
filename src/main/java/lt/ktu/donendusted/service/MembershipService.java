package lt.ktu.donendusted.service;

import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.error.BusinessRuleException;
import lt.ktu.donendusted.error.ConflictException;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.GuildMembershipRepository;
import lt.ktu.donendusted.repository.UserRepository;
import lt.ktu.donendusted.web.dto.MemberRequest;
import lt.ktu.donendusted.web.dto.MemberRoleRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Grupes nariai. Sarasa mato visi nariai, narius prideda ir roles keicia savininkas arba ADMIN.
 * Grupeje daugiausia vienas DM - bandymas paskirti antra grazina 409.
 */
@Service
@Transactional
public class MembershipService {

    private final GuildMembershipRepository memberships;
    private final GuildService guilds;
    private final UserRepository users;

    public MembershipService(GuildMembershipRepository memberships, GuildService guilds, UserRepository users) {
        this.memberships = memberships;
        this.guilds = guilds;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<GuildMembership> list(Long guildId, GuildRole role, Pageable pageable) {
        guilds.access(guildId);
        return role == null
                ? memberships.findByGuildId(guildId, pageable)
                : memberships.findByGuildIdAndRole(guildId, role, pageable);
    }

    @Transactional(readOnly = true)
    public GuildMembership get(Long guildId, Long userId) {
        guilds.access(guildId);
        return find(guildId, userId);
    }

    public GuildMembership add(Long guildId, MemberRequest request) {
        GuildAccess access = guilds.access(guildId);
        access.require(access.isOwner() || access.isAdmin(), "Narius prideti gali tik grupes savininkas arba administratorius");
        // Naudotojas nurodomas body, ne kelyje - jo nebuvimas yra turinio klaida (422), ne 404.
        User user = users.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BusinessRuleException("Naudotojas '" + request.username() + "' nerastas"));
        if (memberships.findByGuildIdAndUserId(guildId, user.getId()).isPresent()) {
            throw new ConflictException("Naudotojas '" + user.getUsername() + "' jau yra grupes narys");
        }
        if (request.role() == GuildRole.DUNGEON_MASTER) {
            requireNoDm(guildId);
        }
        return memberships.save(new GuildMembership(access.guild(), user, request.role()));
    }

    public GuildMembership changeRole(Long guildId, Long userId, MemberRoleRequest request) {
        GuildAccess access = guilds.access(guildId);
        access.require(access.isOwner() || access.isAdmin(), "Roles keisti gali tik grupes savininkas arba administratorius");
        GuildMembership membership = find(guildId, userId);
        if (request.role() == GuildRole.DUNGEON_MASTER && membership.getRole() != GuildRole.DUNGEON_MASTER) {
            requireNoDm(guildId);
        }
        membership.setRole(request.role());
        return membership;
    }

    /** Pasalinti nari gali savininkas ar ADMIN; pats narys - iseiti is grupes. */
    public void remove(Long guildId, Long userId) {
        GuildAccess access = guilds.access(guildId);
        access.require(access.isOwner() || access.isAdmin() || userId.equals(access.user().id()),
                "Pasalinti nari gali tik grupes savininkas arba administratorius");
        GuildMembership membership = find(guildId, userId);
        if (userId.equals(access.guild().getOwner().getId())) {
            throw new BusinessRuleException("Grupes savininko pasalinti negalima");
        }
        memberships.delete(membership);
    }

    private GuildMembership find(Long guildId, Long userId) {
        return memberships.findWithUserByGuildIdAndUserId(guildId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Naudotojas " + userId + " nera grupes " + guildId + " narys"));
    }

    private void requireNoDm(Long guildId) {
        if (memberships.existsByGuildIdAndRole(guildId, GuildRole.DUNGEON_MASTER)) {
            throw new ConflictException("Grupe jau turi DM - pirma pakeiskite esamo DM role i PLAYER");
        }
    }
}
