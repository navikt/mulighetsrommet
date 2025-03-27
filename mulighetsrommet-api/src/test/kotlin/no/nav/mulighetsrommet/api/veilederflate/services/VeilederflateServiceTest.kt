package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import kotliquery.Query
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.sanity.CacheUsage
import no.nav.mulighetsrommet.api.sanity.SanityArrangor
import no.nav.mulighetsrommet.api.sanity.SanityArrangorKontaktperson
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplassAnskaffet
import no.nav.mulighetsrommet.api.veilederflate.routes.ApentForPamelding
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innsatsgruppe
import java.util.*

class VeilederflateServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val enkelAmoSanityId = UUID.randomUUID()
    val arbeidstreningSanityId = UUID.randomUUID()
    val arbeidsrettetRehabiliteringSanityId = UUID.randomUUID()

    val domain = MulighetsrommetTestDomain(
        navEnheter = listOf(
            NavEnhetFixtures.Innlandet,
            NavEnhetFixtures.Lillehammer,
            NavEnhetFixtures.Oslo,
        ),
        ansatte = emptyList(),
        arrangorer = emptyList(),
        tiltakstyper = listOf(
            TiltakstypeFixtures.EnkelAmo,
            TiltakstypeFixtures.Arbeidstrening,
            TiltakstypeFixtures.ArbeidsrettetRehabilitering,
        ),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
    ) {
        session.execute(Query("update tiltakstype set sanity_id = '$enkelAmoSanityId' where id = '${TiltakstypeFixtures.EnkelAmo.id}'"))
        session.execute(Query("update tiltakstype set sanity_id = '$arbeidstreningSanityId' where id = '${TiltakstypeFixtures.Arbeidstrening.id}'"))
        session.execute(Query("update tiltakstype set sanity_id = '$arbeidsrettetRehabiliteringSanityId' where id = '${TiltakstypeFixtures.ArbeidsrettetRehabilitering.id}'"))
    }

    beforeEach {
        domain.initialize(database.db)
    }

    val sanityService: SanityService = mockk(relaxed = true)

    fun createService() = VeilederflateService(
        db = database.db,
        sanityService = sanityService,
        tiltakstypeService = TiltakstypeService(database.db),
        navEnhetService = NavEnhetService(database.db),
    )

    beforeEach {
        clearMocks(sanityService)
    }

    val sanityTiltak = listOf(
        SanityTiltaksgjennomforing(
            _id = "6c64a4bd-2ae1-4aee-ad19-716884bf3b5e",
            tiltaksgjennomforingNavn = "Enkel AMO",
            stedForGjennomforing = null,
            tiltaksnummer = "2023#176408",
            tiltakstype = SanityTiltakstype(
                _id = "$enkelAmoSanityId",
                tiltakstypeNavn = "Arbeidsmarkedsopplæring (enkeltplass)",
                innsatsgrupper = setOf(
                    Innsatsgruppe.TRENGER_VEILEDNING,
                    Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
                    Innsatsgruppe.JOBBE_DELVIS,
                    Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ),
            ),
            fylker = listOf("0300"),
            enheter = emptyList(),
            arrangor = SanityArrangor(
                _id = UUID.randomUUID(),
                navn = "Fretex",
                organisasjonsnummer = null,
                kontaktpersoner = listOf(
                    SanityArrangorKontaktperson(
                        _id = UUID.randomUUID(),
                        navn = "Donald",
                        telefon = "12341234",
                        epost = "donald@fretex.no",
                        beskrivelse = "Daglig leder",
                    ),
                ),
            ),
        ),
        SanityTiltaksgjennomforing(
            _id = "f21d1e35-d63b-4de7-a0a5-589e57111527",
            tiltaksgjennomforingNavn = "Arbeidstrening Innlandet",
            tiltaksnummer = null,
            stedForGjennomforing = "Innlandet",
            tiltakstype = SanityTiltakstype(
                _id = "$arbeidstreningSanityId",
                tiltakstypeNavn = "Arbeidstrening",
                innsatsgrupper = setOf(
                    Innsatsgruppe.TRENGER_VEILEDNING,
                    Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
                    Innsatsgruppe.JOBBE_DELVIS,
                    Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ),
            ),
            fylker = listOf("0400"),
            enheter = null,
        ),
        SanityTiltaksgjennomforing(
            _id = "82cebdb9-24ef-4f6d-b6b2-6ed45c67d3b6",
            tiltaksgjennomforingNavn = "Arbeidstrening",
            stedForGjennomforing = null,
            tiltaksnummer = null,
            fylker = listOf("0400"),
            tiltakstype = SanityTiltakstype(
                _id = "$arbeidstreningSanityId",
                tiltakstypeNavn = "Arbeidstrening",
                innsatsgrupper = setOf(
                    Innsatsgruppe.TRENGER_VEILEDNING,
                    Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
                    Innsatsgruppe.JOBBE_DELVIS,
                    Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
                ),
            ),
            enheter = listOf("0501"),
            faneinnhold = Faneinnhold(forHvemInfoboks = "infoboks"),
        ),
    )

    test("utleder gjennomføringer som enkeltplass anskaffet tiltak når de har arrangør") {
        val veilederFlateService = createService()

        coEvery { sanityService.getAllTiltak(any(), any()) } returns sanityTiltak

        val tiltak = veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0300"),
            apentForPamelding = ApentForPamelding.APENT,
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        )

        tiltak shouldHaveSize 1

        tiltak.first().shouldBeInstanceOf<VeilederflateTiltakEnkeltplassAnskaffet>().should {
            it.arrangor.selskapsnavn shouldBe "Fretex"
        }
    }

    test("henter ikke gjennomføringer fra Sanity når filter for 'Åpent for påmelding' er STENGT") {
        val veilederFlateService = VeilederflateService(
            db = database.db,
            sanityService = sanityService,
            tiltakstypeService = TiltakstypeService(database.db),
            navEnhetService = NavEnhetService(database.db),
        )

        coEvery { sanityService.getAllTiltak(any(), any()) } returns sanityTiltak

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForPamelding = ApentForPamelding.APENT,
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForPamelding = ApentForPamelding.APENT_ELLER_STENGT,
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForPamelding = ApentForPamelding.STENGT,
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ) shouldHaveSize 0
    }
})
