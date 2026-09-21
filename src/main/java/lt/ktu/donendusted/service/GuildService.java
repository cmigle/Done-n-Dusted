package lt.ktu.donendusted.service;

import lt.ktu.donendusted.domain.Guild;
import lt.ktu.donendusted.error.ConflictException;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.GuildRepository;
import lt.ktu.donendusted.web.dto.GuildRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class GuildService {

    private final GuildRepository guilds;

    public GuildService(GuildRepository guilds) {
        this.guilds = guilds;
    }

    @Transactional(readOnly = true)
    public Page<Guild> list(String nameFilter, Pageable pageable) {
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
        return guilds.save(new Guild(request.name(), request.description()));
    }

    public Guild update(Long id, GuildRequest request) {
        Guild guild = get(id);
        if (guilds.existsByNameIgnoreCaseAndIdNot(request.name(), id)) {
            throw new ConflictException("Grupe pavadinimu '" + request.name() + "' jau egzistuoja");
        }
        guild.setName(request.name());
        guild.setDescription(request.description());
        return guild;
    }

    public void delete(Long id) {
        // Duomenu bazeje FK yra ON DELETE CASCADE, todel kartu dingsta uzduotys ir ju irasai.
        guilds.delete(get(id));
    }
}
