package no.nav.tiltak.historikk.db.queries

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.model.ArenaDeltakerStatus
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.TiltakshistorikkArenaDeltaker
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.util.Tiltaksnavn
import org.intellij.lang.annotations.Language
import java.util.UUID

class ArenaDeltakerQueries(private val session: Session) {

    fun upsertArenaDeltaker(deltaker: TiltakshistorikkArenaDeltaker) {
        @Language("PostgreSQL")
        val query = """
            insert into arena_deltaker (id,
                                        norsk_ident,
                                        arena_gjennomforing_id,
                                        status,
                                        start_dato,
                                        slutt_dato,
                                        arena_reg_dato,
                                        arena_mod_dato,
                                        dager_per_uke,
                                        deltidsprosent)
            values (:id::uuid,
                    :norsk_ident,
                    :arena_gjennomforing_id,
                    :status::arena_deltaker_status,
                    :start_dato,
                    :slutt_dato,
                    :arena_reg_dato,
                    :arena_mod_dato,
                    :dager_per_uke,
                    :deltidsprosent)
            on conflict (id) do update set norsk_ident                  = excluded.norsk_ident,
                                           arena_gjennomforing_id       = excluded.arena_gjennomforing_id,
                                           status                       = excluded.status,
                                           start_dato                   = excluded.start_dato,
                                           slutt_dato                   = excluded.slutt_dato,
                                           arena_reg_dato               = excluded.arena_reg_dato,
                                           arena_mod_dato               = excluded.arena_mod_dato,
                                           dager_per_uke                = excluded.dager_per_uke,
                                           deltidsprosent               = excluded.deltidsprosent

        """.trimIndent()

        val params = mapOf(
            "id" to deltaker.id,
            "arena_gjennomforing_id" to deltaker.arenaGjennomforingId,
            "norsk_ident" to deltaker.norskIdent.value,
            "status" to deltaker.status.name,
            "start_dato" to deltaker.startDato,
            "slutt_dato" to deltaker.sluttDato,
            "arena_reg_dato" to deltaker.arenaRegDato,
            "arena_mod_dato" to deltaker.arenaModDato,
            "dager_per_uke" to deltaker.dagerPerUke,
            "deltidsprosent" to deltaker.deltidsprosent,
        )

        session.execute(queryOf(query, params))
    }

    fun getArenaHistorikk(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
    ): List<TiltakshistorikkV1Dto.ArenaDeltakelse> {
        @Language("PostgreSQL")
        val query = """
                select
                    deltaker.id,
                    deltaker.norsk_ident,
                    deltaker.status,
                    deltaker.start_dato,
                    deltaker.slutt_dato,
                    deltaker.deltidsprosent,
                    deltaker.dager_per_uke,
                    tiltakstype.arena_tiltakskode as tiltakstype_tiltakskode,
                    tiltakstype.navn as tiltakstype_navn,
                    gjennomforing.id as gjennomforing_id,
                    gjennomforing.navn as gjennomforing_navn,
                    gjennomforing.deltidsprosent as gjennomforing_deltidsprosent,
                    arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
                    arrangor.navn as arrangor_navn,
                    arrangor_hovedenhet.organisasjonsnummer as arrangor_hovedenhet_organisasjonsnummer,
                    arrangor_hovedenhet.navn as arrangor_hovedenhet_navn
                from arena_deltaker deltaker
                    join arena_gjennomforing gjennomforing on gjennomforing.id = arena_gjennomforing_id
                    join tiltakstype on tiltakstype.tiltakstype_id = gjennomforing.tiltakstype_id
                    join virksomhet arrangor on gjennomforing.arrangor_organisasjonsnummer = arrangor.organisasjonsnummer
                    left join virksomhet arrangor_hovedenhet on arrangor.overordnet_enhet_organisasjonsnummer = arrangor_hovedenhet.organisasjonsnummer
                where deltaker.norsk_ident = any(:identer)
                and (:max_age_years::integer is null or age(coalesce(deltaker.slutt_dato, deltaker.arena_reg_dato)) < make_interval(years => :max_age_years::integer))
                order by deltaker.start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOfValue(identer) { it.value },
            "max_age_years" to maxAgeYears,
        )

        return session.list(queryOf(query, params)) { it.toArenaDeltakelse() }
    }

    fun deleteArenaDeltaker(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from arena_deltaker
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }
}

private fun Row.toArenaDeltakelse(): TiltakshistorikkV1Dto.ArenaDeltakelse {
    val tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
        tiltakskode = string("tiltakstype_tiltakskode"),
        navn = string("tiltakstype_navn"),
    )
    val arrangor = TiltakshistorikkV1Dto.Arrangor(
        hovedenhet = stringOrNull("arrangor_hovedenhet_organisasjonsnummer")?.let {
            TiltakshistorikkV1Dto.Virksomhet(
                organisasjonsnummer = Organisasjonsnummer(it),
                navn = stringOrNull("arrangor_hovedenhet_navn"),
            )
        },
        underenhet = TiltakshistorikkV1Dto.Virksomhet(
            organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
            navn = stringOrNull("arrangor_navn"),
        ),
    )
    return TiltakshistorikkV1Dto.ArenaDeltakelse(
        norskIdent = NorskIdent(string("norsk_ident")),
        id = uuid("id"),
        status = ArenaDeltakerStatus.valueOf(string("status")),
        startDato = localDateOrNull("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        tittel = Tiltaksnavn.hosTitleCaseVirksomhet(tiltakstype.navn, arrangor.underenhet.navn),
        tiltakstype = tiltakstype,
        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
            id = uuid("gjennomforing_id"),
            navn = stringOrNull("gjennomforing_navn"),
            deltidsprosent = floatOrNull("gjennomforing_deltidsprosent"),
        ),
        arrangor = arrangor,
        deltidsprosent = floatOrNull("deltidsprosent"),
        dagerPerUke = floatOrNull("dager_per_uke"),
    )
}
