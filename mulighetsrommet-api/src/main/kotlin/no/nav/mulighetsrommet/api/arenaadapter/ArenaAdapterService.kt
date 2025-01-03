package no.nav.mulighetsrommet.api.arenaadapter

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.Queries
import no.nav.mulighetsrommet.api.arrangor.ArrangorService
import no.nav.mulighetsrommet.api.avtale.model.AvtaleDto
import no.nav.mulighetsrommet.api.clients.brreg.BrregError
import no.nav.mulighetsrommet.api.gjennomforing.kafka.SisteTiltaksgjennomforingerV1KafkaProducer
import no.nav.mulighetsrommet.api.gjennomforing.model.TiltaksgjennomforingDto
import no.nav.mulighetsrommet.api.services.DocumentClass
import no.nav.mulighetsrommet.api.services.EndretAv
import no.nav.mulighetsrommet.api.services.EndringshistorikkService
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.database.Database
import no.nav.mulighetsrommet.domain.Tiltakskoder
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering.TiltaksgjennomforingSluttDatoCutoffDate
import no.nav.mulighetsrommet.domain.dbo.ArenaAvtaleDbo
import no.nav.mulighetsrommet.domain.dbo.ArenaTiltaksgjennomforingDbo
import no.nav.mulighetsrommet.domain.dto.Organisasjonsnummer
import org.slf4j.LoggerFactory
import java.util.*

class ArenaAdapterService(
    private val db: Database,
    private val tiltaksgjennomforingKafkaProducer: SisteTiltaksgjennomforingerV1KafkaProducer,
    private val sanityService: SanityService,
    private val arrangorService: ArrangorService,
    private val endringshistorikk: EndringshistorikkService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun upsertAvtale(avtale: ArenaAvtaleDbo): AvtaleDto = db.tx {
        syncArrangorFromBrreg(Organisasjonsnummer(avtale.arrangorOrganisasjonsnummer))

        val previous = Queries.avtale.get(avtale.id)
        if (previous?.toArenaAvtaleDbo() == avtale) {
            return@tx previous
        }

        Queries.avtale.upsertArenaAvtale(avtale)

        val next = requireNotNull(Queries.avtale.get(avtale.id))

        logUpdateAvtale(next)

        next
    }

    suspend fun upsertTiltaksgjennomforing(arenaGjennomforing: ArenaTiltaksgjennomforingDbo): UUID? = db.session {
        val tiltakstype = Queries.tiltakstype.get(arenaGjennomforing.tiltakstypeId)
            ?: throw IllegalStateException("Ukjent tiltakstype id=${arenaGjennomforing.tiltakstypeId}")

        syncArrangorFromBrreg(Organisasjonsnummer(arenaGjennomforing.arrangorOrganisasjonsnummer))

        if (Tiltakskoder.isEgenRegiTiltak(tiltakstype.arenaKode)) {
            upsertEgenRegiTiltak(tiltakstype, arenaGjennomforing)
        } else {
            upsertGruppetiltak(tiltakstype, arenaGjennomforing)
            null
        }
    }

    suspend fun removeSanityTiltaksgjennomforing(sanityId: UUID) {
        sanityService.deleteSanityTiltaksgjennomforing(sanityId)
    }

    private suspend fun upsertEgenRegiTiltak(
        tiltakstype: TiltakstypeDto,
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
    ): UUID? {
        require(Tiltakskoder.isEgenRegiTiltak(tiltakstype.arenaKode)) {
            "Gjennomføring for tiltakstype ${tiltakstype.arenaKode} skal ikke skrives til Sanity"
        }

        val sluttDato = arenaGjennomforing.sluttDato
        return if (sluttDato == null || sluttDato.isAfter(TiltaksgjennomforingSluttDatoCutoffDate)) {
            sanityService.createOrPatchSanityTiltaksgjennomforing(arenaGjennomforing, tiltakstype.sanityId)
        } else {
            null
        }
    }

    private fun upsertGruppetiltak(
        tiltakstype: TiltakstypeDto,
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
    ): Unit = db.tx {
        require(Tiltakskoder.isGruppetiltak(tiltakstype.arenaKode)) {
            "Gjennomføringer er ikke støttet for tiltakstype ${tiltakstype.arenaKode}"
        }

        val previous = requireNotNull(Queries.gjennomforing.get(arenaGjennomforing.id)) {
            "Alle gruppetiltak har blitt migrert. Forventet å finne gjennomføring i databasen."
        }

        if (!hasRelevantChanges(arenaGjennomforing, previous)) {
            logger.info("Gjennomføring hadde ingen endringer")
            return@tx
        }

        Queries.gjennomforing.updateArenaData(
            arenaGjennomforing.id,
            arenaGjennomforing.tiltaksnummer,
            arenaGjennomforing.arenaAnsvarligEnhet,
        )

        val next = requireNotNull(Queries.gjennomforing.get(arenaGjennomforing.id)) {
            "Gjennomføring burde ikke være null siden den nettopp ble lagt til"
        }

        if (previous.tiltaksnummer == null) {
            logTiltaksnummerHentetFraArena(next)
        } else {
            logUpdateGjennomforing(next)
        }

        tiltaksgjennomforingKafkaProducer.publish(next.toTiltaksgjennomforingV1Dto())
    }

    private suspend fun syncArrangorFromBrreg(orgnr: Organisasjonsnummer) {
        arrangorService.getOrSyncArrangorFromBrreg(orgnr)
            .onLeft { error ->
                if (error == BrregError.NotFound) {
                    logger.warn("Virksomhet mer orgnr=$orgnr finnes ikke i brreg. Er dette en utenlandsk arrangør?")
                }

                throw IllegalArgumentException("Klarte ikke hente virksomhet med orgnr=$orgnr fra brreg: $error")
            }
    }

    private fun hasRelevantChanges(
        arenaGjennomforing: ArenaTiltaksgjennomforingDbo,
        current: TiltaksgjennomforingDto,
    ): Boolean {
        return arenaGjennomforing.tiltaksnummer != current.tiltaksnummer || arenaGjennomforing.arenaAnsvarligEnhet != current.arenaAnsvarligEnhet?.enhetsnummer
    }

    private fun TransactionalSession.logUpdateAvtale(dto: AvtaleDto) {
        endringshistorikk.logEndring(
            this@TransactionalSession,
            DocumentClass.AVTALE,
            "Endret i Arena",
            EndretAv.Arena,
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun TransactionalSession.logUpdateGjennomforing(dto: TiltaksgjennomforingDto) {
        endringshistorikk.logEndring(
            this@TransactionalSession,
            DocumentClass.TILTAKSGJENNOMFORING,
            "Endret i Arena",
            EndretAv.Arena,
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }

    private fun TransactionalSession.logTiltaksnummerHentetFraArena(dto: TiltaksgjennomforingDto) {
        endringshistorikk.logEndring(
            this@TransactionalSession,
            DocumentClass.TILTAKSGJENNOMFORING,
            "Oppdatert med tiltaksnummer fra Arena",
            EndretAv.System,
            dto.id,
        ) { Json.encodeToJsonElement(dto) }
    }
}
