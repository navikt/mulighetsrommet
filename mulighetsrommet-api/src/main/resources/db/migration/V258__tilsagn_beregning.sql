drop view if exists tilsagn_admin_dto_view;
drop view if exists tilsagn_arrangorflate_view;

alter table tilsagn
    add belop_beregnet integer,
    add prismodell     prismodell;

create table tilsagn_forhandsgodkjent_beregning
(
    tilsagn_id     uuid    not null references tilsagn (id),
    sats           integer not null,
    antall_plasser integer not null
);

create index on tilsagn_forhandsgodkjent_beregning (tilsagn_id);

update tilsagn
set belop_beregnet = (beregning -> 'output' ->> 'belop')::integer,
    prismodell     = (beregning ->> 'type')::prismodell;

alter table tilsagn
    alter belop_beregnet set not null,
    alter prismodell set not null;

insert into tilsagn_forhandsgodkjent_beregning (tilsagn_id, sats, antall_plasser)
select id,
       (beregning -> 'input' ->> 'sats')::integer,
       (beregning -> 'input' ->> 'antallPlasser')::integer
from tilsagn
where (beregning ->> 'type') = 'FORHANDSGODKJENT';

alter table tilsagn
    drop column beregning;
