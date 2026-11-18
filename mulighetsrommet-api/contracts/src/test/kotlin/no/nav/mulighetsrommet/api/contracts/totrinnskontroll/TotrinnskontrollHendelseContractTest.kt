package no.nav.mulighetsrommet.api.contracts.totrinnskontroll

import io.kotest.core.spec.style.FunSpec
import no.nav.mulighetsrommet.api.contracts.test.assertContract
import no.nav.mulighetsrommet.api.domain.totrinnskontroll.TotrinnskontrollType
import no.nav.mulighetsrommet.model.NavIdent
import java.time.Instant
import java.util.UUID

class TotrinnskontrollHendelseContractTest : FunSpec({
    val hendelse = TotrinnskontrollHendelse(
        id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
        entityId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
        type = TotrinnskontrollType.ENKELTPLASS_OKONOMI,
        status = TotrinnskontrollHendelse.Status.GODKJENT,
        behandletAv = TotrinnskontrollAgent.NavAnsatt(NavIdent("A123456")),
        behandletTidspunkt = Instant.parse("2026-01-02T10:15:30Z"),
        behandletBegrunnelse = "Pris kontrollert",
        behandletAarsaker = listOf("PRISENDRING"),
        besluttetAv = TotrinnskontrollAgent.NavAnsatt(NavIdent("B654321")),
        besluttetTidspunkt = Instant.parse("2026-02-03T11:16:31.123456Z"),
        besluttetBegrunnelse = "Godkjent pris",
        besluttetAarsaker = listOf("KONTROLLERT"),
    )

    test("hendelse behandlet og besluttet av navansatte") {
        assertContract(hendelse)
    }

    test("hendelse behandlet av system uten beslutning") {
        assertContract(
            hendelse.copy(
                status = TotrinnskontrollHendelse.Status.TIL_BEHANDLING,
                behandletAv = TotrinnskontrollAgent.System("mulighetsrommet"),
                behandletBegrunnelse = null,
                behandletAarsaker = emptyList(),
                besluttetAv = null,
                besluttetTidspunkt = null,
                besluttetBegrunnelse = null,
                besluttetAarsaker = emptyList(),
            ),
        )
    }

    test("hendelse behandlet av arrangør") {
        assertContract(hendelse.copy(behandletAv = TotrinnskontrollAgent.Arrangor))
    }
})
