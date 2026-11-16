package lt.ktu.donendusted.service;

/**
 * LIKE sablonas "turi savyje" paieskai be raidziu dydzio. Kliento tekste % ir _ - paprasti simboliai,
 * ne sablono zenklai: kitaip ?name=_ rastu visas grupes.
 */
final class LikePattern {

    static final char ESCAPE = '\\';

    private LikePattern() {
    }

    static String contains(String text) {
        String escaped = text.toLowerCase()
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }
}
