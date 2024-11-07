package no.nav.mulighetsrommet.api.fixtures

import io.kotest.assertions.arrow.core.shouldBeRight
import no.nav.mulighetsrommet.api.domain.dbo.*
import no.nav.mulighetsrommet.api.domain.dto.ArrangorDto
import no.nav.mulighetsrommet.api.okonomi.refusjon.db.RefusjonskravDbo
import no.nav.mulighetsrommet.api.okonomi.refusjon.db.RefusjonskravRepository
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.Database

data class MulighetsrommetTestDomain(
    val enheter: List<NavEnhetDbo> = listOf(NavEnhetFixtures.IT, NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik),
    val ansatte: List<NavAnsattDbo> = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
    val arrangorer: List<ArrangorDto> = listOf(
        ArrangorFixtures.hovedenhet,
        ArrangorFixtures.underenhet1,
        ArrangorFixtures.underenhet2,
    ),
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
