package io.github.cmigle.donendusted.service;

import java.time.Instant;
import io.github.cmigle.donendusted.domain.User;
import io.github.cmigle.donendusted.domain.UserRole;
import io.github.cmigle.donendusted.error.BusinessRuleException;
import io.github.cmigle.donendusted.error.ForbiddenException;
import io.github.cmigle.donendusted.error.ResourceNotFoundException;
import io.github.cmigle.donendusted.repository.RefreshTokenRepository;
import io.github.cmigle.donendusted.repository.UserRepository;
import io.github.cmigle.donendusted.security.CurrentUser;
import io.github.cmigle.donendusted.web.dto.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Naudotoju valdymas - pasiekiamas tik ADMIN (tikrinama SecurityConfig). */
@Service
@Transactional
public class UserService {

    private final UserRepository users;
    private final RefreshTokenRepository refreshTokens;

    public UserService(UserRepository users, RefreshTokenRepository refreshTokens) {
        this.users = users;
        this.refreshTokens = refreshTokens;
    }

    @Transactional(readOnly = true)
    public Page<User> list(String usernameFilter, Pageable pageable) {
        Specification<User> spec = (root, query, cb) -> StringUtils.hasText(usernameFilter)
                ? cb.like(cb.lower(root.get("username")), LikePattern.contains(usernameFilter), LikePattern.ESCAPE)
                : cb.conjunction();
        return users.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    /**
     * Nauja role galioja nuo kito access zetono (ne veliau kaip po 15 min). Uzblokavus atsaukiami visi
     * refresh zetonai, todel naujo access zetono naudotojas nebegaus.
     */
    public User update(Long id, UserUpdateRequest request) {
        // ADMIN role zetone galioja iki 15 min - teises keicia tik tas, kuris ADMIN ir aktyvus DB dabar.
        // Kitaip ka tik atimtas ar uzblokuotas administratorius per ta laika grazintu sau teises.
        Long callerId = CurrentUser.get().id();
        users.findById(callerId)
                .filter(caller -> caller.isEnabled() && caller.getRole() == UserRole.ADMIN)
                .orElseThrow(() -> new ForbiddenException("Your admin rights have been revoked. Please log in again."));
        User user = get(id);
        boolean self = id.equals(callerId);
        if (self && (request.role() == UserRole.USER || Boolean.FALSE.equals(request.enabled()))) {
            throw new BusinessRuleException("You cannot remove your own admin role or block your own account.");
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
            if (!request.enabled()) {
                refreshTokens.revokeAllForUser(id, Instant.now());
            }
        }
        return user;
    }
}
