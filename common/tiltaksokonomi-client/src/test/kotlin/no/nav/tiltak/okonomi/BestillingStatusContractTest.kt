package no.nav.tiltak.okonomi

import io.kotest.core.spec.style.FunSpec
import no.nav.tiltak.okonomi.test.assertContract
import java.time.Instant

class BestillingStatusContractTest : FunSpec({
    test("BESTILLING_STATUS") {
        assertContract(
            BestillingStatus(
                bestillingsnummer = Bestillingsnummer("A-1-1"),
                status = BestillingStatusType.AKTIV,
                statusSistOppdatert = Instant.parse("2025-01-01T00:00:00Z"),
            ),
        )
    }
})
