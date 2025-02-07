delete
from delutbetaling;

delete
from tilsagn;

alter table tilsagn
    add bestillingsnummer text unique not null;

alter table tilsagn
    alter column lopenummer set data type int;

alter table tilsagn
    add constraint unique_gjennomforing_id_lopenummer unique (gjennomforing_id, lopenummer);

drop function if exists next_tilsagn_lopenummer cascade;
--
-- create or replace function generate_tilsagn_lopenummer_and_bestillingsnummer() returns trigger as
-- $$
-- begin
--     select coalesce(max(lopenummer), 0) + 1
--     into new.lopenummer
--     from tilsagn
--     where gjennomforing_id = new.gjennomforing_id;
--
--     select gjennomforing.lopenummer || '/' || new.lopenummer
--     into new.bestillingsnummer
--     from gjennomforing
--     where gjennomforing.id = new.gjennomforing_id;
--
--     return new;
-- end;
-- $$ language plpgsql;
--
-- create trigger set_tilsagn_lopenummer
--     before insert
--     on tilsagn
--     for each row
-- execute function generate_tilsagn_lopenummer_and_bestillingsnummer();
