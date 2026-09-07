package az.fitnest.payment.util;

import az.fitnest.payment.dto.epoint.EpointResponse;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Epoint transaction ids: {@code te} (redirect checkout) and {@code tw} (widget / Apple Pay).
 * Live ids pad the numeric token to 9 digits ({@code te022240154}). Widget URLs omit the prefix
 * and leading zeros ({@code /widget/22240181}), so callers must try equivalent forms when
 * matching callbacks or querying {@code /get-status}.
 */
public final class EpointTransactionIds {

    private EpointTransactionIds() {
    }

    /** Canonical widget id stored on create: {@code tw} + 9-digit padded token. */
    public static String fromWidgetToken(String token) {
        String digits = digitsOf(token);
        if (digits.isEmpty()) {
            return token == null ? null : token.trim();
        }
        return "tw" + pad(digits, 9);
    }

    /**
     * Lookup/query variants for a stored or callback transaction id, most likely first.
     * Includes 9- and 10-digit {@code tw}/{@code te} forms so historic 10-digit widget rows still match.
     */
    public static List<String> lookupCandidates(String... ids) {
        Set<String> out = new LinkedHashSet<>();
        for (String id : ids) {
            if (id == null || id.isBlank()) {
                continue;
            }
            String trimmed = id.trim();
            out.add(trimmed);
            if (trimmed.indexOf('-') >= 0) {
                continue;
            }
            String digits = digitsOf(trimmed);
            if (digits.isEmpty() || digits.length() > 12) {
                continue;
            }
            String pad9 = pad(digits, 9);
            String pad10 = pad(digits, 10);
            out.add("tw" + pad9);
            out.add("tw" + pad10);
            out.add("te" + pad9);
            out.add("te" + pad10);
            out.add("tw" + stripLeadingZeros(digits));
            out.add("te" + stripLeadingZeros(digits));
        }
        return new ArrayList<>(out);
    }

    /** Epoint recognized this id: do not try another padding variant. */
    public static boolean isDefinitiveStatus(EpointResponse response) {
        if (response == null || response.status() == null || response.status().isBlank()) {
            return false;
        }
        String status = response.status().toLowerCase(Locale.ROOT);
        if ("success".equals(status) || "returned".equals(status) || "new".equals(status)) {
            return true;
        }
        if ("error".equals(status) || "failed".equals(status)) {
            return hasBankAttempt(response);
        }
        return false;
    }

    public static boolean hasBankAttempt(EpointResponse response) {
        if (response == null) {
            return false;
        }
        return (response.cardMask() != null && !response.cardMask().isBlank())
                || (response.bankTransaction() != null && !response.bankTransaction().isBlank())
                || (response.bankResponse() != null && !response.bankResponse().isBlank())
                || (response.code() != null && !response.code().isBlank()
                && !"500".equals(response.code()) && !"ERROR".equalsIgnoreCase(response.code()));
    }

    static String digitsOf(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        StringBuilder digits = new StringBuilder();
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            }
        }
        return digits.toString();
    }

    private static String pad(String digits, int width) {
        long n = Long.parseLong(digits);
        return String.format("%0" + width + "d", n);
    }

    private static String stripLeadingZeros(String digits) {
        int i = 0;
        while (i < digits.length() - 1 && digits.charAt(i) == '0') {
            i++;
        }
        return digits.substring(i);
    }
}
