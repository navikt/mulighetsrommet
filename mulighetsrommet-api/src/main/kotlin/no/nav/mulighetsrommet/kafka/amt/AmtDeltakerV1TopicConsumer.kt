package no.nav.mulighetsrommet.kafka.amt

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.amt.AmtDeltakerV1Dto.Status
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.*

class AmtDeltakerV1TopicConsumer(
    config: Config,
    private val deltakere: DeltakerRepository,
) : KafkaTopicConsumer<UUID, JsonElement>(
    config,
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        logger.info("Konsumerer deltaker med id=$key")

        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerV1Dto?>(message)

        when {
            amtDeltaker == null -> {
                logger.info("Mottok tombstone for deltaker med id=$key, sletter deltakeren")
                deltakere.delete(key)
            }

            amtDeltaker.status == Status.FEILREGISTRERT -> {
                logger.info("Sletter deltaker med id=$key fordi den var feilregistrert")
                deltakere.delete(key)
            }

            else -> {
                logger.info("Forsøker å lagre deltaker med id=$key")
                val deltaker = amtDeltaker.toDeltakerDbo()
                query { deltakere.upsert(deltaker) }
                    .onLeft {
                        when (it) {
                            is DatabaseOperationError.ForeignKeyViolation -> {
                                logger.info("Ignorerer deltakelse med id=$key da den tilhører en gjennomføring som ikke finnes i databasen")
                            }

                            else -> {
                                logger.warn("Feil under konsumering av deltaker med id=$key", it.error)
                                throw it.error
                            }
                        }
                    }
            }
        }
    }

    private fun AmtDeltakerV1Dto.toDeltakerDbo(): DeltakerDbo = DeltakerDbo(
        id = id,
        tiltaksgjennomforingId = gjennomforingId,
        status = when (status) {
            Status.VENTER_PA_OPPSTART -> Deltakerstatus.VENTER
            Status.DELTAR -> Deltakerstatus.DELTAR
            Status.HAR_SLUTTET -> Deltakerstatus.AVSLUTTET
            Status.IKKE_AKTUELL -> Deltakerstatus.IKKE_AKTUELL
            Status.FEILREGISTRERT -> Deltakerstatus.IKKE_AKTUELL
            Status.PABEGYNT_REGISTRERING -> Deltakerstatus.PABEGYNT_REGISTRERING
            Status.PABEGYNT -> Deltakerstatus.PABEGYNT_REGISTRERING
            Status.AVBRUTT -> Deltakerstatus.AVSLUTTET
            Status.SOKT_INN -> Deltakerstatus.VENTER
            Status.VENTELISTE -> Deltakerstatus.VENTER
        },
        opphav = Deltakeropphav.AMT,
        startDato = startDato,
        sluttDato = sluttDato,
        registrertDato = registrertDato,
    )
}
