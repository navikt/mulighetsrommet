create table tilsagn_fri_beregning
(
    id          uuid primary key default gen_random_uuid(),
    tilsagn_id  uuid    not null references tilsagn (id) on delete cascade,
    beskrivelse text    not null,
    belop       integer not null,
    antall      integer not null
);

create index on tilsagn_fri_beregning (tilsagn_id);

insert into tilsagn_fri_beregning (tilsagn_id, beskrivelse, belop, antall)
select t.id, '<mangler beskrivelse>', t.belop_beregnet, 1
from tilsagn t
where t.prismodell = 'FRI';
