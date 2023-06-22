package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.OverordnetEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.utils.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.QueryResult
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dto.VirksomhetKontaktperson
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
                navn        = excluded.navn,
                postnummer  = excluded.postnummer,
                poststed    = excluded.poststed
            returning *
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertUnderenheter = """
             insert into virksomhet (overordnet_enhet, navn, organisasjonsnummer, postnummer, poststed)
             values (?, ?, ?, ?, ?)
             on conflict (organisasjonsnummer) do update set
                navn             = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet,
                postnummer       = excluded.postnummer,
                poststed         = excluded.poststed
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
                overordnet_enhet = excluded.overordnet_enhet,
                postnummer = excluded.postnummer,
                poststed = excluded.poststed
            returning *
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, virksomhetDto.toSqlParameters()).asExecute)
        }
    }

    fun getAll(filter: VirksomhetFilter): QueryResult<List<VirksomhetDto>> = query {
        val join = when (filter.til) {
            VirksomhetTil.AVTALE -> {
                "inner join avtale on avtale.leverandor_organisasjonsnummer = v.organisasjonsnummer"
            }
            VirksomhetTil.TILTAKSGJENNOMFORING -> {
                "inner join tiltaksgjennomforing t on t.virksomhetsnummer = v.organisasjonsnummer"
            }
            else -> ""
        }

        @Language("PostgreSQL")
        val selectVirksomheter = """
            select distinct
                v.organisasjonsnummer,
                v.overordnet_enhet,
                v.navn,
                v.postnummer,
                v.poststed
            from virksomhet v
                $join
            order by v.navn asc
        """.trimIndent()

        queryOf(selectVirksomheter)
            .map { it.toVirksomhetDto() }
            .asList
            .let { db.run(it) }
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

    fun upsertKontaktperson(virksomhetKontaktperson: VirksomhetKontaktperson): VirksomhetKontaktperson {
        @Language("PostgreSQL")
        val upsertVirksomhetKontaktperson = """
            insert into virksomhet_kontaktperson(id, organisasjonsnummer, navn, telefon, epost)
            values (:id::uuid, :organisasjonsnummer, :navn, :telefon, :epost)
            on conflict (id) do update set
                navn                = excluded.navn,
                organisasjonsnummer = excluded.organisasjonsnummer,
                telefon             = excluded.telefon,
                epost               = excluded.epost
            returning *
        """.trimIndent()

        return queryOf(upsertVirksomhetKontaktperson, virksomhetKontaktperson.toSqlParameters())
            .map { it.toVirksomhetKontaktperson() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun getKontaktpersoner(orgnr: String): List<VirksomhetKontaktperson> {
        @Language("PostgreSQL")
        val query = """
            select
                vk.id,
                vk.organisasjonsnummer,
                vk.navn,
                vk.telefon,
                vk.epost
            from virksomhet_kontaktperson vk
            where vk.organisasjonsnummer = ?
        """.trimIndent()

        return queryOf(query, orgnr)
            .map { it.toVirksomhetKontaktperson() }
            .asList
            .let { db.run(it) }
    }

    private fun Row.toVirksomhetDto() = VirksomhetDto(
        organisasjonsnummer = string("organisasjonsnummer"),
        navn = string("navn"),
        overordnetEnhet = stringOrNull("overordnet_enhet"),
        underenheter = null,
        postnummer = stringOrNull("postnummer"),
        poststed = stringOrNull("poststed"),
    )

    private fun Row.toVirksomhetKontaktperson() = VirksomhetKontaktperson(
        id = uuid("id"),
        organisasjonsnummer = string("organisasjonsnummer"),
        navn = string("navn"),
        telefon = string("telefon"),
        epost = string("epost"),
    )

    private fun VirksomhetDto.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "overordnet_enhet" to overordnetEnhet,
        "postnummer" to postnummer,
        "poststed" to poststed,
    )

    private fun VirksomhetKontaktperson.toSqlParameters() = mapOf(
        "id" to id,
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "telefon" to telefon,
        "epost" to epost,
    )

    private fun OverordnetEnhetDbo.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "postnummer" to postnummer,
        "poststed" to poststed,
    )
}
