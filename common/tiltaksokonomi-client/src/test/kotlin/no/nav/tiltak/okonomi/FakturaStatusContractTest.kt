package no.nav.tiltak.okonomi

import io.kotest.core.spec.style.FunSpec
import no.nav.tiltak.okonomi.test.assertContract
import java.time.Instant

class FakturaStatusContractTest : FunSpec({
    test("FAKTURA_STATUS") {
        assertContract(
            FakturaStatus(
                fakturanummer = Fakturanummer("A-1-1-1"),
                status = FakturaStatusType.FULLT_BETALT,
                statusSistOppdatert = Instant.parse("2025-01-01T00:00:00Z"),
            ),
        )
    }
})
