package lt.ktu.donendusted.service;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import lt.ktu.donendusted.domain.Guild;
import lt.ktu.donendusted.domain.GuildMembership;
import lt.ktu.donendusted.domain.GuildRole;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.error.ConflictException;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.GuildMembershipRepository;
import lt.ktu.donendusted.repository.GuildRepository;
import lt.ktu.donendusted.repository.UserRepository;
import lt.ktu.donendusted.security.CurrentUser;
import lt.ktu.donendusted.web.dto.GuildRequest;
import lt.ktu.donendusted.web.dto.GuildUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * @Transactional ant klases: kiekvienas viesas metodas vyksta vienoje transakcijoje.
 * Jei metodo viduje ismetama RuntimeException (pvz. ConflictException), viskas atsaukiama.
 * Skaitymo metodai pazymeti readOnly = true - Hibernate tada netikrina objektu pakeitimu.
 */
@Service
@Transactional
public class GuildService {

    private final GuildRepository guilds;
    private final GuildMembershipRepository memberships;
    private final UserRepository users;

    public GuildService(GuildRepository guilds, GuildMembershipRepository memberships, UserRepository users) {
        this.guilds = guilds;
        this.memberships = memberships;
        this.users = users;
    }

    /** ADMIN mato visas grupes, kiti naudotojai - tik tas, kuriu nariai yra. */
    @Transactional(readOnly = true)
    public Page<Guild> list(String nameFilter, Pageable pageable) {
        CurrentUser me = CurrentUser.get();
        // Specification - dinamine WHERE salyga is atskiru daliu, sujungiamu AND.
        // LIKE su lower() - paieska be raidziu dydzio.
        Specification<Guild> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StringUtils.hasText(nameFilter)) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + nameFilter.toLowerCase() + "%"));
            }
            if (!me.admin()) {
                // WHERE guild.id IN (SELECT guild_id FROM guild_membership WHERE user_id = :me)
                Subquery<Long> myGuilds = query.subquery(Long.class);
                Root<GuildMembership> m = myGuilds.from(GuildMembership.class);
                myGuilds.select(m.get("guild").get("id")).where(cb.equal(m.get("user").get("id"), me.id()));
                predicates.add(root.get("id").in(myGuilds));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return guilds.findAll(spec, pageable);
    }

    /**
     * Grupe kartu su prisijungusio naudotojo teisemis joje. Visi grupes ir jos vaiku veiksmai prasideda cia:
     * grupes nera - 404, naudotojas ne narys ir ne ADMIN - 403.
     */
    @Transactional(readOnly = true)
    public GuildAccess access(Long guildId) {
        Guild guild = guilds.findById(guildId).orElseThrow(() -> ResourceNotFoundException.guild(guildId));
        CurrentUser me = CurrentUser.get();
        GuildRole role = memberships.findRole(guildId, me.id()).orElse(null);
        GuildAccess access = new GuildAccess(guild, me, role);
        access.require(access.isMember() || access.isAdmin(), "Nesate grupes " + guildId + " narys");
        return access;
    }

    @Transactional(readOnly = true)
    public Guild get(Long id) {
        return access(id).guild();
    }

    public Guild create(GuildRequest request) {
        if (guilds.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Grupe pavadinimu '" + request.name() + "' jau egzistuoja");
        }
        // Kurejas tampa savininku ir grupes DM.
        User owner = users.getReferenceById(CurrentUser.get().id());
        Guild guild = guilds.save(new Guild(request.name(), request.description(), owner));
        memberships.save(new GuildMembership(guild, owner, GuildRole.DUNGEON_MASTER));
        return guild;
    }

    /** PATCH: null laukas reiskia "neatsiustas" - tokia reiksme paliekama kokia buvo. */
    public Guild update(Long id, GuildUpdateRequest request) {
        GuildAccess access = access(id);
        access.require(access.isOwner(), "Grupe keisti gali tik jos savininkas");
        Guild guild = access.guild();
        // save() kviesti nereikia: guild yra "managed" objektas (gautas toje pacioje transakcijoje),
        // todel Hibernate pats pastebi pakeitimus (dirty checking) ir transakcijos pabaigoje daro UPDATE.
        if (request.name() != null) {
            if (guilds.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
                throw new ConflictException("Grupe pavadinimu '" + request.name() + "' jau egzistuoja");
            }
            guild.setName(request.name());
        }
        if (request.description() != null) {
            guild.setDescription(request.description());
        }
        return guild;
    }

    public void delete(Long id) {
        GuildAccess access = access(id);
        access.require(access.isOwner() || access.isAdmin(), "Grupe istrinti gali tik jos savininkas arba administratorius");
        // Duomenu bazeje FK yra ON DELETE CASCADE, todel kartu dingsta nariai, uzduotys ir ju irasai.
        guilds.delete(access.guild());
    }
}
