insert into dashboard (id, customer_id, title, "status", created_at)
select
    format('00000000-0000-0000-0000-%s', lpad(gs::text, 12, '0'))::uuid,
    format('10000000-0000-0000-0000-%s', lpad((((gs - 1) % 100) + 1)::text, 12, '0'))::uuid,
    format('Dashboard %s', gs),
    case when gs % 7 = 0 then 'DEGRADED' when gs % 5 = 0 then 'PENDING' else 'ACTIVE' end,
    now() - make_interval(days => gs % 30, mins => gs % 180)
from generate_series(1, 1000) gs;

insert into dashboard_item (id, dashboard_id, metric_name, metric_value, created_at)
select
    md5(format('item-%s-%s', d.id, item_no))::uuid,
    d.id,
    format('metric_%s', item_no),
    round((random() * 1000)::numeric, 2),
    d.created_at + make_interval(secs => item_no)
from dashboard d
cross join lateral generate_series(1, 10 + ((abs(hashtext(d.id::text)) % 41))) item_no;

insert into partner_snapshot (id, dashboard_id, external_ref, payload, created_at)
select
    md5(format('partner-%s', d.id))::uuid,
    d.id,
    format('partner-%s', right(d.id::text, 12)),
    jsonb_build_object(
        'externalRef', format('partner-%s', right(d.id::text, 12)),
        'partnerMode', 'normal',
        'score', 40 + (abs(hashtext(d.id::text)) % 60),
        'recommendation', 'cached-snapshot'
    ),
    d.created_at + interval '5 minutes'
from dashboard d
where mod(abs(hashtext(d.id::text)), 3) = 0;

insert into admin_status (id, "status", updated_at)
values (1, 'GREEN', now())
on conflict (id) do update
set "status" = excluded."status",
    updated_at = excluded.updated_at;
