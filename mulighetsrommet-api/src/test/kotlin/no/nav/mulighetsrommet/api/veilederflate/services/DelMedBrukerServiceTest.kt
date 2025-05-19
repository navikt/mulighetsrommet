package no.nav.mulighetsrommet.api.veilederflate.services

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeDto
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.*
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

        test("lagrer og henter siste deling for tiltak") {
            val sanityId = UUID.randomUUID()

            val payload = DelMedBrukerInsertDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = sanityId,
                gjennomforingId = null,
                dialogId = "1234",
                tiltakstypeId = TiltakstypeFixtures.Avklaring.id,
                deltFraFylke = NavEnhetNummer("0300"),
                deltFraEnhet = NavEnhetNummer("0301"),
            )

            service.lagreDelMedBruker(payload)

            service.getTiltakDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = sanityId,
            ).shouldNotBeNull().should {
                it.id shouldBe 1
                it.sanityId shouldBe sanityId
                it.dialogId shouldBe "1234"
            }

            service.lagreDelMedBruker(payload.copy(navIdent = NavIdent("B123456"), dialogId = "987"))

            service.getTiltakDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = sanityId,
            ).shouldNotBeNull().should {
                it.id shouldBe 2
                it.sanityId shouldBe sanityId
                it.dialogId shouldBe "987"
            }
        }

        test("insert med gjennomforingId") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1),
            ).initialize(database.db)

            val request = DelMedBrukerInsertDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1234",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraFylke = NavEnhetNummer("0300"),
                deltFraEnhet = NavEnhetNummer("0301"),
            )

            service.lagreDelMedBruker(request)

            val delMedBruker = service.getTiltakDeltMedBruker(
                fnr = NorskIdent("12345678910"),
                sanityOrGjennomforingId = GjennomforingFixtures.Oppfolging1.id,
            )

            delMedBruker.shouldNotBeNull().should {
                it.gjennomforingId shouldBe GjennomforingFixtures.Oppfolging1.id
                it.sanityId shouldBe null
            }
        }

        test("Hent Del med bruker-historikk fra database og Sanity") {
            MulighetsrommetTestDomain(
                gjennomforinger = listOf(GjennomforingFixtures.Oppfolging1.copy(navn = "Delt med bruker - tabell")),
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

            val request1 = DelMedBrukerInsertDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = null,
                gjennomforingId = GjennomforingFixtures.Oppfolging1.id,
                dialogId = "1234",
                tiltakstypeId = TiltakstypeFixtures.Oppfolging.id,
                deltFraFylke = NavEnhetNummer("0300"),
                deltFraEnhet = NavEnhetNummer("0301"),
            )

            val request2 = DelMedBrukerInsertDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = sanityGjennomforingIdForEnkeltplass,
                gjennomforingId = null,
                dialogId = "1235",
                tiltakstypeId = TiltakstypeFixtures.EnkelAmo.id,
                deltFraFylke = NavEnhetNummer("0300"),
                deltFraEnhet = NavEnhetNummer("0301"),
            )

            val request3 = DelMedBrukerInsertDbo(
                norskIdent = NorskIdent("12345678910"),
                navIdent = NavIdent("B123456"),
                sanityId = sanityGjennomforingIdForArbeidstrening,
                gjennomforingId = null,
                dialogId = "1235",
                tiltakstypeId = TiltakstypeFixtures.Arbeidstrening.id,
                deltFraFylke = NavEnhetNummer("0300"),
                deltFraEnhet = NavEnhetNummer("0301"),
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
