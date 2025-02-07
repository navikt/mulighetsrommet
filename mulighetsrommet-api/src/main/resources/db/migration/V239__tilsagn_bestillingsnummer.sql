drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

delete
from delutbetaling;

alter table delutbetaling
    add lopenummer    int         not null,
    add fakturanummer text unique not null;

alter table delutbetaling
    add constraint unique_tilsagn_id_lopenummer unique (tilsagn_id, lopenummer);

delete
from tilsagn;

alter table tilsagn
    alter column lopenummer set data type int;

alter table tilsagn
    drop periode_start,
    drop periode_slutt,
    add bestillingsnummer text unique not null,
    add periode           daterange   not null;

alter table tilsagn
    add constraint unique_gjennomforing_id_lopenummer unique (gjennomforing_id, lopenummer);

drop function if exists next_tilsagn_lopenummer cascade;
