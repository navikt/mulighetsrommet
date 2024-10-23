package no.nav.mulighetsrommet.altinn

import no.nav.mulighetsrommet.altinn.models.BedriftRettigheter
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import java.time.Duration
import java.time.LocalDateTime

class AltinnRettigheterService(
    private val altinnClient: AltinnClient,
    private val altinnRettigheterRepository: AltinnRettigheterRepository,
) {
    private val rolleExpiryDuration = Duration.ofDays(1)

    suspend fun getRettigheter(norskIdent: NorskIdent): List<BedriftRettigheter> {
        val bedriftRettigheter = altinnRettigheterRepository.getRettigheter(norskIdent)
        return if (bedriftRettigheter.isEmpty() || bedriftRettigheter.any { it.rettigheter.any { it.expiry.isBefore(LocalDateTime.now()) } }) {
            syncRettigheter(norskIdent)
        } else {
            bedriftRettigheter.map { it.toBedriftRettigheter() }
        }
    }

    private suspend fun syncRettigheter(norskIdent: NorskIdent): List<BedriftRettigheter> {
        val rettigheter = altinnClient.hentRettigheter()
        altinnRettigheterRepository.upsertRettighet(
            PersonBedriftRettigheter(
                norskIdent = norskIdent,
                bedriftRettigheter = rettigheter,
                expiry = LocalDateTime.now().plusSeconds(rolleExpiryDuration.seconds),
            ),
        )

        return rettigheter
    }
}

fun BedriftRettigheterDbo.toBedriftRettigheter() =
    BedriftRettigheter(
        organisasjonsnummer = this.organisasjonsnummer,
        rettigheter = this.rettigheter.map { it.rettighet },
    )
