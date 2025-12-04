package no.nav.mulighetsrommet.api.navenhet

import io.ktor.http.*
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.api.sanity.EnhetSlug
import no.nav.mulighetsrommet.api.sanity.FylkeRef
import no.nav.mulighetsrommet.api.sanity.SanityEnhet
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class NavEnheterSyncService(
    private val db: ApiDatabase,
    private val norg2Client: Norg2Client,
    private val sanityService: SanityService,
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

    private fun lagreEnheter(enheter: List<Norg2Response>) = db.session {
        logger.info("Lagrer ${enheter.size} enheter til database")

        enheter.forEach { (enhet, overordnetEnhet) ->
            queries.enhet.upsert(
                NavEnhetDbo(
                    navn = enhet.navn,
                    enhetsnummer = enhet.enhetNr,
                    status = NavEnhetStatus.valueOf(enhet.status.name),
                    type = enhet.type,
                    overordnetEnhet = overordnetEnhet ?: tryResolveOverordnetEnhet(enhet),
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
                _key = fylke.enhetNr.value,
            )
        }

        return SanityEnhet(
            _id = NavEnhetUtils.toEnhetId(enhet),
            _type = "enhet",
            navn = enhet.navn,
            nummer = EnhetSlug(
                _type = "slug",
                current = enhet.enhetNr.value,
            ),
            type = NavEnhetUtils.toType(enhet.type.name),
            status = NavEnhetUtils.toStatus(enhet.status.name),
            fylke = fylkeTilEnhet,
        )
    }

    private fun tryResolveOverordnetEnhet(enhet: Norg2EnhetDto): NavEnhetNummer? {
        val spesialEnheterTilFylkeMap = TILTAKSENHETER_TIL_FYKLE_MAP + SPESIALENHET_SOM_KAN_VELGES_I_MODIA_TIL_FYLKE_MAP
        return spesialEnheterTilFylkeMap[enhet.enhetNr.value]?.let { NavEnhetNummer(it) }
    }
}
