package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.server.plugins.*
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import java.util.*

class DelMedBrukerServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val sanityClient: SanityClient = mockk(relaxed = true)

    afterEach {
        database.db.truncateAll()
    }

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(database.db, sanityClient)

        val payload = DelMedBrukerDbo(
            id = "123",
            norskIdent = "12345678910",
            navident = "nav123",
            sanityId = UUID.randomUUID(),
            dialogId = "1234",
        )

        test("Insert del med bruker-data") {
            service.lagreDelMedBruker(payload)

            database.assertThat("del_med_bruker").row(0)
                .value("id").isEqualTo(1)
                .value("norsk_ident").isEqualTo("12345678910")
                .value("navident").isEqualTo("nav123")
                .value("sanity_id").isEqualTo(payload.sanityId.toString())
        }

        test("Lagre til tabell feiler dersom input for brukers fnr er ulikt 11 tegn") {
            val payloadMedFeilData = payload.copy(
                norskIdent = "12345678910123",
            )
            val exception = shouldThrow<BadRequestException> {
                service.lagreDelMedBruker(payloadMedFeilData)
            }

            exception.message shouldContain "Brukers fnr er ikke 11 tegn"
        }

        test("Les fra tabell") {
            service.lagreDelMedBruker(payload)
            service.lagreDelMedBruker(payload.copy(navident = "nav234", dialogId = "987"))

            val delMedBruker = service.getDeltMedBruker(
                fnr = "12345678910",
                id = payload.sanityId!!,
            )

            delMedBruker.shouldBeRight().should {
                it.shouldNotBeNull()

                it.id shouldBe "2"
                it.norskIdent shouldBe "12345678910"
                it.navident shouldBe "nav234"
                it.sanityId shouldBe payload.sanityId
                it.dialogId shouldBe "987"
            }
        }

        test("insert med tiltaksgjennomforingId") {
            MulighetsrommetTestDomain().initialize(database.db)

            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforingRepository.upsert(TiltaksgjennomforingFixtures.Oppfolging1)
            val request = DelMedBrukerDbo(
                id = "123",
                norskIdent = "12345678910",
                navident = "nav123",
                sanityId = null,
                tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
                dialogId = "1234",
            )

            service.lagreDelMedBruker(request).shouldBeRight()

            val delMedBruker = service.getDeltMedBruker(
                fnr = "12345678910",
                id = TiltaksgjennomforingFixtures.Oppfolging1.id,
            )

            delMedBruker.shouldBeRight().should {
                it.shouldNotBeNull()
                it.tiltaksgjennomforingId shouldBe TiltaksgjennomforingFixtures.Oppfolging1.id
            }
        }

        test("Hent Del med bruker-historikk fra database og Sanity") {
            MulighetsrommetTestDomain().initialize(database.db)
            val sanityId = UUID.randomUUID()

            coEvery {
                sanityClient.query(any(), any())
            } returns SanityResponse.Result(
                500,
                "",
                Json.parseToJsonElement(
                    """
                        [
                            {
                                "_id": "$sanityId",
                                "tiltaksgjennomforingNavn": "Delt med bruker - Sanity"
                            }
                        ]
                    """.trimIndent(),
                ),
            )

            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforingRepository.upsert(TiltaksgjennomforingFixtures.Oppfolging1.copy(navn = "Delt med bruker - tabell"))
            val request1 = DelMedBrukerDbo(
                id = "123",
                norskIdent = "12345678910",
                navident = "nav123",
                sanityId = null,
                tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
                dialogId = "1234",
            )

            val request2 = DelMedBrukerDbo(
                id = "1234",
                norskIdent = "12345678910",
                navident = "nav123",
                sanityId = sanityId,
                tiltaksgjennomforingId = null,
                dialogId = "1235",
            )

            service.lagreDelMedBruker(request1).shouldBeRight()
            service.lagreDelMedBruker(request2).shouldBeRight()

            val delMedBruker = service.getDelMedBrukerHistorikk("12345678910")

            delMedBruker.shouldBeRight().should {
                it.shouldNotBeNull()
                it.size shouldBe 2
                it[0].navn shouldBe "Delt med bruker - tabell"
                it[1].navn shouldBe "Delt med bruker - Sanity"
            }
        }
    }
})
