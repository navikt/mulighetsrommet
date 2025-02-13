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
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeDbo
import no.nav.mulighetsrommet.api.utbetaling.db.DeltakerDbo
import no.nav.mulighetsrommet.api.utbetaling.db.DelutbetalingDbo
import no.nav.mulighetsrommet.api.utbetaling.db.UtbetalingDbo

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
    val tilsagn: List<TilsagnDbo> = listOf(),
    val utbetalinger: List<UtbetalingDbo> = listOf(),
    val delutbetalinger: List<DelutbetalingDbo> = listOf(),
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
            tilsagn.forEach { queries.tilsagn.upsert(it) }
            utbetalinger.forEach { queries.utbetaling.upsert(it) }
            delutbetalinger.forEach { queries.delutbetaling.upsert(it) }
        }

        additionalSetup?.invoke(context)

        return this
    }
}
