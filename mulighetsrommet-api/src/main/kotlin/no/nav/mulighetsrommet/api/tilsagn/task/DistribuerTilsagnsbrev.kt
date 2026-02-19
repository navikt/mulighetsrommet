package no.nav.mulighetsrommet.api.tilsagn.task

import arrow.core.Either
import arrow.core.getOrElse
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistClient
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistError
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistRequest
import no.nav.mulighetsrommet.api.clients.teamdokumenthandtering.DokdistResponse
import no.nav.mulighetsrommet.brreg.BrregClient
import no.nav.mulighetsrommet.brreg.BrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.BrregUnderenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregHovedenhetDto
import no.nav.mulighetsrommet.brreg.SlettetBrregUnderenhetDto
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.executeSuspend
import no.nav.mulighetsrommet.tokenprovider.AccessType
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class DistribuerTilsagnsbrev(
    private val db: ApiDatabase,
    private val dokdistClient: DokdistClient,
    private val brregClient: BrregClient,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Serializable
    data class TaskData(
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
    )

    val task: OneTimeTask<TaskData> = Tasks
        .oneTime(javaClass.simpleName, TaskData::class.java)
        .executeSuspend { inst, _ ->
            distribuerDok(inst.data.tilsagnId).onLeft { message ->
                throw Exception("Feil distribuering av tilsagnsbrev for tilsagn id=${inst.data.tilsagnId}: $message")
            }
        }

    private val client = SchedulerClient.Builder
        .create(db.getDatasource(), task)
        .build()

    fun schedule(tilsagnId: UUID, startTime: Instant = Instant.now()): UUID {
        val id = UUID.randomUUID()
        val instance = task.instance(id.toString(), TaskData(tilsagnId))
        client.scheduleIfNotExists(instance, startTime)
        return id
    }

    suspend fun distribuerDok(tilsagnId: UUID): Either<DokdistError, DokdistResponse> = db.session {
        logger.info("Distribuerer journalpost for tilsagn id: $tilsagnId")

        val tilsagn = queries.tilsagn.getOrError(tilsagnId)
        require(tilsagn.journalpostId != null) { "Tilsagn med id=$tilsagnId har ingen journalpostId, distribuering ikke mulig" }
        val arrangor = queries.arrangor.get(tilsagn.arrangor.organisasjonsnummer)
        require(arrangor != null) { "Arrangør med orgnr=${tilsagn.arrangor.organisasjonsnummer} ikke funnet i database, distribuering ikke mulig" }
        val adresse = if (arrangor.erUtenlandsk) {
            hentUtenlandskArrangorAdresse(arrangor.id)
        } else {
            hentNorskArrangorAdresse(arrangor.organisasjonsnummer)
        }

        dokdistClient.distribuerJournalpost(
            journalpostId = tilsagn.journalpostId,
            accessType = AccessType.M2M,
            distribusjonstype = DokdistRequest.DistribusjonsType.VIKTIG,
            adresse = adresse,
        )
    }

    fun hentUtenlandskArrangorAdresse(arrangorId: UUID): DokdistRequest.Adresse.UtenlandskPostadresse = db.session {
        val utenlandskArrangor = queries.arrangor.getUtenlandskArrangor(arrangorId)
        require(utenlandskArrangor != null) { "Utenlandsk arrangør med id=$arrangorId mangler informasjon, kunne ikke hente adresse" }
        DokdistRequest.Adresse.UtenlandskPostadresse(
            land = utenlandskArrangor.landKode,
            adresselinje1 = utenlandskArrangor.gateNavn,
            adresselinje2 = utenlandskArrangor.by,
            adresselinje3 = null,
        )
    }

    suspend fun hentNorskArrangorAdresse(organisasjonsnummer: Organisasjonsnummer): DokdistRequest.Adresse.NorskPostAdresse {
        val enhet = brregClient.getBrregEnhet(organisasjonsnummer)
            .getOrElse { error ->
                throw Exception("Feil ved henting av norsk arrangøradresse for arrangør med orgnr=$organisasjonsnummer: $error")
            }
        val postadresse =
            when (enhet) {
                is BrregHovedenhetDto -> enhet.postadresse
                is SlettetBrregHovedenhetDto -> enhet.postadresse
                is BrregUnderenhetDto -> TODO("Hvordan håndtere underenhet? Skal vi hente hovedenhetens adresse?")
                is SlettetBrregUnderenhetDto -> TODO("Hvordan håndtere slettet underenhet? Skal vi hente hovedenhetens adresse?")
            }
        requireNotNull(postadresse)
        requireNotNull(postadresse.postnummer) { "Norsk arrangør med orgnr=$organisasjonsnummer mangler postnummer i postadresse, kunne ikke hente adresse" }
        requireNotNull(postadresse.poststed) { "Norsk arrangør med orgnr=$organisasjonsnummer mangler poststed i postadresse, kunne ikke hente adresse" }

        return DokdistRequest.Adresse.NorskPostAdresse(
            postnummer = postadresse.postnummer!!,
            poststed = postadresse.poststed!!,
            adresselinje1 = postadresse.adresse?.getOrNull(0),
            adresselinje2 = postadresse.adresse?.getOrNull(1),
            adresselinje3 = postadresse.adresse?.getOrNull(2),
        )
    }
}
