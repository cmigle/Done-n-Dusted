package io.github.cmigle.donendusted.service;

import io.github.cmigle.donendusted.domain.Guild;
import io.github.cmigle.donendusted.domain.GuildRole;
import io.github.cmigle.donendusted.domain.User;
import io.github.cmigle.donendusted.error.ForbiddenException;
import io.github.cmigle.donendusted.security.CurrentUser;

/**
 * Ka prisijunges naudotojas gali daryti konkrecioje grupeje. Sukuriama per GuildService.access(),
 * kuri jau patikrino: grupe egzistuoja (kitaip 404), naudotojas yra jos narys arba ADMIN (kitaip 403).
 *
 * @param role naudotojo role grupeje; null - ADMIN, kuris nera grupes narys
 */
public record GuildAccess(Guild guild, CurrentUser user, GuildRole role) {

    public boolean isMember() {
        return role != null;
    }

    public boolean isDm() {
        return role == GuildRole.DUNGEON_MASTER;
    }

    public boolean isOwner() {
        return guild.getOwner().getId().equals(user.id());
    }

    public boolean isAdmin() {
        return user.admin();
    }

    /** Ar nurodytas naudotojas (autorius, kurejas) yra pats prisijunges naudotojas. */
    public boolean is(User other) {
        return other != null && other.getId().equals(user.id());
    }

    /** Teises patikra: netenkinama salyga - 403 su paaiskinimu, ko truko. */
    public void require(boolean allowed, String message) {
        if (!allowed) {
            throw new ForbiddenException(message);
        }
    }
}
