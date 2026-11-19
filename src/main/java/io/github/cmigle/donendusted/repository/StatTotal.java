package io.github.cmigle.donendusted.repository;

import io.github.cmigle.donendusted.domain.Stat;

/**
 * Vienos charakteristikos patvirtinto XP ir inspiration suma - viena GROUP BY uzklausos eilute.
 * Long, ne long: JPQL sum() grazina Long, o konstruktoriaus israiska ieskoma pagal tikslius tipus.
 */
public record StatTotal(Stat stat, Long xp, Long inspiration) {
}
