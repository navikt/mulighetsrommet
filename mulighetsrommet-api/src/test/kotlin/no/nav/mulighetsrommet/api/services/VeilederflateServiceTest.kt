package no.nav.mulighetsrommet.api.services

import arrow.core.nonEmptyListOf
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotliquery.Query
import no.nav.mulighetsrommet.api.clients.sanity.SanityClient
import no.nav.mulighetsrommet.api.createDatabaseTestConfig
import no.nav.mulighetsrommet.api.domain.dto.SanityResponse
import no.nav.mulighetsrommet.api.domain.dto.VeilederflateTiltakEnkeltplassAnskaffet
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.repositories.NavEnhetRepository
import no.nav.mulighetsrommet.api.repositories.TiltaksgjennomforingRepository
import no.nav.mulighetsrommet.api.repositories.TiltakstypeRepository
import no.nav.mulighetsrommet.api.routes.v1.ApentForInnsok
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import java.util.*

class VeilederflateServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(createDatabaseTestConfig()))

    val domain = MulighetsrommetTestDomain(
        enheter = listOf(
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
    )

    val enkelAmoSanityId = UUID.randomUUID()
    val arbeidstreningSanityId = UUID.randomUUID()

    beforeEach {
        domain.initialize(database.db)

        listOf(
            Query("update tiltakstype set sanity_id = '$enkelAmoSanityId' where id = '${TiltakstypeFixtures.EnkelAmo.id}'"),
            Query("update tiltakstype set sanity_id = '$arbeidstreningSanityId' where id = '${TiltakstypeFixtures.Arbeidstrening.id}'"),
        ).forEach {
            database.db.run(it.asExecute)
        }
    }

    val sanityClient: SanityClient = mockk(relaxed = true)

    fun createVeilederflateService(): VeilederflateService = VeilederflateService(
        sanityClient = sanityClient,
        tiltaksgjennomforingRepository = TiltaksgjennomforingRepository(database.db),
        tiltakstypeService = TiltakstypeService(TiltakstypeRepository(database.db), listOf()),
        navEnhetService = NavEnhetService(NavEnhetRepository(database.db)),
    )

    beforeEach {
        clearMocks(sanityClient)
    }

    val sanityResult = SanityResponse.Result(
        ms = 12,
        query = "",
        result = Json.parseToJsonElement(
            """
        [
            {
                "_id": "6c64a4bd-2ae1-4aee-ad19-716884bf3b5e",
                "tiltaksgjennomforingNavn": "Enkel AMO",
                "oppstart": null,
                "oppstartsdato": null,
                "stedForGjennomforing": null,
                "tiltaksnummer": "2023#176408",
                "tiltakstype": {
                    "_id": "$enkelAmoSanityId",
                    "tiltakstypeNavn": "Arbeidsmarkedsopplæring (enkeltplass)",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "fylke": "0300",
                "enheter": [],
                "arrangor": {
                    "_id": "${UUID.randomUUID()}",
                    "navn": "Fretex",
                    "organisasjonsnummer": null,
                    "kontaktpersoner": [
                        {
                            "_id": "${UUID.randomUUID()}",
                            "navn": "Donald",
                            "telefon": "12341234",
                            "epost": "donald@fretex.no",
                            "beskrivelse": "Daglig leder"
                        }
                    ]
                }
            },
            {
                "_id": "f21d1e35-d63b-4de7-a0a5-589e57111527",
                "tiltaksgjennomforingNavn": "Arbeidstrening Innlandet",
                "tiltaksnummer": null,
                "stedForGjennomforing": "Innlandet",
                "tiltakstype": {
                    "_id": "$arbeidstreningSanityId",
                    "tiltakstypeNavn": "Arbeidstrening",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "fylke": "0400",
                "oppstart": null,
                "oppstartsdato": null,
                "enheter": null
            },
            {
                "_id": "82cebdb9-24ef-4f6d-b6b2-6ed45c67d3b6",
                "tiltaksgjennomforingNavn": "Arbeidstrening",
                "oppstart": "dato",
                "stedForGjennomforing": null,
                "oppstartsdato": "2020-11-02",
                "tiltaksnummer": null,
                "fylke": "0400",
                "tiltakstype": {
                    "_id": "$arbeidstreningSanityId",
                    "tiltakstypeNavn": "Arbeidstrening",
                    "innsatsgrupper": ["SITUASJONSBESTEMT_INNSATS", "SPESIELT_TILPASSET_INNSATS", "GRADERT_VARIG_TILPASSET_INNSATS", "VARIG_TILPASSET_INNSATS"]
                },
                "enheter": ["0501"],
                "faneinnhold": { "forHvemInfoboks": "infoboks" }
            }
        ]
    """,
        ),
    )

    test("utleder gjennomføringer som enkeltplass anskaffet tiltak når de har arrangør") {
        val veilederFlateService = createVeilederflateService()

        coEvery { sanityClient.query(any(), any()) } returns sanityResult

        val tiltak = veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0300"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
        )

        tiltak shouldHaveSize 1

        tiltak.first().shouldBeInstanceOf<VeilederflateTiltakEnkeltplassAnskaffet>().should {
            it.arrangor.selskapsnavn shouldBe "Fretex"
        }
    }

    test("henter ikke gjennomføringer fra Sanity når filter for 'Åpent for innsøk' er STENGT") {
        val veilederFlateService = createVeilederflateService()

        coEvery { sanityClient.query(any(), any()) } returns sanityResult

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.APENT_ELLER_STENGT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.STENGT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
        ) shouldHaveSize 0
    }

    test("Med UseCache kalles sanity kun én gang") {
        val veilederFlateService = createVeilederflateService()

        coEvery { sanityClient.query(any(), any()) } returns sanityResult

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.UseCache,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.UseCache,
        ) shouldHaveSize 2

        coVerify(exactly = 1) {
            sanityClient.query(any(), any())
        }
    }
})
