package io.github.cmigle.donendusted.domain;

/** Naudotojo role konkrecioje grupeje. */
public enum GuildRole {
    /** Kuria uzduotis ir registruoja savo progresa; mato tik savo irasus. */
    PLAYER,
    /** Tvirtina irasus, skiria XP ir inspiration, kuria SIDE_QUEST; mato visus grupes irasus. Grupeje - vienas. */
    DUNGEON_MASTER
}
