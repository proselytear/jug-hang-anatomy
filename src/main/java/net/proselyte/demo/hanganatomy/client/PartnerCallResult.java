package net.proselyte.demo.hanganatomy.client;

public record PartnerCallResult(
        PartnerPayload payload,
        boolean fallback,
        String outcome
) {

    public static PartnerCallResult live(PartnerPayload payload) {
        return new PartnerCallResult(payload, false, "success");
    }

    public static PartnerCallResult fallback(PartnerPayload payload, String outcome) {
        return new PartnerCallResult(payload, true, outcome);
    }
}
