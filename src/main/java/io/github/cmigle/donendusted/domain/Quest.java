package io.github.cmigle.donendusted.domain;

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
import java.util.Objects;
import org.hibernate.annotations.CreationTimestamp;

/** Uzduotis - konkretus issukis grupes viduje. Priklauso lygiai vienai grupei. */
@Entity
@Table(name = "quest")
public class Quest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guild_id", nullable = false)
    private Guild guild;

    @Column(nullable = false, length = 120)
    private String title;

    // Kaip ir Guild: nenurodytas aprasymas saugomas kaip "", kad laukas atsakyme niekada nedingtu.
    @Column(nullable = false, length = 1000)
    private String description = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private Stat stat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private QuestType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Difficulty difficulty;

    /** Tik GRIND tipo uzduotims: kiek patvirtintu irasu reikia tikslui pasiekti. */
    @Column(name = "target_count")
    private Integer targetCount;

    @Column(nullable = false)
    private boolean active = true;

    /** Kas sukure; null, jei naudotojas pasalintas. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Quest() {
        // JPA
    }

    public Quest(Guild guild, String title, String description, Stat stat, QuestType type,
                 Difficulty difficulty, Integer targetCount, boolean active, User createdBy) {
        this.guild = guild;
        this.createdBy = createdBy;
        this.title = title;
        setDescription(description);
        this.stat = stat;
        this.type = type;
        this.difficulty = difficulty;
        this.targetCount = targetCount;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public Guild getGuild() {
        return guild;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNullElse(description, "");
    }

    public Stat getStat() {
        return stat;
    }

    public void setStat(Stat stat) {
        this.stat = stat;
    }

    public QuestType getType() {
        return type;
    }

    public void setType(QuestType type) {
        this.type = type;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public Integer getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(Integer targetCount) {
        this.targetCount = targetCount;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
