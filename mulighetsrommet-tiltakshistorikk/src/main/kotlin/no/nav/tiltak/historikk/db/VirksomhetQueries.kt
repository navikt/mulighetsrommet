package no.nav.tiltak.historikk.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import java.time.LocalDate

data class VirksomhetDbo(
    val organisasjonsnummer: Organisasjonsnummer,
    val overordnetEnhetOrganisasjonsnummer: Organisasjonsnummer?,
    val navn: String?,
    val organisasjonsform: String?,
    val slettetDato: LocalDate?,
)

class VirksomhetQueries(private val session: Session) {

    fun upsert(virksomhet: VirksomhetDbo) {
        @Language("PostgreSQL")
        val query = """
            insert into virksomhet (organisasjonsnummer, overordnet_enhet_organisasjonsnummer, navn, organisasjonsform, slettet_dato)
            values (:organisasjonsnummer, :overordnet_enhet_organisasjonsnummer, :navn, :organisasjonsform, :slettet_dato)
            on conflict (organisasjonsnummer) do update set
                overordnet_enhet_organisasjonsnummer = excluded.overordnet_enhet_organisasjonsnummer,
                navn = excluded.navn,
                organisasjonsform = excluded.organisasjonsform,
                slettet_dato = excluded.slettet_dato;
        """.trimIndent()

        val params = mapOf(
            "organisasjonsnummer" to virksomhet.organisasjonsnummer.value,
            "overordnet_enhet_organisasjonsnummer" to virksomhet.overordnetEnhetOrganisasjonsnummer?.value,
            "navn" to virksomhet.navn,
            "organisasjonsform" to virksomhet.organisasjonsform,
            "slettet_dato" to virksomhet.slettetDato,
        )

        session.execute(queryOf(query, params))
    }

    fun delete(organisasjonsnummer: Organisasjonsnummer) {
        @Language("PostgreSQL")
        val query = """
            delete from virksomhet
            where organisasjonsnummer = ?
        """.trimIndent()

        session.execute(queryOf(query, organisasjonsnummer.value))
    }
}
