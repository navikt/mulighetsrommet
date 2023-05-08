package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.norg2.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.api.utils.NavEnhetUtils
import no.nav.mulighetsrommet.slack.SlackNotifier
import org.slf4j.LoggerFactory

class NavEnheterSyncService(
    private val norg2Client: Norg2Client,
    private val sanityClient: SanityClient,
    private val enhetRepository: NavEnhetRepository,
    private val slackNotifier: SlackNotifier,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val fylkerOgEnheterTyper = listOf(Norg2Type.FYLKE, Norg2Type.TILTAK, Norg2Type.LOKAL)
    private val spesialEnheterTyper = listOf(Norg2Type.ALS)

    suspend fun synkroniserEnheter(): List<Norg2Response> {
        val enheterFraNorg = norg2Client.hentEnheter()
        val (tilLagring, tilSletting) = enheterFraNorg.partition { erFylkeEllerUnderenhet(it) }
        val spesialEnheter =
            enheterFraNorg.filter { erSpesialenhet(it) }
        logger.info("Hentet ${enheterFraNorg.size} enheter fra NORG2. Sletter potensielt ${tilSletting.size} enheter som ikke har en whitelistet type ($fylkerOgEnheterTyper). Lagrer ${tilLagring.size} enheter fra NORG2 med type = $fylkerOgEnheterTyper")

        val spesialEnheterToSanity = spesialEnheterToSanityEnheter(spesialEnheter)
        val fylkerOgEnheterToSanity = fylkeOgUnderenheterToSanity(tilLagring)

        slettEnheterSomIkkeHarWhitelistetType(tilSletting.map { it.enhet.enhetNr })
        lagreEnheter(tilLagring + spesialEnheter)
        lagreEnheterTilSanity(spesialEnheterToSanity + fylkerOgEnheterToSanity)

        return tilLagring
    }

    private fun lagreEnheter(enheter: List<Norg2Response>) {
        enheter.forEach {
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = it.enhet.navn,
                    enhetsnummer = it.enhet.enhetNr,
                    status = NavEnhetStatus.valueOf(it.enhet.status.name),
                    type = Norg2Type.valueOf(it.enhet.type.name),
                    overordnetEnhet = it.overordnetEnhet,
                ),
            )
        }
    }

    suspend fun lagreEnheterTilSanity(sanityEnheter: List<SanityEnhet>) {
        logger.info("Oppdaterer Sanity-enheter - Antall: ${sanityEnheter.size}")
        val mutations = Mutations(mutations = sanityEnheter.map { Mutation(createOrReplace = it) })

        val response = sanityClient.mutate(mutations)

        if (response.status.value != HttpStatusCode.OK.value) {
            logger.error("Klarte ikke oppdatere enheter fra NORG til Sanity: {}", response.status)
            slackNotifier.sendMessage("Klarte ikke oppdatere enheter fra NORG til Sanity. Statuskode: ${response.status.value}. Dette må sees på av en utvikler.")
        } else {
            logger.info("Oppdaterte enheter fra NORG til Sanity.")
        }
    }

    fun spesialEnheterToSanityEnheter(enheter: List<Norg2Response>): List<SanityEnhet> {
        return enheter.filter { NavEnhetUtils.relevanteStatuser(it.enhet.status) }.map { toSanityEnhet(it.enhet) }
    }

    fun fylkeOgUnderenheterToSanity(fylkerOgEnheter: List<Norg2Response>): List<SanityEnhet> {
        val fylker =
            fylkerOgEnheter.filter { NavEnhetUtils.relevanteStatuser(it.enhet.status) && it.enhet.type == Norg2Type.FYLKE }

        return fylker.flatMap { fylke ->
            val underliggendeEnheter = fylkerOgEnheter.filter { NavEnhetUtils.isUnderliggendeEnhet(fylke.enhet, it) }
                .map { toSanityEnhet(it.enhet, fylke.enhet) }
            listOf(toSanityEnhet(fylke.enhet)) + underliggendeEnheter
        }
    }

    private fun toSanityEnhet(enhet: Norg2EnhetDto, fylke: Norg2EnhetDto? = null): SanityEnhet {
        var fylkeTilEnhet: FylkeRef? = null

        if (fylke != null) {
            fylkeTilEnhet = FylkeRef(
                _type = "reference",
                _ref = NavEnhetUtils.toEnhetId(fylke),
                _key = fylke.enhetNr,
            )
        } else if (enhet.type == Norg2Type.ALS) {
            val fylkesnummer = getFylkesnummerForSpesialenhet(enhet.enhetNr)
            if (fylkesnummer != null) {
                fylkeTilEnhet = FylkeRef(
                    _type = "reference",
                    _ref = "enhet.fylke.$fylkesnummer",
                    _key = fylkesnummer,
                )
            }
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

    private fun getFylkesnummerForSpesialenhet(enhetNr: String): String? {
        val spesialEnheterTilFylkeMap = mapOf(
            "1291" to "1200", // Vestland
            "0291" to "0200", // Øst-Viken
            "1591" to "1500", // Møre og Romsdal,
            "1891" to "1800", // Nordland
            "0491" to "0400", // Innlandet
            "0691" to "0600", // Vest-Viken,
            "0891" to "0800", // Vestfold og Telemark
            "1091" to "1000", // Agder,
            "1991" to "1900", // Troms og Finnmark
            "5772" to "5700", // Trøndelag,
            "0391" to "0300", // Oslo
            "1191" to "1100", // Rogaland
        )

        val fantFylke = spesialEnheterTilFylkeMap[enhetNr]
        if (fantFylke == null) {
            slackNotifier.sendMessage("Fant ikke fylke for spesialenhet med enhetsnummer: $enhetNr. En utvikler må sjekke om enheten skal mappe til et fylke.")
            return null
        }
        return fantFylke
    }

    private fun slettEnheterSomIkkeHarWhitelistetType(ider: List<String>) {
        enhetRepository.deleteWhereEnhetsnummer(ider)
    }

    private fun erSpesialenhet(enhet: Norg2Response): Boolean {
        return enhet.enhet.type in spesialEnheterTyper && enhet.enhet.status in getWhitelistForStatus()
    }

    private fun erFylkeEllerUnderenhet(enhet: Norg2Response): Boolean {
        return enhet.enhet.type in fylkerOgEnheterTyper && enhet.enhet.status in getWhitelistForStatus()
    }

    private fun getWhitelistForStatus(): List<Norg2EnhetStatus> {
        return listOf(Norg2EnhetStatus.AKTIV, Norg2EnhetStatus.UNDER_AVVIKLING, Norg2EnhetStatus.UNDER_ETABLERING)
    }
}
