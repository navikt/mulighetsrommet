package no.nav.mulighetsrommet.api.tilskuddbehandling

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.databaseConfig
import no.nav.mulighetsrommet.api.fixtures.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddBehandlingDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.db.TilskuddDbo
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddBehandlingStatus
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.TilskuddOpplaeringType
import no.nav.mulighetsrommet.api.tilskuddbehandling.model.VedtakResultat
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.mulighetsrommet.model.Kid
import no.nav.mulighetsrommet.model.NavEnhetNummer
import no.nav.mulighetsrommet.model.Periode
import no.nav.mulighetsrommet.model.Valuta
import java.time.LocalDate
import java.util.UUID

class TilskuddBehandlingQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener(databaseConfig))

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT),
        gjennomforinger = listOf(GjennomforingFixtures.AFT1),
    )

    val behandling = TilskuddBehandlingDbo(
        id = UUID.randomUUID(),
        gjennomforingId = GjennomforingFixtures.AFT1.id,
        soknadJournalpostId = "J-2024-001",
        soknadDato = LocalDate.of(2024, 1, 15),
        periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 7, 1)),
        kostnadssted = NavEnhetNummer("0502"),
        tilskudd = listOf(
            TilskuddDbo(
                id = UUID.randomUUID(),
                tilskuddOpplaeringType = TilskuddOpplaeringType.SKOLEPENGER,
                soknadBelop = 50000,
                soknadValuta = Valuta.NOK,
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = "k1",
                utbetalingMottaker = "bruker",
                kid = null,
                belop = 100,
            ),
            TilskuddDbo(
                id = UUID.randomUUID(),
                tilskuddOpplaeringType = TilskuddOpplaeringType.EKSAMENSAVGIFT,
                soknadBelop = 1000,
                soknadValuta = Valuta.NOK,
                vedtakResultat = VedtakResultat.INNVILGELSE,
                kommentarVedtaksbrev = "k2",
                utbetalingMottaker = "arrangor",
                kid = Kid.parse("116"),
                belop = 200,
            ),
        ),
        status = TilskuddBehandlingStatus.TIL_ATTESTERING,
        kommentarIntern = "kommentar intern",
    )

    context("insert and get") {
        test("insert og get returnerer behandling med tilskudd") {
            database.runAndRollback { session ->
                domain.setup(session)

                queries.tilskuddBehandling.upsert(behandling)

                queries.tilskuddBehandling.get(behandling.id) should {
                    requireNotNull(it)
                    it.id shouldBe behandling.id
                    it.gjennomforingId shouldBe GjennomforingFixtures.AFT1.id
                    it.soknadJournalpostId shouldBe behandling.soknadJournalpostId
                    it.soknadDato shouldBe behandling.soknadDato
                    it.periode shouldBe behandling.periode
                    it.kostnadssted shouldBe behandling.kostnadssted
                    it.kommentarIntern shouldBe behandling.kommentarIntern

                    it.tilskudd.size shouldBe 2
                    it.tilskudd[0] should { v ->
                        v.id shouldBe behandling.tilskudd[0].id
                        v.tilskuddOpplaeringType shouldBe TilskuddOpplaeringType.SKOLEPENGER
                        v.soknadBelop shouldBe 50000
                        v.soknadValuta shouldBe Valuta.NOK
                        v.vedtakResultat shouldBe VedtakResultat.INNVILGELSE
                        v.kommentarVedtaksbrev shouldBe "k1"
                        v.utbetalingMottaker shouldBe "bruker"
                        v.kid shouldBe null
                        v.belop shouldBe 100
                    }
                    it.tilskudd[1] should { v ->
                        v.id shouldBe behandling.tilskudd[1].id
                        v.tilskuddOpplaeringType shouldBe TilskuddOpplaeringType.EKSAMENSAVGIFT
                        v.soknadBelop shouldBe 1000
                        v.soknadValuta shouldBe Valuta.NOK
                        v.vedtakResultat shouldBe VedtakResultat.INNVILGELSE
                        v.kommentarVedtaksbrev shouldBe "k2"
                        v.utbetalingMottaker shouldBe "arrangor"
                        v.kid shouldBe Kid.parse("116")
                        v.belop shouldBe 200
                    }
                }
            }
        }
    }
})
