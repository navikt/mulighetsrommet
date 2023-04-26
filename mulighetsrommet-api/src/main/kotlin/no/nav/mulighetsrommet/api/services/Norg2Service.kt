package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.SanityEnhet
import no.nav.mulighetsrommet.api.repositories.EnhetRepository
import org.slf4j.LoggerFactory

class Norg2Service(
    private val norg2Client: Norg2Client,
    private val enhetRepository: EnhetRepository,
    private val sanityService: SanityService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val fylkerOgEnheterTyper = listOf(Norg2Type.FYLKE, Norg2Type.TILTAK, Norg2Type.LOKAL)
    private val spesialEnheterTyper = listOf(Norg2Type.ALS)
    suspend fun synkroniserEnheter(): List<Norg2Response> {
        val enheterFraNorg = norg2Client.hentEnheter()
        val (tilLagring, tilSletting) = enheterFraNorg.partition { erFylkeEllerUnderenhet(it) }
        log.info("Hentet ${enheterFraNorg.size} enheter fra NORG2. Sletter potensielt ${tilSletting.size} enheter som ikke har en whitelistet type ($fylkerOgEnheterTyper). Lagrer ${tilLagring.size} enheter fra NORG2 med type = $fylkerOgEnheterTyper")

        slettEnheterSomIkkeHarWhitelistetType(tilSletting.map { it.enhet.enhetNr })
        lagreEnheter(tilLagring)

        return tilLagring
    }

    suspend fun synkroniserEnheterToSanity(): List<SanityEnhet> {
        val enheterFraNorg = norg2Client.hentEnheter()

        val fylkerOgEnheter =
            enheterFraNorg.filter { erFylkeEllerUnderenhet(it) }
        val spesialEnheter =
            enheterFraNorg.filter { erSpesialenhet(it) }

        val spesialEnheterToSanity = sanityService.spesialEnheterToSanityEnheter(spesialEnheter)
        val fylkerOgEnheterToSanity = sanityService.fylkeOgUnderenheterToSanity(fylkerOgEnheter)

        return synkroniserToSanity(spesialEnheterToSanity + fylkerOgEnheterToSanity)
    }

    private suspend fun synkroniserToSanity(enheterSomSkalLagres: List<SanityEnhet>): List<SanityEnhet> {
        sanityService.updateEnheterToSanity(enheterSomSkalLagres)
        return enheterSomSkalLagres
    }

    private fun lagreEnheter(enheter: List<Norg2Response>) {
        enheter.forEach {
            enhetRepository.upsert(
                NavEnhetDbo(
                    navn = it.enhet.navn,
                    enhetNr = it.enhet.enhetNr,
                    status = NavEnhetStatus.valueOf(it.enhet.status.name),
                    type = Norg2Type.valueOf(it.enhet.type.name),
                    overordnetEnhet = it.overordnetEnhet,
                ),
            )
        }
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
