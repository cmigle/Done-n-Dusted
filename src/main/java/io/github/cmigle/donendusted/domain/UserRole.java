package io.github.cmigle.donendusted.domain;

/**
 * Globali (visos sistemos) naudotojo role. PLAYER ir DUNGEON_MASTER yra ne cia, o grupes narystes
 * roles (GuildRole) - tas pats naudotojas vienoje grupeje gali buti DM, kitoje - zaidejas.
 */
public enum UserRole {
    /** Iprastas registruotas naudotojas. */
    USER,
    /** Valdo naudotojus, mato visas grupes ir gali salinti netinkama turini. */
    ADMIN
}
