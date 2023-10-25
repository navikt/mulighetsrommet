package no.nav.mulighetsrommet.api.avtaler

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val newAvtaleId = UUID.randomUUID()
    val existingAvtaleId = UUID.randomUUID()

    val avtaleDbo = AvtaleDbo(
        id = newAvtaleId,
        navn = "Avtale",
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = listOf("123456789"),
        leverandorKontaktpersonId = null,
        avtalenummer = "123456",
        startDato = LocalDate.of(2023, 6, 1),
        sluttDato = LocalDate.of(2024, 6, 1),
        url = "http://localhost:8080",
        administratorer = listOf("B123456"),
        avtaletype = Avtaletype.Avtale,
        prisbetingelser = null,
        navEnheter = listOf("2990"),
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        antallPlasser = null,
        updatedAt = LocalDateTime.now(),
    )

    val avtaleAdminDto = avtaleDbo.run {
        AvtaleAdminDto(
            id = id,
            tiltakstype = AvtaleAdminDto.Tiltakstype(
                id = TiltakstypeFixtures.AFT.id,
                navn = TiltakstypeFixtures.AFT.navn,
                arenaKode = TiltakstypeFixtures.AFT.tiltakskode,
            ),
            navn = navn,
            avtalenummer = avtalenummer,
            leverandor = AvtaleAdminDto.Leverandor(
                organisasjonsnummer = leverandorOrganisasjonsnummer,
                navn = "Bedrift",
                slettet = false,
            ),
            leverandorUnderenheter = leverandorUnderenheter.map {
                AvtaleAdminDto.LeverandorUnderenhet(
                    organisasjonsnummer = it,
                    navn = it,
                )
            },
            leverandorKontaktperson = null,
            startDato = startDato,
            sluttDato = sluttDato,
            avtaletype = avtaletype,
            avtalestatus = Avtalestatus.Aktiv,
            prisbetingelser = prisbetingelser,
            administrator = administratorer.firstOrNull()?.let {
                AvtaleAdminDto.Administrator(navIdent = it, navn = it)
            },
            url = url,
            antallPlasser = antallPlasser,
            opphav = opphav,
            updatedAt = avtaleDbo.updatedAt,
            kontorstruktur = listOf(
                Kontorstruktur(
                    region = EmbeddedNavEnhet(
                        enhetsnummer = "0100",
                        navn = "NAV Mockdata",
                        type = NavEnhetType.FYLKE,
                        overordnetEnhet = null,
                    ),
                    kontorer = listOf(
                        EmbeddedNavEnhet(
                            enhetsnummer = "0101",
                            navn = "NAV Mock-kontor",
                            type = NavEnhetType.LOKAL,
                            overordnetEnhet = "0100",
                        ),
                    ),
                ),
            ),
        )
    }

    val tiltakstyper = mockk<TiltakstypeRepository>()
    every { tiltakstyper.get(TiltakstypeFixtures.AFT.id) } returns TiltakstypeDto.from(TiltakstypeFixtures.AFT)
    every { tiltakstyper.get(TiltakstypeFixtures.VTA.id) } returns TiltakstypeDto.from(TiltakstypeFixtures.VTA)
    every { tiltakstyper.get(TiltakstypeFixtures.Oppfolging.id) } returns TiltakstypeDto.from(TiltakstypeFixtures.Oppfolging)

    val avtaler = mockk<AvtaleRepository>()

    val gjennomforinger = mockk<TiltaksgjennomforingRepository>()

    beforeEach {
        every { avtaler.get(newAvtaleId) } returns null

        every {
            gjennomforinger.getAll(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns Pair(0, emptyList())
    }

    test("should accumulate errors when dbo has multiple issues") {
        val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

        val dbo = avtaleDbo.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            leverandorUnderenheter = emptyList(),
        )

        validator.validate(dbo).shouldBeLeft().shouldContainAll(
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

            val dbo = avtaleDbo.copy(tiltakstypeId = TiltakstypeFixtures.Oppfolging.id)

            validator.validate(dbo).shouldBeLeft().shouldContain(
                ValidationError(
                    "tiltakstypeId",
                    "Avtaler kan bare opprettes når de gjelder for tiltakstypene AFT eller VTA",
                ),
            )
        }

        test("should fail when opphav is not MR_ADMIN_FLATE") {
            val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

            val dbo = avtaleDbo.copy(opphav = ArenaMigrering.Opphav.ARENA)

            validator.validate(dbo).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"),
            )
        }
    }

    context("når avtalen allerede eksisterer") {
        test("skal ikke kunne endre opphav") {
            every { avtaler.get(existingAvtaleId) } returns avtaleAdminDto.copy(
                opphav = ArenaMigrering.Opphav.ARENA,
            )

            val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

            val dbo = avtaleDbo.copy(id = existingAvtaleId, opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE)

            validator.validate(dbo).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Avtalens opphav kan ikke endres"),
            )
        }

        test("skal bare kunne endre aktive avtaler") {
            forAll(
                row(Avtalestatus.Avsluttet),
                row(Avtalestatus.Avbrutt),
            ) { status ->
                every { avtaler.get(existingAvtaleId) } returns avtaleAdminDto.copy(
                    avtalestatus = status,
                )

                val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

                val dbo = avtaleDbo.copy(id = existingAvtaleId)

                validator.validate(dbo).shouldBeLeft().shouldContain(
                    ValidationError(
                        "navn",
                        "Kan bare gjøre endringer når avtalen har status Planlagt eller Aktiv",
                    ),
                )
            }
        }

        context("når avtalen har gjennomføringer") {
            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                every { avtaler.get(existingAvtaleId) } returns avtaleAdminDto
                every {
                    gjennomforinger.getAll(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                    )
                } returns Pair(
                    1,
                    listOf(
                        TiltaksgjennomforingFixtures.Oppfolging1AdminDto.copy(
                            tiltakstype = TiltaksgjennomforingAdminDto.Tiltakstype(
                                id = avtaleAdminDto.tiltakstype.id,
                                navn = avtaleAdminDto.tiltakstype.navn,
                                arenaKode = avtaleAdminDto.tiltakstype.arenaKode,
                            ),
                            avtaleId = avtaleAdminDto.id,
                            arrangor = TiltaksgjennomforingAdminDto.Arrangor(
                                organisasjonsnummer = "000000001",
                                navn = "Annen arrangør",
                                kontaktperson = null,
                                slettet = false,
                            ),
                            navEnheter = listOf(
                                EmbeddedNavEnhet(navn = "NAV Gjøvik", enhetsnummer = "0502", type = NavEnhetType.LOKAL, overordnetEnhet = null),
                            ),
                        ),
                    ),
                )

                val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

                val dbo = avtaleDbo.copy(id = existingAvtaleId, tiltakstypeId = TiltakstypeFixtures.VTA.id)

                validator.validate(dbo).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    listOf(
                        ValidationError(
                            "tiltakstypeId",
                            "Kan ikke endre tiltakstype fordi det finnes gjennomføringer for avtalen",
                        ),
                        ValidationError(
                            "leverandorUnderenheter",
                            "Arrangøren 000000001 er i bruk på en av avtalens gjennomføringer, men mangler blandt leverandørens underenheter",
                        ),
                        ValidationError(
                            "navEnheter",
                            "NAV-enheten 0502 er i bruk på en av avtalens gjennomføringer, men mangler blandt avtalens NAV-enheter",
                        ),
                    ),
                )
            }
        }

        context("når avtalen er aktiv") {
            test("skal ikke kunne endre felter relatert til tilsagn/refusjon") {
                val avtaleMedEndringer = AvtaleDbo(
                    id = avtaleDbo.id,
                    navn = "Nytt navn",
                    tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                    leverandorOrganisasjonsnummer = "999999999",
                    leverandorUnderenheter = listOf("888888888"),
                    leverandorKontaktpersonId = null,
                    avtalenummer = "123456",
                    startDato = LocalDate.of(2023, 6, 2),
                    sluttDato = LocalDate.of(2024, 6, 2),
                    url = "nav.no",
                    administratorer = listOf("B123456"),
                    avtaletype = Avtaletype.Rammeavtale,
                    prisbetingelser = null,
                    navEnheter = listOf("2990"),
                    opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                    antallPlasser = null,
                    updatedAt = avtaleDbo.updatedAt,
                )

                every { avtaler.get(avtaleMedEndringer.id) } returns avtaleAdminDto

                val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

                validator.validate(avtaleMedEndringer).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    listOf(
                        ValidationError("avtaletype", "Avtaletype kan ikke endres når avtalen er aktiv"),
                        ValidationError("startDato", "Startdato kan ikke endres når avtalen er aktiv"),
                        ValidationError("sluttDato", "Sluttdato kan ikke endres når avtalen er aktiv"),
                        ValidationError(
                            "leverandorOrganisasjonsnummer",
                            "Leverandøren kan ikke endres når avtalen er aktiv",
                        ),
                    ),
                )
            }

            test("skal kunne endre felter relatert til tilsagn/refusjon når avtalen er AFT/VTA") {
                val avtaleMedEndringer = AvtaleDbo(
                    id = avtaleDbo.id,
                    navn = "Nytt navn",
                    tiltakstypeId = TiltakstypeFixtures.AFT.id,
                    leverandorOrganisasjonsnummer = "999999999",
                    leverandorUnderenheter = listOf("888888888"),
                    leverandorKontaktpersonId = null,
                    avtalenummer = "123456",
                    startDato = LocalDate.of(2023, 6, 2),
                    sluttDato = LocalDate.of(2024, 6, 2),
                    url = "nav.no",
                    administratorer = listOf("B123456"),
                    avtaletype = Avtaletype.Rammeavtale,
                    prisbetingelser = null,
                    navEnheter = listOf("2990"),
                    opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
                    antallPlasser = null,
                    updatedAt = avtaleDbo.updatedAt,
                )

                every { avtaler.get(avtaleMedEndringer.id) } returns avtaleAdminDto

                val validator = AvtaleValidator(tiltakstyper, avtaler, gjennomforinger)

                validator.validate(avtaleMedEndringer).shouldBeRight()
            }
        }
    }
})
