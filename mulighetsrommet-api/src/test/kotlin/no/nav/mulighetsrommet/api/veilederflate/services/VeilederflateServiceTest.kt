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
import no.nav.mulighetsrommet.api.domain.dto.SanityArrangor
import no.nav.mulighetsrommet.api.domain.dto.SanityArrangorKontaktperson
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltaksgjennomforing
import no.nav.mulighetsrommet.api.domain.dto.SanityTiltakstype
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.NavEnhetFixtures
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.navenhet.NavEnhetService
import no.nav.mulighetsrommet.api.navenhet.db.NavEnhetRepository
import no.nav.mulighetsrommet.api.services.cms.CacheUsage
import no.nav.mulighetsrommet.api.services.cms.SanityService
import no.nav.mulighetsrommet.api.tiltakstype.TiltakstypeService
import no.nav.mulighetsrommet.api.tiltakstype.db.TiltakstypeRepository
import no.nav.mulighetsrommet.api.veilederflate.VeilederflateTiltakRepository
import no.nav.mulighetsrommet.api.veilederflate.models.VeilederflateTiltakEnkeltplassAnskaffet
import no.nav.mulighetsrommet.api.veilederflate.routes.ApentForInnsok
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.domain.dto.Faneinnhold
import no.nav.mulighetsrommet.domain.dto.Innsatsgruppe
import java.util.*

class VeilederflateServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

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
            TiltakstypeFixtures.ArbeidsrettetRehabilitering,
        ),
        avtaler = emptyList(),
        gjennomforinger = emptyList(),
    )

    val enkelAmoSanityId = UUID.randomUUID()
    val arbeidstreningSanityId = UUID.randomUUID()
    val arbeidsrettetRehabiliteringSanityId = UUID.randomUUID()

    beforeEach {
        domain.initialize(database.db)

        listOf(
            Query("update tiltakstype set sanity_id = '$enkelAmoSanityId' where id = '${TiltakstypeFixtures.EnkelAmo.id}'"),
            Query("update tiltakstype set sanity_id = '$arbeidstreningSanityId' where id = '${TiltakstypeFixtures.Arbeidstrening.id}'"),
            Query("update tiltakstype set sanity_id = '$arbeidsrettetRehabiliteringSanityId' where id = '${TiltakstypeFixtures.ArbeidsrettetRehabilitering.id}'"),
        ).forEach {
            database.db.run(it.asExecute)
        }
    }

    val sanityService: SanityService = mockk(relaxed = true)

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
                    Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                    Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                ),
            ),
            fylke = "0300",
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
                    Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                    Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                ),
            ),
            fylke = "0400",
            enheter = null,
        ),
        SanityTiltaksgjennomforing(
            _id = "82cebdb9-24ef-4f6d-b6b2-6ed45c67d3b6",
            tiltaksgjennomforingNavn = "Arbeidstrening",
            stedForGjennomforing = null,
            tiltaksnummer = null,
            fylke = "0400",
            tiltakstype = SanityTiltakstype(
                _id = "$arbeidstreningSanityId",
                tiltakstypeNavn = "Arbeidstrening",
                innsatsgrupper = setOf(
                    Innsatsgruppe.SITUASJONSBESTEMT_INNSATS,
                    Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
                    Innsatsgruppe.GRADERT_VARIG_TILPASSET_INNSATS,
                    Innsatsgruppe.VARIG_TILPASSET_INNSATS,
                ),
            ),
            enheter = listOf("0501"),
            faneinnhold = Faneinnhold(forHvemInfoboks = "infoboks"),
        ),
    )

    test("utleder gjennomføringer som enkeltplass anskaffet tiltak når de har arrangør") {
        val veilederFlateService = VeilederflateService(
            sanityService = sanityService,
            veilederflateTiltakRepository = VeilederflateTiltakRepository(database.db),
            tiltakstypeService = TiltakstypeService(TiltakstypeRepository(database.db)),
            navEnhetService = NavEnhetService(NavEnhetRepository(database.db)),
        )

        coEvery { sanityService.getAllTiltak(any(), any()) } returns sanityTiltak

        val tiltak = veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0300"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.SPESIELT_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        )

        tiltak shouldHaveSize 1

        tiltak.first().shouldBeInstanceOf<VeilederflateTiltakEnkeltplassAnskaffet>().should {
            it.arrangor.selskapsnavn shouldBe "Fretex"
        }
    }

    test("henter ikke gjennomføringer fra Sanity når filter for 'Åpent for innsøk' er STENGT") {
        val veilederFlateService = VeilederflateService(
            sanityService = sanityService,
            veilederflateTiltakRepository = VeilederflateTiltakRepository(database.db),
            tiltakstypeService = TiltakstypeService(TiltakstypeRepository(database.db)),
            navEnhetService = NavEnhetService(NavEnhetRepository(database.db)),
        )

        coEvery { sanityService.getAllTiltak(any(), any()) } returns sanityTiltak

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.APENT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.APENT_ELLER_STENGT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ) shouldHaveSize 2

        veilederFlateService.hentTiltaksgjennomforinger(
            enheter = nonEmptyListOf("0501"),
            apentForInnsok = ApentForInnsok.STENGT,
            innsatsgruppe = Innsatsgruppe.VARIG_TILPASSET_INNSATS,
            cacheUsage = CacheUsage.NoCache,
            erSykmeldtMedArbeidsgiver = false,
        ) shouldHaveSize 0
    }
})
