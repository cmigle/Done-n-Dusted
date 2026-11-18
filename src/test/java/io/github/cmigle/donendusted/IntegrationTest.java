package io.github.cmigle.donendusted;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Visu integraciniu testu baze. Visos klases paveldi tas pacias anotacijas, todel Spring kontekstas - ir jame
 * paleidziama PostgreSQL su Flyway seed duomenimis - sukuriamas viena karta ir pernaudojamas.
 * app.embedded-db.port=0: laisvas atsitiktinis prievadas, kad testai nesusikirstu su veikiancia demo kopija.
 *
 * Testai nepriklauso nuo eiles: seed duomenys tik skaitomi (arba bandomi keisti nesekmingai), o keiciami tik
 * paciu testu susikurti naudotojai ir grupes su unikaliais vardais.
 */
@SpringBootTest(properties = "app.embedded-db.port=0")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public abstract class IntegrationTest {

    protected static final String PASSWORD = "Demo123!";

    @Autowired
    protected MockMvc mvc;

    /** Naujai uzregistruotas testo naudotojas su access zetonu. */
    protected record TestUser(long id, String username, String token) {
    }

    // ---- HTTP ----

    protected ResultActions getAs(String token, String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.get(url), token));
    }

    protected ResultActions postAs(String token, String url, String json) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.post(url), token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions patchAs(String token, String url, String json) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.patch(url), token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteAs(String token, String url) throws Exception {
        return mvc.perform(auth(MockMvcRequestBuilders.delete(url), token));
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
    }

    protected static String body(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    protected static long id(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(body(result), "$.id")).longValue();
    }

    // ---- Naudotojai ir zetonai ----

    protected ResultActions login(String username, String password) throws Exception {
        return postAs(null, "/api/auth/login",
                "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password));
    }

    protected String token(String username) throws Exception {
        MvcResult result = login(username, PASSWORD).andExpect(status().isOk()).andReturn();
        return JsonPath.read(body(result), "$.accessToken");
    }

    protected static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    protected TestUser newUser() throws Exception {
        String username = unique("it_");
        MvcResult result = postAs(null, "/api/auth/register",
                "{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, PASSWORD))
                .andExpect(status().isCreated()).andReturn();
        return new TestUser(id(result), username, token(username));
    }

    // ---- Grupes, uzduotys ir irasai ----

    /** Kurejas tampa grupes savininku ir DM. */
    protected long createGuild(String token) throws Exception {
        return id(postAs(token, "/api/guilds", "{\"name\":\"%s\"}".formatted(unique("IT guild ")))
                .andExpect(status().isCreated()).andReturn());
    }

    protected void addMember(String ownerToken, long guildId, String username, String role) throws Exception {
        postAs(ownerToken, "/api/guilds/" + guildId + "/members",
                "{\"username\":\"%s\",\"role\":\"%s\"}".formatted(username, role))
                .andExpect(status().isCreated());
    }

    protected long createQuest(String token, long guildId, String stat, String type, String difficulty) throws Exception {
        String json = "{\"title\":\"%s\",\"stat\":\"%s\",\"type\":\"%s\",\"difficulty\":\"%s\"}"
                .formatted(unique("IT quest "), stat, type, difficulty);
        return id(postAs(token, "/api/guilds/" + guildId + "/quests", json)
                .andExpect(status().isCreated()).andReturn());
    }

    protected long createLog(String token, long guildId, long questId, Instant occurredAt) throws Exception {
        return id(postAs(token, logsUrl(guildId, questId), "{\"occurredAt\":\"%s\"}".formatted(occurredAt))
                .andExpect(status().isCreated()).andReturn());
    }

    /** DM perziura, pvz. {"status":"APPROVED","xpAwarded":600}. */
    protected void review(String dmToken, long guildId, long questId, long logId, String json) throws Exception {
        patchAs(dmToken, logsUrl(guildId, questId) + "/" + logId, json).andExpect(status().isOk());
    }

    protected static String logsUrl(long guildId, long questId) {
        return "/api/guilds/" + guildId + "/quests/" + questId + "/logs";
    }
}
