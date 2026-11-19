package io.github.cmigle.donendusted.service;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import io.github.cmigle.donendusted.domain.Difficulty;
import io.github.cmigle.donendusted.domain.Guild;
import io.github.cmigle.donendusted.domain.Quest;
import io.github.cmigle.donendusted.domain.QuestType;
import io.github.cmigle.donendusted.domain.Stat;
import io.github.cmigle.donendusted.error.BusinessRuleException;
import io.github.cmigle.donendusted.error.ConflictException;
import io.github.cmigle.donendusted.error.ResourceNotFoundException;
import io.github.cmigle.donendusted.repository.QuestRepository;
import io.github.cmigle.donendusted.repository.UserRepository;
import io.github.cmigle.donendusted.security.CurrentUser;
import io.github.cmigle.donendusted.web.dto.QuestRequest;
import io.github.cmigle.donendusted.web.dto.QuestUpdateRequest;
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
        guilds.access(guildId); // grupes nera -> 404 (ne tuscias sarasas), ne narys -> 403
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
        return get(guilds.access(guildId), questId);
    }

    /** Uzduotis grupeje, kurios teises jau patikrintos. */
    @Transactional(readOnly = true)
    public Quest get(GuildAccess access, Long questId) {
        // Ieskome pagal abu id, todel /guilds/1/quests/5 grazins 404, jei quest 5 yra kitoje grupeje.
        Long guildId = access.guild().getId();
        return quests.findByIdAndGuildId(questId, guildId)
                .orElseThrow(() -> ResourceNotFoundException.quest(guildId, questId));
    }

    public Quest create(Long guildId, QuestRequest request) {
        GuildAccess access = guilds.access(guildId);
        access.require(access.isMember(), "Only guild members can create quests.");
        access.require(request.type() != QuestType.SIDE_QUEST || access.isDm(),
                "Only the game master can create SIDE_QUEST quests.");
        Guild guild = access.guild();
        validate(request.type(), request.targetCount());
        if (quests.existsByGuildIdAndTitleIgnoreCase(guildId, request.title())) {
            throw new ConflictException("Quest \"" + request.title() + "\" already exists in this guild.");
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
        GuildAccess access = guilds.access(guildId);
        Quest quest = get(access, questId);
        access.require(access.is(quest.getCreatedBy()) || access.isDm() || access.isOwner(),
                "Only the quest creator, the game master or the guild owner can edit this quest.");
        access.require(request.type() != QuestType.SIDE_QUEST || quest.getType() == QuestType.SIDE_QUEST
                || access.isDm(), "Only the game master can change a quest to SIDE_QUEST.");

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
                throw new ConflictException("Quest \"" + request.title() + "\" already exists in this guild.");
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
        GuildAccess access = guilds.access(guildId);
        Quest quest = get(access, questId);
        access.require(access.is(quest.getCreatedBy()) || access.isDm() || access.isOwner() || access.isAdmin(),
                "Only the quest creator, the game master, the guild owner or an admin can delete this quest.");
        quests.delete(quest);
    }

    /**
     * Taisykles, kuriu negalima isreiksti anotacijomis prie atskiro lauko - todel 422, o ne 400.
     */
    private void validate(QuestType type, Integer targetCount) {
        if (type == QuestType.GRIND && targetCount == null) {
            throw new BusinessRuleException("A GRIND quest requires targetCount.");
        }
        if (type != QuestType.GRIND && targetCount != null) {
            throw new BusinessRuleException("targetCount is only allowed for GRIND quests.");
        }
    }
}
