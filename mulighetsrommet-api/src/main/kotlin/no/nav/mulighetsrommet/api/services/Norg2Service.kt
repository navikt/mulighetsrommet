package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.clients.norg2.Norg2EnhetDto
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.repositories.EnhetRepository
import org.slf4j.LoggerFactory

class Norg2Service(private val norg2Client: Norg2Client, private val enhetRepository: EnhetRepository) {
    private val log = LoggerFactory.getLogger(javaClass)
    suspend fun synkroniserEnheter(): List<Norg2EnhetDto> {
        val whitelistTyper = listOf(Norg2Type.FYLKE, Norg2Type.TILTAK, Norg2Type.LOKAL)
        val alleEnheter = norg2Client.hentEnheter()
        val tilSletting = alleEnheter.filterNot { it.type in whitelistTyper }.map { it.enhetId }
        val tilLagring = alleEnheter.filter { it.type in whitelistTyper }
        log.info("Hentet ${alleEnheter.size} enheter fra NORG2. Sletter potensielt ${tilSletting.size} enheter som ikke har en whitelistet type ($whitelistTyper). Lagrer ${tilLagring.size} enheter fra NORG2 med type = $whitelistTyper")

        slettEnheterSomIkkeHarWhitelistetType(tilSletting)
        lagreEnheter(tilLagring)

        return tilLagring
    }

    private fun lagreEnheter(enheter: List<Norg2EnhetDto>) {
        enheter.forEach {
            enhetRepository.upsert(
                NavEnhetDbo(
                    enhetId = it.enhetId,
                    navn = it.navn,
                    enhetNr = it.enhetNr,
                    status = NavEnhetStatus.valueOf(it.status.name)
                )
            )
        }
    }

    private fun slettEnheterSomIkkeHarWhitelistetType(ider: List<Int>) {
        enhetRepository.deleteWhereIds(ider)
    }
}
