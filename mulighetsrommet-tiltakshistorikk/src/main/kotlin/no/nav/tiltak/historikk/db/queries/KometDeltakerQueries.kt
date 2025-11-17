package no.nav.tiltak.historikk.db.queries

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import org.intellij.lang.annotations.Language
import java.util.*

class KometDeltakerQueries(private val session: Session) {

    fun upsertKometDeltaker(deltaker: AmtDeltakerV1Dto) {
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

        val params = mapOf(
            "id" to deltaker.id,
            "gjennomforing_id" to deltaker.gjennomforingId,
            "person_ident" to deltaker.personIdent,
            "start_dato" to deltaker.startDato,
            "slutt_dato" to deltaker.sluttDato,
            "status_type" to deltaker.status.type.name,
            "status_opprettet_dato" to deltaker.status.opprettetDato,
            "status_aarsak" to deltaker.status.aarsak?.name,
            "registrert_dato" to deltaker.registrertDato,
            "endret_dato" to deltaker.endretDato,
            "dager_per_uke" to deltaker.dagerPerUke,
            "prosent_stilling" to deltaker.prosentStilling,
        )

        session.execute(queryOf(query, params))
    }

    fun getKometHistorikk(
        identer: List<NorskIdent>,
        maxAgeYears: Int?,
    ): List<TiltakshistorikkV1Dto.GruppetiltakDeltakelse> {
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
                    gjennomforing.id as gjennomforing_id,
                    gjennomforing.navn as gjennomforing_navn,
                    gjennomforing.tiltakskode as gjennomforing_tiltakskode,
                    gjennomforing.arrangor_organisasjonsnummer
                from komet_deltaker deltaker join gjennomforing on deltaker.gjennomforing_id = gjennomforing.id
                where deltaker.person_ident = any(:identer)
                and (:max_age_years::integer is null or age(coalesce(deltaker.slutt_dato, deltaker.registrert_dato)) < make_interval(years => :max_age_years::integer))
                order by deltaker.start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOfValue(identer) { it.value },
            "max_age_years" to maxAgeYears,
        )

        return session.list(queryOf(query, params)) { it.toGruppetiltakDeltakelse() }
    }

    fun deleteKometDeltaker(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from komet_deltaker
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }
}

private fun Row.toGruppetiltakDeltakelse() = TiltakshistorikkV1Dto.GruppetiltakDeltakelse(
    norskIdent = NorskIdent(string("norsk_ident")),
    id = uuid("id"),
    startDato = localDateOrNull("start_dato"),
    sluttDato = localDateOrNull("slutt_dato"),
    status = DeltakerStatus(
        type = DeltakerStatusType.valueOf(string("status_type")),
        aarsak = stringOrNull("status_aarsak")?.let { aarsak ->
            DeltakerStatusAarsak.valueOf(aarsak)
        },
        opprettetDato = localDateTime("status_opprettet_dato"),
    ),
    tiltakstype = TiltakshistorikkV1Dto.GruppetiltakDeltakelse.Tiltakstype(
        tiltakskode = Tiltakskode.valueOf(string("gjennomforing_tiltakskode")),
        navn = null,
    ),
    gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
        id = uuid("gjennomforing_id"),
        navn = stringOrNull("gjennomforing_navn"),
        tiltakskode = Tiltakskode.valueOf(string("gjennomforing_tiltakskode")),
    ),
    arrangor = TiltakshistorikkV1Dto.Arrangor(
        organisasjonsnummer = Organisasjonsnummer(string("arrangor_organisasjonsnummer")),
    ),
)
