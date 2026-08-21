package no.nav.mulighetsrommet.api.sanity.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
import no.nav.mulighetsrommet.api.domain.tiltakdokument.TiltakDokument
import no.nav.mulighetsrommet.api.sanity.SanityArrangor
import no.nav.mulighetsrommet.api.sanity.SanityResponse
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.tasks.DbSchedulerKotlinSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class MigrerSanityTiltaksgjennomforinger(
    private val db: ApiDatabase,
    private val sanityClient: SanityClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    val task: OneTimeTask<Void> = Tasks
        .oneTime(javaClass.simpleName, Void::class.java)
        .executeSuspend { _, _ ->
            migrer()
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .serializer(DbSchedulerKotlinSerializer())
        .build()

    fun schedule(id: UUID = UUID.randomUUID(), startTime: Instant = Instant.now()): UUID {
        val instance = task.instance(id.toString())
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun migrer() {
        logger.debug("Starter migrering av tiltaksgjennomføringer fra Sanity til database")

        val alleDokumenter = hentAlleTiltakFraSanity()
        logger.debug("Hentet ${alleDokumenter.size} dokumenter fra Sanity (inkl. utkast)")

        // PREVIEW_DRAFTS returns canonical IDs (no "drafts." prefix). A separate PUBLISHED
        // query tells us which documents are actually published.
        val publiserteSanityIds = hentPubliserteSanityIds()
        logger.debug("Fant ${publiserteSanityIds.size} publiserte tiltaksgjennomføringer")

        logger.debug("Migrerer ${alleDokumenter.size} tiltaksgjennomføringer")

        var antallOpprettet = 0
        var antallOppdatert = 0
        var antallFeilet = 0

        for (tiltak in alleDokumenter) {
            val sanityId = runCatching { UUID.fromString(tiltak._id) }.getOrElse { e ->
                logger.error("Ugyldig sanityId '${tiltak._id}': ${e.message}")
                antallFeilet++
                continue
            }
            runCatching {
                // Reuse existing DB id if sanity_id already exists (ensures idempotency)
                val eksisterende = db.session { repository.tiltakDokument.get(sanityId) }
                val id = eksisterende?.id ?: UUID.randomUUID()

                val navEnheter = buildList {
                    parseEnhetsnummer(tiltak.fylkeRef)?.let { add(it) }
                    tiltak.enheterRefs?.filterNotNull()?.mapNotNull { parseEnhetsnummer(it) }?.forEach { add(it) }
                }
                db.session {
                    val faneinnhold = tiltak.faneinnhold?.copy(
                        delMedBruker = tiltak.delingMedBruker ?: tiltak.faneinnhold.delMedBruker,
                    ) ?: tiltak.delingMedBruker?.let {
                        Faneinnhold(delMedBruker = it)
                    }

                    if (eksisterende != null) {
                        repository.tiltakDokument.save(
                            TiltakDokument(
                                id = id,
                                navn = eksisterende.navn,
                                tiltakstypeId = eksisterende.tiltakstypeId,
                                stedForGjennomforing = eksisterende.stedForGjennomforing,
                                arrangorId = eksisterende.arrangorId,
                                faneinnhold = faneinnhold,
                                beskrivelse = eksisterende.beskrivelse,
                                tiltaksnummer = tiltak.tiltaksnummer,
                                sanityId = sanityId,
                                administratorer = eksisterende.administratorer,
                                kontaktpersoner = eksisterende.kontaktpersoner,
                                navEnheter = eksisterende.navEnheter,
                                arrangorKontaktpersoner = eksisterende.arrangorKontaktpersoner,
                                publisert = eksisterende.publisert,
                            ),
                        )
                    }

                    logger.debug(
                        "sanityId={}, navEnheter={} (fylkeRef={}, enheterRefs={})",
                        sanityId,
                        navEnheter,
                        tiltak.fylkeRef,
                        tiltak.enheterRefs,
                    )
                }

                if (eksisterende != null) antallOppdatert++ else antallOpprettet++
            }.onFailure { e ->
                logger.error("Feil ved migrering av tiltaksgjennomforing med sanityId=$sanityId: ${e.message}", e)
                antallFeilet++
            }
        }

        logger.debug("Migrering fullført. Opprettet: $antallOpprettet, Oppdatert: $antallOppdatert, Feilet: $antallFeilet")
    }

    private suspend fun hentAlleTiltakFraSanity(): List<SanityGjennomforingMigrasjonDto> {
        val query = """
            *[_type == "tiltaksgjennomforing" && defined(tiltakstype) && defined(tiltaksgjennomforingNavn)] {
                _id,
                tiltakstype->{ _id },
                tiltaksgjennomforingNavn,
                "tiltaksnummer": tiltaksnummer.current,
                beskrivelse,
                stedForGjennomforing,
                "fylkeRef": fylke._ref,
                "enheterRefs": enheter[]._ref,
                arrangor->{ _id, navn, organisasjonsnummer },
                faneinnhold {
                    forHvemInfoboks,
                    forHvem,
                    detaljerOgInnholdInfoboks,
                    detaljerOgInnhold,
                    pameldingOgVarighetInfoboks,
                    pameldingOgVarighet,
                    kontaktinfoInfoboks,
                    kontaktinfo,
                    lenker,
                    oppskrift
                },
                delingMedBruker,
                "redaktor": redaktor[]->navIdent.current,
                "kontaktpersoner": kontaktpersoner[].navKontaktperson->navIdent.current
            }
        """.trimIndent()

        return when (val result = sanityClient.query(query, perspective = SanityPerspective.PREVIEW_DRAFTS)) {
            is SanityResponse.Result -> {
                result.decode<List<SanityGjennomforingMigrasjonDto>?>() ?: emptyList()
            }

            is SanityResponse.Error -> throw Exception("Klarte ikke hente tiltaksgjennomføringer fra Sanity: ${result.error}")
        }
    }

    /** Ref-format er "enhet.{type}.{nummer}", f.eks. "enhet.fylke.0200" → "0200" */
    private fun parseEnhetsnummer(ref: String?): NavEnhetNummer? {
        return ref?.substringAfterLast(".")?.let { runCatching { NavEnhetNummer(it) }.getOrNull() }
    }

    private suspend fun hentPubliserteSanityIds(): Set<UUID> {
        val query = """*[_type == "tiltaksgjennomforing"]._id"""

        return when (val result = sanityClient.query(query, perspective = SanityPerspective.PUBLISHED)) {
            is SanityResponse.Result -> result.decode<List<String>?>()
                ?.mapNotNull { runCatching { UUID.fromString(it) }.getOrNull() }
                ?.toSet()
                ?: emptySet()

            is SanityResponse.Error -> throw Exception("Klarte ikke hente publiserte sanityIds: ${result.error}")
        }
    }

    @Serializable
    data class SanityGjennomforingMigrasjonDto(
        val _id: String,
        val tiltakstype: SanityTiltakstype,
        val tiltaksgjennomforingNavn: String? = null,
        val tiltaksnummer: String? = null,
        val beskrivelse: String? = null,
        val stedForGjennomforing: String? = null,
        val fylkeRef: String? = null,
        val enheterRefs: List<String?>? = null,
        val arrangor: SanityArrangor? = null,
        val faneinnhold: Faneinnhold? = null,
        val delingMedBruker: String? = null,
        val redaktor: List<String?>? = null,
        val kontaktpersoner: List<String?>? = null,
    )
}
