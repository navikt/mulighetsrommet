package no.nav.tiltak.historikk.db.queries

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.model.DeltakerStatusAarsakType
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.model.KometDeltaker
import no.nav.tiltak.historikk.util.Tiltaksnavn
import org.intellij.lang.annotations.Language
import java.util.UUID

class KometDeltakerQueries(private val session: Session) {

    fun upsertKometDeltaker(deltaker: KometDeltaker) {
        @Language("PostgreSQL")
        val query = """
            insert into komet_deltaker (
                id,
                gjennomforing_id,
                person_ident,
                start_dato,
                slutt_dato,
                status_type,
                status_opprettet_tidspunkt,
                status_aarsak,
                registrert_tidspunkt,
                endret_tidspunkt,
                dager_per_uke,
                prosent_stilling
            ) values (
                :id::uuid,
                :gjennomforing_id::uuid,
                :person_ident,
                :start_dato,
                :slutt_dato,
                :status_type,
                :status_opprettet_tidspunkt,
                :status_aarsak,
                :registrert_tidspunkt,
                :endret_tidspunkt,
                :dager_per_uke,
                :prosent_stilling
            )
            on conflict (id) do update set
                gjennomforing_id            = excluded.gjennomforing_id,
                person_ident                = excluded.person_ident,
                start_dato                  = excluded.start_dato,
                slutt_dato                  = excluded.slutt_dato,
                status_type                 = excluded.status_type,
                status_opprettet_tidspunkt  = excluded.status_opprettet_tidspunkt,
                status_aarsak               = excluded.status_aarsak,
                registrert_tidspunkt        = excluded.registrert_tidspunkt,
                endret_tidspunkt            = excluded.endret_tidspunkt,
                dager_per_uke               = excluded.dager_per_uke,
                prosent_stilling            = excluded.prosent_stilling
        """.trimIndent()

        val params = mapOf(
            "id" to deltaker.id,
            "gjennomforing_id" to deltaker.gjennomforingId,
            "person_ident" to deltaker.personIdent,
            "start_dato" to deltaker.startDato,
            "slutt_dato" to deltaker.sluttDato,
            "status_type" to deltaker.statusType.name,
            "status_opprettet_tidspunkt" to deltaker.statusOpprettetTidspunkt,
            "status_aarsak" to deltaker.statusAarsak?.name,
            "registrert_tidspunkt" to deltaker.registrertTidspunkt,
            "endret_tidspunkt" to deltaker.endretTidspunkt,
            "dager_per_uke" to deltaker.dagerPerUke,
            "prosent_stilling" to deltaker.prosentStilling,
        )

        session.execute(queryOf(query, params))
    }

    fun getKometHistorikk(
        identer: List<NorskIdent>,
    ): List<TiltakshistorikkV1Dto.TeamKometDeltakelse> {
        @Language("PostgreSQL")
        val query = """
                select
                    deltaker.person_ident as norsk_ident,
                    deltaker.id,
                    deltaker.start_dato,
                    deltaker.slutt_dato,
                    deltaker.status_type,
                    deltaker.status_aarsak,
                    deltaker.status_opprettet_tidspunkt,
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
                order by deltaker.start_dato desc nulls last;
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOfValue(identer) { it.value },
        )

        return session.list(queryOf(query, params)) { it.toTeamKometDeltakelse() }
    }

    fun deleteKometDeltaker(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from komet_deltaker
            where id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    fun get(id: UUID): KometDeltaker? {
        @Language("PostgreSQL")
        val query = """
            select *
            from komet_deltaker
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toTiltakshistorikkKometDeltaker() }
    }
}

private fun Row.toTiltakshistorikkKometDeltaker(): KometDeltaker {
    return KometDeltaker(
        id = uuid("id"),
        gjennomforingId = uuid("gjennomforing_id"),
        personIdent = string("person_ident"),
        startDato = localDateOrNull("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        statusType = DeltakerStatusType.valueOf(string("status_type")),
        statusAarsak = stringOrNull("status_aarsak")?.let { DeltakerStatusAarsakType.valueOf(it) },
        statusOpprettetTidspunkt = localDateTime("status_opprettet_tidspunkt"),
        registrertTidspunkt = localDateTime("registrert_tidspunkt"),
        endretTidspunkt = localDateTime("endret_tidspunkt"),
        dagerPerUke = floatOrNull("dager_per_uke"),
        prosentStilling = floatOrNull("prosent_stilling"),
    )
}

private fun Row.toTeamKometDeltakelse(): TiltakshistorikkV1Dto.TeamKometDeltakelse {
    val tiltakstype = TiltakshistorikkV1Dto.TeamKometDeltakelse.Tiltakstype(
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
    return TiltakshistorikkV1Dto.TeamKometDeltakelse(
        norskIdent = NorskIdent(string("norsk_ident")),
        id = uuid("id"),
        startDato = localDateOrNull("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        tittel = Tiltaksnavn.hosTitleCaseVirksomhet(
            tiltakstype.navn,
            arrangor.hovedenhet?.navn ?: arrangor.underenhet.navn,
        ),
        status = TiltakshistorikkV1Dto.TeamKometDeltakelse.Status(
            type = DeltakerStatusType.valueOf(string("status_type")),
            aarsak = stringOrNull("status_aarsak")?.let { aarsak ->
                DeltakerStatusAarsakType.valueOf(aarsak)
            },
            opprettetDato = localDateTime("status_opprettet_tidspunkt"),
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
