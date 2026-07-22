package no.nav.mulighetsrommet.api.deltaker

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.deltaker.Deltakelsesmengde
import no.nav.mulighetsrommet.api.domain.deltaker.Deltaker
import no.nav.mulighetsrommet.api.domain.deltaker.NavVeileder
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
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

    val opprettetTidspunkt = LocalDateTime.of(2023, 3, 1, 0, 0, 0)
    val mengdeOpprettetTidspunkt = Instant.parse("2023-03-01T00:00:00Z")

    val deltaker1 = Deltaker(
        id = UUID.randomUUID(),
        gjennomforingId = domain.gjennomforinger[0].id,
        startDato = null,
        sluttDato = null,
        registrertTidspunkt = opprettetTidspunkt,
        endretTidspunkt = opprettetTidspunkt,
        status = DeltakerStatus(
            DeltakerStatusType.VENTER_PA_OPPSTART,
            aarsak = null,
            opprettetTidspunkt = opprettetTidspunkt,
        ),
        deltakelsesmengder = emptyList(),
        innholdAnnet = null,
        navVeileder = NavVeileder(
            navIdent = NavIdent("B123456"),
            enhetsnummer = NavEnhetNummer("0200"),
        ),
    )
    val deltaker2 = deltaker1.copy(
        id = UUID.randomUUID(),
        gjennomforingId = domain.gjennomforinger[1].id,
    )

    test("CRUD") {
        database.runAndRollback {
            domain.initialize()

            repository.deltaker.save(deltaker1)
            repository.deltaker.save(deltaker2)

            repository.deltaker.get(deltaker1.id) shouldBe deltaker1
            repository.deltaker.get(deltaker2.id) shouldBe deltaker2

            val avsluttetDeltaker2 = deltaker2.copy(
                status = DeltakerStatus(
                    DeltakerStatusType.HAR_SLUTTET,
                    aarsak = null,
                    opprettetTidspunkt = LocalDateTime.of(2023, 3, 2, 0, 0, 0),
                ),
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

            val deltakerMedEnMengde = deltaker1.copy(
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

            val deltakerMedToMengder = deltaker1.copy(
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
