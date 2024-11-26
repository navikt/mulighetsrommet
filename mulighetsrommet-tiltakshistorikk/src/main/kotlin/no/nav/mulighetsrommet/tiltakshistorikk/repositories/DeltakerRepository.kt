package no.nav.mulighetsrommet.tiltakshistorikk.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.dbo.ArenaDeltakerDbo
import no.nav.mulighetsrommet.domain.dto.ArenaDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.DeltakerStatus
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.domain.dto.Tiltakshistorikk
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

    fun getArenaHistorikk(identer: List<NorskIdent>, maxAgeYears: Int?) = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
                select
                    id,
                    norsk_ident,
                    arena_tiltakskode,
                    status,
                    start_dato,
                    slutt_dato,
                    beskrivelse,
                    arrangor_organisasjonsnummer
                from arena_deltaker
                where norsk_ident = any(:identer)
                and (:max_age_years::integer is null or age(coalesce(slutt_dato, registrert_i_arena_dato)) < make_interval(years => :max_age_years::integer))
                order by start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOf("text", identer.map { it.value }),
            "max_age_years" to maxAgeYears,
        )

        queryOf(query, params).map { it.toArenaDeltakelse() }.asList.runWithSession(session)
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

    fun getKometHistorikk(identer: List<NorskIdent>, maxAgeYears: Int?) = db.useSession { session ->
        @Language("PostgreSQL")
        val query = """
                select
                    deltaker.person_ident as norsk_ident,
                    deltaker.id,
                    deltaker.start_dato,
                    deltaker.slutt_dato,
                    deltaker.status_type,
                    deltaker.status_aarsak,
                    deltaker.status_opprettet_dato,
                    gruppetiltak.id as gruppetiltak_id,
                    gruppetiltak.navn as gruppetiltak_navn,
                    gruppetiltak.tiltakskode as gruppetiltak_tiltakskode,
                    gruppetiltak.arrangor_organisasjonsnummer
                from komet_deltaker deltaker join gruppetiltak on deltaker.gjennomforing_id = gruppetiltak.id
                where deltaker.person_ident = any(:identer)
                and (:max_age_years::integer is null or age(coalesce(deltaker.slutt_dato::timestamp, deltaker.registrert_dato)) < make_interval(years => :max_age_years::integer))
                order by deltaker.start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOf("text", identer.map { it.value }),
            "max_age_years" to maxAgeYears,
        )

        queryOf(query, params)
            .map { it.toGruppetiltakDeltakelse() }
            .asList
            .runWithSession(session)
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

private fun Row.toArenaDeltakelse() = Tiltakshistorikk.ArenaDeltakelse(
    norskIdent = NorskIdent(string("norsk_ident")),
    id = uuid("id"),
    arenaTiltakskode = string("arena_tiltakskode"),
    status = ArenaDeltakerStatus.valueOf(string("status")),
    startDato = localDateOrNull("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
    beskrivelse = string("beskrivelse"),
    arrangor = Tiltakshistorikk.Arrangor(
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
    ),
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

private fun Row.toGruppetiltakDeltakelse() = Tiltakshistorikk.GruppetiltakDeltakelse(
    norskIdent = NorskIdent(string("norsk_ident")),
    id = uuid("id"),
    startDato = localDateOrNull("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
    status = DeltakerStatus(
        type = DeltakerStatus.Type.valueOf(string("status_type")),
        aarsak = stringOrNull("status_aarsak")?.let { aarsak ->
            DeltakerStatus.Aarsak.valueOf(aarsak)
        },
        opprettetDato = localDateTime("status_opprettet_dato"),
    ),
    gjennomforing = Tiltakshistorikk.Gjennomforing(
        id = uuid("gruppetiltak_id"),
        navn = string("gruppetiltak_navn"),
        tiltakskode = Tiltakskode.valueOf(string("gruppetiltak_tiltakskode")),
    ),
    arrangor = Tiltakshistorikk.Arrangor(
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
    ),
)
