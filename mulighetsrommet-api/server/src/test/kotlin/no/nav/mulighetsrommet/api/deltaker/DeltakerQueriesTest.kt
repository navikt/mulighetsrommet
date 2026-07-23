package no.nav.mulighetsrommet.api.deltaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.deltaker.Deltakelsesmengde
import no.nav.mulighetsrommet.api.domain.deltaker.NavVeileder
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.DeltakerFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

// TODO: flyttes til "persistence" etter at avhengigheter også er flyttet (avtale, gjennomføring)
class DeltakerQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.oppfolging),
        gjennomforinger = listOf(
            GjennomforingFixtures.Oppfolging1,
            GjennomforingFixtures.Oppfolging1.copy(id = UUID.randomUUID()),
        ),
    )

    val opprettetTidspunkt = Instant.parse("2023-03-01T00:00:00Z")
    val mengdeOpprettetTidspunkt = Instant.parse("2023-03-01T00:00:00Z")

    val deltaker1 = DeltakerFixtures.createDeltaker(
        id = UUID.randomUUID(),
        gjennomforingId = domain.gjennomforinger[0].id,
        endretTidspunkt = opprettetTidspunkt,
        status = DeltakerStatusType.VENTER_PA_OPPSTART,
        innhold = null,
        veileder = NavVeileder(
            navIdent = NavIdent("B123456"),
            enhetsnummer = NavEnhetNummer("0200"),
        ),
    )

    val deltaker2 = DeltakerFixtures.createDeltaker(
        id = UUID.randomUUID(),
        gjennomforingId = domain.gjennomforinger[1].id,
        endretTidspunkt = opprettetTidspunkt,
        status = DeltakerStatusType.VENTER_PA_OPPSTART,
        innhold = "Noe innhold",
        veileder = null,
    )

    test("CRUD") {
        database.runAndRollback {
            domain.initialize()

            repository.deltaker.save(deltaker1)
            repository.deltaker.save(deltaker2)

            repository.deltaker.get(deltaker1.id) shouldBe deltaker1
            repository.deltaker.get(deltaker2.id) shouldBe deltaker2

            val avsluttetDeltaker2 = deltaker2.registrerStatus(
                type = DeltakerStatusType.HAR_SLUTTET,
                endretTidspunkt = Instant.parse("2023-03-02T00:00:00Z"),
            )
            repository.deltaker.save(avsluttetDeltaker2)

            repository.deltaker.get(deltaker1.id) shouldBe deltaker1
            repository.deltaker.get(deltaker2.id) shouldBe avsluttetDeltaker2

            repository.deltaker.delete(deltaker1.id)

            repository.deltaker.get(deltaker1.id) shouldBe null
            repository.deltaker.get(deltaker2.id) shouldBe avsluttetDeltaker2
        }
    }

    test("deltakelsesmengder blir overskrevet og hentet i riktig rekkefølge") {
        database.runAndRollback {
            domain.initialize()

            val deltakerMedEnMengde = DeltakerFixtures.createDeltakerMedDeltakelsesmengder(
                id = deltaker1.id,
                gjennomforingId = deltaker1.gjennomforingId,
                deltakelsesmengder = listOf(
                    Deltakelsesmengde(
                        gyldigFra = LocalDate.of(2023, 3, 1),
                        deltakelsesprosent = 100.0,
                        opprettetTidspunkt = mengdeOpprettetTidspunkt,
                    ),
                ),
            )
            repository.deltaker.save(deltakerMedEnMengde)

            repository.deltaker.get(deltaker1.id).shouldNotBeNull().deltakelsesmengder.shouldContainExactly(
                deltakerMedEnMengde.deltakelsesmengder,
            )

            val deltakerMedToMengder = DeltakerFixtures.createDeltakerMedDeltakelsesmengder(
                id = deltaker1.id,
                gjennomforingId = deltaker1.gjennomforingId,
                deltakelsesmengder = listOf(
                    Deltakelsesmengde(
                        gyldigFra = LocalDate.of(2023, 3, 10),
                        deltakelsesprosent = 100.0,
                        opprettetTidspunkt = mengdeOpprettetTidspunkt,
                    ),
                    Deltakelsesmengde(
                        gyldigFra = LocalDate.of(2023, 3, 5),
                        deltakelsesprosent = 100.0,
                        opprettetTidspunkt = mengdeOpprettetTidspunkt,
                    ),
                ),
            )
            repository.deltaker.save(deltakerMedToMengder)

            repository.deltaker.get(deltaker1.id).shouldNotBeNull().deltakelsesmengder.shouldContainExactly(
                listOf(
                    Deltakelsesmengde(LocalDate.of(2023, 3, 5), 100.0, mengdeOpprettetTidspunkt),
                    Deltakelsesmengde(LocalDate.of(2023, 3, 10), 100.0, mengdeOpprettetTidspunkt),
                ),
            )

            repository.deltaker.delete(deltaker1.id)
            repository.deltaker.get(deltaker1.id) shouldBe null
        }
    }

    test("get by gjennomforing") {
        database.runAndRollback {
            domain.initialize()

            repository.deltaker.save(deltaker1)
            repository.deltaker.save(deltaker2)

            repository.deltaker
                .getByGjennomforing(domain.gjennomforinger[0].id)
                .shouldContainExactly(deltaker1)

            repository.deltaker
                .getByGjennomforing(domain.gjennomforinger[1].id)
                .shouldContainExactly(deltaker2)
        }
    }
})
