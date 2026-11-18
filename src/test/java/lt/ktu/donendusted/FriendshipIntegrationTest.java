package lt.ktu.donendusted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Draugystes: kvietimas, priemimas, draugo kortele, blokavimas ir 403/404/409/422. Kiekvienas testas - savi naudotojai. */
class FriendshipIntegrationTest extends IntegrationTest {

    @Test
    void requestAcceptViewSheetBlockAndUnblock() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        // bob - zaidejas dviejose alice grupese: draugo kortele sumuoja visas grupes.
        long guild1 = createGuild(alice.token());
        long guild2 = createGuild(alice.token());
        addMember(alice.token(), guild1, bob.username(), "PLAYER");
        addMember(alice.token(), guild2, bob.username(), "PLAYER");
        long con = createQuest(alice.token(), guild1, "CON", "HABIT", "EASY");   // 20 XP
        long cha = createQuest(alice.token(), guild2, "CHA", "HABIT", "MEDIUM"); // 30 XP
        Instant hourAgo = Instant.now().minus(Duration.ofHours(1));
        review(alice.token(), guild1, con, createLog(bob.token(), guild1, con, hourAgo),
                "{\"status\":\"APPROVED\",\"inspirationAwarded\":1}");
        review(alice.token(), guild2, cha, createLog(bob.token(), guild2, cha, hourAgo), "{\"status\":\"APPROVED\"}");

        // Kvietimas: 201 + Location, siuntejui OUTGOING, gavejui INCOMING.
        MvcResult created = invite(alice, bob.username())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(bob.id()))
                .andExpect(jsonPath("$.username").value(bob.username()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.direction").value("OUTGOING"))
                .andExpect(jsonPath("$.respondedAt").doesNotExist())
                .andReturn();
        long id = id(created);
        String url = "/api/friends/" + id;
        assertThat(created.getResponse().getHeader(HttpHeaders.LOCATION)).endsWith(url);
        getAs(bob.token(), "/api/friends?status=PENDING")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + id + ")].direction").value(hasItem("INCOMING")));

        // Pora jau yra - abiem kryptimis 409.
        invite(alice, bob.username()).andExpect(status().isConflict());
        invite(bob, alice.username()).andExpect(status().isConflict());

        // Kol nepriimta - kortele nematoma; priimti gali tik gavejas.
        getAs(alice.token(), url + "/sheet").andExpect(status().isForbidden());
        patchAs(alice.token(), url, "{\"status\":\"ACCEPTED\"}").andExpect(status().isForbidden());
        patchAs(bob.token(), url, "{\"status\":\"ACCEPTED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.direction").value("INCOMING"))
                .andExpect(jsonPath("$.respondedAt").exists());
        patchAs(bob.token(), url, "{\"status\":\"ACCEPTED\"}").andExpect(status().isUnprocessableEntity());

        // Draugo kortele: PUBLIC, per visas grupes, be grupes lauku ir irasu.
        getAs(alice.token(), url + "/sheet")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(bob.id()))
                .andExpect(jsonPath("$.view").value("PUBLIC"))
                .andExpect(jsonPath("$.guildId").doesNotExist())
                .andExpect(jsonPath("$.guildName").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist())
                .andExpect(jsonPath("$.recentLogs").doesNotExist())
                .andExpect(jsonPath("$.pendingCount").doesNotExist())
                .andExpect(jsonPath("$.totalXp").value(50))
                .andExpect(jsonPath("$.level").value(1))
                .andExpect(jsonPath("$.inspiration").value(1))
                .andExpect(jsonPath("$.streakDays").value(1))
                .andExpect(jsonPath("$.stats[2].stat").value("CON"))
                .andExpect(jsonPath("$.stats[2].xp").value(20))
                .andExpect(jsonPath("$.stats[5].stat").value("CHA"))
                .andExpect(jsonPath("$.stats[5].xp").value(30));

        // Blokavimas: blokuojantysis tampa OUTGOING, uzblokuotasis draugystes nebemato.
        patchAs(bob.token(), url, "{\"status\":\"BLOCKED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOCKED"))
                .andExpect(jsonPath("$.direction").value("OUTGOING"))
                .andExpect(jsonPath("$.userId").value(alice.id()));
        getAs(alice.token(), url).andExpect(status().isNotFound());
        getAs(alice.token(), "/api/friends").andExpect(jsonPath("$.content[*].id", not(hasItem((int) id))));
        deleteAs(alice.token(), url).andExpect(status().isNotFound());
        // Blokavimas neatskleidziamas - tas pats 409 kaip esamai draugystei.
        invite(alice, bob.username()).andExpect(status().isConflict());
        getAs(bob.token(), url + "/sheet").andExpect(status().isForbidden());

        // Atblokuoti gali tik uzblokaves; po to vel galima kviesti.
        deleteAs(bob.token(), url).andExpect(status().isNoContent());
        invite(alice, bob.username()).andExpect(status().isCreated());
    }

    @Test
    void cannotInviteSelfOrUnknownUser() throws Exception {
        TestUser alice = newUser();
        invite(alice, alice.username().toUpperCase()).andExpect(status().isUnprocessableEntity());
        invite(alice, unique("nobody_"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(endsWith("not found")));
        postAs(alice.token(), "/api/friends", "{\"username\":\" \"}").andExpect(status().isBadRequest());
    }

    @Test
    void strangerGets404ForSomeoneElsesFriendship() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        TestUser carol = newUser();
        String url = "/api/friends/" + id(invite(alice, bob.username()).andReturn());

        getAs(carol.token(), url).andExpect(status().isNotFound());
        patchAs(carol.token(), url, "{\"status\":\"BLOCKED\"}").andExpect(status().isNotFound());
        deleteAs(carol.token(), url).andExpect(status().isNotFound());
        getAs(carol.token(), url + "/sheet").andExpect(status().isNotFound());
    }

    @Test
    void listShowsBothDirectionsAndRejectsUnknownSort() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        TestUser carol = newUser();
        long outgoing = id(invite(alice, bob.username()).andReturn());
        long incoming = id(invite(carol, alice.username()).andReturn());

        getAs(alice.token(), "/api/friends")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[?(@.id == " + outgoing + ")].direction").value(hasItem("OUTGOING")))
                .andExpect(jsonPath("$.content[?(@.id == " + incoming + ")].direction").value(hasItem("INCOMING")));
        getAs(alice.token(), "/api/friends?sort=requester").andExpect(status().isBadRequest());
    }

    @Test
    void eitherPartyCanCancelOrUnfriend() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();

        // Siuntejas atsaukia kvietima.
        String url = "/api/friends/" + id(invite(alice, bob.username()).andReturn());
        deleteAs(alice.token(), url).andExpect(status().isNoContent());
        getAs(bob.token(), url).andExpect(status().isNotFound());

        // Gavejas nutraukia patvirtinta draugyste.
        url = "/api/friends/" + id(invite(alice, bob.username()).andReturn());
        patchAs(bob.token(), url, "{\"status\":\"ACCEPTED\"}").andExpect(status().isOk());
        deleteAs(bob.token(), url).andExpect(status().isNoContent());
        getAs(alice.token(), url).andExpect(status().isNotFound());
    }

    @Test
    void pendingIsNotAllowedAsPatchTarget() throws Exception {
        TestUser alice = newUser();
        TestUser bob = newUser();
        String url = "/api/friends/" + id(invite(alice, bob.username()).andReturn());

        patchAs(bob.token(), url, "{\"status\":\"PENDING\"}").andExpect(status().isUnprocessableEntity());
        patchAs(bob.token(), url, "{}").andExpect(status().isBadRequest());
    }

    private ResultActions invite(TestUser from, String username) throws Exception {
        return postAs(from.token(), "/api/friends", "{\"username\":\"%s\"}".formatted(username));
    }
}
