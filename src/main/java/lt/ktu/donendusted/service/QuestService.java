package lt.ktu.donendusted.service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lt.ktu.donendusted.domain.Difficulty;
import lt.ktu.donendusted.domain.Guild;
import lt.ktu.donendusted.domain.Quest;
import lt.ktu.donendusted.domain.QuestType;
import lt.ktu.donendusted.domain.Stat;
import lt.ktu.donendusted.error.BusinessRuleException;
import lt.ktu.donendusted.error.ConflictException;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.QuestRepository;
import lt.ktu.donendusted.repository.UserRepository;
import lt.ktu.donendusted.security.CurrentUser;
import lt.ktu.donendusted.web.dto.QuestRequest;
import lt.ktu.donendusted.web.dto.QuestUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class QuestService {

    private final QuestRepository quests;
    private final GuildService guilds;
    private final UserRepository users;

    public QuestService(QuestRepository quests, GuildService guilds, UserRepository users) {
        this.quests = quests;
        this.guilds = guilds;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<Quest> list(Long guildId, Stat stat, QuestType type, Difficulty difficulty,
                            Boolean active, Pageable pageable) {
        guilds.get(guildId); // neegzistuojanti grupe -> 404, o ne tuscias sarasas
        // Kiekvienas nepaduotas filtras (null) tiesiog nepridedamas, o visi likusieji sujungiami AND.
        // Taip vienas metodas aptarnauja bet kokia filtru kombinacija be atskiru uzklausu kiekvienai.
        Specification<Quest> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("guild").get("id"), guildId));
            if (stat != null) {
                predicates.add(cb.equal(root.get("stat"), stat));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return quests.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Quest get(Long guildId, Long questId) {
        // Ieskome pagal abu id, todel /guilds/1/quests/5 grazins 404, jei quest 5 yra kitoje grupeje.
        // Jei nerado - lambda dar patikrina grupe: kai nera pacios grupes, guilds.get() ismes
        // "Guild X nerasta", ir klientas suzinos tikslesne priezasti nei "Quest nerasta".
        return quests.findByIdAndGuildId(questId, guildId)
                .orElseThrow(() -> {
                    guilds.get(guildId); // jei nera ir grupes, pranesame apie ja
                    return ResourceNotFoundException.quest(guildId, questId);
                });
    }

    public Quest create(Long guildId, QuestRequest request) {
        Guild guild = guilds.get(guildId);
        validate(request.type(), request.targetCount());
        if (quests.existsByGuildIdAndTitleIgnoreCase(guildId, request.title())) {
            throw new ConflictException("Grupeje jau yra uzduotis pavadinimu '" + request.title() + "'");
        }
        return quests.save(new Quest(guild, request.title(), request.description(), request.stat(),
                request.type(), request.difficulty(), request.targetCount(), request.activeOrDefault(),
                users.getReferenceById(CurrentUser.get().id())));
    }

    /**
     * PATCH: keiciami tik atsiusti (ne null) laukai. GRIND/targetCount taisykle tikrinama galutinei
     * busenai - atsiustos reiksmes sujungiamos su esamomis.
     */
    public Quest update(Long guildId, Long questId, QuestUpdateRequest request) {
        Quest quest = get(guildId, questId);

        QuestType type = request.type() != null ? request.type() : quest.getType();
        Integer targetCount;
        if (request.targetCount() != null) {
            targetCount = request.targetCount();
        } else if (type == QuestType.GRIND) {
            targetCount = quest.getTargetCount();
        } else {
            targetCount = null; // tipas nebe GRIND - tikslo skaicius nebeturi prasmes, todel isvalomas
        }
        validate(type, targetCount);

        if (request.title() != null) {
            // "IdNot" - pavadinimas gali sutapti su pacia atnaujinama uzduotimi, bet ne su kita.
            if (quests.existsByGuildIdAndTitleIgnoreCaseAndIdNot(guildId, request.title(), questId)) {
                throw new ConflictException("Grupeje jau yra uzduotis pavadinimu '" + request.title() + "'");
            }
            quest.setTitle(request.title());
        }
        if (request.description() != null) {
            quest.setDescription(request.description());
        }
        if (request.stat() != null) {
            quest.setStat(request.stat());
        }
        // Keiciant difficulty jau patvirtintu irasu XP nesikeicia - jis uzsaldytas LogEntry.
        if (request.difficulty() != null) {
            quest.setDifficulty(request.difficulty());
        }
        if (request.active() != null) {
            quest.setActive(request.active());
        }
        quest.setType(type);
        quest.setTargetCount(targetCount);
        return quest;
    }

    public void delete(Long guildId, Long questId) {
        quests.delete(get(guildId, questId));
    }

    /**
     * Taisykles, kuriu negalima isreiksti anotacijomis prie atskiro lauko - todel 422, o ne 400.
     */
    private void validate(QuestType type, Integer targetCount) {
        if (type == QuestType.GRIND && targetCount == null) {
            throw new BusinessRuleException("GRIND tipo uzduotis privalo tureti targetCount");
        }
        if (type != QuestType.GRIND && targetCount != null) {
            throw new BusinessRuleException("targetCount leidziamas tik GRIND tipo uzduotims");
        }
    }
}
