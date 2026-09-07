package az.fitnest.payment.util;

import az.fitnest.payment.dto.epoint.EpointResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EpointTransactionIdsTest {

    @Test
    void widgetTokenMatchesLiveNineDigitTeShape() {
        assertEquals("tw022240181", EpointTransactionIds.fromWidgetToken("22240181"));
        assertEquals("tw022240181", EpointTransactionIds.fromWidgetToken("022240181"));
    }

    @Test
    void historicTenDigitWidgetIdStillProducesNineDigitCandidate() {
        List<String> candidates = EpointTransactionIds.lookupCandidates("tw0022240181");
        assertTrue(candidates.contains("tw0022240181"));
        assertTrue(candidates.contains("tw022240181"));
        assertEquals("tw0022240181", candidates.get(0));
        assertEquals("tw022240181", candidates.get(1));
    }

    @Test
    void uuidIsNotTreatedAsNumericToken() {
        List<String> candidates = EpointTransactionIds.lookupCandidates(
                "tw0022240181", "3874721e-3adb-4761-bf85-6b32c5f1990a");
        assertTrue(candidates.contains("tw0022240181"));
        assertTrue(candidates.contains("tw022240181"));
        assertTrue(candidates.contains("3874721e-3adb-4761-bf85-6b32c5f1990a"));
        assertFalse(candidates.stream().anyMatch(id -> id.length() > 20 && !id.contains("-")));
    }

    @Test
    void callbackWithoutPrefixStillMatchesStoredWidgetId() {
        List<String> candidates = EpointTransactionIds.lookupCandidates("22240181");
        assertTrue(candidates.contains("tw022240181"));
        assertTrue(candidates.contains("tw0022240181"));
    }

    @Test
    void serverErrorWithoutAttemptIsNotDefinitive() {
        EpointResponse unknown = EpointResponse.builder()
                .status("server_error")
                .code("500")
                .build();
        assertFalse(EpointTransactionIds.isDefinitiveStatus(unknown));
        assertFalse(EpointTransactionIds.hasBankAttempt(unknown));
    }

    @Test
    void newAndSuccessAreDefinitive() {
        assertTrue(EpointTransactionIds.isDefinitiveStatus(
                EpointResponse.builder().status("new").build()));
        assertTrue(EpointTransactionIds.isDefinitiveStatus(
                EpointResponse.builder().status("success").rrn("123").build()));
    }
}
