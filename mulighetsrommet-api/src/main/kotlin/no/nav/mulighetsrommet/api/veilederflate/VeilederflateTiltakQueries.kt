package no.nav.mulighetsrommet.api.veilederflate

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.veilederflate.models.*
import no.nav.mulighetsrommet.database.createTextArray
import no.nav.mulighetsrommet.database.createUuidArray
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingStatus
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Tiltakskode
import org.intellij.lang.annotations.Language
import java.util.*

class VeilederflateTiltakQueries(private val session: Session) {

    fun get(id: UUID): VeilederflateTiltakGruppe? = with(session) {
        @Language("PostgreSQL")
        val query = """
            select *
            from veilederflate_tiltak_view
            where id = ?::uuid
        """.trimIndent()

        return single(queryOf(query, id)) { it.toVeilederflateTiltaksgjennomforing() }
    }

    fun getAll(
        innsatsgruppe: Innsatsgruppe,
        brukersEnheter: List<String>,
        search: String? = null,
        apentForPamelding: Boolean? = null,
        sanityTiltakstypeIds: List<UUID>? = null,
        erSykmeldtMedArbeidsgiver: Boolean = false,
    ): List<VeilederflateTiltakGruppe> = with(session) {
        val parameters = mapOf(
            "innsatsgruppe" to innsatsgruppe.name,
            "brukers_enheter" to createTextArray(brukersEnheter),
            "search" to search?.toFTSPrefixQuery(),
            "apent_for_pamelding" to apentForPamelding,
            "sanityTiltakstypeIds" to sanityTiltakstypeIds?.let { createUuidArray(it) },
            "er_sykmeldt_med_arbeidsgiver" to erSykmeldtMedArbeidsgiver,
        )

        @Language("PostgreSQL")
        val query = """
            select *
            from veilederflate_tiltak_view
            where publisert
              and (
                (:innsatsgruppe::innsatsgruppe = any(tiltakstype_innsatsgrupper))
                or (
                    :er_sykmeldt_med_arbeidsgiver = true
                    and tiltakstype_tiltakskode = 'ARBEIDSRETTET_REHABILITERING'
                    and :innsatsgruppe::innsatsgruppe = 'SITUASJONSBESTEMT_INNSATS'
                   )
              )
              and nav_enheter && :brukers_enheter
              and (:search::text is null or fts @@ to_tsquery('norwegian', :search))
              and (:sanityTiltakstypeIds::uuid[] is null or tiltakstype_sanity_id = any(:sanityTiltakstypeIds))
              and (:apent_for_pamelding::boolean is null or apent_for_pamelding = :apent_for_pamelding)
        """.trimIndent()

        return list(queryOf(query, parameters)) { it.toVeilederflateTiltaksgjennomforing() }
    }
}

private fun Row.toVeilederflateTiltaksgjennomforing(): VeilederflateTiltakGruppe {
    val navEnheter = arrayOrNull<String>("nav_enheter")?.asList() ?: emptyList()
    val personopplysningerSomKanBehandles = arrayOrNull<String>("personopplysninger_som_kan_behandles")
        ?.asList()
        ?.map { Personopplysning.valueOf(it).toPersonopplysningData() }
        ?: emptyList()
    val tiltaksansvarlige = stringOrNull("nav_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<VeilederflateKontaktinfoTiltaksansvarlig>>(it) }
        ?: emptyList()
    val arrangorKontaktpersoner = stringOrNull("arrangor_kontaktpersoner_json")
        ?.let { Json.decodeFromString<List<VeilederflateArrangorKontaktperson>>(it) }
        ?: emptyList()

    return VeilederflateTiltakGruppe(
        id = uuid("id"),
        tiltakstype = VeilederflateTiltakstype(
            sanityId = uuid("tiltakstype_sanity_id").toString(),
            navn = string("tiltakstype_navn"),
            tiltakskode = stringOrNull("tiltakstype_tiltakskode")?.let { Tiltakskode.valueOf(it) },
        ),
        navn = string("navn"),
        stedForGjennomforing = stringOrNull("sted_for_gjennomforing"),
        apentForPamelding = boolean("apent_for_pamelding"),
        tiltaksnummer = stringOrNull("tiltaksnummer"),
        oppstart = GjennomforingOppstartstype.valueOf(string("oppstart")),
        oppstartsdato = localDate("start_dato"),
        sluttdato = localDateOrNull("slutt_dato"),
        kontaktinfo = VeilederflateKontaktinfo(
            tiltaksansvarlige = tiltaksansvarlige,
        ),
        arrangor = VeilederflateArrangor(
            arrangorId = uuid("arrangor_id"),
            organisasjonsnummer = string("arrangor_organisasjonsnummer"),
            selskapsnavn = stringOrNull("arrangor_navn"),
            kontaktpersoner = arrangorKontaktpersoner,
        ),
        fylke = string("nav_region"),
        enheter = navEnheter,
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
        status = GjennomforingStatus.valueOf(string("status")),
    )
}
