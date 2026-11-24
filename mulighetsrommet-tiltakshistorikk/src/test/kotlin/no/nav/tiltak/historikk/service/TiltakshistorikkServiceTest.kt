package no.nav.tiltak.historikk.service

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockk
import no.nav.mulighetsrommet.database.kotest.extensions.FlywayDatabaseTestListener
import no.nav.mulighetsrommet.model.NorskIdent
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.tiltak.historikk.TestFixtures
import no.nav.tiltak.historikk.TiltakshistorikkV1Request
import no.nav.tiltak.historikk.databaseConfig
import no.nav.tiltak.historikk.db.TiltakshistorikkDatabase
import no.nav.tiltak.historikk.model.ArrangorDeltakerHistorikkKilde
import no.nav.tiltak.historikk.replikering.VirksomhetReplikering
import no.nav.tiltak.historikk.shouldMatchExactlyIds
import java.time.LocalDate
import java.util.UUID

class TiltakshistorikkServiceTest : FunSpec({
    val database = extension(FlywayDatabaseTestListener(databaseConfig))
    val db by lazy { TiltakshistorikkDatabase(database.db) }
    val request = TiltakshistorikkV1Request(listOf(NorskIdent("10101010100")))

    fun service(config: TiltakshistorikkService.Config) = TiltakshistorikkService(
        config = config,
        db = db,
        virksomheter = VirksomhetReplikering(db, mockk()),
    )

    afterEach {
        database.truncateAll()
    }

    context("kildevalg mellom Arena og Komet") {
        val arenaDeltakerId = TestFixtures.ArenaDeltaker.EnkelAmo.id
        val kometDeltakerId = TestFixtures.Deltaker.EnkelAmo.id
        val gruppeDeltakerId = TestFixtures.Deltaker.GruppeAmo.id

        beforeEach {
            db.session {
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arrangor)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.GruppeAmo)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.EnkelAmo)
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.GruppeAmo)
                queries.gjennomforing.upsert(TestFixtures.Gjennomforing.EnkelAmo)
                queries.arenaGjennomforing.upsert(TestFixtures.ArenaGjennomforing.EnkelAmo)
            }
        }

        test("returnerer deltakere fra begge kilder når tiltakstypen ikke har konfigurert kilde") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val config = TiltakshistorikkService.Config()
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(arenaDeltakerId, kometDeltakerId, gruppeDeltakerId)
        }

        test("returnerer bare deltakere fra Arena for tiltakstypen når Arena er valgt") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val config = TiltakshistorikkService.Config(
                arrangorTiltakHistorikkKilder = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.ARENA,
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(arenaDeltakerId, gruppeDeltakerId)
        }

        test("bytter mellom deltakere fra Komet og Arena uten å slette deltakere fra noen av kildene") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val config1 = TiltakshistorikkService.Config(
                arrangorTiltakHistorikkKilder = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.KOMET,
                ),
            )
            val kometHistorikk = service(config1).getTiltakshistorikk(request).historikk

            kometHistorikk shouldMatchExactlyIds listOf(kometDeltakerId, gruppeDeltakerId)

            val config2 = TiltakshistorikkService.Config(
                arrangorTiltakHistorikkKilder = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.ARENA,
                ),
            )
            val arenaHistorikk = service(config2).getTiltakshistorikk(request).historikk

            arenaHistorikk shouldMatchExactlyIds listOf(arenaDeltakerId, gruppeDeltakerId)
        }

        test("returnerer ikke deltakere fra Arena når Komet er valgt og Komet-deltakelsen mangler") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
            }

            val config = TiltakshistorikkService.Config(
                arrangorTiltakHistorikkKilder = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.KOMET,
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(gruppeDeltakerId)
        }

        test("returnerer deltakere fra valgt kilde uavhengig per tiltakstype") {
            val arenaGruppeDeltakerId = UUID.fromString("67bd61bc-f560-48ce-8fbd-83eff4607a65")
            val arenaGruppeAmo = TestFixtures.ArenaGjennomforing.EnkelAmo.copy(
                id = UUID.fromString("7915c5b5-6341-4566-b516-e8671784924e"),
                tiltakstypeId = TestFixtures.Tiltakstype.GruppeAmo.tiltakstypeId,
            )
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(TestFixtures.ArenaDeltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.EnkelAmo)
                queries.kometDeltaker.upsertKometDeltaker(TestFixtures.Deltaker.GruppeAmo)
                queries.arenaGjennomforing.upsert(arenaGruppeAmo)
                queries.arenaDeltaker.upsertArenaDeltaker(
                    TestFixtures.ArenaDeltaker.EnkelAmo.copy(
                        id = arenaGruppeDeltakerId,
                        arenaDeltakerId = 456,
                        arenaGjennomforingId = arenaGruppeAmo.id,
                    ),
                )
            }

            val config = TiltakshistorikkService.Config(
                arrangorTiltakHistorikkKilder = mapOf(
                    Tiltakskode.ENKELTPLASS_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.KOMET,
                    Tiltakskode.GRUPPE_ARBEIDSMARKEDSOPPLAERING to ArrangorDeltakerHistorikkKilde.ARENA,
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(kometDeltakerId, arenaGruppeDeltakerId)
        }
    }

    context("kildevalg mellom Arena og Team Tiltak") {
        val arenaArbeidstreningId = TestFixtures.ArenaDeltaker.Arbeidstrening.id
        val avtaleId = TestFixtures.Avtale.Arbeidstrening.avtaleId

        beforeEach {
            db.session {
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arrangor)
                queries.tiltakstype.upsert(TestFixtures.Tiltakstype.Arbeidstrening)
                queries.virksomhet.upsert(TestFixtures.Virksomhet.Arbeidsgiver)
                queries.arenaGjennomforing.upsert(TestFixtures.ArenaGjennomforing.Arbeidstrening)
            }
        }

        test("returnerer deltakere fra Arena når tiltakstypen ikke har konfigurert cutoff-dato") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(
                    TestFixtures.ArenaDeltaker.Arbeidstrening.copy(sluttDato = null),
                )
                queries.arbeidsgiverAvtale.upsert(
                    TestFixtures.Avtale.Arbeidstrening.copy(sluttDato = null),
                )
            }

            val config = TiltakshistorikkService.Config(
                arbeidsgiverTiltakCutoffDatoer = mapOf(
                    Tiltakskode.MENTOR to LocalDate.of(2025, 1, 1),
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(arenaArbeidstreningId)
        }

        test("returnerer deltakere fra Arena når sluttdato er før cutoff-dato") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(
                    TestFixtures.ArenaDeltaker.Arbeidstrening.copy(sluttDato = LocalDate.of(2024, 12, 31)),
                )
                queries.arbeidsgiverAvtale.upsert(
                    TestFixtures.Avtale.Arbeidstrening.copy(sluttDato = LocalDate.of(2024, 12, 31)),
                )
            }

            val config = TiltakshistorikkService.Config(
                arbeidsgiverTiltakCutoffDatoer = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2025, 1, 1),
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(arenaArbeidstreningId)
        }

        test("returnerer deltakere fra Team Tiltak når sluttdato er på cutoff-dato") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(
                    TestFixtures.ArenaDeltaker.Arbeidstrening.copy(sluttDato = LocalDate.of(2025, 1, 1)),
                )
                queries.arbeidsgiverAvtale.upsert(
                    TestFixtures.Avtale.Arbeidstrening.copy(sluttDato = LocalDate.of(2025, 1, 1)),
                )
            }

            val config = TiltakshistorikkService.Config(
                arbeidsgiverTiltakCutoffDatoer = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2025, 1, 1),
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(avtaleId)
        }

        test("returnerer deltakere fra Team Tiltak når sluttdato er etter cutoff-dato") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(
                    TestFixtures.ArenaDeltaker.Arbeidstrening.copy(sluttDato = LocalDate.of(2025, 1, 2)),
                )
                queries.arbeidsgiverAvtale.upsert(
                    TestFixtures.Avtale.Arbeidstrening.copy(sluttDato = LocalDate.of(2025, 1, 2)),
                )
            }

            val config = TiltakshistorikkService.Config(
                arbeidsgiverTiltakCutoffDatoer = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2025, 1, 1),
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(avtaleId)
        }

        test("returnerer deltakere fra Team Tiltak når sluttdato mangler og cutoff-dato er konfigurert") {
            db.session {
                queries.arenaDeltaker.upsertArenaDeltaker(
                    TestFixtures.ArenaDeltaker.Arbeidstrening.copy(sluttDato = null),
                )
                queries.arbeidsgiverAvtale.upsert(
                    TestFixtures.Avtale.Arbeidstrening.copy(sluttDato = null),
                )
            }

            val config = TiltakshistorikkService.Config(
                arbeidsgiverTiltakCutoffDatoer = mapOf(
                    Tiltakskode.ARBEIDSTRENING to LocalDate.of(2025, 1, 1),
                ),
            )
            val historikk = service(config).getTiltakshistorikk(request).historikk

            historikk shouldMatchExactlyIds listOf(avtaleId)
        }
    }
})
