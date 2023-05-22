package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class VirksomhetRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun upsert(virksomhetDto: VirksomhetDto): QueryResult<Unit> = query {
        logger.info("Lagrer virksomhet ${virksomhetDto.organisasjonsnummer}")
        if (virksomhetDto.overordnetEnhet != null) {
            throw IllegalArgumentException("Virksomhet må være en overordnet enhet")
        }

        @Language("PostgreSQL")
        val query = """
            insert into virksomhet(
                organisasjonsnummer,
                navn
            )
            values (:organisasjonsnummer, :navn)
            on conflict (organisasjonsnummer) do update
                set navn = excluded.navn
            returning *
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertUnderenheter = """
             insert into underenhet (organisasjonsnummer, navn, underenhetsnummer)
             values (?, ?, ?)
             on conflict (underenhetsnummer) do nothing
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteUnderenheter = """
             delete from underenhet
             where organisasjonsnummer = ? and not (underenhetsnummer = any (?))
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, virksomhetDto.toSqlParameters()).asExecute)

            virksomhetDto.underenheter?.forEach { underenhet ->
                queryOf(
                    upsertUnderenheter,
                    virksomhetDto.organisasjonsnummer,
                    underenhet.navn,
                    underenhet.organisasjonsnummer,
                ).asExecute.let { tx.run(it) }
            }

            virksomhetDto.underenheter?.let {
                queryOf(
                    deleteUnderenheter,
                    virksomhetDto.organisasjonsnummer,
                    db.createTextArray(virksomhetDto.underenheter.map { it.organisasjonsnummer }),
                ).asExecute.let { tx.run(it) }
            }
        }
    }

    fun get(orgnr: String): QueryResult<VirksomhetDto?> = query {
        @Language("PostgreSQL")
        val selectVirksomhet = """
            select
                v.organisasjonsnummer,
                null as overordnetEnhet,
                v.navn
            from virksomhet v
            where v.organisasjonsnummer = ?
        """.trimIndent()

        @Language("PostgreSQL")
        val selectUnderenhet = """
            select
                u.organisasjonsnummer as overordnetEnhet,
                u.underenhetsnummer as organisasjonsnummer,
                u.navn
            from underenhet u
            where u.underenhetsnummer = ?
        """.trimIndent()

        @Language("PostgreSQL")
        val selectUnderenheterTilVirksomhet = """
            select
                u.organisasjonsnummer as overordnetEnhet,
                u.underenhetsnummer as organisasjonsnummer,
                u.navn
            from underenhet u
            where u.organisasjonsnummer = ?
        """.trimIndent()

        val virksomhet = queryOf(selectVirksomhet, orgnr)
            .map { it.toVirksomhetDto() }
            .asSingle
            .let { db.run(it) }

        if (virksomhet != null) {
            val underenheter = queryOf(selectUnderenheterTilVirksomhet, orgnr)
                .map { it.toVirksomhetDto() }
                .asList
                .let { db.run(it) }
            virksomhet.copy(underenheter = underenheter)
        } else {
            queryOf(selectUnderenhet, orgnr)
                .map { it.toVirksomhetDto() }
                .asSingle
                .let { db.run(it) }
        }
    }

    private fun Row.toVirksomhetDto() = VirksomhetDto(
        organisasjonsnummer = string("organisasjonsnummer"),
        navn = string("navn"),
        overordnetEnhet = stringOrNull("overordnetEnhet"),
        underenheter = null,
    )

    private fun VirksomhetDto.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
    )
}
