package no.nav.mulighetsrommet.api.navansatt.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattDto
import no.nav.mulighetsrommet.api.navansatt.model.NavAnsattRolle
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetDbo
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetStatus
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.NavIdent
import java.util.*

class NavAnsattQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    context("NavAnsattQueries") {
        val enhet1 = NavEnhetDbo(
            enhetsnummer = "1000",
            navn = "Andeby",
            status = NavEnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
            overordnetEnhet = null,
        )

        val enhet2 = NavEnhetDbo(
            enhetsnummer = "2000",
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
            NavAnsattDto(
                azureId = azureId,
                navIdent = navIdent,
                fornavn = fornavn,
                etternavn = etternavn,
                hovedenhet = NavAnsattDto.Hovedenhet(
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
            azureId = UUID.randomUUID(),
            navIdent = NavIdent("D1"),
            fornavn = "Donald",
            etternavn = "Duck",
            hovedenhet = "1000",
            mobilnummer = "12345678",
            epost = "donald@nav.no",
            skalSlettesDato = null,
        )

        val ansatt2 = NavAnsattDbo(
            azureId = UUID.randomUUID(),
            navIdent = NavIdent("D2"),
            fornavn = "Dolly",
            etternavn = "Duck",
            hovedenhet = "2000",
            mobilnummer = "12345678",
            epost = "dolly@nav.no",
            skalSlettesDato = null,
        )

        val ansatt3 = NavAnsattDbo(
            azureId = UUID.randomUUID(),
            navIdent = NavIdent("D3"),
            fornavn = "Ole",
            etternavn = "Duck",
            hovedenhet = "1000",
            mobilnummer = "12345678",
            epost = "ole@nav.no",
            skalSlettesDato = null,
        )

        test("CRUD") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)

                queries.getByAzureId(ansatt1.azureId) shouldBe toDto(ansatt1, enhet1, setOf())
                queries.getByNavIdent(ansatt1.navIdent) shouldBe toDto(ansatt1, enhet1, setOf())

                queries.deleteByAzureId(ansatt1.azureId)

                queries.getByAzureId(ansatt1.azureId) shouldBe null
                queries.getByNavIdent(ansatt1.navIdent) shouldBe null
            }
        }

        test("oppdatere roller") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)

                val enRolle = setOf(NavAnsattRolle.KONTAKTPERSON)
                queries.setRoller(ansatt1.navIdent, enRolle)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe enRolle

                val flereRoller = setOf(
                    NavAnsattRolle.BESLUTTER_TILSAGN,
                    NavAnsattRolle.ATTESTANT_UTBETALING,
                )
                queries.setRoller(ansatt1.navIdent, flereRoller)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe flereRoller

                val ingenRoller = setOf<NavAnsattRolle>()
                queries.setRoller(ansatt1.navIdent, ingenRoller)
                queries.getByNavIdent(ansatt1.navIdent).shouldNotBeNull().roller shouldBe ingenRoller
            }
        }

        test("hent ansatte gitt rolle") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                val rolleTiltaksadministrasjon = NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL

                val rolleKontaktperson = NavAnsattRolle.KONTAKTPERSON

                queries.upsert(ansatt1)
                queries.setRoller(ansatt1.navIdent, setOf(rolleTiltaksadministrasjon))

                queries.upsert(ansatt2)
                queries.setRoller(ansatt2.navIdent, setOf(rolleKontaktperson))

                queries.upsert(ansatt3)
                queries.setRoller(ansatt3.navIdent, setOf(rolleTiltaksadministrasjon, rolleKontaktperson))

                queries.getAll(
                    roller = listOf(NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
                ) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt1, enhet1, setOf(rolleTiltaksadministrasjon)),
                    toDto(ansatt3, enhet1, setOf(rolleTiltaksadministrasjon, rolleKontaktperson)),
                )
                queries.getAll(
                    roller = listOf(NavAnsattRolle.KONTAKTPERSON),
                ) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt2, enhet2, setOf(rolleKontaktperson)),
                    toDto(ansatt3, enhet1, setOf(rolleTiltaksadministrasjon, rolleKontaktperson)),
                )
                queries.getAll(
                    roller = listOf(NavAnsattRolle.KONTAKTPERSON, NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
                ) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt3, enhet1, setOf(rolleTiltaksadministrasjon, rolleKontaktperson)),
                )
            }
        }

        test("hent ansatte gitt hovedenhet") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)
                queries.upsert(ansatt2)
                queries.upsert(ansatt3)

                queries.getAll(hovedenhetIn = listOf()) shouldBe listOf()
                queries.getAll(hovedenhetIn = listOf("1000")) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt1, enhet1, setOf()),
                    toDto(ansatt3, enhet1, setOf()),
                )
                queries.getAll(hovedenhetIn = listOf("2000")) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt2, enhet2, setOf()),
                )
                queries.getAll(hovedenhetIn = listOf("1000", "2000")) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt2, enhet2, setOf()),
                    toDto(ansatt1, enhet1, setOf()),
                    toDto(ansatt3, enhet1, setOf()),
                )
            }
        }
    }
})
