package no.nav.mulighetsrommet.api.arrangor.db

import kotlinx.serialization.Serializable
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorTil
import no.nav.mulighetsrommet.api.arrangor.model.BrregVirksomhetDto
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.util.*

class ArrangorQueries(private val session: Session) {

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
                "organisasjonsnummer" to organisasjonsnummer.value,
                "navn" to navn,
                "overordnet_enhet" to overordnetEnhet?.value,
                "slettet_dato" to slettetDato,
                "postnummer" to postnummer,
                "poststed" to poststed,
            )
        }

        session.execute(queryOf(query, parameters))
    }

    /** Upserter kun enheten og tar ikke hensyn til underenheter */
    fun upsert(brregVirksomhet: BrregVirksomhetDto) {
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

        val params = mapOf(
            "organisasjonsnummer" to brregVirksomhet.organisasjonsnummer.value,
            "navn" to brregVirksomhet.navn,
            "overordnet_enhet" to brregVirksomhet.overordnetEnhet?.value,
            "slettet_dato" to brregVirksomhet.slettetDato,
            "postnummer" to brregVirksomhet.postnummer,
            "poststed" to brregVirksomhet.poststed,
        )

        session.execute(queryOf(query, params))
    }

    fun getAll(
        til: ArrangorTil? = null,
        sok: String? = null,
        overordnetEnhetOrgnr: Organisasjonsnummer? = null,
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

        val isRelatedToTiltak = when (til) {
            ArrangorTil.AVTALE -> "id in (select arrangor_hovedenhet_id from avtale) and"
            ArrangorTil.TILTAKSGJENNOMFORING -> "id in (select arrangor_id from gjennomforing) and"
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
                arrangor.poststed,
                count(*) over() as total_count
            from arrangor
            where $isRelatedToTiltak
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
            "overordnet_enhet" to overordnetEnhetOrgnr?.value,
            "slettet" to slettet,
            "utenlandsk" to utenlandsk,
        )

        return queryOf(query, params + pagination.parameters)
            .mapPaginated { it.toVirksomhetDto() }
            .runWithSession(session)
    }

    fun get(orgnr: Organisasjonsnummer): ArrangorDto? {
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

        return session.single(queryOf(selectHovedenhet, orgnr.value)) { it.toVirksomhetDto() }?.let { arrangor ->
            val underenheter = session.list(queryOf(selectUnderenheter, orgnr.value)) { it.toVirksomhetDto() }
            arrangor.copy(underenheter = underenheter.takeIf { it.isNotEmpty() })
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

        val arrangor = session.single(queryOf(query, id)) { it.toVirksomhetDto() }

        return requireNotNull(arrangor) {
            "Arrangør med id=$id finnes ikke"
        }
    }

    fun getHovedenhetById(id: UUID): ArrangorDto {
        val arrangor = getById(id)

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

        val underenheter = session.list(queryOf(queryForUnderenheter, arrangor.organisasjonsnummer.value)) {
            it.toVirksomhetDto()
        }

        return arrangor.copy(underenheter = underenheter)
    }

    fun delete(orgnr: String) {
        @Language("PostgreSQL")
        val query = """
            delete from arrangor where organisasjonsnummer = ?
        """.trimIndent()

        session.execute(queryOf(query, orgnr))
    }

    fun upsertKontaktperson(kontaktperson: ArrangorKontaktperson) {
        @Language("PostgreSQL")
        val query = """
            insert into arrangor_kontaktperson(id, arrangor_id, navn, telefon, epost, beskrivelse, ansvarlig_for)
            values (:id::uuid, :arrangor_id, :navn, :telefon, :epost, :beskrivelse, :ansvarligFor::arrangor_kontaktperson_ansvarlig_for_type[])
            on conflict (id) do update set
                navn                    = excluded.navn,
                arrangor_id             = excluded.arrangor_id,
                telefon                 = excluded.telefon,
                epost                   = excluded.epost,
                beskrivelse             = excluded.beskrivelse,
                ansvarlig_for           = excluded.ansvarlig_for::arrangor_kontaktperson_ansvarlig_for_type[]
        """.trimIndent()

        val params = mapOf(
            "id" to kontaktperson.id,
            "arrangor_id" to kontaktperson.arrangorId,
            "navn" to kontaktperson.navn,
            "telefon" to kontaktperson.telefon,
            "epost" to kontaktperson.epost,
            "beskrivelse" to kontaktperson.beskrivelse,
            "ansvarligFor" to kontaktperson.ansvarligFor?.let {
                session.createArrayOf("arrangor_kontaktperson_ansvarlig_for_type", it)
            },
        )

        session.execute(queryOf(query, params))
    }

    fun koblingerTilKontaktperson(kontaktpersonId: UUID): Pair<List<DokumentKoblingForKontaktperson>, List<DokumentKoblingForKontaktperson>> {
        @Language("PostgreSQL")
        val gjennomforingQuery = """
            select tg.navn, tg.id
            from gjennomforing_arrangor_kontaktperson join gjennomforing tg on gjennomforing_id = tg.id
            where arrangor_kontaktperson_id = ?
        """.trimIndent()

        val gjennomforinger = session.list(queryOf(gjennomforingQuery, kontaktpersonId)) {
            DokumentKoblingForKontaktperson(id = it.uuid("id"), navn = it.string("navn"))
        }

        @Language("PostgreSQL")
        val avtaleQuery = """
            select a.navn, a.id
            from avtale_arrangor_kontaktperson join avtale a on avtale_id = a.id
            where arrangor_kontaktperson_id = ?
        """.trimIndent()

        val avtaler = session.list(queryOf(avtaleQuery, kontaktpersonId)) {
            DokumentKoblingForKontaktperson(id = it.uuid("id"), navn = it.string("navn"))
        }

        return gjennomforinger to avtaler
    }

    fun deleteKontaktperson(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from arrangor_kontaktperson where id = ?
        """.trimIndent()

        session.execute(queryOf(query, id))
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
                beskrivelse,
                ansvarlig_for
            from arrangor_kontaktperson
            where arrangor_id = ?::uuid
        """.trimIndent()

        return session.list(queryOf(query, arrangorId)) { it.toArrangorKontaktperson() }
    }

    private fun Row.toVirksomhetDto() = ArrangorDto(
        id = uuid("id"),
        organisasjonsnummer = Organisasjonsnummer(string("organisasjonsnummer")),
        navn = string("navn"),
        overordnetEnhet = stringOrNull("overordnet_enhet")?.let { Organisasjonsnummer(it) },
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
        ansvarligFor = arrayOrNull<String>("ansvarlig_for")
            ?.map { ArrangorKontaktperson.AnsvarligFor.valueOf(it) }
            ?: emptyList(),
    )
}

@Serializable
data class DokumentKoblingForKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
