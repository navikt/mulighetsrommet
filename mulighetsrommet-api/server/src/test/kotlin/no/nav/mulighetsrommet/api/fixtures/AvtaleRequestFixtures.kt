package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.avtale.api.AmoKategoriseringRequest
import no.nav.mulighetsrommet.api.avtale.api.DetaljerRequest
import no.nav.mulighetsrommet.api.avtale.api.OpprettAvtaleRequest
import no.nav.mulighetsrommet.api.avtale.api.PersonvernRequest
import no.nav.mulighetsrommet.api.avtale.api.VeilederinfoRequest
import no.nav.mulighetsrommet.api.avtale.model.AvtaltSatsRequest
import no.nav.mulighetsrommet.api.avtale.model.PrismodellRequest
import no.nav.mulighetsrommet.api.domain.tiltak.Opsjonsmodell
import no.nav.mulighetsrommet.api.domain.tiltak.OpsjonsmodellType
import no.nav.mulighetsrommet.api.domain.tiltak.Prismodell
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.SakarkivNummer
import no.nav.mulighetsrommet.model.Tiltakskode
import java.time.LocalDate
import java.util.UUID

fun createAvtaleRequest(
    tiltakskode: Tiltakskode,
    avtaletype: Avtaletype = Avtaletype.RAMMEAVTALE,
    arrangor: DetaljerRequest.Arrangor = DetaljerRequest.Arrangor(
        hovedenhet = ArrangorFixtures.hovedenhet.organisasjonsnummer,
        underenheter = listOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
        kontaktpersoner = emptyList(),
    ),
    administratorer: List<NavIdent> = listOf(NavAnsattFixture.DonaldDuck.navIdent),
    prismodell: List<Prismodell> = listOf(PrismodellFixtures.AnnenAvtaltPris),
    opsjonsmodell: Opsjonsmodell = Opsjonsmodell(OpsjonsmodellType.TO_PLUSS_EN, LocalDate.now().plusYears(3)),
    amo: AmoKategoriseringRequest? = null,
): OpprettAvtaleRequest {
    return OpprettAvtaleRequest(
        id = UUID.randomUUID(),
        detaljer = DetaljerRequest(
            navn = "Avtale",
            tiltakskode = tiltakskode,
            arrangor = arrangor,
            sakarkivNummer = SakarkivNummer("24/1234"),
            startDato = LocalDate.now().minusDays(1),
            sluttDato = LocalDate.now().plusMonths(1),
            administratorer = administratorer,
            avtaletype = avtaletype,
            amoKategorisering = amo,
            opsjonsmodell = opsjonsmodell,
            utdanningslop = null,
        ),
        veilederinformasjon = VeilederinfoRequest(
            navEnheter = listOf(NavEnhetNummer("0400"), NavEnhetNummer("0502")),
            beskrivelse = null,
            faneinnhold = null,
        ),
        personvern = PersonvernRequest(
            personopplysninger = emptyList(),
            personvernBekreftet = false,
            annetChecked = false,
            annetBeskrivelse = null,
        ),
        prismodeller = prismodell.map { it.toPrismodellRequest() },
    )
}

private fun Prismodell.toPrismodellRequest(): PrismodellRequest = PrismodellRequest(
    id = id,
    type = type,
    valuta = valuta,
    prisbetingelser = prisbetingelser(),
    satser = satser().map { AvtaltSatsRequest(it.gjelderFra, it.sats.belop) },
    tilsagnPerDeltaker = (this as? Prismodell.AnnenAvtaltPris)?.tilsagnPerDeltaker ?: false,
)
