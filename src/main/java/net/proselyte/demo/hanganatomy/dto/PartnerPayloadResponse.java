package net.proselyte.demo.hanganatomy.dto;

import tools.jackson.databind.JsonNode;

public record PartnerPayloadResponse(
        String source,
        String externalRef,
        String partnerMode,
        double score,
        String recommendation,
        JsonNode payload
) {
}
