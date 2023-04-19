package no.nav.mulighetsrommet.arena.adapter.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.arena.adapter.models.arena.Administrasjonskode
import no.nav.mulighetsrommet.arena.adapter.models.arena.Handlingsplan
import no.nav.mulighetsrommet.arena.adapter.models.arena.Rammeavtale
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TiltakstypeRepository(private val db: Database) {
    private val logger: Logger = LoggerFactory.getLogger(javaClass)

    fun upsert(tiltak: Tiltakstype) = query {
        logger.info("Lagrer tiltakstype id=${tiltak.id}")

        @Language("PostgreSQL")
        val query = """
           insert into tiltakstype (id,
                         navn,
                         tiltakskode,
                         registrert_dato_i_arena,                          
                         sist_endret_dato_i_arena,
                         fra_dato,
                         til_dato,
                         rett_paa_tiltakspenger,
                         tiltaksgruppekode,
                         administrasjonskode,
                         send_tilsagnsbrev_til_deltaker,
                         skal_ha_anskaffelsesprosess,
                         maks_antall_plasser,
                         maks_antall_sokere,
                         har_fast_antall_plasser,
                         skal_sjekke_antall_deltakere,
                         vis_lonnstilskuddskalkulator,
                         rammeavtale,
                         opplaeringsgruppe,
                         handlingsplan,
                         tiltaksgjennomforing_krever_sluttdato,
                         maks_periode_i_mnd,
                         tiltaksgjennomforing_krever_meldeplikt,
                         tiltaksgjennomforing_krever_vedtak,
                         tiltaksgjennomforing_reservert_for_ia_bedrift,
                         har_rett_paa_tilleggsstonader,
                         har_rett_paa_utdanning,
                         tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk,
                         vis_begrunnelse_for_innsoking,
                         henvisningsbrev_og_hovedbrev_til_arbeidsgiver,
                         kopibrev_og_hovedbrev_til_arbeidsgiver)
values (:id::uuid,
        :navn,
        :tiltakskode,
        :registrert_dato_i_arena,                          
        :sist_endret_dato_i_arena,
        :fra_dato,
        :til_dato,
        :rett_paa_tiltakspenger,
        :tiltaksgruppekode,
        :administrasjonskode,
        :send_tilsagnsbrev_til_deltaker,
        :skal_ha_anskaffelsesprosess,
        :maks_antall_plasser,
        :maks_antall_sokere,
        :har_fast_antall_plasser,
        :skal_sjekke_antall_deltakere,
        :vis_lonnstilskuddskalkulator,
        :rammeavtale,
        :opplaeringsgruppe,
        :handlingsplan,
        :tiltaksgjennomforing_krever_sluttdato,
        :maks_periode_i_mnd,
        :tiltaksgjennomforing_krever_meldeplikt,
        :tiltaksgjennomforing_krever_vedtak,
        :tiltaksgjennomforing_reservert_for_ia_bedrift,
        :har_rett_paa_tilleggsstonader,
        :har_rett_paa_utdanning,
        :tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk,
        :vis_begrunnelse_for_innsoking,
        :henvisningsbrev_og_hovedbrev_til_arbeidsgiver,
        :kopibrev_og_hovedbrev_til_arbeidsgiver)
on conflict (id)
    do update set navn                                                   = excluded.navn,
                  tiltakskode                                            = excluded.tiltakskode,
                  registrert_dato_i_arena                                = excluded.registrert_dato_i_arena,                          
                  sist_endret_dato_i_arena                               = excluded.sist_endret_dato_i_arena,
                  fra_dato                                               = excluded.fra_dato,
                  til_dato                                               = excluded.til_dato,
                  rett_paa_tiltakspenger                                 = excluded.rett_paa_tiltakspenger,
                  tiltaksgruppekode                                      = excluded.tiltaksgruppekode,
                  administrasjonskode                                    = excluded.administrasjonskode,
                  send_tilsagnsbrev_til_deltaker                         = excluded.send_tilsagnsbrev_til_deltaker,
                  skal_ha_anskaffelsesprosess                            = excluded.skal_ha_anskaffelsesprosess,
                  maks_antall_plasser                                    = excluded.maks_antall_plasser,
                  maks_antall_sokere                                     = excluded.maks_antall_sokere,
                  har_fast_antall_plasser                                = excluded.har_fast_antall_plasser,
                  skal_sjekke_antall_deltakere                           = excluded.skal_sjekke_antall_deltakere,
                  vis_lonnstilskuddskalkulator                           = excluded.vis_lonnstilskuddskalkulator,
                  rammeavtale                                            = excluded.rammeavtale,
                  opplaeringsgruppe                                      = excluded.opplaeringsgruppe,
                  handlingsplan                                          = excluded.handlingsplan,
                  tiltaksgjennomforing_krever_sluttdato                  = excluded.tiltaksgjennomforing_krever_sluttdato,
                  maks_periode_i_mnd                                     = excluded.maks_periode_i_mnd,
                  tiltaksgjennomforing_krever_meldeplikt                 = excluded.tiltaksgjennomforing_krever_meldeplikt,
                  tiltaksgjennomforing_krever_vedtak                     = excluded.tiltaksgjennomforing_krever_vedtak,
                  tiltaksgjennomforing_reservert_for_ia_bedrift          = excluded.tiltaksgjennomforing_reservert_for_ia_bedrift,
                  har_rett_paa_tilleggsstonader                          = excluded.har_rett_paa_tilleggsstonader,
                  har_rett_paa_utdanning                                 = excluded.har_rett_paa_utdanning,
                  tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk = excluded.tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk,
                  vis_begrunnelse_for_innsoking                          = excluded.vis_begrunnelse_for_innsoking,
                  henvisningsbrev_og_hovedbrev_til_arbeidsgiver          = excluded.henvisningsbrev_og_hovedbrev_til_arbeidsgiver,
                  kopibrev_og_hovedbrev_til_arbeidsgiver                 = excluded.kopibrev_og_hovedbrev_til_arbeidsgiver
returning *
        """.trimIndent()

        queryOf(query, tiltak.toSqlParameters())
            .map { it.toTiltakstype() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun delete(id: UUID): QueryResult<Unit> = query {
        @Language("PostgreSQL")
        val query = """
            delete from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id)
            .asExecute
            .let { db.run(it) }
    }

    fun get(id: UUID): Tiltakstype? {
        logger.info("Henter tiltakstype id=$id")

        @Language("PostgreSQL")
        val query = """
            select *
            from tiltakstype
            where id = ?::uuid
        """.trimIndent()

        return queryOf(query, id)
            .map { it.toTiltakstype() }
            .asSingle
            .let { db.run(it) }
    }

    private fun Tiltakstype.toSqlParameters() = mapOf(
        "id" to id,
        "navn" to navn,
        "tiltakskode" to tiltakskode,
        "registrert_dato_i_arena" to registrertIArenaDato,
        "sist_endret_dato_i_arena" to sistEndretIArenaDato,
        "fra_dato" to fraDato,
        "til_dato" to tilDato,
        "rett_paa_tiltakspenger" to rettPaaTiltakspenger,
        "tiltaksgruppekode" to tiltaksgruppekode,
        "administrasjonskode" to administrasjonskode.name,
        "send_tilsagnsbrev_til_deltaker" to sendTilsagnsbrevTilDeltaker,
        "skal_ha_anskaffelsesprosess" to tiltakstypeSkalHaAnskaffelsesprosess,
        "maks_antall_plasser" to maksAntallPlasser,
        "maks_antall_sokere" to maksAntallSokere,
        "har_fast_antall_plasser" to harFastAntallPlasser,
        "skal_sjekke_antall_deltakere" to skalSjekkeAntallDeltakere,
        "vis_lonnstilskuddskalkulator" to visLonnstilskuddskalkulator,
        "rammeavtale" to rammeavtale?.name,
        "opplaeringsgruppe" to opplaeringsgruppe,
        "handlingsplan" to handlingsplan?.name,
        "tiltaksgjennomforing_krever_sluttdato" to tiltaksgjennomforingKreverSluttdato,
        "maks_periode_i_mnd" to maksPeriodeIMnd,
        "tiltaksgjennomforing_krever_meldeplikt" to tiltaksgjennomforingKreverMeldeplikt,
        "tiltaksgjennomforing_krever_vedtak" to tiltaksgjennomforingKreverVedtak,
        "tiltaksgjennomforing_reservert_for_ia_bedrift" to tiltaksgjennomforingReservertForIABedrift,
        "har_rett_paa_tilleggsstonader" to harRettPaaTilleggsstonader,
        "har_rett_paa_utdanning" to harRettPaaUtdanning,
        "tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk" to tiltaksgjennomforingGenererTilsagnsbrevAutomatisk,
        "vis_begrunnelse_for_innsoking" to visBegrunnelseForInnsoking,
        "henvisningsbrev_og_hovedbrev_til_arbeidsgiver" to sendHenvisningsbrevOgHovedbrevTilArbeidsgiver,
        "kopibrev_og_hovedbrev_til_arbeidsgiver" to sendKopibrevOgHovedbrevTilArbeidsgiver,
    )

    private fun Row.toTiltakstype() = Tiltakstype(
        id = uuid("id"),
        navn = string("navn"),
        tiltaksgruppekode = string("tiltaksgruppekode"),
        tiltakskode = string("tiltakskode"),
        registrertIArenaDato = localDateTime("registrert_dato_i_arena"),
        sistEndretIArenaDato = localDateTime("sist_endret_dato_i_arena"),
        fraDato = localDateTime("fra_dato"),
        tilDato = localDateTime("til_dato"),
        rettPaaTiltakspenger = boolean("rett_paa_tiltakspenger"),
        administrasjonskode = Administrasjonskode.valueOf(string("administrasjonskode")),
        sendTilsagnsbrevTilDeltaker = boolean("send_tilsagnsbrev_til_deltaker"),
        tiltakstypeSkalHaAnskaffelsesprosess = boolean("skal_ha_anskaffelsesprosess"),
        maksAntallPlasser = intOrNull("maks_antall_plasser"),
        maksAntallSokere = intOrNull("maks_antall_sokere"),
        harFastAntallPlasser = boolean("har_fast_antall_plasser"),
        skalSjekkeAntallDeltakere = boolean("skal_sjekke_antall_deltakere"),
        visLonnstilskuddskalkulator = boolean("vis_lonnstilskuddskalkulator"),
        rammeavtale = Rammeavtale.valueOf(string("rammeavtale")),
        opplaeringsgruppe = stringOrNull("opplaeringsgruppe"),
        handlingsplan = Handlingsplan.valueOf(string("handlingsplan")),
        tiltaksgjennomforingKreverSluttdato = boolean("tiltaksgjennomforing_krever_sluttdato"),
        maksPeriodeIMnd = intOrNull("maks_periode_i_mnd"),
        tiltaksgjennomforingKreverMeldeplikt = boolean("tiltaksgjennomforing_krever_meldeplikt"),
        tiltaksgjennomforingKreverVedtak = boolean("tiltaksgjennomforing_krever_vedtak"),
        tiltaksgjennomforingReservertForIABedrift = boolean("tiltaksgjennomforing_reservert_for_ia_bedrift"),
        harRettPaaTilleggsstonader = boolean("har_rett_paa_tilleggsstonader"),
        harRettPaaUtdanning = boolean("har_rett_paa_utdanning"),
        tiltaksgjennomforingGenererTilsagnsbrevAutomatisk = boolean("tiltaksgjennomforing_genererer_tilsagnsbrev_automatisk"),
        visBegrunnelseForInnsoking = boolean("vis_begrunnelse_for_innsoking"),
        sendHenvisningsbrevOgHovedbrevTilArbeidsgiver = boolean("henvisningsbrev_og_hovedbrev_til_arbeidsgiver"),
        sendKopibrevOgHovedbrevTilArbeidsgiver = boolean("kopibrev_og_hovedbrev_til_arbeidsgiver"),
    )
}
