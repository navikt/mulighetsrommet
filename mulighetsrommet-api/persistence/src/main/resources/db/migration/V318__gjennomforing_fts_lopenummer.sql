drop view if exists gjennomforing_admin_dto_view;
drop view if exists veilederflate_tiltak_view;

alter table gjennomforing
    drop column fts;

alter table gjennomforing
    add fts tsvector generated always as (
        to_tsvector('norwegian',
            coalesce(tiltaksnummer, '') || ' ' ||
            coalesce(navn, '') || ' ' ||
            coalesce(lopenummer, '')
        )
    ) stored;

create index gjennomforing_fts_idx on gjennomforing using gin (fts);
