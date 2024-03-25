package no.nav.mulighetsrommet.api.repositories

import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.ArrangorTil
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.database.Database
import org.intellij.lang.annotations.Language
import org.slf4j.LoggerFactory
import java.util.*

class ArrangorRepository(private val db: Database) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /** Upserter kun enheten og tar ikke hensyn til underenheter */
    fun upsert(arrangor: ArrangorDto) {
        @Language("PostgreSQL")
        val query = """
            insert into arrangor(id, organisasjonsnummer, navn, overordnet_enhet, slettet_dato, postnummer, poststed)
            values (:id, :organisasjonsnummer, :navn, :overordnet_enhet, :slettet_dato, :postnummer, :poststed)
            on conflict (id) do update set
                organisasjonsnummer = excluded.organisasjonsnummer,
                navn = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet,
                slettet_dato = excluded.slettet_dato,
                postnummer = excluded.postnummer,
                poststed = excluded.poststed
            returning *
        """.trimIndent()

        val parameters = arrangor.run {
            mapOf(
                "id" to id,
                "organisasjonsnummer" to organisasjonsnummer,
                "navn" to navn,
                "overordnet_enhet" to overordnetEnhet,
                "slettet_dato" to slettetDato,
                "postnummer" to postnummer,
                "poststed" to poststed,
            )
        }

        db.run(queryOf(query, parameters).asExecute)
    }

    /** Upserter kun enheten og tar ikke hensyn til underenheter */
    fun upsert(brregVirksomhet: BrregVirksomhetDto) {
        logger.info("Lagrer arrangør ${brregVirksomhet.organisasjonsnummer}")

        @Language("PostgreSQL")
        val query = """
            insert into arrangor(organisasjonsnummer, navn, overordnet_enhet, slettet_dato, postnummer, poststed)
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
            tx.run(queryOf(query, brregVirksomhet.toSqlParameters()).asExecute)
        }
    }

    fun getAll(
        til: ArrangorTil? = null,
        sok: String? = null,
        overordnetEnhetOrgnr: String? = null,
        slettet: Boolean? = null,
        utenlandsk: Boolean? = null,
    ): List<ArrangorDto> {
        val join = when (til) {
            ArrangorTil.AVTALE -> {
                "inner join avtale on avtale.arrangor_hovedenhet_id = arrangor.id"
            }

            ArrangorTil.TILTAKSGJENNOMFORING -> {
                "inner join tiltaksgjennomforing t on t.arrangor_id = arrangor.id"
            }

            else -> ""
        }

        @Language("PostgreSQL")
        val query = """
            select distinct
                arrangor.id,
                arrangor.organisasjonsnummer,
                arrangor.overordnet_enhet,
                arrangor.navn,
                arrangor.slettet_dato,
                arrangor.postnummer,
                arrangor.poststed
            from arrangor
                $join
            where (:sok::text is null or arrangor.navn ilike :sok)
              and (:overordnet_enhet::text is null or arrangor.overordnet_enhet = :overordnet_enhet)
              and (:slettet::boolean is null or arrangor.slettet_dato is not null = :slettet)
              and (:utenlandsk::boolean is null or arrangor.er_utenlandsk_virksomhet = :utenlandsk)
            order by arrangor.navn asc
        """.trimIndent()

        val params = mapOf(
            "sok" to sok?.let { "%$it%" },
            "overordnet_enhet" to overordnetEnhetOrgnr,
            "slettet" to slettet,
            "utenlandsk" to utenlandsk,
        )

        return queryOf(query, params)
            .map { it.toVirksomhetDto() }
            .asList
            .let { db.run(it) }
    }

    fun get(orgnr: String): ArrangorDto? {
        @Language("PostgreSQL")
        val selectHovedenhet = """
            select
                id,
                organisasjonsnummer,
                overordnet_enhet,
                navn,
                slettet_dato,
                postnummer,
                poststed
            from arrangor
            where organisasjonsnummer = ?
        """.trimIndent()

        @Language("PostgreSQL")
        val selectUnderenheter = """
            select
                id,
                organisasjonsnummer,
                overordnet_enhet,
                navn,
                slettet_dato,
                postnummer,
                poststed
            from arrangor
            where overordnet_enhet = ?
        """.trimIndent()

        val arrangor = queryOf(selectHovedenhet, orgnr)
            .map { it.toVirksomhetDto() }
            .asSingle
            .let { db.run(it) }

        return if (arrangor != null) {
            val underenheter = queryOf(selectUnderenheter, orgnr)
                .map { it.toVirksomhetDto() }
                .asList
                .let { db.run(it) }
                .takeIf { it.isNotEmpty() }
            arrangor.copy(underenheter = underenheter)
        } else {
            null
        }
    }

    fun getById(id: UUID): ArrangorDto {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                organisasjonsnummer,
                overordnet_enhet,
                navn,
                slettet_dato,
                postnummer,
                poststed
            from arrangor
            where id = ?::uuid
        """.trimIndent()

        val arrangor = queryOf(query, id)
            .map { it.toVirksomhetDto() }
            .asSingle
            .let { db.run(it) }

        return requireNotNull(arrangor) {
            "Arrangør med id=$id finnes ikke"
        }
    }

    fun delete(orgnr: String) {
        logger.info("Sletter arrangør orgnr=$orgnr")

        @Language("PostgreSQL")
        val query = """
            delete from arrangor where organisasjonsnummer = ?
        """.trimIndent()

        db.transaction { tx ->
            tx.run(queryOf(query, orgnr).asExecute)
        }
    }

    fun upsertKontaktperson(kontaktperson: ArrangorKontaktperson): ArrangorKontaktperson {
        @Language("PostgreSQL")
        val upsert = """
            insert into arrangor_kontaktperson(id, arrangor_id, navn, telefon, epost, beskrivelse)
            values (:id::uuid, :arrangor_id, :navn, :telefon, :epost, :beskrivelse)
            on conflict (id) do update set
                navn                = excluded.navn,
                arrangor_id         = excluded.arrangor_id,
                telefon             = excluded.telefon,
                epost               = excluded.epost,
                beskrivelse         = excluded.beskrivelse
            returning *
        """.trimIndent()

        return queryOf(upsert, kontaktperson.toSqlParameters())
            .map { it.toArrangorKontaktperson() }
            .asSingle
            .let { db.run(it)!! }
    }

    fun koblingerTilKontaktperson(id: UUID): Pair<List<UUID>, List<UUID>> {
        @Language("PostgreSQL")
        val gjennomforingQuery = """
            select tiltaksgjennomforing_id from tiltaksgjennomforing_arrangor_kontaktperson where arrangor_kontaktperson_id = ?
        """.trimIndent()

        val gjennomforinger = queryOf(gjennomforingQuery, id)
            .map { it.uuid("tiltaksgjennomforing_id") }
            .asList
            .let { db.run(it) }

        @Language("PostgreSQL")
        val avtaleQuery = """
            select id from avtale where arrangor_kontaktperson_id = ?
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
            delete from arrangor_kontaktperson where id = ?
        """.trimIndent()

        queryOf(query, id)
            .asUpdate
            .let { db.run(it) }
    }

    fun getKontaktpersoner(arrangorId: UUID): List<ArrangorKontaktperson> {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                arrangor_id,
                navn,
                telefon,
                epost,
                beskrivelse
            from arrangor_kontaktperson
            where arrangor_id = ?::uuid
        """.trimIndent()

        return queryOf(query, arrangorId)
            .map { it.toArrangorKontaktperson() }
            .asList
            .let { db.run(it) }
    }

    private fun Row.toVirksomhetDto() = ArrangorDto(
        id = uuid("id"),
        organisasjonsnummer = string("organisasjonsnummer"),
        navn = string("navn"),
        overordnetEnhet = stringOrNull("overordnet_enhet"),
        slettetDato = localDateOrNull("slettet_dato"),
        postnummer = stringOrNull("postnummer"),
        poststed = stringOrNull("poststed"),
    )

    private fun Row.toArrangorKontaktperson() = ArrangorKontaktperson(
        id = uuid("id"),
        arrangorId = uuid("arrangor_id"),
        navn = string("navn"),
        telefon = stringOrNull("telefon"),
        epost = string("epost"),
        beskrivelse = stringOrNull("beskrivelse"),
    )

    private fun BrregVirksomhetDto.toSqlParameters() = mapOf(
        "organisasjonsnummer" to organisasjonsnummer,
        "navn" to navn,
        "overordnet_enhet" to overordnetEnhet,
        "slettet_dato" to slettetDato,
        "postnummer" to postnummer,
        "poststed" to poststed,
    )

    private fun ArrangorKontaktperson.toSqlParameters() = mapOf(
        "id" to id,
        "arrangor_id" to arrangorId,
        "navn" to navn,
        "telefon" to telefon,
        "epost" to epost,
        "beskrivelse" to beskrivelse,
    )
}
