create or replace view view_tilskudd_behandling_kompakt as
select tb.id,
       tb.gjennomforing_id,
       tb.status,
       tb.type,
       tb.behandlende_enhet,
       tilskudd_vedtak.id as tilskudd_vedtak_id,
       tilskudd_vedtak.soknad_dato as tilskudd_vedtak_soknad_dato,
       tilskudd_vedtak.soknad_journalpost_id as tilskudd_vedtak_soknad_journalpost_id,
       tilskudd_vedtak.periode as tilskudd_vedtak_periode,
       tilskudd_vedtak.vedtak_resultat as tilskudd_vedtak_resultat,
       tilskudd_vedtak.kostnadssted as tilskudd_vedtak_kostnadssted,
       nav_enhet.navn as tilskudd_vedtak_kostnadssted_navn,
       tilskudd_opplaering.kode as tilskudd_opplaering_kode
from tilskudd_behandling tb
    inner join tilskudd_vedtak on tilskudd_vedtak.tilskudd_behandling_id = tb.id
    inner join tilskudd on tilskudd.id = tilskudd_vedtak.tilskudd_id
    inner join tilskudd_opplaering on tilskudd_opplaering.id = tilskudd.tilskudd_opplaering_id
    inner join nav_enhet on nav_enhet.enhetsnummer = tilskudd_vedtak.kostnadssted
