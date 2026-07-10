drop view if exists avtale_admin_dto_view;

alter table avtale
    add fts tsvector generated always as (
        to_tsvector('norwegian', coalesce(avtale.avtalenummer, '') || ' ' ||
                                 coalesce(avtale.lopenummer, '') || ' ' ||
                                 coalesce(regexp_replace(avtale.lopenummer, '/', ' '), '') || ' ' ||
                                 avtale.navn)) stored;

create index avtale_fts_idx on avtale using gin (fts);
