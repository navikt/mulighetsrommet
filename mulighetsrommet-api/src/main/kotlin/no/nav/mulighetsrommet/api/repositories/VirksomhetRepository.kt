package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.OverordnetEnhetDbo
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

    /** Upserter en overordnet enhet og oppdaterer listen med underenheter */
    fun upsertOverordnetEnhet(overordnetEnhetDbo: OverordnetEnhetDbo): QueryResult<Unit> = query {
        logger.info("Lagrer overordnet enhet ${overordnetEnhetDbo.organisasjonsnummer}")

        @Language("PostgreSQL")
        val query = """
            insert into virksomhet(
                organisasjonsnummer,
                navn,
                postnummer,
                poststed
            )
            values (:organisasjonsnummer, :navn, :postnummer, :poststed)
            on conflict (organisasjonsnummer) do update set
                navn = excluded.navn
            returning *
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertUnderenheter = """
             insert into virksomhet (overordnet_enhet, navn, organisasjonsnummer, postnummer, poststed)
             values (?, ?, ?, ?, ?)
             on conflict (organisasjonsnummer) do update set
                navn             = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet
            returning *
        """.trimIndent()

        @Language("PostgreSQL")
        val deleteUnderenheter = """
             delete from virksomhet
             where overordnet_enhet = ? and not (organisasjonsnummer = any (?))
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, overordnetEnhetDbo.toSqlParameters()).asExecute)

            overordnetEnhetDbo.underenheter.forEach { underenhet ->
                queryOf(
                    upsertUnderenheter,
                    overordnetEnhetDbo.organisasjonsnummer,
                    underenhet.navn,
                    underenhet.organisasjonsnummer,
                    underenhet.postnummer,
                    underenhet.poststed,
                ).asExecute.let { tx.run(it) }
            }

            queryOf(
                deleteUnderenheter,
                overordnetEnhetDbo.organisasjonsnummer,
                db.createTextArray(overordnetEnhetDbo.underenheter.map { it.organisasjonsnummer }),
            ).asExecute.let { tx.run(it) }
        }
    }

    /** Upserter kun enheten og tar ikke hensyn til underenheter */
    fun upsert(virksomhetDto: VirksomhetDto): QueryResult<Unit> = query {
        logger.info("Lagrer virksomhet ${virksomhetDto.organisasjonsnummer}")

        @Language("PostgreSQL")
        val query = """
            insert into virksomhet(
                organisasjonsnummer,
                navn,
                overordnet_enhet,
                postnummer,
                poststed
            )
            values (:organisasjonsnummer, :navn, :overordnet_enhet, :postnummer, :poststed)
            on conflict (organisasjonsnummer) do update set
                navn = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet
            returning *
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, virksomhetDto.toSqlParameters()).asExecute)
        }
    }

    fun get(orgnr: String): QueryResult<VirksomhetDto?> = query {
        @Language("PostgreSQL")
        val selectVirksomhet = """
            select
                v.organisasjonsnummer,
                v.overordnet_enhet,
                v.navn,
                v.postnummer,
                v.poststed
            from virksomhet v
            where v.organisasjonsnummer = ?
        """.trimIndent()

        @Language("PostgreSQL")
        val selectUnderenhet = """
            select
                v.organisasjonsnummer,
                v.overordnet_enhet,
                v.navn,
                v.postnummer,
                v.poststed
            from virksomhet v
            where v.organisasjonsnummer = ?
        """.trimIndent()

        @Language("PostgreSQL")
        val selectUnderenheterTilVirksomhet = """
            select
                v.organisasjonsnummer,
                v.overordnet_enhet,
                v.navn,
                v.postnummer,
                v.poststed
            from virksomhet v
            where v.overordnet_enhet = ?
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

    fun delete(orgnr: String): QueryResult<Unit> = query {
        logger.info("Sletter virksomhet $orgnr")

        @Language("PostgreSQL")
        val query = """
            delete from virksomhet where organisasjonsnummer = ?
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, orgnr).asExecute)
        }
    }

    private fun Row.toVirksomhetDto() = VirksomhetDto(
        organisasjonsnummer = string("organisasjonsnummer"),
        navn = string("navn"),
        overordnetEnhet = stringOrNull("overordnet_enhet"),
        underenheter = null,
        postnummer = stringOrNull("postnummer"),
        poststed = stringOrNull("poststed"),
    )

    private fun VirksomhetDto.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "overordnet_enhet" to overordnetEnhet,
        "postnummer" to postnummer,
        "poststed" to poststed,
    )

    private fun OverordnetEnhetDbo.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "postnummer" to postnummer,
        "poststed" to poststed,
    )
}
