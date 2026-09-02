package com.bbpms.integration;

import com.bbpms.auth.service.RsaKeyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full core-business-chain integration test (needs local MySQL + Redis, dev profile).
 *
 * <p>Intentionally NOT @Transactional: the work-order auto-creation after audit runs
 * in an {@code @Async} post-commit listener (OrderAuditedListener), so the chain must
 * really commit. Uses unique ids (E2E-IT-*) so re-runs are idempotent; leaves the same
 * kind of test rows the e2e scripts leave, and cleans up the Redis online/workload keys
 * it wrote so the seeded installers do not stay "online" after the run.</p>
 *
 * <p>Covers: create → audit → auto work order → dispatch → accept → start →
 * arrive/info/photos/signature → complete (BSS mock) → COMPLETED + FINISHED
 * (SA-P0-007 / SA-P1-006 regression).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkOrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper om;
    @Autowired
    private RsaKeyService rsaKeyService;
    @Autowired
    private StringRedisTemplate redis;

    private static final String UNIQUE = "E2E-IT-" + (System.nanoTime() % 100_000_000);
    private String phone;
    private String tokenCs, tokenAudit, tokenDisp;
    private final Map<Long, String> installerTokens = new HashMap<>();
    private long orderId;
    private long workOrderId;
    private long pickedInstallerId;

    @Test
    @Order(1)
    void createAuditAndAutoCreateWorkOrder() throws Exception {
        TestClients.Login cs = TestClients.login(mockMvc, om, rsaKeyService, "cs1", "admin123");
        TestClients.Login audit = TestClients.login(mockMvc, om, rsaKeyService, "audit1", "admin123");
        TestClients.Login disp = TestClients.login(mockMvc, om, rsaKeyService, "disp1", "admin123");
        tokenCs = cs.token();
        tokenAudit = audit.token();
        tokenDisp = disp.token();
        for (String u : new String[]{"install1", "install2", "install3", "install4", "install5"}) {
            TestClients.Login inst = TestClients.login(mockMvc, om, rsaKeyService, u, "admin123");
            installerTokens.put(inst.userId(), inst.token());
        }

        phone = "138" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        // idCardNo must be unique per run — upsert is idempotent by idCardNo too
        String idCardNo = "110101" + String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        JsonNode cust = TestClients.post(mockMvc, om, "/api/customers", Map.of(
                "name", "IT链路客户", "phone", phone, "idCardNo", idCardNo,
                "address", "北京市海淀区中关村软件园", "province", "北京", "city", "北京市", "district", "海淀区"
        ), tokenCs);
        assertEquals(0, cust.get("code").asInt(), "customer create");
        long customerId = cust.get("data").asLong();

        JsonNode ord = TestClients.post(mockMvc, om, "/api/orders", Map.of(
                "customerId", customerId, "packageCode", "FIBER_100M", "packageName", "光纤宽带100M",
                "installAddress", "北京市海淀区中关村软件园二期", "expectedInstallDate", "2026-09-04T10:00:00",
                "appointmentTime", "2026-09-03T09:00:00", "contactPhone", phone, "remark", UNIQUE
        ), tokenCs);
        assertEquals(0, ord.get("code").asInt(), "order create");
        orderId = ord.get("data").asLong();

        JsonNode aud = TestClients.post(mockMvc, om, "/api/orders/" + orderId + "/audit",
                Map.of("pass", true, "remark", "IT审核通过"), tokenAudit);
        assertEquals(0, aud.get("code").asInt(), "audit pass");

        // Poll for the async post-commit work-order creation (≤10s).
        // data may be null while the async listener has not created the order yet.
        workOrderId = 0;
        for (int i = 0; i < 20 && workOrderId == 0; i++) {
            JsonNode wo = TestClients.get(mockMvc, om, "/api/work-orders/by-order/" + orderId, tokenAudit);
            JsonNode data = wo.get("data");
            if (wo.get("code").asInt() == 0 && data != null && !data.isNull() && data.has("id") && !data.get("id").isNull()) {
                workOrderId = data.get("id").asLong();
            } else {
                Thread.sleep(500);
            }
        }
        assertTrue(workOrderId > 0, "work order auto-created after audit (async listener)");
    }

    @Test
    @Order(2)
    void dispatchAndFullInstallFlow() throws Exception {
        assertTrue(orderId > 0, "orderId from step 1");

        // installer goes online (Redis installers:active)
        long firstInstallerId = installerTokens.keySet().iterator().next();
        JsonNode loc = TestClients.post(mockMvc, om, "/api/installers/location", Map.of(
                "userId", firstInstallerId, "lat", 39.9803, "lng", 116.3094, "onDuty", 1), installerTokens.get(firstInstallerId));
        assertEquals(0, loc.get("code").asInt(), "installer online");

        // auto dispatch
        JsonNode disp = TestClients.post(mockMvc, om, "/api/dispatch/auto?orderId=" + orderId, null, tokenDisp);
        assertEquals(0, disp.get("code").asInt(), "dispatch/auto");
        assertEquals(workOrderId, disp.get("data").get("workOrderId").asLong(), "dispatched workOrderId matches");
        pickedInstallerId = disp.get("data").get("installerId").asLong();
        String instToken = installerTokens.get(pickedInstallerId);
        assertNotNull(instToken, "dispatched installer is a known seeded installer");

        // accept → start
        JsonNode acc = TestClients.post(mockMvc, om, "/api/work-orders/" + workOrderId + "/accept", null, instToken);
        assertEquals(0, acc.get("code").asInt(), "accept");
        assertEquals("ACCEPTED", acc.get("data").get("status").asText(), "status ACCEPTED");
        JsonNode st = TestClients.post(mockMvc, om, "/api/work-orders/" + workOrderId + "/start", null, instToken);
        assertEquals(0, st.get("code").asInt(), "start");
        assertEquals("IN_PROGRESS", st.get("data").get("status").asText(), "status IN_PROGRESS");

        // install steps
        JsonNode ar = TestClients.post(mockMvc, om, "/api/install/" + workOrderId + "/arrive", Map.of(
                "lat", 39.9803, "lng", 116.3094, "address", "海淀区中关村软件园二期"), instToken);
        assertEquals(0, ar.get("code").asInt(), "arrive");
        JsonNode inf = TestClients.post(mockMvc, om, "/api/install/" + workOrderId + "/info", Map.of(
                "onuMac", "AA:BB:CC:DD:EE:FF", "onuSn", "SN-IT-0001", "oltPort", "0/1/1", "signal", -18.5), instToken);
        assertEquals(0, inf.get("code").asInt(), "info");
        // complete requires >= minCount (default 3) persisted photos — one call per photo
        for (int i = 1; i <= 3; i++) {
            JsonNode ph = TestClients.post(mockMvc, om, "/api/install/" + workOrderId + "/photos", Map.of(
                    "url", "http://mock.local/it/photo" + i + ".jpg", "objectKey", "it/" + workOrderId + "/photo" + i + ".jpg"), instToken);
            assertEquals(0, ph.get("code").asInt(), "photos #" + i);
        }
        JsonNode sg = TestClients.post(mockMvc, om, "/api/install/" + workOrderId + "/signature", Map.of(
                "customerName", "IT链路客户", "dataUrl", "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==",
                "objectKey", "it/" + workOrderId + "/sign.png"), instToken);
        assertEquals(0, sg.get("code").asInt(), "signature");

        // complete → BSS mock fires
        JsonNode cp = TestClients.post(mockMvc, om, "/api/install/" + workOrderId + "/complete", Map.of(
                "orderId", orderId, "lat", 39.9803, "lng", 116.3094, "distance", 520, "remark", "IT完工"), instToken);
        assertEquals(0, cp.get("code").asInt(), "complete");

        // final states
        JsonNode wo = TestClients.get(mockMvc, om, "/api/work-orders/" + workOrderId, tokenAudit);
        assertEquals("COMPLETED", wo.get("data").get("status").asText(), "work order COMPLETED");
        JsonNode order = TestClients.get(mockMvc, om, "/api/orders/" + orderId, tokenAudit);
        assertEquals("FINISHED", order.get("data").get("order").get("status").asText(), "order FINISHED");
    }

    @AfterAll
    void cleanup() {
        // Only Redis state persists past the run; DB rows are unique-id test data
        // (same policy as the e2e scripts). Remove the online set + today's workload
        // so the seeded installers are not left "online"/loaded for manual testing.
        redis.delete("installers:active");
        redis.delete("installers:workload:" + LocalDate.now());
    }
}
