package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.EnhetSlug
import no.nav.mulighetsrommet.api.domain.dto.FylkeRef
import no.nav.mulighetsrommet.api.domain.dto.SanityEnhet
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.utils.NavEnhetUtils
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

val NAV_EGNE_ANSATTE_TIL_FYLKE_MAP = mapOf(
    // NAV egne ansatte Vestfold og Telemark
    "0883" to "0800",
    // NAV egne ansatte Vestland
    "1283" to "1200",
    // NAV egne ansatte Troms og Finnmark
    "1983" to "1900",
    // NAV egne ansatte Oslo
    "0383" to "0300",
    // NAV egne ansatte Rogaland
    "1183" to "1100",
    // NAV egne ansatte Møre og Romsdal
    "1583" to "1500",
    // NAV egne ansatte Vest-Viken
    "0683" to "0600",
    // NAV egne ansatte Agder
    "1083" to "1000",
    // NAV egne ansatte Nordland
    "1883" to "1800",
    // NAV egne ansatte Øst-Viken
    "0283" to "0200",
    // NAV egne ansatte Innlandet
    "0483" to "0400",
    // NAV egne ansatte Trøndelag
    "1683" to "5700",
)

class NavEnheterSyncService(
    private val norg2Client: Norg2Client,
    private val sanityService: SanityService,
    private val enhetRepository: NavEnhetRepository,
    private val slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun synkroniserEnheter() {
        val enheter = norg2Client.hentEnheter()

        logger.info("Hentet ${enheter.size} enheter fra NORG2")

        lagreEnheter(enheter)

        val enheterToSanity = utledEnheterTilSanity(enheter)
        val response = sanityService.createSanityEnheter(enheterToSanity)

        if (response.status != HttpStatusCode.OK) {
            logger.error("Klarte ikke opprette enheter i sanity: ${response.status}")
            slackNotifier.sendMessage("Klarte ikke oppdatere enheter fra NORG til Sanity. Statuskode: ${response.status.value}. Dette må sees på av en utvikler.")
        }
    }

    private fun lagreEnheter(enheter: List<Norg2Response>) {
        logger.info("Lagrer ${enheter.size} enheter til database")

        enheter.forEach {
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = it.enhet.navn,
                    enhetsnummer = it.enhet.enhetNr,
                    status = NavEnhetStatus.valueOf(it.enhet.status.name),
                    type = Norg2Type.valueOf(it.enhet.type.name),
                    overordnetEnhet = it.overordnetEnhet ?: tryResolveOverordnetEnhet(it.enhet),
                ),
            )
        }
    }

    fun utledEnheterTilSanity(enheter: List<Norg2Response>): List<SanityEnhet> {
        val relevanteEnheterMedJustertOverordnetEnhet = enheter
            .filter { isRelevantEnhetForSanity(it) }
            .map {
                val overordnetEnhet = it.overordnetEnhet ?: tryResolveOverordnetEnhet(it.enhet)
                it.copy(overordnetEnhet = overordnetEnhet)
            }

        val fylker = relevanteEnheterMedJustertOverordnetEnhet
            .filter { it.enhet.type == Norg2Type.FYLKE }

        return fylker.flatMap { fylke ->
            val underliggendeEnheter = relevanteEnheterMedJustertOverordnetEnhet
                .filter { it.overordnetEnhet == fylke.enhet.enhetNr }
                .map { toSanityEnhet(it.enhet, fylke.enhet) }

            listOf(toSanityEnhet(fylke.enhet)) + underliggendeEnheter
        }
    }

    private fun isRelevantEnhetForSanity(it: Norg2Response): Boolean {
        return NavEnhetUtils.isRelevantEnhetStatus(it.enhet.status) && NavEnhetUtils.isRelevantEnhetType(it.enhet.type)
    }

    private fun toSanityEnhet(enhet: Norg2EnhetDto, fylke: Norg2EnhetDto? = null): SanityEnhet {
        var fylkeTilEnhet: FylkeRef? = null

        if (fylke != null) {
            fylkeTilEnhet = FylkeRef(
                _type = "reference",
                _ref = NavEnhetUtils.toEnhetId(fylke),
                _key = fylke.enhetNr,
            )
        }

        return SanityEnhet(
            _id = NavEnhetUtils.toEnhetId(enhet),
            _type = "enhet",
            navn = enhet.navn,
            nummer = EnhetSlug(
                _type = "slug",
                current = enhet.enhetNr,
            ),
            type = NavEnhetUtils.toType(enhet.type.name),
            status = NavEnhetUtils.toStatus(enhet.status.name),
            fylke = fylkeTilEnhet,
        )
    }

    private fun tryResolveOverordnetEnhet(enhet: Norg2EnhetDto): String? {
        if (!NavEnhetUtils.isRelevantEnhetStatus(enhet.status) ||
            !listOf(Norg2Type.ALS, Norg2Type.TILTAK, Norg2Type.KO).contains(enhet.type)
        ) {
            return null
        }

        val spesialEnheterTilFylkeMap = mapOf(
            // Vestland
            "1291" to "1200",
            // Øst-Viken
            "0291" to "0200",
            // Møre og Romsdal,
            "1591" to "1500",
            // Nordland
            "1891" to "1800",
            // Innlandet
            "0491" to "0400",
            // Vest-Viken,
            "0691" to "0600",
            // Vestfold og Telemark
            "0891" to "0800",
            // Agder,
            "1091" to "1000",
            // Troms og Finnmark
            "1991" to "1900",
            // Trøndelag,
            "5772" to "5700",
            // Oslo
            "0391" to "0300",
            // Rogaland
            "1191" to "1100",
            // NAV Tiltak Vestland
            "1287" to "1200",
            // NAV Tiltak Troms og Finnmark,
            "1987" to "1900",
            // NAV Tiltak Øst-Viken
            "0287" to "0200",
            // NAV Tiltak Oslo
            "0387" to "0300",
            // NAV Tiltak Innlandet,
            "0587" to "0400",
            // NAV Forvaltningstjenester Vest-Viken
            "0687" to "0600",
            // NAV Tiltak Agder
            "1087" to "1000",
            // NAV Tiltak Rogaland
            "1187" to "1100",
            // NAV Marked Sør-Rogaland
            "1194" to "1100",
            // NAV Marked Nord-Rogaland
            "1193" to "1100",
            // NAV Tiltak Trøndelag
            "5771" to "5700",
        ) + NAV_EGNE_ANSATTE_TIL_FYLKE_MAP

        val fantFylke = spesialEnheterTilFylkeMap[enhet.enhetNr]
        if (fantFylke == null && enhet.type != Norg2Type.KO) {
            slackNotifier.sendMessage("Fant ikke fylke for spesialenhet med enhetsnummer: ${enhet.enhetNr}. En utvikler må sjekke om enheten skal mappe til et fylke.")
            return null
        }
        return fantFylke
    }
}
