package com.bbpms.integration;

import com.bbpms.auth.service.RsaKeyService;
import com.bbpms.common.util.CryptoUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared helpers for the Sprint A integration tests.
 *
 * <p>Non-test class (no *Test/*IT suffix) — never picked up by Surefire/Failsafe.</p>
 */
public final class TestClients {

    /** Login result: JWT access token + the user id from the login payload. */
    public record Login(String token, long userId) {}

    private TestClients() {}

    /**
     * Full real-login flow over MockMvc: fetch the RSA public key, encrypt the
     * password with RSA/ECB/PKCS1, POST /api/auth/login, return the access token.
     */
    public static Login login(MockMvc mvc, ObjectMapper om, RsaKeyService rsaKeyService,
                              String username, String password) throws Exception {
        String publicKey = rsaKeyService.getPublicKey();
        String enc = CryptoUtils.rsaEncryptBase64(publicKey, password);

        MvcResult r = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"" + username + "\",\"password\":\"" + enc + "\"}"))
                .andReturn();

        assertEquals(200, r.getResponse().getStatus(), "login HTTP status for " + username);
        JsonNode node = om.readTree(r.getResponse().getContentAsString());
        assertEquals(0, node.get("code").asInt(), "login code for " + username);
        String token = node.get("data").get("accessToken").asText();
        long userId = node.get("data").get("user").get("id").asLong();
        assertTrue(!token.isEmpty(), "accessToken non-empty for " + username);
        return new Login(token, userId);
    }

    public static JsonNode post(MockMvc mvc, ObjectMapper om, String path, Object body, String token) throws Exception {
        return json(mvc, om, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path), body, token);
    }

    public static JsonNode get(MockMvc mvc, ObjectMapper om, String path, String token) throws Exception {
        return json(mvc, om, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path), null, token);
    }

    private static JsonNode json(MockMvc mvc, ObjectMapper om,
                                 org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder,
                                 Object body, String token) throws Exception {
        if (token != null) {
            builder = builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder = builder.contentType(MediaType.APPLICATION_JSON)
                    .content(om.writeValueAsString(body));
        }
        MvcResult r = mvc.perform(builder).andReturn();
        return om.readTree(r.getResponse().getContentAsString());
    }
}
