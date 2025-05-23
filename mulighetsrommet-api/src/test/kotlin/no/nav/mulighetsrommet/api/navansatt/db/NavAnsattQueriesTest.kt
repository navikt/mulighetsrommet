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
import java.util.*

class NavAnsattQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("NavAnsattQueries") {
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

        MulighetsrommetTestDomain(
            navEnheter = listOf(enhet1, enhet2),
            ansatte = listOf(),
            arrangorer = listOf(),
            avtaler = listOf(),
        ).initialize(database.db)

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

        test("CRUD") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)

                queries.getByEntraObjectId(ansatt1.entraObjectId) shouldBe toDto(ansatt1, enhet1, setOf())
                queries.getByNavIdent(ansatt1.navIdent) shouldBe toDto(ansatt1, enhet1, setOf())

                queries.deleteByEntraObjectId(ansatt1.entraObjectId)

                queries.getByEntraObjectId(ansatt1.entraObjectId) shouldBe null
                queries.getByNavIdent(ansatt1.navIdent) shouldBe null
            }
        }

        test("oppdatere roller") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)

                val enRolle = setOf(
                    NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL),
                )
                queries.setRoller(ansatt1.navIdent, enRolle)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle

                val flereRoller = setOf(
                    NavAnsattRolle.generell(Rolle.KONTAKTPERSON),
                    NavAnsattRolle.generell(Rolle.ATTESTANT_UTBETALING),
                )
                queries.setRoller(ansatt1.navIdent, flereRoller)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                val ingenRoller = setOf<NavAnsattRolle>()
                queries.setRoller(ansatt1.navIdent, ingenRoller)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
            }
        }

        test("oppdatere roller med kontortilhørlighet") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)

                val enRolle = setOf(
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(NavEnhetNummer("1000"))),
                )
                queries.setRoller(ansatt1.navIdent, enRolle)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle

                val flereRoller = setOf(
                    NavAnsattRolle.kontorspesifikk(Rolle.BESLUTTER_TILSAGN, setOf(NavEnhetNummer("1000"))),
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf(NavEnhetNummer("2000"))),
                )
                queries.setRoller(ansatt1.navIdent, flereRoller)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                val ingenRoller = setOf<NavAnsattRolle>(
                    NavAnsattRolle.kontorspesifikk(Rolle.ATTESTANT_UTBETALING, setOf()),
                )
                queries.setRoller(ansatt1.navIdent, ingenRoller)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
            }
        }

        test("hent ansatte gitt rolle") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                val generell = NavAnsattRolle.generell(Rolle.TILTAKADMINISTRASJON_GENERELL)

                val kontaktperson = NavAnsattRolle.generell(Rolle.KONTAKTPERSON)

                queries.upsert(ansatt1)
                queries.setRoller(ansatt1.navIdent, setOf(generell))

                queries.upsert(ansatt2)
                queries.setRoller(ansatt2.navIdent, setOf(kontaktperson))

                queries.upsert(ansatt3)
                queries.setRoller(ansatt3.navIdent, setOf(generell, kontaktperson))

                val expectedAnsatt1 = toDto(ansatt1, enhet1, setOf(generell))
                val expectedAnsatt2 = toDto(ansatt2, enhet2, setOf(kontaktperson))
                val expectedAnsatt3 = toDto(ansatt3, enhet1, setOf(generell, kontaktperson))

                queries.getAll(
                    rollerContainsAll = listOf(generell),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt3)

                queries.getAll(
                    rollerContainsAll = listOf(kontaktperson),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2, expectedAnsatt3)

                queries.getAll(
                    rollerContainsAll = listOf(generell, kontaktperson),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt3)
            }
        }

        test("hent ansatte gitt rolle med kontortilhørlighet") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

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

                queries.upsert(ansatt1)
                queries.setRoller(ansatt1.navIdent, setOf(beslutterTilsagnAndeby))

                queries.upsert(ansatt2)
                queries.setRoller(ansatt2.navIdent, setOf(beslutterTilsagnForBeggeKontor))

                val expectedAnsatt1 = toDto(ansatt1, enhet1, setOf(beslutterTilsagnAndeby))
                val expectedAnsatt2 = toDto(ansatt2, enhet2, setOf(beslutterTilsagnForBeggeKontor))

                queries.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnAndeby),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt2)

                queries.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnGaseby),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2)

                queries.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnUtenKontor),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt2)

                queries.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnForBeggeKontor),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2)

                queries.getAll(
                    rollerContainsAll = listOf(beslutterTilsagnForUkjentKontor),
                ) shouldContainExactlyInAnyOrder listOf()
            }
        }

        test("hent ansatte gitt hovedenhet") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)
                queries.upsert(ansatt2)
                queries.upsert(ansatt3)

                val expectedAnsatt1 = toDto(ansatt1, enhet1, setOf())
                val expectedAnsatt2 = toDto(ansatt2, enhet2, setOf())
                val expectedAnsatt3 = toDto(ansatt3, enhet1, setOf())

                queries.getAll(hovedenhetIn = listOf()) shouldBe listOf()
                queries.getAll(
                    hovedenhetIn = listOf(NavEnhetNummer("1000")),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt1, expectedAnsatt3)

                queries.getAll(
                    hovedenhetIn = listOf(NavEnhetNummer("2000")),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2)

                queries.getAll(
                    hovedenhetIn = listOf(NavEnhetNummer("1000"), NavEnhetNummer("2000")),
                ) shouldContainExactlyInAnyOrder listOf(expectedAnsatt2, expectedAnsatt1, expectedAnsatt3)
            }
        }
    }
})
