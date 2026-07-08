package no.nav.mulighetsrommet.admin.tiltak

import io.kotest.assertions.arrow.core.shouldBeLeft
import io.kotest.assertions.arrow.core.shouldBeRight
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.verify
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.tiltak.Tiltakstype
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.model.NavIdent
import no.nav.mulighetsrommet.model.TiltakstypeV3Dto
import java.time.LocalDateTime
import java.util.UUID

class UpdateTiltakstypeTest : FunSpec({
    val navIdent = NavIdent("Z999999")

    val tiltakstypeV3 = TiltakstypeV3Dto(
        id = TiltakstypeFixtures.AFT.id,
        navn = TiltakstypeFixtures.AFT.navn,
        tiltakskode = TiltakstypeFixtures.AFT.tiltakskode,
        innsatsgrupper = TiltakstypeFixtures.AFT.innsatsgrupper,
        arenaKode = TiltakstypeFixtures.AFT.arenakode,
        deltakerRegistreringInnhold = null,
        opprettetTidspunkt = LocalDateTime.now(),
        oppdatertTidspunkt = LocalDateTime.now(),
    )

    fun mockGetEksternTiltakstype(db: TestAdminDatabase, dto: TiltakstypeV3Dto) {
        every { db.queries.tiltakstype.getEksternTiltakstype(TiltakstypeFixtures.AFT.id) } returns dto
    }

    context("upsert veilederinfo") {
        test("returnerer NotFound for ukjent id") {
            val unknownId = UUID.randomUUID()

            val command = UpsertVeilederinfoCommand(
                id = unknownId,
                veilederinfo = Tiltakstype.Veilederinfo(),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(TestAdminDatabase())
                .execute(command)
                .shouldBeLeft(TiltakstypeUseCaseError.NotFound(unknownId))
        }

        test("lagrer oppdatert veilederinfo") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)
            mockGetEksternTiltakstype(db, tiltakstypeV3)

            val veilederinfo = Tiltakstype.Veilederinfo(beskrivelse = "Ny beskrivelse")

            val command = UpsertVeilederinfoCommand(
                id = TiltakstypeFixtures.AFT.id,
                veilederinfo = veilederinfo,
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db).execute(command).shouldBeRight()

            db.repository.tiltakstype.get(TiltakstypeFixtures.AFT.id)?.veilederinfo shouldBe veilederinfo
        }

        test("loggfører endring") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)
            mockGetEksternTiltakstype(db, tiltakstypeV3)

            val command = UpsertVeilederinfoCommand(
                id = TiltakstypeFixtures.AFT.id,
                veilederinfo = Tiltakstype.Veilederinfo(),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db).execute(command).shouldBeRight()

            verify { db.queries.endringshistorikk.logEndring(any(), any(), any(), any(), any(), any()) }
        }

        test("publiserer til kafka for tiltakstyper med system TILTAKSADMINISTRASJON") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)
            mockGetEksternTiltakstype(db, tiltakstypeV3)

            val command = UpsertVeilederinfoCommand(
                id = TiltakstypeFixtures.AFT.id,
                veilederinfo = Tiltakstype.Veilederinfo(),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db)
                .execute(command)
                .shouldBeRight()

            verify { db.outbox.publish(tiltakstypeV3) }
        }

        test("publiserer ikke til kafka for tiltakstyper med system ARENA") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.IPS)

            val command = UpsertVeilederinfoCommand(
                id = TiltakstypeFixtures.IPS.id,
                veilederinfo = Tiltakstype.Veilederinfo(),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db)
                .execute(command)
                .shouldBeRight()

            verify(exactly = 0) { db.outbox.publish(any<TiltakstypeV3Dto>()) }
        }
    }

    context("upsert deltakerinfo") {
        test("returnerer NotFound for ukjent id") {
            val unknownId = UUID.randomUUID()

            val command = UpsertDeltakerinfoCommand(
                id = unknownId,
                deltakerinfo = Tiltakstype.Deltakerinfo(ledetekst = null, innholdskoder = emptyList()),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(TestAdminDatabase())
                .execute(command)
                .shouldBeLeft(TiltakstypeUseCaseError.NotFound(unknownId))
        }

        test("lagrer oppdatert deltakerinfo") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)
            mockGetEksternTiltakstype(db, tiltakstypeV3)

            val deltakerinfo = Tiltakstype.Deltakerinfo(ledetekst = "Velg innhold", innholdskoder = listOf("kode1"))

            val command = UpsertDeltakerinfoCommand(
                id = TiltakstypeFixtures.AFT.id,
                deltakerinfo = deltakerinfo,
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db)
                .execute(command)
                .shouldBeRight(Unit)

            db.repository.tiltakstype.get(TiltakstypeFixtures.AFT.id)?.deltakerinfo shouldBe deltakerinfo
        }

        test("loggfører endring") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)
            mockGetEksternTiltakstype(db, tiltakstypeV3)

            val command = UpsertDeltakerinfoCommand(
                id = TiltakstypeFixtures.AFT.id,
                deltakerinfo = Tiltakstype.Deltakerinfo(null, emptyList()),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db)
                .execute(command)
                .shouldBeRight()

            verify { db.queries.endringshistorikk.logEndring(any(), any(), any(), any(), any(), any()) }
        }

        test("publiserer til kafka for tiltakstyper med system TILTAKSADMINISTRASJON") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)
            mockGetEksternTiltakstype(db, tiltakstypeV3)

            val command = UpsertDeltakerinfoCommand(
                id = TiltakstypeFixtures.AFT.id,
                deltakerinfo = Tiltakstype.Deltakerinfo(null, emptyList()),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db)
                .execute(command)
                .shouldBeRight()

            verify { db.outbox.publish(tiltakstypeV3) }
        }

        test("publiserer ikke til kafka for tiltakstyper med system ARENA") {
            val db = TestAdminDatabase()
            db.repository.tiltakstype.save(TiltakstypeFixtures.IPS)

            val command = UpsertDeltakerinfoCommand(
                id = TiltakstypeFixtures.IPS.id,
                deltakerinfo = Tiltakstype.Deltakerinfo(null, emptyList()),
                endretAv = navIdent,
            )
            UpdateTiltakstypeUseCase(db)
                .execute(command)
                .shouldBeRight()

            verify(exactly = 0) { db.outbox.publish(any<TiltakstypeV3Dto>()) }
        }
    }
})
