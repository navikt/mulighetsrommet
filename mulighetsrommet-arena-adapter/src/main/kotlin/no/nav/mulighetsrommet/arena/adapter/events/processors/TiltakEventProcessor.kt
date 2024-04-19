package no.nav.mulighetsrommet.arena.adapter.events.processors

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.raise.either
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingError
import no.nav.mulighetsrommet.arena.adapter.models.ProcessingResult
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTable
import no.nav.mulighetsrommet.arena.adapter.models.arena.ArenaTiltak
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEntityMapping.Status.Handled
import no.nav.mulighetsrommet.arena.adapter.models.db.ArenaEvent
import no.nav.mulighetsrommet.arena.adapter.models.db.Tiltakstype
import no.nav.mulighetsrommet.arena.adapter.services.ArenaEntityService
import no.nav.mulighetsrommet.arena.adapter.utils.ArenaUtils
import java.util.*

class TiltakEventProcessor(
    private val entities: ArenaEntityService,
) : ArenaEventProcessor {
    override val arenaTable: ArenaTable = ArenaTable.Tiltakstype

    override suspend fun handleEvent(event: ArenaEvent) = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()

        event.decodePayload<ArenaTiltak>()
            .toTiltakstype(mapping.entityId)
            .flatMap { entities.upsertTiltakstype(it) }
            .map { ProcessingResult(Handled) }
            .bind()
    }

    override suspend fun deleteEntity(event: ArenaEvent): Either<ProcessingError, Unit> = either {
        val mapping = entities.getMapping(event.arenaTable, event.arenaId).bind()
        entities.deleteTiltakstype(mapping.entityId).bind()
    }

    private fun ArenaTiltak.toTiltakstype(id: UUID) = Either
        .catch {
            Tiltakstype(
                id = id,
                navn = TILTAKSNAVN,
                tiltaksgruppekode = TILTAKSGRUPPEKODE,
                tiltakskode = TILTAKSKODE,
                registrertIArenaDato = ArenaUtils.parseTimestamp(REG_DATO),
                sistEndretIArenaDato = ArenaUtils.parseTimestamp(MOD_DATO),
                fraDato = ArenaUtils.parseTimestamp(DATO_FRA),
                tilDato = ArenaUtils.parseTimestamp(DATO_TIL),
                rettPaaTiltakspenger = ArenaUtils.parseJaNei(STATUS_BASISYTELSE),
                administrasjonskode = ADMINISTRASJONKODE,
                sendTilsagnsbrevTilDeltaker = ArenaUtils.parseJaNei(STATUS_KOPI_TILSAGN),
                tiltakstypeSkalHaAnskaffelsesprosess = ArenaUtils.parseJaNei(STATUS_ANSKAFFELSE),
                maksAntallPlasser = MAKS_ANT_PLASSER,
                maksAntallSokere = MAKS_ANT_SOKERE,
                harFastAntallPlasser = ArenaUtils.parseNulleableJaNei(STATUS_FAST_ANT_PLASSER),
                skalSjekkeAntallDeltakere = ArenaUtils.parseNulleableJaNei(STATUS_SJEKK_ANT_DELTAKERE),
                visLonnstilskuddskalkulator = ArenaUtils.parseJaNei(STATUS_KALKULATOR),
                rammeavtale = RAMMEAVTALE,
                opplaeringsgruppe = OPPLAERINGSGRUPPE,
                handlingsplan = HANDLINGSPLAN,
                tiltaksgjennomforingKreverSluttdato = ArenaUtils.parseJaNei(STATUS_SLUTTDATO),
                maksPeriodeIMnd = MAKS_PERIODE,
                tiltaksgjennomforingKreverMeldeplikt = ArenaUtils.parseNulleableJaNei(STATUS_MELDEPLIKT),
                tiltaksgjennomforingKreverVedtak = ArenaUtils.parseJaNei(STATUS_VEDTAK),
                tiltaksgjennomforingReservertForIABedrift = ArenaUtils.parseJaNei(STATUS_IA_AVTALE),
                harRettPaaTilleggsstonader = ArenaUtils.parseJaNei(STATUS_TILLEGGSSTONADER),
                harRettPaaUtdanning = ArenaUtils.parseJaNei(STATUS_UTDANNING),
                tiltaksgjennomforingGenererTilsagnsbrevAutomatisk = ArenaUtils.parseJaNei(AUTOMATISK_TILSAGNSBREV),
                visBegrunnelseForInnsoking = ArenaUtils.parseJaNei(STATUS_BEGRUNNELSE_INNSOKT),
                sendHenvisningsbrevOgHovedbrevTilArbeidsgiver = ArenaUtils.parseJaNei(STATUS_HENVISNING_BREV),
                sendKopibrevOgHovedbrevTilArbeidsgiver = ArenaUtils.parseJaNei(STATUS_KOPIBREV),
            )
        }
        .mapLeft { ProcessingError.ProcessingFailed(it.localizedMessage) }
}
