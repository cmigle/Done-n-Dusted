package lt.ktu.donendusted.service;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lt.ktu.donendusted.domain.LogEntry;
import lt.ktu.donendusted.domain.LogStatus;
import lt.ktu.donendusted.domain.Quest;
import lt.ktu.donendusted.error.BusinessRuleException;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.LogEntryRepository;
import lt.ktu.donendusted.web.dto.LogEntryCreateRequest;
import lt.ktu.donendusted.web.dto.LogEntryUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogEntryService {

    private final LogEntryRepository logs;
    private final QuestService quests;

    public LogEntryService(LogEntryRepository logs, QuestService quests) {
        this.logs = logs;
        this.quests = quests;
    }

    @Transactional(readOnly = true)
    public Page<LogEntry> list(Long guildId, Long questId, LogStatus status, Pageable pageable) {
        quests.get(guildId, questId); // patvirtina visa tevine grandine
        Specification<LogEntry> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("quest").get("id"), questId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return logs.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public LogEntry get(Long guildId, Long questId, Long logId) {
        // Tas pats principas kaip QuestService.get(): tikrinama visa grandine vienu SQL (JOIN i quest),
        // o jei nerasta - lambda issiaiskina, kuris lygis luzo (grupe, uzduotis ar pats irasas).
        return logs.findByIdAndQuestIdAndQuestGuildId(logId, questId, guildId)
                .orElseThrow(() -> {
                    quests.get(guildId, questId); // jei luzta auksciau, pranesame apie ta lygi
                    return ResourceNotFoundException.logEntry(questId, logId);
                });
    }

    public LogEntry create(Long guildId, Long questId, LogEntryCreateRequest request) {
        Quest quest = quests.get(guildId, questId);
        requireNotInFuture(request.occurredAt());
        if (!quest.isActive()) {
            throw new BusinessRuleException("Uzduotis neaktyvi - nauju irasu prie jos registruoti negalima");
        }
        // XP ir busena cia nenustatomi: konstruktorius palieka PENDING ir 0 tasku.
        return logs.save(new LogEntry(quest, request.note(), request.evidenceUrl(), request.occurredAt()));
    }

    /**
     * PATCH - dalinis atnaujinimas: keiciami tik atsiusti (ne null) laukai.
     * xpAwarded, inspirationAwarded ir reviewerNote yra DM sprendimo dalis, todel priimami tik kartu su status.
     * Busenu perejimai (PENDING/APPROVED/REJECTED) atliekami per LogEntry metodus approve/reject/
     * resetToPending, o ne per setterius - taip XP niekada negali atsidurti ne APPROVED irase.
     */
    public LogEntry update(Long guildId, Long questId, Long logId, LogEntryUpdateRequest request) {
        LogEntry entry = get(guildId, questId, logId);
        Quest quest = quests.get(guildId, questId); // reikia sudetingumo XP apskaiciavimui

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
            boolean reviewFieldsSent = request.xpAwarded() != null || request.inspirationAwarded() != null
                    || request.reviewerNote() != null;
            if (reviewFieldsSent) {
                throw new BusinessRuleException("xpAwarded, inspirationAwarded ir reviewerNote keiciami tik kartu su status");
            }
            return entry;
        }

        // Java 21 "arrow" switch per enum: kompiliatorius nereikalauja break, o kiekviena saka atskira.
        switch (request.status()) {
            case APPROVED -> {
                // Praleidus xpAwarded, serveris isveda verte is uzduoties sudetingumo.
                int xp = request.xpAwarded() != null ? request.xpAwarded() : quest.getDifficulty().xpValue();
                int inspiration = request.inspirationAwarded() != null ? request.inspirationAwarded() : 0;
                entry.approve(xp, inspiration, request.reviewerNote(), Instant.now());
            }
            case REJECTED -> {
                requireNoAwards(request, "REJECTED");
                entry.reject(request.reviewerNote(), Instant.now());
            }
            case PENDING -> {
                requireNoAwards(request, "PENDING");
                entry.resetToPending(request.reviewerNote());
            }
        }
        return entry;
    }

    public void delete(Long guildId, Long questId, Long logId) {
        logs.delete(get(guildId, questId, logId));
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
