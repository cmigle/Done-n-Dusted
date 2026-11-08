package lt.ktu.donendusted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Progreso irasas prie uzduoties. Sukuriamas PENDING busenoje ir neduoda jokiu tasku,
 * kol nera patvirtintas.
 */
@Entity
@Table(name = "log_entry")
public class LogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: uzduotis is DB kraunama tik tada, kai jos tikrai prireikia (numatytasis @ManyToOne yra EAGER).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    /** Kas uzregistravo progresa. Savo iraso autorius tvirtinti negali. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    /** DM, kuris patvirtino arba atmete; PENDING irasui - null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    @Column(length = 1000)
    private String note;

    /** Irodymo nuoroda, pvz., i nuotrauka. */
    @Column(name = "evidence_url", length = 500)
    private String evidenceUrl;

    // STRING, o ne ORDINAL: DB saugomas "APPROVED", ne skaicius 1. Pakeitus enum tvarka duomenys nesugestu.
    // Setterio statusui nera - busena keiciama tik per approve/reject/resetToPending.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LogStatus status = LogStatus.PENDING;

    /** Patvirtinimo metu uzsaldyta tasku verte - veliau keiciant uzduoti neperskaiciuojama. */
    @Column(name = "xp_awarded", nullable = false)
    private int xpAwarded;

    @Column(name = "inspiration_awarded", nullable = false)
    private int inspirationAwarded;

    @Column(name = "reviewer_note", length = 500)
    private String reviewerNote;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected LogEntry() {
        // JPA
    }

    public LogEntry(Quest quest, User author, String note, String evidenceUrl, Instant occurredAt) {
        this.quest = quest;
        this.author = author;
        this.note = note;
        this.evidenceUrl = evidenceUrl;
        this.occurredAt = occurredAt;
    }

    /** Patvirtina irasa: uzsaldo XP ir ikvepima, uzfiksuoja perziuros laika ir perziurejusi DM. */
    public void approve(User reviewer, int xpAwarded, int inspirationAwarded, String reviewerNote, Instant reviewedAt) {
        this.status = LogStatus.APPROVED;
        this.reviewer = reviewer;
        this.xpAwarded = xpAwarded;
        this.inspirationAwarded = inspirationAwarded;
        this.reviewerNote = reviewerNote;
        this.reviewedAt = reviewedAt;
    }

    /** Atmeta irasa: jokiu tasku neskiriama. */
    public void reject(User reviewer, String reviewerNote, Instant reviewedAt) {
        this.status = LogStatus.REJECTED;
        this.reviewer = reviewer;
        this.xpAwarded = 0;
        this.inspirationAwarded = 0;
        this.reviewerNote = reviewerNote;
        this.reviewedAt = reviewedAt;
    }

    /** Grazina irasa i neperziureta busena - visi apdovanojimai nuimami. */
    public void resetToPending(String reviewerNote) {
        this.status = LogStatus.PENDING;
        this.reviewer = null;
        this.xpAwarded = 0;
        this.inspirationAwarded = 0;
        this.reviewerNote = reviewerNote;
        this.reviewedAt = null;
    }

    public Long getId() {
        return id;
    }

    public Quest getQuest() {
        return quest;
    }

    public User getAuthor() {
        return author;
    }

    public User getReviewer() {
        return reviewer;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getEvidenceUrl() {
        return evidenceUrl;
    }

    public void setEvidenceUrl(String evidenceUrl) {
        this.evidenceUrl = evidenceUrl;
    }

    public LogStatus getStatus() {
        return status;
    }

    public int getXpAwarded() {
        return xpAwarded;
    }

    public int getInspirationAwarded() {
        return inspirationAwarded;
    }

    public String getReviewerNote() {
        return reviewerNote;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
