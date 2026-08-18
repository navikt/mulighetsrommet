package no.nav.tiltak.historikk.db.queries

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.model.Virksomhet
import org.intellij.lang.annotations.Language

class VirksomhetQueries(private val session: Session) {

    fun upsert(virksomhet: Virksomhet) {
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

    fun get(organisasjonsnummer: Organisasjonsnummer): Virksomhet? {
        @Language("PostgreSQL")
        val query = """
            select organisasjonsnummer, overordnet_enhet_organisasjonsnummer, navn, organisasjonsform, slettet_dato
            from virksomhet
            where organisasjonsnummer = ?
        """.trimIndent()

        return session.single(queryOf(query, organisasjonsnummer.value)) { row ->
            Virksomhet(
                organisasjonsnummer = Organisasjonsnummer(row.string("organisasjonsnummer")),
                overordnetEnhetOrganisasjonsnummer = row.stringOrNull("overordnet_enhet_organisasjonsnummer")?.let {
                    Organisasjonsnummer(it)
                },
                navn = row.stringOrNull("navn"),
                organisasjonsform = row.stringOrNull("organisasjonsform"),
                slettetDato = row.localDateOrNull("slettet_dato"),
            )
        }
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
