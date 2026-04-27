create table dashboard (
    id uuid primary key,
    customer_id uuid not null,
    title varchar(255) not null,
    "status" varchar(64) not null,
    created_at timestamp not null
);

create table dashboard_item (
    id uuid primary key,
    dashboard_id uuid not null references dashboard(id),
    metric_name varchar(255) not null,
    metric_value numeric(14, 2) not null,
    created_at timestamp not null
);

create table partner_snapshot (
    id uuid primary key,
    dashboard_id uuid not null references dashboard(id),
    external_ref varchar(255) not null,
    payload jsonb not null,
    created_at timestamp not null
);

create table admin_status (
    id bigint primary key,
    "status" varchar(64) not null,
    updated_at timestamp not null
);
