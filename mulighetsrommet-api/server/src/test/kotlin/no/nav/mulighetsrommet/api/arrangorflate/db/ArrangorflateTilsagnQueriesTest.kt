package no.nav.mulighetsrommet.api.arrangorflate.db

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateFilterDirection
import no.nav.mulighetsrommet.api.arrangorflate.dto.ArrangorflateTilsagnFilter
import no.nav.mulighetsrommet.api.domain.testing.fixture.ArrangorFixtures
import no.nav.mulighetsrommet.api.domain.testing.fixture.AvtaleFixtures
import no.nav.mulighetsrommet.api.fixtures.GjennomforingFixtures
import no.nav.mulighetsrommet.api.fixtures.MulighetsrommetTestDomain
import no.nav.mulighetsrommet.api.fixtures.TilsagnFixtures
import no.nav.mulighetsrommet.api.fixtures.setTilsagnStatus
import no.nav.mulighetsrommet.api.shared.Pagination
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnStatus
import no.nav.mulighetsrommet.api.tilsagn.model.TilsagnType
import no.nav.mulighetsrommet.database.kotest.extensions.ApiDatabaseTestListener
import no.nav.tiltak.okonomi.BestillingStatusType
import java.util.UUID

class ArrangorflateTilsagnQueriesTest : FunSpec({
    val database = extension(ApiDatabaseTestListener())

    val aft1 = GjennomforingFixtures.AFT1.copy(arrangorId = ArrangorFixtures.underenhet1.id)

    val vta1 = GjennomforingFixtures.VTA1.copy(arrangorId = ArrangorFixtures.underenhet1.id)

    val aftHosAnnenArrangor = GjennomforingFixtures.AFT1.copy(
        id = UUID.randomUUID(),
        arrangorId = ArrangorFixtures.underenhet2.id,
    )

    val tilsagnAft = TilsagnFixtures.createTilsagn(
        aft1.id,
        lopenummer = 1,
        type = TilsagnType.TILSAGN,
        bestillingStatus = BestillingStatusType.AKTIV,
    )
    val ekstratilsagnAft = TilsagnFixtures.createTilsagn(
        aft1.id,
        lopenummer = 2,
        type = TilsagnType.EKSTRATILSAGN,
        bestillingStatus = BestillingStatusType.AKTIV,
    )
    val investeringtilsagnAft = TilsagnFixtures.createTilsagn(
        aft1.id,
        lopenummer = 3,
        type = TilsagnType.INVESTERING,
        bestillingStatus = BestillingStatusType.AKTIV,
    )
    val tilsagnIkkeGodkjent = TilsagnFixtures.createTilsagn(
        aft1.id,
        lopenummer = 6,
        type = TilsagnType.TILSAGN,
        bestillingStatus = BestillingStatusType.AKTIV,
    )
    val tilsagnVta = TilsagnFixtures.createTilsagn(
        vta1.id,
        lopenummer = 4,
        type = TilsagnType.TILSAGN,
        bestillingStatus = BestillingStatusType.AKTIV,
    )
    val tilsagnAnnenArrangor = TilsagnFixtures.createTilsagn(
        aftHosAnnenArrangor.id,
        lopenummer = 5,
        type = TilsagnType.TILSAGN,
        bestillingStatus = BestillingStatusType.AKTIV,
    )

    val domain = MulighetsrommetTestDomain(
        avtaler = listOf(AvtaleFixtures.AFT, AvtaleFixtures.VTA),
        gjennomforinger = listOf(aft1, vta1, aftHosAnnenArrangor),
        tilsagn = listOf(
            tilsagnAft,
            ekstratilsagnAft,
            investeringtilsagnAft,
            tilsagnVta,
            tilsagnAnnenArrangor,
            tilsagnIkkeGodkjent,
        ),
    ) {
        setTilsagnStatus(tilsagnAft, TilsagnStatus.GODKJENT)
        setTilsagnStatus(ekstratilsagnAft, TilsagnStatus.GODKJENT)
        setTilsagnStatus(investeringtilsagnAft, TilsagnStatus.GODKJENT)
        setTilsagnStatus(tilsagnVta, TilsagnStatus.GODKJENT)
        setTilsagnStatus(tilsagnAnnenArrangor, TilsagnStatus.GODKJENT)
    }

    beforeSpec {
        domain.initialize(database.api)
    }

    test("henter tilsagn sortert på tilsagnstype uten å feile") {
        database.run {
            val filter = ArrangorflateTilsagnFilter(
                search = null,
                pagination = Pagination.all(),
                orderBy = ArrangorflateTilsagnFilter.OrderBy.TILSAGN,
                direction = ArrangorflateFilterDirection.ASC,
            )

            val result = queries.arrangorflate.tilsagn.getFiltered(
                arrangorer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                filter = filter,
            )

            result.items.map { it.type } shouldContainExactly listOf(
                TilsagnType.EKSTRATILSAGN,
                TilsagnType.INVESTERING,
                TilsagnType.TILSAGN,
                TilsagnType.TILSAGN,
            )
        }
    }

    test("sortering på tilsagnstype er synkende når direction er DESC") {
        database.run {
            val filter = ArrangorflateTilsagnFilter(
                search = null,
                pagination = Pagination.all(),
                orderBy = ArrangorflateTilsagnFilter.OrderBy.TILSAGN,
                direction = ArrangorflateFilterDirection.DESC,
            )

            val result = queries.arrangorflate.tilsagn.getFiltered(
                arrangorer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                filter = filter,
            )

            result.items.map { it.type } shouldContainExactly listOf(
                TilsagnType.TILSAGN,
                TilsagnType.TILSAGN,
                TilsagnType.INVESTERING,
                TilsagnType.EKSTRATILSAGN,
            )
        }
    }

    test("henter kun tilsagn for arrangører man har tilgang til") {
        database.run {
            val filter = ArrangorflateTilsagnFilter(
                search = null,
                pagination = Pagination.all(),
                orderBy = ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO,
                direction = ArrangorflateFilterDirection.ASC,
            )

            val result = queries.arrangorflate.tilsagn.getFiltered(
                arrangorer = setOf(ArrangorFixtures.underenhet2.organisasjonsnummer),
                filter = filter,
            )

            result.items.map { it.id } shouldContainExactlyInAnyOrder listOf(tilsagnAnnenArrangor.id)
        }
    }

    test("henter ingen tilsagn når man ikke har tilgang til noen arrangører") {
        database.run {
            val filter = ArrangorflateTilsagnFilter(
                search = null,
                pagination = Pagination.all(),
                orderBy = ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO,
                direction = ArrangorflateFilterDirection.ASC,
            )

            val result = queries.arrangorflate.tilsagn.getFiltered(
                arrangorer = setOf(),
                filter = filter,
            )

            result.items.shouldBeEmpty()
        }
    }

    test("ekskluderer tilsagn med status som ikke er relevant for arrangørflate") {
        database.run {
            val filter = ArrangorflateTilsagnFilter(
                search = null,
                pagination = Pagination.all(),
                orderBy = ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO,
                direction = ArrangorflateFilterDirection.ASC,
            )

            val result = queries.arrangorflate.tilsagn.getFiltered(
                arrangorer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                filter = filter,
            )

            result.items.map { it.id } shouldNotContainId tilsagnIkkeGodkjent.id
        }
    }

    test("søk filtrerer på gjennomføringens navn") {
        database.run {
            val filter = ArrangorflateTilsagnFilter(
                search = "AFT",
                pagination = Pagination.all(),
                orderBy = ArrangorflateTilsagnFilter.OrderBy.SLUTT_DATO,
                direction = ArrangorflateFilterDirection.ASC,
            )

            val result = queries.arrangorflate.tilsagn.getFiltered(
                arrangorer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                filter = filter,
            )

            result.items.map { it.id } shouldContainExactlyInAnyOrder listOf(
                tilsagnAft.id,
                ekstratilsagnAft.id,
                investeringtilsagnAft.id,
            )
        }
    }

    test("paginering begrenser antall treff, men totalCount reflekterer alle treff") {
        database.run {
            val filter = ArrangorflateTilsagnFilter(
                search = null,
                pagination = Pagination.of(page = 1, size = 2),
                orderBy = ArrangorflateTilsagnFilter.OrderBy.TILSAGN,
                direction = ArrangorflateFilterDirection.ASC,
            )

            val result = queries.arrangorflate.tilsagn.getFiltered(
                arrangorer = setOf(ArrangorFixtures.underenhet1.organisasjonsnummer),
                filter = filter,
            )

            result.items.size shouldBe 2
            result.totalCount shouldBe 4
        }
    }
})

private infix fun Collection<UUID>.shouldNotContainId(id: UUID) {
    contains(id) shouldBe false
}
