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
        behandling = Totrinnskontroll.Behandling(
            utfortAv = NavIdent("B123456"),
            tidspunkt = Instant.parse("2026-09-23T10:00:00Z"),
            begrunnelse = "Begrunnelse fra behandler",
            aarsaker = listOf("BEHANDLET_AARSAK"),
        ),
        beslutning = if (status == TotrinnskontrollStatus.TIL_BEHANDLING) {
            null
        } else {
            Totrinnskontroll.Beslutning(
                utfortAv = NavIdent("B654321"),
                tidspunkt = Instant.parse("2026-09-23T11:00:00Z"),
                begrunnelse = "Begrunnelse fra beslutter",
                aarsaker = listOf("BESLUTTET_AARSAK"),
            )
        },
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
