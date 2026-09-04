drop view if exists view_tilskudd_behandling;

alter table bruker_utbetaling
    alter column behandling_id type integer
        using behandling_id::integer;

create table tilskudd_vedtak
(
    id                              uuid     not null primary key,
    tilskudd_id                     uuid     not null references tilskudd (id),
    tilskudd_behandling_id          uuid     not null references tilskudd_behandling (id),
    lopenummer                      integer  not null,
    periode                         daterange not null,
    kostnadssted                    text      not null,
    soknad_dato                     date     not null,
    soknad_belop                    integer  not null,
    soknad_valuta                   currency not null,
    vedtak_resultat                 text     not null
       references vedtak_resultat (value),
    kommentar_vedtaksbrev           text,
    kommentar_intern                text,
    utbetaling_mottaker             text     not null,
    kid                             text,
    belop                           integer,
    valuta                          currency,
    utbetaling_id                   uuid
        references utbetaling (id),
    bruker_utbetaling_id            uuid
        references bruker_utbetaling (id),
    bruker_utbetaling_behandling_id int,
    vedtak_journalpost_id               text,
    vedtak_journalpost_distribuering_id text,
    vedtak_journalfort_tidspunkt        timestamp with time zone,
    vedtak_distribuert_tidspunkt        timestamp with time zone
);


insert into tilskudd_vedtak (id,
                             tilskudd_id,
                             tilskudd_behandling_id,
                             lopenummer,
                             periode,
                             kostnadssted,
                             soknad_dato,
                             soknad_belop,
                             soknad_valuta,
                             vedtak_resultat,
                             kommentar_vedtaksbrev,
                             kommentar_intern,
                             utbetaling_mottaker,
                             kid,
                             belop,
                             valuta,
                             utbetaling_id,
                             bruker_utbetaling_id,
                             bruker_utbetaling_behandling_id,
                             vedtak_journalpost_id,
                             vedtak_journalpost_distribuering_id,
                             vedtak_journalfort_tidspunkt,
                             vedtak_distribuert_tidspunkt)
select gen_random_uuid(),
       tilskudd.id,
       tilskudd.tilskudd_behandling_id,
       1::integer,
       tilskudd_behandling.periode,
       tilskudd_behandling.kostnadssted,
       tilskudd_behandling.soknad_dato,
       tilskudd.soknad_belop,
       tilskudd.soknad_valuta,
       tilskudd.vedtak_resultat,
       tilskudd.kommentar_vedtaksbrev,
       tilskudd_behandling.kommentar_intern,
       tilskudd.utbetaling_mottaker,
       tilskudd.kid,
       tilskudd.belop,
       tilskudd.valuta,
       tilskudd.utbetaling_id,
       bruker_utbetaling.id,
       bruker_utbetaling.behandling_id,
       tilskudd_behandling.vedtak_journalpost_id,
       tilskudd_behandling.vedtak_journalpost_distribuering_id,
       tilskudd_behandling.vedtak_journalfort_tidspunkt,
       tilskudd_behandling.vedtak_distribuert_tidspunkt

from tilskudd
    inner join tilskudd_behandling on tilskudd.tilskudd_behandling_id = tilskudd_behandling.id
    left outer join bruker_utbetaling on tilskudd.bruker_utbetaling_id = bruker_utbetaling.id;

alter table tilskudd
    drop column vedtak_resultat,
    drop column kommentar_vedtaksbrev,
    drop column utbetaling_mottaker,
    drop column soknad_belop,
    drop column soknad_valuta,
    drop column kid,
    drop column belop,
    drop column valuta,
    drop column utbetaling_id,
    drop column bruker_utbetaling_id;

alter table tilskudd_behandling
    drop column soknad_dato,
    drop column periode,
    drop column kostnadssted,
    drop column kommentar_intern,
    drop column vedtak_journalpost_id,
    drop column vedtak_journalpost_distribuering_id,
    drop column vedtak_journalfort_tidspunkt,
    drop column vedtak_distribuert_tidspunkt;
