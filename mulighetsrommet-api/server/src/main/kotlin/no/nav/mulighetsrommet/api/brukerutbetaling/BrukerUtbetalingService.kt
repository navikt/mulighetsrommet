package no.nav.mulighetsrommet.api.brukerutbetaling

import arrow.core.toNonEmptySetOrThrow
import kotlinx.serialization.json.Json
import no.nav.common.kafka.producer.KafkaProducerClient
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.brukerutbetaling.db.BrukerUtbetalingDbo
import no.nav.mulighetsrommet.api.brukerutbetaling.db.UpsertBrukerUtbetalingDbo
import no.nav.mulighetsrommet.api.clients.helved.HelVedStatus
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.gjennomforing.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddVedtakDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.env.NaisEnv
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class UtbetalTilskudd(
    val behandlingId: UUID,
    val saksbehandler: NavIdent,
    val beslutter: NavIdent,
    val besluttetTidspunkt: Instant,
)

data class OpphorTilskuddUtbetaling(
    val behandlingId: UUID,
    val saksbehandler: NavIdent,
    val beslutter: NavIdent,
    val besluttetTidspunkt: Instant,
)

class BrukerUtbetalingService(
    val config: Config,
    val db: ApiDatabase,
    val kafkaProducerClient: KafkaProducerClient<ByteArray, ByteArray?>,
    private val personaliaService: PersonaliaService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    data class Config(
        val helvedUtbetalingTopic: String,
    )

    suspend fun utbetalTilskudd(command: UtbetalTilskudd) {
        val behandling = getBehandling(command.behandlingId, TilskuddBehandlingType.REGISTRERING)
        val gjennomforing = db.session {
            queries.gjennomforing.getGjennomforingEnkeltplassOrError(behandling.gjennomforingId)
        }
        val norskIdent = getNorskIdent(behandling)
        nyUtbetalingTilBruker(command, behandling, gjennomforing, norskIdent)
    }

    suspend fun opphorTilskuddUtbetaling(command: OpphorTilskuddUtbetaling) {
        val behandling = getBehandling(command.behandlingId, TilskuddBehandlingType.REVURDERING)
        val norskIdent = getNorskIdent(behandling)
        opphorUtbetalingTilBruker(command, behandling, norskIdent)
    }

    private fun getBehandling(behandlingId: UUID, forventetType: TilskuddBehandlingType): TilskuddBehandlingDto {
        val behandling = db.session { queries.tilskuddBehandling.get(behandlingId) }
            ?: error("Fant ikke godkjent tilskudd_behandling id=$behandlingId")
        check(behandling.type == forventetType) {
            "Forventet $forventetType, men fant ${behandling.type} for behandlingId=$behandlingId"
        }
        return behandling
    }

    private suspend fun getNorskIdent(behandling: TilskuddBehandlingDto): NorskIdent {
        val deltaker = db.session { getDeltaker(behandling.gjennomforingId) }
        val personalia = personaliaService.getPersonalia(deltaker.id, PersonaliaService.OnBehalfOf.System)
        return checkNotNull(personalia.norskIdent()) {
            "Fant ikke norsk ident for deltaker med id=${deltaker.id}, behandlingId=${behandling.id}"
        }
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker {
        val deltakelser = repository.deltaker.getByGjennomforing(gjennomforingId)
        if (deltakelser.size != 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet akkurat én)")
        }
        return deltakelser.first()
    }

    private fun nyUtbetalingTilBruker(
        command: UtbetalTilskudd,
        behandling: TilskuddBehandlingDto,
        gjennomforing: GjennomforingEnkeltplass,
        personIdent: NorskIdent,
    ) {
        behandling.tilskuddVedtak
            .filter { it.vedtakResultat.type == VedtakResultat.INNVILGELSE }
            .filter { it.utbetalingMottaker is TilskuddMottaker.Bruker }
            .forEach { tilskuddVedtak ->
                db.transaction {
                    queries.tilskuddBehandling.acquireLockTilskudd(tilskuddVedtak.tilskuddId)
                    val tidligereUtbetaling = queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtak.id)
                    if (tidligereUtbetaling != null) {
                        logger.warn("Utbetaling for tilskudd vedtak med id=${tilskuddVedtak.id} er allerede opprettet. Sak id=${tidligereUtbetaling.sakId}, behandling id=${tidligereUtbetaling.behandlingId}")
                        return@transaction
                    }

                    queries.brukerUtbetaling.insert(
                        UpsertBrukerUtbetalingDbo(
                            id = UUID.randomUUID(),
                            sakId = tilskuddVedtak.tilskuddsnummer.value,
                            transaksjonsDato = command.besluttetTidspunkt.tilNorskDato(),
                            belop = requireNotNull(tilskuddVedtak.utbetalingBelop?.belop) {
                                "utbetalingBelop var null"
                            },
                            tilskuddstype = tilskuddVedtak.tilskuddOpplaeringType.toHelVedTilskuddstype(),
                            saksbehandler = command.saksbehandler,
                            beslutter = command.beslutter,
                            besluttetTidspunkt = command.besluttetTidspunkt,
                            tiltakskode = gjennomforing.tiltakstype.tiltakskode.toHelVedTiltakskode(),
                            tilskuddVedtakId = tilskuddVedtak.id,
                        ),
                    )

                    publishHelVedUtbetaling(tilskuddVedtak.id, personIdent)
                }
            }
    }

    private fun opphorUtbetalingTilBruker(
        command: OpphorTilskuddUtbetaling,
        behandling: TilskuddBehandlingDto,
        personIdent: NorskIdent,
    ) {
        behandling.tilskuddVedtak
            .filter { it.vedtakResultat.type == VedtakResultat.INNVILGELSE }
            .filter { it.utbetalingMottaker is TilskuddMottaker.Bruker }
            .forEach { tilskuddVedtak ->
                db.transaction {
                    queries.tilskuddBehandling.acquireLockTilskudd(tilskuddVedtak.tilskuddId)
                    val tidligereUtbetaling = queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtak.id)
                    if (tidligereUtbetaling != null) {
                        logger.warn("Revurdering av utbetaling for tilskudd vedtak med id=${tilskuddVedtak.id} er allerede opprettet. Sak id=${tidligereUtbetaling.sakId}, behandling id=${tidligereUtbetaling.behandlingId}")
                        return@transaction
                    }
                    utbetalingTilOpphor(tilskuddVedtak, personIdent, command)
                }
            }
    }

    context(tx: TransactionalQueryContext)
    private fun utbetalingTilOpphor(
        tilskuddVedtak: TilskuddVedtakDto,
        personIdent: NorskIdent,
        command: OpphorTilskuddUtbetaling,
    ) = with(tx) {
        val forrigeUtbetaling = queries.brukerUtbetaling.getLastFromTilskudd(tilskuddVedtak.tilskuddId)

        requireNotNull(forrigeUtbetaling) {
            "Fant ikke tidligere utbetaling for tilskudd med id=${tilskuddVedtak.tilskuddId} som skal opphøres"
        }

        queries.brukerUtbetaling.insert(
            UpsertBrukerUtbetalingDbo(
                belop = tilskuddVedtak.utbetalingBelop!!.belop,
                saksbehandler = command.saksbehandler,
                beslutter = command.beslutter,
                besluttetTidspunkt = command.besluttetTidspunkt,
                tilskuddVedtakId = tilskuddVedtak.id,
                id = forrigeUtbetaling.id,
                sakId = forrigeUtbetaling.sakId,
                transaksjonsDato = forrigeUtbetaling.transaksjonsDato,
                tilskuddstype = forrigeUtbetaling.tilskuddstype,
                tiltakskode = forrigeUtbetaling.tiltakskode,
            ),
        )

        publishHelVedUtbetaling(tilskuddVedtak.id, personIdent)
    }

    context(tx: TransactionalQueryContext)
    private fun publishHelVedUtbetaling(tilskuddVedtakId: UUID, personIdent: NorskIdent) = with(tx) {
        val utbetaling = requireNotNull(queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtakId)) {
            "Fant ikke utbetaling for vedtak $tilskuddVedtakId"
        }
        outbox.publish(utbetaling.toHelVedUtbetaling(personIdent))
    }

    fun handleHelvedStatus(id: UUID, statusMelding: HelVedStatus) {
        logger.info("Melding fra hel ved: {}", Json.encodeToString(statusMelding))
        val behandlingIds = statusMelding.detaljer?.linjer
            ?.mapNotNull { it.behandlingId.toIntOrNull() }
            ?.toNonEmptySetOrThrow()
        requireNotNull(behandlingIds) { "Mottok statusmelding fra hel ved uten behandlingId(er)" }
        db.session {
            queries.brukerUtbetaling.setHelVedStatus(id, behandlingIds, statusMelding)
        }
    }

    fun produceTilskuddUtbetaling(utbetaling: HelVedUtbetaling) {
        val record = ProducerRecord(
            config.helvedUtbetalingTopic,
            utbetaling.id.toString().toByteArray(),
            Json.encodeToString(HelVedUtbetaling.serializer(), utbetaling).toByteArray(),
        )
        logger.info("Produserer utbetaling id=${utbetaling.id} mot Hel Ved på ${config.helvedUtbetalingTopic}")

        kafkaProducerClient.sendSync(record)
    }

    fun produceTilskuddUtbetalingTest(): UUID? {
        if (NaisEnv.current().isProdGCP()) {
            logger.info("Ignorerer HelVedUtbetaling task for Prod")
            return null
        }
        val utbetaling = HelVedUtbetaling(
            id = UUID.randomUUID(),
            sakId = "2026/test",
            behandlingId = "1",
            personIdent = NorskIdent("21528416400"), // Gjørme, Proaktiv
            periode = HelVedUtbetaling.Periode(LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30)),
            belop = 1234,
            kostnadssted = NavEnhetNummer("1234"),
            tilskuddstype = HelVedUtbetaling.Tilskuddstype.EKSAMENSGEBYR,
            saksbehandler = NavIdent("Z990279"), // Test NavIdent
            beslutter = NavIdent("Z993433"), // Test NavIdent
            besluttetTidspunkt = Instant.now(),
            tiltakskode = HelVedUtbetaling.Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV,
            dryrun = false,
        )
        produceTilskuddUtbetaling(utbetaling)

        return utbetaling.id
    }
}

private fun BrukerUtbetalingDbo.toHelVedUtbetaling(
    personIdent: NorskIdent,
    simulering: Boolean = false,
): HelVedUtbetaling = HelVedUtbetaling(
    id = id,
    sakId = sakId,
    behandlingId = behandlingId.toString(),
    personIdent = personIdent,
    periode = HelVedUtbetaling.Periode(transaksjonsDato, transaksjonsDato),
    belop = belop,
    kostnadssted = kostnadssted.enhetsnummer,
    tilskuddstype = tilskuddstype,
    tiltakskode = tiltakskode,
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    besluttetTidspunkt = besluttetTidspunkt,
    dryrun = simulering,
)

private fun Tiltakskode.toHelVedTiltakskode(): HelVedUtbetaling.Tiltakskode = when (this) {
    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING -> HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING
    Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING -> HelVedUtbetaling.Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING
    Tiltakskode.ARBEIDSMARKEDSOPPLAERING -> HelVedUtbetaling.Tiltakskode.ARBEIDSMARKEDSOPPLAERING
    Tiltakskode.FAG_OG_YRKESOPPLAERING -> HelVedUtbetaling.Tiltakskode.FAG_OG_YRKESOPPLAERING
    Tiltakskode.HOYERE_UTDANNING -> HelVedUtbetaling.Tiltakskode.HOYERE_UTDANNING
    Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING -> HelVedUtbetaling.Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING
    Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV -> HelVedUtbetaling.Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV
    Tiltakskode.STUDIESPESIALISERING -> HelVedUtbetaling.Tiltakskode.STUDIESPESIALISERING
    else -> throw IllegalStateException("Tiltakstype $this ikke støttet for utbetaling av tilskudd til bruker")
}

private fun Opplaeringtilskudd.Kode.toHelVedTilskuddstype(): HelVedUtbetaling.Tilskuddstype = when (this) {
    Opplaeringtilskudd.Kode.SKOLEPENGER -> HelVedUtbetaling.Tilskuddstype.SKOLEPENGER
    Opplaeringtilskudd.Kode.STUDIEREISE -> HelVedUtbetaling.Tilskuddstype.STUDIEREISE
    Opplaeringtilskudd.Kode.EKSAMENSGEBYR -> HelVedUtbetaling.Tilskuddstype.EKSAMENSGEBYR
    Opplaeringtilskudd.Kode.SEMESTERAVGIFT -> HelVedUtbetaling.Tilskuddstype.SEMESTERAVGIFT
    Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD -> HelVedUtbetaling.Tilskuddstype.INTEGRERT_BOTILBUD
}
