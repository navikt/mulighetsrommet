package no.nav.mulighetsrommet.api.tiltakstype.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.api.sanity.RegelverkLenke
import no.nav.mulighetsrommet.api.sanity.SanityService
import no.nav.mulighetsrommet.api.sanity.SanityTiltakstype
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeDeltakerinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.api.TiltakstypeVeilederinfoRequest
import no.nav.mulighetsrommet.api.tiltakstype.model.RedaksjoneltInnholdLenke
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeFeature
import no.nav.mulighetsrommet.api.tiltakstype.model.TiltakstypeVeilderinfo
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Faneinnhold
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import no.nav.mulighetsrommet.utils.toUUID
import java.util.UUID

const val TEST_TILTAKSTYPE_TOPIC = "tiltakstype-v3"

class TiltakstypeDetaljerServiceTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val sanityId = UUID.randomUUID()

    val sanityRegelverkLenke = RegelverkLenke(
        _id = UUID.randomUUID().toString(),
        regelverkUrl = "https://sanity.example.com",
        regelverkLenkeNavn = "Sanity-lenke",
    )
    val sanityTiltakstype = SanityTiltakstype(
        _id = sanityId.toString(),
        tiltakstypeNavn = "AFT fra Sanity",
        beskrivelse = "Sanity-beskrivelse",
        faneinnhold = Faneinnhold(forHvemInfoboks = "Sanity-infoboks"),
        regelverkLenker = listOf(sanityRegelverkLenke),
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
        tiltakstyper = listOf(TiltakstypeFixtures.AFT, TiltakstypeFixtures.IPS),
    ) {
        queries.tiltakstype.setSanityId(TiltakstypeFixtures.AFT.id, sanityId)
    }

    beforeSpec {
        domain.initialize(database.db)
        coEvery { sanityService.getTiltakstyper() } returns listOf(sanityTiltakstype)
    }

    fun createService(vararg features: TiltakstypeFeature): TiltakstypeDetaljerService {
        val tiltakstypeService = TiltakstypeService(
            config = TiltakstypeService.Config(
                features = mapOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to setOf(*features)),
            ),
            db = database.db,
        )
        return TiltakstypeDetaljerService(
            config = TiltakstypeDetaljerService.Config(topic = TEST_TILTAKSTYPE_TOPIC),
            db = database.db,
            tiltakstypeService = tiltakstypeService,
            sanityService = sanityService,
            navAnsattService = mockk(),
        )
    }

    context("getById") {
        test("returnerer redaksjonelt innhold fra Sanity når MIGRERT_REDAKSJONELT_INNHOLD ikke er satt") {
            val service = createService()

            val dto = service.getById(TiltakstypeFixtures.AFT.id).shouldNotBeNull()

            dto.veilederinfo shouldBe TiltakstypeVeilderinfo(
                beskrivelse = "Sanity-beskrivelse",
                faneinnhold = Faneinnhold(forHvemInfoboks = "Sanity-infoboks"),
                faglenker = listOf(
                    RedaksjoneltInnholdLenke(
                        sanityRegelverkLenke._id!!.toUUID(),
                        sanityRegelverkLenke.regelverkUrl!!,
                        sanityRegelverkLenke.regelverkLenkeNavn,
                    ),
                ),
                kanKombineresMed = listOf("Oppfølging"),
            )
        }

        test("returnerer redaksjonelt innhold fra databasen når MIGRERT_REDAKSJONELT_INNHOLD er satt") {
            val service = createService(TiltakstypeFeature.MIGRERT_REDAKSJONELT_INNHOLD)

            service.upsertVeilederinfo(
                TiltakstypeFixtures.AFT.id,
                TiltakstypeVeilederinfoRequest(
                    beskrivelse = "DB-beskrivelse",
                    faneinnhold = Faneinnhold(forHvemInfoboks = "DB-infoboks"),
                    faglenker = listOf(domain.regelverklenke[0].id),
                    kanKombineresMed = emptyList(),
                ),
            )

            val dto = service.getById(TiltakstypeFixtures.AFT.id).shouldNotBeNull()

            dto.veilederinfo shouldBe TiltakstypeVeilderinfo(
                beskrivelse = "DB-beskrivelse",
                faneinnhold = Faneinnhold(forHvemInfoboks = "DB-infoboks"),
                faglenker = listOf(domain.regelverklenke[0]),
                kanKombineresMed = listOf(),
            )
        }
    }

    context("upsertVeilederinfo") {
        test("lagrer og returnerer oppdatert redaksjonelt innhold") {
            val service = createService(TiltakstypeFeature.MIGRERT_REDAKSJONELT_INNHOLD)

            val faneinnhold = Faneinnhold(kontaktinfoInfoboks = "Kontaktinfo")
            val request = TiltakstypeVeilederinfoRequest(
                beskrivelse = "Oppdatert beskrivelse",
                faneinnhold = faneinnhold,
                faglenker = listOf(),
                kanKombineresMed = listOf(),
            )

            val dto = service.upsertVeilederinfo(TiltakstypeFixtures.AFT.id, request).shouldNotBeNull()

            dto.veilederinfo shouldBe TiltakstypeVeilderinfo(
                beskrivelse = "Oppdatert beskrivelse",
                faneinnhold = faneinnhold,
                faglenker = listOf(),
                kanKombineresMed = listOf(),
            )
        }
    }

    context("upsertDeltakerinfo") {
        val request = TiltakstypeDeltakerinfoRequest(
            ledetekst = "Velg innhold",
            innholdskoder = listOf(),
        )

        afterEach {
            database.truncateAll()
            domain.initialize(database.db)
        }

        test("publiserer til kafka for tiltakstyper med system TILTAKSADMINISTRASJON") {
            val service = createService()

            service.upsertDeltakerinfo(TiltakstypeFixtures.AFT.id, request)

            database.run {
                val records = queries.kafkaProducerRecord.getRecords(10, listOf(TEST_TILTAKSTYPE_TOPIC))
                records.shouldHaveSize(1).first().let { record ->
                    record.key shouldBe TiltakstypeFixtures.AFT.id.toString().toByteArray()
                    val decoded = Json.decodeFromString<TiltakstypeV3Dto>(record.value.decodeToString())
                    decoded.id shouldBe TiltakstypeFixtures.AFT.id
                }
            }
        }

        test("publiserer ikke til kafka for tiltakstyper med system ARENA") {
            val service = createService()

            service.upsertDeltakerinfo(TiltakstypeFixtures.IPS.id, request)

            database.run {
                queries.kafkaProducerRecord.getRecords(10, listOf(TEST_TILTAKSTYPE_TOPIC)).shouldBeEmpty()
            }
        }
    }
})
