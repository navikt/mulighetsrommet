package no.nav.mulighetsrommet.api.repositories

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.clients.norg2.Norg2Type
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavAnsattRolle
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetDbo
import no.nav.mulighetsrommet.api.domain.dbo.NavEnhetStatus
import no.nav.mulighetsrommet.api.domain.dto.NavAnsattDto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import java.util.*

class NavAnsattRepositoryTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    context("NavAnsattRepository") {
        val enheter = NavEnhetRepository(database.db)
        val ansatte = NavAnsattRepository(database.db)

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

        beforeAny {
            enheter.upsert(enhet1).shouldBeRight()
            enheter.upsert(enhet2).shouldBeRight()
        }

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
            navIdent = "D1",
            fornavn = "Donald",
            etternavn = "Duck",
            hovedenhet = "1000",
            mobilnummer = "12345678",
            epost = "donald@nav.no",
            roller = setOf(NavAnsattRolle.BETABRUKER, NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
        )

        val ansatt2 = NavAnsattDbo(
            azureId = UUID.randomUUID(),
            navIdent = "D2",
            fornavn = "Dolly",
            etternavn = "Duck",
            hovedenhet = "2000",
            mobilnummer = "12345678",
            epost = "dolly@nav.no",
            roller = setOf(NavAnsattRolle.KONTAKTPERSON),
        )

        val ansatt3 = NavAnsattDbo(
            azureId = UUID.randomUUID(),
            navIdent = "D3",
            fornavn = "Ole",
            etternavn = "Duck",
            hovedenhet = "1000",
            mobilnummer = "12345678",
            epost = "ole@nav.no",
            roller = setOf(NavAnsattRolle.BETABRUKER, NavAnsattRolle.KONTAKTPERSON, NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
        )

        test("CRUD") {
            ansatte.upsert(ansatt1).shouldBeRight()

            ansatte.getByAzureId(ansatt1.azureId) shouldBeRight toDto(ansatt1, enhet1)
            ansatte.getByNavIdent(ansatt1.navIdent) shouldBeRight toDto(ansatt1, enhet1)

            ansatte.deleteByAzureId(ansatt1.azureId).shouldBeRight()

            ansatte.getByAzureId(ansatt1.azureId) shouldBeRight null
            ansatte.getByNavIdent(ansatt1.navIdent) shouldBeRight null
        }

        test("hent ansatte gitt rolle") {
            ansatte.upsert(ansatt1).shouldBeRight()
            ansatte.upsert(ansatt2).shouldBeRight()
            ansatte.upsert(ansatt3).shouldBeRight()

            ansatte.getAll(
                roller = listOf(NavAnsattRolle.BETABRUKER, NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
            ) shouldBeRight listOf(toDto(ansatt1, enhet1), toDto(ansatt3, enhet1))
            ansatte.getAll(
                roller = listOf(NavAnsattRolle.KONTAKTPERSON),
            ) shouldBeRight listOf(toDto(ansatt2, enhet2), toDto(ansatt3, enhet1))
            ansatte.getAll(
                roller = listOf(NavAnsattRolle.BETABRUKER, NavAnsattRolle.KONTAKTPERSON, NavAnsattRolle.TILTAKADMINISTRASJON_GENERELL),
            ) shouldBeRight listOf(toDto(ansatt3, enhet1))
        }

        test("hent ansatte gitt hovedenhet") {
            ansatte.upsert(ansatt1).shouldBeRight()
            ansatte.upsert(ansatt2).shouldBeRight()
            ansatte.upsert(ansatt3).shouldBeRight()

            ansatte.getAll(hovedenhetIn = listOf()) shouldBeRight listOf()
            ansatte.getAll(hovedenhetIn = listOf("1000")) shouldBeRight listOf(
                toDto(ansatt1, enhet1),
                toDto(ansatt3, enhet1),
            )
            ansatte.getAll(hovedenhetIn = listOf("2000")) shouldBeRight listOf(
                toDto(ansatt2, enhet2),
            )
            ansatte.getAll(hovedenhetIn = listOf("1000", "2000")) shouldBeRight listOf(
                toDto(ansatt2, enhet2),
                toDto(ansatt1, enhet1),
                toDto(ansatt3, enhet1),
            )
        }
    }
})
