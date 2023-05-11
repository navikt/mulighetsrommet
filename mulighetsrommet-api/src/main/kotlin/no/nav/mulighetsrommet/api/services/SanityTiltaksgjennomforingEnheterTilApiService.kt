package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltaksgjennomforingResponse
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.utils.getOrThrow
import org.slf4j.LoggerFactory

class SanityTiltaksgjennomforingEnheterTilApiService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val sanityClient: SanityClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private suspend fun hentEnheterForTiltaksgjennomforinger(): SanityResponse {
        val query =
            """
            *[_type == "tiltaksgjennomforing" && !(_id in path('drafts.**'))]{
              _id,
              "tiltaksnummer": tiltaksnummer.current,
              "enheter": enheter
            }
            """.trimIndent()
        return sanityClient.query(query)
    }

    suspend fun oppdaterTiltaksgjennomforingEnheter() {
        val gjennomforinger = when (val response = hentEnheterForTiltaksgjennomforinger()) {
            is SanityResponse.Result -> {
                response.decode<List<SanityTiltaksgjennomforingResponse>>()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }

        val gjennomforingerMedEnheter = gjennomforinger
            .filter {
                (it.tiltaksnummer != null) &&
                    !it.enheter.isNullOrEmpty() &&
                    it.enheter.any { enhet -> enhet._ref != null }
            }

        val suksesser = gjennomforingerMedEnheter
            .sumOf {
                val enheter = it.enheter!!
                    .filter { enhet -> enhet._ref != null }
                    .map { enhet -> enhet._ref!!.takeLast(4) }

                tiltaksgjennomforingRepository.updateEnheter(it.tiltaksnummer!!, enheter)
                    .getOrThrow()
            }

        logger.info(
            "Oppdaterte enheter for tiltaksgjennomføringer fra Sanity." +
                " $suksesser suksesser, ${gjennomforingerMedEnheter.size - suksesser} feilet.",
        )
    }
}
