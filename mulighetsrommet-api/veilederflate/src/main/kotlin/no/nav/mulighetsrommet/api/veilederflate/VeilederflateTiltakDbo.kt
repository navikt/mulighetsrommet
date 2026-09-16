package no.nav.mulighetsrommet.api.veilederflate

import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serializers.LocalDateSerializer
import java.time.LocalDate
import java.util.UUID

data class VeilederflateTiltakDbo(
    val id: UUID,
    val tiltakskode: Tiltakskode,
    val navn: String,
    val status: VeilederflateTiltakGruppeStatus,
    val tiltaksnummer: String?,
    val apentForPamelding: Boolean,
    val oppstartsdato: LocalDate,
    val sluttdato: LocalDate?,
    val oppstart: GjennomforingOppstartstype,
    val oppmoteSted: String?,
    val arrangor: VeilederflateArrangor,
    val kontaktinfo: VeilederflateKontaktinfo,
    val beskrivelse: String?,
    val faneinnhold: Faneinnhold?,
    val fylker: List<NavEnhetNummer>,
    val enheter: List<NavEnhetNummer>,
    val estimertVentetid: EstimertVentetid?,
    val personvernBekreftet: Boolean,
    val personopplysningerSomKanBehandles: List<Personopplysning>,
    val lopenummer: String,
    val stengt: List<StengtPeriode>,
) {
    @Serializable
    data class StengtPeriode(
        val id: Int,
        @Serializable(with = LocalDateSerializer::class)
        val start: LocalDate,
        @Serializable(with = LocalDateSerializer::class)
        val slutt: LocalDate,
        val beskrivelse: String,
    )
}
