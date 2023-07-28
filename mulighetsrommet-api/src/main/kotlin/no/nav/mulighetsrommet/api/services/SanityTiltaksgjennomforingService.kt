package no.nav.mulighetsrommet.api.services

import io.ktor.http.*
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.domain.dto.*
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.repositories.VirksomhetRepository
import no.nav.mulighetsrommet.domain.dto.TiltaksgjennomforingAdminDto
import org.slf4j.LoggerFactory
import java.util.*

class SanityTiltaksgjennomforingService(
    private val sanityClient: SanityClient,
    private val tiltaksgjennomforingRepository: TiltaksgjennomforingRepository,
    private val avtaleRepository: AvtaleRepository,
    private val tiltakstypeRepository: TiltakstypeRepository,
    private val virksomhetRepository: VirksomhetRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private suspend fun oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing: TiltaksgjennomforingAdminDto): Boolean {
        val tiltaksnummer = tiltaksgjennomforing.tiltaksnummer ?: return false

        val sanityTiltaksgjennomforinger = hentTiltaksgjennomforinger(tiltaksnummer)
        if (sanityTiltaksgjennomforinger.size > 1) {
            throw Exception("Fant ${sanityTiltaksgjennomforinger.size} sanity dokumenter med tiltaksnummer: $tiltaksnummer")
        }
        if (sanityTiltaksgjennomforinger.isEmpty()) {
            return false
        }
        tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
            tiltaksgjennomforing.id,
            UUID.fromString(sanityTiltaksgjennomforinger[0]._id),
        )
        return true
    }

    suspend fun opprettSanityTiltaksgjennomforing(tiltaksgjennomforing: TiltaksgjennomforingAdminDto) {
        if (tiltaksgjennomforing.sanityId != null || oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing)) {
            return
        }

        val sanityTiltaksgjennomforingId = UUID.randomUUID()
        val avtale = tiltaksgjennomforing.avtaleId?.let { avtaleRepository.get(it) }
        val tiltakstype = tiltakstypeRepository.get(tiltaksgjennomforing.tiltakstype.id)
        val enhet = virksomhetRepository.get(tiltaksgjennomforing.arrangor.organisasjonsnummer).getOrNull()
        val lokasjonForVirksomhetFraBrreg = "${enhet?.postnummer ?: ""} ${enhet?.poststed ?: ""}"

        val sanityTiltaksgjennomforing = SanityTiltaksgjennomforing(
            _id = "drafts.$sanityTiltaksgjennomforingId", // For å ikke autopublisere dokument i Sanity før redaktør manuelt publiserer
            tiltaksgjennomforingNavn = tiltaksgjennomforing.navn,
            enheter = tiltaksgjennomforing.navEnheter.map {
                EnhetRef(_ref = "enhet.lokal.${it.enhetsnummer}", _key = it.enhetsnummer)
            },
            fylke = avtale?.navRegion?.enhetsnummer?.let {
                FylkeRef(_ref = "enhet.fylke.$it")
            },
            tiltakstype = tiltakstype?.sanityId?.let { TiltakstypeRef(_ref = it.toString()) },
            tiltaksnummer = tiltaksgjennomforing.tiltaksnummer?.let { TiltaksnummerSlug(current = it) },
            sluttdato = tiltaksgjennomforing.sluttDato,
            lokasjon = lokasjonForVirksomhetFraBrreg,
        )

        val response = sanityClient.mutate(
            Mutations(mutations = listOf(Mutation(createIfNotExists = sanityTiltaksgjennomforing))),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke opprette tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Opprettet tiltaksgjennomforing i Sanity med id: $sanityTiltaksgjennomforingId")
        }

        tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
            tiltaksgjennomforing.id,
            sanityTiltaksgjennomforingId,
        )
    }

    private suspend fun hentTiltaksgjennomforinger(tiltaksnummer: String): List<SanityTiltaksgjennomforingResponse> {
        val query = """
            *[_type == "tiltaksgjennomforing" &&
            !(_id in path('drafts.**')) &&
            (tiltaksnummer.current == "$tiltaksnummer" || tiltaksnummer.current == "${
            tiltaksnummer.split("#").getOrNull(1)
        }")]
        """.trimIndent()
        return when (val response = sanityClient.query(query)) {
            is SanityResponse.Result -> {
                response.decode()
            }

            is SanityResponse.Error -> {
                throw RuntimeException("Feil ved henting av gjennomføringer fra Sanity: ${response.error}")
            }
        }
    }
}
