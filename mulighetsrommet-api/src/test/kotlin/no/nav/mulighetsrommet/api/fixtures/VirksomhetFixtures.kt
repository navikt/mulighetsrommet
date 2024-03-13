package no.nav.mulighetsrommet.api.fixtures

import no.nav.mulighetsrommet.api.domain.dto.VirksomhetDto
import java.util.*

object VirksomhetFixtures {
    val hovedenhet = VirksomhetDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = "123456789",
        navn = "Hovedenhet AS",
        postnummer = "0102",
        poststed = "Oslo",
    )

    val underenhet1 = VirksomhetDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = "976663934",
        overordnetEnhet = "123456789",
        navn = "Underenhet 1 AS",
        postnummer = "0103",
        poststed = "Oslo",
    )

    val underenhet2 = VirksomhetDto(
        id = UUID.randomUUID(),
        organisasjonsnummer = "890765789",
        overordnetEnhet = "123456789",
        navn = "Underenhet 2 AS",
        postnummer = "0201",
        poststed = "Lillestrøm",
    )
}
