package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.norg2.Norg2Client
import no.nav.mulighetsrommet.api.domain.Norg2Enhet
import no.nav.mulighetsrommet.api.domain.dbo.EnhetStatus
import no.nav.mulighetsrommet.api.domain.dbo.Norg2EnhetDbo
import no.nav.mulighetsrommet.api.repositories.EnhetRepository
import org.slf4j.LoggerFactory

class Norg2Service(private val norg2Client: Norg2Client, private val enhetRepository: EnhetRepository) {
    private val log = LoggerFactory.getLogger(javaClass)
    suspend fun synkroniserEnheter(): List<Norg2Enhet> {
        val enheter = norg2Client.hentEnheter()
        log.info("Hentet ${enheter.size} enheter fra NORG2")
        enheter.forEach {
            enhetRepository.upsert(
                Norg2EnhetDbo(
                    enhet_id = it.enhetId,
                    navn = it.navn,
                    enhetNr = it.enhetNr,
                    status = EnhetStatus.valueOf(it.status.name)
                )
            )
        }
        return enheter
    }
}
