package io.github.cmigle.donendusted;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Veikejo kortele. Klase susikuria savo grupe (DM, herojus ir kitas zaidejas) su zinomu XP, todel skaiciai
 * nepriklauso nuo seed duomenu ir kitu testu. Duomenys sukuriami viena karta ir testuose tik skaitomi.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CharacterSheetIntegrationTest extends IntegrationTest {

    private TestUser dm;
    private TestUser hero;
    private TestUser other;
    private long guildId;
    private long todayLog;
    private long yesterdayLog;
    private long olderLog;

    @BeforeAll
    void createGuildWithReviewedLogs() throws Exception {
        dm = newUser();
        hero = newUser();
        other = newUser();
        guildId = createGuild(dm.token());
        addMember(dm.token(), guildId, hero.username(), "PLAYER");
        addMember(dm.token(), guildId, other.username(), "PLAYER");
        long str = createQuest(dm.token(), guildId, "STR", "MILESTONE", "LEGENDARY"); // 80 XP
        long wis = createQuest(dm.token(), guildId, "WIS", "HABIT", "TRIVIAL");       // 10 XP

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        todayLog = createLog(hero.token(), guildId, str, today.atStartOfDay().toInstant(ZoneOffset.UTC));
        yesterdayLog = createLog(hero.token(), guildId, wis, today.minusDays(1).atTime(12, 0).toInstant(ZoneOffset.UTC));
        olderLog = createLog(hero.token(), guildId, str, today.minusDays(3).atTime(12, 0).toInstant(ZoneOffset.UTC));
        // Uzpildytu tarpa pries 2 dienas, jei neperziureti ar atmesti irasai klaidingai butu skaiciuojami.
        Instant twoDaysAgo = today.minusDays(2).atTime(12, 0).toInstant(ZoneOffset.UTC);
        long rejected = createLog(hero.token(), guildId, wis, twoDaysAgo);
        createLog(hero.token(), guildId, str, twoDaysAgo); // lieka PENDING

        review(dm.token(), guildId, str, todayLog, "{\"status\":\"APPROVED\",\"xpAwarded\":600}");
        review(dm.token(), guildId, wis, yesterdayLog, "{\"status\":\"APPROVED\",\"inspirationAwarded\":2}");
        review(dm.token(), guildId, str, olderLog, "{\"status\":\"APPROVED\"}");
        review(dm.token(), guildId, wis, rejected, "{\"status\":\"REJECTED\"}");
    }

    @Test
    void ownSheetIsFullWithComputedValues() throws Exception {
        // STR: 600 + 80 = 680 -> min(20, 10 + 13) = 20; WIS: 10 -> 10. Viso 690 XP -> 7 lygis, 90 i 100.
        sheet(hero.token(), hero.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(hero.id()))
                .andExpect(jsonPath("$.username").value(hero.username()))
                .andExpect(jsonPath("$.guildId").value(guildId))
                .andExpect(jsonPath("$.role").value("PLAYER"))
                .andExpect(jsonPath("$.view").value("FULL"))
                .andExpect(jsonPath("$.totalXp").value(690))
                .andExpect(jsonPath("$.level").value(7))
                .andExpect(jsonPath("$.xpIntoLevel").value(90))
                .andExpect(jsonPath("$.xpForNextLevel").value(100))
                .andExpect(jsonPath("$.inspiration").value(2))
                .andExpect(jsonPath("$.streakDays").value(2))
                .andExpect(jsonPath("$.stats[*].stat", contains("STR", "DEX", "CON", "INT", "WIS", "CHA")))
                .andExpect(jsonPath("$.stats[*].xp", contains(680, 0, 0, 0, 10, 0)))
                .andExpect(jsonPath("$.stats[*].score", contains(20, 10, 10, 10, 10, 10)))
                .andExpect(jsonPath("$.recentLogs[*].logId",
                        contains((int) todayLog, (int) yesterdayLog, (int) olderLog)))
                .andExpect(jsonPath("$.recentLogs[0].xpAwarded").value(600))
                .andExpect(jsonPath("$.recentLogs[1].stat").value("WIS"))
                .andExpect(jsonPath("$.recentLogs[1].inspirationAwarded").value(2))
                .andExpect(jsonPath("$.pendingCount").value(1));
    }

    @Test
    void dmAndAdminSeeFullSheet() throws Exception {
        sheet(dm.token(), hero.id()).andExpect(status().isOk()).andExpect(jsonPath("$.view").value("FULL"));
        sheet(token("admin"), hero.id()).andExpect(status().isOk()).andExpect(jsonPath("$.view").value("FULL"));
    }

    @Test
    void otherMemberSeesPublicSheetWithoutLogs() throws Exception {
        sheet(other.token(), hero.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.view").value("PUBLIC"))
                .andExpect(jsonPath("$.totalXp").value(690))
                .andExpect(jsonPath("$.recentLogs").doesNotExist())
                .andExpect(jsonPath("$.pendingCount").doesNotExist());
    }

    @Test
    void memberWithoutApprovedLogsStartsAtLevelOne() throws Exception {
        sheet(dm.token(), other.id())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.totalXp").value(0))
                .andExpect(jsonPath("$.streakDays").value(0))
                .andExpect(jsonPath("$.stats[*].score", everyItem(is(10))))
                .andExpect(jsonPath("$.recentLogs").isEmpty())
                .andExpect(jsonPath("$.pendingCount").value(0));
    }

    @Test
    void nonMemberRequesterIs403() throws Exception {
        sheet(token("jonas"), hero.id()).andExpect(status().isForbidden());
    }

    @Test
    void targetNotInGuildIs404() throws Exception {
        sheet(dm.token(), 1).andExpect(status().isNotFound()); // admin nera sios grupes narys
        getAs(dm.token(), "/api/guilds/999999/members/" + hero.id() + "/sheet").andExpect(status().isNotFound());
    }

    private ResultActions sheet(String token, long userId) throws Exception {
        return getAs(token, "/api/guilds/" + guildId + "/members/" + userId + "/sheet");
    }
}
