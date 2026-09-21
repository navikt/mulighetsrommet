package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.brukerutbetaling.BrukerUtbetalingService
import no.nav.mulighetsrommet.api.brukerutbetaling.db.BrukerUtbetalingDbo
import no.nav.mulighetsrommet.api.brukerutbetaling.db.UpsertBrukerUtbetalingDbo
import no.nav.mulighetsrommet.api.contracts.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.contracts.totrinnskontroll.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingEnkeltplass
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.totrinnskontroll.kafka.TotrinnskontrollHendelseDeserializer
import no.nav.mulighetsrommet.api.utbetaling.service.Personalia
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.api.utils.DatoUtils.tilNorskDato
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.UUID

class TilskuddBrukerUtbetalingConsumer(
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
    private val brukerUtbetalingService: BrukerUtbetalingService,
) : KafkaTopicConsumer<UUID, TotrinnskontrollHendelse>(
    uuidDeserializer(),
    TotrinnskontrollHendelseDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: TotrinnskontrollHendelse) {
        if (message.status != TotrinnskontrollHendelse.Status.GODKJENT) {
            return
        }
        if (!listOf(
                TotrinnskontrollType.TILSKUDD_OPPRETTELSE,
                TotrinnskontrollType.TILSKUDD_OPPHOR,
            ).contains(message.type)
        ) {
            return
        }

        val behandling = db.session { queries.tilskuddBehandling.get(key) }
            ?: throw IllegalStateException("Fant ikke attestert tilskudd_behandling id=$key")
        utbetalTilskuddTilBruker(behandling, message)
    }

    private suspend fun utbetalTilskuddTilBruker(
        behandling: TilskuddBehandlingDto,
        totrinnskontroll: TotrinnskontrollHendelse,
    ) {
        val (gjennomforing, deltaker) = db.session {
            val gjennomforing = queries.gjennomforing.getGjennomforingEnkeltplassOrError(behandling.gjennomforingId)
            val deltaker = getDeltaker(gjennomforing.id)
            gjennomforing to deltaker
        }
        val personalia = personaliaService.getPersonalia(deltaker.id, PersonaliaService.OnBehalfOf.System)

        when (behandling.type) {
            TilskuddBehandlingType.REGISTRERING -> nyUtbetalingTilBruker(
                behandling,
                totrinnskontroll,
                gjennomforing,
                personalia,
            )

            TilskuddBehandlingType.REVURDERING -> revurderUtbetalingTilBruker(
                behandling,
                totrinnskontroll,
                personalia,
            )
        }
    }

    private fun revurderUtbetalingTilBruker(
        behandling: TilskuddBehandlingDto,
        totrinnskontroll: TotrinnskontrollHendelse,
        brukerPersonalia: Personalia,
    ) {
        val saksbehandler = (totrinnskontroll.behandletAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
            ?: error("behandletAv must be NavAnsatt")
        val beslutter = (totrinnskontroll.besluttetAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
            ?: error("besluttetAv must be NavAnsatt")
        val besluttetTidspunkt = requireNotNull(totrinnskontroll.besluttetTidspunkt)

        behandling.tilskudd
            .filter { it.vedtakResultat.type == VedtakResultat.INNVILGELSE }
            .filter { it.utbetalingMottaker == TilskuddMottaker.BRUKER }
            .forEach { tilskudd ->
                if (totrinnskontroll.type != TotrinnskontrollType.TILSKUDD_OPPHOR) {
                    throw IllegalStateException("Revurdering av tilskudd med type ${totrinnskontroll.type} støttes ikke for utbetaling til bruker")
                }
                db.transaction {
                    // Idempotency check
                    queries.tilskuddBehandling.acquireLockTilskudd(tilskudd.tilskuddId)
                    val tidligereUtbetaling = queries.brukerUtbetaling.getByTilskuddVedtak(tilskudd.id)
                    if (tidligereUtbetaling != null) {
                        logger.warn("Revurdering av utbetaling for tilskudd vedtak med id=${tilskudd.id} er allerede opprettet. Sak id=${tidligereUtbetaling.sakId}, behandling id=${tidligereUtbetaling.behandlingId}")
                        return@transaction
                    }
                    require(queries.brukerUtbetaling.getLastFromTilskudd(tilskudd.tilskuddId) != null) {
                        "Forventet å kunne revurdere utbetaling for tilskuddId=${tilskudd.tilskuddId}, men det finnes ingen tidligere utbetalinger"
                    }

                    utbetalingTilOpphor(tilskudd, brukerPersonalia, saksbehandler, beslutter, besluttetTidspunkt)
                }
            }
    }

    private fun nyUtbetalingTilBruker(
        behandling: TilskuddBehandlingDto,
        totrinnskontroll: TotrinnskontrollHendelse,
        gjennomforing: GjennomforingEnkeltplass,
        brukerPersonalia: Personalia,
    ) {
        val saksbehandler = (totrinnskontroll.behandletAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
            ?: error("behandletAv must be NavAnsatt")
        val beslutter = (totrinnskontroll.besluttetAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
            ?: error("besluttetAv must be NavAnsatt")

        behandling.tilskudd
            .filter { it.vedtakResultat.type == VedtakResultat.INNVILGELSE }
            .filter { it.utbetalingMottaker == TilskuddMottaker.BRUKER }
            .forEach { tilskuddVedtak ->
                db.transaction {
                    // Idempotency check
                    queries.tilskuddBehandling.acquireLockTilskudd(tilskuddVedtak.tilskuddId)
                    val tidligereUtbetaling = queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtak.id)
                    if (tidligereUtbetaling != null) {
                        logger.warn("Utbetaling for tilskudd vedtak med id=${tilskuddVedtak.id} er allerede opprettet. Sak id=${tidligereUtbetaling.sakId}, behandling id=${tidligereUtbetaling.behandlingId}")
                        return@transaction
                    }

                    val besluttetDato = requireNotNull(totrinnskontroll.besluttetTidspunkt)
                    queries.brukerUtbetaling.insert(
                        UpsertBrukerUtbetalingDbo(
                            id = UUID.randomUUID(),
                            sakId = tilskuddVedtak.tilskuddsnummer,
                            transaksjonsDato = besluttetDato.tilNorskDato(),
                            belop = requireNotNull(tilskuddVedtak.utbetalingBelop?.belop) {
                                "utbetalingBelop var null"
                            },
                            tilskuddstype = tilskuddVedtak.tilskuddOpplaeringType.toHelVedTilskuddstype(),
                            saksbehandler = saksbehandler,
                            beslutter = beslutter,
                            besluttetTidspunkt = besluttetDato,
                            tiltakskode = gjennomforing.tiltakstype.tiltakskode.toHelVedTiltakskode(),
                            tilskuddVedtakId = tilskuddVedtak.id,
                        ),
                    )

                    val brukerUtbetaling = requireNotNull(queries.brukerUtbetaling.getByTilskuddVedtak(tilskuddVedtak.id))
                    brukerUtbetalingService.produceTilskuddUtbetaling(
                        brukerUtbetaling.toHelVedUtbetaling(
                            brukerPersonalia.norskIdent(),
                        ),
                    )
                }
            }
    }

    context(tx: TransactionalQueryContext)
    private fun utbetalingTilOpphor(
        tilskudd: TilskuddOpplaeringDto,
        brukerPersonalia: Personalia,
        saksbehandler: NavIdent,
        beslutter: NavIdent,
        besluttetTidspunkt: Instant,
    ) = with(tx) {
        val forrigeUtbetaling = queries.brukerUtbetaling.getLastFromTilskudd(tilskudd.tilskuddId)

        requireNotNull(forrigeUtbetaling) {
            "Fant ikke tidligere utbetaling for tilskudd med id=${tilskudd.tilskuddId} som skal opphøres"
        }

        queries.brukerUtbetaling.insert(
            UpsertBrukerUtbetalingDbo(
                belop = tilskudd.utbetalingBelop!!.belop,
                saksbehandler = saksbehandler,
                beslutter = beslutter,
                besluttetTidspunkt = besluttetTidspunkt,
                tilskuddVedtakId = tilskudd.id,
                id = forrigeUtbetaling.id,
                sakId = forrigeUtbetaling.sakId,
                transaksjonsDato = forrigeUtbetaling.transaksjonsDato,
                tilskuddstype = forrigeUtbetaling.tilskuddstype,
                tiltakskode = forrigeUtbetaling.tiltakskode,
            ),
        )

        val opphor = requireNotNull(queries.brukerUtbetaling.getByTilskuddVedtak(tilskudd.id))
        brukerUtbetalingService.produceTilskuddUtbetaling(
            opphor.toHelVedUtbetaling(brukerPersonalia.norskIdent()),
        )
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker {
        val deltakelser = repository.deltaker.getByGjennomforing(gjennomforingId)
        if (deltakelser.size != 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet akkurat én)")
        }
        return deltakelser.first()
    }
}

fun BrukerUtbetalingDbo.toHelVedUtbetaling(personIdent: NorskIdent?): HelVedUtbetaling = HelVedUtbetaling(
    id = id,
    sakId = sakId,
    behandlingId = behandlingId.toString(),
    personIdent = personIdent
        ?: throw IllegalStateException("Fant ikke norsk ident for bruker utbetaling med id=$id, sakId=$sakId, behandlingId=$behandlingId"),
    periode = HelVedUtbetaling.Periode(transaksjonsDato, transaksjonsDato),
    belop = belop,
    kostnadssted = kostnadssted.enhetsnummer,
    tilskuddstype = tilskuddstype,
    tiltakskode = tiltakskode,
    saksbehandler = saksbehandler,
    beslutter = beslutter,
    besluttetTidspunkt = besluttetTidspunkt,
    dryrun = false,
)

fun Tiltakskode.toHelVedTiltakskode(): HelVedUtbetaling.Tiltakskode = when (this) {
    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING ->
        HelVedUtbetaling.Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING

    Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING ->
        HelVedUtbetaling.Tiltakskode.ENKELTPLASS_FAG_OG_YRKESOPPLAERING

    Tiltakskode.ARBEIDSMARKEDSOPPLAERING ->
        HelVedUtbetaling.Tiltakskode.ARBEIDSMARKEDSOPPLAERING

    Tiltakskode.FAG_OG_YRKESOPPLAERING ->
        HelVedUtbetaling.Tiltakskode.FAG_OG_YRKESOPPLAERING

    Tiltakskode.HOYERE_UTDANNING ->
        HelVedUtbetaling.Tiltakskode.HOYERE_UTDANNING

    Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING ->
        HelVedUtbetaling.Tiltakskode.HOYERE_YRKESFAGLIG_UTDANNING

    Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV ->
        HelVedUtbetaling.Tiltakskode.NORSKOPPLAERING_GRUNNLEGGENDE_FERDIGHETER_FOV

    Tiltakskode.STUDIESPESIALISERING ->
        HelVedUtbetaling.Tiltakskode.STUDIESPESIALISERING

    else -> throw IllegalStateException("Tiltakstype $this ikke støttet for utbetaling av tilskudd til bruker")
}

fun Opplaeringtilskudd.Kode.toHelVedTilskuddstype(): HelVedUtbetaling.Tilskuddstype {
    return when (this) {
        Opplaeringtilskudd.Kode.SKOLEPENGER -> HelVedUtbetaling.Tilskuddstype.SKOLEPENGER
        Opplaeringtilskudd.Kode.STUDIEREISE -> HelVedUtbetaling.Tilskuddstype.STUDIEREISE
        Opplaeringtilskudd.Kode.EKSAMENSGEBYR -> HelVedUtbetaling.Tilskuddstype.EKSAMENSGEBYR
        Opplaeringtilskudd.Kode.SEMESTERAVGIFT -> HelVedUtbetaling.Tilskuddstype.SEMESTERAVGIFT
        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD -> HelVedUtbetaling.Tilskuddstype.INTEGRERT_BOTILBUD
    }
}
