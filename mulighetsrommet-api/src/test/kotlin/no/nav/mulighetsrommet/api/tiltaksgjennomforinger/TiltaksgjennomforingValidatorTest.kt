package no.nav.mulighetsrommet.api.tiltaksgjennomforinger

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures.Oppfolging1
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dbo.TiltaksgjennomforingOppstartstype
import no.nav.mulighetsrommet.domain.dto.AvtaleAdminDto
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.NavEnhet
import no.nav.mulighetsrommet.domain.dto.Tiltaksgjennomforingsstatus
import java.time.LocalDate
import java.util.*

class TiltaksgjennomforingValidatorTest : FunSpec({

    val tiltaksgjennomforinger = mockk<TiltaksgjennomforingRepository>()
    every { tiltaksgjennomforinger.get(any()) } returns null

    val avtaler = mockk<AvtaleRepository>()

    test("should fail when avtale does not exist") {
        val unknownAvtaleId = UUID.randomUUID()
        every { avtaler.get(unknownAvtaleId) } returns null

        val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

        val dbo = Oppfolging1.copy(avtaleId = unknownAvtaleId)

        validator.validate(dbo).shouldBeLeft().shouldContain(
            ValidationError("avtaleId", "Avtalen finnes ikke"),
        )
    }

    test("should fail when tiltakstype does not match with avtale") {
        every { avtaler.get(AvtaleFixtures.avtale1.id) } returns AvtaleFixtures.avtaleAdminDto

        val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

        validator.validate(Oppfolging1).shouldBeLeft().shouldContain(
            ValidationError("tiltakstypeId", "Tiltakstypen må være den samme som for avtalen"),
        )
    }

    test("should fail when avtale is Avbrutt") {
        every { avtaler.get(AvtaleFixtures.avtale1.id) } returns AvtaleFixtures.oppfolgingAvtaleAdminDto.copy(
            avtalestatus = Avtalestatus.Avbrutt,
        )

        val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

        validator.validate(Oppfolging1).shouldBeLeft().shouldContain(
            ValidationError("avtaleId", "Kan ikke endre gjennomføring fordi avtalen har status Avbrutt"),
        )
    }

    test("should fail when avtale is Avsluttet") {
        every { avtaler.get(AvtaleFixtures.avtale1.id) } returns AvtaleFixtures.oppfolgingAvtaleAdminDto.copy(
            avtalestatus = Avtalestatus.Avsluttet,
        )

        val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

        validator.validate(Oppfolging1).shouldBeLeft().shouldContain(
            ValidationError("avtaleId", "Kan ikke endre gjennomføring fordi avtalen har status Avsluttet"),
        )
    }

    test("should validate fields in the gjennomføring and fields related to the avtale") {
        every { avtaler.get(AvtaleFixtures.avtale1.id) } returns AvtaleFixtures.oppfolgingAvtaleAdminDto.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 2, 1),
            navRegion = NavEnhet(enhetsnummer = "0400", navn = "NAV Innlandet"),
            navEnheter = listOf(NavEnhet(enhetsnummer = "0402", navn = "NAV Kongsvinger")),
            leverandor = AvtaleAdminDto.Leverandor(
                organisasjonsnummer = "000000000",
                navn = "Bedrift",
                slettet = false,
            ),
            leverandorUnderenheter = listOf(
                AvtaleAdminDto.LeverandorUnderenhet(organisasjonsnummer = "000000001", navn = "Bedrift underenhet"),
            ),
        )

        val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

        val gjennomforing = Oppfolging1.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2023, 2, 1),
            antallPlasser = 10,
            navEnheter = listOf("0402"),
            arrangorOrganisasjonsnummer = "000000001",
        )

        forAll(
            row(
                gjennomforing.copy(
                    startDato = LocalDate.of(2022, 12, 31),
                    sluttDato = LocalDate.of(2023, 1, 1),
                ),
                listOf(ValidationError("startDato", "Startdato må være etter avtalens startdato")),
            ),
            row(
                gjennomforing.copy(
                    startDato = LocalDate.of(2023, 3, 1),
                    sluttDato = LocalDate.of(2023, 3, 1),
                ),
                listOf(
                    ValidationError("startDato", "Startdato må være før avtalens sluttdato"),
                    ValidationError("sluttDato", "Sluttdato må være før avtalens sluttdato"),
                ),
            ),
            row(
                gjennomforing.copy(
                    startDato = LocalDate.of(2023, 1, 2),
                    sluttDato = LocalDate.of(2023, 1, 1),
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
                gjennomforing.copy(arrangorOrganisasjonsnummer = "000000002"),
                listOf(ValidationError("arrangorOrganisasjonsnummer", "Arrangøren mangler i avtalen")),
            ),
        ) { dbo, error ->
            validator.validate(dbo).shouldBeLeft(error)
        }
    }

    context("when gjennomføring does not already exist") {
        test("should fail when opphav is not MR_ADMIN_FLATE") {
            every { avtaler.get(AvtaleFixtures.avtale1.id) } returns AvtaleFixtures.oppfolgingAvtaleAdminDto
            val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

            val dbo = Oppfolging1.copy(
                opphav = ArenaMigrering.Opphav.ARENA,
            )

            validator.validate(dbo).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"),
            )
        }
    }

    context("when gjennomføring already exists") {
        test("should fail when opphav is different") {
            val dbo = Oppfolging1.copy(
                opphav = ArenaMigrering.Opphav.ARENA,
            )

            every { avtaler.get(AvtaleFixtures.avtale1.id) } returns AvtaleFixtures.oppfolgingAvtaleAdminDto
            every { tiltaksgjennomforinger.get(dbo.id) } returns TiltaksgjennomforingFixtures.Oppfolging1AdminDto

            val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

            validator.validate(dbo).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Avtalens opphav kan ikke endres"),
            )
        }

        test("should fail when status is Avsluttet") {
            forAll(
                row(Tiltaksgjennomforingsstatus.AVBRUTT),
                row(Tiltaksgjennomforingsstatus.AVLYST),
                row(Tiltaksgjennomforingsstatus.AVSLUTTET),
            ) { status ->
                every { avtaler.get(AvtaleFixtures.avtale1.id) } returns AvtaleFixtures.oppfolgingAvtaleAdminDto
                every { tiltaksgjennomforinger.get(Oppfolging1.id) } returns TiltaksgjennomforingFixtures.Oppfolging1AdminDto.copy(
                    status = status,
                )

                val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

                validator.validate(Oppfolging1).shouldBeLeft().shouldContain(
                    ValidationError("navn", "Kan bare gjøre endringer når gjennomføringen er aktiv"),
                )
            }
        }

        context("når gjennomføring har status GJENNOMFORES") {
            test("skal ikke kunne endre felter relatert til tilsagn/refursjon") {
                val differentAvtaleId = UUID.randomUUID()

                val dbo = Oppfolging1.copy(
                    navn = "Nytt navn",
                    avtaleId = differentAvtaleId,
                    arrangorOrganisasjonsnummer = Oppfolging1.arrangorOrganisasjonsnummer,
                    startDato = Oppfolging1.startDato.plusDays(1),
                    sluttDato = Oppfolging1.sluttDato?.minusDays(1),
                    antallPlasser = Oppfolging1.antallPlasser + 1,
                    administratorer = listOf("Donald Duck"),
                    navEnheter = listOf("0402"),
                    oppstart = TiltaksgjennomforingOppstartstype.LOPENDE,
                    kontaktpersoner = emptyList(),
                    arrangorKontaktpersonId = Oppfolging1.arrangorKontaktpersonId,
                    stengtFra = LocalDate.of(2023, 10, 10),
                    stengtTil = LocalDate.of(2023, 10, 10),
                    stedForGjennomforing = "Hjemmekontor",
                    estimertVentetid = "Leeenge",
                )

                every { avtaler.get(differentAvtaleId) } returns AvtaleFixtures.oppfolgingAvtaleAdminDto.copy(
                    id = differentAvtaleId,
                    navRegion = NavEnhet(enhetsnummer = "0400", navn = "NAV Innlandet"),
                    navEnheter = listOf(NavEnhet(enhetsnummer = "0402", navn = "NAV Kongsvinger")),
                )
                every { tiltaksgjennomforinger.get(dbo.id) } returns TiltaksgjennomforingFixtures.Oppfolging1AdminDto

                val validator = TiltaksgjennomforingValidator(avtaler, tiltaksgjennomforinger)

                validator.validate(dbo).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    listOf(
                        ValidationError("avtaleId", "Avtalen kan ikke endres når gjennomføringen er aktiv"),
                        ValidationError("oppstart", "Oppstartstype kan ikke endres når gjennomføringen er aktiv"),
                        ValidationError("startDato", "Startdato kan ikke endres når gjennomføringen er aktiv"),
                        ValidationError("sluttDato", "Sluttdato kan ikke endres når gjennomføringen er aktiv"),
                        ValidationError("antallPlasser", "Antall plasser kan ikke endres når gjennomføringen er aktiv"),
                    ),
                )
            }
        }
    }
})
