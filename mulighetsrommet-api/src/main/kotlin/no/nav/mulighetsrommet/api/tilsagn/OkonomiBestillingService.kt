package no.nav.mulighetsrommet.api.tilsagn

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotliquery.Session
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.tiltak.okonomi.*
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class OkonomiBestillingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val kafkaProducerClient: KafkaProducerClient<String, String>,
) {
    data class Config(
        val topic: String,
    )

    @Serializable
    data class ScheduledOkonomiTask(
        val type: Type,
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
        @Serializable(with = UUIDSerializer::class)
        val utbetalingId: UUID?,
    ) {
        enum class Type {
            BEHANDLE_GODKJENT_TILSAGN,
            BEHANDLE_ANNULLERT_TILSAGN,
            BEHANDLE_GODKJENT_UTBETALING,
        }
    }

    val task: OneTimeTask<ScheduledOkonomiTask> = Tasks
        .oneTime(javaClass.simpleName, ScheduledOkonomiTask::class.java)
        .execute { instance, _ ->
            val (type, tilsagnId, utbetalingId) = instance.data

            when (type) {
                ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_TILSAGN -> behandleGodkjentTilsagn(tilsagnId)

                ScheduledOkonomiTask.Type.BEHANDLE_ANNULLERT_TILSAGN -> behandleAnnullertTilsagn(tilsagnId)

                ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_UTBETALING -> {
                    requireNotNull(utbetalingId) {
                        "utbetalingId trengs for faktura jobb"
                    }
                    behandleGodkjentUtbetaling(utbetalingId, tilsagnId)
                }
            }
        }

    fun scheduleBehandleGodkjentTilsagn(tilsagnId: UUID, session: Session) {
        val task = ScheduledOkonomiTask(ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_TILSAGN, tilsagnId, utbetalingId = null)
        schedule(task, session)
    }

    fun scheduleBehandleAnnullertTilsagn(tilsagnId: UUID, session: Session) {
        val task = ScheduledOkonomiTask(ScheduledOkonomiTask.Type.BEHANDLE_ANNULLERT_TILSAGN, tilsagnId, utbetalingId = null)
        schedule(task, session)
    }

    fun scheduleBehandleGodkjentUtbetaling(utbetalingId: UUID, tilsagnId: UUID, session: Session) {
        val task = ScheduledOkonomiTask(ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_UTBETALING, tilsagnId, utbetalingId)
        schedule(task, session)
    }

    private fun schedule(message: ScheduledOkonomiTask, session: Session) {
        val instance = task.instance(UUID.randomUUID().toString(), message)
        val client = transactionalSchedulerClient(task, session.connection.underlying)
        client.scheduleIfNotExists(instance, Instant.now())
    }

    private fun behandleGodkjentTilsagn(tilsagnId: UUID): Unit = db.session {
        val tilsagn = requireNotNull(queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }

        val gjennomforing = requireNotNull(queries.gjennomforing.get(tilsagn.gjennomforing.id)) {
            "Fant ikke gjennomforing for tilsagn"
        }

        val avtale = requireNotNull(gjennomforing.avtaleId?.let { queries.avtale.get(it) }) {
            "Gjennomføring ${gjennomforing.id} mangler avtale"
        }

        val arrangor = requireNotNull(
            avtale.arrangor?.let {
                OpprettBestilling.Arrangor(
                    hovedenhet = avtale.arrangor.organisasjonsnummer,
                    underenhet = gjennomforing.arrangor.organisasjonsnummer,
                )
            },
        ) {
            "Avtale ${avtale.id} mangler arrangør"
        }

        val bestilling = OpprettBestilling(
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = arrangor,
            kostnadssted = NavEnhetNummer(tilsagn.kostnadssted.enhetsnummer),
            bestillingsnummer = tilsagn.bestillingsnummer,
            // TODO: hvilket avtalenummer?
            avtalenummer = avtale.avtalenummer,
            belop = tilsagn.beregning.output.belop,
            periode = Periode.fromInclusiveDates(tilsagn.periodeStart, tilsagn.periodeSlutt),
            // TODO: send riktig part og tidspunkt
            opprettetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            opprettetTidspunkt = LocalDateTime.now(),
            besluttetAv = OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON),
            besluttetTidspunkt = LocalDateTime.now(),
        )

        val message = OkonomiBestillingMelding.Bestilling(bestilling)
        publish(bestilling.bestillingsnummer, message)
    }

    private fun behandleAnnullertTilsagn(tilsagnId: UUID): Unit = db.session {
        val tilsagn = requireNotNull(queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }

        publish(tilsagn.bestillingsnummer, OkonomiBestillingMelding.Annullering)
    }

    private fun behandleGodkjentUtbetaling(utbetalingId: UUID, tilsagnId: UUID): Unit = db.session {
        val utbetaling = requireNotNull(queries.utbetaling.get(utbetalingId)) {
            "Utbetaling med id=$utbetalingId finnes ikke"
        }
        val delutbetaling = requireNotNull(queries.delutbetaling.get(utbetalingId, tilsagnId)) {
            "Delutbetaling med utbetalingId=$utbetalingId, tilsagnId=$tilsagnId finnes ikke"
        }
        require(delutbetaling is DelutbetalingDto.DelutbetalingOverfortTilUtbetaling) {
            "delutbetaling er ikke godkjent"
        }
        val kontonummer = requireNotNull(utbetaling.betalingsinformasjon.kontonummer) {
            "Kontonummer mangler for utbetaling med id=${utbetaling.id}"
        }

        val tilsagn = requireNotNull(queries.tilsagn.get(delutbetaling.tilsagnId)) {
            "Tilsagn med id=${delutbetaling.tilsagnId} finnes ikke"
        }

        val faktura = OpprettFaktura(
            fakturanummer = delutbetaling.fakturanummer,
            bestillingsnummer = tilsagn.bestillingsnummer,
            betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                kontonummer = kontonummer,
                kid = utbetaling.betalingsinformasjon.kid,
            ),
            belop = delutbetaling.belop,
            periode = delutbetaling.periode,
            opprettetAv = OkonomiPart.NavAnsatt(delutbetaling.opprettetAv),
            opprettetTidspunkt = delutbetaling.opprettetTidspunkt,
            besluttetAv = OkonomiPart.NavAnsatt(delutbetaling.besluttetAv),
            besluttetTidspunkt = delutbetaling.besluttetTidspunkt,
        )

        val message = OkonomiBestillingMelding.Faktura(faktura)
        publish(faktura.bestillingsnummer, message)
    }

    private fun publish(bestillingsnummer: String, message: OkonomiBestillingMelding) {
        val record: ProducerRecord<String, String?> = ProducerRecord(
            config.topic,
            bestillingsnummer,
            Json.encodeToString(message),
        )
        kafkaProducerClient.sendSync(record)
    }
}
