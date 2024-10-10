package no.nav.mulighetsrommet.api.services

import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dbo.DelMedBrukerDbo
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltakstype
import no.nav.mulighetsrommet.api.domain.dto.TiltakstypeDto
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.database.kotest.extensions.truncateAll
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import java.time.LocalDate
import java.util.*

class DelMedBrukerServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))
    val sanityService: SanityService = mockk(relaxed = true)
    val tiltakstypeService: TiltakstypeService = mockk(relaxed = true)

    afterEach {
        database.db.truncateAll()
    }

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(database.db, sanityService, tiltakstypeService)

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

            delMedBruker.should {
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

            delMedBruker.should {
                it.shouldNotBeNull()
                it.tiltaksgjennomforingId shouldBe TiltaksgjennomforingFixtures.Oppfolging1.id
            }
        }

        test("Hent Del med bruker-historikk fra database og Sanity") {
            MulighetsrommetTestDomain().initialize(database.db)
            val sanityGjennomforingIdForEnkeltplass = UUID.randomUUID()
            val sanityGjennomforingIdForArbeidstrening = UUID.randomUUID()
            val enkeltAmoSanityId = UUID.randomUUID()
            val arbeidstreningSanityId = UUID.randomUUID()

            every { tiltakstypeService.getBySanityId(enkeltAmoSanityId) } returns TiltakstypeDto(
                id = TiltakstypeFixtures.EnkelAmo.id,
                navn = "EnkelAMo",
                innsatsgrupper = emptySet(),
                arenaKode = TiltakstypeFixtures.EnkelAmo.arenaKode,
                tiltakskode = null,
                startDato = LocalDate.of(2022, 1, 1),
                sluttDato = null,
                status = TiltakstypeStatus.AKTIV,
                sanityId = enkeltAmoSanityId,
            )
            every { tiltakstypeService.getBySanityId(arbeidstreningSanityId) } returns TiltakstypeDto(
                id = TiltakstypeFixtures.Arbeidstrening.id,
                navn = TiltakstypeFixtures.Arbeidstrening.navn,
                innsatsgrupper = emptySet(),
                arenaKode = TiltakstypeFixtures.Arbeidstrening.arenaKode,
                tiltakskode = null,
                startDato = TiltakstypeFixtures.Arbeidstrening.startDato,
                sluttDato = null,
                status = TiltakstypeStatus.AKTIV,
                sanityId = arbeidstreningSanityId,
            )

            coEvery {
                sanityService.getAllTiltak(any(), any())
            } returns listOf(
                SanityTiltaksgjennomforing(
                    _id = sanityGjennomforingIdForEnkeltplass.toString(),
                    tiltaksgjennomforingNavn = "Delt med bruker - Lokalt navn fra Sanity",
                    tiltakstype = SanityTiltakstype(
                        _id = "$enkeltAmoSanityId",
                        tiltakstypeNavn = "Arbeidsmarkedsopplæring (AMO) enkeltplass",
                    ),
                ),
                SanityTiltaksgjennomforing(
                    _id = sanityGjennomforingIdForArbeidstrening.toString(),
                    tiltaksgjennomforingNavn = "Delt med bruker - Sanity",
                    tiltakstype = SanityTiltakstype(
                        _id = "$arbeidstreningSanityId",
                        tiltakstypeNavn = "Arbeidstrening",
                    ),
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

            delMedBruker.should {
                it.shouldNotBeNull()
                it.size shouldBe 3
                it[0].tittel shouldBe "Oppfølging"
                it[1].tittel shouldBe "Delt med bruker - Lokalt navn fra Sanity"
                it[2].tittel shouldBe "Arbeidstrening"
            }
        }
    }
})
