drop view if exists view_tilsagn;

alter table tilsagn
    rename type to tilsagn_type;

alter table tilsagn
    add updated_at timestamptz default now();

create trigger set_timestamp
    before update
    on tilsagn
    for each row
execute procedure trigger_set_timestamp();
