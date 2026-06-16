package no.nav.mulighetsrommet.api.tilskuddbehandling.kafka

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import no.nav.common.kafka.consumer.util.deserializer.Deserializers.uuidDeserializer
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.brukerutbetaling.BrukerUtbetalingService
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling
import no.nav.mulighetsrommet.api.clients.helved.HelVedUtbetaling.Periode
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingDto
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollAgent
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollBesluttelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollHendelse
import no.nav.mulighetsrommet.api.totrinnskontroll.model.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.model.Deltaker
import no.nav.mulighetsrommet.api.utbetaling.service.PersonaliaService
import no.nav.mulighetsrommet.kafka.KafkaTopicConsumer
import no.nav.mulighetsrommet.kafka.serialization.JsonElementDeserializer
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.serialization.json.JsonIgnoreUnknownKeys
import org.slf4j.LoggerFactory
import java.util.UUID

class TilskuddBrukerUtbetalingConsumer(
    private val db: ApiDatabase,
    private val personaliaService: PersonaliaService,
    private val brukerUtbetalingService: BrukerUtbetalingService,
) : KafkaTopicConsumer<UUID, JsonElement>(
    uuidDeserializer(),
    JsonElementDeserializer(),
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun consume(key: UUID, message: JsonElement) {
        val totrinnskontrollHendelse = JsonIgnoreUnknownKeys.decodeFromJsonElement<TotrinnskontrollHendelse?>(message)
        if (totrinnskontrollHendelse == null) {
            logger.warn("Mottok tombstone for totrinnskontroll med key=$key")
            return
        }

        if (totrinnskontrollHendelse.type != TotrinnskontrollType.TILSKUDD_OPPRETTELSE) {
            return
        }
        if (totrinnskontrollHendelse.besluttelse != TotrinnskontrollBesluttelse.GODKJENT) {
            return
        }

        val behandling = db.session { queries.tilskuddBehandling.get(key) }
            ?: throw IllegalStateException("Fant ikke attestert tilskudd_behandling id=$key")

        utbetalTilskuddTilBruker(behandling, totrinnskontrollHendelse)
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

        val saksbehandler = (totrinnskontroll.behandletAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
            ?: error("behandletAv must be NavAnsatt")
        val beslutter = (totrinnskontroll.besluttetAv as? TotrinnskontrollAgent.NavAnsatt)?.navIdent
            ?: error("besluttetAv must be NavAnsatt")

        behandling.tilskudd
            .filter { it.vedtakResultat.type == VedtakResultat.INNVILGELSE }
            .filter { it.utbetalingMottaker == TilskuddMottaker.BRUKER }
            // Idempotency check
            .filter { db.session { queries.helvedUtbetaling.getByTilskudd(it.id) } == null }
            .forEach { t ->
                db.transaction {
                    val utbetaling = HelVedUtbetaling(
                        id = UUID.randomUUID(),
                        sakId = gjennomforing.lopenummer.value,
                        behandlingId = "1",
                        personIdent = requireNotNull(personalia.norskIdent()) {
                            "Norsk ident var null"
                        },
                        periode = Periode(behandling.periode.start, behandling.periode.getLastInclusiveDate()),
                        belop = requireNotNull(t.utbetalingBelop) {
                            "utbetalingBelop var null"
                        }.belop,
                        tilskuddstype = t.tilskuddOpplaeringType.toHelVedTilskuddstype(),
                        saksbehandler = NavIdent(saksbehandler),
                        beslutter = NavIdent(beslutter),
                        besluttetTidspunkt = requireNotNull(totrinnskontroll.besluttetTidspunkt),
                        tiltakskode = gjennomforing.tiltakstype.tiltakskode.toHelVedTiltakskode(),
                        dryrun = false,
                    )
                    queries.helvedUtbetaling.insert(utbetaling)

                    queries.tilskuddBehandling.setBrukerUtbetaling(t.id, utbetaling.id)

                    brukerUtbetalingService.produceTilskuddUtbetaling(utbetaling)
                }
            }
    }

    private fun QueryContext.getDeltaker(gjennomforingId: UUID): Deltaker {
        val deltakelser = queries.deltaker.getByGjennomforingId(gjennomforingId)
        if (deltakelser.size != 1) {
            error("Enkeltplass med id=$gjennomforingId har ${deltakelser.size} antall deltakere (forventet akkurat én)")
        }
        return deltakelser.first()
    }
}

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
        Opplaeringtilskudd.Kode.EKSAMENSAVGIFT -> HelVedUtbetaling.Tilskuddstype.EKSAMENSGEBYR
        Opplaeringtilskudd.Kode.SEMESTERAVGIFT -> HelVedUtbetaling.Tilskuddstype.SEMESTERAVGIFT
        Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD -> HelVedUtbetaling.Tilskuddstype.INTEGRERT_BOTILBUD
    }
}
