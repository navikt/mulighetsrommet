package no.nav.mulighetsrommet.api.fixtures

import kotliquery.TransactionalSession
import no.nav.mulighetsrommet.api.ApiDatabase
import no.nav.mulighetsrommet.api.QueryContext
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerDbo
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeDbo
import no.nav.mulighetsrommet.database.Database

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
    val gjennomforinger: List<TiltaksgjennomforingDbo> = listOf(),
    val deltakere: List<DeltakerDbo> = listOf(),
    val refusjonskrav: List<RefusjonskravDbo> = listOf(),
    val additionalSetup: (QueryContext.() -> Unit)? = null,
) {
    fun initialize(database: Database) = database.transaction { session ->
        val context = QueryContext(session)

        with(context) {
            enheter.forEach { Queries.enhet.upsert(it) }
            ansatte.forEach { Queries.ansatt.upsert(it) }
            arrangorer.forEach { Queries.arrangor.upsert(it) }
            arrangorKontaktpersoner.forEach { Queries.arrangor.upsertKontaktperson(it) }
            tiltakstyper.forEach { Queries.tiltakstype.upsert(it) }
            avtaler.forEach { Queries.avtale.upsert(it) }
            gjennomforinger.forEach { Queries.gjennomforing.upsert(it) }
            deltakere.forEach { Queries.deltaker.upsert(it) }
            refusjonskrav.forEach { Queries.refusjonskrav.upsert(it) }
        }

        additionalSetup?.invoke(context)
    }

    fun initialize(database: ApiDatabase) = database.tx {
        enheter.forEach { Queries.enhet.upsert(it) }
        ansatte.forEach { Queries.ansatt.upsert(it) }
        arrangorer.forEach { Queries.arrangor.upsert(it) }
        arrangorKontaktpersoner.forEach { Queries.arrangor.upsertKontaktperson(it) }
        tiltakstyper.forEach { Queries.tiltakstype.upsert(it) }
        avtaler.forEach { Queries.avtale.upsert(it) }
        gjennomforinger.forEach { Queries.gjennomforing.upsert(it) }
        deltakere.forEach { Queries.deltaker.upsert(it) }
        refusjonskrav.forEach { Queries.refusjonskrav.upsert(it) }

        additionalSetup?.invoke(this)
    }

    fun setup(session: TransactionalSession): MulighetsrommetTestDomain {
        val context = QueryContext(session)

        with(context) {
            enheter.forEach { Queries.enhet.upsert(it) }
            ansatte.forEach { Queries.ansatt.upsert(it) }
            arrangorer.forEach { Queries.arrangor.upsert(it) }
            arrangorKontaktpersoner.forEach { Queries.arrangor.upsertKontaktperson(it) }
            tiltakstyper.forEach { Queries.tiltakstype.upsert(it) }
            avtaler.forEach { Queries.avtale.upsert(it) }
            gjennomforinger.forEach { Queries.gjennomforing.upsert(it) }
            deltakere.forEach { Queries.deltaker.upsert(it) }
            refusjonskrav.forEach { Queries.refusjonskrav.upsert(it) }
        }

        additionalSetup?.invoke(context)

        return this
    }

    fun teardown(session: TransactionalSession): MulighetsrommetTestDomain {
        with(QueryContext(session)) {
            deltakere.forEach { Queries.deltaker.delete(it.id) }
            gjennomforinger.forEach { Queries.gjennomforing.delete(it.id) }
            avtaler.forEach { Queries.avtale.delete(it.id) }
            arrangorer.forEach { Queries.arrangor.delete(it.organisasjonsnummer.value) }
            arrangorKontaktpersoner.forEach { Queries.arrangor.deleteKontaktperson(it.id) }
            ansatte.forEach { Queries.ansatt.getByNavIdent(it.navIdent) }
        }

        return this
    }
}
