package no.nav.tiltak.historikk.db.queries

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.model.ArbeidsgiverAvtale
import org.intellij.lang.annotations.Language
import java.util.UUID

class ArbeidsgiverAvtaleQueries(private val session: Session) {

    fun upsert(avtale: ArbeidsgiverAvtale) {
        @Language("PostgreSQL")
        val query = """
            insert into arbeidsgiver_avtale (
                avtale_id,
                norsk_ident,
                organisasjonsnummer,
                tiltakstype,
                start_dato,
                slutt_dato,
                status,
                stillingsprosent,
                dager_per_uke,
                opprettet_tidspunkt,
                oppdatert_tidspunkt
            ) values (
                :avtale_id::uuid,
                :norsk_ident,
                :organisasjonsnummer,
                :tiltakstype,
                :start_dato,
                :slutt_dato,
                :status,
                :stillingsprosent,
                :dager_per_uke,
                :opprettet_tidspunkt,
                :oppdatert_tidspunkt
            )
            on conflict (avtale_id) do update set
                norsk_ident          = excluded.norsk_ident,
                organisasjonsnummer  = excluded.organisasjonsnummer,
                tiltakstype          = excluded.tiltakstype,
                start_dato           = excluded.start_dato,
                slutt_dato           = excluded.slutt_dato,
                status               = excluded.status,
                stillingsprosent     = excluded.stillingsprosent,
                dager_per_uke        = excluded.dager_per_uke,
                opprettet_tidspunkt  = excluded.opprettet_tidspunkt,
                oppdatert_tidspunkt  = excluded.oppdatert_tidspunkt
        """.trimIndent()

        val params = mapOf(
            "avtale_id" to avtale.avtaleId,
            "norsk_ident" to avtale.norskIdent.value,
            "organisasjonsnummer" to avtale.organisasjonsnummer.value,
            "tiltakstype" to avtale.tiltakstype.name,
            "start_dato" to avtale.startDato,
            "slutt_dato" to avtale.sluttDato,
            "status" to avtale.status.name,
            "stillingsprosent" to avtale.stillingsprosent,
            "dager_per_uke" to avtale.dagerPerUke,
            "opprettet_tidspunkt" to avtale.opprettetTidspunkt,
            "oppdatert_tidspunkt" to avtale.oppdatertTidspunkt,
        )

        session.execute(queryOf(query, params))
    }

    fun delete(avtaleId: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from arbeidsgiver_avtale
            where avtale_id = ?::uuid
        """.trimIndent()

        session.execute(queryOf(query, avtaleId))
    }

    fun get(avtaleId: UUID): ArbeidsgiverAvtale? {
        @Language("PostgreSQL")
        val query = """
            select *
            from arbeidsgiver_avtale
            where avtale_id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, avtaleId)) { it.toArbeidsgiverAvtale() }
    }

    fun getByNorskIdent(identer: List<NorskIdent>): List<ArbeidsgiverAvtale> {
        @Language("PostgreSQL")
        val query = """
            select *
            from arbeidsgiver_avtale
            where norsk_ident = any(:identer)
        """.trimIndent()

        val params = mapOf(
            "identer" to session.createArrayOfValue(identer) { it.value },
        )

        return session.list(queryOf(query, params)) { it.toArbeidsgiverAvtale() }
    }
}

private fun Row.toArbeidsgiverAvtale(): ArbeidsgiverAvtale {
    return ArbeidsgiverAvtale(
        avtaleId = uuid("avtale_id"),
        norskIdent = NorskIdent(string("norsk_ident")),
        organisasjonsnummer = Organisasjonsnummer(string("organisasjonsnummer")),
        tiltakstype = Tiltakskode.valueOf(string("tiltakstype")),
        startDato = localDateOrNull("start_dato"),
        sluttDato = localDateOrNull("slutt_dato"),
        status = ArbeidsgiverAvtale.Status.valueOf(string("status")),
        stillingsprosent = floatOrNull("stillingsprosent"),
        dagerPerUke = floatOrNull("dager_per_uke"),
        opprettetTidspunkt = instant("opprettet_tidspunkt"),
        oppdatertTidspunkt = instant("oppdatert_tidspunkt"),
    )
}
