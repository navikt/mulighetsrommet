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
import org.intellij.lang.annotations.Language
import java.util.*

class ArenaDeltakerQueries(private val session: Session) {

    fun upsertArenaDeltaker(deltaker: TiltakshistorikkArenaDeltaker) {
        @Language("PostgreSQL")
        val query = """
            insert into arena_deltaker (id,
                                        norsk_ident,
                                        arena_gjennomforing_id,
                                        arena_tiltakskode,
                                        status,
                                        start_dato,
                                        slutt_dato,
                                        beskrivelse,
                                        arrangor_organisasjonsnummer,
                                        arena_reg_dato,
                                        arena_mod_dato,
                                        dager_per_uke,
                                        deltidsprosent)
            values (:id::uuid,
                    :norsk_ident,
                    :arena_gjennomforing_id,
                    :arena_tiltakskode,
                    :status::arena_deltaker_status,
                    :start_dato,
                    :slutt_dato,
                    :beskrivelse,
                    :arrangor_organisasjonsnummer,
                    :arena_reg_dato,
                    :arena_mod_dato,
                    :dager_per_uke,
                    :deltidsprosent)
            on conflict (id) do update set norsk_ident                  = excluded.norsk_ident,
                                           arena_gjennomforing_id       = excluded.arena_gjennomforing_id,
                                           arena_tiltakskode            = excluded.arena_tiltakskode,
                                           status                       = excluded.status,
                                           start_dato                   = excluded.start_dato,
                                           slutt_dato                   = excluded.slutt_dato,
                                           beskrivelse                  = excluded.beskrivelse,
                                           arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                                           arena_reg_dato               = excluded.arena_reg_dato,
                                           arena_mod_dato               = excluded.arena_mod_dato,
                                           dager_per_uke                = excluded.dager_per_uke,
                                           deltidsprosent               = excluded.deltidsprosent

        """.trimIndent()

        val params = mapOf(
            "id" to deltaker.id,
            "arena_gjennomforing_id" to deltaker.arenaGjennomforingId,
            "norsk_ident" to deltaker.norskIdent.value,
            "arena_tiltakskode" to deltaker.arenaTiltakskode,
            "status" to deltaker.status.name,
            "start_dato" to deltaker.startDato,
            "slutt_dato" to deltaker.sluttDato,
            "arena_reg_dato" to deltaker.arenaRegDato,
            "arena_mod_dato" to deltaker.arenaModDato,
            "beskrivelse" to deltaker.beskrivelse,
            "arrangor_organisasjonsnummer" to deltaker.arrangorOrganisasjonsnummer.value,
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
                    deltaker.arena_tiltakskode,
                    deltaker.status,
                    deltaker.start_dato,
                    deltaker.slutt_dato,
                    deltaker.beskrivelse,
                    deltaker.arrangor_organisasjonsnummer,
                    deltaker.deltidsprosent,
                    deltaker.dager_per_uke,
                    virksomhet.navn as arrangor_navn
                from arena_deltaker deltaker
                    left join virksomhet on virksomhet.organisasjonsnummer = deltaker.arrangor_organisasjonsnummer
                where norsk_ident = any(:identer)
                and (:max_age_years::integer is null or age(coalesce(slutt_dato, arena_reg_dato)) < make_interval(years => :max_age_years::integer))
                order by start_dato desc nulls last;
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

private fun Row.toArenaDeltakelse() = TiltakshistorikkV1Dto.ArenaDeltakelse(
    norskIdent = NorskIdent(string("norsk_ident")),
    id = uuid("id"),
    status = ArenaDeltakerStatus.valueOf(string("status")),
    startDato = localDateOrNull("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
    beskrivelse = string("beskrivelse"),
    tiltakstype = TiltakshistorikkV1Dto.ArenaDeltakelse.Tiltakstype(
        tiltakskode = string("arena_tiltakskode"),
        navn = null,
    ),
    arrangor = TiltakshistorikkV1Dto.Arrangor(
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
        navn = stringOrNull("arrangor_navn"),
    ),
    deltidsprosent = floatOrNull("deltidsprosent"),
    dagerPerUke = floatOrNull("dager_per_uke"),
)
