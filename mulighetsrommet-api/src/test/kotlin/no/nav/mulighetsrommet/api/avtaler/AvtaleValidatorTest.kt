package no.nav.mulighetsrommet.api.avtaler

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockkObject
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.AvtaleDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.AvtaleRepository
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.responses.ValidationError
import no.nav.mulighetsrommet.api.services.NavEnhetService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.constants.ArenaMigrering
import no.nav.mulighetsrommet.domain.dto.Avtaletype
import no.nav.mulighetsrommet.env.NaisEnv
import java.time.LocalDate
import java.util.*

class AvtaleValidatorTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val avtaleDbo = AvtaleDbo(
        id = UUID.randomUUID(),
        navn = "Avtale",
        tiltakstypeId = TiltakstypeFixtures.AFT.id,
        leverandorOrganisasjonsnummer = "123456789",
        leverandorUnderenheter = listOf("123456789"),
        leverandorKontaktpersonId = null,
        avtalenummer = "123456",
        startDato = LocalDate.now().minusDays(1),
        sluttDato = LocalDate.now().plusMonths(1),
        url = "http://localhost:8080",
        administratorer = listOf("B123456"),
        avtaletype = Avtaletype.Avtale,
        prisbetingelser = null,
        navEnheter = listOf("0400", "0502"),
        opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE,
        antallPlasser = null,
        beskrivelse = null,
        faneinnhold = null,
    )

    lateinit var navEnheterService: NavEnhetService
    lateinit var tiltakstyper: TiltakstypeRepository
    lateinit var avtaler: AvtaleRepository
    lateinit var gjennomforinger: TiltaksgjennomforingRepository

    beforeEach {
        tiltakstyper = TiltakstypeRepository(database.db)
        tiltakstyper.upsert(TiltakstypeFixtures.AFT)
        tiltakstyper.upsert(TiltakstypeFixtures.VTA)
        tiltakstyper.upsert(TiltakstypeFixtures.Oppfolging)

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
        navEnheterService = NavEnhetService(enheter)

        avtaler = AvtaleRepository(database.db)

        gjennomforinger = TiltaksgjennomforingRepository(database.db)
    }

    test("should accumulate errors when dbo has multiple issues") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)

        val dbo = avtaleDbo.copy(
            startDato = LocalDate.of(2023, 1, 1),
            sluttDato = LocalDate.of(2020, 1, 1),
            navEnheter = emptyList(),
            leverandorUnderenheter = emptyList(),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainAll(
            listOf(
                ValidationError("startDato", "Startdato må være før sluttdato"),
                ValidationError("navEnheter", "Minst én NAV-region må være valgt"),
                ValidationError("leverandorUnderenheter", "Minst én underenhet til leverandøren må være valgt"),
            ),
        )
    }

    test("Avtalenavn må være minst 5 tegn når avtalen er opprettet i Admin-flate") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)
        val dbo = avtaleDbo.copy(navn = "Avt", opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE)
        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navn", "Avtalenavn må være minst 5 tegn langt"),
            ),
        )
    }

    test("skal validere at ") {
        val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)

        val dbo = avtaleDbo.copy(
            navEnheter = listOf("0300", "0502"),
        )

        validator.validate(dbo, null).shouldBeLeft().shouldContainExactlyInAnyOrder(
            listOf(
                ValidationError("navEnheter", "NAV-enheten 0502 passer ikke i avtalens kontorstruktur"),
            ),
        )
    }

    context("når avtalen ikke allerede eksisterer") {
        test("skal feile når tiltakstypen ikke er VTA eller AFT og miljø er produksjon") {
            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)
            mockkObject(NaisEnv.current())
            coEvery { NaisEnv.current().isProdGCP() } returns true

            val dbo = avtaleDbo.copy(tiltakstypeId = TiltakstypeFixtures.Oppfolging.id)

            validator.validate(dbo, null).shouldBeLeft().shouldContain(
                ValidationError(
                    "tiltakstypeId",
                    "Avtaler kan bare opprettes når de gjelder for tiltakstypene AFT eller VTA",
                ),
            )
        }

        test("skal ikke feile når tiltakstypen ikke er VTA eller AFT og miljø ikke er produksjon") {
            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)
            mockkObject(NaisEnv.current())
            coEvery { NaisEnv.current().isProdGCP() } returns false

            val dbo = avtaleDbo.copy(tiltakstypeId = TiltakstypeFixtures.Oppfolging.id)

            validator.validate(dbo, null).shouldBeRight {
                dbo.tiltakstypeId.toString() shouldBe TiltakstypeFixtures.Oppfolging.id.toString()
            }
        }

        test("skal feile når opphav ikke er MR_ADMIN_FLATE") {
            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)

            val dbo = avtaleDbo.copy(opphav = ArenaMigrering.Opphav.ARENA)

            validator.validate(dbo, null).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Opphav må være MR_ADMIN_FLATE"),
            )
        }
    }

    context("når avtalen allerede eksisterer") {
        test("skal ikke kunne endre opphav") {
            avtaler.upsert(avtaleDbo.copy(opphav = ArenaMigrering.Opphav.ARENA, administratorer = listOf()))

            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)

            val dbo = avtaleDbo.copy(opphav = ArenaMigrering.Opphav.MR_ADMIN_FLATE)

            val previous = avtaler.get(avtaleDbo.id)
            validator.validate(dbo, previous).shouldBeLeft().shouldContain(
                ValidationError("opphav", "Avtalens opphav kan ikke endres"),
            )
        }

        test("skal ikke kunne endre felter med opphav fra Arena") {
            val avtaleMedEndringer = AvtaleDbo(
                id = avtaleDbo.id,
                navn = "Nytt navn",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                leverandorOrganisasjonsnummer = "999999999",
                leverandorUnderenheter = listOf("888888888"),
                leverandorKontaktpersonId = null,
                avtalenummer = "123456",
                startDato = LocalDate.now(),
                sluttDato = LocalDate.now().plusYears(1),
                url = "nav.no",
                administratorer = listOf("B123456"),
                avtaletype = Avtaletype.Rammeavtale,
                prisbetingelser = null,
                navEnheter = listOf("0300"),
                opphav = ArenaMigrering.Opphav.ARENA,
                antallPlasser = null,
                beskrivelse = null,
                faneinnhold = null,
            )

            avtaler.upsert(
                avtaleDbo.copy(
                    opphav = ArenaMigrering.Opphav.ARENA,
                    administratorer = listOf(),
                ),
            )

            val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)

            val previous = avtaler.get(avtaleDbo.id)
            validator.validate(avtaleMedEndringer, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    ValidationError("navn", "Navn kan ikke endres utenfor Arena"),
                    ValidationError("tiltakstypeId", "Tiltakstype kan ikke endres utenfor Arena"),
                    ValidationError("startDato", "Startdato kan ikke endres utenfor Arena"),
                    ValidationError("sluttDato", "Sluttdato kan ikke endres utenfor Arena"),
                    ValidationError("avtaletype", "Avtaletype kan ikke endres utenfor Arena"),
                    ValidationError("leverandorOrganisasjonsnummer", "Leverandøren kan ikke endres utenfor Arena"),
                ),
            )
        }

        context("når avtalen har gjennomføringer") {
            beforeAny {
                avtaler.upsert(avtaleDbo.copy(administratorer = listOf()))
                gjennomforinger.upsert(
                    TiltaksgjennomforingFixtures.Oppfolging1.copy(
                        avtaleId = avtaleDbo.id,
                        navRegion = "0400",
                        navEnheter = listOf("0502"),
                        arrangorOrganisasjonsnummer = "000000001",
                    ),
                )
            }

            afterAny {
                gjennomforinger.delete(TiltaksgjennomforingFixtures.Oppfolging1.id)
            }

            test("skal validere at data samsvarer med avtalens gjennomføringer") {
                val validator = AvtaleValidator(tiltakstyper, gjennomforinger, navEnheterService)

                val dbo = avtaleDbo.copy(
                    tiltakstypeId = TiltakstypeFixtures.VTA.id,
                    avtaletype = Avtaletype.Forhaandsgodkjent,
                    navEnheter = listOf("0400"),
                )

                val previous = avtaler.get(avtaleDbo.id)
                validator.validate(dbo, previous).shouldBeLeft().shouldContainExactlyInAnyOrder(
                    listOf(
                        ValidationError(
                            "tiltakstypeId",
                            "Tiltakstype kan ikke endres fordi det finnes gjennomføringer for avtalen",
                        ),
                        ValidationError(
                            "avtaletype",
                            "Avtaletype kan ikke endres fordi det finnes gjennomføringer for avtalen",
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
    }
})
