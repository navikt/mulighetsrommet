package no.nav.mulighetsrommet.api.persistence.navansatt

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.admin.navansatt.NavAnsattDto
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsatt
import no.nav.mulighetsrommet.api.domain.navansatt.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.navansatt.Rolle
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhet
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.navenhet.NavEnhetType
import no.nav.mulighetsrommet.api.persistence.SqlAdminDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

class NavAnsattQueriesTest : FunSpec({
    val database = extension(SqlAdminDatabaseTestListener())

    val enhet1 = NavEnhet(
        enhetsnummer = NavEnhetNummer("1000"),
        navn = "Andeby",
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.LOKAL,
        overordnetEnhet = null,
    )

    val enhet2 = NavEnhet(
        enhetsnummer = NavEnhetNummer("2000"),
        navn = "Gåseby",
        status = NavEnhetStatus.AKTIV,
        type = NavEnhetType.LOKAL,
        overordnetEnhet = null,
    )

    fun createAnsatt(
        navIdent: NavIdent,
        fornavn: String,
        etternavn: String,
        hovedenhet: NavEnhet,
        roller: Set<NavAnsattRolle> = setOf(),
    ) = NavAnsatt.opprett(
        entraObjectId = UUID.randomUUID(),
        navIdent = navIdent,
        fornavn = fornavn,
        etternavn = etternavn,
        hovedenhet = hovedenhet.enhetsnummer,
        mobilnummer = "12345678",
        epost = "${fornavn.lowercase()}@nav.no",
        roller = roller,
    )

    context("NavAnsattQueries") {
        val ansatt1 = createAnsatt(NavIdent("D1"), "Donald", "Duck", enhet1)

        test("CRUD") {
            database.runAndRollback {
                repository.navEnhet.save(enhet1)
                repository.navAnsatt.save(ansatt1)

                repository.navAnsatt.getByEntraObjectId(ansatt1.entraObjectId) shouldBe ansatt1
                repository.navAnsatt.get(ansatt1.navIdent) shouldBe ansatt1

                repository.navAnsatt.deleteByEntraObjectId(ansatt1.entraObjectId)

                repository.navAnsatt.getByEntraObjectId(ansatt1.entraObjectId) shouldBe null
                repository.navAnsatt.get(ansatt1.navIdent) shouldBe null
            }
        }

        test("oppdatere roller") {
            database.runAndRollback {
                repository.navEnhet.save(enhet1)
                repository.navAnsatt.save(ansatt1)

                val enRolle = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL),
                )
                repository.navAnsatt.save(ansatt1.medRoller(enRolle))
                repository.navAnsatt.get(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle

                val flereRoller = setOf(
                    NavAnsattRolle.generell(Rolle.KONTAKTPERSON),
                    NavAnsattRolle.generell(Rolle.OKONOMI_LES),
                    NavAnsattRolle.generell(Rolle.OPPFOLGER_GJENNOMFORING),
                    NavAnsattRolle.generell(Rolle.TILTAKSTYPER_SKRIV),
                )
                repository.navAnsatt.save(ansatt1.medRoller(flereRoller))
                repository.navAnsatt.get(ansatt1.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                val ingenRoller = setOf<NavAnsattRolle>()
                repository.navAnsatt.save(ansatt1.medRoller(ingenRoller))
                repository.navAnsatt.get(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
            }
        }

        test("oppdatere roller med kontortilhørlighet") {
            database.runAndRollback {
                repository.navEnhet.save(enhet1)
                repository.navEnhet.save(enhet2)
                repository.navAnsatt.save(ansatt1)

                val enRolle = setOf(
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(NavEnhetNummer("1000"))),
                )
                repository.navAnsatt.save(ansatt1.medRoller(enRolle))
                repository.navAnsatt.get(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle

                val flereRoller = setOf(
                    NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(NavEnhetNummer("1000"))),
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(NavEnhetNummer("2000"))),
                )
                repository.navAnsatt.save(ansatt1.medRoller(flereRoller))
                repository.navAnsatt.get(ansatt1.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                val ingenRoller = setOf(
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf()),
                )
                repository.navAnsatt.save(ansatt1.medRoller(ingenRoller))
                repository.navAnsatt.get(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
            }
        }
    }

    context("getNavAnsattDto") {
        val ansatt1 = createAnsatt(NavIdent("D1"), "Donald", "Duck", enhet1)
        val ansatt2 = createAnsatt(NavIdent("D2"), "Dolly", "Duck", enhet2)
        val ansatt3 = createAnsatt(NavIdent("D3"), "Ole", "Duck", enhet1)

        test("henter ansatt med hovedenhet") {
            database.runAndRollback {
                repository.navEnhet.save(enhet1)
                repository.navAnsatt.save(ansatt1)

                queries.navAnsattDto.getByNavIdent(NavIdent("D1")) shouldBe NavAnsattDto(
                    navIdent = ansatt1.navIdent,
                    fornavn = "Donald",
                    etternavn = "Duck",
                    hovedenhet = NavAnsattDto.Hovedenhet(NavEnhetNummer("1000"), "Andeby"),
                    mobilnummer = "12345678",
                    epost = "donald@nav.no",
                    roller = listOf(),
                )
            }
        }

        test("hent alle ansatte") {
            database.runAndRollback {
                repository.navEnhet.save(enhet1)
                repository.navEnhet.save(enhet2)

                repository.navAnsatt.save(ansatt1)
                repository.navAnsatt.save(ansatt2)
                repository.navAnsatt.save(ansatt3)

                queries.navAnsattDto.getAll() shouldContainExactlyNavIdent listOf(ansatt1, ansatt2, ansatt3)
            }
        }

        test("hent ansatte gitt rolle") {
            database.runAndRollback {
                repository.navEnhet.save(enhet1)
                repository.navEnhet.save(enhet2)

                val generell = NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)
                val kontaktperson = NavAnsattRolle.generell(Rolle.KONTAKTPERSON)

                val expectedAnsatt1 = ansatt1.medRoller(setOf(generell))
                val expectedAnsatt2 = ansatt2.medRoller(setOf(kontaktperson))
                val expectedAnsatt3 = ansatt3.medRoller(setOf(generell, kontaktperson))

                repository.navAnsatt.save(expectedAnsatt1)
                repository.navAnsatt.save(expectedAnsatt2)
                repository.navAnsatt.save(expectedAnsatt3)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(generell),
                ) shouldContainExactlyNavIdent listOf(expectedAnsatt1, expectedAnsatt3)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(kontaktperson),
                ) shouldContainExactlyNavIdent listOf(expectedAnsatt2, expectedAnsatt3)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(generell, kontaktperson),
                ) shouldContainExactlyNavIdent listOf(expectedAnsatt3)
            }
        }

        test("hent ansatte gitt rolle med kontortilhørlighet") {
            database.runAndRollback {
                repository.navEnhet.save(enhet1)
                repository.navEnhet.save(enhet2)

                val beslutterTilsagnAndeby =
                    NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(NavEnhetNummer("1000")))
                val beslutterTilsagnGaseby =
                    NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(NavEnhetNummer("2000")))
                val beslutterTilsagnUtenKontor =
                    NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf())
                val beslutterTilsagnForBeggeKontor =
                    NavAnsattRolle.kontorspesifikk(
                        Rolle.BESLUTTER_TILSAGN,
                        setOf(NavEnhetNummer("1000"), NavEnhetNummer("2000")),
                    )
                val beslutterTilsagnForUkjentKontor =
                    NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(NavEnhetNummer("3000")))

                val expectedAnsatt1 = ansatt1.medRoller(setOf(beslutterTilsagnAndeby))
                val expectedAnsatt2 = ansatt2.medRoller(setOf(beslutterTilsagnForBeggeKontor))

                repository.navAnsatt.save(expectedAnsatt1)
                repository.navAnsatt.save(expectedAnsatt2)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnAndeby),
                ) shouldContainExactlyNavIdent listOf(expectedAnsatt1, expectedAnsatt2)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnGaseby),
                ) shouldContainExactlyNavIdent listOf(expectedAnsatt2)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnUtenKontor),
                ) shouldContainExactlyNavIdent listOf(expectedAnsatt1, expectedAnsatt2)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnForBeggeKontor),
                ) shouldContainExactlyNavIdent listOf(expectedAnsatt2)

                queries.navAnsattDto.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnForUkjentKontor),
                ) shouldContainExactlyNavIdent listOf()
            }
        }
    }
})

private infix fun Collection<NavAnsattDto>.shouldContainExactlyNavIdent(listOf: Collection<NavAnsatt>) {
    map { it.navIdent }.shouldContainExactlyInAnyOrder(listOf.map { it.navIdent })
}
