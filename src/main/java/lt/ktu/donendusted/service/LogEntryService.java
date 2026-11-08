package lt.ktu.donendusted.service;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;
import lt.ktu.donendusted.domain.Quest;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.error.BusinessRuleException;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.LogEntryRepository;
import lt.ktu.donendusted.repository.UserRepository;
import lt.ktu.donendusted.web.dto.LogEntryCreateRequest;
import lt.ktu.donendusted.web.dto.LogEntryUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Matomumas (LD0): zaidejas mato tik savo irasus, grupes DM ir ADMIN - visus.
 * Tvirtina tik grupes DM ir niekada ne savo iraso.
 */
@Service
@Transactional
public class LogEntryService {

    private final LogEntryRepository logs;
    private final QuestService quests;
    private final GuildService guilds;
    private final UserRepository users;

    public LogEntryService(LogEntryRepository logs, QuestService quests, GuildService guilds, UserRepository users) {
        this.logs = logs;
        this.quests = quests;
        this.guilds = guilds;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<LogEntry> list(Long guildId, Long questId, LogStatus status, Pageable pageable) {
        GuildAccess access = guilds.access(guildId);
        quests.get(access, questId); // patvirtina visa tevine grandine
        boolean seesAll = access.isDm() || access.isAdmin();
        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("quest").get("id"), questId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (!seesAll) {
                predicates.add(cb.equal(root.get("author").get("id"), access.user().id()));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return logs.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public LogEntry get(Long guildId, Long questId, Long logId) {
        GuildAccess access = guilds.access(guildId);
        LogEntry entry = find(access, questId, logId);
        access.require(access.isDm() || access.isAdmin() || access.is(entry.getAuthor()),
                "Svetimus irasus mato tik grupes DM");
        return entry;
    }

    public LogEntry create(Long guildId, Long questId, LogEntryCreateRequest request) {
        GuildAccess access = guilds.access(guildId);
        access.require(access.isMember(), "Progresa registruoti gali tik grupes nariai");
        Quest quest = quests.get(access, questId);
        requireNotInFuture(request.occurredAt());
        if (!quest.isActive()) {
            throw new BusinessRuleException("Uzduotis neaktyvi - nauju irasu prie jos registruoti negalima");
        }
        // XP ir busena cia nenustatomi: konstruktorius palieka PENDING ir 0 tasku.
        User author = users.getReferenceById(access.user().id());
        return logs.save(new LogEntry(quest, author, request.note(), request.evidenceUrl(), request.occurredAt()));
    }

    /**
     * PATCH - dalinis atnaujinimas: keiciami tik atsiusti (ne null) laukai.
     * Turini (note, evidenceUrl, occurredAt) keicia tik autorius ir tik kol irasas PENDING.
     * Perziuros laukus (status, xpAwarded, inspirationAwarded, reviewerNote) keicia tik grupes DM, ne autorius.
     * Busenu perejimai atliekami per LogEntry metodus approve/reject/resetToPending, o ne per setterius -
     * taip XP niekada negali atsidurti ne APPROVED irase.
     */
    public LogEntry update(Long guildId, Long questId, Long logId, LogEntryUpdateRequest request) {
        GuildAccess access = guilds.access(guildId);
        LogEntry entry = find(access, questId, logId);
        Quest quest = entry.getQuest(); // reikia sudetingumo XP apskaiciavimui

        boolean contentSent = request.note() != null || request.evidenceUrl() != null || request.occurredAt() != null;
        boolean reviewSent = request.status() != null || request.xpAwarded() != null
                || request.inspirationAwarded() != null || request.reviewerNote() != null;

        if (contentSent) {
            access.require(access.is(entry.getAuthor()), "Iraso turini keisti gali tik jo autorius");
            if (entry.getStatus() != LogStatus.PENDING) {
                throw new BusinessRuleException("Perziureto iraso turinio keisti negalima - DM pirma turi grazinti ji i PENDING");
            }
        }
        if (reviewSent) {
            access.require(access.isDm(), "Irasus perziuri tik grupes DM");
            access.require(!access.is(entry.getAuthor()), "Savo iraso tvirtinti negalima");
        }

        if (request.note() != null) {
            entry.setNote(request.note());
        }
        if (request.evidenceUrl() != null) {
            entry.setEvidenceUrl(request.evidenceUrl());
        }
        if (request.occurredAt() != null) {
            requireNotInFuture(request.occurredAt());
            entry.setOccurredAt(request.occurredAt());
        }

        if (request.status() == null) {
            if (reviewSent) {
                throw new BusinessRuleException("xpAwarded, inspirationAwarded ir reviewerNote keiciami tik kartu su status");
            }
            return entry;
        }

        User reviewer = users.getReferenceById(access.user().id());
        // Java 21 "arrow" switch per enum: kompiliatorius nereikalauja break, o kiekviena saka atskira.
        switch (request.status()) {
            case APPROVED -> {
                // Praleidus xpAwarded, serveris isveda verte is uzduoties sudetingumo.
                int xp = request.xpAwarded() != null ? request.xpAwarded() : quest.getDifficulty().xpValue();
                int inspiration = request.inspirationAwarded() != null ? request.inspirationAwarded() : 0;
                entry.approve(reviewer, xp, inspiration, request.reviewerNote(), Instant.now());
            }
            case REJECTED -> {
                requireNoAwards(request, "REJECTED");
                entry.reject(reviewer, request.reviewerNote(), Instant.now());
            }
            case PENDING -> {
                requireNoAwards(request, "PENDING");
                entry.resetToPending(request.reviewerNote());
            }
        }
        return entry;
    }

    public void delete(Long guildId, Long questId, Long logId) {
        GuildAccess access = guilds.access(guildId);
        LogEntry entry = find(access, questId, logId);
        access.require(access.is(entry.getAuthor()) || access.isDm() || access.isAdmin(),
                "Irasa istrinti gali jo autorius, grupes DM arba administratorius");
        logs.delete(entry);
    }

    /** Tikrinama visa grandine guild -> quest -> log: neteisingas kelias - 404 tam lygiui, kuris luzo. */
    private LogEntry find(GuildAccess access, Long questId, Long logId) {
        quests.get(access, questId);
        return logs.findByIdAndQuestIdAndQuestGuildId(logId, questId, access.guild().getId())
                .orElseThrow(() -> ResourceNotFoundException.logEntry(questId, logId));
    }

    private void requireNotInFuture(Instant occurredAt) {
        if (occurredAt.isAfter(Instant.now())) {
            throw new BusinessRuleException("occurredAt negali buti ateityje");
        }
    }

    /** Tik patvirtinti irasai duoda tasku - kitos busenos privalo likti be apdovanojimu. */
    private void requireNoAwards(LogEntryUpdateRequest request, String status) {
        boolean hasXp = request.xpAwarded() != null && request.xpAwarded() != 0;
        boolean hasInspiration = request.inspirationAwarded() != null && request.inspirationAwarded() != 0;
        if (hasXp || hasInspiration) {
            throw new BusinessRuleException(status + " busenos irasas negali tureti XP ar inspiration");
        }
    }
}
