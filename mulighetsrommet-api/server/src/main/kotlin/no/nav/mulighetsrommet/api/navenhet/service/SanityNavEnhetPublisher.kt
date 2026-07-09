package no.nav.mulighetsrommet.api.navenhet.service

import io.ktor.http.HttpStatusCode
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.sanity.EnhetSlug
import no.nav.mulighetsrommet.api.sanity.FylkeRef
import no.nav.mulighetsrommet.api.sanity.SanityEnhet
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class SanityNavEnhetPublisher(
    private val sanityService: SanityService,
    private val slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun publish(enheter: List<NavEnhet>) {
        val enheterToSanity = utledEnheterTilSanity(enheter)
        val response = sanityService.createSanityEnheter(enheterToSanity)

        if (response.status != HttpStatusCode.OK) {
            logger.error("Klarte ikke opprette enheter i sanity: ${response.status}")
            slackNotifier.sendMessage("Klarte ikke oppdatere enheter fra NORG til Sanity. Statuskode: ${response.status.value}. Dette må sees på av en utvikler.")
        }
    }

    fun utledEnheterTilSanity(enheter: List<NavEnhet>): List<SanityEnhet> {
        val relevanteEnheter = enheter.filter { isRelevantEnhetForSanity(it) }

        val fylker = relevanteEnheter.filter { it.type == NavEnhetType.FYLKE }

        return fylker.flatMap { fylke ->
            val underliggendeEnheter = relevanteEnheter
                .filter { it.overordnetEnhet == fylke.enhetsnummer }
                .map { toSanityEnhet(it, fylke) }

            listOf(toSanityEnhet(fylke)) + underliggendeEnheter
        }
    }

    private fun isRelevantEnhetForSanity(enhet: NavEnhet): Boolean {
        return enhet.status in listOf(
            NavEnhetStatus.UNDER_ETABLERING,
            NavEnhetStatus.UNDER_AVVIKLING,
            NavEnhetStatus.AKTIV,
        ) && enhet.type in listOf(
            NavEnhetType.FYLKE,
            NavEnhetType.LOKAL,
        )
    }

    private fun toSanityEnhet(enhet: NavEnhet, fylke: NavEnhet? = null): SanityEnhet {
        var fylkeTilEnhet: FylkeRef? = null

        if (fylke != null) {
            fylkeTilEnhet = FylkeRef(
                _type = "reference",
                _ref = SanityNavEnhetUtils.toEnhetId(fylke),
                _key = fylke.enhetsnummer.value,
            )
        }

        return SanityEnhet(
            _id = SanityNavEnhetUtils.toEnhetId(enhet),
            _type = "enhet",
            navn = enhet.navn,
            nummer = EnhetSlug(
                _type = "slug",
                current = enhet.enhetsnummer.value,
            ),
            type = SanityNavEnhetUtils.toType(enhet.type.name),
            status = SanityNavEnhetUtils.toStatus(enhet.status.name),
            fylke = fylkeTilEnhet,
        )
    }
}
