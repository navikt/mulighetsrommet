package no.nav.mulighetsrommet.api.persistence.arrangor.db

import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.admin.arrangor.ArrangorDto
import no.nav.mulighetsrommet.admin.arrangor.ArrangorHovedenhetDto
import no.nav.mulighetsrommet.admin.arrangor.ArrangorKobling
import no.nav.mulighetsrommet.admin.arrangor.ArrangorQueryHandler
import no.nav.mulighetsrommet.admin.arrangor.DokumentKoblingForKontaktperson
import no.nav.mulighetsrommet.admin.arrangor.medUnderenheter
import no.nav.mulighetsrommet.api.domain.arrangor.Arrangor
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.arrangor.ArrangorRepository
import no.nav.mulighetsrommet.api.domain.arrangor.Betalingsinformasjon
import no.nav.mulighetsrommet.database.requireSingle
import no.nav.mulighetsrommet.database.utils.PaginatedResult
import no.nav.mulighetsrommet.database.utils.Pagination
import no.nav.mulighetsrommet.database.utils.mapPaginated
import no.nav.mulighetsrommet.database.utils.parameters
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import org.intellij.lang.annotations.Language
import java.sql.Array
import java.util.UUID

class ArrangorQueries(private val session: Session) : ArrangorRepository, ArrangorQueryHandler {
    /** Upserter enheten (tar ikke hensyn til underenheter) */
    override fun save(arrangor: Arrangor) {
        @Language("PostgreSQL")
        val query = """
            insert into arrangor(id, organisasjonsnummer, organisasjonsform, navn, overordnet_enhet, slettet_dato, er_utenlandsk_virksomhet)
            values (:id, :organisasjonsnummer, :organisasjonsform, :navn, :overordnet_enhet, :slettet_dato, :er_utenlandsk_virksomhet)
            on conflict (id) do update set
                organisasjonsnummer = excluded.organisasjonsnummer,
                organisasjonsform = excluded.organisasjonsform,
                navn = excluded.navn,
                overordnet_enhet = excluded.overordnet_enhet,
                slettet_dato = excluded.slettet_dato,
                er_utenlandsk_virksomhet = excluded.er_utenlandsk_virksomhet
        """.trimIndent()

        val parameters = arrangor.run {
            mapOf(
                "id" to id,
                "organisasjonsnummer" to organisasjonsnummer.value,
                "organisasjonsform" to organisasjonsform,
                "navn" to navn,
                "overordnet_enhet" to overordnetEnhet?.value,
                "slettet_dato" to slettetDato,
                "er_utenlandsk_virksomhet" to (this is Arrangor.Utenlandsk),
            )
        }

        session.execute(queryOf(query, parameters))

        syncKontaktpersoner(arrangor.id, arrangor.kontaktpersoner)

        when (arrangor) {
            is Arrangor.Utenlandsk -> saveUtenlandskInformasjon(arrangor)
            is Arrangor.Norsk -> Unit
        }
    }

    private fun saveUtenlandskInformasjon(arrangor: Arrangor.Utenlandsk) {
        val betalingsinformasjon = arrangor.betalingsinformasjon
        val adresse = arrangor.adresse

        if (betalingsinformasjon == null || adresse == null) {
            return
        }

        @Language("PostgreSQL")
        val query = """
            insert into arrangor_utenlandsk(arrangor_id, bic, iban, bank_navn, adresse_gate_navn, adresse_by, adresse_post_nummer, adresse_land_kode)
            values (:arrangor_id, :bic, :iban, :bank_navn, :adresse_gate_navn, :adresse_by, :adresse_post_nummer, :adresse_land_kode)
            on conflict (arrangor_id) do update set
                bic = excluded.bic,
                iban = excluded.iban,
                bank_navn = excluded.bank_navn,
                adresse_gate_navn = excluded.adresse_gate_navn,
                adresse_by = excluded.adresse_by,
                adresse_post_nummer = excluded.adresse_post_nummer,
                adresse_land_kode = excluded.adresse_land_kode
        """.trimIndent()

        val parameters = mapOf(
            "arrangor_id" to arrangor.id,
            "bic" to betalingsinformasjon.bic,
            "iban" to betalingsinformasjon.iban,
            "bank_navn" to betalingsinformasjon.bankNavn,
            "adresse_gate_navn" to adresse.gateNavn,
            "adresse_by" to adresse.by,
            "adresse_post_nummer" to adresse.postNummer,
            "adresse_land_kode" to adresse.landKode,
        )

        session.execute(queryOf(query, parameters))
    }

    private fun syncKontaktpersoner(arrangorId: UUID, kontaktpersoner: List<ArrangorKontaktperson>) {
        val eksisterendeIder = getKontaktpersoner(arrangorId).map { it.id }.toSet()
        val nyeIder = kontaktpersoner.map { it.id }.toSet()
        kontaktpersoner.forEach { upsertKontaktperson(it) }
        (eksisterendeIder - nyeIder).forEach { deleteKontaktperson(it) }
    }

    override fun getAll(
        kobling: ArrangorKobling?,
        sok: String?,
        overordnetEnhetOrgnr: Organisasjonsnummer?,
        slettet: Boolean?,
        utenlandsk: Boolean?,
        pagination: Pagination,
        sortering: String?,
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

    override fun getById(id: UUID): ArrangorDto {
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

    override fun getHovedenhetById(id: UUID): ArrangorHovedenhetDto {
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

        return arrangor.medUnderenheter(underenheter)
    }

    override fun get(id: UUID): Arrangor {
        @Language("PostgreSQL")
        val query = """
            select
                arrangor.id,
                arrangor.organisasjonsnummer,
                arrangor.organisasjonsform,
                arrangor.overordnet_enhet,
                arrangor.navn,
                arrangor.slettet_dato,
                arrangor.er_utenlandsk_virksomhet,
                arrangor_utenlandsk.bic,
                arrangor_utenlandsk.iban,
                arrangor_utenlandsk.bank_navn,
                arrangor_utenlandsk.adresse_gate_navn,
                arrangor_utenlandsk.adresse_by,
                arrangor_utenlandsk.adresse_post_nummer,
                arrangor_utenlandsk.adresse_land_kode
            from arrangor
                left join arrangor_utenlandsk on arrangor_utenlandsk.arrangor_id = arrangor.id
            where arrangor.id = ?::uuid
        """.trimIndent()

        val arrangor = session.requireSingle(queryOf(query, id)) { it.toArrangor() }

        return arrangor.medKontaktpersoner(getKontaktpersoner(id))
    }

    override fun getByOrganisasjonsnummer(orgnr: Organisasjonsnummer): Arrangor? {
        @Language("PostgreSQL")
        val query = """
            select
                arrangor.id,
                arrangor.organisasjonsnummer,
                arrangor.organisasjonsform,
                arrangor.overordnet_enhet,
                arrangor.navn,
                arrangor.slettet_dato,
                arrangor.er_utenlandsk_virksomhet,
                arrangor_utenlandsk.bic,
                arrangor_utenlandsk.iban,
                arrangor_utenlandsk.bank_navn,
                arrangor_utenlandsk.adresse_gate_navn,
                arrangor_utenlandsk.adresse_by,
                arrangor_utenlandsk.adresse_post_nummer,
                arrangor_utenlandsk.adresse_land_kode
            from arrangor
                left join arrangor_utenlandsk on arrangor_utenlandsk.arrangor_id = arrangor.id
            where arrangor.organisasjonsnummer = ?
        """.trimIndent()

        val arrangor = session.single(queryOf(query, orgnr.value)) { it.toArrangor() } ?: return null

        return arrangor.medKontaktpersoner(getKontaktpersoner(arrangor.id))
    }

    override fun delete(orgnr: Organisasjonsnummer) {
        @Language("PostgreSQL")
        val query = """
            delete from arrangor where organisasjonsnummer = ?
        """.trimIndent()

        session.execute(queryOf(query, orgnr.value))
    }

    private fun upsertKontaktperson(kontaktperson: ArrangorKontaktperson) {
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

    override fun koblingerTilKontaktperson(
        kontaktpersonId: UUID,
    ): Pair<List<DokumentKoblingForKontaktperson>, List<DokumentKoblingForKontaktperson>> {
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

    private fun deleteKontaktperson(id: UUID) {
        @Language("PostgreSQL")
        val query = """
            delete from arrangor_kontaktperson where id = ?
        """.trimIndent()

        session.execute(queryOf(query, id))
    }

    override fun getKontaktpersoner(arrangorId: UUID): List<ArrangorKontaktperson> {
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

    private fun Row.toArrangor(): Arrangor {
        val id = uuid("id")
        val organisasjonsnummer = Organisasjonsnummer(string("organisasjonsnummer"))
        val organisasjonsform = stringOrNull("organisasjonsform")
        val navn = string("navn")
        val overordnetEnhet = stringOrNull("overordnet_enhet")?.let { Organisasjonsnummer(it) }
        val slettetDato = localDateOrNull("slettet_dato")

        if (!boolean("er_utenlandsk_virksomhet")) {
            return Arrangor.Norsk(
                id = id,
                organisasjonsnummer = organisasjonsnummer,
                organisasjonsform = organisasjonsform,
                navn = navn,
                overordnetEnhet = overordnetEnhet,
                slettetDato = slettetDato,
            )
        }

        val betalingsinformasjon = stringOrNull("bic")?.let {
            Betalingsinformasjon.IBan(
                bic = it,
                iban = string("iban"),
                bankNavn = string("bank_navn"),
                bankLandKode = string("adresse_land_kode"),
            )
        }
        val adresse = stringOrNull("adresse_gate_navn")?.let {
            Arrangor.Utenlandsk.Adresse(
                gateNavn = it,
                by = string("adresse_by"),
                postNummer = string("adresse_post_nummer"),
                landKode = string("adresse_land_kode"),
            )
        }

        return Arrangor.Utenlandsk(
            id = id,
            organisasjonsnummer = organisasjonsnummer,
            organisasjonsform = organisasjonsform,
            navn = navn,
            overordnetEnhet = overordnetEnhet,
            slettetDato = slettetDato,
            betalingsinformasjon = betalingsinformasjon,
            adresse = adresse,
        )
    }

    private fun Row.toArrangorDtoUtenUnderenheter() = ArrangorDto(
        id = uuid("id"),
        organisasjonsnummer = Organisasjonsnummer(string("organisasjonsnummer")),
        organisasjonsform = stringOrNull("organisasjonsform"),
        navn = string("navn"),
        overordnetEnhet = stringOrNull("overordnet_enhet")?.let { Organisasjonsnummer(it) },
        slettetDato = localDateOrNull("slettet_dato"),
        erUtenlandsk = boolean("er_utenlandsk_virksomhet"),
    )

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
