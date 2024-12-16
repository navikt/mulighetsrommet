package no.nav.mulighetsrommet.api.veilederflate

import kotlinx.serialization.json.Json
import kotliquery.Row
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.veilederflate.models.*
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.database.utils.DatabaseUtils.toFTSPrefixQuery
import no.nav.mulighetsrommet.domain.Tiltakskode
import no.nav.mulighetsrommet.domain.Tiltakskoder.isKursTiltak
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.Personopplysning
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingStatus
import org.intellij.lang.annotations.Language
import java.util.*

class VeilederflateTiltakRepository(private val db: Database) {
    fun get(id: UUID): VeilederflateTiltakGruppe? {
        @Language("PostgreSQL")
        val query = """
            select *
            from veilederflate_tiltak_view
            where id = :id::uuid
        """.trimIndent()

        return queryOf(query, mapOf("id" to id))
            .map { it.toVeilederflateTiltaksgjennomforing() }
            .asSingle
            .let { db.run(it) }
    }

    fun getAll(
        innsatsgruppe: Innsatsgruppe,
        brukersEnheter: List<String>,
        search: String? = null,
        apentForPamelding: Boolean? = null,
        sanityTiltakstypeIds: List<UUID>? = null,
        erSykmeldtMedArbeidsgiver: Boolean = false,
    ): List<VeilederflateTiltakGruppe> {
        val parameters = mapOf(
            "innsatsgruppe" to innsatsgruppe.name,
            "brukers_enheter" to db.createTextArray(brukersEnheter),
            "search" to search?.toFTSPrefixQuery(),
            "apent_for_pamelding" to apentForPamelding,
            "sanityTiltakstypeIds" to sanityTiltakstypeIds?.let { db.createUuidArray(it) },
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

        return queryOf(query, parameters)
            .map { it.toVeilederflateTiltaksgjennomforing() }
            .asList
            .let { db.run(it) }
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

        val tiltakstypeNavn = string("tiltakstype_navn")
        val navn = string("navn")
        val tittel = tiltakstypeNavn
        val underTittel = navn

        return VeilederflateTiltakGruppe(
            id = uuid("id"),
            tiltakstype = VeilederflateTiltakstype(
                sanityId = uuid("tiltakstype_sanity_id").toString(),
                navn = tiltakstypeNavn,
                tiltakskode = stringOrNull("tiltakstype_tiltakskode")?.let { Tiltakskode.valueOf(it) },
            ),
            tittel = tittel,
            underTittel = underTittel,
            stedForGjennomforing = stringOrNull("sted_for_gjennomforing"),
            apentForPamelding = boolean("apent_for_pamelding"),
            tiltaksnummer = stringOrNull("tiltaksnummer"),
            oppstart = TiltaksgjennomforingOppstartstype.valueOf(string("oppstart")),
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
            status = TiltaksgjennomforingStatus.valueOf(string("status")),
        )
    }
}
