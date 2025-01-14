package no.nav.mulighetsrommet.api.fixtures

import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.GjennomforingDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerDbo
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeDbo

data class MulighetsrommetTestDomain(
    val enheter: List<NavEnhetDbo> = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
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
    val refusjonskrav: List<RefusjonskravDbo> = listOf(),
    val additionalSetup: (QueryContext.() -> Unit)? = null,
) {
    fun initialize(database: ApiDatabase): MulighetsrommetTestDomain = database.transaction {
        enheter.forEach { queries.enhet.upsert(it) }
        ansatte.forEach { queries.ansatt.upsert(it) }
        arrangorer.forEach { queries.arrangor.upsert(it) }
        arrangorKontaktpersoner.forEach { queries.arrangor.upsertKontaktperson(it) }
        tiltakstyper.forEach { queries.tiltakstype.upsert(it) }
        avtaler.forEach { queries.avtale.upsert(it) }
        gjennomforinger.forEach { queries.gjennomforing.upsert(it) }
        deltakere.forEach { queries.deltaker.upsert(it) }
        refusjonskrav.forEach { queries.refusjonskrav.upsert(it) }

        additionalSetup?.invoke(this)

        this@MulighetsrommetTestDomain
    }

    fun setup(session: TransactionalSession): MulighetsrommetTestDomain {
        val context = QueryContext(session)

        with(context) {
            enheter.forEach { queries.enhet.upsert(it) }
            ansatte.forEach { queries.ansatt.upsert(it) }
            arrangorer.forEach { queries.arrangor.upsert(it) }
            arrangorKontaktpersoner.forEach { queries.arrangor.upsertKontaktperson(it) }
            tiltakstyper.forEach { queries.tiltakstype.upsert(it) }
            avtaler.forEach { queries.avtale.upsert(it) }
            gjennomforinger.forEach { queries.gjennomforing.upsert(it) }
            deltakere.forEach { queries.deltaker.upsert(it) }
            refusjonskrav.forEach { queries.refusjonskrav.upsert(it) }
        }

        additionalSetup?.invoke(context)

        return this
    }
}
