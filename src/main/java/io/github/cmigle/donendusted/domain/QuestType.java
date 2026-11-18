package io.github.cmigle.donendusted.domain;

public enum QuestType {
    /** Vienkartinis tikslas. */
    MILESTONE,
    /** Pasikartojantis iprotis. */
    HABIT,
    /** Kaupiamoji uzduotis iki tikslo (targetCount). */
    GRIND,
    /** DM skiriama uzduotis uz netiketa ar juokinga veikla. */
    SIDE_QUEST
}
