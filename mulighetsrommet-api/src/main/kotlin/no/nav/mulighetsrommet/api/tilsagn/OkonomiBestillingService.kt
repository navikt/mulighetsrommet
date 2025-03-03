package no.nav.mulighetsrommet.api.tilsagn

import com.github.kagkarlsson.scheduler.task.helper.OneTimeTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotliquery.Session
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.model.*
import no.nav.mulighetsrommet.serializers.UUIDSerializer
import no.nav.mulighetsrommet.tasks.transactionalSchedulerClient
import no.nav.tiltak.okonomi.*
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.util.*

class OkonomiBestillingService(
    private val config: Config,
    private val db: ApiDatabase,
    private val kafkaProducerClient: KafkaProducerClient<String, String>,
) {
    private val log = LoggerFactory.getLogger(javaClass.simpleName)

    data class Config(
        val topic: String,
    )

    @Serializable
    data class ScheduledOkonomiTask(
        val type: Type,
        @Serializable(with = UUIDSerializer::class)
        val tilsagnId: UUID,
    ) {
        enum class Type {
            BEHANDLE_GODKJENT_TILSAGN,
            BEHANDLE_ANNULLERT_TILSAGN,
            BEHANDLE_GODKJENT_UTBETALINGER,
        }
    }

    val task: OneTimeTask<ScheduledOkonomiTask> = Tasks
        .oneTime(javaClass.simpleName, ScheduledOkonomiTask::class.java)
        .execute { instance, _ ->
            val (type, tilsagnId) = instance.data

            when (type) {
                ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_TILSAGN -> behandleGodkjentTilsagn(tilsagnId)

                ScheduledOkonomiTask.Type.BEHANDLE_ANNULLERT_TILSAGN -> behandleAnnullertTilsagn(tilsagnId)

                ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_UTBETALINGER -> {
                    behandleGodkjentUtbetalinger(tilsagnId)
                }
            }
        }

    fun scheduleBehandleGodkjentTilsagn(tilsagnId: UUID, session: Session) {
        val task = ScheduledOkonomiTask(ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_TILSAGN, tilsagnId)
        schedule(task, session)
    }

    fun scheduleBehandleAnnullertTilsagn(tilsagnId: UUID, session: Session) {
        val task = ScheduledOkonomiTask(ScheduledOkonomiTask.Type.BEHANDLE_ANNULLERT_TILSAGN, tilsagnId)
        schedule(task, session)
    }

    fun scheduleBehandleGodkjenteUtbetalinger(tilsagnId: UUID, session: Session) {
        val task = ScheduledOkonomiTask(ScheduledOkonomiTask.Type.BEHANDLE_GODKJENT_UTBETALINGER, tilsagnId)
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
        require(tilsagn.status == TilsagnStatus.GODKJENT) {
            "Tilsagn er ikke godkjent id=$tilsagnId status=${tilsagn.status}"
        }
        require(tilsagn.opprettelse.besluttetAv != null && tilsagn.opprettelse.besluttetTidspunkt != null) {
            "Tilsagn id=$tilsagnId må være besluttet godkjent for å sendes til økonomi"
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
            bestillingsnummer = tilsagn.bestillingsnummer,
            tilskuddstype = when (tilsagn.type) {
                TilsagnType.INVESTERING -> Tilskuddstype.TILTAK_INVESTERINGER
                else -> Tilskuddstype.TILTAK_DRIFTSTILSKUDD
            },
            tiltakskode = gjennomforing.tiltakstype.tiltakskode,
            arrangor = arrangor,
            kostnadssted = NavEnhetNummer(tilsagn.kostnadssted.enhetsnummer),
            // TODO: hvilket avtalenummer?
            avtalenummer = avtale.avtalenummer,
            belop = tilsagn.beregning.output.belop,
            periode = Periode.fromInclusiveDates(tilsagn.periodeStart, tilsagn.periodeSlutt),
            behandletAv = tilsagn.opprettelse.behandletAv.toOkonomiPart(),
            behandletTidspunkt = tilsagn.opprettelse.behandletTidspunkt,
            besluttetAv = tilsagn.opprettelse.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = tilsagn.opprettelse.besluttetTidspunkt,
        )

        publish(bestilling.bestillingsnummer, OkonomiBestillingMelding.Bestilling(bestilling))
    }

    private fun behandleAnnullertTilsagn(tilsagnId: UUID): Unit = db.session {
        val tilsagn = requireNotNull(queries.tilsagn.get(tilsagnId)) {
            "Tilsagn med id=$tilsagnId finnes ikke"
        }
        require(tilsagn.status == TilsagnStatus.ANNULLERT) {
            "Tilsagn er ikke annullert id=$tilsagnId status=${tilsagn.status}"
        }
        requireNotNull(tilsagn.annullering) {
            "Tilsagn id=$tilsagnId mangler annullering"
        }
        require(tilsagn.annullering.besluttetAv != null && tilsagn.annullering.besluttetTidspunkt != null) {
            "Tilsagn id=$tilsagnId må være besluttet annullert for å sendes som annullert til økonomi"
        }

        val annullering = AnnullerBestilling(
            bestillingsnummer = tilsagn.bestillingsnummer,
            behandletAv = tilsagn.annullering.behandletAv.toOkonomiPart(),
            behandletTidspunkt = tilsagn.annullering.behandletTidspunkt,
            besluttetAv = tilsagn.annullering.besluttetAv.toOkonomiPart(),
            besluttetTidspunkt = tilsagn.annullering.besluttetTidspunkt,
        )

        publish(tilsagn.bestillingsnummer, OkonomiBestillingMelding.Annullering(annullering))
    }

    fun behandleGodkjentUtbetalinger(tilsagnId: UUID) {
        val delutbetalinger = db.session { queries.delutbetaling.getSkalSendesTilOkonomi(tilsagnId) }
            .filterIsInstance<DelutbetalingDto.DelutbetalingOverfortTilUtbetaling>()
            .sortedBy { it.opprettelse.besluttetTidspunkt }

        delutbetalinger
            .forEach { delutbetaling ->
                log.info("Sender delutbetaling med utbetalingId: ${delutbetaling.utbetalingId} tilsagnId: ${delutbetaling.tilsagnId} på kafka")
                val utbetaling = requireNotNull(db.session { queries.utbetaling.get(delutbetaling.utbetalingId) }) {
                    "Utbetaling med id=${delutbetaling.utbetalingId} finnes ikke"
                }
                val kontonummer = requireNotNull(utbetaling.betalingsinformasjon.kontonummer) {
                    "Kontonummer mangler for utbetaling med id=${utbetaling.id}"
                }
                val tilsagn = requireNotNull(db.session { queries.tilsagn.get(delutbetaling.tilsagnId) }) {
                    "Tilsagn med id=${delutbetaling.tilsagnId} finnes ikke"
                }
                requireNotNull(delutbetaling.opprettelse.besluttetTidspunkt)
                requireNotNull(delutbetaling.opprettelse.besluttetAv)

                val faktura = OpprettFaktura(
                    fakturanummer = delutbetaling.fakturanummer,
                    bestillingsnummer = tilsagn.bestillingsnummer,
                    betalingsinformasjon = OpprettFaktura.Betalingsinformasjon(
                        kontonummer = kontonummer,
                        kid = utbetaling.betalingsinformasjon.kid,
                    ),
                    belop = delutbetaling.belop,
                    periode = delutbetaling.periode,
                    behandletAv = delutbetaling.opprettelse.behandletAv.toOkonomiPart(),
                    behandletTidspunkt = delutbetaling.opprettelse.behandletTidspunkt,
                    besluttetAv = delutbetaling.opprettelse.besluttetAv.toOkonomiPart(),
                    besluttetTidspunkt = delutbetaling.opprettelse.besluttetTidspunkt,
                )

                val message = OkonomiBestillingMelding.Faktura(faktura)
                db.transaction {
                    queries.delutbetaling.setSendtTilOkonomi(
                        delutbetaling.utbetalingId,
                        delutbetaling.tilsagnId,
                        LocalDateTime.now(),
                    )
                    publish(faktura.bestillingsnummer, message)
                }
            }
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

fun Agent.toOkonomiPart(): OkonomiPart = when (this) {
    is NavIdent -> OkonomiPart.NavAnsatt(this)
    is Tiltaksadministrasjon -> OkonomiPart.System(OkonomiSystem.TILTAKSADMINISTRASJON)
    Arrangor, Arena -> throw IllegalStateException("ugyldig agent")
}
