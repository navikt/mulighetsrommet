drop view if exists view_avtale;
drop view if exists view_gjennomforing;
drop view if exists view_gjennomforing_enkeltplass;
drop view if exists view_tilsagn;
drop view if exists view_utbetaling;
drop view if exists view_veilederflate_tiltak;

create table gjennomforing_gruppetiltak
(
    gjennomforing_id               uuid                                   not null primary key references gjennomforing on delete cascade,
    created_at                     timestamp with time zone default now() not null,
    updated_at                     timestamp with time zone default now() not null,
    avtale_id                      uuid                                   not null references avtale,
    oppstart                       gjennomforing_oppstartstype            not null,
    pamelding_type                 pamelding_type                         not null,
    faneinnhold                    jsonb,
    beskrivelse                    text,
    publisert                      boolean                  default false not null,
    apent_for_pamelding            boolean                  default true  not null,
    tilgjengelig_for_arrangor_dato date,
    oppmote_sted                   text,
    estimert_ventetid_verdi        integer,
    estimert_ventetid_enhet        text
);

create index gjennomforing_gruppetiltak_avtale_id_idx on gjennomforing_gruppetiltak (avtale_id);

create trigger set_timestamp
    before update
    on gjennomforing_gruppetiltak
    for each row
execute procedure trigger_set_timestamp();

create type gjennomforing_type as enum ('GRUPPETILTAK', 'ARENA_GRUPPETILTAK', 'ARENA_ENKELTPLASS');

alter table gjennomforing
    add gjennomforing_type gjennomforing_type;

update gjennomforing
set gjennomforing_type = 'GRUPPETILTAK'
where avtale_id is not null;

update gjennomforing
set gjennomforing_type = 'ARENA_GRUPPETILTAK'
where avtale_id is null;

alter table gjennomforing
    alter gjennomforing_type set not null;

insert into gjennomforing_gruppetiltak(gjennomforing_id,
                                       avtale_id,
                                       oppstart,
                                       pamelding_type,
                                       faneinnhold,
                                       beskrivelse,
                                       publisert,
                                       apent_for_pamelding,
                                       tilgjengelig_for_arrangor_dato,
                                       oppmote_sted,
                                       estimert_ventetid_verdi,
                                       estimert_ventetid_enhet)
select id,
       avtale_id,
       oppstart,
       pamelding_type,
       faneinnhold,
       beskrivelse,
       publisert,
       apent_for_pamelding,
       tilgjengelig_for_arrangor_dato,
       oppmote_sted,
       estimert_ventetid_verdi,
       estimert_ventetid_enhet
from gjennomforing
where avtale_id is not null;

alter table gjennomforing
    drop fts,
    drop avtale_id,
    drop oppstart,
    drop pamelding_type,
    drop faneinnhold,
    drop beskrivelse,
    drop publisert,
    drop apent_for_pamelding,
    drop tilgjengelig_for_arrangor_dato,
    drop oppmote_sted,
    drop estimert_ventetid_verdi,
    drop estimert_ventetid_enhet;

alter table gjennomforing
    add fts tsvector;

update gjennomforing
set fts = to_tsvector('norwegian',
                      concat_ws(' ',
                                lopenummer,
                                regexp_replace(lopenummer, '/', ' '),
                                coalesce(arena_tiltaksnummer, ''),
                                navn
                      )
          );

create index gjennomforing_fts_idx on gjennomforing using gin (fts);

insert into gjennomforing (id,
                           created_at,
                           updated_at,
                           tiltakstype_id,
                           arrangor_id,
                           opphav,
                           gjennomforing_type,
                           arena_tiltaksnummer,
                           arena_ansvarlig_enhet,
                           navn,
                           start_dato,
                           slutt_dato,
                           status,
                           deltidsprosent,
                           antall_plasser)
select id,
       created_at,
       updated_at,
       tiltakstype_id,
       arrangor_id,
       'ARENA'::opphav,
       'ARENA_ENKELTPLASS',
       arena_tiltaksnummer,
       arena_ansvarlig_enhet,
       arena_navn,
       arena_start_dato,
       arena_slutt_dato,
       arena_status,
       100.00,
       1
from enkeltplass;

drop table enkeltplass;
