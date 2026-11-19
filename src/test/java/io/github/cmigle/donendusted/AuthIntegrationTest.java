package io.github.cmigle.donendusted;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Prisijungimas, refresh rotacija, pakartotinio panaudojimo aptikimas, atsijungimas ir 401. */
class AuthIntegrationTest extends IntegrationTest {

    @Test
    void loginReturnsAccessAndRefreshTokens() throws Exception {
        login("rokas", PASSWORD)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        login("rokas", "Blogas123!")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    void requestWithoutTokenIs401WithBearerChallenge() throws Exception {
        getAs(null, "/api/guilds")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void brokenTokenIs401() throws Exception {
        getAs("not.a.jwt", "/api/auth/me")
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("invalid_token")));
    }

    @Test
    void publicAuthEndpointsIgnoreBrokenToken() throws Exception {
        postAs("not.a.jwt", "/api/auth/login", "{\"username\":\"jonas\",\"password\":\"%s\"}".formatted(PASSWORD))
                .andExpect(status().isOk());
    }

    @Test
    void refreshRotatesTokensAndReuseRevokesAllSessions() throws Exception {
        TestUser user = newUser();
        String first = refreshToken(login(user.username(), PASSWORD));

        MvcResult rotated = refresh(first).andExpect(status().isOk()).andReturn();
        String second = JsonPath.read(body(rotated), "$.refreshToken");
        String access = JsonPath.read(body(rotated), "$.accessToken");
        assertThat(second).isNotEqualTo(first);
        getAs(access, "/api/auth/me")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.username()));

        // Senasis zetonas jau panaudotas - laikomas pavogtu, atsaukiamos visos sesijos, ir naujoji.
        refresh(first)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(containsString("All sessions have been revoked")));
        refresh(second).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesRefreshTokenAndIsIdempotent() throws Exception {
        TestUser user = newUser();
        String refreshToken = refreshToken(login(user.username(), PASSWORD));

        logout(refreshToken).andExpect(status().isNoContent());
        refresh(refreshToken).andExpect(status().isUnauthorized());
        logout(refreshToken).andExpect(status().isNoContent());
    }

    private String refreshToken(ResultActions login) throws Exception {
        return JsonPath.read(body(login.andExpect(status().isOk()).andReturn()), "$.refreshToken");
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return postAs(null, "/api/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }

    private ResultActions logout(String refreshToken) throws Exception {
        return postAs(null, "/api/auth/logout", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }
}
