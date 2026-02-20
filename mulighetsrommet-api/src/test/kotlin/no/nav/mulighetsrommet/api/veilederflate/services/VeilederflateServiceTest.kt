package no.nav.mulighetsrommet.api.veilederflate.services

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeTypeOf
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
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplass
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplassAnskaffet
import no.nav.mulighetsrommet.api.veilederflate.routes.ApentForPamelding
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Innsatsgruppe
import no.nav.mulighetsrommet.model.NavEnhetNummer
import java.util.UUID

class VeilederflateServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val tiltakstypeEnkelAmo = SanityTiltakstype(
        _id = UUID.randomUUID().toString(),
    )
    val tiltakstypeArbeidstrening = SanityTiltakstype(
        _id = UUID.randomUUID().toString(),
    )
    val tiltakEnkelAmo = SanityTiltaksgjennomforing(
        _id = "6c64a4bd-2ae1-4aee-ad19-716884bf3b5e",
        tiltaksgjennomforingNavn = "Enkel AMO",
        tiltaksnummer = "2023#176408",
        tiltakstype = tiltakstypeEnkelAmo,
        fylke = NavEnhetNummer("0300"),
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
    )
    val tiltakArbeidstrening1 = SanityTiltaksgjennomforing(
        _id = "f21d1e35-d63b-4de7-a0a5-589e57111527",
        tiltaksgjennomforingNavn = "Arbeidstrening Innlandet",
        tiltaksnummer = null,
        tiltakstype = tiltakstypeArbeidstrening,
        fylke = NavEnhetNummer("0400"),
        enheter = null,
    )
    val tiltakArbeidstrening2 = SanityTiltaksgjennomforing(
        _id = "82cebdb9-24ef-4f6d-b6b2-6ed45c67d3b6",
        tiltaksgjennomforingNavn = "Arbeidstrening",
        tiltaksnummer = null,
        fylke = NavEnhetNummer("0400"),
        tiltakstype = tiltakstypeArbeidstrening,
        enheter = listOf(NavEnhetNummer("0501")),
        faneinnhold = Faneinnhold(forHvemInfoboks = "infoboks"),
    )

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
        ),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
    ) {
        val innsatsgrupper =
            "'{TRENGER_VEILEDNING, TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE, JOBBE_DELVIS, LITEN_MULIGHET_TIL_A_JOBBE}'::innsatsgruppe[]"
        session.execute(Query("update tiltakstype set sanity_id = '${tiltakstypeEnkelAmo._id}' where id = '${TiltakstypeFixtures.EnkelAmo.id}'"))
        session.execute(Query("update tiltakstype set innsatsgrupper = $innsatsgrupper where id = '${TiltakstypeFixtures.EnkelAmo.id}'"))

        session.execute(Query("update tiltakstype set sanity_id = '${tiltakstypeArbeidstrening._id}' where id = '${TiltakstypeFixtures.Arbeidstrening.id}'"))
        session.execute(Query("update tiltakstype set innsatsgrupper = $innsatsgrupper where id = '${TiltakstypeFixtures.Arbeidstrening.id}'"))
    }

    beforeSpec {
        domain.initialize(database.db)
    }

    val sanityService: SanityService = mockk(relaxed = true)
    coEvery { sanityService.getTiltakstyper() } returns listOf(
        tiltakstypeEnkelAmo,
        tiltakstypeArbeidstrening,
    )
    coEvery { sanityService.getAllTiltak(any(), any()) } returns listOf(
        tiltakEnkelAmo,
        tiltakArbeidstrening1,
        tiltakArbeidstrening2,
    )

    fun createService() = VeilederflateService(
        db = database.db,
        sanityService = sanityService,
        navEnhetService = NavEnhetService(database.db),
    )

    test("utleder gjennomføringer som enkeltplass anskaffet tiltak når de har arrangør") {
        val veilederFlateService = createService()

        val tiltak = veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0300")),
            apentForPamelding = ApentForPamelding.APENT,
            innsatsgruppe = Innsatsgruppe.TRENGER_VEILEDNING_NEDSATT_ARBEIDSEVNE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        )

        tiltak.shouldHaveSize(1).first().shouldBeInstanceOf<VeilederflateTiltakEnkeltplassAnskaffet>().should {
            it.sanityId shouldBe tiltakEnkelAmo._id
            it.arrangor.selskapsnavn shouldBe "Fretex"
        }
    }

    test("henter ikke gjennomføringer fra Sanity når filter for 'Åpent for påmelding' er STENGT") {
        val veilederFlateService = VeilederflateService(
            db = database.db,
            sanityService = sanityService,
            navEnhetService = NavEnhetService(database.db),
        )

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0501")),
            apentForPamelding = ApentForPamelding.APENT,
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ).shouldHaveSize(2).should { (first, second) ->
            first.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().sanityId shouldBe tiltakArbeidstrening1._id
            second.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().sanityId shouldBe tiltakArbeidstrening2._id
        }

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0501")),
            apentForPamelding = ApentForPamelding.APENT_ELLER_STENGT,
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ).shouldHaveSize(2).should { (first, second) ->
            first.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().sanityId shouldBe tiltakArbeidstrening1._id
            second.shouldBeTypeOf<VeilederflateTiltakEnkeltplass>().sanityId shouldBe tiltakArbeidstrening2._id
        }

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf(NavEnhetNummer("0501")),
            apentForPamelding = ApentForPamelding.STENGT,
            innsatsgruppe = Innsatsgruppe.LITEN_MULIGHET_TIL_A_JOBBE,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ).shouldBeEmpty()
    }
})
