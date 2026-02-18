package no.nav.mulighetsrommet.api.navansatt.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsatt
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navansatt.model.Rolle
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.NavIdent
import java.util.UUID

class NavAnsattQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val enhet1 = NavEnhetDbo(
        enhetsnummer = NavEnhetNummer("1000"),
        navn = "Andeby",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = null,
    )

    val enhet2 = NavEnhetDbo(
        enhetsnummer = NavEnhetNummer("2000"),
        navn = "Gåseby",
        status = NavEnhetStatus.AKTIV,
        type = Norg2Type.LOKAL,
        overordnetEnhet = null,
    )

    fun toDto(ansatt: NavAnsattDbo, enhet: NavEnhetDbo, roller: Set<NavAnsattRolle>) = ansatt.run {
        NavAnsatt(
            entraObjectId = entraObjectId,
            navIdent = navIdent,
            fornavn = fornavn,
            etternavn = etternavn,
            hovedenhet = NavAnsatt.Hovedenhet(
                enhetsnummer = enhet.enhetsnummer,
                navn = enhet.navn,
            ),
            mobilnummer = mobilnummer,
            epost = epost,
            roller = roller,
            skalSlettesDato = skalSlettesDato,
        )
    }

    val ansatt1 = NavAnsattDbo(
        entraObjectId = UUID.randomUUID(),
        navIdent = NavIdent("D1"),
        fornavn = "Donald",
        etternavn = "Duck",
        hovedenhet = NavEnhetNummer("1000"),
        mobilnummer = "12345678",
        epost = "donald@nav.no",
        skalSlettesDato = null,
    )

    val ansatt2 = NavAnsattDbo(
        entraObjectId = UUID.randomUUID(),
        navIdent = NavIdent("D2"),
        fornavn = "Dolly",
        etternavn = "Duck",
        hovedenhet = NavEnhetNummer("2000"),
        mobilnummer = "12345678",
        epost = "dolly@nav.no",
        skalSlettesDato = null,
    )

    val ansatt3 = NavAnsattDbo(
        entraObjectId = UUID.randomUUID(),
        navIdent = NavIdent("D3"),
        fornavn = "Ole",
        etternavn = "Duck",
        hovedenhet = NavEnhetNummer("1000"),
        mobilnummer = "12345678",
        epost = "ole@nav.no",
        skalSlettesDato = null,
    )

    context("NavAnsattQueries") {
        MulighetsrommetTestDomain(
            navEnheter = listOf(enhet1, enhet2),
            ansatte = listOf(),
            arrangorer = listOf(),
        ).initialize(database.db)

        test("CRUD") {
            database.runAndRollback {
                queries.ansatt.upsert(ansatt1)

                queries.ansatt.getByEntraObjectId(ansatt1.entraObjectId) shouldBe toDto(ansatt1, enhet1, setOf())
                queries.ansatt.getByNavIdent(ansatt1.navIdent) shouldBe toDto(ansatt1, enhet1, setOf())

                queries.ansatt.deleteByEntraObjectId(ansatt1.entraObjectId)

                queries.ansatt.getByEntraObjectId(ansatt1.entraObjectId) shouldBe null
                queries.ansatt.getByNavIdent(ansatt1.navIdent) shouldBe null
            }
        }

        test("oppdatere roller") {
            database.runAndRollback {
                queries.ansatt.upsert(ansatt1)

                val enRolle = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL),
                )
                queries.ansatt.setRoller(ansatt1.navIdent, enRolle)
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle

                val flereRoller = setOf(
                    NavAnsattRolle.generell(Rolle.KONTAKTPERSON),
                    NavAnsattRolle.generell(Rolle.OKONOMI_LES),
                    NavAnsattRolle.generell(Rolle.OPPFOLGER_GJENNOMFORING),
                )
                queries.ansatt.setRoller(ansatt1.navIdent, flereRoller)
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                val ingenRoller = setOf<NavAnsattRolle>()
                queries.ansatt.setRoller(ansatt1.navIdent, ingenRoller)
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
            }
        }

        test("oppdatere roller med kontortilhørlighet") {
            database.runAndRollback {
                queries.ansatt.upsert(ansatt1)

                val enRolle = setOf(
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(NavEnhetNummer("1000"))),
                )
                queries.ansatt.setRoller(ansatt1.navIdent, enRolle)
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle

                val flereRoller = setOf(
                    NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(NavEnhetNummer("1000"))),
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(NavEnhetNummer("2000"))),
                )
                queries.ansatt.setRoller(ansatt1.navIdent, flereRoller)
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                val ingenRoller = setOf<NavAnsattRolle>(
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf()),
                )
                queries.ansatt.setRoller(ansatt1.navIdent, ingenRoller)
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
            }
        }

        test("hent ansatte gitt rolle") {
            database.runAndRollback {
                val generell = NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)

                val kontaktperson = NavAnsattRolle.generell(Rolle.KONTAKTPERSON)

                queries.ansatt.upsert(ansatt1)
                queries.ansatt.setRoller(ansatt1.navIdent, setOf(generell))

                queries.ansatt.upsert(ansatt2)
                queries.ansatt.setRoller(ansatt2.navIdent, setOf(kontaktperson))

                queries.ansatt.upsert(ansatt3)
                queries.ansatt.setRoller(ansatt3.navIdent, setOf(generell, kontaktperson))

                val expectedAnsatt1 = toDto(ansatt1, enhet1, setOf(generell))
                val expectedAnsatt2 = toDto(ansatt2, enhet2, setOf(kontaktperson))
                val expectedAnsatt3 = toDto(ansatt3, enhet1, setOf(generell, kontaktperson))

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(generell),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt3)

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(kontaktperson),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2, expectedAnsatt3)

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(generell, kontaktperson),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt3)
            }
        }

        test("hent ansatte gitt rolle med kontortilhørlighet") {
            database.runAndRollback {
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

                queries.ansatt.upsert(ansatt1)
                queries.ansatt.setRoller(ansatt1.navIdent, setOf(beslutterTilsagnAndeby))

                queries.ansatt.upsert(ansatt2)
                queries.ansatt.setRoller(ansatt2.navIdent, setOf(beslutterTilsagnForBeggeKontor))

                val expectedAnsatt1 = toDto(ansatt1, enhet1, setOf(beslutterTilsagnAndeby))
                val expectedAnsatt2 = toDto(ansatt2, enhet2, setOf(beslutterTilsagnForBeggeKontor))

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnAndeby),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt2)

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnGaseby),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2)

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnUtenKontor),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt2)

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnForBeggeKontor),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2)

                queries.ansatt.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnForUkjentKontor),
                ) shouldContainExactlyInAnyOrder listOf()
            }
        }

        test("hent ansatte gitt hovedenhet") {
            database.runAndRollback {
                queries.ansatt.upsert(ansatt1)
                queries.ansatt.upsert(ansatt2)
                queries.ansatt.upsert(ansatt3)

                val expectedAnsatt1 = toDto(ansatt1, enhet1, setOf())
                val expectedAnsatt2 = toDto(ansatt2, enhet2, setOf())
                val expectedAnsatt3 = toDto(ansatt3, enhet1, setOf())

                queries.ansatt.getAll(hovedenhetIn = listOf()) shouldBe listOf()
                queries.ansatt.getAll(
                    hovedenhetIn = listOf(NavEnhetNummer("1000")),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt3)

                queries.ansatt.getAll(
                    hovedenhetIn = listOf(NavEnhetNummer("2000")),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2)

                queries.ansatt.getAll(
                    hovedenhetIn = listOf(NavEnhetNummer("1000"), NavEnhetNummer("2000")),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2, expectedAnsatt1, expectedAnsatt3)
            }
        }
    }

    context("batch") {
        MulighetsrommetTestDomain(
            navEnheter = listOf(enhet1, enhet2),
            ansatte = listOf(),
            arrangorer = listOf(),
        ).initialize(database.db)

        test("upsert batch") {
            database.runAndRollback {
                queries.ansatt.upsertBatch(listOf(ansatt1, ansatt2))

                queries.ansatt.getByEntraObjectId(ansatt1.entraObjectId) shouldBe toDto(ansatt1, enhet1, setOf())
                queries.ansatt.getByEntraObjectId(ansatt2.entraObjectId) shouldBe toDto(ansatt2, enhet2, setOf())
            }
        }

        test("oppdatere roller batch") {
            database.runAndRollback {
                queries.ansatt.upsert(ansatt1)
                queries.ansatt.upsert(ansatt2)

                val ingenRoller = setOf<NavAnsattRolle>()
                val enRolle = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL),
                )
                val flereRoller = setOf(
                    NavAnsattRolle.generell(Rolle.KONTAKTPERSON),
                    NavAnsattRolle.generell(Rolle.OKONOMI_LES),
                    NavAnsattRolle.generell(Rolle.OPPFOLGER_GJENNOMFORING),
                )

                queries.ansatt.setRollerBatch(
                    mapOf(
                        ansatt1.navIdent to enRolle,
                        ansatt2.navIdent to flereRoller,
                    ),
                )
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle
                queries.ansatt.getByNavIdent(ansatt2.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                // Endre
                queries.ansatt.setRollerBatch(
                    mapOf(
                        ansatt1.navIdent to ingenRoller,
                        ansatt2.navIdent to enRolle,
                    ),
                )
                queries.ansatt.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
                queries.ansatt.getByNavIdent(ansatt2.navIdent).shouldNotBeNull().roller shouldBe enRolle
            }
        }
    }
})
