package lt.ktu.donendusted;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

/**
 * Ne API keliai - React sasajos puslapiai: vieši, be zetono. API lieka saugomas.
 * Ar index.html supakuotas, priklauso nuo build'o (200 arba 404), bet niekada ne 401.
 */
class FrontendRoutingIntegrationTest extends IntegrationTest {

    @Test
    void frontendRoutesDoNotRequireToken() throws Exception {
        for (String path : new String[] {"/taverna", "/grupes/2/uzduotys/4"}) {
            mvc.perform(get(path)).andExpect(result -> assertNotEquals(401, result.getResponse().getStatus(), path));
        }
    }

    @Test
    void apiStillRequiresToken() throws Exception {
        mvc.perform(get("/api/guilds"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"));
    }

    @Test
    void unknownApiPathIsJson404NotIndexHtml() throws Exception {
        getAs(token("rokas"), "/api/nera-tokio")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
