package no.nav.mulighetsrommet.admin.tiltak

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.admin.testing.TestAdminDatabase
import no.nav.mulighetsrommet.api.domain.tiltak.TiltakstypeFeature
import no.nav.mulighetsrommet.api.fixtures.TiltakstypeFixtures
import no.nav.mulighetsrommet.model.Tiltakskode
import no.nav.mulighetsrommet.model.TiltakstypeEgenskap

class TiltakstypeKompaktDtoQueryTest : FunSpec({
    val db = TestAdminDatabase()

    beforeSpec {
        db.repository.tiltakstype.save(TiltakstypeFixtures.AFT)
        db.repository.tiltakstype.save(TiltakstypeFixtures.IPS)
    }

    fun createQuery(vararg features: TiltakstypeFeature): TiltakstypeKompaktQuery {
        val tiltakstypeService = TiltakstypeService(
            config = TiltakstypeService.Config(
                features = mapOf(Tiltakskode.ARBEIDSFORBEREDENDE_TRENING to setOf(*features)),
            ),
            db = db,
        )
        return TiltakstypeKompaktQuery(tiltakstypeService)
    }

    test("returnerer kun tiltakstyper med VISES_I_TILTAKSADMINISTRASJON") {
        val query = createQuery(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON)

        val result = query.execute(GetAllTiltakstypeKompakt(egenskaper = setOf()))

        result.shouldHaveSize(1)
        result.first().id shouldBe TiltakstypeFixtures.AFT.id
    }

    test("returnerer ingen tiltakstyper når ingen har VISES_I_TILTAKSADMINISTRASJON") {
        val query = createQuery()

        val result = query.execute(GetAllTiltakstypeKompakt(egenskaper = setOf()))

        result.shouldBeEmpty()
    }

    test("returnerer tiltakstyper som inkluderer egenskaper fra filter") {
        val query = createQuery(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON)

        val result = query.execute(
            GetAllTiltakstypeKompakt(egenskaper = setOf(TiltakstypeEgenskap.STOTTER_AVTALER)),
        )

        result.shouldHaveSize(1)
        result.first().id shouldBe TiltakstypeFixtures.AFT.id
    }

    test("returnerer ingen tiltakstyper når ingen matcher egenskaper fra filter") {
        val query = createQuery(TiltakstypeFeature.VISES_I_TILTAKSADMINISTRASJON)

        val result = query.execute(
            GetAllTiltakstypeKompakt(egenskaper = setOf(TiltakstypeEgenskap.STOTTER_ENKELTPLASSER)),
        )

        result.shouldBeEmpty()
    }
})
