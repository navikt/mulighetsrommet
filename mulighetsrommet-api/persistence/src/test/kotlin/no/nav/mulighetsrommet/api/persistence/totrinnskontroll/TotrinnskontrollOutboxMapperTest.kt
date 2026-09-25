package no.nav.mulighetsrommet.api.persistence.totrinnskontroll

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.Totrinnskontroll
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollStatus
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.util.UUID

class TotrinnskontrollOutboxMapperTest : FunSpec({
    fun totrinnskontroll(status: TotrinnskontrollStatus) = Totrinnskontroll(
        id = UUID.randomUUID(),
        entityId = UUID.randomUUID(),
        type = TotrinnskontrollType.TILSAGN_OPPRETTELSE,
        status = status,
        behandletAv = NavIdent("B123456"),
        behandletTidspunkt = Instant.parse("2026-09-23T10:00:00Z"),
        behandletBegrunnelse = "Begrunnelse fra behandler",
        behandletAarsaker = listOf("BEHANDLET_AARSAK"),
        besluttetAv = NavIdent("B654321"),
        besluttetTidspunkt = Instant.parse("2026-09-23T11:00:00Z"),
        besluttetBegrunnelse = "Begrunnelse fra beslutter",
        besluttetAarsaker = listOf("BESLUTTET_AARSAK"),
    )

    listOf(TotrinnskontrollStatus.TIL_BEHANDLING, TotrinnskontrollStatus.GODKJENT).forEach { status ->
        test("v1-hendelse for $status bruker begrunnelse og årsaker fra behandler") {
            val hendelse = totrinnskontroll(status).toTotrinnskontrollHendelseV1()

            hendelse.forklaring shouldBe "Begrunnelse fra behandler"
            hendelse.aarsaker shouldBe listOf("BEHANDLET_AARSAK")
        }
    }

    listOf(TotrinnskontrollStatus.SATT_PA_VENT, TotrinnskontrollStatus.RETURNERT).forEach { status ->
        test("v1-hendelse for $status bruker begrunnelse og årsaker fra beslutter") {
            val hendelse = totrinnskontroll(status).toTotrinnskontrollHendelseV1()

            hendelse.forklaring shouldBe "Begrunnelse fra beslutter"
            hendelse.aarsaker shouldBe listOf("BESLUTTET_AARSAK")
        }
    }
})
