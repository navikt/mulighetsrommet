package no.nav.mulighetsrommet.api.gjennomforing.service

import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingArenaDataDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingType
import no.nav.mulighetsrommet.api.gjennomforing.mapper.TiltaksgjennomforingV2Mapper
import no.nav.mulighetsrommet.api.gjennomforing.model.Gjennomforing
import no.nav.mulighetsrommet.api.gjennomforing.model.GjennomforingArena
import no.nav.mulighetsrommet.model.GjennomforingOppstartstype
import no.nav.mulighetsrommet.model.GjennomforingPameldingType
import no.nav.mulighetsrommet.model.GjennomforingStatusType
import no.nav.mulighetsrommet.model.Tiltaksnummer
import java.time.LocalDate
import java.util.UUID

data class OpprettGjennomforingArena(
    val id: UUID,
    val tiltakstypeId: UUID,
    val arrangorId: UUID,
    val navn: String,
    val startDato: LocalDate,
    val sluttDato: LocalDate?,
    val status: GjennomforingStatusType,
    val deltidsprosent: Double,
    val antallPlasser: Int,
    val arenaTiltaksnummer: Tiltaksnummer?,
    val arenaAnsvarligEnhet: String?,
    val oppstart: GjennomforingOppstartstype,
    val pameldingType: GjennomforingPameldingType,
)

class GjennomforingArenaService(
    private val db: ApiDatabase,
) {
    fun upsert(opprett: OpprettGjennomforingArena): Unit = db.transaction {
        val previous = queries.gjennomforing.getGjennomforing(opprett.id)
        if (previous != null && !harGjennomforingEndringer(opprett, previous)) {
            return
        }

        val dbo = GjennomforingDbo(
            type = GjennomforingType.ARENA,
            id = opprett.id,
            tiltakstypeId = opprett.tiltakstypeId,
            arrangorId = opprett.arrangorId,
            navn = opprett.navn,
            startDato = opprett.startDato,
            sluttDato = opprett.sluttDato,
            status = opprett.status,
            deltidsprosent = opprett.deltidsprosent,
            antallPlasser = opprett.antallPlasser,
            arenaTiltaksnummer = opprett.arenaTiltaksnummer,
            arenaAnsvarligEnhet = opprett.arenaAnsvarligEnhet,
            oppstart = opprett.oppstart,
            pameldingType = opprett.pameldingType,
            ansvarligEnhet = null,
            avtaleId = null,
            prismodellId = null,
        )
        queries.gjennomforing.upsert(dbo)

        getOrError(dbo.id).also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    fun updateArenaData(id: UUID, arenadata: Gjennomforing.ArenaData): GjennomforingArena = db.transaction {
        val previous = getOrError(id)
        if (previous.arena == arenadata) {
            return previous
        }

        queries.gjennomforing.setArenaData(
            GjennomforingArenaDataDbo(
                id = id,
                tiltaksnummer = arenadata.tiltaksnummer,
                arenaAnsvarligEnhet = arenadata.ansvarligNavEnhet,
            ),
        )

        getOrError(id).also { publishTiltaksgjennomforingV2ToKafka(it) }
    }

    private fun QueryContext.getOrError(id: UUID): GjennomforingArena {
        return queries.gjennomforing.getGjennomforingArenaOrError(id)
    }

    private fun QueryContext.publishTiltaksgjennomforingV2ToKafka(gjennomforing: GjennomforingArena) {
        outbox.publish(gjennomforing.id, TiltaksgjennomforingV2Mapper.fromGjennomforingArena(gjennomforing))
    }
}

private fun harGjennomforingEndringer(opprett: OpprettGjennomforingArena, gjennomforing: Gjennomforing): Boolean {
    return gjennomforing !is GjennomforingArena || opprett != OpprettGjennomforingArena(
        id = gjennomforing.id,
        tiltakstypeId = gjennomforing.tiltakstype.id,
        arrangorId = gjennomforing.arrangor.id,
        navn = gjennomforing.navn,
        startDato = gjennomforing.startDato,
        sluttDato = gjennomforing.sluttDato,
        status = gjennomforing.status,
        deltidsprosent = gjennomforing.deltidsprosent,
        antallPlasser = gjennomforing.antallPlasser,
        arenaTiltaksnummer = gjennomforing.arena?.tiltaksnummer,
        arenaAnsvarligEnhet = gjennomforing.arena?.ansvarligNavEnhet,
        oppstart = gjennomforing.oppstart,
        pameldingType = gjennomforing.pameldingType,
    )
}
