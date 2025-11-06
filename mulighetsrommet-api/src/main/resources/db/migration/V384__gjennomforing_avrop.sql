create table gjennomforing_avrop
(
    gjennomforing_id               uuid                                   not null primary key references gjennomforing,
    created_at                     timestamp with time zone default now() not null,
    updated_at                     timestamp with time zone default now() not null,
    avtale_id                      uuid                                   not null references avtale on delete set null,
    navn                           text                                   not null,
    start_dato                     date                                   not null,
    slutt_dato                     date,
    antall_plasser                 integer                                not null,
    oppstart                       gjennomforing_oppstartstype            not null,
    faneinnhold                    jsonb,
    beskrivelse                    text,
    publisert                      boolean                  default false not null,
    apent_for_pamelding            boolean                  default true  not null,
    tilgjengelig_for_arrangor_dato date,
    oppmote_sted                   text,
    sted_for_gjennomforing         text,
    deltidsprosent                 numeric(5, 2)                          not null,
    estimert_ventetid_verdi        integer,
    estimert_ventetid_enhet        text,
    status                         gjennomforing_status                   not null,
    avsluttet_tidspunkt            timestamp,
    avbrutt_aarsaker               text[],
    avbrutt_forklaring             text
    -- følgende er tenkt hentet fra gjennomføring-tabell i stedet:
    -- opphav                         opphav                   default 'TILTAKSADMINISTRASJON'::opphav not null,
    -- arena_tiltaksnummer            text unique,
    -- arena_ansvarlig_enhet          text,
-- TODO: hvordan støtte fts?
--         fts                            tsvector generated always as (to_tsvector('norwegian'::regconfig,
--                                                                              ((((COALESCE(tiltaksnummer, ''::text) || ' '::text) ||
--                                                                                 COALESCE(navn, ''::text)) ||
--                                                                                ' '::text) ||
--                                                                               COALESCE(lopenummer, ''::text)))) stored,
);

create index gjennomforing_avrop_status_idx on gjennomforing_avrop (status);

create index gjennomforing_avrop_avtale_id_idx on gjennomforing_avrop (avtale_id);

create index gjennomforing_avrop_start_dato_idx on gjennomforing_avrop (start_dato);

create index gjennomforing_avrop_slutt_dato_idx on gjennomforing_avrop (slutt_dato);

create trigger set_timestamp
    before update
    on gjennomforing_avrop
    for each row
execute procedure trigger_set_timestamp();

insert into gjennomforing_avrop(gjennomforing_id,
                                avtale_id,
                                navn,
                                start_dato,
                                slutt_dato,
                                antall_plasser,
                                oppstart,
                                faneinnhold,
                                beskrivelse,
                                publisert,
                                apent_for_pamelding,
                                tilgjengelig_for_arrangor_dato,
                                oppmote_sted,
                                sted_for_gjennomforing,
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
       faneinnhold,
       beskrivelse,
       publisert,
       apent_for_pamelding,
       tilgjengelig_for_arrangor_dato,
       oppmote_sted,
       sted_for_gjennomforing,
       deltidsprosent,
       estimert_ventetid_verdi,
       estimert_ventetid_enhet,
       status,
       avsluttet_tidspunkt,
       avbrutt_aarsaker,
       avbrutt_forklaring
from gjennomforing
where avtale_id is not null;

alter table gjennomforing
    drop avtale_id,
    drop navn,
    drop start_dato,
    drop slutt_dato,
    drop antall_plasser,
    drop oppstart,
    drop faneinnhold,
    drop beskrivelse,
    drop publisert,
    drop apent_for_pamelding,
    drop tilgjengelig_for_arrangor_dato,
    drop oppmote_sted,
    drop sted_for_gjennomforing,
    drop deltidsprosent,
    drop estimert_ventetid_verdi,
    drop estimert_ventetid_enhet,
    drop status,
    drop avsluttet_tidspunkt,
    drop avbrutt_aarsaker,
    drop avbrutt_forklaring;
