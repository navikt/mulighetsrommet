package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dbo.OverordnetEnhetDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetKontaktperson
import no.nav.mulighetsrommet.api.utils.VirksomhetTil
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class VirksomhetRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Upserter en overordnet enhet og oppdaterer listen med underenheter */
    fun upsertOverordnetEnhet(overordnetEnhetDbo: OverordnetEnhetDbo) {
        logger.info("Lagrer overordnet enhet ${overordnetEnhetDbo.organisasjonsnummer}")

        @Language("PostgreSQL")
        val query = """
            insert into virksomhet(organisasjonsnummer, navn, slettet_dato, postnummer, poststed)
            values (:organisasjonsnummer, :navn, :slettet_dato, :postnummer, :poststed)
            on conflict (organisasjonsnummer) do update set
                navn         = excluded.navn,
                slettet_dato = excluded.slettet_dato,
                postnummer   = excluded.postnummer,
                poststed     = excluded.poststed
            returning *
        """.trimIndent()

        @Language("PostgreSQL")
        val upsertUnderenheter = """
            insert into virksomhet (organisasjonsnummer, navn, overordnet_enhet, slettet_dato, postnummer, poststed)
            values (:organisasjonsnummer, :navn, :overordnet_enhet, :slettet_dato, :postnummer, :poststed)
            on conflict (organisasjonsnummer) do update set
                navn             = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet,
                slettet_dato     = excluded.slettet_dato,
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
                logger.info("Lagrer underenhet ${underenhet.organisasjonsnummer}")
                queryOf(upsertUnderenheter, underenhet.toSqlParameters())
                    .asExecute
                    .let { tx.run(it) }
            }

            queryOf(
                deleteUnderenheter,
                overordnetEnhetDbo.organisasjonsnummer,
                db.createTextArray(overordnetEnhetDbo.underenheter.map { it.organisasjonsnummer }),
            ).asExecute.let { tx.run(it) }
        }
    }

    /** Upserter kun enheten og tar ikke hensyn til underenheter */
    fun upsert(virksomhetDto: VirksomhetDto) {
        logger.info("Lagrer virksomhet ${virksomhetDto.organisasjonsnummer}")

        @Language("PostgreSQL")
        val query = """
            insert into virksomhet(organisasjonsnummer, navn, overordnet_enhet, slettet_dato, postnummer, poststed)
            values (:organisasjonsnummer, :navn, :overordnet_enhet, :slettet_dato, :postnummer, :poststed)
            on conflict (organisasjonsnummer) do update set
                navn = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet,
                slettet_dato = excluded.slettet_dato,
                postnummer = excluded.postnummer,
                poststed = excluded.poststed
            returning *
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, virksomhetDto.toSqlParameters()).asExecute)
        }
    }

    fun getAll(til: VirksomhetTil? = null): List<VirksomhetDto> {
        val join = when (til) {
            VirksomhetTil.AVTALE -> {
                "inner join avtale on avtale.leverandor_organisasjonsnummer = v.organisasjonsnummer"
            }

            VirksomhetTil.TILTAKSGJENNOMFORING -> {
                "inner join tiltaksgjennomforing t on t.arrangor_organisasjonsnummer = v.organisasjonsnummer"
            }

            else -> ""
        }

        @Language("PostgreSQL")
        val selectVirksomheter = """
            select distinct
                v.organisasjonsnummer,
                v.overordnet_enhet,
                v.navn,
                v.slettet_dato,
                v.postnummer,
                v.poststed
            from virksomhet v
                $join
            order by v.navn asc
        """.trimIndent()

        return queryOf(selectVirksomheter)
            .map { it.toVirksomhetDto() }
            .asList
            .let { db.run(it) }
    }

    fun get(orgnr: String): VirksomhetDto? {
        @Language("PostgreSQL")
        val selectVirksomhet = """
            select
                v.organisasjonsnummer,
                v.overordnet_enhet,
                v.navn,
                v.slettet_dato,
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
                v.slettet_dato,
                v.postnummer,
                v.poststed
            from virksomhet v
            where v.overordnet_enhet = ?
        """.trimIndent()

        val virksomhet = queryOf(selectVirksomhet, orgnr)
            .map { it.toVirksomhetDto() }
            .asSingle
            .let { db.run(it) }

        return if (virksomhet != null) {
            val underenheter = queryOf(selectUnderenheterTilVirksomhet, orgnr)
                .map { it.toVirksomhetDto() }
                .asList
                .let { db.run(it) }
                .takeIf { it.isNotEmpty() }
            virksomhet.copy(underenheter = underenheter)
        } else {
            null
        }
    }

    fun delete(orgnr: String) {
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
            insert into virksomhet_kontaktperson(id, organisasjonsnummer, navn, telefon, epost, beskrivelse)
            values (:id::uuid, :organisasjonsnummer, :navn, :telefon, :epost, :beskrivelse)
            on conflict (id) do update set
                navn                = excluded.navn,
                organisasjonsnummer = excluded.organisasjonsnummer,
                telefon             = excluded.telefon,
                epost               = excluded.epost,
                beskrivelse         = excluded.beskrivelse
            returning *
        """.trimIndent()

        return queryOf(upsertVirksomhetKontaktperson, virksomhetKontaktperson.toSqlParameters())
            .map { it.toVirksomhetKontaktperson() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun koblingerTilKontaktperson(id: UUID): Pair<List<UUID>, List<UUID>> {
        @Language("PostgreSQL")
        val gjennomforingQuery = """
            select tiltaksgjennomforing_id from tiltaksgjennomforing_virksomhet_kontaktperson where virksomhet_kontaktperson_id = ?
        """.trimIndent()

        val gjennomforinger = queryOf(gjennomforingQuery, id)
            .map { it.uuid("tiltaksgjennomforing_id") }
            .asList
            .let { db.run(it) }

        @Language("PostgreSQL")
        val avtaleQuery = """
            select id from avtale tg where leverandor_kontaktperson_id = ?
        """.trimIndent()

        val avtaler = queryOf(avtaleQuery, id)
            .map { it.uuid("id") }
            .asList
            .let { db.run(it) }

        return gjennomforinger to avtaler
    }

    fun deleteKontaktperson(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from virksomhet_kontaktperson where id = ?
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getKontaktpersoner(orgnr: String): List<VirksomhetKontaktperson> {
        @Language("PostgreSQL")
        val query = """
            select
                vk.id,
                vk.organisasjonsnummer,
                vk.navn,
                vk.telefon,
                vk.epost,
                vk.beskrivelse
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
        slettetDato = localDateOrNull("slettet_dato"),
        postnummer = stringOrNull("postnummer"),
        poststed = stringOrNull("poststed"),
    )

    private fun Row.toVirksomhetKontaktperson() = VirksomhetKontaktperson(
        id = uuid("id"),
        organisasjonsnummer = string("organisasjonsnummer"),
        navn = string("navn"),
        telefon = stringOrNull("telefon"),
        epost = string("epost"),
        beskrivelse = stringOrNull("beskrivelse"),
    )

    private fun VirksomhetDto.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "overordnet_enhet" to overordnetEnhet,
        "slettet_dato" to slettetDato,
        "postnummer" to postnummer,
        "poststed" to poststed,
    )

    private fun VirksomhetKontaktperson.toSqlParameters() = mapOf(
        "id" to id,
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "telefon" to telefon,
        "epost" to epost,
        "beskrivelse" to beskrivelse,
    )

    private fun OverordnetEnhetDbo.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "slettet_dato" to slettetDato,
        "postnummer" to postnummer,
        "poststed" to poststed,
    )
}
