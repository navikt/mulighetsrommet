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

        val enhet = NavEnhetDbo(
            enhetsnummer = "1000",
            navn = "Andeby",
            status = NavEnhetStatus.AKTIV,
            type = Norg2Type.LOKAL,
            overordnetEnhet = null,
        )

        beforeAny {
            enheter.upsert(enhet).shouldBeRight()
        }

        fun toDto(dbo: NavAnsattDbo) = dbo.run {
            NavAnsattDto(
                azureId = azureId,
                navIdent = navIdent,
                fornavn = fornavn,
                etternavn = etternavn,
                hovedenhet = NavAnsattDto.Hovedenhet(
                    enhetsnummer = hovedenhet,
                    navn = enhet.navn,
                ),
                mobilnummer = mobilnummer,
                epost = epost,
                roller = roller,
                skalSlettesDato = skalSlettesDato,
            )
        }

        test("CRUD") {
            val azureId = UUID.randomUUID()

            val ansatt = NavAnsattDbo(
                azureId = azureId,
                navIdent = "DD123456",
                fornavn = "Donald",
                etternavn = "Duck",
                hovedenhet = "1000",
                mobilnummer = "12345678",
                epost = "test@test.no",
                roller = listOf(NavAnsattRolle.BETABRUKER, NavAnsattRolle.KONTAKTPERSON),
            )

            ansatte.upsert(ansatt).shouldBeRight()

            ansatte.getByAzureId(ansatt.azureId) shouldBeRight toDto(ansatt)
            ansatte.getByNavIdent(ansatt.navIdent) shouldBeRight toDto(ansatt)

            ansatte.deleteByAzureId(ansatt.azureId).shouldBeRight()

            ansatte.getByAzureId(ansatt.azureId) shouldBeRight null
            ansatte.getByNavIdent(ansatt.navIdent) shouldBeRight null
        }

        test("Skal hente alle ansatte for en gitt rolle") {
            val ansatt1 = NavAnsattDbo(
                azureId = UUID.randomUUID(),
                navIdent = "D1",
                fornavn = "Donald",
                etternavn = "Duck",
                hovedenhet = "1000",
                mobilnummer = "12345678",
                epost = "donald@nav.no",
                roller = listOf(NavAnsattRolle.BETABRUKER),
            )

            val ansatt2 = NavAnsattDbo(
                azureId = UUID.randomUUID(),
                navIdent = "D2",
                fornavn = "Dolly",
                etternavn = "Duck",
                hovedenhet = "1000",
                mobilnummer = "12345678",
                epost = "dolly@nav.no",
                roller = listOf(NavAnsattRolle.KONTAKTPERSON),
            )

            val ansatt3 = NavAnsattDbo(
                azureId = UUID.randomUUID(),
                navIdent = "D3",
                fornavn = "Ole",
                etternavn = "Duck",
                hovedenhet = "1000",
                mobilnummer = "12345678",
                epost = "ole@nav.no",
                roller = listOf(NavAnsattRolle.BETABRUKER, NavAnsattRolle.KONTAKTPERSON),
            )

            ansatte.upsert(ansatt1).shouldBeRight()
            ansatte.upsert(ansatt2).shouldBeRight()
            ansatte.upsert(ansatt3).shouldBeRight()

            ansatte.getAll(
                roller = listOf(NavAnsattRolle.BETABRUKER),
            ) shouldBeRight listOf(toDto(ansatt1), toDto(ansatt3))
            ansatte.getAll(
                roller = listOf(NavAnsattRolle.KONTAKTPERSON),
            ) shouldBeRight listOf(toDto(ansatt2), toDto(ansatt3))
            ansatte.getAll(
                roller = listOf(NavAnsattRolle.BETABRUKER, NavAnsattRolle.KONTAKTPERSON),
            ) shouldBeRight listOf(toDto(ansatt3))
        }
    }
})
