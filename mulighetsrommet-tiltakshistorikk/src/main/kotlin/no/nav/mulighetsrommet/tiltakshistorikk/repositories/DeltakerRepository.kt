package no.nav.mulighetsrommet.tiltakshistorikk.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskode.Companion.toArenaKode
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.TiltakshistorikkDto
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class DeltakerRepository(private val db: Database) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsertArenaDeltaker(deltaker: ArenaDeltakerDbo) = db.useSession { session ->
        logger.info("Lagrer arena_deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into arena_deltaker (id,
                                        norsk_ident,
                                        arena_tiltakskode,
                                        status,
                                        start_dato,
                                        slutt_dato,
                                        beskrivelse,
                                        arrangor_organisasjonsnummer,
                                        registrert_i_arena_dato)
            values (:id::uuid,
                    :norsk_ident,
                    :arena_tiltakskode,
                    :status::arena_deltaker_status,
                    :start_dato,
                    :slutt_dato,
                    :beskrivelse,
                    :arrangor_organisasjonsnummer,
                    :registrert_i_arena_dato)
            on conflict (id) do update set norsk_ident                  = excluded.norsk_ident,
                                           arena_tiltakskode            = excluded.arena_tiltakskode,
                                           status                       = excluded.status,
                                           start_dato                   = excluded.start_dato,
                                           slutt_dato                   = excluded.slutt_dato,
                                           beskrivelse                  = excluded.beskrivelse,
                                           arrangor_organisasjonsnummer = excluded.arrangor_organisasjonsnummer,
                                           registrert_i_arena_dato      = excluded.registrert_i_arena_dato

        """.trimIndent()

        queryOf(query, deltaker.toSqlParameters()).asExecute.runWithSession(session)
    }

    fun getArenaDeltakelser(identer: List<NorskIdent>): List<ArenaDeltakerDbo> = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select *
            from arena_deltaker
            where norsk_ident = any(:identer)
            order by start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOf("text", identer.map { it.value }),
        )

        queryOf(query, params).map { it.toArenaDeltaker() }.asList.runWithSession(session)
    }

    fun deleteArenaDeltaker(id: UUID) = db.useSession { session ->
        logger.info("Sletter arena_deltaker id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from arena_deltaker
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id).asExecute.runWithSession(session)
    }

    fun upsertKometDeltaker(deltaker: AmtDeltakerV1Dto) = db.useSession { session ->
        logger.info("Lagrer komet_deltaker id=${deltaker.id}")

        @Language("PostgreSQL")
        val query = """
            insert into komet_deltaker (
                id,
                gjennomforing_id,
                person_ident,
                start_dato,
                slutt_dato,
                status_type,
                status_opprettet_dato,
                status_aarsak,
                registrert_dato,
                endret_dato,
                dager_per_uke,
                prosent_stilling
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :person_ident,
                :start_dato,
                :slutt_dato,
                :status_type,
                :status_opprettet_dato,
                :status_aarsak,
                :registrert_dato,
                :endret_dato,
                :dager_per_uke,
                :prosent_stilling
            )
            on conflict (id) do update set
                gjennomforing_id            = excluded.gjennomforing_id,
                person_ident                = excluded.person_ident,
                start_dato                  = excluded.start_dato,
                slutt_dato                  = excluded.slutt_dato,
                status_type                 = excluded.status_type,
                status_opprettet_dato       = excluded.status_opprettet_dato,
                status_aarsak               = excluded.status_aarsak,
                registrert_dato             = excluded.registrert_dato,
                endret_dato                 = excluded.endret_dato,
                dager_per_uke               = excluded.dager_per_uke,
                prosent_stilling            = excluded.prosent_stilling
        """.trimIndent()

        queryOf(query, deltaker.toSqlParameters()).asExecute.runWithSession(session)
    }

    fun getKometHistorikk(identer: List<NorskIdent>): List<TiltakshistorikkDto> = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
            select
                komet_deltaker.id,
                komet_deltaker.start_dato,
                komet_deltaker.slutt_dato,
                komet_deltaker.status_type,
                komet_deltaker.status_opprettet_dato,
                komet_deltaker.status_aarsak,
                gruppetiltak.navn,
                gruppetiltak.tiltakskode,
                gruppetiltak.arrangor_organisasjonsnummer
            from komet_deltaker
                inner join gruppetiltak on gruppetiltak.id = komet_deltaker.gjennomforing_id
            where person_ident = any(:identer)
            order by start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOf("text", identer.map { it.value }),
        )

        queryOf(query, params).map { it.toKometTiltakshistorikkDto() }.asList.runWithSession(session)
    }

    fun deleteKometDeltaker(id: UUID) = db.useSession { session ->
        logger.info("Sletter komet_deltaker id=$id")

        @Language("PostgreSQL")
        val query = """
            delete from komet_deltaker
            where id = ?::uuid
        """.trimIndent()

        queryOf(query, id).asExecute.runWithSession(session)
    }
}

private fun ArenaDeltakerDbo.toSqlParameters() = mapOf(
    "id" to id,
    "norsk_ident" to norskIdent.value,
    "arena_tiltakskode" to arenaTiltakskode,
    "status" to status.name,
    "start_dato" to startDato,
    "slutt_dato" to sluttDato,
    "registrert_i_arena_dato" to registrertIArenaDato,
    "beskrivelse" to beskrivelse,
    "arrangor_organisasjonsnummer" to arrangorOrganisasjonsnummer.value,
)

private fun Row.toArenaDeltaker(): ArenaDeltakerDbo = ArenaDeltakerDbo(
    id = uuid("id"),
    norskIdent = NorskIdent(string("norsk_ident")),
    arenaTiltakskode = string("arena_tiltakskode"),
    status = ArenaDeltakerStatus.valueOf(string("status")),
    startDato = localDateTimeOrNull("start_dato"),
    sluttDato = localDateTimeOrNull("slutt_dato"),
    registrertIArenaDato = localDateTime("registrert_i_arena_dato"),
    beskrivelse = string("beskrivelse"),
    arrangorOrganisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
)

private fun AmtDeltakerV1Dto.toSqlParameters() = mapOf(
    "id" to id,
    "gjennomforing_id" to gjennomforingId,
    "person_ident" to personIdent,
    "start_dato" to startDato,
    "slutt_dato" to sluttDato,
    "status_type" to status.type.name,
    "status_opprettet_dato" to status.opprettetDato,
    "status_aarsak" to status.aarsak?.name,
    "registrert_dato" to registrertDato,
    "endret_dato" to endretDato,
    "dager_per_uke" to dagerPerUke,
    "prosent_stilling" to prosentStilling,
)

private fun Row.toKometTiltakshistorikkDto(): TiltakshistorikkDto = TiltakshistorikkDto(
    id = uuid("id"),
    status = AmtDeltakerStatus(
        type = AmtDeltakerStatus.Type.valueOf(string("status_type")),
        aarsak = stringOrNull("status_aarsak")?.let {
            AmtDeltakerStatus.Aarsak.valueOf(it)
        },
        opprettetDato = localDateTime("status_opprettet_dato"),
    ).toDeltakerstatus(),
    startDato = localDateOrNull("start_dato")?.atStartOfDay(),
    sluttDato = localDateOrNull("slutt_dato")?.atStartOfDay(),
    tiltaksnavn = string("navn"),
    arenaTiltakskode = toArenaKode(Tiltakskode.valueOf(string("tiltakskode"))),
    arrangorOrganisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
)

fun AmtDeltakerStatus.toDeltakerstatus() =
    when (this.type) {
        AmtDeltakerStatus.Type.PABEGYNT_REGISTRERING -> Deltakerstatus.PABEGYNT_REGISTRERING

        AmtDeltakerStatus.Type.UTKAST_TIL_PAMELDING, // TODO: Skal denne her? Dette er vel før påbegynt registrering egentlig?
        AmtDeltakerStatus.Type.VURDERES, // TODO: Skal denne her? Dette er vel før påbegynt registrering egentlig?
        AmtDeltakerStatus.Type.SOKT_INN,
        AmtDeltakerStatus.Type.VENTELISTE,
        AmtDeltakerStatus.Type.VENTER_PA_OPPSTART,
        -> Deltakerstatus.VENTER

        AmtDeltakerStatus.Type.AVBRUTT_UTKAST,
        AmtDeltakerStatus.Type.IKKE_AKTUELL,
        AmtDeltakerStatus.Type.FEILREGISTRERT,
        -> Deltakerstatus.IKKE_AKTUELL

        AmtDeltakerStatus.Type.DELTAR -> Deltakerstatus.DELTAR

        AmtDeltakerStatus.Type.HAR_SLUTTET,
        AmtDeltakerStatus.Type.AVBRUTT,
        AmtDeltakerStatus.Type.FULLFORT,
        -> Deltakerstatus.AVSLUTTET
    }
