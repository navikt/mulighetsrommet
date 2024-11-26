package no.nav.mulighetsrommet.api.fixtures

import io.kotest.assertions.arrow.core.shouldBeRight
import no.nav.mulighetsrommet.api.arrangor.db.ArrangorRepository
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorKontaktperson
import no.nav.mulighetsrommet.api.avtale.db.AvtaleDbo
import no.nav.mulighetsrommet.api.avtale.db.AvtaleRepository
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.gjennomforing.db.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattDbo
import no.nav.mulighetsrommet.api.navansatt.db.NavAnsattRepository
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetRepository
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerDbo
import no.nav.mulighetsrommet.api.refusjon.db.DeltakerRepository
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeDbo
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
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
) {
    fun initialize(database: Database) {
        NavEnhetRepository(database).also { repository ->
            enheter.forEach { repository.upsert(it).shouldBeRight() }
        }

        NavAnsattRepository(database).also { repository ->
            ansatte.forEach { repository.upsert(it) }
        }

        ArrangorRepository(database).also { repository ->
            arrangorer.forEach { repository.upsert(it) }
            arrangorKontaktpersoner.forEach { repository.upsertKontaktperson(it) }
        }

        TiltakstypeRepository(database).also { repository ->
            tiltakstyper.forEach { repository.upsert(it) }
        }

        AvtaleRepository(database).also { repository ->
            avtaler.forEach { repository.upsert(it) }
        }

        TiltaksgjennomforingRepository(database).also { repository ->
            gjennomforinger.forEach { repository.upsert(it) }
        }

        DeltakerRepository(database).also { repository ->
            deltakere.forEach { repository.upsert(it) }
        }

        RefusjonskravRepository(database).also { repository ->
            refusjonskrav.forEach { repository.upsert(it) }
        }
    }
}
