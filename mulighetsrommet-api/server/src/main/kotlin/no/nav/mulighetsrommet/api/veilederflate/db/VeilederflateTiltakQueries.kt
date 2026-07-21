package no.nav.mulighetsrommet.api.veilederflate.db

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingAvtale
import no.nav.mulighetsrommet.api.veilederflate.models.EstimertVentetid
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateArrangor
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateArrangorKontaktperson
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateKontaktinfo
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateKontaktinfoTiltaksansvarlig
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateNavEnhet
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppeStatus
import no.nav.mulighetsrommet.database.createArrayOfValue
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.UUID

class VeilederflateTiltakQueries(private val session: Session) {

    fun get(id: UUID): Tiltaksgjennomforing? {
        @Language("PostgreSQL")
        val query = """
            select *
            from view_veilederflate_tiltak
            where id = ?::uuid
        """.trimIndent()

        return session.single(queryOf(query, id)) { it.toTiltaksgjennomforing() }
    }

    fun getAll(
        innsatsgruppe: Innsatsgruppe,
        brukersEnheter: List<NavEnhetNummer>,
        search: String? = null,
        apentForPamelding: Boolean? = null,
        tiltakskoder: List<Tiltakskode>? = null,
        erSykmeldtMedArbeidsgiver: Boolean = false,
    ): List<Tiltaksgjennomforing> = with(session) {
        val parameters = mapOf(
            "innsatsgruppe" to innsatsgruppe.name,
            "brukers_enheter" to createArrayOfValue(brukersEnheter) { it.value },
            "search" to search?.toFTSPrefixQuery(),
            "apent_for_pamelding" to apentForPamelding,
            "tiltakskoder" to tiltakskoder?.let { createTextArray(it) },
            "er_sykmeldt_med_arbeidsgiver" to erSykmeldtMedArbeidsgiver,
        )

        @Language("PostgreSQL")
        val query = """
            select *
            from view_veilederflate_tiltak
            where publisert
              and (
                (:innsatsgruppe::innsatsgruppe = any(tiltakstype_innsatsgrupper))
                or (
                    :er_sykmeldt_med_arbeidsgiver = true
                    and tiltakstype_tiltakskode = 'ARBEIDSRETTET_REHABILITERING'
                    and :innsatsgruppe::innsatsgruppe = 'TRENGER_VEILEDNING'
                   )
              )
              and exists(select true
                          from jsonb_array_elements(nav_enheter_json) as VeilederflateNavEnheter
                          where VeilederflateNavEnheter ->> 'enhetsnummer' = any(:brukers_enheter)
                          )
              and (:search::text is null or fts @@ to_tsquery('norwegian', :search))
              and (:tiltakskoder::text[] is null or tiltakstype_tiltakskode = any(:tiltakskoder))
              and (:apent_for_pamelding::boolean is null or apent_for_pamelding = :apent_for_pamelding)
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toTiltaksgjennomforing() }
    }

    fun getAllTiltakDokument(
        brukersEnheter: List<NavEnhetNummer>,
        tiltakskoder: List<Tiltakskode>? = null,
    ): List<VeilederflateTiltakDokument> = with(session) {
        val parameters = mapOf(
            "brukers_enheter" to createArrayOfValue(brukersEnheter) { it.value },
            "tiltakskoder" to tiltakskoder?.let { createTextArray(it) },
        )

        @Language("PostgreSQL")
        val query = """
        select *
        from view_tiltak_dokument
        where publisert
            and (:tiltakskoder::text[] is null or tiltakstype_tiltakskode = any(:tiltakskoder))
            and exists(select true
            from jsonb_array_elements(nav_enheter_json) as nav_enhet
                where nav_enhet ->> 'enhetsnummer' = any(:brukers_enheter))
        """.trimIndent()

        return list(queryOf(query, parameters)) { toVeilederflateTiltakDokument(it) }
    }

    fun getTiltakDokument(id: UUID): VeilederflateTiltakDokument? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select * from view_tiltak_dokument
            where id = ?::uuid or sanity_id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id, id)) { toVeilederflateTiltakDokument(it) }
    }
}

private fun Row.toTiltaksgjennomforing(): Tiltaksgjennomforing {
    val navEnheter = stringOrNull("nav_enheter_json")
        ?.let { Json.decodeFromString<List<VeilederflateNavEnhet>>(it) }
        ?: emptyList()
    val fylker = navEnheter.filter { it.type == Norg2Type.FYLKE }.map { it.enhetsnummer }
    val enheter = navEnheter.filter { it.type != Norg2Type.FYLKE }.map { it.enhetsnummer }
    val personopplysningerSomKanBehandles = stringOrNull("personopplysninger_json")
        ?.let { Json.decodeFromString<List<Personopplysning>>(it) }
        ?: emptyList()
    val tiltaksansvarlige = stringOrNull("nav_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<VeilederflateKontaktinfoTiltaksansvarlig>>(it) }
        ?: emptyList()
    val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<VeilederflateArrangorKontaktperson>>(it) }
        ?: emptyList()
    val stengt = stringOrNull("stengt_perioder_json")
        ?.let { Json.decodeFromString<List<GjennomforingAvtale.StengtPeriode>>(it) }
        ?: emptyList()

    val status = GjennomforingStatusType.valueOf(string("status"))

    return Tiltaksgjennomforing(
        id = uuid("id"),
        tiltakskode = Tiltakskode.valueOf(string("tiltakstype_tiltakskode")),
        navn = string("navn"),
        apentForPamelding = boolean("apent_for_pamelding"),
        tiltaksnummer = stringOrNull("arena_tiltaksnummer"),
        oppstart = GjennomforingOppstartstype.valueOf(string("oppstart")),
        oppstartsdato = localDate("start_dato"),
        sluttdato = localDateOrNull("slutt_dato"),
        kontaktinfo = VeilederflateKontaktinfo(
            tiltaksansvarlige = tiltaksansvarlige,
        ),
        arrangor = VeilederflateArrangor(
            organisasjonsnummer = string("arrangor_organisasjonsnummer"),
            selskapsnavn = stringOrNull("arrangor_navn"),
            kontaktpersoner = arrangorKontaktpersoner,
        ),
        fylker = fylker,
        enheter = enheter,
        beskrivelse = stringOrNull("beskrivelse"),
        faneinnhold = stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        estimertVentetid = intOrNull("estimert_ventetid_verdi")?.let {
            EstimertVentetid(
                verdi = int("estimert_ventetid_verdi"),
                enhet = string("estimert_ventetid_enhet"),
            )
        },
        personvernBekreftet = boolean("personvern_bekreftet"),
        personopplysningerSomKanBehandles = personopplysningerSomKanBehandles,
        oppmoteSted = stringOrNull("oppmote_sted"),
        status = VeilederflateTiltakGruppeStatus(
            type = status,
            beskrivelse = status.beskrivelse,
        ),
        lopenummer = string("lopenummer"),
        stengt = stengt,
    )
}

private fun toVeilederflateTiltakDokument(row: Row): VeilederflateTiltakDokument {
    val arrangorId = row.uuidOrNull("arrangor_id")

    val navEnheter = row.stringOrNull("nav_enheter_json")
        ?.let { Json.decodeFromString<List<VeilederflateTiltakDokument.NavEnhet>>(it) }
        ?: emptyList()

    return VeilederflateTiltakDokument(
        id = row.uuid("id"),
        sanityId = row.uuidOrNull("sanity_id"),
        navn = row.string("navn"),
        beskrivelse = row.stringOrNull("beskrivelse"),
        faneinnhold = row.stringOrNull("faneinnhold")?.let { Json.decodeFromString(it) },
        tiltaksnummer = row.stringOrNull("tiltaksnummer"),
        tiltakskode = Tiltakskode.valueOf(row.string("tiltakstype_tiltakskode")),
        stedForGjennomforing = row.stringOrNull("sted_for_gjennomforing"),
        navEnheter = navEnheter,
        kontaktpersoner = row.stringOrNull("kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<VeilederflateTiltakDokument.Kontaktperson>>(it) }
            ?: emptyList(),
        arrangor = arrangorId?.let {
            VeilederflateTiltakDokument.Arrangor(
                id = it,
                navn = row.string("arrangor_navn"),
                organisasjonsnummer = row.string("arrangor_organisasjonsnummer"),
            )
        },
        arrangorKontaktpersoner = row.stringOrNull("arrangor_kontaktpersoner_json")
            ?.let { Json.decodeFromString<List<VeilederflateTiltakDokument.ArrangorKontaktperson>>(it) }
            ?: emptyList(),
    )
}
