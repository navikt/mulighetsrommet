package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotliquery.Query
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.NorskIdent
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
            norskIdent = NorskIdent("12345678910"),
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

        test("Les fra tabell") {
            service.lagreDelMedBruker(payload)
            service.lagreDelMedBruker(payload.copy(navident = "nav234", dialogId = "987"))

            val delMedBruker = service.getDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = payload.sanityId!!,
            )

            delMedBruker.shouldBeRight().should {
                it.shouldNotBeNull()

                it.id shouldBe "2"
                it.norskIdent shouldBe NorskIdent("12345678910")
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
                norskIdent = NorskIdent("12345678910"),
                navident = "nav123",
                sanityId = null,
                tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
                dialogId = "1234",
            )

            service.lagreDelMedBruker(request).shouldBeRight()

            val delMedBruker = service.getDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            )

            delMedBruker.shouldBeRight().should {
                it.shouldNotBeNull()
                it.tiltaksgjennomforingId shouldBe TiltaksgjennomforingFixtures.Oppfolging1.id
            }
        }

        test("Hent Del med bruker-historikk fra database og Sanity") {
            MulighetsrommetTestDomain().initialize(database.db)
            val sanityGjennomforingIdForEnkeltplass = UUID.randomUUID()
            val sanityGjennomforingIdForArbeidstrening = UUID.randomUUID()
            val tiltakstypeIdForEnkeltAmo = UUID.randomUUID()
            val tiltakstypeIdForArbeidstrening = UUID.randomUUID()

            Query("update tiltakstype set sanity_id = '$tiltakstypeIdForEnkeltAmo' where id = '${TiltakstypeFixtures.EnkelAmo.id}'")
                .asUpdate.let { database.db.run(it) }

            Query("update tiltakstype set sanity_id = '$tiltakstypeIdForArbeidstrening' where id = '${TiltakstypeFixtures.Arbeidstrening.id}'")
                .asUpdate.let { database.db.run(it) }

            coEvery {
                sanityClient.query(any(), any())
            } returns SanityResponse.Result(
                500,
                "",
                Json.parseToJsonElement(
                    """
                        [
                            {
                                "_id": "$sanityGjennomforingIdForEnkeltplass",
                                "tiltaksgjennomforingNavn": "Delt med bruker - Lokalt navn fra Sanity",
                                "tiltakstype":  {
                                    "_id": "$tiltakstypeIdForEnkeltAmo",
                                    "tiltakstypeNavn": "Arbeidsmarkedsopplæring (AMO) enkeltplass"
                                }
                            },
                            {
                                "_id": "$sanityGjennomforingIdForArbeidstrening",
                                "tiltaksgjennomforingNavn": "Delt med bruker - Sanity",
                                "tiltakstype":  {
                                    "_id": "$tiltakstypeIdForArbeidstrening",
                                    "tiltakstypeNavn": "Arbeidstrening"
                                }
                            }
                        ]
                    """.trimIndent(),
                ),
            )

            val tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db)
            tiltaksgjennomforingRepository.upsert(TiltaksgjennomforingFixtures.Oppfolging1.copy(navn = "Delt med bruker - tabell"))
            val request1 = DelMedBrukerDbo(
                id = "123",
                norskIdent = NorskIdent("12345678910"),
                navident = "nav123",
                sanityId = null,
                tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
                dialogId = "1234",
            )

            val request2 = DelMedBrukerDbo(
                id = "1234",
                norskIdent = NorskIdent("12345678910"),
                navident = "nav123",
                sanityId = sanityGjennomforingIdForEnkeltplass,
                tiltaksgjennomforingId = null,
                dialogId = "1235",
            )

            val request3 = DelMedBrukerDbo(
                id = "12345",
                norskIdent = NorskIdent("12345678910"),
                navident = "nav123",
                sanityId = sanityGjennomforingIdForArbeidstrening,
                tiltaksgjennomforingId = null,
                dialogId = "1235",
            )

            service.lagreDelMedBruker(request1).shouldBeRight()
            service.lagreDelMedBruker(request2).shouldBeRight()
            service.lagreDelMedBruker(request3).shouldBeRight()

            val delMedBruker = service.getDelMedBrukerHistorikk(NorskIdent("12345678910"))

            delMedBruker.shouldBeRight().should {
                it.shouldNotBeNull()
                it.size shouldBe 3
                it[0].konstruertNavn shouldBe "Oppfølging"
                it[1].konstruertNavn shouldBe "Delt med bruker - Lokalt navn fra Sanity"
                it[2].konstruertNavn shouldBe "Arbeidstrening"
            }
        }
    }
})
