package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetStatus
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Response
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.repositories.EnhetRepository
import org.slf4j.LoggerFactory

class Norg2Service(private val norg2Client: Norg2Client, private val enhetRepository: EnhetRepository) {
    private val log = LoggerFactory.getLogger(javaClass)
    suspend fun synkroniserEnheter(): List<Norg2Response> {
        val whitelistTyper = listOf(Norg2Type.FYLKE, Norg2Type.TILTAK, Norg2Type.LOKAL)
        val whitelistStatus = listOf(Norg2EnhetStatus.AKTIV, Norg2EnhetStatus.UNDER_AVVIKLING, Norg2EnhetStatus.UNDER_ETABLERING)
        val alleEnheter = norg2Client.hentEnheter()
        val (tilLagring, tilSletting) = alleEnheter.partition { it.enhet.type in whitelistTyper && it.enhet.status in whitelistStatus }
        log.info("Hentet ${alleEnheter.size} enheter fra NORG2. Sletter potensielt ${tilSletting.size} enheter som ikke har en whitelistet type ($whitelistTyper). Lagrer ${tilLagring.size} enheter fra NORG2 med type = $whitelistTyper")

        slettEnheterSomIkkeHarWhitelistetType(tilSletting.map { it.enhet.enhetNr })
        lagreEnheter(tilLagring)

        return tilLagring
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
}
