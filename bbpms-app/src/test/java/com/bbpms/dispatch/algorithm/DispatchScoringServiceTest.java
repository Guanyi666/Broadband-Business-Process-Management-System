package com.bbpms.dispatch.algorithm;

import com.bbpms.dispatch.config.DispatchProperties;
import com.bbpms.dispatch.dto.CandidateDTO;
import com.bbpms.dispatch.dto.InstallerDTO;
import com.bbpms.dispatch.dto.OrderDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DispatchScoringServiceTest {

    @Test
    void exposesInstallerIdentityAndAvailabilityForSelectionControls() {
        DispatchScoringService service = new DispatchScoringService(new DispatchProperties());
        InstallerDTO installer = new InstallerDTO(
                8L, "张师傅", null, null, 1, 5, 4.8, 1,
                List.of("FTTH"), List.of(), 1, "install8", "13800000008");

        CandidateDTO candidate = service.score(
                new OrderDTO(1L, "BB1", null, null, null, "PKG", List.of("FTTH")),
                List.of(installer), null).get(0);

        assertEquals(8L, candidate.installerId());
        assertEquals("install8", candidate.username());
        assertEquals("13800000008", candidate.phone());
        assertEquals("AVAILABLE", candidate.status());
    }
}
