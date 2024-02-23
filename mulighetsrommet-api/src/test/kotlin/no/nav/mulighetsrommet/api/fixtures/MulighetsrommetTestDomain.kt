package no.nav.mulighetsrommet.api.fixtures

import io.kotest.assertions.arrow.core.shouldBeRight
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.TiltaksgjennomforingDbo
import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.database.FlywayDatabaseAdapter
import no.nav.mulighetsrommet.domain.dbo.TiltakstypeDbo

data class MulighetsrommetTestDomain(
    val enheter: List<NavEnhetDbo> = listOf(NavEnhetFixtures.IT),
    val ansatte: List<NavAnsattDbo> = listOf(NavAnsattFixture.ansatt1, NavAnsattFixture.ansatt2),
    val tiltakstyper: List<TiltakstypeDbo> = listOf(
        TiltakstypeFixtures.Oppfolging,
        TiltakstypeFixtures.Arbeidstrening,
        TiltakstypeFixtures.VTA,
        TiltakstypeFixtures.Jobbklubb,
        TiltakstypeFixtures.AFT,
    ),
    val avtaler: List<AvtaleDbo> = listOf(AvtaleFixtures.oppfolging, AvtaleFixtures.avtaleForVta),
    val gjennomforinger: List<TiltaksgjennomforingDbo> = listOf(),
    val virksomhter: List<VirksomhetDto> = listOf(),
) {
    fun initialize(database: FlywayDatabaseAdapter) {
        NavEnhetRepository(database).also { repository ->
            enheter.forEach { repository.upsert(it).shouldBeRight() }
        }

        NavAnsattRepository(database).also { repository ->
            ansatte.forEach { repository.upsert(it).shouldBeRight() }
        }

        TiltakstypeRepository(database).also { repository ->
            tiltakstyper.forEach { repository.upsert(it).shouldBeRight() }
        }

        AvtaleRepository(database).also { repository ->
            avtaler.forEach { repository.upsert(it) }
        }

        TiltaksgjennomforingRepository(database).also { repository ->
            gjennomforinger.forEach { repository.upsert(it) }
        }

        VirksomhetRepository(database).also { repository ->
            virksomhter.forEach { repository.upsert(it) }
        }
    }
}
