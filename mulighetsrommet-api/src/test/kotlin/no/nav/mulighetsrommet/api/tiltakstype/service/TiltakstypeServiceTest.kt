package no.nav.mulighetsrommet.api.tiltakstype.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.sanity.RegelverkLenke
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeRedaksjoneltInnholdRequest
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Tiltakskode
import java.util.UUID

class TiltakstypeServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val sanityId = UUID.randomUUID()

    val sanityTiltakstype = SanityTiltakstype(
        _id = sanityId.toString(),
        tiltakstypeNavn = "AFT fra Sanity",
        beskrivelse = "Sanity-beskrivelse",
        faneinnhold = Faneinnhold(forHvemInfoboks = "Sanity-infoboks"),
        regelverkLenker = listOf(
            RegelverkLenke(
                _id = UUID.randomUUID().toString(),
                regelverkUrl = "https://sanity.example.com",
                regelverkLenkeNavn = "Sanity-lenke",
            ),
        ),
        kanKombineresMed = listOf("Oppfølging"),
    )

    val sanityService: SanityService = mockk()

    val domain = MulighetsrommetTestDomain(
        regelverklenke = listOf(
            RedaksjoneltInnholdLenke(
                id = UUID.randomUUID(),
                url = "https://db.example.com",
                navn = "DB-lenke",
                beskrivelse = null,
            ),
        ),
        tiltakstyper = listOf(TiltakstypeFixtures.AFT),
    ) {
        queries.tiltakstype.setSanityId(TiltakstypeFixtures.AFT.id, sanityId)
    }

    beforeSpec {
        domain.initialize(database.db)
        coEvery { sanityService.getTiltakstyper() } returns listOf(sanityTiltakstype)
    }

    fun createService(vararg features: TiltakstypeFeature) = TiltakstypeService(
        config = TiltakstypeService.Config(
            features = mapOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to setOf(*features)),
        ),
        db = database.db,
        sanityService = sanityService,
    )

    context("getById") {
        test("returnerer redaksjonelt innhold fra Sanity når MIGRERT_REDAKSJONELT_INNHOLD ikke er satt") {
            val service = createService()

            val dto = service.getById(TiltakstypeFixtures.AFT.id).shouldNotBeNull()

            dto.beskrivelse shouldBe "Sanity-beskrivelse"
            dto.faneinnhold?.forHvemInfoboks shouldBe "Sanity-infoboks"
            dto.faglenker.first().url shouldBe "https://sanity.example.com"
            dto.kanKombineresMed shouldBe listOf("Oppfølging")
        }

        test("returnerer redaksjonelt innhold fra databasen når MIGRERT_REDAKSJONELT_INNHOLD er satt") {
            val service = createService(TiltakstypeFeature.MIGRERT_REDAKSJONELT_INNHOLD)

            service.upsertRedaksjoneltInnhold(
                TiltakstypeFixtures.AFT.id,
                TiltakstypeRedaksjoneltInnholdRequest(
                    beskrivelse = "DB-beskrivelse",
                    faneinnhold = Faneinnhold(forHvemInfoboks = "DB-infoboks"),
                    faglenker = listOf(domain.regelverklenke[0].id),
                    kanKombineresMed = emptyList(),
                ),
            )

            val dto = service.getById(TiltakstypeFixtures.AFT.id).shouldNotBeNull()

            dto.beskrivelse shouldBe "DB-beskrivelse"
            dto.faneinnhold?.forHvemInfoboks shouldBe "DB-infoboks"
            dto.faglenker.first().url shouldBe "https://db.example.com"
            dto.kanKombineresMed.shouldBeEmpty()
        }
    }

    context("upsertRedaksjoneltInnhold") {
        test("lagrer og returnerer oppdatert redaksjonelt innhold") {
            val service = createService(TiltakstypeFeature.MIGRERT_REDAKSJONELT_INNHOLD)

            val faneinnhold = Faneinnhold(kontaktinfoInfoboks = "Kontaktinfo")
            val request = TiltakstypeRedaksjoneltInnholdRequest(
                beskrivelse = "Oppdatert beskrivelse",
                faneinnhold = faneinnhold,
                faglenker = listOf(),
                kanKombineresMed = listOf(),
            )

            val dto = service.upsertRedaksjoneltInnhold(TiltakstypeFixtures.AFT.id, request).shouldNotBeNull()

            dto.beskrivelse shouldBe "Oppdatert beskrivelse"
            dto.faneinnhold shouldBe faneinnhold
            dto.faglenker.shouldBeEmpty()
        }
    }
})
