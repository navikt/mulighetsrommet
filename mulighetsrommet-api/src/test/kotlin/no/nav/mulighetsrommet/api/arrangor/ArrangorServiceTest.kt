package no.nav.mulighetsrommet.api.arrangor

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotliquery.Query
import no.nav.mulighetsrommet.api.arrangor.model.ArrangorDto
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.brreg.*
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import java.time.LocalDate
import java.util.*

class ArrangorServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val underenhet = BrregUnderenhetDto(
        organisasjonsnummer = Organisasjonsnummer("234567891"),
        organisasjonsform = "BEDR",
        navn = "Underenhet til Testbedriften AS",
        overordnetEnhet = Organisasjonsnummer("123456789"),
    )
    val hovedenhet = BrregHovedenhetDto(
        organisasjonsnummer = Organisasjonsnummer("123456789"),
        organisasjonsform = "AS",
        navn = "Testbedriften AS",
        postadresse = null,
        forretningsadresse = null,
    )

    context("get or sync arrangør fra brreg") {
        val brregClient: BrregClient = mockk()
        val arrangorService = ArrangorService(database.db, brregClient)

        afterEach {
            clearAllMocks()
            database.truncateAll()
        }

        test("skal synkronisere hovedenhet uten underenheter fra brreg til databasen gitt orgnr til hovedenhet") {
            coEvery { brregClient.getBrregEnhet(hovedenhet.organisasjonsnummer) } returns hovedenhet.right()

            arrangorService.getArrangorOrSyncFromBrreg(hovedenhet.organisasjonsnummer).shouldBeRight()

            database.run {
                queries.arrangor.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                    it.id.shouldNotBeNull()
                    it.navn shouldBe "Testbedriften AS"
                    it.organisasjonsnummer shouldBe Organisasjonsnummer("123456789")
                    it.underenheter.shouldBeNull()
                }
                queries.arrangor.get(underenhet.organisasjonsnummer).shouldBeNull()
            }
        }

        test("skal synkronisere hovedenhet i tillegg til underenhet fra brreg til databasen gitt orgnr til underenhet") {
            coEvery { brregClient.getBrregEnhet(hovedenhet.organisasjonsnummer) } returns hovedenhet.right()
            coEvery { brregClient.getBrregEnhet(underenhet.organisasjonsnummer) } returns underenhet.right()

            arrangorService.getArrangorOrSyncFromBrreg(underenhet.organisasjonsnummer).shouldBeRight()

            database.run {
                queries.arrangor.get(hovedenhet.organisasjonsnummer).shouldNotBeNull().should {
                    it.navn shouldBe "Testbedriften AS"
                    it.organisasjonsnummer shouldBe Organisasjonsnummer("123456789")
                }
                queries.arrangor.get(underenhet.organisasjonsnummer).shouldNotBeNull().should {
                    it.navn shouldBe "Underenhet til Testbedriften AS"
                    it.organisasjonsnummer shouldBe Organisasjonsnummer("234567891")
                }
            }
        }

        test("skal synkronisere slettet enhet fra brreg og til databasen gitt orgnr til enheten") {
            val orgnr = Organisasjonsnummer("100200300")
            val slettetVirksomhet = SlettetBrregHovedenhetDto(
                organisasjonsnummer = orgnr,
                organisasjonsform = "AS",
                navn = "Slettet bedrift",
                slettetDato = LocalDate.of(2020, 1, 1),
            )

            coEvery { brregClient.getBrregEnhet(orgnr) } returns slettetVirksomhet.right()

            arrangorService.getArrangorOrSyncFromBrreg(orgnr).shouldBeRight()

            database.run {
                queries.arrangor.get(orgnr).shouldNotBeNull().should {
                    it.navn shouldBe "Slettet bedrift"
                    it.organisasjonsnummer shouldBe orgnr
                    it.slettetDato shouldBe LocalDate.of(2020, 1, 1)
                }
            }
        }

        test("NotFound error når enhet ikke finnes") {
            val orgnr = Organisasjonsnummer("123123123")

            coEvery { brregClient.getBrregEnhet(orgnr) } returns BrregError.NotFound.left()
            coEvery { brregClient.getBrregEnhet(orgnr) } returns BrregError.NotFound.left()

            arrangorService.getArrangorOrSyncFromBrreg(orgnr) shouldBeLeft BrregError.NotFound

            database.run {
                queries.arrangor.get(orgnr) shouldBe null
            }
        }
    }

    context("brreg sok med utenlandske bedrifter") {
        val brregClient: BrregClient = mockk()
        val arrangorService = ArrangorService(database.db, brregClient)
        val utenlandskArrangor = ArrangorDto(
            id = UUID.randomUUID(),
            organisasjonsnummer = Organisasjonsnummer("100000056"),
            organisasjonsform = null,
            navn = "X - Utbildning Nord",
            overordnetEnhet = null,
            underenheter = emptyList(),
            slettetDato = null,
        )

        beforeEach {
            database.run {
                queries.arrangor.upsert(utenlandskArrangor)
                it.execute(
                    Query(
                        "update arrangor set er_utenlandsk_virksomhet = true where id = '${utenlandskArrangor.id}'",
                    ),
                )
            }
        }

        afterEach {
            clearAllMocks()
            database.truncateAll()
        }

        test("sok gir med utenlandske arrangører") {
            coEvery { brregClient.sokHovedenhet(any()) } returns emptyList<BrregHovedenhetDto>().right()

            arrangorService.brregSok("Nord").shouldBeRight()[0] should {
                it.navn shouldBe utenlandskArrangor.navn
                it.organisasjonsnummer shouldBe utenlandskArrangor.organisasjonsnummer
            }
        }

        test("hent underenheter for utenlandsk arrangør gir liste med hovedenheten") {
            coEvery { brregClient.getUnderenheterForHovedenhet(utenlandskArrangor.organisasjonsnummer) } returns emptyList<BrregUnderenhetDto>().right()

            arrangorService.brregUnderenheter(utenlandskArrangor.organisasjonsnummer).shouldBeRight()[0] should {
                it.navn shouldBe utenlandskArrangor.navn
                it.organisasjonsnummer shouldBe utenlandskArrangor.organisasjonsnummer
            }
        }
    }
})
