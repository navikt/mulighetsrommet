package no.nav.mulighetsrommet.api.deltaker.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.amt.model.AmtDeltakerEksternV1Dto
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.admin.deltaker.ReplikerDeltaker
import no.nav.mulighetsrommet.admin.deltaker.ReplikerDeltakerResultat
import no.nav.mulighetsrommet.admin.deltaker.ReplikerDeltakerUseCase
import no.nav.mulighetsrommet.api.domain.deltaker.Deltakelsesmengde
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.deltaker.NavVeileder
import no.nav.mulighetsrommet.api.utbetaling.service.GenererUtbetalingService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.UUID

class ReplikerDeltakerKafkaConsumer(
    private val replikerDeltaker: ReplikerDeltakerUseCase,
    private val genererUtbetalingService: GenererUtbetalingService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        val amtDeltaker = JsonIgnoreUnknownKeys.decodeFromJsonElement<AmtDeltakerEksternV1Dto?>(message)

        when (val resultat = replikerDeltaker.execute(ReplikerDeltaker(key, amtDeltaker?.toDeltaker()))) {
            is ReplikerDeltakerResultat.Slettet -> skedulerOppdaterUtbetalinger(resultat.gjennomforingId)
            is ReplikerDeltakerResultat.Lagret -> skedulerOppdaterUtbetalinger(resultat.gjennomforingId)
            ReplikerDeltakerResultat.IngenEndring -> {}
        }
    }

    private fun skedulerOppdaterUtbetalinger(gjennomforingId: UUID) {
        val offsetITilfelleDetErMangeEndringerForGjennomforing = Instant.now().plusSeconds(30)
        genererUtbetalingService.skedulerOppdaterUtbetalingerForGjennomforing(
            gjennomforingId = gjennomforingId,
            tidspunkt = offsetITilfelleDetErMangeEndringerForGjennomforing,
        )
    }
}

fun AmtDeltakerEksternV1Dto.toDeltaker(): Deltaker = Deltaker(
    id = id,
    gjennomforingId = gjennomforingId,
    startDato = startDato,
    sluttDato = sluttDato,
    registrertTidspunkt = registrertTidspunkt,
    endretTidspunkt = truncateMicros(endretTidspunkt),
    status = status.toDeltakerStatus(),
    deltakelsesmengder = deltakelsesmengder.map {
        Deltakelsesmengde(it.gyldigFraDato, it.deltakelsesprosent.toDouble(), it.opprettetTidspunkt.tilNorskInstant())
    },
    innholdAnnet = innhold?.let { innhold ->
        innhold.valgtInnhold.find { it.innholdskode == "annet" }?.tekst
    },
    navVeileder = navVeileder?.let {
        NavVeileder(
            navIdent = it.navIdent,
            enhetsnummer = it.enhetsnummer,
        )
    },
)

private fun AmtDeltakerEksternV1Dto.StatusDto.toDeltakerStatus(): DeltakerStatus = DeltakerStatus(
    type = type,
    aarsak = aarsak.type,
    opprettetTidspunkt = opprettetTidspunkt,
)

private fun truncateMicros(timestamp: LocalDateTime) = timestamp.truncatedTo(ChronoUnit.MICROS)

private fun LocalDateTime.tilNorskInstant(): Instant = atZone(ZoneId.of("Europe/Oslo")).toInstant()
