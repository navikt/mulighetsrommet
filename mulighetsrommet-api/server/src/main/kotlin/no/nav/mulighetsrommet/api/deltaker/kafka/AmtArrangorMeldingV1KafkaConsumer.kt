package no.nav.mulighetsrommet.api.deltaker.kafka

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtArrangorMelding
import no.nav.amt.model.EndringAarsak
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.domain.deltaker.DeltakerForslag
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import org.slf4j.LoggerFactory
import java.util.UUID

class AmtArrangorMeldingV1KafkaConsumer(
    private val db: ApiDatabase,
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement): Unit = db.session {
        logger.info("Konsumerer arrangor-melding med id=$key")

        val gjennomforingId = when (val melding = Json.decodeFromJsonElement<AmtArrangorMelding?>(message)) {
            is AmtArrangorMelding.Forslag -> {
                when (melding.status) {
                    is AmtArrangorMelding.Forslag.Status.Avvist,
                    is AmtArrangorMelding.Forslag.Status.Erstattet,
                    is AmtArrangorMelding.Forslag.Status.Godkjent,
                    is AmtArrangorMelding.Forslag.Status.Tilbakekalt,
                    -> {
                        val gjennomforingId = repository.deltakerForslag.get(key)?.let {
                            repository.deltaker.get(it.deltakerId)?.gjennomforingId
                        }
                        repository.deltakerForslag.delete(melding.id)
                        gjennomforingId
                    }

                    AmtArrangorMelding.Forslag.Status.VenterPaSvar -> {
                        val deltaker = repository.deltaker.get(melding.deltakerId)
                        if (deltaker != null) {
                            repository.deltakerForslag.save(melding.toForslag())
                        }
                        deltaker?.gjennomforingId
                    }
                }
            }

            null -> {
                val gjennomforingId = repository.deltakerForslag.get(key)?.let {
                    repository.deltaker.get(it.deltakerId)?.gjennomforingId
                }
                repository.deltakerForslag.delete(key)
                gjennomforingId
            }
        }

        gjennomforingId?.let {
            genererUtbetalingService.oppdaterUtbetalingBlokkeringerForGjennomforing(it)
        }
    }
}

fun AmtArrangorMelding.Forslag.toForslag(): DeltakerForslag {
    return DeltakerForslag(
        id = id,
        deltakerId = deltakerId,
        endring = endring.toEndring(),
        status = status.toStatus(),
    )
}

fun AmtArrangorMelding.Forslag.Status.toStatus(): DeltakerForslag.Status = when (this) {
    is AmtArrangorMelding.Forslag.Status.Avvist -> DeltakerForslag.Status.AVVIST
    is AmtArrangorMelding.Forslag.Status.Erstattet -> DeltakerForslag.Status.ERSTATTET
    is AmtArrangorMelding.Forslag.Status.Godkjent -> DeltakerForslag.Status.GODKJENT
    is AmtArrangorMelding.Forslag.Status.Tilbakekalt -> DeltakerForslag.Status.TILBAKEKALT
    AmtArrangorMelding.Forslag.Status.VenterPaSvar -> DeltakerForslag.Status.VENTER_PA_SVAR
}

fun AmtArrangorMelding.Forslag.Endring.toEndring(): DeltakerForslag.Endring = when (this) {
    is AmtArrangorMelding.Forslag.Endring.ForlengDeltakelse -> DeltakerForslag.Endring.ForlengDeltakelse(
        sluttdato = sluttdato,
    )

    is AmtArrangorMelding.Forslag.Endring.AvsluttDeltakelse -> DeltakerForslag.Endring.AvsluttDeltakelse(
        sluttdato = sluttdato,
        aarsak = aarsak?.toEndringAarsak(),
        harDeltatt = harDeltatt,
        harFullfort = harFullfort,
    )

    is AmtArrangorMelding.Forslag.Endring.IkkeAktuell -> DeltakerForslag.Endring.IkkeAktuell(
        aarsak = aarsak.toEndringAarsak(),
    )

    is AmtArrangorMelding.Forslag.Endring.Deltakelsesmengde -> DeltakerForslag.Endring.Deltakelsesmengde(
        deltakelsesprosent = deltakelsesprosent,
        dagerPerUke = dagerPerUke,
        gyldigFra = gyldigFra,
    )

    is AmtArrangorMelding.Forslag.Endring.Startdato -> DeltakerForslag.Endring.Startdato(
        startdato = startdato,
        sluttdato = sluttdato,
    )

    is AmtArrangorMelding.Forslag.Endring.Sluttdato -> DeltakerForslag.Endring.Sluttdato(
        sluttdato = sluttdato,
    )

    is AmtArrangorMelding.Forslag.Endring.Sluttarsak -> DeltakerForslag.Endring.Sluttarsak(
        aarsak = aarsak.toEndringAarsak(),
    )

    AmtArrangorMelding.Forslag.Endring.FjernOppstartsdato -> DeltakerForslag.Endring.FjernOppstartsdato

    is AmtArrangorMelding.Forslag.Endring.EndreAvslutning -> DeltakerForslag.Endring.EndreAvslutning(
        aarsak = aarsak?.toEndringAarsak(),
        harDeltatt = harDeltatt,
        harFullfort = harFullfort,
        sluttdato = sluttdato,
    )
}

fun EndringAarsak.toEndringAarsak(): DeltakerForslag.EndringAarsak = when (this) {
    EndringAarsak.Syk -> DeltakerForslag.EndringAarsak.Syk
    EndringAarsak.FattJobb -> DeltakerForslag.EndringAarsak.FattJobb
    EndringAarsak.TrengerAnnenStotte -> DeltakerForslag.EndringAarsak.TrengerAnnenStotte
    EndringAarsak.Utdanning -> DeltakerForslag.EndringAarsak.Utdanning
    EndringAarsak.IkkeMott -> DeltakerForslag.EndringAarsak.IkkeMott
    is EndringAarsak.Annet -> DeltakerForslag.EndringAarsak.Annet(beskrivelse = beskrivelse)
}
