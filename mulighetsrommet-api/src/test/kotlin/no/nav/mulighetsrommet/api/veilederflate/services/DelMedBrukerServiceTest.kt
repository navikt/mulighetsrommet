package no.nav.mulighetsrommet.api.veilederflate.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltakstype
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltaksgjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.api.veilederflate.models.DelMedBrukerDbo
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import no.nav.mulighetsrommet.domain.dto.NorskIdent
import no.nav.mulighetsrommet.domain.dto.TiltakstypeStatus
import java.time.LocalDate
import java.util.*

class DelMedBrukerServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))
    val sanityService: SanityService = mockk(relaxed = true)
    val tiltakstypeService: TiltakstypeService = mockk(relaxed = true)

    afterEach {
        database.truncateAll()
    }

    context("DelMedBrukerService") {
        val service = DelMedBrukerService(database.db, sanityService, tiltakstypeService)

        val sanityId = UUID.randomUUID()

        val payload = DelMedBrukerDbo(
            id = "123",
            norskIdent = NorskIdent("12345678910"),
            navident = "nav123",
            sanityId = sanityId,
            dialogId = "1234",
        )

        test("lagrer og henter siste deling for tiltak") {
            service.lagreDelMedBruker(payload)

            service.getDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = sanityId,
            ).shouldNotBeNull().should {
                it.id shouldBe "1"
                it.norskIdent shouldBe NorskIdent("12345678910")
                it.navident shouldBe "nav123"
                it.sanityId shouldBe sanityId
                it.dialogId shouldBe "1234"
            }

            service.lagreDelMedBruker(payload.copy(navident = "nav234", dialogId = "987"))

            service.getDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = sanityId,
            ).shouldNotBeNull().should {
                it.id shouldBe "2"
                it.norskIdent shouldBe NorskIdent("12345678910")
                it.navident shouldBe "nav234"
                it.sanityId shouldBe sanityId
                it.dialogId shouldBe "987"
            }
        }

        test("insert med tiltaksgjennomforingId") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1),
            ).initialize(database.db)

            val request = DelMedBrukerDbo(
                id = "123",
                norskIdent = NorskIdent("12345678910"),
                navident = "nav123",
                sanityId = null,
                tiltaksgjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
                dialogId = "1234",
            )

            service.lagreDelMedBruker(request)

            val delMedBruker = service.getDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = TiltaksgjennomforingFixtures.Oppfolging1.id,
            )

            delMedBruker.shouldNotBeNull().should {
                it.tiltaksgjennomforingId shouldBe TiltaksgjennomforingFixtures.Oppfolging1.id
                it.sanityId shouldBe null
            }
        }

        test("Hent Del med bruker-historikk fra database og Sanity") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(TiltaksgjennomforingFixtures.Oppfolging1.copy(navn = "Delt med bruker - tabell")),
            ).initialize(database.db)

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
                        innsatsgrupper = Innsatsgruppe.entries.toSet(),
                    ),
                ),
                SanityTiltaksgjennomforing(
                    _id = sanityGjennomforingIdForArbeidstrening.toString(),
                    tiltaksgjennomforingNavn = "Delt med bruker - Sanity",
                    tiltakstype = SanityTiltakstype(
                        _id = "$arbeidstreningSanityId",
                        tiltakstypeNavn = "Arbeidstrening",
                        innsatsgrupper = Innsatsgruppe.entries.toSet(),
                    ),
                ),
            )

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

            service.lagreDelMedBruker(request1)
            service.lagreDelMedBruker(request2)
            service.lagreDelMedBruker(request3)

            val delMedBruker = service.getDelMedBrukerHistorikk(NorskIdent("12345678910"))

            delMedBruker.shouldNotBeNull().should {
                it.size shouldBe 3
                it[0].tiltakstype.navn shouldBe "Oppfølging"
                it[1].tiltakstype.navn shouldBe "Arbeidsmarkedsopplæring (AMO) enkeltplass"
                it[2].tiltakstype.navn shouldBe "Arbeidstrening"
            }
        }
    }
})
