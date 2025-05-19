package no.nav.mulighetsrommet.altinn.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.altinn.model.BedriftRettigheter
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.time.Instant

class AltinnRettigheterQueries(private val session: Session) {
    fun upsertRettigheter(
        norskIdent: NorskIdent,
        bedriftRettigheter: List<BedriftRettigheter>,
        expiry: Instant,
    ) {
        @Language("PostgreSQL")
        val upsertRolle = """
             insert into altinn_person_rettighet (
                norsk_ident,
                organisasjonsnummer,
                rettighet,
                expiry
             ) values (
                :norsk_ident,
                :organisasjonsnummer,
                :rettighet::altinn_ressurs,
                :expiry
             ) on conflict (norsk_ident, organisasjonsnummer) do update set
                rettighet = excluded.rettighet,
                expiry = excluded.expiry
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteRoller = """
             delete from altinn_person_rettighet
             where norsk_ident = :norsk_ident
               and organisasjonsnummer = :organisasjonsnummer
               and not (rettighet = any (:rettigheter))
        """.trimIndent()

        bedriftRettigheter.forEach { bedriftRettighet ->
            val roller = bedriftRettighet.rettigheter.map {
                mapOf(
                    "norsk_ident" to norskIdent.value,
                    "organisasjonsnummer" to bedriftRettighet.organisasjonsnummer.value,
                    "rettighet" to it.name,
                    "expiry" to expiry,
                )
            }
            session.batchPreparedNamedStatement(upsertRolle, roller)

            val deleteParams = mapOf(
                "norsk_ident" to norskIdent.value,
                "organisasjonsnummer" to bedriftRettighet.organisasjonsnummer.value,
                "rettigheter" to session.createArrayOfAltinnRessurs(bedriftRettighet.rettigheter),
            )
            session.execute(queryOf(deleteRoller, deleteParams))
        }
    }

    fun getRettigheter(norskIdent: NorskIdent): List<BedriftRettigheterDbo> {
        @Language("PostgreSQL")
        val query = """
            select
                organisasjonsnummer,
                rettighet,
                expiry
            from altinn_person_rettighet
            where norsk_ident = ?
        """.trimIndent()

        val rettigheterForOrgnummer = session.list(queryOf(query, norskIdent.value)) {
            Organisasjonsnummer(it.string("organisasjonsnummer")) to BedriftRettighetWithExpiry(
                rettighet = AltinnRessurs.valueOf(it.string("rettighet")),
                expiry = it.instant("expiry"),
            )
        }
        return rettigheterForOrgnummer.groupBy({ it.first }, { it.second }).map {
            BedriftRettigheterDbo(
                organisasjonsnummer = it.key,
                rettigheter = it.value,
            )
        }
    }

    fun deleteAll() {
        @Language("PostgreSQL")
        val query = "delete from altinn_person_rettighet"
        session.execute(queryOf(query))
    }
}

fun Session.createArrayOfAltinnRessurs(
    ressurs: List<AltinnRessurs>,
): Array = createArrayOf("altinn_ressurs", ressurs)
