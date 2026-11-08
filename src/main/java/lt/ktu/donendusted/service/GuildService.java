package lt.ktu.donendusted.service;

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

    @Transactional(readOnly = true)
    public Page<Guild> list(String nameFilter, Pageable pageable) {
        // Specification - dinamine WHERE salyga. Jei filtras nepaduotas, cb.conjunction()
        // yra tiesiog "true", t.y. grazinamos visos grupes. LIKE su lower() - paieska be raidziu dydzio.
        Specification<Guild> spec = (root, query, cb) -> StringUtils.hasText(nameFilter)
                ? cb.like(cb.lower(root.get("name")), "%" + nameFilter.toLowerCase() + "%")
                : cb.conjunction();
        return guilds.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Guild get(Long id) {
        return guilds.findById(id).orElseThrow(() -> ResourceNotFoundException.guild(id));
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
        Guild guild = get(id);
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
        // Duomenu bazeje FK yra ON DELETE CASCADE, todel kartu dingsta uzduotys ir ju irasai.
        guilds.delete(get(id));
    }
}
