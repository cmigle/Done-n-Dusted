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
import lt.ktu.donendusted.web.dto.QuestRequest;
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

    public QuestService(QuestRepository quests, GuildService guilds) {
        this.quests = quests;
        this.guilds = guilds;
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
        validate(request);
        if (quests.existsByGuildIdAndTitleIgnoreCase(guildId, request.title())) {
            throw new ConflictException("Grupeje jau yra uzduotis pavadinimu '" + request.title() + "'");
        }
        return quests.save(new Quest(guild, request.title(), request.description(), request.stat(),
                request.type(), request.difficulty(), request.targetCount(), request.activeOrDefault()));
    }

    public Quest update(Long guildId, Long questId, QuestRequest request) {
        Quest quest = get(guildId, questId);
        validate(request);
        // "IdNot" - pavadinimas gali sutapti su pacia atnaujinama uzduotimi, bet ne su kita.
        // Keiciant difficulty jau patvirtintu irasu XP nesikeicia - jis uzsaldytas LogEntry.
        if (quests.existsByGuildIdAndTitleIgnoreCaseAndIdNot(guildId, request.title(), questId)) {
            throw new ConflictException("Grupeje jau yra uzduotis pavadinimu '" + request.title() + "'");
        }
        quest.setTitle(request.title());
        quest.setDescription(request.description());
        quest.setStat(request.stat());
        quest.setType(request.type());
        quest.setDifficulty(request.difficulty());
        quest.setTargetCount(request.targetCount());
        quest.setActive(request.activeOrDefault());
        return quest;
    }

    public void delete(Long guildId, Long questId) {
        quests.delete(get(guildId, questId));
    }

    /**
     * Taisykles, kuriu negalima isreiksti anotacijomis prie atskiro lauko - todel 422, o ne 400.
     */
    private void validate(QuestRequest request) {
        if (request.type() == QuestType.GRIND && request.targetCount() == null) {
            throw new BusinessRuleException("GRIND tipo uzduotis privalo tureti targetCount");
        }
        if (request.type() != QuestType.GRIND && request.targetCount() != null) {
            throw new BusinessRuleException("targetCount leidziamas tik GRIND tipo uzduotims");
        }
    }
}
