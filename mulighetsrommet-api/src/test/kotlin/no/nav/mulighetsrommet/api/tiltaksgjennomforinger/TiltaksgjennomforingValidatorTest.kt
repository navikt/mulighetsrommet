package no.nav.mulighetsrommet.api.tiltaksgjennomforinger

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.*
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class TiltaksgjennomforingValidatorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val avtaleStartDato = LocalDate.now()
    val avtaleSluttDato = LocalDate.now().plusMonths(1)
    val avtale = AvtaleFixtures.avtale1.copy(
        startDato = avtaleStartDato,
        sluttDato = avtaleSluttDato,
        leverandorOrganisasjonsnummer = "000000000",
        leverandorUnderenheter = listOf("000000001", "000000002"),
        navEnheter = listOf("0400", "0502"),
    )

    val gjennomforing = TiltaksgjennomforingFixtures.Oppfolging1.copy(
        startDato = avtaleStartDato,
        sluttDato = avtaleSluttDato,
        navRegion = "0400",
        navEnheter = listOf("0502"),
        arrangorOrganisasjonsnummer = "000000001",
        administratorer = listOf("B123456"),
    )

    lateinit var tiltakstyper: TiltakstypeRepository
    lateinit var avtaler: AvtaleRepository
    lateinit var tiltaksgjennomforinger: TiltaksgjennomforingRepository
    lateinit var deltakere: DeltakerRepository

    beforeEach {
        tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(TiltakstypeFixtures.AFT).shouldBeRight()
        tiltakstyper.upsert(TiltakstypeFixtures.Oppfolging).shouldBeRight()

        val enheter = NavEnhetRepository(database.db)
        enheter.upsert(
            NavEnhetDbo(
                navn = "NAV Oslo",
                enhetsnummer = "0300",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
        ).shouldBeRight()
        enheter.upsert(
            NavEnhetDbo(
                navn = "NAV Innlandet",
                enhetsnummer = "0400",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.FYLKE,
                overordnetEnhet = null,
            ),
        ).shouldBeRight()
        enheter.upsert(
            NavEnhetDbo(
                navn = "NAV Gjøvik",
                enhetsnummer = "0502",
                status = NavEnhetStatus.AKTIV,
                type = Norg2Type.LOKAL,
                overordnetEnhet = "0400",
            ),
        ).shouldBeRight()

        avtaler = AvtaleRepository(database.db)
        avtaler.upsert(avtale)

        tiltaksgjennomforinger = TiltaksgjennomforingRepository(database.db)

        deltakere = DeltakerRepository(database.db)
    }

    test("should fail when avtale does not exist") {
        val unknownAvtaleId = UUID.randomUUID()

        val validator = TiltaksgjennomforingValidator(avtaler, deltakere)

        val dbo = gjennomforing.copy(avtaleId = unknownAvtaleId)

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError("avtaleId", "Avtalen finnes ikke"),
        )
    }

    test("should fail when tiltakstype does not match with avtale") {
        avtaler.upsert(avtale.copy(tiltakstypeId = TiltakstypeFixtures.AFT.id))

        val validator = TiltaksgjennomforingValidator(avtaler, deltakere)

        validator.validate(gjennomforing, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            ValidationError("tiltakstypeId", "Tiltakstypen må være den samme som for avtalen"),
        )
    }

    test("skal godta endringer selv om avtale er avbrutt") {
        val id = UUID.randomUUID()
        avtaler.upsert(avtale.copy(id = id))

        forAll(
            row(Avslutningsstatus.AVBRUTT),
            row(Avslutningsstatus.AVSLUTTET),
        ) { status ->
            avtaler.setAvslutningsstatus(id, status)

            val validator = TiltaksgjennomforingValidator(avtaler, deltakere)
            val dbo = gjennomforing.copy(avtaleId = id)

            validator.validate(dbo, null).shouldBeRight()
        }
    }

    test("should validate fields in the gjennomføring and fields related to the avtale") {
        val validator = TiltaksgjennomforingValidator(avtaler, deltakere)

        forAll(
            row(
                gjennomforing.copy(
                    startDato = avtaleStartDato.minusDays(1),
                    sluttDato = avtaleStartDato,
                ),
                listOf(ValidationError("startDato", "Startdato må være etter avtalens startdato")),
            ),
            row(
                gjennomforing.copy(
                    startDato = avtaleSluttDato.plusDays(1),
                    sluttDato = avtaleSluttDato.plusDays(1),
                ),
                listOf(
                    ValidationError("startDato", "Startdato må være før avtalens sluttdato"),
                ),
            ),
            row(
                gjennomforing.copy(
                    startDato = avtaleSluttDato,
                    sluttDato = avtaleStartDato,
                ),
                listOf(ValidationError("startDato", "Startdato må være før sluttdato")),
            ),
            row(
                gjennomforing.copy(antallPlasser = 0),
                listOf(ValidationError("antallPlasser", "Antall plasser må være større enn 0")),
            ),
            row(
                gjennomforing.copy(navEnheter = listOf("0401")),
                listOf(ValidationError("navEnheter", "NAV-enhet 0401 mangler i avtalen")),
            ),
            row(
                gjennomforing.copy(arrangorOrganisasjonsnummer = "000000003"),
                listOf(ValidationError("arrangorOrganisasjonsnummer", "Arrangøren mangler i avtalen")),
            ),
        ) { input, error ->
            validator.validate(input, null).shouldBeLeft(error)
        }
    }

    context("when gjennomføring does not already exist") {
        test("should fail when opphav is not MR_ADMIN_FLATE") {
            val validator = TiltaksgjennomforingValidator(avtaler, deltakere)

            val dbo = gjennomforing.copy(
                id = UUID.randomUUID(),
                opphav = ArenaMigrering.Opphav.ARENA,
            )

            validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"),
            )
        }
    }

    context("when gjennomføring already exists") {
        beforeEach {
            tiltaksgjennomforinger.upsert(gjennomforing.copy(administratorer = listOf()))
        }

        afterEach {
            tiltaksgjennomforinger.delete(gjennomforing.id)
        }

        test("should fail when opphav is different") {
            val validator = TiltaksgjennomforingValidator(avtaler, deltakere)

            val dbo = gjennomforing.copy(
                opphav = ArenaMigrering.Opphav.ARENA,
            )
            val previous = tiltaksgjennomforinger.get(gjennomforing.id)

            validator.validate(dbo, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                ValidationError("opphav", "Opphav kan ikke endres"),
            )
        }

        test("should fail when status is Avsluttet") {
            forAll(
                row(Avslutningsstatus.AVBRUTT),
                row(Avslutningsstatus.AVLYST),
                row(Avslutningsstatus.AVSLUTTET),
            ) { status ->
                tiltaksgjennomforinger.setAvslutningsstatus(gjennomforing.id, status)

                val validator = TiltaksgjennomforingValidator(avtaler, deltakere)

                val previous = tiltaksgjennomforinger.get(gjennomforing.id)
                validator.validate(gjennomforing, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    ValidationError("navn", "Kan bare gjøre endringer når gjennomføringen er aktiv"),
                )
            }
        }

        context("når gjennomføring har status GJENNOMFORES") {
            val deltaker = DeltakerDbo(
                id = UUID.randomUUID(),
                tiltaksgjennomforingId = gjennomforing.id,
                opphav = Deltakeropphav.ARENA,
                registrertDato = LocalDateTime.now(),
                startDato = gjennomforing.startDato,
                sluttDato = gjennomforing.sluttDato,
                status = Deltakerstatus.DELTAR,
            )

            beforeEach {
                deltakere.upsert(deltaker)
            }

            afterEach {
                deltakere.delete(deltaker.id)
            }

            test("skal ikke kunne endre felter relatert til tilsagn/refusjon") {
                val differentAvtaleId = UUID.randomUUID()

                val dbo = gjennomforing.copy(
                    navn = "Nytt navn",
                    avtaleId = differentAvtaleId,
                    arrangorOrganisasjonsnummer = "000000002",
                    startDato = gjennomforing.startDato.plusDays(1),
                    sluttDato = gjennomforing.sluttDato?.minusDays(1),
                    antallPlasser = gjennomforing.antallPlasser + 1,
                    administratorer = listOf("Donald Duck"),
                    navRegion = "0400",
                    navEnheter = listOf("0502"),
                    oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                    kontaktpersoner = emptyList(),
                    stengtFra = LocalDate.of(2023, 10, 10),
                    stengtTil = LocalDate.of(2023, 10, 10),
                    stedForGjennomforing = "Hjemmekontor",
                )

                avtaler.upsert(avtale.copy(id = differentAvtaleId))

                val validator = TiltaksgjennomforingValidator(avtaler, deltakere)

                val previous = tiltaksgjennomforinger.get(gjennomforing.id)
                validator.validate(dbo, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    listOf(
                        ValidationError("avtaleId", "Avtalen kan ikke endres når gjennomføringen er aktiv"),
                        ValidationError(
                            "arrangorOrganisasjonsnummer",
                            "Arrangøren kan ikke endres når gjennomføringen er aktiv",
                        ),
                        ValidationError("oppstart", "Oppstartstype kan ikke endres når det finnes påmeldte deltakere"),
                        ValidationError("startDato", "Startdato kan ikke endres når gjennomføringen er aktiv"),
                        ValidationError("sluttDato", "Sluttdato kan ikke endres når gjennomføringen er aktiv"),
                        ValidationError("antallPlasser", "Antall plasser kan ikke endres når gjennomføringen er aktiv"),
                    ),
                )
            }
        }
    }
})
