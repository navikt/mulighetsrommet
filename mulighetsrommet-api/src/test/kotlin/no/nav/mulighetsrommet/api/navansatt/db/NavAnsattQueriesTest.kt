package no.nav.mulighetsrommet.api.navansatt.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
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

        fun toDto(ansatt: NavAnsattDbo, enhet: NavEnhetDbo) = ansatt.run {
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
            roller = setOf(NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
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
            roller = setOf(NavAnsattRolle.KONTAKTPERSON),
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
            roller = setOf(NavAnsattRolle.KONTAKTPERSON, NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
            skalSlettesDato = null,
        )

        test("CRUD") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)

                queries.getByAzureId(ansatt1.azureId) shouldBe toDto(ansatt1, enhet1)
                queries.getByNavIdent(ansatt1.navIdent) shouldBe toDto(ansatt1, enhet1)

                queries.deleteByAzureId(ansatt1.azureId)

                queries.getByAzureId(ansatt1.azureId) shouldBe null
                queries.getByNavIdent(ansatt1.navIdent) shouldBe null
            }
        }

        test("hent ansatte gitt rolle") {
            database.runAndRollback { session ->
                val queries = NavAnsattQueries(session)

                queries.upsert(ansatt1)
                queries.upsert(ansatt2)
                queries.upsert(ansatt3)

                queries.getAll(
                    roller = listOf(NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
                ) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt1, enhet1),
                    toDto(ansatt3, enhet1),
                )
                queries.getAll(
                    roller = listOf(NavAnsattRolle.KONTAKTPERSON),
                ) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt2, enhet2),
                    toDto(ansatt3, enhet1),
                )
                queries.getAll(
                    roller = listOf(NavAnsattRolle.KONTAKTPERSON, NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
                ) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt3, enhet1),
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
                    toDto(ansatt1, enhet1),
                    toDto(ansatt3, enhet1),
                )
                queries.getAll(hovedenhetIn = listOf("2000")) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt2, enhet2),
                )
                queries.getAll(hovedenhetIn = listOf("1000", "2000")) shouldContainExactlyInAnyOrder listOf(
                    toDto(ansatt2, enhet2),
                    toDto(ansatt1, enhet1),
                    toDto(ansatt3, enhet1),
                )
            }
        }
    }
})
