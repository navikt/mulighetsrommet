package no.nav.mulighetsrommet.altinn.db

import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.altinn.model.AltinnRessurs
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.intellij.lang.annotations.Language

class AltinnRettigheterRepository(private val db: Database) {
    fun upsertRettighet(personBedriftRettigheter: PersonBedriftRettigheterDbo) = db.transaction { tx ->
        upsertRettighet(personBedriftRettigheter, tx)
    }

    private fun upsertRettighet(personBedriftRettigheter: PersonBedriftRettigheterDbo, tx: Session) {
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
             where norsk_ident = ? and organisasjonsnummer = ? and not (rettighet = any (?))
        """.trimIndent()

        personBedriftRettigheter.bedriftRettigheter.forEach { bedriftRettighet ->
            bedriftRettighet.rettigheter.forEach {
                tx.run(
                    queryOf(
                        upsertRolle,
                        mapOf(
                            "norsk_ident" to personBedriftRettigheter.norskIdent.value,
                            "organisasjonsnummer" to bedriftRettighet.organisasjonsnummer.value,
                            "rettighet" to it.name,
                            "expiry" to personBedriftRettigheter.expiry,
                        ),
                    ).asExecute,
                )
            }

            tx.run(
                queryOf(
                    deleteRoller,
                    personBedriftRettigheter.norskIdent.value,
                    bedriftRettighet.organisasjonsnummer.value,
                    db.createArrayOf("altinn_ressurs", bedriftRettighet.rettigheter.map { it.name }),
                ).asExecute,
            )
        }
    }

    fun getRettigheter(norskIdent: NorskIdent): List<BedriftRettigheterDbo> {
        @Language("PostgreSQL")
        val query = """
            select
                norsk_ident,
                organisasjonsnummer,
                rettighet,
                expiry
            from altinn_person_rettighet
            where norsk_ident = ?
        """.trimIndent()

        return queryOf(query, norskIdent.value)
            .map {
                Organisasjonsnummer(it.string("organisasjonsnummer")) to RettighetDbo(
                    rettighet = AltinnRessurs.valueOf(it.string("rettighet")),
                    expiry = it.localDateTime("expiry"),
                )
            }
            .asList
            .let { db.run(it) }
            .groupBy({ it.first }, { it.second })
            .map {
                BedriftRettigheterDbo(
                    organisasjonsnummer = it.key,
                    rettigheter = it.value,
                )
            }
    }
}
