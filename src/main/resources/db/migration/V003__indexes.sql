create index idx_dashboard_customer_id on dashboard(customer_id);
create index idx_dashboard_item_dashboard_id on dashboard_item(dashboard_id);
create index idx_partner_snapshot_dashboard_id on partner_snapshot(dashboard_id);
create index idx_partner_snapshot_dashboard_created_at on partner_snapshot(dashboard_id, created_at desc);
