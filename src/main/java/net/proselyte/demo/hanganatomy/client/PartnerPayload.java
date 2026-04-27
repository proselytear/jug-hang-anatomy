package net.proselyte.demo.hanganatomy.client;

import tools.jackson.databind.JsonNode;

public record PartnerPayload(
        String source,
        String externalRef,
        String partnerMode,
        double score,
        String recommendation,
        JsonNode payload
) {
}
