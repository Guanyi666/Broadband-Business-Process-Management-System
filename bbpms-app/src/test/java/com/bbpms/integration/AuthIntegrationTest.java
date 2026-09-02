package com.bbpms.integration;

import com.bbpms.auth.service.RsaKeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auth chain integration test (needs local MySQL + Redis, dev profile).
 *
 * <p>Covers: RSA public-key endpoint shape, real RSA-encrypted login returning an
 * access token, protected endpoint authorized by Bearer token, and HTTP 401 for a
 * malformed token (SA-P1-011 / SA-P1-012 regression).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private RsaKeyService rsaKeyService;

    @Test
    void publicKeyEndpointReturnsRawBase64() throws Exception {
        mockMvc.perform(get("/api/auth/public-key"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode node = TestClients.get(mockMvc, om, "/api/auth/public-key", null);
        assertEquals(0, node.get("code").asInt(), "code=0");
        String pk = node.get("data").asText();
        assertFalse(pk.isEmpty(), "public key non-empty");
        assertFalse(pk.contains("BEGIN PUBLIC KEY"), "raw base64, not PEM-wrapped");
    }

    @Test
    void loginReturnsAccessToken() throws Exception {
        TestClients.Login login = TestClients.login(mockMvc, om, rsaKeyService, "admin", "admin123");
        assertNotNull(login.token());
        assertTrue(login.userId() > 0, "user id present");
    }

    @Test
    void protectedEndpointAuthorizedWithBearerToken() throws Exception {
        TestClients.Login login = TestClients.login(mockMvc, om, rsaKeyService, "admin", "admin123");
        JsonNode page = TestClients.get(mockMvc, om,
                "/api/users/page?pageNum=1&pageSize=5", login.token());
        assertEquals(0, page.get("code").asInt(), "users/page authorized");
        assertTrue(page.get("data").get("total").asInt() > 0, "user rows present");
    }

    @Test
    void malformedTokenRejectedWithHttp401() throws Exception {
        mockMvc.perform(get("/api/users/page").header("Authorization", "Bearer not.a.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void missingTokenRejectedWithHttp401() throws Exception {
        mockMvc.perform(get("/api/users/page"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongPasswordRejectedAsBusinessError() throws Exception {
        String publicKey = rsaKeyService.getPublicKey();
        String enc = com.bbpms.common.util.CryptoUtils.rsaEncryptBase64(publicKey, "wrong-password");
        JsonNode node = TestClients.post(mockMvc, om, "/api/auth/login",
                java.util.Map.of("username", "admin", "password", enc), null);
        assertEquals(1002, node.get("code").asInt(), "PASSWORD_ERROR business code");
    }
}
