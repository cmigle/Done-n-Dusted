package io.github.cmigle.donendusted.domain;

/**
 * Sudetingumas yra bazinio XP daugiklis. Serveris is jo isveda uzduoties verte -
 * klientas XP niekada nepaduoda.
 */
public enum Difficulty {
    TRIVIAL(1),
    EASY(2),
    MEDIUM(3),
    HARD(5),
    LEGENDARY(8);

    /** Bazine vieno atlikimo karto verte tasku. */
    public static final int BASE_XP = 10;

    private final int multiplier;

    Difficulty(int multiplier) {
        this.multiplier = multiplier;
    }

    public int multiplier() {
        return multiplier;
    }

    public int xpValue() {
        return BASE_XP * multiplier;
    }
}
