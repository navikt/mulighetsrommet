package no.nav.mulighetsrommet.api.arrangor.db

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKobling
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.arrangor.model.UtenlandskArrangor
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.UUID
import kotlin.String

class ArrangorQueries(private val session: Session) {
    private val underenheterLateralJoin = """
        left join lateral (
            select json_agg(
                json_build_object(
                    'id', id,
                    'organisasjonsnummer', organisasjonsnummer,
                    'overordnetEnhet', overordnet_enhet,
                    'organisasjonsform', organisasjonsform,
                    'navn', navn,
                    'slettetDato', slettet_dato,
                    'erUtenlandsk', er_utenlandsk_virksomhet
                )
            ) as underenheter_json
            from arrangor arrangor_underenhet
        where arrangor_underenhet.overordnet_enhet = arrangor.organisasjonsnummer) on true
    """

    /** Upserter kun enheten og tar ikke hensyn til underenheter */
    fun upsert(arrangor: ArrangorDto) {
        @Language("PostgreSQL")
        val query = """
            insert into arrangor(id, organisasjonsnummer, organisasjonsform, navn, overordnet_enhet, slettet_dato)
            values (:id, :organisasjonsnummer, :organisasjonsform, :navn, :overordnet_enhet, :slettet_dato)
            on conflict (id) do update set
                organisasjonsnummer = excluded.organisasjonsnummer,
                organisasjonsform = excluded.organisasjonsform,
                navn = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet,
                slettet_dato = excluded.slettet_dato
        """.trimIndent()

        val parameters = arrangor.run {
            mapOf(
                "id" to id,
                "organisasjonsnummer" to organisasjonsnummer.value,
                "organisasjonsform" to organisasjonsform,
                "navn" to navn,
                "overordnet_enhet" to overordnetEnhet?.value,
                "slettet_dato" to slettetDato,
            )
        }

        session.execute(queryOf(query, parameters))
    }

    fun getAll(
        kobling: ArrangorKobling? = null,
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

        val isRelatedToTiltak = when (kobling) {
            ArrangorKobling.AVTALE -> "id in (select arrangor_hovedenhet_id from avtale) and"
            ArrangorKobling.TILTAKSGJENNOMFORING -> "id in (select arrangor_id from gjennomforing) and"
            else -> ""
        }

        @Language("PostgreSQL")
        val query = """
            select distinct
                arrangor.id,
                arrangor.organisasjonsnummer,
                arrangor.organisasjonsform,
                arrangor.overordnet_enhet,
                arrangor.navn,
                arrangor.slettet_dato,
                arrangor.er_utenlandsk_virksomhet,
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
            .mapPaginated { it.toArrangorDtoUtenUnderenheter() }
            .runWithSession(session)
    }

    fun get(orgnr: Organisasjonsnummer): ArrangorDto? {
        @Language("PostgreSQL")
        val selectHovedenhet = """
            select
                id,
                organisasjonsnummer,
                organisasjonsform,
                overordnet_enhet,
                er_utenlandsk_virksomhet,
                navn,
                slettet_dato,
                underenheter_json
            from arrangor
                $underenheterLateralJoin
            where arrangor.organisasjonsnummer = ?
        """.trimIndent()

        return session.single(queryOf(selectHovedenhet, orgnr.value)) { it.toArrangorDtoMedUnderenheter() }
    }

    fun getByGjennomforingId(gjennomforingId: UUID): ArrangorDto? {
        @Language("PostgreSQL")
        val selectHovedenhet = """
            select
                arrangor.id,
                arrangor.organisasjonsnummer,
                arrangor.organisasjonsform,
                arrangor.overordnet_enhet,
                arrangor.er_utenlandsk_virksomhet,
                arrangor.navn,
                arrangor.slettet_dato,
                underenheter_json
            from gjennomforing
                inner join arrangor on arrangor.id = gjennomforing.arrangor_id
                $underenheterLateralJoin
            where gjennomforing.id = ?
        """.trimIndent()

        return session.single(queryOf(selectHovedenhet, gjennomforingId)) { it.toArrangorDtoMedUnderenheter() }
    }

    fun get(orgnr: List<Organisasjonsnummer>): List<ArrangorDto> {
        @Language("PostgreSQL")
        val selectHovedenhet = """
            select
                id,
                organisasjonsnummer,
                organisasjonsform,
                overordnet_enhet,
                er_utenlandsk_virksomhet,
                navn,
                slettet_dato,
                underenheter_json
            from arrangor
                $underenheterLateralJoin
            where arrangor.organisasjonsnummer = any(?)
        """.trimIndent()

        return session.list(queryOf(selectHovedenhet, session.createTextArray(orgnr.map { it.value }))) {
            it.toArrangorDtoMedUnderenheter()
        }
    }

    fun getById(id: UUID): ArrangorDto {
        @Language("PostgreSQL")
        val query = """
            select
                id,
                organisasjonsnummer,
                organisasjonsform,
                overordnet_enhet,
                navn,
                slettet_dato,
                er_utenlandsk_virksomhet
            from arrangor
            where id = ?::uuid
        """.trimIndent()

        val arrangor = session.single(queryOf(query, id)) { it.toArrangorDtoUtenUnderenheter() }

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
                organisasjonsform,
                overordnet_enhet,
                navn,
                slettet_dato,
                er_utenlandsk_virksomhet
            from arrangor
            where overordnet_enhet = ?
            order by navn
        """.trimIndent()

        val underenheter = session.list(queryOf(queryForUnderenheter, arrangor.organisasjonsnummer.value)) {
            it.toArrangorDtoUtenUnderenheter()
        }

        return arrangor.copy(underenheter = underenheter)
    }

    fun delete(orgnr: Organisasjonsnummer) {
        @Language("PostgreSQL")
        val query = """
            delete from arrangor where organisasjonsnummer = ?
        """.trimIndent()

        session.execute(queryOf(query, orgnr.value))
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
            "ansvarligFor" to kontaktperson.ansvarligFor.let { session.createArrayOfAnsvarligFor(it) },
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

    fun getUtenlandskArrangor(arrangorId: UUID): UtenlandskArrangor? {
        @Language("PostgreSQL")
        val query = """
            select
                bic,
                iban,
                adresse_gate_navn,
                adresse_by,
                adresse_post_nummer,
                adresse_land_kode,
                bank_navn
            from arrangor
                inner join arrangor_utenlandsk on arrangor_utenlandsk.id = arrangor.arrangor_utenlandsk_id
            where arrangor.id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, arrangorId)) { it.toUtenlandskArrangor() }
    }

    private fun Row.toUtenlandskArrangor() = UtenlandskArrangor(
        bic = string("bic"),
        iban = string("iban"),
        gateNavn = string("adresse_gate_navn"),
        by = string("adresse_by"),
        postNummer = string("adresse_post_nummer"),
        landKode = string("adresse_land_kode"),
        bankNavn = string("bank_navn"),
    )

    private fun Row.toArrangorDtoUtenUnderenheter() = ArrangorDto(
        id = uuid("id"),
        organisasjonsnummer = Organisasjonsnummer(string("organisasjonsnummer")),
        organisasjonsform = stringOrNull("organisasjonsform"),
        navn = string("navn"),
        overordnetEnhet = stringOrNull("overordnet_enhet")?.let { Organisasjonsnummer(it) },
        slettetDato = localDateOrNull("slettet_dato"),
        erUtenlandsk = boolean("er_utenlandsk_virksomhet"),
    )

    private fun Row.toArrangorDtoMedUnderenheter(): ArrangorDto {
        val underenheter = stringOrNull("underenheter_json")?.let {
            Json.decodeFromString<List<ArrangorDto>>(it)
        }
        return ArrangorDto(
            id = uuid("id"),
            organisasjonsnummer = Organisasjonsnummer(string("organisasjonsnummer")),
            organisasjonsform = stringOrNull("organisasjonsform"),
            navn = string("navn"),
            overordnetEnhet = stringOrNull("overordnet_enhet")?.let { Organisasjonsnummer(it) },
            slettetDato = localDateOrNull("slettet_dato"),
            underenheter = underenheter,
            erUtenlandsk = boolean("er_utenlandsk_virksomhet"),
        )
    }

    private fun Row.toArrangorKontaktperson() = ArrangorKontaktperson(
        id = uuid("id"),
        arrangorId = uuid("arrangor_id"),
        navn = string("navn"),
        telefon = stringOrNull("telefon"),
        epost = string("epost"),
        beskrivelse = stringOrNull("beskrivelse"),
        ansvarligFor = arrayOrNull<String>("ansvarlig_for")
            ?.map { ArrangorKontaktperson.Ansvar.valueOf(it) }
            ?: emptyList(),
    )
}

fun Session.createArrayOfAnsvarligFor(
    ansvarligFor: List<ArrangorKontaktperson.Ansvar>,
): Array = createArrayOf("arrangor_kontaktperson_ansvarlig_for_type", ansvarligFor)

@Serializable
data class DokumentKoblingForKontaktperson(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val navn: String,
)
