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

    private suspend fun oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing: TiltaksgjennomforingAdminDto): UUID? {
        val tiltaksnummer = tiltaksgjennomforing.tiltaksnummer ?: return null

        val sanityTiltaksgjennomforinger = hentTiltaksgjennomforinger(tiltaksnummer)
        if (sanityTiltaksgjennomforinger.size > 1) {
            throw Exception("Fant ${sanityTiltaksgjennomforinger.size} sanity dokumenter med tiltaksnummer: $tiltaksnummer")
        }
        if (sanityTiltaksgjennomforinger.isEmpty()) {
            return null
        }
        val sanityId = UUID.fromString(sanityTiltaksgjennomforinger[0]._id)
        tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
            tiltaksgjennomforing.id,
            sanityId,
        )
        return sanityId
    }

    suspend fun createOrPatchSanityTiltaksgjennomforing(tiltaksgjennomforing: TiltaksgjennomforingAdminDto) {
        val eksisterendeSanityId = tiltaksgjennomforing.sanityId ?: oppdaterIdOmAlleredeFinnes(tiltaksgjennomforing)

        val avtale = tiltaksgjennomforing.avtaleId?.let { avtaleRepository.get(it) }
        val tiltakstype = tiltakstypeRepository.get(tiltaksgjennomforing.tiltakstype.id)
        val enhet = virksomhetRepository.get(tiltaksgjennomforing.arrangor.organisasjonsnummer).getOrNull()
        val lokasjonForVirksomhetFraBrreg = "${enhet?.postnummer ?: ""} ${enhet?.poststed ?: ""}"

        val sanityTiltaksgjennomforingFields = SanityTiltaksgjennomforingFields(
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
            lokasjon = tiltaksgjennomforing.lokasjonArrangor ?: lokasjonForVirksomhetFraBrreg,
        )

        if (eksisterendeSanityId != null) {
            patchSanityTiltaksgjennomforing(eksisterendeSanityId, sanityTiltaksgjennomforingFields)
        } else {
            val sanityId = UUID.randomUUID()
            createSanityTiltaksgjennomforing(sanityId, sanityTiltaksgjennomforingFields)

            tiltaksgjennomforingRepository.updateSanityTiltaksgjennomforingId(
                tiltaksgjennomforing.id,
                sanityId,
            )
        }
    }

    private suspend fun createSanityTiltaksgjennomforing(
        sanityId: UUID,
        sanityTiltaksgjennomforingFields: SanityTiltaksgjennomforingFields,
    ) {
        val sanityTiltaksgjennomforing = sanityTiltaksgjennomforingFields.toSanityTiltaksgjennomforing(
            id = "drafts.$sanityId", // For å ikke autopublisere dokument i Sanity før redaktør manuelt publiserer
        )

        val response = sanityClient.mutate(
            listOf(Mutation(createOrReplace = sanityTiltaksgjennomforing)),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke opprette tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Opprettet tiltaksgjennomforing i Sanity med id: $sanityId")
        }
    }

    private suspend fun patchSanityTiltaksgjennomforing(
        sanityId: UUID,
        sanityTiltaksgjennomforingFields: SanityTiltaksgjennomforingFields,
    ) {
        val response = sanityClient.mutate(
            listOf(Mutation(patch = Patch(id = sanityId.toString(), set = sanityTiltaksgjennomforingFields))),
        )

        if (response.status.value != HttpStatusCode.OK.value) {
            throw Exception("Klarte ikke patche tiltaksgjennomforing i sanity: ${response.status}")
        } else {
            log.info("Patchet tiltaksgjennomforing i Sanity med id: $sanityId")
        }
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
