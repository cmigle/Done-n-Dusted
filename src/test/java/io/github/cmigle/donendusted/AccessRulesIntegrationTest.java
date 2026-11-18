package io.github.cmigle.donendusted;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * 403 taisykles su seed duomenimis: Iron Vow (2) - DM rokas, zaidejai jonas (4) ir ieva (3);
 * The Quiet Library (3) - jonas ne narys. Uzduotis 4 "Run 50 kilometers" ir 5 "Morning mobility routine"
 * priklauso Iron Vow; irasas 7 - jono PENDING, irasas 8 - ievos APPROVED. Uzklausos, kurios turi buti
 * atmestos, nieko nekeicia, todel seed duomenys lieka tokie patys.
 */
class AccessRulesIntegrationTest extends IntegrationTest {

    @Test
    void nonMemberCannotSeeGuild() throws Exception {
        getAs(token("jonas"), "/api/guilds/3")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You are not a member of guild 3."));
    }

    @Test
    void playerCannotApproveLog() throws Exception {
        patchAs(token("jonas"), logsUrl(2, 4) + "/7", "{\"status\":\"APPROVED\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the game master can review log entries."));
    }

    @Test
    void dmCannotApproveOwnLog() throws Exception {
        String rokas = token("rokas");
        long logId = createLog(rokas, 2, 5, Instant.now().minus(Duration.ofHours(1)));

        patchAs(rokas, logsUrl(2, 5) + "/" + logId, "{\"status\":\"APPROVED\"}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You cannot review your own log entry."));

        deleteAs(rokas, logsUrl(2, 5) + "/" + logId).andExpect(status().isNoContent());
    }

    @Test
    void playerCannotCreateSideQuest() throws Exception {
        postAs(token("jonas"), "/api/guilds/2/quests",
                "{\"title\":\"%s\",\"stat\":\"CHA\",\"type\":\"SIDE_QUEST\",\"difficulty\":\"EASY\"}"
                        .formatted(unique("IT side ")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the game master can create SIDE_QUEST quests."));
    }

    @Test
    void userManagementIsAdminOnly() throws Exception {
        getAs(token("rokas"), "/api/users").andExpect(status().isForbidden());
        getAs(token("admin"), "/api/users").andExpect(status().isOk());
    }

    @Test
    void emptyPatchOnSomeoneElsesLogIs403() throws Exception {
        patchAs(token("jonas"), logsUrl(2, 5) + "/8", "{}")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Only the game master can see other players' log entries."));
    }

    @Test
    void playerSeesOnlyOwnLogs() throws Exception {
        getAs(token("jonas"), logsUrl(2, 4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].authorId", everyItem(is(4))));
    }
}
