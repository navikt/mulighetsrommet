package no.nav.mulighetsrommet.api.veilederflate.db

import no.nav.mulighetsrommet.api.veilederflate.models.EstimertVentetid
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateArrangor
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateKontaktinfo
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakGruppeStatus
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.PersonopplysningData
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

data class Tiltaksgjennomforing(
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
    val personopplysningerSomKanBehandles: List<PersonopplysningData>,
)
