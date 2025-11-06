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
    avtale_id                      uuid                                   references avtale on delete set null,
    navn                           text                                   not null,
    start_dato                     date                                   not null,
    slutt_dato                     date,
    antall_plasser                 integer                                not null,
    oppstart                       gjennomforing_oppstartstype            not null,
    pamelding_type                 pamelding_type                         not null,
    faneinnhold                    jsonb,
    beskrivelse                    text,
    publisert                      boolean                  default false not null,
    apent_for_pamelding            boolean                  default true  not null,
    tilgjengelig_for_arrangor_dato date,
    oppmote_sted                   text,
    deltidsprosent                 numeric(5, 2)                          not null,
    estimert_ventetid_verdi        integer,
    estimert_ventetid_enhet        text,
    status                         gjennomforing_status                   not null,
    avsluttet_tidspunkt            timestamp,
    avbrutt_aarsaker               text[],
    avbrutt_forklaring             text
);

create index gjennomforing_gruppetiltak_status_idx on gjennomforing_gruppetiltak (status);
create index gjennomforing_gruppetiltak_avtale_id_idx on gjennomforing_gruppetiltak (avtale_id);
create index gjennomforing_gruppetiltak_start_dato_idx on gjennomforing_gruppetiltak (start_dato);
create index gjennomforing_gruppetiltak_slutt_dato_idx on gjennomforing_gruppetiltak (slutt_dato);

create trigger set_timestamp
    before update
    on gjennomforing_gruppetiltak
    for each row
execute procedure trigger_set_timestamp();

insert into gjennomforing_gruppetiltak(gjennomforing_id,
                                       avtale_id,
                                       navn,
                                       start_dato,
                                       slutt_dato,
                                       antall_plasser,
                                       oppstart,
                                       pamelding_type,
                                       faneinnhold,
                                       beskrivelse,
                                       publisert,
                                       apent_for_pamelding,
                                       tilgjengelig_for_arrangor_dato,
                                       oppmote_sted,
                                       deltidsprosent,
                                       estimert_ventetid_verdi,
                                       estimert_ventetid_enhet,
                                       status,
                                       avsluttet_tidspunkt,
                                       avbrutt_aarsaker,
                                       avbrutt_forklaring)
select id,
       avtale_id,
       navn,
       start_dato,
       slutt_dato,
       antall_plasser,
       oppstart,
       pamelding_type,
       faneinnhold,
       beskrivelse,
       publisert,
       apent_for_pamelding,
       tilgjengelig_for_arrangor_dato,
       oppmote_sted,
       deltidsprosent,
       estimert_ventetid_verdi,
       estimert_ventetid_enhet,
       status,
       avsluttet_tidspunkt,
       avbrutt_aarsaker,
       avbrutt_forklaring
from gjennomforing;

alter table gjennomforing
    drop fts,
    drop avtale_id,
    drop navn,
    drop start_dato,
    drop slutt_dato,
    drop antall_plasser,
    drop oppstart,
    drop pamelding_type,
    drop faneinnhold,
    drop beskrivelse,
    drop publisert,
    drop apent_for_pamelding,
    drop tilgjengelig_for_arrangor_dato,
    drop oppmote_sted,
    drop deltidsprosent,
    drop estimert_ventetid_verdi,
    drop estimert_ventetid_enhet,
    drop status,
    drop avsluttet_tidspunkt,
    drop avbrutt_aarsaker,
    drop avbrutt_forklaring;

alter table gjennomforing
    add fts tsvector;

with gruppe as (select id, arena_tiltaksnummer, lopenummer, navn
                from gjennomforing_gruppetiltak
                         join gjennomforing on id = gjennomforing_id)
update gjennomforing
set fts = to_tsvector('norwegian',
                      concat_ws(' ',
                                gruppe.lopenummer,
                                regexp_replace(gruppe.lopenummer, '/', ' '),
                                coalesce(gruppe.arena_tiltaksnummer, ''),
                                gruppe.navn
                      )
          )
from gruppe
where gjennomforing.id = gruppe.id;

create index gjennomforing_fts_idx on gjennomforing using gin (fts);
