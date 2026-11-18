package io.github.cmigle.donendusted.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Grupe (komanda) - virsutinis hierarchijos Guild -> Quest -> LogEntry lygis. */
@Entity
@Table(name = "guild")
public class Guild {

    // IDENTITY: id sugeneruoja pati PostgreSQL (bigserial stulpelis V1__schema.sql).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String name;

    // Niekada ne null: nenurodytas aprasymas saugomas kaip "", todel JSON atsakyme laukas visada yra
    // (null laukai i JSON neirasomi - zr. application.yml). DB tai uztikrina V3 migracija.
    @Column(nullable = false, length = 500)
    private String description = "";

    /** Grupes kurejas - tik jis keicia grupe ir valdo narius. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Guild() {
        // JPA reikalauja tuscio konstruktoriaus; protected - kad kode niekas nesukurtu grupes be vardo
    }

    public Guild(String name, String description, User owner) {
        this.name = name;
        this.owner = owner;
        setDescription(description);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = Objects.requireNonNullElse(description, "");
    }

    public User getOwner() {
        return owner;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
