package no.nav.mulighetsrommet.kafka.consumers.amt

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.repositories.DeltakerRepository
import no.nav.mulighetsrommet.database.utils.DatabaseOperationError
import no.nav.mulighetsrommet.database.utils.query
import no.nav.mulighetsrommet.domain.dbo.DeltakerDbo
import no.nav.mulighetsrommet.domain.dbo.Deltakeropphav
import no.nav.mulighetsrommet.domain.dbo.Deltakerstatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerStatus
import no.nav.mulighetsrommet.domain.dto.amt.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.*

class AmtDeltakerV1KafkaConsumer(
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

            amtDeltaker.status.type == AmtDeltakerStatus.Type.FEILREGISTRERT -> {
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
        // TODO ta en ny runde på statuser og se om vi trenger å gjøre noen oppdatering
        status = when (status.type) {
            AmtDeltakerStatus.Type.VENTER_PA_OPPSTART -> Deltakerstatus.VENTER
            AmtDeltakerStatus.Type.DELTAR -> Deltakerstatus.DELTAR
            AmtDeltakerStatus.Type.HAR_SLUTTET -> Deltakerstatus.AVSLUTTET
            AmtDeltakerStatus.Type.IKKE_AKTUELL -> Deltakerstatus.IKKE_AKTUELL
            AmtDeltakerStatus.Type.FEILREGISTRERT -> Deltakerstatus.IKKE_AKTUELL
            AmtDeltakerStatus.Type.PABEGYNT_REGISTRERING -> Deltakerstatus.PABEGYNT_REGISTRERING
            AmtDeltakerStatus.Type.SOKT_INN -> Deltakerstatus.VENTER
            AmtDeltakerStatus.Type.VURDERES -> Deltakerstatus.VENTER
            AmtDeltakerStatus.Type.VENTELISTE -> Deltakerstatus.VENTER
            AmtDeltakerStatus.Type.AVBRUTT -> Deltakerstatus.AVSLUTTET
            AmtDeltakerStatus.Type.FULLFORT -> Deltakerstatus.AVSLUTTET
            AmtDeltakerStatus.Type.UTKAST_TIL_PAMELDING -> Deltakerstatus.PABEGYNT_REGISTRERING
            AmtDeltakerStatus.Type.AVBRUTT_UTKAST -> Deltakerstatus.IKKE_AKTUELL
        },
        opphav = Deltakeropphav.AMT,
        startDato = startDato,
        sluttDato = sluttDato,
        registrertDato = registrertDato,
    )
}
