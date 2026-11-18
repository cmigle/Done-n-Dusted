package lt.ktu.donendusted.service;

import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lt.ktu.donendusted.domain.Friendship;
import lt.ktu.donendusted.domain.FriendshipStatus;
import lt.ktu.donendusted.domain.User;
import lt.ktu.donendusted.error.BusinessRuleException;
import lt.ktu.donendusted.error.ConflictException;
import lt.ktu.donendusted.error.ForbiddenException;
import lt.ktu.donendusted.error.ResourceNotFoundException;
import lt.ktu.donendusted.repository.FriendshipRepository;
import lt.ktu.donendusted.repository.UserRepository;
import lt.ktu.donendusted.security.CurrentUser;
import lt.ktu.donendusted.web.dto.CharacterSheetResponse;
import lt.ktu.donendusted.web.dto.FriendRequest;
import lt.ktu.donendusted.web.dto.FriendshipUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Draugystes. Kvietima priima tik gavejas, uzblokuoti gali bet kuri puse. Naudotojas mato ir keicia tik savo
 * draugystes - svetima arba ji uzblokavusio naudotojo eilute grazina 404, kad blokavimas nebutu atskleistas.
 * Patvirtinta draugyste atveria draugo viesa veikejo kortele.
 */
@Service
@Transactional
public class FriendshipService {

    private final FriendshipRepository friendships;
    private final UserRepository users;
    private final CharacterSheetService sheets;

    public FriendshipService(FriendshipRepository friendships, UserRepository users, CharacterSheetService sheets) {
        this.friendships = friendships;
        this.users = users;
        this.sheets = sheets;
    }

    /** Savo draugystes ir kvietimai abiem kryptimis; uzblokuotajam BLOCKED eilute nerodoma. */
    @Transactional(readOnly = true)
    public Page<Friendship> list(FriendshipStatus status, Pageable pageable) {
        Long me = CurrentUser.get().id();
        Specification<Friendship> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.or(
                    cb.equal(root.get("requester").get("id"), me),
                    cb.and(cb.equal(root.get("addressee").get("id"), me),
                            cb.notEqual(root.get("status"), FriendshipStatus.BLOCKED))));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
        return friendships.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Friendship get(Long id) {
        return find(id, CurrentUser.get().id());
    }

    public Friendship request(FriendRequest request) {
        Long me = CurrentUser.get().id();
        // Kaip ir pridedant nari: naudotojas nurodomas body, todel jo nebuvimas - turinio klaida (422), ne 404.
        User addressee = users.findByUsernameIgnoreCase(request.username())
                .orElseThrow(() -> new BusinessRuleException("Naudotojas '" + request.username() + "' nerastas"));
        if (addressee.getId().equals(me)) {
            throw new BusinessRuleException("Pakviesti i draugus saves negalima");
        }
        // Bet kokia esama pora, ir BLOCKED, - tas pats 409: neatskleidziame, kad kitas naudotojas uzblokavo.
        if (friendships.existsBetween(me, addressee.getId())) {
            throw new ConflictException("Draugyste ar kvietimas su naudotoju '" + addressee.getUsername() + "' jau yra");
        }
        return friendships.save(new Friendship(users.getReferenceById(me), addressee));
    }

    public Friendship update(Long id, FriendshipUpdateRequest request) {
        Long me = CurrentUser.get().id();
        Friendship friendship = find(id, me);
        switch (request.status()) {
            case ACCEPTED -> {
                if (friendship.getStatus() != FriendshipStatus.PENDING) {
                    throw new BusinessRuleException("Priimti galima tik laukianti kvietima - si draugyste jau "
                            + friendship.getStatus());
                }
                if (!friendship.isAddressee(me)) {
                    throw new ForbiddenException("Kvietima priimti gali tik jo gavejas");
                }
                friendship.accept(Instant.now());
            }
            case BLOCKED -> {
                // find() BLOCKED eilute grazina tik uzblokavusiajam - antra karta blokuoti nera ko.
                if (friendship.getStatus() == FriendshipStatus.BLOCKED) {
                    throw new BusinessRuleException("Naudotojas jau uzblokuotas");
                }
                friendship.block(me, Instant.now());
            }
            case PENDING -> throw new BusinessRuleException(
                    "Busena keiciama tik i ACCEPTED arba BLOCKED; kvietima atsaukti ar atmesti - DELETE");
        }
        return friendship;
    }

    /** Atsaukti ar atmesti kvietima, nutraukti draugyste arba (uzblokavusiajam) atblokuoti - eilute istrinama. */
    public void delete(Long id) {
        friendships.delete(find(id, CurrentUser.get().id()));
    }

    /** Draugo viesa kortele, sujungta per visas jo grupes. Tik patvirtintai draugystei. */
    @Transactional(readOnly = true)
    public CharacterSheetResponse friendSheet(Long id) {
        Long me = CurrentUser.get().id();
        Friendship friendship = find(id, me);
        if (friendship.getStatus() != FriendshipStatus.ACCEPTED) {
            throw new ForbiddenException("Veikejo kortele matoma tik patvirtintiems draugams");
        }
        return sheets.publicSheet(friendship.other(me));
    }

    private Friendship find(Long id, Long me) {
        return friendships.findWithUsersById(id)
                .filter(friendship -> friendship.isVisibleTo(me))
                .orElseThrow(() -> new ResourceNotFoundException("Draugyste " + id + " nerasta"));
    }
}
