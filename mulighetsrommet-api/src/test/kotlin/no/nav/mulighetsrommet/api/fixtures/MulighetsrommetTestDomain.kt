package no.nav.mulighetsrommet.api.fixtures

import kotliquery.Session
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.tilsagn.db.TilsagnDbo
import no.nav.mulighetsrommet.api.tilsagn.model.Besluttelse
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnDto
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeDbo
import no.nav.mulighetsrommet.api.totrinnskontroll.db.TotrinnskontrollType
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.model.DelutbetalingDto
import no.nav.mulighetsrommet.model.Periode

data class MulighetsrommetTestDomain(
    val navEnheter: List<NavEnhetDbo> = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
    val ansatte: List<NavAnsattDbo> = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
    val arrangorer: List<ArrangorDto> = listOf(
        ArrangorFixtures.hovedenhet,
        ArrangorFixtures.underenhet1,
        ArrangorFixtures.underenhet2,
    ),
    val arrangorKontaktpersoner: List<ArrangorKontaktperson> = listOf(),
    val tiltakstyper: List<TiltakstypeDbo> = listOf(
        TiltakstypeFixtures.Oppfolging,
        TiltakstypeFixtures.Arbeidstrening,
        TiltakstypeFixtures.VTA,
        TiltakstypeFixtures.Jobbklubb,
        TiltakstypeFixtures.AFT,
        TiltakstypeFixtures.EnkelAmo,
        TiltakstypeFixtures.GruppeFagOgYrkesopplaering,
        TiltakstypeFixtures.GruppeAmo,
        TiltakstypeFixtures.DigitalOppfolging,
    ),
    val avtaler: List<AvtaleDbo> = listOf(
        AvtaleFixtures.oppfolging,
        AvtaleFixtures.VTA,
        AvtaleFixtures.AFT,
        AvtaleFixtures.jobbklubb,
        AvtaleFixtures.EnkelAmo,
    ),
    val gjennomforinger: List<GjennomforingDbo> = listOf(),
    val deltakere: List<DeltakerDbo> = listOf(),
    val tilsagn: List<TilsagnDto> = listOf(),
    val utbetalinger: List<UtbetalingDbo> = listOf(),
    val delutbetalinger: List<DelutbetalingDto> = listOf(),
    val additionalSetup: (QueryContext.() -> Unit)? = null,
) {
    fun initialize(database: ApiDatabase): MulighetsrommetTestDomain = database.transaction {
        setup(session)
    }

    fun setup(session: Session): MulighetsrommetTestDomain {
        val context = QueryContext(session)

        with(context) {
            navEnheter.forEach { queries.enhet.upsert(it) }
            ansatte.forEach { queries.ansatt.upsert(it) }
            arrangorer.forEach { queries.arrangor.upsert(it) }
            arrangorKontaktpersoner.forEach { queries.arrangor.upsertKontaktperson(it) }
            tiltakstyper.forEach { queries.tiltakstype.upsert(it) }
            avtaler.forEach { queries.avtale.upsert(it) }
            gjennomforinger.forEach { queries.gjennomforing.upsert(it) }
            deltakere.forEach { queries.deltaker.upsert(it) }
            tilsagn.forEach { insertTilsagnDto(it) }
            utbetalinger.forEach { queries.utbetaling.upsert(it) }
            delutbetalinger.forEach { insertDelutbetalingDto(it) }
        }

        additionalSetup?.invoke(context)

        return this
    }
}

fun QueryContext.insertDelutbetalingDto(dto: DelutbetalingDto) {
    queries.delutbetaling.upsert(dto.toDbo())
    queries.totrinnskontroll.behandler(
        entityId = dto.id,
        navIdent = dto.opprettetAv,
        aarsaker = null,
        forklaring = null,
        type = TotrinnskontrollType.OPPRETT,
        tidspunkt = dto.opprettetTidspunkt,
    )

    when (dto) {
        is DelutbetalingDto.DelutbetalingAvvist ->
            queries.totrinnskontroll.beslutter(
                entityId = dto.id,
                navIdent = dto.besluttetAv,
                besluttelse = Besluttelse.AVVIST,
                aarsaker = dto.aarsaker,
                forklaring = dto.forklaring,
                type = TotrinnskontrollType.OPPRETT,
                tidspunkt = dto.besluttetTidspunkt,
            )
        is DelutbetalingDto.DelutbetalingUtbetalt ->
            queries.totrinnskontroll.beslutter(
                entityId = dto.id,
                navIdent = dto.besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                aarsaker = null,
                forklaring = null,
                type = TotrinnskontrollType.OPPRETT,
                tidspunkt = dto.besluttetTidspunkt,
            )
        is DelutbetalingDto.DelutbetalingOverfortTilUtbetaling ->
            queries.totrinnskontroll.beslutter(
                entityId = dto.id,
                navIdent = dto.besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                aarsaker = null,
                forklaring = null,
                type = TotrinnskontrollType.OPPRETT,
                tidspunkt = dto.besluttetTidspunkt,
            )
        is DelutbetalingDto.DelutbetalingTilGodkjenning -> {}
    }
}

fun QueryContext.insertTilsagnDto(tilsagnDto: TilsagnDto) {
    queries.tilsagn.upsert(tilsagnDto.toDbo())
    queries.totrinnskontroll.behandler(
        entityId = tilsagnDto.id,
        navIdent = tilsagnDto.status.opprettelse.behandletAv,
        aarsaker = null,
        forklaring = null,
        type = TotrinnskontrollType.OPPRETT,
        tidspunkt = tilsagnDto.status.opprettelse.behandletTidspunkt,
    )

    when (val status = tilsagnDto.status) {
        is TilsagnDto.TilsagnStatusDto.Annullert -> {
            queries.totrinnskontroll.beslutter(
                entityId = tilsagnDto.id,
                navIdent = status.opprettelse.besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                aarsaker = null,
                forklaring = null,
                type = TotrinnskontrollType.OPPRETT,
                tidspunkt = status.opprettelse.besluttetTidspunkt,
            )
            queries.totrinnskontroll.behandler(
                entityId = tilsagnDto.id,
                navIdent = status.annullering.behandletAv,
                aarsaker = status.annullering.aarsaker,
                forklaring = status.annullering.forklaring,
                type = TotrinnskontrollType.ANNULLER,
                tidspunkt = status.annullering.behandletTidspunkt,
            )
            queries.totrinnskontroll.beslutter(
                entityId = tilsagnDto.id,
                navIdent = status.annullering.besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                aarsaker = null,
                forklaring = null,
                type = TotrinnskontrollType.ANNULLER,
                tidspunkt = status.annullering.besluttetTidspunkt,
            )
        }
        is TilsagnDto.TilsagnStatusDto.Godkjent ->
            queries.totrinnskontroll.beslutter(
                entityId = tilsagnDto.id,
                navIdent = status.opprettelse.besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                aarsaker = null,
                forklaring = null,
                type = TotrinnskontrollType.OPPRETT,
                tidspunkt = status.opprettelse.besluttetTidspunkt,
            )
        is TilsagnDto.TilsagnStatusDto.Returnert ->
            queries.totrinnskontroll.beslutter(
                entityId = tilsagnDto.id,
                navIdent = status.opprettelse.besluttetAv,
                besluttelse = Besluttelse.AVVIST,
                aarsaker = status.opprettelse.aarsaker,
                forklaring = status.opprettelse.forklaring,
                type = TotrinnskontrollType.OPPRETT,
                tidspunkt = status.opprettelse.besluttetTidspunkt,
            )
        is TilsagnDto.TilsagnStatusDto.TilAnnullering -> {
            queries.totrinnskontroll.beslutter(
                entityId = tilsagnDto.id,
                navIdent = status.opprettelse.besluttetAv,
                besluttelse = Besluttelse.GODKJENT,
                aarsaker = null,
                forklaring = null,
                type = TotrinnskontrollType.OPPRETT,
                tidspunkt = status.opprettelse.besluttetTidspunkt,
            )
            queries.totrinnskontroll.behandler(
                entityId = tilsagnDto.id,
                navIdent = status.annullering.behandletAv,
                aarsaker = status.annullering.aarsaker,
                forklaring = status.annullering.forklaring,
                type = TotrinnskontrollType.ANNULLER,
                tidspunkt = status.annullering.behandletTidspunkt,
            )
        }
        is TilsagnDto.TilsagnStatusDto.TilGodkjenning -> {}
    }
}

fun TilsagnDto.toDbo(): TilsagnDbo {
    return TilsagnDbo(
        id = id,
        gjennomforingId = gjennomforing.id,
        type = type,
        periode = Periode.fromInclusiveDates(periodeStart, periodeSlutt),
        lopenummer = lopenummer,
        bestillingsnummer = bestillingsnummer,
        kostnadssted = kostnadssted.enhetsnummer,
        beregning = beregning,
        arrangorId = arrangor.id,
        endretAv = status.opprettelse.behandletAv,
        endretTidspunkt = status.opprettelse.behandletTidspunkt,
    )
}

fun DelutbetalingDto.toDbo(): DelutbetalingDbo {
    return DelutbetalingDbo(
        id = id,
        tilsagnId = tilsagnId,
        utbetalingId = utbetalingId,
        belop = belop,
        periode = periode,
        lopenummer = lopenummer,
        fakturanummer = fakturanummer,
        opprettetAv = opprettetAv,
    )
}
