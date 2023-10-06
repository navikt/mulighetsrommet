package no.nav.mulighetsrommet.api.avtaler

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtalestatus
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.domain.dto.TiltakstypeDto
import java.time.LocalDate
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val newAvtaleId = UUID.randomUUID()
    val existingAvtaleId = UUID.randomUUID()

    val avtale = AvtaleDbo(
        id = newAvtaleId,
        navn = "Avtale",
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = listOf("123456789"),
        leverandorKontaktpersonId = null,
        avtalenummer = "123456",
        startDato = LocalDate.of(2023, 6, 1),
        sluttDato = LocalDate.of(2024, 6, 1),
        navRegion = "2990",
        url = "http://localhost:8080",
        administratorer = listOf("B123456"),
        avtaletype = Avtaletype.Avtale,
        prisbetingelser = null,
        navEnheter = listOf("2990"),
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        antallPlasser = null,
    )

    val tiltakstyper = mockk<TiltakstypeRepository>()
    every { tiltakstyper.get(TiltakstypeFixtures.AFT.id) } returns TiltakstypeDto.from(TiltakstypeFixtures.AFT)
    every { tiltakstyper.get(TiltakstypeFixtures.VTA.id) } returns TiltakstypeDto.from(TiltakstypeFixtures.VTA)
    every { tiltakstyper.get(TiltakstypeFixtures.Oppfolging.id) } returns TiltakstypeDto.from(TiltakstypeFixtures.Oppfolging)

    val avtaler = mockk<AvtaleRepository>()
    every { avtaler.get(newAvtaleId) } returns null

    val gjennomforinger = mockk<TiltaksgjennomforingRepository>()

    test("should accumulate errors when request has multiple issues") {
        val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

        val request = avtale.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            leverandorUnderenheter = emptyList(),
        )

        validator.validate(request).shouldBeLeft().shouldContainAll(
            listOf(
                ValidationError("startDato", "Startdato må være før sluttdato"),
                ValidationError("navEnheter", "Minst ett NAV-kontor må være valgt"),
                ValidationError("leverandorUnderenheter", "Minst én underenhet til leverandøren må være valgt"),
            ),
        )
    }

    context("when avtale does not already exist") {
        test("should fail when tiltakstype is not VTA or AFT") {
            val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

            val request = avtale.copy(tiltakstypeId = TiltakstypeFixtures.Oppfolging.id)

            validator.validate(request).shouldBeLeft().shouldContain(
                ValidationError(
                    "tiltakstype",
                    "Avtaler kan bare opprettes når de gjelder for tiltakstypene AFT eller VTA",
                ),
            )
        }

        test("should fail when opphav is not MR_ADMIN_FLATE") {
            val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

            val request = avtale.copy(opphav = ArenaMigrering.Opphav.ARENA)

            validator.validate(request).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"),
            )
        }
    }

    context("when avtale already exists") {
        test("should fail when opphav is different") {
            every { avtaler.get(existingAvtaleId) } returns AvtaleFixtures.avtaleAdminDto.copy(
                opphav = ArenaMigrering.Opphav.ARENA,
            )

            val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

            val request = avtale.copy(id = existingAvtaleId, opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE)

            validator.validate(request).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Avtalens opphav kan ikke endres"),
            )
        }

        test("should fail when status is Avsluttet") {
            forAll(
                row(Avtalestatus.Avsluttet),
                row(Avtalestatus.Avbrutt),
            ) { status ->
                every { avtaler.get(existingAvtaleId) } returns AvtaleFixtures.avtaleAdminDto.copy(
                    avtalestatus = status,
                )

                val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

                val request = avtale.copy(id = existingAvtaleId)

                validator.validate(request).shouldBeLeft().shouldContain(
                    ValidationError(
                        "navn",
                        "Kan bare gjøre endringer når avtalen har status Planlagt eller Aktiv",
                    ),
                )
            }
        }
    }
})
