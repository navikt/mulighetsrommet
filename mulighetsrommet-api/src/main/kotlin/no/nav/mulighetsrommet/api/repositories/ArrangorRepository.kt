package no.nav.mulighetsrommet.api.repositories

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.domain.dto.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.ArrangorTil
import no.nav.mulighetsrommet.api.domain.dto.BrregVirksomhetDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.domain.serializers.UUIDSerializer
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
        pagination: Pagination = Pagination.all(),
        sortering: String? = null,
    ): PaginatedResult<ArrangorDto> {
        val order = when (sortering) {
            "navn-ascending" -> "arrangor.navn asc"
            "navn-descending" -> "arrangor.navn desc"
            else -> "arrangor.navn asc"
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
                arrangor.poststed,
                count(*) over() as total_count
            from arrangor
            where ${
            when (til) {
                ArrangorTil.AVTALE -> {
                    "id in (select arrangor_hovedenhet_id from avtale) and"
                }

                ArrangorTil.TILTAKSGJENNOMFORING -> {
                    "id in (select arrangor_id from tiltaksgjennomforing) and"
                }

                else -> ""
            }
        }
              (:sok::text is null or arrangor.navn ilike :sok or arrangor.organisasjonsnummer ilike :sok)
              and (:overordnet_enhet::text is null or arrangor.overordnet_enhet = :overordnet_enhet)
              and (:slettet::boolean is null or arrangor.slettet_dato is not null = :slettet)
              and (:utenlandsk::boolean is null or arrangor.er_utenlandsk_virksomhet = :utenlandsk)
            order by $order
            limit :limit
            offset :offset
        """.trimIndent()

        val params = mapOf(
            "sok" to sok?.let { "%$it%" },
            "overordnet_enhet" to overordnetEnhetOrgnr,
            "slettet" to slettet,
            "utenlandsk" to utenlandsk,
        )

        return db.useSession { session ->
            queryOf(query, params + pagination.parameters)
                .mapPaginated { it.toVirksomhetDto() }
                .runWithSession(session)
        }
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

    fun getHovedenhetBy(id: UUID): ArrangorDto {
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

        @Language("PostgreSQL")
        val queryForUnderenheter = """
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
            order by navn
        """.trimIndent()

        val underenheter = when (arrangor != null) {
            true -> queryOf(queryForUnderenheter, arrangor.organisasjonsnummer)
                .map { it.toVirksomhetDto() }
                .asList
                .let { db.run(it) }

            else -> emptyList()
        }

        val arrangorMedUnderenheter = arrangor?.copy(underenheter = underenheter)

        return requireNotNull(arrangorMedUnderenheter) {
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

    fun koblingerTilKontaktperson(kontaktpersonId: UUID): Pair<List<DokumentKoblingForKontaktperson>, List<DokumentKoblingForKontaktperson>> {
        @Language("PostgreSQL")
        val gjennomforingQuery = """
            select tg.navn, tg.id from tiltaksgjennomforing_arrangor_kontaktperson tak join tiltaksgjennomforing tg on tak.tiltaksgjennomforing_id = tg.id
            where tak.arrangor_kontaktperson_id = ?
        """.trimIndent()

        val gjennomforinger = queryOf(gjennomforingQuery, kontaktpersonId)
            .map {
                DokumentKoblingForKontaktperson(
                    id = it.uuid("id"),
                    navn = it.string("navn"),
                )
            }
            .asList
            .let { db.run(it) }

        @Language("PostgreSQL")
        val avtaleQuery = """
            select a.navn, a.id from avtale_arrangor_kontaktperson aak join avtale a on aak.avtale_id = a.id
             where arrangor_kontaktperson_id = ?
        """.trimIndent()

        val avtaler = queryOf(avtaleQuery, kontaktpersonId)
            .map {
                DokumentKoblingForKontaktperson(
                    id = it.uuid("id"),
                    navn = it.string("navn"),
                )
            }
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

@Serializable
data class DokumentKoblingForKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
