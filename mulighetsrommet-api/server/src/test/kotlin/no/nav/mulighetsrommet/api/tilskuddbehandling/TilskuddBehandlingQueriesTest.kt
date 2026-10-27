package no.nav.mulighetsrommet.api.tilskuddbehandling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.mulighetsrommet.api.TransactionalQueryContext
import no.nav.mulighetsrommet.api.domain.opplaring.Opplaeringtilskudd
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.UtbetalingFixtures
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.BrukerUtbetalingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingType
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddMottaker
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddVedtak
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandling
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.JournalpostId
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import no.nav.mulighetsrommet.model.ValutaBelop
import java.time.LocalDate
import java.util.UUID

class TilskuddBehandlingQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1),
    )

    val behandligId = UUID.randomUUID()
    val behandling = TilskuddBehandling(
        id = behandligId,
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        tilskudd = listOf(
            TilskuddVedtak(
                id = UUID.randomUUID(),
                behandlingId = behandligId,
                tilskuddId = UUID.randomUUID(),
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.SKOLEPENGER,
                soknadBelop = ValutaBelop(
                    belop = 100,
                    valuta = Valuta.SEK,
                ),
                utbetalingBelop = ValutaBelop(
                    belop = 100,
                    valuta = Valuta.NOK,
                ),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = "k1",
                utbetalingMottaker = TilskuddMottaker.BRUKER,
                kid = null,
                brukerUtbetalingType = BrukerUtbetalingType.REFUSJON,
                soknadJournalpostId = JournalpostId("123"),
                soknadDato = LocalDate.of(2024, 1, 15),
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 7, 1)),
                kostnadssted = NavEnhetNummer("0502"),
                kommentarIntern = "kommentar intern 1",
            ),
            TilskuddVedtak(
                id = UUID.randomUUID(),
                behandlingId = behandligId,
                tilskuddId = UUID.randomUUID(),
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.EKSAMENSGEBYR,
                soknadBelop = ValutaBelop(
                    belop = 1000,
                    valuta = Valuta.NOK,
                ),
                utbetalingBelop = ValutaBelop(
                    belop = 200,
                    valuta = Valuta.NOK,
                ),
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = "k2",
                utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                kid = Kid.parse("116"),
                brukerUtbetalingType = null,
                soknadJournalpostId = JournalpostId("123"),
                soknadDato = LocalDate.of(2024, 1, 15),
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 7, 1)),
                kostnadssted = NavEnhetNummer("0502"),
                kommentarIntern = "kommentar intern 2",
            ),
            TilskuddVedtak(
                id = UUID.randomUUID(),
                behandlingId = behandligId,
                tilskuddId = UUID.randomUUID(),
                tilskuddOpplaeringType = Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD,
                soknadBelop = ValutaBelop(
                    belop = 1000,
                    valuta = Valuta.NOK,
                ),
                utbetalingBelop = null,
                vedtakResultat = VedtakResultat.AVSLAG,
                kommentarVedtaksbrev = "k2",
                utbetalingMottaker = TilskuddMottaker.ARRANGOR,
                kid = null,
                brukerUtbetalingType = null,
                soknadJournalpostId = JournalpostId("123"),
                soknadDato = LocalDate.of(2024, 1, 15),
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 7, 1)),
                kostnadssted = NavEnhetNummer("0502"),
                kommentarIntern = "kommentar intern 3",
            ),
        ),
        status = TilskuddBehandlingStatus.TIL_GODKJENNING,
        type = TilskuddBehandlingType.REGISTRERING,
        behandlendeEnhet = NavEnhetNummer("0502"),
    )

    context("insert and get") {
        test("insert og get returnerer behandling med tilskudd") {
            database.runAndRollback {
                domain.initialize()

                queries.tilskuddBehandling.upsert(behandling)

                queries.tilskuddBehandling.get(behandling.id) should {
                    requireNotNull(it)
                    it.id shouldBe behandling.id
                    it.gjennomforingId shouldBe GjennomforingFixtures.AFT1.id

                    it.tilskudd.size shouldBe 3
                    val vedtakById = it.tilskudd.associateBy { vedtak -> vedtak.id }
                    vedtakById[behandling.tilskudd[0].id] should { v ->
                        requireNotNull(v)
                        v.id shouldBe behandling.tilskudd[0].id
                        v.tilskuddId shouldBe behandling.tilskudd[0].tilskuddId
                        v.tilskuddOpplaeringType shouldBe Opplaeringtilskudd.Kode.SKOLEPENGER
                        v.soknadBelop.belop shouldBe 100
                        v.soknadBelop.valuta shouldBe Valuta.SEK
                        v.vedtakResultat.type shouldBe VedtakResultat.INNVILGELSE
                        v.kommentarVedtaksbrev shouldBe "k1"
                        v.utbetalingMottaker shouldBe TilskuddMottaker.BRUKER
                        v.kid shouldBe null
                        v.utbetalingBelop?.valuta shouldBe Valuta.NOK
                        v.utbetalingBelop?.belop shouldBe 100
                        v.soknadJournalpostId shouldBe behandling.tilskudd[0].soknadJournalpostId
                        v.soknadDato shouldBe behandling.tilskudd[0].soknadDato
                        v.periode shouldBe behandling.tilskudd[0].periode
                        v.kostnadssted.enhetsnummer shouldBe behandling.tilskudd[0].kostnadssted
                        v.kommentarIntern shouldBe behandling.tilskudd[0].kommentarIntern
                    }
                    vedtakById[behandling.tilskudd[1].id] should { v ->
                        requireNotNull(v)
                        v.id shouldBe behandling.tilskudd[1].id
                        v.tilskuddId shouldBe behandling.tilskudd[1].tilskuddId
                        v.tilskuddOpplaeringType shouldBe Opplaeringtilskudd.Kode.EKSAMENSGEBYR
                        v.soknadBelop.belop shouldBe 1000
                        v.soknadBelop.valuta shouldBe Valuta.NOK
                        v.vedtakResultat.type shouldBe VedtakResultat.INNVILGELSE
                        v.kommentarVedtaksbrev shouldBe "k2"
                        v.utbetalingMottaker shouldBe TilskuddMottaker.ARRANGOR
                        v.kid shouldBe Kid.parse("116")
                        v.utbetalingBelop?.belop shouldBe 200
                        v.utbetalingBelop?.valuta shouldBe Valuta.NOK
                        v.soknadJournalpostId shouldBe behandling.tilskudd[1].soknadJournalpostId
                        v.soknadDato shouldBe behandling.tilskudd[1].soknadDato
                        v.periode shouldBe behandling.tilskudd[1].periode
                        v.kostnadssted.enhetsnummer shouldBe behandling.tilskudd[1].kostnadssted
                        v.kommentarIntern shouldBe behandling.tilskudd[1].kommentarIntern
                    }
                    vedtakById[behandling.tilskudd[2].id] should { v ->
                        requireNotNull(v)
                        v.id shouldBe behandling.tilskudd[2].id
                        v.tilskuddId shouldBe behandling.tilskudd[2].tilskuddId
                        v.tilskuddOpplaeringType shouldBe Opplaeringtilskudd.Kode.INTEGRERT_BOTILBUD
                        v.utbetalingBelop shouldBe null
                        v.vedtakResultat.type shouldBe VedtakResultat.AVSLAG
                        v.soknadJournalpostId shouldBe behandling.tilskudd[2].soknadJournalpostId
                        v.soknadDato shouldBe behandling.tilskudd[2].soknadDato
                        v.periode shouldBe behandling.tilskudd[2].periode
                        v.kostnadssted.enhetsnummer shouldBe behandling.tilskudd[2].kostnadssted
                        v.kommentarIntern shouldBe behandling.tilskudd[2].kommentarIntern
                    }
                }
            }
        }

        test("getByGjennomforingId grupperer behandlinger og vedtak") {
            database.runAndRollback {
                domain.initialize()
                val otherVedtak = behandling.tilskudd.first().copy(
                    id = UUID.randomUUID(),
                    tilskuddId = UUID.randomUUID(),
                )
                val otherBehandling = behandling.copy(
                    id = UUID.randomUUID(),
                    tilskudd = listOf(otherVedtak),
                )
                queries.tilskuddBehandling.upsert(behandling)
                queries.tilskuddBehandling.upsert(otherBehandling)

                val result = queries.tilskuddBehandling.getByGjennomforingId(GjennomforingFixtures.AFT1.id)
                    .associateBy { it.id }

                result.keys shouldBe setOf(behandling.id, otherBehandling.id)
                result.getValue(behandling.id).tilskudd.map { it.id }.toSet() shouldBe behandling.tilskudd.map { it.id }.toSet()
                result.getValue(otherBehandling.id).tilskudd.map { it.id }.toSet() shouldBe setOf(otherVedtak.id)
                queries.tilskuddBehandling.get(UUID.randomUUID()) shouldBe null
            }
        }

        test("behandling uten vedtak gir feil ved henting") {
            database.runAndRollback {
                domain.initialize()
                val noVedtakBehandling = behandling.copy(id = UUID.randomUUID(), tilskudd = emptyList())
                queries.tilskuddBehandling.upsert(noVedtakBehandling)

                shouldThrow<IllegalArgumentException> {
                    queries.tilskuddBehandling.get(noVedtakBehandling.id)
                }.message shouldBe "Tilskuddsbehandling med id ${noVedtakBehandling.id} mangler vedtak"
            }
        }

        test("getByGjennomforingId feiler hvis en av behandlingene mangler vedtak") {
            database.runAndRollback {
                domain.initialize()
                queries.tilskuddBehandling.upsert(behandling)
                val noVedtakBehandling = behandling.copy(id = UUID.randomUUID(), tilskudd = emptyList())
                queries.tilskuddBehandling.upsert(noVedtakBehandling)

                shouldThrow<IllegalArgumentException> {
                    queries.tilskuddBehandling.getByGjennomforingId(GjennomforingFixtures.AFT1.id)
                }.message shouldBe "Tilskuddsbehandling med id ${noVedtakBehandling.id} mangler vedtak"

                queries.tilskuddBehandling.getByGjennomforingId(UUID.randomUUID()) shouldBe emptyList()
            }
        }
    }

    test("utbetaling_id kan settes") {
        database.runAndRollback {
            domain.initialize()

            val tilskudd = behandling.tilskudd[0]
            queries.tilskuddBehandling.upsert(behandling)
            queries.utbetaling.upsert(UtbetalingFixtures.utbetaling1)

            queries.tilskuddBehandling.setUtbetalingTilskuddVedtak(tilskudd.id, UtbetalingFixtures.utbetaling1.id)

            queries.utbetaling.getByTilskuddVedtak(tilskudd.id) should {
                it!!.id shouldBe UtbetalingFixtures.utbetaling1.id
            }
        }
    }

    test("upsert beholder tilskuddsnummer ved retry") {
        database.runAndRollback {
            domain.initialize()

            queries.tilskuddBehandling.upsert(behandling)
            val forsteTilskuddsnummer = behandling.tilskudd.map { tilskuddVedtak ->
                tilskuddsnummerFor(tilskuddVedtak.tilskuddId)
            }

            queries.tilskuddBehandling.upsert(behandling)
            val andreTilskuddsnummer = behandling.tilskudd.map { tilskuddVedtak ->
                tilskuddsnummerFor(tilskuddVedtak.tilskuddId)
            }

            andreTilskuddsnummer shouldBe forsteTilskuddsnummer
        }
    }
})

private fun TransactionalQueryContext.tilskuddsnummerFor(tilskuddId: UUID): String {
    return requireNotNull(
        session.single(queryOf("select tilskuddsnummer from tilskudd where id = ?::uuid", tilskuddId)) {
            it.string("tilskuddsnummer")
        },
    )
}
