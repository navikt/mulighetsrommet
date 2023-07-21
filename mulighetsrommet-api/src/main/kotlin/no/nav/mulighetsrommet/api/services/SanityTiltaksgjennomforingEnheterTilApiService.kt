package no.nav.mulighetsrommet.api.services

import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltaksgjennomforingResponse
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import org.slf4j.LoggerFactory

class SanityTiltaksgjennomforingEnheterTilApiService(
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val sanityClient: SanityClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    private suspend fun hentTiltaksgjennomforinger(): List<SanityTiltaksgjennomforingResponse> {
        val query = """ *[_type == "tiltaksgjennomforing" && !(_id in path('drafts.**'))] """.trimIndent()
        return when (val response = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                response.decode()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }
    }

    suspend fun oppdaterTiltaksgjennomforingEnheter() {
        val gjennomforinger = hentTiltaksgjennomforinger()

        val gjennomforingerMedEnheter = gjennomforinger
            .filter {
                (it.tiltaksnummer != null) && !it.enheter.isNullOrEmpty()
            }

        val suksesser = gjennomforingerMedEnheter
            .sumOf {
                val enheter = it.enheter!!
                    .map { enhet -> enhet._ref.takeLast(4) }

                tiltaksgjennomforingRepository.updateEnheter(it.tiltaksnummer!!.current, enheter)
            }

        logger.info(
            "Oppdaterte enheter for tiltaksgjennomføringer fra Sanity." +
                " $suksesser suksesser, ${gjennomforingerMedEnheter.size - suksesser} feilet.",
        )
    }
}
