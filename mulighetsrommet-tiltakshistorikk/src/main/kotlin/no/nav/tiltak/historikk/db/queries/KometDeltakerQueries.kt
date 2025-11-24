package no.nav.tiltak.historikk.db.queries

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.model.*
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.util.Tiltaksnavn
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
                    deltaker.prosent_stilling,
                    deltaker.dager_per_uke,
                    gjennomforing.id as gjennomforing_id,
                    gjennomforing.navn as gjennomforing_navn,
                    gjennomforing.deltidsprosent as gjennomforing_deltidsprosent,
                    tiltakstype.tiltakskode as tiltakstype_tiltakskode,
                    tiltakstype.navn as tiltakstype_navn,
                    arrangor.organisasjonsnummer as arrangor_organisasjonsnummer,
                    arrangor.navn as arrangor_navn,
                    arrangor_hovedenhet.organisasjonsnummer as arrangor_hovedenhet_organisasjonsnummer,
                    arrangor_hovedenhet.navn as arrangor_hovedenhet_navn
                from komet_deltaker deltaker
                    join gjennomforing on deltaker.gjennomforing_id = gjennomforing.id
                    join tiltakstype on tiltakstype.tiltakskode = gjennomforing.tiltakskode
                    join virksomhet arrangor on gjennomforing.arrangor_organisasjonsnummer = arrangor.organisasjonsnummer
                    left join virksomhet arrangor_hovedenhet on arrangor.overordnet_enhet_organisasjonsnummer = arrangor_hovedenhet.organisasjonsnummer
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

private fun Row.toGruppetiltakDeltakelse(): TiltakshistorikkV1Dto.GruppetiltakDeltakelse {
    val tiltakstype = TiltakshistorikkV1Dto.GruppetiltakDeltakelse.Tiltakstype(
        tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
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
    return TiltakshistorikkV1Dto.GruppetiltakDeltakelse(
        norskIdent = NorskIdent(string("norsk_ident")),
        id = uuid("id"),
        startDato = localDateOrNull("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        tittel = Tiltaksnavn.hosTitleCaseVirksomhet(
            tiltakstype.navn,
            arrangor.hovedenhet?.navn ?: arrangor.underenhet.navn,
        ),
        status = DeltakerStatus(
            type = DeltakerStatusType.valueOf(string("status_type")),
            aarsak = stringOrNull("status_aarsak")?.let { aarsak ->
                DeltakerStatusAarsak.valueOf(aarsak)
            },
            opprettetDato = localDateTime("status_opprettet_dato"),
        ),
        tiltakstype = tiltakstype,
        gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
            id = uuid("gjennomforing_id"),
            navn = stringOrNull("gjennomforing_navn"),
            deltidsprosent = floatOrNull("gjennomforing_deltidsprosent"),
        ),
        arrangor = arrangor,
        deltidsprosent = floatOrNull("prosent_stilling"),
        dagerPerUke = floatOrNull("dager_per_uke"),
    )
}
