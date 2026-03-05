drop view if exists view_tilsagn;
drop view if exists view_avtale;
drop view if exists view_arrangorflate_tiltak;
drop view if exists view_gjennomforing;

create table tilsagn_deltaker (
    tilsagn_id uuid not null references tilsagn(id) on delete cascade,
    deltaker_id uuid not null references deltaker(id) on delete cascade,
    primary key (tilsagn_id, deltaker_id)
);

alter table prismodell
    add column med_deltakere boolean not null default false;
