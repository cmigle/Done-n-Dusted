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
 * Draugyste tarp dvieju naudotoju. Porai - viena eilute nepriklausomai nuo krypties (DB uq_friendship_pair).
 * Uzblokavus uzblokavusysis tampa requester: taip be atskiro stulpelio zinoma, kas gali atblokuoti,
 * o uzblokuotajam si eilute nerodoma (blokavimas neatskleidziamas).
 */
@Entity
@Table(name = "friendship")
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Kas siunte kvietima; BLOCKED busenoje - kas uzblokavo. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addressee_id", nullable = false)
    private User addressee;

    // Setterio nera - busena keiciama tik per accept/block.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private FriendshipStatus status = FriendshipStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Kada kvietimas priimtas ar draugyste uzblokuota; PENDING - null. */
    @Column(name = "responded_at")
    private Instant respondedAt;

    protected Friendship() {
        // JPA
    }

    public Friendship(User requester, User addressee) {
        this.requester = requester;
        this.addressee = addressee;
    }

    public void accept(Instant now) {
        this.status = FriendshipStatus.ACCEPTED;
        this.respondedAt = now;
    }

    /** Uzblokuoti gali bet kuri puse; jei tai gavejas, puses sukeiciamos - requester visada uzblokavusysis. */
    public void block(Long blockerId, Instant now) {
        if (addressee.getId().equals(blockerId)) {
            User blocked = requester;
            this.requester = addressee;
            this.addressee = blocked;
        }
        this.status = FriendshipStatus.BLOCKED;
        this.respondedAt = now;
    }

    public boolean isRequester(Long userId) {
        return requester.getId().equals(userId);
    }

    public boolean isAddressee(Long userId) {
        return addressee.getId().equals(userId);
    }

    /** Savo draugyste naudotojas mato visada, isskyrus kai jis pats yra uzblokuotas. */
    public boolean isVisibleTo(Long userId) {
        return isRequester(userId) || (isAddressee(userId) && status != FriendshipStatus.BLOCKED);
    }

    /** Kita poros puse prisijungusio naudotojo akimis. */
    public User other(Long userId) {
        return isRequester(userId) ? addressee : requester;
    }

    public Long getId() {
        return id;
    }

    public User getRequester() {
        return requester;
    }

    public User getAddressee() {
        return addressee;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRespondedAt() {
        return respondedAt;
    }
}
