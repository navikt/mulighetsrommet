drop view if exists view_tilsagn;

alter type prismodell_type add value 'ANNEN_AVTALT_PRIS_PER_DELTAKER';

create table tilsagn_deltaker (
    tilsagn_id uuid not null references tilsagn(id) on delete cascade,
    deltaker_id uuid not null references deltaker(id) on delete cascade,
    primary key (tilsagn_id, deltaker_id)
);
