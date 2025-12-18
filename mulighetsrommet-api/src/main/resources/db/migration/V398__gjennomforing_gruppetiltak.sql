drop view if exists view_avtale;
drop view if exists view_gjennomforing;
drop view if exists view_gjennomforing_enkeltplass;
drop view if exists view_tilsagn;
drop view if exists view_utbetaling;
drop view if exists view_veilederflate_tiltak;
drop view if exists view_datavarehus_gruppetiltak;
drop view if exists view_datavarehus_enkeltplass;

create type gjennomforing_type as enum ('GRUPPETILTAK', 'ENKELTPLASS');

alter table gjennomforing
    add gjennomforing_type gjennomforing_type;

update gjennomforing
set gjennomforing_type = 'GRUPPETILTAK';

alter table gjennomforing
    alter gjennomforing_type set not null;

create index idx_gjennomforing_type_status on gjennomforing (gjennomforing_type, status);

alter table gjennomforing
    drop fts;

alter table gjennomforing
    add fts tsvector;

update gjennomforing
set fts = to_tsvector('norwegian',
                      concat_ws(' ',
                                gjennomforing.lopenummer,
                                regexp_replace(gjennomforing.lopenummer, '/', ' '),
                                coalesce(gjennomforing.arena_tiltaksnummer, ''),
                                gjennomforing.navn,
                                arrangor.navn
                      )
          )
from arrangor
where arrangor.id = gjennomforing.arrangor_id;

create index gjennomforing_fts_idx on gjennomforing using gin (fts);

insert into gjennomforing (id,
                           created_at,
                           updated_at,
                           tiltakstype_id,
                           arrangor_id,
                           opphav,
                           gjennomforing_type,
                           oppstart,
                           pamelding_type,
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
       'ENKELTPLASS',
       'LOPENDE',
       'TRENGER_GODKJENNING',
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
