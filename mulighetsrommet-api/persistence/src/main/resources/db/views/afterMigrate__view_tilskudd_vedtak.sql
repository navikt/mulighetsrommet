create or replace view view_tilskudd_vedtak as
select v.id,
       v.tilskudd_id,
       v.lopenummer,
       v.tilskudd_behandling_id,
       v.soknad_journalpost_id,
       v.soknad_dato,
       v.periode,
       v.kostnadssted,
       v.soknad_belop,
       v.soknad_valuta,
       v.belop,
       v.valuta,
       v.vedtak_resultat,
       v.kommentar_vedtaksbrev,
       v.utbetaling_mottaker,
       v.kid,
       v.bruker_utbetaling_type,
       v.kommentar_intern,
       v.vedtak_journalpost_id,
       v.vedtak_journalpost_distribuering_id,
       v.vedtak_journalfort_tidspunkt,
       v.vedtak_distribuert_tidspunkt,
       utbetaling.id as utbetaling_id,
       bruker_utbetaling.id as bruker_utbetaling_id,
       tilskudd.tilskuddsnummer,
       tilskudd_opplaering.kode as tilskudd_opplaering_type,
       nav_enhet.navn as kostnadssted_navn
from tilskudd_vedtak v
    inner join nav_enhet on nav_enhet.enhetsnummer = v.kostnadssted
    inner join tilskudd on v.tilskudd_id = tilskudd.id
    inner join tilskudd_opplaering on tilskudd_opplaering.id = tilskudd.tilskudd_opplaering_id
    left outer join utbetaling on v.utbetaling_id = utbetaling.id
    left outer join bruker_utbetaling on v.id = bruker_utbetaling.tilskudd_vedtak_id
