package lt.ktu.donendusted;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/** 404 neteisingai tevinei grandinei ir 400 netinkamiems parametrams ar turiniui. */
class ValidationIntegrationTest extends IntegrationTest {

    @Test
    void questThroughWrongGuildIs404() throws Exception {
        // Uzduotis 4 priklauso Iron Vow (2), ne Thursday Night Crew (1), nors rokas abieju narys.
        getAs(token("rokas"), "/api/guilds/1/quests/4")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Quest 4 not found in guild 1"));
    }

    @Test
    void logThroughWrongQuestIs404() throws Exception {
        getAs(token("rokas"), logsUrl(2, 5) + "/4")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Log entry 4 not found in quest 5"));
    }

    @Test
    void blankPathIdIs400() throws Exception {
        // URI, ne String: kitaip MockMvc %20 uzkoduotu dar karta (%2520).
        mvc.perform(get(URI.create("/api/guilds/%20")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("rokas")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter \"guildId\" must be a number, got an empty value."));
    }

    @Test
    void sortByUnknownFieldIs400() throws Exception {
        getAs(token("rokas"), "/api/guilds?sort=class")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Cannot sort by \"class\"")));
    }

    @Test
    void hugePageNumberIs400() throws Exception {
        getAs(token("rokas"), "/api/guilds?page=99999999999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Page number is too large")));
    }

    @Test
    void enumAsNumberIs400() throws Exception {
        patchAs(token("rokas"), logsUrl(2, 4) + "/7", "{\"status\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field \"status\" must be a string, not a number. Allowed values: PENDING, APPROVED, REJECTED."));
    }

    @Test
    void unknownBodyFieldIs400() throws Exception {
        postAs(token("rokas"), "/api/guilds", "{\"name\":\"%s\",\"xp\":100}".formatted(unique("IT guild ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Unknown field \"xp\""));
    }

    @Test
    void blankRequiredFieldIs400WithDetails() throws Exception {
        postAs(token("rokas"), "/api/guilds", "{\"name\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request body"))
                .andExpect(jsonPath("$.details[0].field").value("name"))
                .andExpect(jsonPath("$.details[*].message", hasItem("Must not be blank")));
    }

    /** Lauku pranesimai is ValidationMessages.properties: su min ir be jo (tik max). */
    @Test
    void sizeViolationsUseCustomMessages() throws Exception {
        postAs(token("rokas"), "/api/guilds", "{\"name\":\"ab\",\"description\":\"%s\"}".formatted("x".repeat(501)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[?(@.field == 'name')].message")
                        .value("Must be between 3 and 60 characters"))
                .andExpect(jsonPath("$.details[?(@.field == 'description')].message")
                        .value("Must be at most 500 characters"));
    }

    /** Jackson klaidos - trumpas pranesimas be Java tipu vardu ir vidiniu Jackson detaliu. */
    @Test
    void unreadableBodyErrorsAreReadable() throws Exception {
        String quests = "/api/guilds/2/quests";
        postAs(token("rokas"), quests, "{\"title\":\"Bogus\",\"stat\":\"BOGUS\",\"type\":\"HABIT\",\"difficulty\":\"EASY\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid value \"BOGUS\" for field \"stat\". Allowed values: STR, DEX, CON, INT, WIS, CHA."));
        postAs(token("rokas"), quests, "{\"title\":\"Bogus\",\"targetCount\":\"daug\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Field \"targetCount\" must be a number."));
        postAs(token("rokas"), quests, "{\"title\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed JSON"));
    }

    @Test
    void pathTypeMismatchIs400WithoutJavaTypeNames() throws Exception {
        getAs(token("rokas"), "/api/guilds/abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Parameter \"guildId\" must be a number, got \"abc\"."));
    }
}
