package lt.ktu.donendusted;

import static org.hamcrest.Matchers.containsString;
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
                .andExpect(jsonPath("$.message").value("Quest 4 nerasta grupeje 1"));
    }

    @Test
    void logThroughWrongQuestIs404() throws Exception {
        getAs(token("rokas"), logsUrl(2, 5) + "/4")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("LogEntry 4 nerastas uzduotyje 5"));
    }

    @Test
    void blankPathIdIs400() throws Exception {
        // URI, ne String: kitaip MockMvc %20 uzkoduotu dar karta (%2520).
        mvc.perform(get(URI.create("/api/guilds/%20")).header(HttpHeaders.AUTHORIZATION, "Bearer " + token("rokas")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("tuscia reiksme")));
    }

    @Test
    void sortByUnknownFieldIs400() throws Exception {
        getAs(token("rokas"), "/api/guilds?sort=class")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Rikiuoti pagal 'class' negalima")));
    }

    @Test
    void hugePageNumberIs400() throws Exception {
        getAs(token("rokas"), "/api/guilds?page=99999999999")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Per didelis puslapio numeris")));
    }

    @Test
    void enumAsNumberIs400() throws Exception {
        patchAs(token("rokas"), logsUrl(2, 4) + "/7", "{\"status\":1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("enum reiksme rasoma tekstu")));
    }

    @Test
    void unknownBodyFieldIs400() throws Exception {
        postAs(token("rokas"), "/api/guilds", "{\"name\":\"%s\",\"xp\":100}".formatted(unique("IT guild ")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void blankRequiredFieldIs400WithDetails() throws Exception {
        postAs(token("rokas"), "/api/guilds", "{\"name\":\"  \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].field").value("name"));
    }
}
