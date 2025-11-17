package no.nav.tiltak.historikk.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.amt.model.AmtDeltakerV1Dto
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.DeltakerStatus
import no.nav.mulighetsrommet.model.DeltakerStatusType
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Organisasjonsnummer
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.TiltakshistorikkV1Dto
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.queries.KometDeltakerQueries
import no.nav.tiltak.historikk.kafka.consumers.toGjennomforingDbo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class KometDeltakerQueriesTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))

    val gruppeAmo = TestFixtures.gjennomforingGruppe
    val amtDeltaker = TestFixtures.amtDeltaker

    beforeAny {
        TiltakshistorikkDatabase(database.db).session {
            queries.virksomhet.upsert(TestFixtures.virksomhet)
            queries.gjennomforing.upsert(toGjennomforingDbo(gruppeAmo))
        }
    }

    test("kometHistorikk") {
        database.runAndRollback { session ->
            val deltaker = KometDeltakerQueries(session)
            deltaker.upsertKometDeltaker(amtDeltaker)

            deltaker.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), null) shouldBe listOf(
                TiltakshistorikkV1Dto.GruppetiltakDeltakelse(
                    id = amtDeltaker.id,
                    norskIdent = NorskIdent("10101010100"),
                    startDato = null,
                    sluttDato = null,
                    status = DeltakerStatus(
                        type = DeltakerStatusType.VENTER_PA_OPPSTART,
                        aarsak = null,
                        opprettetDato = LocalDateTime.of(2022, 1, 1, 0, 0),
                    ),
                    tiltakstype = TiltakshistorikkV1Dto.GruppetiltakDeltakelse.Tiltakstype(
                        tiltakskode = gruppeAmo.tiltakskode,
                        navn = null,
                    ),
                    gjennomforing = TiltakshistorikkV1Dto.Gjennomforing(
                        id = gruppeAmo.id,
                        navn = gruppeAmo.navn,
                        tiltakskode = gruppeAmo.tiltakskode,
                    ),
                    arrangor = TiltakshistorikkV1Dto.Arrangor(Organisasjonsnummer("123123123")),
                ),
            )
        }
    }

    test("maxAgeYears komet") {
        database.runAndRollback { session ->
            val deltaker = KometDeltakerQueries(session)

            val amtDeltakerReg2005 = AmtDeltakerV1Dto(
                id = UUID.randomUUID(),
                gjennomforingId = gruppeAmo.id,
                personIdent = "10101010100",
                startDato = null,
                sluttDato = null,
                status = DeltakerStatus(
                    type = DeltakerStatusType.VENTER_PA_OPPSTART,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                ),
                registrertDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                endretDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                dagerPerUke = 2.5f,
                prosentStilling = null,
                deltakelsesmengder = listOf(),
            )
            val amtDeltakerReg2005Slutt2024 = AmtDeltakerV1Dto(
                id = UUID.randomUUID(),
                gjennomforingId = gruppeAmo.id,
                personIdent = "10101010100",
                startDato = null,
                sluttDato = LocalDate.of(2024, 1, 1),
                status = DeltakerStatus(
                    type = DeltakerStatusType.VENTER_PA_OPPSTART,
                    aarsak = null,
                    opprettetDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                ),
                registrertDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                endretDato = LocalDateTime.of(2005, 3, 1, 0, 0, 0),
                dagerPerUke = 2.5f,
                prosentStilling = null,
                deltakelsesmengder = listOf(),
            )
            deltaker.upsertKometDeltaker(amtDeltaker)
            deltaker.upsertKometDeltaker(amtDeltakerReg2005)
            deltaker.upsertKometDeltaker(amtDeltakerReg2005Slutt2024)

            deltaker.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), null)
                .map { it.id } shouldContainExactlyInAnyOrder listOf(
                amtDeltaker.id,
                amtDeltakerReg2005.id,
                amtDeltakerReg2005Slutt2024.id,
            )
            deltaker.getKometHistorikk(listOf(NorskIdent(amtDeltaker.personIdent)), 5)
                .map { it.id } shouldContainExactlyInAnyOrder listOf(
                amtDeltaker.id,
                amtDeltakerReg2005Slutt2024.id,
            )
        }
    }
})
