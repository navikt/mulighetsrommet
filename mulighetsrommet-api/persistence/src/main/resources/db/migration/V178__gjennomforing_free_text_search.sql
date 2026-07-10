drop view if exists tiltaksgjennomforing_admin_dto_view;
drop view if exists veilederflate_tiltak_view;

alter table tiltaksgjennomforing
    add fts tsvector generated always as (
        to_tsvector('norwegian', coalesce(tiltaksnummer, '') || ' ' || navn)) stored;

create index tiltaksgjennomforing_fts_idx on tiltaksgjennomforing using gin (fts);
