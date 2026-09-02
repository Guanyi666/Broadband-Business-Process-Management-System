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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Order-domain integration test (needs local MySQL + Redis, dev profile).
 *
 * <p>Runs inside a test transaction that is rolled back afterwards, so no rows
 * survive. Covers the create-customer → create-order → list → decrypted-search →
 * detail round trip (SA-P0-008 customers/page and SA-P0-009 orders/page regression).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private RsaKeyService rsaKeyService;

    @Test
    void customerAndOrderLifecycleRoundTrip() throws Exception {
        TestClients.Login cs = TestClients.login(mockMvc, om, rsaKeyService, "cs1", "admin123");

        String phone = "139" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        String name = "E2E-IT客户" + System.nanoTime() % 10_000_000;

        // 1. create customer (SM4-encrypted PII path)
        // NB: idCardNo must be unique per run — upsert is idempotent by idCardNo too,
        // and reusing the e2e scripts' fixed idCardNo would return their customer.
        String idCardNo = "110101" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        JsonNode cust = TestClients.post(mockMvc, om, "/api/customers", Map.of(
                "name", name, "phone", phone, "idCardNo", idCardNo,
                "address", "北京市朝阳区望京SOHO T3", "province", "北京", "city", "北京市", "district", "朝阳区"
        ), cs.token());
        assertEquals(0, cust.get("code").asInt(), "customer create");
        long customerId = cust.get("data").asLong();
        assertTrue(customerId > 0, "customerId returned");

        // 2. create order
        JsonNode ord = TestClients.post(mockMvc, om, "/api/orders", Map.of(
                "customerId", customerId, "packageCode", "FIBER_100M", "packageName", "光纤宽带100M",
                "installAddress", "北京市朝阳区望京SOHO T3-1502", "expectedInstallDate", "2026-09-03T10:00:00",
                "appointmentTime", "2026-09-02T14:00:00", "contactPhone", phone, "remark", "E2E-IT订单"
        ), cs.token());
        assertEquals(0, ord.get("code").asInt(), "order create");
        long orderId = ord.get("data").asLong();
        assertTrue(orderId > 0, "orderId returned");

        // 3. orders/page — real pagination, newest first (SA-P0-009 regression)
        JsonNode page = TestClients.get(mockMvc, om, "/api/orders/page?pageNum=1&pageSize=50", cs.token());
        assertEquals(0, page.get("code").asInt(), "orders/page HTTP-clean");
        boolean found = false;
        for (JsonNode row : page.get("data").get("records")) {
            if (row.get("id").asLong() == orderId) { found = true; break; }
        }
        assertTrue(found, "new order visible in orders/page");

        // 4. customers/search — decrypted in-memory match (SA-P0-008 regression).
        // NB: use .param() so MockMvc URL-encodes the keyword; hand-built
        // "?keyword=" + URLEncoder.encode(...) URLs get mangled by UriTemplate.
        MvcResult searchRes = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/customers/search")
                        .param("keyword", name)
                        .param("limit", "10")
                        .header("Authorization", "Bearer " + cs.token()))
                .andReturn();
        JsonNode hits = om.readTree(searchRes.getResponse().getContentAsString());
        assertEquals(0, hits.get("code").asInt(), "customers/search");
        boolean hit = false;
        for (JsonNode c : hits.get("data")) {
            if (c.get("id").asLong() == customerId) { hit = true; break; }
        }
        assertTrue(hit, "new customer found via decrypted search");

        // 5. order detail
        JsonNode detail = TestClients.get(mockMvc, om, "/api/orders/" + orderId, cs.token());
        assertEquals(0, detail.get("code").asInt(), "order detail");
        assertEquals("CREATED", detail.get("data").get("order").get("status").asText(), "fresh order is CREATED");
        assertTrue(!detail.get("data").get("order").get("orderNo").asText().isEmpty(), "orderNo present");
    }
}
