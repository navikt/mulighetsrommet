package no.nav.mulighetsrommet.api.individuellgjennomforing.task

import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.clients.sanity.SanityPerspective
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

@Serializable
private data class SanityGjennomforingMigrasjonDto(
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
)

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
        logger.info("Starter migrering av tiltaksgjennomføringer fra Sanity til database")

        val alleDokumenter = hentAlleTiltakFraSanity()
        logger.info("Hentet ${alleDokumenter.size} dokumenter fra Sanity (inkl. utkast)")
        alleDokumenter.take(3).forEach { tiltak ->
            logger.info("Sanity dokument: _id=${tiltak._id}, fylkeRef=${tiltak.fylkeRef}, enheterRefs=${tiltak.enheterRefs}")
        }

        // PREVIEW_DRAFTS returns canonical IDs (no "drafts." prefix). A separate PUBLISHED
        // query tells us which documents are actually published.
        val publiserteSanityIds = hentPubliserteSanityIds()
        logger.info("Fant ${publiserteSanityIds.size} publiserte tiltaksgjennomføringer")

        logger.info("Migrerer ${alleDokumenter.size} tiltaksgjennomføringer")

        val tiltakstypePerSanityId = db.session {
            queries.tiltakstype.getAll(tiltakskoder = emptySet()).associateBy { it.sanityId?.toString() }
        }

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
                val navn = requireNotNull(tiltak.tiltaksgjennomforingNavn) {
                    "tiltaksgjennomforingNavn mangler"
                }
                val publisert = sanityId in publiserteSanityIds

                val tiltakstypeId = tiltakstypePerSanityId[tiltak.tiltakstype._id]?.id.also {
                    if (it == null) {
                        logger.warn(
                            "Fant ikke tiltakstype med sanityId=${tiltak.tiltakstype._id} " +
                                "for gjennomforing sanityId=$sanityId",
                        )
                    }
                }

                val arrangorId = tiltak.arrangor?.organisasjonsnummer?.let { orgnr ->
                    db.session { queries.arrangor.get(orgnr) }?.id.also {
                        if (it == null) {
                            logger.warn("Fant ikke arrangør med orgnr=$orgnr for gjennomforing sanityId=$sanityId")
                        }
                    }
                }

                // Reuse existing DB id if sanity_id already exists (ensures idempotency)
                val eksisterende = db.session { queries.individuellGjennomforing.getBySanityId(sanityId) }
                val id = eksisterende?.id ?: UUID.randomUUID()

                db.transaction {
                    queries.individuellGjennomforing.upsert(
                        id = id,
                        navn = navn,
                        tiltakstypeId = tiltakstypeId,
                        stedForGjennomforing = tiltak.stedForGjennomforing,
                        arrangorId = arrangorId,
                        faneinnhold = tiltak.faneinnhold,
                        beskrivelse = tiltak.beskrivelse,
                        tiltaksnummer = tiltak.tiltaksnummer,
                        sanityId = sanityId,
                    )
                    queries.individuellGjennomforing.setPublisert(id, publisert)

                    val navEnheter = buildSet {
                        parseEnhetsnummer(tiltak.fylkeRef)?.let { add(it) }
                        tiltak.enheterRefs?.filterNotNull()?.mapNotNull { parseEnhetsnummer(it) }?.forEach { add(it) }
                    }
                    logger.info("sanityId=$sanityId, navEnheter=$navEnheter (fylkeRef=${tiltak.fylkeRef}, enheterRefs=${tiltak.enheterRefs})")
                    queries.individuellGjennomforing.setNavEnheter(id, navEnheter)
                }

                if (eksisterende != null) antallOppdatert++ else antallOpprettet++
                logger.debug("Migrert sanityId={}, id={}, publisert={}", sanityId, id, publisert)
            }.onFailure { e ->
                logger.error("Feil ved migrering av tiltaksgjennomforing med sanityId=$sanityId: ${e.message}", e)
                antallFeilet++
            }
        }

        logger.info("Migrering fullført. Opprettet: $antallOpprettet, Oppdatert: $antallOppdatert, Feilet: $antallFeilet")
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
                }
            }
        """.trimIndent()

        return when (val result = sanityClient.query(query, perspective = SanityPerspective.PREVIEW_DRAFTS)) {
            is SanityResponse.Result -> {
                logger.info("Råsvar fra Sanity: ${result.result}")
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
}
