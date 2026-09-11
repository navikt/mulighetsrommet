package no.nav.mulighetsrommet.api.domain.avtale

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.NavEnhetFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.PrismodellFixtures
import no.nav.mulighetsrommet.api.domain.tiltak.PrismodellType
import no.nav.mulighetsrommet.model.Avtaletype
import no.nav.mulighetsrommet.model.FieldError
import no.nav.mulighetsrommet.model.Personopplysning
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.time.LocalDateTime

class AvtaleTest : FunSpec({
    context("medRammedetaljer") {
        test("kan ikke legges til for systembestemte (forhåndsgodkjente) avtaler") {
            val avtale = AvtaleFixtures.AFT

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError(
                        "/totalRamme",
                        "Rammedetaljer kan kun legges til anskaffet avtaler",
                    ),
                ),
            )
        }

        test("krever at alle prismodeller på avtalen har samme valuta") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(
                        PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK),
                        PrismodellFixtures.AnnenAvtaltPris.copy(valuta = Valuta.SEK),
                    ),
                ),
            )

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError(
                        "/totalRamme",
                        "Rammedetaljer kan kun legges til avtaler med én type valuta på prismodellene",
                    ),
                ),
            )
        }

        test("total ramme må være et positivt beløp") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            avtale.medRammedetaljer(totalRamme = -1, utbetaltArena = null).shouldBeLeft(
                listOf(
                    FieldError("/totalRamme", "Total ramme må være et positivt beløp"),
                ),
            )
        }

        test("utbetalt beløp fra Arena må være et positivt beløp") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = -1).shouldBeLeft(
                listOf(
                    FieldError("/utbetaltArena", "Utbetalt beløp fra Arena må være et positivt beløp"),
                ),
            )
        }

        test("fjerner rammedetaljer når begge felt er null") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
                rammedetaljer = Avtale.Rammedetaljer(totalRamme = 1000, utbetaltArena = 100, valuta = Valuta.NOK),
            )

            val oppdatert = avtale.medRammedetaljer(totalRamme = null, utbetaltArena = null).shouldBeRight()

            oppdatert.rammedetaljer.shouldBeNull()
        }

        test("samler opp alle feil samtidig") {
            val avtale = AvtaleFixtures.AFT

            avtale.medRammedetaljer(totalRamme = -1, utbetaltArena = -1).shouldBeLeft().shouldContainExactlyInAnyOrder(
                listOf(
                    FieldError("/totalRamme", "Rammedetaljer kan kun legges til anskaffet avtaler"),
                    FieldError("/totalRamme", "Total ramme må være et positivt beløp"),
                    FieldError("/utbetaltArena", "Utbetalt beløp fra Arena må være et positivt beløp"),
                ),
            )
        }

        test("legger til rammedetaljer med valuta hentet fra prismodellene når validering går bra") {
            val avtale = AvtaleFixtures.ARR.copy(
                prisinfo = Avtale.Prisinfo.Egendefinert(
                    listOf(PrismodellFixtures.AvtaltPrisPerManedsverk.copy(valuta = Valuta.NOK)),
                ),
            )

            val oppdatert = avtale.medRammedetaljer(totalRamme = 1000, utbetaltArena = null).shouldBeRight()

            oppdatert.rammedetaljer shouldBe Avtale.Rammedetaljer(
                totalRamme = 1000,
                utbetaltArena = null,
                valuta = Valuta.NOK,
            )
        }
    }

    context("medPrismodeller") {
        test("kan ikke endres for forhåndsgodkjente avtaler") {
            val avtale = AvtaleFixtures.AFT

            avtale.medPrismodeller(listOf(PrismodellFixtures.AnnenAvtaltPris)).shouldBeLeft(
                listOf(FieldError.of("Prismodell kan ikke endres for forhåndsgodkjente avtaler")),
            )
        }

        test("krever minst én prismodell") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.medPrismodeller(emptyList()).shouldBeLeft(
                listOf(FieldError("/prismodeller", "Minst én prismodell er påkrevd")),
            )
        }

        test("prismodelltype må være tillatt for avtalens tiltakskode") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.medPrismodeller(
                listOf(PrismodellFixtures.createPrismodell(type = PrismodellType.FAST_SATS_PER_AVTALT_PLASS_PER_MANED)),
            ).shouldBeLeft(
                listOf(
                    FieldError(
                        "/prismodeller/0/type",
                        "Fast sats per avtalt tiltaksplass per måned er ikke tillatt for tiltakskode OPPFOLGING",
                    ),
                ),
            )
        }

        test("FAST_SATS_PER_BENYTTET_PLASS_PER_MANED er forbeholdt systembestemte prismodeller") {
            val avtale = AvtaleFixtures.AFT.copy(
                avtaletype = Avtaletype.RAMMEAVTALE,
                prisinfo = Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.ForhandsgodkjentAft)),
            )

            avtale.medPrismodeller(listOf(PrismodellFixtures.ForhandsgodkjentAft)).shouldBeLeft(
                listOf(
                    FieldError(
                        "/prismodeller",
                        "Prismodell kan ikke opprettes med typen Fast sats per benyttet tiltaksplass per måned",
                    ),
                ),
            )
        }

        test("oppdaterer prisinfo til egendefinerte prismodeller når validering går bra") {
            val avtale = AvtaleFixtures.oppfolging

            val oppdatert = avtale.medPrismodeller(listOf(PrismodellFixtures.AnnenAvtaltPris)).shouldBeRight()

            oppdatert.prisinfo shouldBe Avtale.Prisinfo.Egendefinert(listOf(PrismodellFixtures.AnnenAvtaltPris))
        }
    }

    context("Personvern.of") {
        test("krever beskrivelse når ANNET er valgt") {
            Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = null,
                erBekreftet = true,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse er påkrevd når annet er valgt")),
            )

            Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "   ",
                erBekreftet = true,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse er påkrevd når annet er valgt")),
            )
        }

        test("beskrivelse kan ikke være lengre enn 300 tegn") {
            Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "a".repeat(301),
                erBekreftet = true,
            ).shouldBeLeft(
                listOf(FieldError("/personvern/annetBeskrivelse", "Beskrivelse kan maks være 300 tegn")),
            )
        }

        test("nullstiller beskrivelse når ANNET ikke er valgt") {
            val personvern = Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.NAVN),
                annetBeskrivelse = "Skal fjernes",
                erBekreftet = true,
            ).shouldBeRight()

            personvern.annetBeskrivelse.shouldBeNull()
        }

        test("oppretter personvern når validering går bra") {
            val personvern = Avtale.Personvern.of(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "En beskrivelse",
                erBekreftet = true,
            ).shouldBeRight()

            personvern shouldBe Avtale.Personvern(
                personopplysninger = setOf(Personopplysning.Type.ANNET),
                annetBeskrivelse = "En beskrivelse",
                erBekreftet = true,
            )
        }
    }

    context("VeilederInfo.of") {
        test("krever minst én Nav-region") {
            Avtale.VeilederInfo.of(
                beskrivelse = null,
                faneinnhold = null,
                navEnheter = setOf(NavEnhetFixtures.Gjovik),
            ).shouldBeLeft(
                listOf(
                    FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region"),
                    FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet"),
                ),
            )
        }

        test("krever minst én Nav-enhet innenfor de valgte regionene") {
            Avtale.VeilederInfo.of(
                beskrivelse = null,
                faneinnhold = null,
                navEnheter = setOf(NavEnhetFixtures.Innlandet),
            ).shouldBeLeft(
                listOf(FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet")),
            )
        }

        test("akkumulerer feil når verken region eller enhet er valgt") {
            Avtale.VeilederInfo.of(
                beskrivelse = null,
                faneinnhold = null,
                navEnheter = setOf(),
            ).shouldBeLeft(
                listOf(
                    FieldError("/veilederinformasjon/navRegioner", "Du må velge minst én Nav-region"),
                    FieldError("/veilederinformasjon/navKontorer", "Du må velge minst én Nav-enhet"),
                ),
            )
        }

        test("filtrerer bort enheter som ikke er regioner eller underenheter av valgte regioner") {
            val veilederinfo = Avtale.VeilederInfo.of(
                beskrivelse = "Beskrivelse",
                faneinnhold = null,
                navEnheter = setOf(NavEnhetFixtures.Innlandet, NavEnhetFixtures.Gjovik, NavEnhetFixtures.TiltakOslo),
            ).shouldBeRight()

            veilederinfo.navEnheter shouldContainExactlyInAnyOrder setOf(
                NavEnhetFixtures.Innlandet.enhetsnummer,
                NavEnhetFixtures.Gjovik.enhetsnummer,
            )
        }
    }
    context("avslutt") {
        test("krever at avtalen er aktiv") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Utkast)

            avtale.avslutt(LocalDateTime.now()).shouldBeLeft(
                listOf(
                    FieldError.of("Avtalen må være aktiv for å kunne avsluttes"),
                    FieldError.of("Avtalen kan ikke avsluttes før sluttdato"),
                ),
            )
        }

        test("krever at avsluttet-tidspunkt er etter sluttdato") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.avslutt(avtale.sluttDato!!.atStartOfDay()).shouldBeLeft(
                listOf(FieldError.of("Avtalen kan ikke avsluttes før sluttdato")),
            )
        }

        test("setter status til Avsluttet når avtalen er aktiv og tidspunktet er etter sluttdato") {
            val avtale = AvtaleFixtures.oppfolging

            avtale.avslutt(avtale.sluttDato!!.plusDays(1).atStartOfDay())
                .shouldBeRight().status shouldBe AvtaleStatus.Avsluttet
        }
    }

    context("avbryt") {
        test("kan avbryte avtale som er Utkast eller Aktiv") {
            val tidspunkt = LocalDateTime.now()

            AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Utkast)
                .avbryt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)
                .shouldBeRight().status shouldBe AvtaleStatus.Avbrutt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)

            AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Aktiv)
                .avbryt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)
                .shouldBeRight().status shouldBe AvtaleStatus.Avbrutt(tidspunkt, listOf(AvbrytAvtaleAarsak.ANNET), null)
        }

        test("kan ikke avbryte avtale som allerede er avbrutt") {
            val avtale = AvtaleFixtures.oppfolging.copy(
                status = AvtaleStatus.Avbrutt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null),
            )

            avtale.avbryt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null).shouldBeLeft(
                listOf(FieldError.of("Avtalen er allerede avbrutt")),
            )
        }

        test("kan ikke avbryte avtale som allerede er avsluttet") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Avsluttet)

            avtale.avbryt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null).shouldBeLeft(
                listOf(FieldError.of("Avtalen er allerede avsluttet")),
            )
        }
    }

    context("oppdaterVarighet") {
        test("setter avtalen til Aktiv når ny sluttdato ikke har passert") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Avsluttet)
            val today = LocalDate.of(2025, 1, 1)

            val oppdatert = avtale.oppdaterVarighet(today.plusDays(1), today)

            oppdatert.sluttDato shouldBe today.plusDays(1)
            oppdatert.status shouldBe AvtaleStatus.Aktiv
        }

        test("setter avtalen til Avsluttet når ny sluttdato har passert") {
            val avtale = AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Aktiv)
            val today = LocalDate.of(2025, 1, 1)

            val oppdatert = avtale.oppdaterVarighet(today.minusDays(1), today)

            oppdatert.sluttDato shouldBe today.minusDays(1)
            oppdatert.status shouldBe AvtaleStatus.Avsluttet
        }

        test("beholder status Utkast og Avbrutt uansett ny sluttdato") {
            val today = LocalDate.of(2025, 1, 1)

            AvtaleFixtures.oppfolging.copy(status = AvtaleStatus.Utkast)
                .oppdaterVarighet(today.minusDays(1), today).status shouldBe AvtaleStatus.Utkast

            val avbrutt = AvtaleStatus.Avbrutt(LocalDateTime.now(), listOf(AvbrytAvtaleAarsak.ANNET), null)
            AvtaleFixtures.oppfolging.copy(status = avbrutt)
                .oppdaterVarighet(today.minusDays(1), today).status shouldBe avbrutt
        }
    }
})
