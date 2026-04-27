package net.proselyte.demo.hanganatomy.observability;

import java.util.Set;

public final class MetricsContract {

    public static final String TAG_ENDPOINT = "endpoint";
    public static final String TAG_OUTCOME = "outcome";
    public static final String TAG_DEPENDENCY = "dependency";
    public static final String TAG_MODE = "mode";

    public static final String PARTNER_DURATION = "hang.partner.duration";
    public static final String PARTNER_ERRORS = "hang.partner.errors";
    public static final String PARTNER_TIMEOUTS = "hang.partner.timeouts";
    public static final String PARTNER_FALLBACKS = "hang.partner.fallbacks";
    public static final String PARTNER_REJECTIONS = "hang.partner.rejections";
    public static final String PARTNER_INFLIGHT = "hang.partner.inflight";
    public static final String DEMO_MODE = "hang.demo.mode";
    public static final String ADMIN_STATUS_DURATION = "hang.admin.status.duration";
    public static final String HEALTH_READINESS_DURATION = "hang.health.readiness.duration";
    public static final String HEALTH_READINESS_FAILURES = "hang.health.readiness.failures";
    public static final String RATE_LIMITER_ALLOWED = "hang.rate_limiter.allowed";
    public static final String RATE_LIMITER_REJECTIONS = "hang.rate_limiter.rejections";
    public static final String DB_BULKHEAD_REJECTIONS = "hang.db.bulkhead.rejections";

    public static final Set<String> LOW_CARDINALITY_TAGS = Set.of(
            TAG_ENDPOINT,
            TAG_OUTCOME,
            TAG_DEPENDENCY,
            TAG_MODE
    );

    private MetricsContract() {
    }
}
