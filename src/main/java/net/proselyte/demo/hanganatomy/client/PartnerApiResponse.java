package net.proselyte.demo.hanganatomy.client;

import tools.jackson.databind.JsonNode;

public record PartnerApiResponse(
        String externalRef,
        String partnerMode,
        double score,
        String recommendation,
        JsonNode payload
) {
}
